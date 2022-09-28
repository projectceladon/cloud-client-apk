#include <getopt.h>
#include <signal.h>
#include <unistd.h>

#include <condition_variable>
#include <iostream>
#include <list>
#include <mutex>
#include <string>
#ifdef USE_SDL
#include <SDL2/SDL.h>
#include <SDL2/SDL_ttf.h>
#include <pthread.h>

#include "GameSession.h"
#include "VideoDecoder.h"
#include "VideoDecoderDispatcher.h"
#include "VideoDispatcher.h"
#include "aic_client_render.h"
#include "owt/base/logging.h"
#include "webrtc/api/task_queue/default_task_queue_factory.h"
#include "webrtc/rtc_base/task_queue.h"
#else
#include "AudioPlayer.h"
#include "EncodedVideoDispatcher.h"
#include "PcObserver.h"
#include "VideoDecoder.h"
#include "VideoDirectRender.h"
#include "VideoRender.h"
#include "owt/base/stream.h"
#include "owt/p2p/p2pclient.h"
#include "owt_signalingchannel.h"
#endif

#include <sys/time.h>

#include <atomic>

#include "owt/base/videorendererinterface.h"

using namespace owt::p2p;
int VIDEO_FPS_INTERVAL;
#ifdef USE_SDL
#define AIC_REFRESH_EVENT (SDL_USEREVENT + 1)

#define AIC_BREAK_EVENT (SDL_USEREVENT + 2)

#define AIC_SWAP_EVENT (SDL_USEREVENT + 3)

#define AIC_FRAME_EVENT (SDL_USEREVENT + 4)

#define AIC_ANIM_EVENT (SDL_USEREVENT + 5)

#define SAVED_FRAME_COUNT 6
typedef struct SessionDescriptor {
  std::vector<AVFrame*> decode_frames;
  int readIndex = 0;
  int writeIndex = 0;
  bool active;
  GLuint textures[2];
  EGLImage images[2];
  string identifier;
  std::mutex mutex;
  std::shared_ptr<VideoDecoder> decoder;
  char fps_buf[10];
  float frame_count = 0;
  long last_render_time = 0;
} SessionDescriptor;

int exit_thread = 0;

int anim_interval = 0;
int anim_state = -1;
int video_fps_thread(void* opaque) {
  int space = 0;
  int count;
  if (opaque != nullptr) {
    space = *((int*)opaque);
    count = space * 1000 / (VIDEO_FPS_INTERVAL);
    space = count;
  }

  int anim_frames = 0;
  if (anim_interval > 0) {
    anim_frames = anim_interval * 1000 / (VIDEO_FPS_INTERVAL);
  }

  count = 0;
  int anim_tick = 0;
  while (!exit_thread) {
    SDL_Event event;
    event.type = AIC_REFRESH_EVENT;
    SDL_PushEvent(&event);
    SDL_Delay(VIDEO_FPS_INTERVAL);  // 30fps
    if (space != 0) {
      count++;
      if (count == space) {
        count = 0;
        SDL_Event event1;
        event1.type = AIC_SWAP_EVENT;
        SDL_PushEvent(&event1);
      }
    }
    if (anim_frames != 0 && anim_state > 0) {
      anim_tick++;
      if (anim_tick == anim_frames) {
        anim_state = anim_state + 1001;
        anim_tick = 0;
      }
    }
  }
  exit_thread = 0;
  SDL_Event event;
  event.type = AIC_BREAK_EVENT;
  SDL_PushEvent(&event);
  return 0;
}
#endif

static const struct option long_option[] = {
    {"client-id", required_argument, NULL, 'c'},
    {"device", required_argument, NULL, 'd'},
    {"resolution", required_argument, NULL, 'r'},
    {"server-id", required_argument, NULL, 's'},
    {"url", required_argument, NULL, 'u'},
    {"video-codec", required_argument, NULL, 'v'},
    {"window-size", required_argument, NULL, 'w'},
    {"window-x", required_argument, NULL, 'x'},
    {"window-y", required_argument, NULL, 'y'},
    {"help", no_argument, NULL, 'h'},
    {"fps", required_argument, NULL, 'f'},
    {"audio", no_argument, NULL, 'a'},
    {"dynamic_resolution", no_argument, NULL, 'z'},
    {"log", required_argument, NULL, 'l'},
    {"movie", required_argument, NULL, 'm'},
    {NULL, 0, NULL, 0}};

void resolveIds(std::string& ids, std::vector<std::string>& vector_ids) {
  std::string sep_comma(",");
  std::string sep_semi(":");
  ids += sep_comma;
  int size = ids.size();
  std::string::size_type pos;
  std::string s;
  std::string::size_type pos_semi;
  int begin;
  int end;
  std::string s_id;
  for (int i = 0; i < size; i++) {
    pos = ids.find(sep_comma, i);
    if (pos < size) {
      s = ids.substr(i, pos - i);
      if (s[0] == 's' || s[0] == 'c') {
        pos_semi = s.find(sep_semi);
        if (pos_semi != -1) {
          begin = atoi(s.substr(1, pos_semi).c_str());
          end = atoi(s.substr(pos_semi + 1, s.length() - pos_semi - 1).c_str());
          for (int j = begin; j <= end; j++) {
            s_id = s.substr(0, 1) + std::to_string(j);
            vector_ids.push_back(s_id);
          }
        } else {
          vector_ids.push_back(s);
        }
      } else {
        vector_ids.push_back(s);
      }
      i = pos;
    }
  }
}

int ceilSqrt(int num) {
  int n = sqrt(num);
  if (n * n == num) {
    return n;
  } else {
    return n + 1;
  }
}

void help() {
  std::cout << "--client-id/-c <client_id>: Client id, ie c0-3,c4,c6-9"
            << std::endl;
  std::cout << "--device/-d <sw/hw>: Software decoding or hardware decoding, "
               "default: hw"
            << std::endl;
  std::cout
      << "--resolution/-r <aic_resolution>: Aic resolution, default: 1280x720"
      << std::endl;
  std::cout << "--server-id/-s <server_id>: Server id, ie s0-3,s4,s6-9"
            << std::endl;
  std::cout << "--url/-u <url>: Url of signaling server, for example: "
               "http://192.168.17.109:8095"
            << std::endl;
  std::cout << "--video-codec/-v <h264/h265>: Video codec, default: h264"
            << std::endl;
  std::cout << "--window-size/-w <window_size>: Window size, default: 352x288"
            << std::endl;
  std::cout << "--cycle_number/-n <cycle_num>: numbers to show in a cycle "
               "option is 4, 9, 16, 25, 36"
            << std::endl;
  std::cout << "--cycle_interval/-i <cycle-interval>: time spaces between a "
               "cycle, minutes , default 5 minutes"
            << std::endl;
  std::cout << "--fps/-f : fps of the client, default 30" << std::endl;
  std::cout << "--audio/-a : with audio , default no audio" << std::endl;
  std::cout << "--dynamic_resolution/-z : dynamic resolution, default false"
            << std::endl;
  std::cout << "--log/-l: dump the log for debug, default 0， 1 for render "
               "detail, 2 for main thread fps"
            << std::endl;
  std::cout << "--movie/-m: streams presents an anim" << std::endl;
#ifdef USE_SDL
  std::cout
      << "--window-x/-x <window_x>: Window postion x, default: in the center"
      << std::endl;
  std::cout
      << "--window-y/-y <window_y>: Window postion y, default: in the center"
      << std::endl;
#endif
}

int main(int argc, char* argv[]) {
  signal(SIGPIPE, SIG_IGN);
  std::string signaling_server_url;
  std::string server_ids;
  std::string client_ids;
  std::string resolution = "1280x720";
  std::string video_codec = "h264";
  std::string device = "hw";
  std::string window_size = "352x288";
  int fps = 30;
  bool playAudio = false;
  bool dynamicRes = false;
  int debug = 0;
#ifdef USE_SDL
  int window_x = SDL_WINDOWPOS_UNDEFINED;
  int window_y = SDL_WINDOWPOS_UNDEFINED;
  int cycle_num = 0;
  int cycle_interval = 10;
#endif
  int opt = 0;
  while ((opt = getopt_long(argc, argv, "c:d:r:s:u:v:w:x:y:hn:i:f:azl:m:",
                            long_option, NULL)) != -1) {
    switch (opt) {
      case 'c':
        client_ids = optarg;
        break;
      case 'd':
        device = optarg;
        break;
      case 'r':
        resolution = optarg;
        break;
      case 's':
        server_ids = optarg;
        break;
      case 'u':
        signaling_server_url = optarg;
        break;
      case 'v':
        video_codec = optarg;
        break;
      case 'w':
        window_size = optarg;
        break;
      case 'a':
        playAudio = true;
        break;
      case 'z':
        dynamicRes = true;
        break;
      case 'l':
        debug = atoi(optarg);
        break;
#ifdef USE_SDL
      case 'x':
        window_x = atoi(optarg);
        break;
      case 'y':
        window_y = atoi(optarg);
        break;
      case 'n':
        cycle_num = atoi(optarg);
        break;
      case 'i':
        cycle_interval = atoi(optarg);
        break;
      case 'f':
        fps = atoi(optarg);
        break;
      case 'm':
        anim_interval = atoi(optarg);
        break;
#endif
      case 'h':
        help();
        exit(0);
        break;
      default:
        exit(0);
        break;
    }
  }

  VIDEO_FPS_INTERVAL = 1000 / fps;

  std::cout << "LAST_COMMIT: " << LAST_COMMIT << ", internal "
            << VIDEO_FPS_INTERVAL << std::endl;

  int fullScreen = -1;

  /***************p2p***********/
  if (signaling_server_url.empty() || server_ids.empty() ||
      client_ids.empty()) {
    std::cout << "Input parameters are not correct!" << std::endl;
    help();
    exit(0);
  }
  std::vector<std::string> vector_servers;
  std::vector<std::string> vector_clients;
  resolveIds(server_ids, vector_servers);
  resolveIds(client_ids, vector_clients);
  int index = 0;
  for (auto id : vector_servers) {
    std::cout << index << ":" << id << " ";
    index++;
  }
  std::cout << std::endl;
  index = 0;
  for (auto id : vector_clients) {
    std::cout << index << ":" << id << " ";
    index++;
  }
  std::cout << std::endl;

  int lines = vector_servers.size();
  if (lines != vector_clients.size()) {
    std::cout << "client an server not match" << std::endl;
    exit(0);
  }
  // parse signaling_server_url
  std::string::size_type pos = signaling_server_url.rfind(":");
  if (pos == std::string::npos) {
    std::cout << "Signaling server url is not correct!" << std::endl;
    exit(0);
  }

  std::string str = signaling_server_url.substr(0, pos);
  std::string port = signaling_server_url.substr(pos + 1);
  pos = str.rfind("/");
  if (pos == std::string::npos) {
    std::cout << "Signaling server url is not correct!" << std::endl;
    exit(0);
  }
  std::string ip = str.substr(pos + 1);

  // parse window_size
  int window_width = 0;
  int window_height = 0;
  pos = window_size.find("x");
  if (pos == std::string::npos) {
    std::cout << "window size is not correct!" << std::endl;
    exit(0);
  }

  window_width = atoi(window_size.substr(0, pos).c_str());
  window_height = atoi(window_size.substr(pos + 1).c_str());

  // codec type
  uint32_t codec_type = (uint32_t)VideoCodecType::kH264;
  if (video_codec == "h265") {
    codec_type = (uint32_t)VideoCodecType::kH265;
  }

#ifdef USE_SDL
  // parse window_size
  atexit(SDL_Quit);
  /***************decoder***********/
  // parse resolution
  pos = resolution.find("x");
  if (pos == std::string::npos) {
    std::cout << "Resolution is not correct!" << std::endl;
    exit(0);
  }

  // *********fix me !!! delete it
  int width = atoi(resolution.substr(0, pos).c_str());
  int height = atoi(resolution.substr(pos + 1).c_str());

  int sp_num;
  int displays;
  if (cycle_num > 0) {
    sp_num = ceilSqrt(cycle_num);
    cycle_num = sp_num * sp_num;
    displays = cycle_num;
  } else {
    sp_num = ceilSqrt(lines);
    displays = lines;
  }
  AicClientRender*
      aicRender;  //(window_x, window_y, &window_width, &window_height, sp_num);
  auto task_queue_factory = webrtc::CreateDefaultTaskQueueFactory();
  auto event_queue_ =
      std::make_unique<rtc::TaskQueue>(task_queue_factory->CreateTaskQueue(
          "aic_linux_render", webrtc::TaskQueueFactory::Priority::HIGH));

  // owt::base::Logging::LogToConsole(owt::base::LoggingSeverity::kInfo);
  std::vector<std::shared_ptr<GameSession>> game_sessions_display_;
  std::vector<std::shared_ptr<GameSession>> game_sessions_;
  std::vector<RenderParams*> render_params_;
  std::map<std::string, std::shared_ptr<SessionDescriptor>> session_des_;
  /***************render 2***********/
  int playingIndex = 0;

  int row;
  int column;
  int margin = 0;  // displays > 1 ? 10 : 0;
  int cell_width;
  int cell_height;
  int cell_with_margin;
  int cell_height_margin;
  int** game_matrix;
  game_matrix = new int*[sp_num];
  for (int i = 0; i < sp_num; i++) game_matrix[i] = new int[sp_num];
  event_queue_->PostTask([&] {
    if (anim_interval > 0) {
      aicRender = new AicClientRender(window_x, window_y, &window_width,
                                      &window_height, 1);
    } else {
      aicRender = new AicClientRender(window_x, window_y, &window_width,
                                      &window_height, sp_num);
    }

    cell_width = window_width / sp_num;
    cell_height = window_height / sp_num;
    cell_with_margin = cell_width - margin;
    cell_height_margin = cell_height - margin;

    for (int i = 0; i < displays; i++) {
      std::unique_ptr<GameP2PParams> p2pParams =
          std::make_unique<GameP2PParams>();
      p2pParams->signaling_server_url = signaling_server_url;
      p2pParams->server_id = vector_servers[i];
      p2pParams->client_id = vector_clients[i];
      p2pParams->server_ip = ip;
      p2pParams->video_codec = video_codec;
      p2pParams->dr = displays > 1 ? dynamicRes : false;
      p2pParams->log = debug & 0x1;
      if (p2pParams->dr) {
        p2pParams->video_width = cell_with_margin;
        p2pParams->video_height = cell_height_margin;
      } else {
        p2pParams->video_width = width;
        p2pParams->video_height = height;
      }

      row = i / sp_num;
      column = i % sp_num;
      RenderParams* render_params = new RenderParams();
      render_params->left = column * cell_width;
      render_params->top = row * cell_height;
      render_params->width = cell_with_margin;
      render_params->height = cell_height_margin;
      render_params_.push_back(render_params);
      std::shared_ptr<GameSession> game_session = std::make_shared<GameSession>(
          std::move(p2pParams), render_params, true, playAudio);
      game_sessions_display_.push_back(game_session);
      game_matrix[row][column] = i;
      game_sessions_.push_back(game_session);

      std::shared_ptr<SessionDescriptor> desc_ =
          std::make_shared<SessionDescriptor>();
      desc_->active = true;
      desc_->identifier = vector_servers[i];
      desc_->decoder = std::make_shared<VideoDecoder>();
      desc_->decoder->initDecoder(codec_type);
      desc_->decode_frames.resize(SAVED_FRAME_COUNT - 1);
      glGenTextures(2, desc_->textures);
      for (int j = 0; j < 2; ++j) {
        glBindTexture(GL_TEXTURE_2D, desc_->textures[j]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      }
      session_des_[vector_servers[i]] = desc_;
    }
    playingIndex = displays - 1;
    for (int i = displays; i < lines; i++) {
      std::unique_ptr<GameP2PParams> p2pParams =
          std::make_unique<GameP2PParams>();
      p2pParams->signaling_server_url = signaling_server_url;
      p2pParams->server_id = vector_servers[i];
      p2pParams->client_id = vector_clients[i];
      p2pParams->server_ip = ip;
      p2pParams->video_codec = video_codec;
      p2pParams->dr = displays > 1 ? dynamicRes : false;
      if (p2pParams->dr) {
        p2pParams->video_width = cell_with_margin;
        p2pParams->video_height = cell_height_margin;
      } else {
        p2pParams->video_width = width;
        p2pParams->video_height = height;
      }

      p2pParams->log = debug & 0x1;
      std::shared_ptr<GameSession> game_session = std::make_shared<GameSession>(
          std::move(p2pParams), nullptr, false, playAudio);
      game_sessions_.push_back(game_session);

      std::shared_ptr<SessionDescriptor> desc_ =
          std::make_shared<SessionDescriptor>();
      desc_->active = false;
      desc_->identifier = vector_servers[i];
      desc_->decoder = std::make_shared<VideoDecoder>();
      desc_->decoder->initDecoder(codec_type);
      desc_->decode_frames.resize(SAVED_FRAME_COUNT - 1);
      glGenTextures(2, desc_->textures);
      for (int j = 0; j < 2; ++j) {
        glBindTexture(GL_TEXTURE_2D, desc_->textures[j]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      }
      session_des_[vector_servers[i]] = desc_;
    }
    for (auto session : game_sessions_) {
      session->startSession();  // pull all the stream
    }
  });

  GlobalConfiguration::SetEncodedVideoFrameEnabled(true);
  auto callback = [&](const std::string& id,
                      std::unique_ptr<VideoEncodedFrame> frame) {
    if (frame->length > 0) {
      if (anim_interval > 0 && anim_state == -1) {
        anim_state = 1;
      }
      AVPacket* pkt = av_packet_alloc();
      pkt->data = const_cast<uint8_t*>(frame->buffer);
      pkt->size = frame->length;
      std::shared_ptr<SessionDescriptor> desc_ = session_des_[id];
      if (desc_ != nullptr && desc_->active) {
        AVFrame* avframe = av_frame_alloc();
        desc_->decoder->decode(pkt, avframe);
        {
          int nextIndex = (desc_->writeIndex + 1) % SAVED_FRAME_COUNT;
          std::unique_lock<std::mutex> locker(desc_->mutex);
          if (desc_->readIndex == nextIndex) {
            std::cout << desc_->identifier << " more than 5 frames"
                      << std::endl;
            av_frame_free(&avframe);
          } else {
            desc_->decode_frames[desc_->writeIndex] = avframe;
            desc_->writeIndex = nextIndex;
          }
          Uint32 time = SDL_GetTicks();
          if (desc_->frame_count == 30) {
            sprintf(desc_->fps_buf, "%.2f",
                    30000.00 / (time - desc_->last_render_time));
            desc_->frame_count = 0;
            desc_->last_render_time = time;
          } else if (desc_->frame_count == 0) {
            desc_->last_render_time = time;
          }
          desc_->frame_count++;
          av_packet_free(&pkt);
        }
      } else {
        std::cout << id << " no matching session" << std::endl;
      }
    }
  };
  std::unique_ptr<owt::base::VideoDecoderInterface> mVideoDispatcher =
      std::make_unique<VideoDispatcher>(callback);
  GlobalConfiguration::SetCustomizedVideoDecoderEnabled(
      std::move(mVideoDispatcher));

  if (lines > displays) {
    SDL_Thread* display_tick_tid = SDL_CreateThread(
        video_fps_thread, "video_fps_thread-2", &cycle_interval);
  } else {
    SDL_Thread* video_tick_tid =
        SDL_CreateThread(video_fps_thread, "video_fps_thread-1", NULL);
  }

  auto onMouseMove = [&](SDL_MouseMotionEvent& e) {
    if (e.state == 1) {
      int row = (e.y + margin) / cell_height;
      int column = (e.x + margin) / cell_width;
      int index = game_matrix[row][column];
      game_sessions_display_[index]->dispatchEvent(e);
    }
  };

  auto onMouseButton = [&](SDL_MouseButtonEvent& e) {
    std::cout << "onMouseButton cell_height: " << cell_height
              << ", cell_width: " << cell_width << std::endl;
    std::cout << "onMouseButton e.y " << e.y << ", e.x " << e.x << std::endl;
    int row = (e.y + margin) / cell_height;
    int column = (e.x + margin) / cell_width;
    int index = game_matrix[row][column];
    std::cout << "onMouseButton row " << row << ", column " << column
              << ", index " << index << std::endl;
    game_sessions_display_[index]->dispatchEvent(e);
  };

  auto onSwapStream = [&]() {
    std::cout << "onSwapStream " << std::endl;
    int startIndex = (playingIndex + 1) % lines;
    int endIndex =
        startIndex + displays <= lines ? startIndex + displays : lines;

    std::vector<std::shared_ptr<GameSession>> sv;
    for (int i = startIndex; i < endIndex; i++) {
      sv.push_back(game_sessions_[i]);
    }
    for (auto session : game_sessions_display_) {
      session->suspendStream(true, nullptr);
      auto desc = session_des_[session->getSessionId()];
      desc->active = false;  // deactive the stream
    }
    game_sessions_display_.assign(sv.begin(), sv.end());

    for (int i = 0; i < game_sessions_display_.size(); i++) {
      game_sessions_display_[i]->suspendStream(false, render_params_[i]);

      auto session = session_des_[game_sessions_display_[i]->getSessionId()];
      session->active = true;
    }
    playingIndex = endIndex - 1;
  };

  bool full_screen = false;
  bool running = true;
  SDL_Event e;
  Uint32 lastRenderTime = SDL_GetTicks();
  Uint32 renderTime;
  Uint32 fpsStartTime = lastRenderTime;
  int frame_count = 0;
  bool printFps = debug & 0x2;

  while (running) {
    SDL_WaitEvent(&e);
    switch (e.type) {
      case AIC_REFRESH_EVENT:
        event_queue_->PostTask([&] {
          Uint32 lastRenderTime = SDL_GetTicks();
          int nextIndex;
          int split_num = anim_state;
          int total_num = 0;
          int count = 0;
          int fullScreenIndex = -1;
          if (anim_interval > 0) {
            if (split_num == -1) {
              std::cout << "AIC_REFRESH_EVENT not any frames" << std::endl;
              return;
            } else if (split_num > 0) {
              if (split_num > 1000) {
                anim_state -= 1000;
                split_num -= 1000;
                aicRender->renderUpdate(split_num);
                if (split_num == sp_num) {
                  anim_state = 0;
                  split_num = 0;
                }
              }
              total_num = split_num * split_num;
              std::cout << "split_num " << split_num << ", sp_num: " << sp_num
                        << std::endl;
            }
          } else {
            std::cout << "anim_state reset" << std::endl;
            if (fullScreen >= 0) {
              total_num = 1;
              fullScreenIndex = fullScreen;
              aicRender->renderUpdate(1);
            } else {
              aicRender->renderUpdate(sp_num);
            }
          }
          std::vector<std::shared_ptr<GameSession>> sessions;
          AVFrame* render_frame = nullptr;
          sessions.assign(game_sessions_display_.begin(),
                          game_sessions_display_.end());
          aicRender->beginFrame();
          for (int i = 0; i < sessions.size(); i++) {
            auto session = session_des_[sessions[i]->getSessionId()];
            {
              std::unique_lock<std::mutex> locker(session->mutex);
              if (session->readIndex == session->writeIndex) {
                std::cout << session->identifier << " no frames to render"
                          << std::endl;
              } else {
                render_frame = session->decode_frames[session->readIndex];
                nextIndex = (session->readIndex + 1) % SAVED_FRAME_COUNT;
                session->readIndex = nextIndex;
              }
            }
            if (fullScreenIndex >= 0) {
              if (i != fullScreenIndex) {
                if (render_frame != nullptr) {
                  av_frame_unref(render_frame);
                  av_frame_free(&render_frame);
                }
                continue;
              }
            } else {
              if (split_num > 0 && i >= total_num) {
                if (render_frame != nullptr) {
                  av_frame_unref(render_frame);
                  av_frame_free(&render_frame);
                }
                continue;
              }
            }
            if (render_frame != nullptr) {
              aicRender->generateTexture(render_frame, &(session->textures[0]),
                                         &(session->images[0]),
                                         session->decoder->getVADisplay());
              av_frame_unref(render_frame);
              av_frame_free(&render_frame);
              if (fullScreenIndex >= 0) {
                aicRender->renderFrame(0, &(session->textures[0]));
              } else {
                aicRender->renderFrame(i, &(session->textures[0]));
              }
              aicRender->destroyImage(&(session->images[0]));
            } else {
              aicRender->renderFrame(i, &(session->textures[0]));
            }
            std::string ss;
            ss.append(session->identifier);
            ss.append(":");
            ss.append(session->fps_buf);
            if (fullScreenIndex >= 0) {
              aicRender->renderText(ss.c_str(), 0);
            } else {
              aicRender->renderText(ss.c_str(), i);
            }
          }
          aicRender->endFrame();
          // std::cout << "AIC_REFRESH_EVENT impl cost "  <<  SDL_GetTicks() -
          // lastRenderTime  << std::endl;
        });
        break;
      case SDL_QUIT:
        exit_thread = 1;
        running = false;
        break;
      case SDL_MOUSEBUTTONDOWN:
      case SDL_MOUSEBUTTONUP:
        onMouseButton(e.button);
        break;
      case SDL_MOUSEMOTION:
        onMouseMove(e.motion);
        break;
      case AIC_SWAP_EVENT:
        onSwapStream();
        break;
      case SDL_KEYDOWN:
        if (SDLK_f == e.key.keysym.sym) {
          if (fullScreen >= 0) {
            fullScreen = -1;
          } else {
            int x, y;
            SDL_GetMouseState(&x, &y);
            int row = (y + margin) / cell_height;
            int column = (x + margin) / cell_width;
            int index = game_matrix[row][column];
            SDL_Log("Mouse cursor is at %d, %d, index is %d", x, y, index);
            fullScreen = index;
          }
          std::cout << "f down " << fullScreen << std::endl;
        }
        break;
      case SDL_WINDOWEVENT:
        break;
      /*case AIC_BREAK_EVENT:
        break;*/
      default:
        // std::cout << "Unhandled SDL event " << e.type << std::endl;
        break;
    }
  }
  for (auto session : game_sessions_) {
    session->freeSession();
  }

  for (auto frame : render_params_) {
    delete frame;
  }
  for (int i = 0; i < sp_num; i++) {
    delete[] game_matrix[i];
  }
  delete[] game_matrix;
  SDL_Quit();
#else
  std::list<AVFrame*> frame_list;
  std::mutex mutex;
  std::condition_variable cond;

  std::shared_ptr<VideoDecoder> decoder = std::make_shared<VideoDecoder>();
  decoder->initDecoder(codec_type);

  auto callback = [&](std::unique_ptr<VideoEncodedFrame> frame) {
    if (frame->length > 0) {
      AVPacket* pkt = av_packet_alloc();
      pkt->data = const_cast<uint8_t*>(frame->buffer);
      pkt->size = frame->length;

      AVFrame* frame = av_frame_alloc();
      decoder->decode(pkt, frame);
      av_packet_free(&pkt);

      std::unique_lock<std::mutex> locker(mutex);
      if (frame_list.size() > 10) {
        av_frame_free(&frame);
        return;
      }

      frame_list.push_back(frame);
      cond.notify_one();
    }
  };

  GlobalConfiguration::SetEncodedVideoFrameEnabled(true);
  std::unique_ptr<owt::base::VideoDecoderInterface> mEncodedVideoDispatcher =
      std::make_unique<EncodedVideoDispatcher>(callback);
  GlobalConfiguration::SetCustomizedVideoDecoderEnabled(
      std::move(mEncodedVideoDispatcher));

  P2PClientConfiguration configuration;
  IceServer stunServer, turnServer;
  std::string stunUrl = "stun:" + ip + ":3478";
  stunServer.urls.push_back(stunUrl);
  stunServer.username = "username";
  stunServer.password = "password";
  configuration.ice_servers.push_back(stunServer);

  std::string turnUrlTcp = "turn:" + ip + ":3478?transport=tcp";
  std::string turnUrlUdp = "turn:" + ip + ":3478?transport=udp";
  turnServer.urls.push_back(turnUrlTcp);
  turnServer.urls.push_back(turnUrlUdp);
  turnServer.username = "username";
  turnServer.password = "password";
  configuration.ice_servers.push_back(turnServer);
  configuration.suspend_remote_stream = false;

  VideoCodecParameters videoParam;
  if (video_codec == "h264") {
    videoParam.name = owt::base::VideoCodec::kH264;
  } else if (video_codec == "h265") {
    videoParam.name = owt::base::VideoCodec::kH265;
  } else {
    std::cout << "Cannot support this codec!" << std::endl;
  }
  VideoEncodingParameters video_params(videoParam, 0, true);
  configuration.video_encodings.push_back(video_params);

  auto sc = std::make_shared<OwtSignalingChannel>();
  auto pc = std::make_shared<P2PClient>(configuration, sc);

  std::shared_ptr<AudioPlayer> audio_player = std::make_shared<AudioPlayer>();
  PcObserver ob(audio_player);
  pc->AddObserver(ob);
  pc->AddAllowedRemoteId(vector_servers[0]);
  pc->Connect(signaling_server_url, vector_clients[0], nullptr, nullptr);
  pc->Send(vector_servers[0], "start", nullptr, nullptr);

  std::shared_ptr<VideoDirectRender> renderer =
      std::make_shared<VideoDirectRender>();
  renderer->initRender(window_width, window_height);
  renderer->setVADisplay(decoder->getVADisplay());
  renderer->setEventListener([&](const char* event, const char* param) {
    char msg[256];
    snprintf(msg, 256,
             "{\"type\": \"control\", \"data\": { \"event\": \"%s\", "
             "\"parameters\": %s }}",
             event, param);
    pc->Send(vector_servers[0], msg, nullptr, nullptr);
  });

  AVFrame* av_frame = nullptr;
  while (1) {
    if (renderer->handleWindowEvents() < 0) {
      break;
    }

    {
      std::unique_lock<std::mutex> locker(mutex);
      while (frame_list.size() <= 0) {
        if (cond.wait_for(locker, std::chrono::milliseconds(10)) ==
            std::cv_status::timeout)
          break;
      }

      if (frame_list.size() <= 0) continue;

      av_frame = frame_list.front();
      frame_list.pop_front();
    }

    VASurfaceID va_surface = (uintptr_t)av_frame->data[3];
    renderer->OnFrame(va_surface);
    av_frame_free(&av_frame);
  }

  pc->Stop(vector_servers[0], nullptr, nullptr);
  pc->RemoveObserver(ob);
  pc->Disconnect(nullptr, nullptr);

  std::unique_lock<std::mutex> locker(mutex);
  while (frame_list.size() > 0) {
    av_frame = frame_list.front();
    frame_list.pop_front();
    av_frame_free(&av_frame);
  }
#endif
  return 0;
}
