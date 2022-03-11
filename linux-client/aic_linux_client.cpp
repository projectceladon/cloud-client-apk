#include <string>
#include <iostream>
#include <unistd.h>
#include <SDL2/SDL.h>
#include <getopt.h>
#include <signal.h>

#include "owt/base/stream.h"
#include "owt/base/videorendererinterface.h"
#include "owt/base/audioplayerinterface.h"
#include "owt/p2p/p2pclient.h"
#include "owt_signalingchannel.h"

#include "CGCodec.h"
#include "VideoRender.h"
#include "EncodedVideoDispatcher.h"

using namespace owt::p2p;

class AudioPlayer : public owt::base::AudioPlayerInterface {
public:
  AudioPlayer() {
    init();
  }

  virtual ~AudioPlayer() {
  }

  void OnData(const void *audio_data, int bits_per_sample,
              int sample_rate, size_t number_of_channels,
              size_t number_of_frames) override {
    //std::cout << __func__ << ":" << " bits:" << bits_per_sample << ",sample rate:" << sample_rate <<
    //    ",channels:" << number_of_channels << ",frames:" << number_of_frames << std::endl;
  }

private:
  void init() {}
};

class PcObserver : public owt::p2p::P2PClientObserver {
public:
  PcObserver() {}
  virtual ~PcObserver() {}

  void OnMessageReceived(const std::string& remote_user_id,
                         const std::string message) {
    std::cout << __func__ << ":from" << remote_user_id << ", msg:" << message << std::endl;
  }

  virtual void OnStreamAdded(std::shared_ptr<owt::base::RemoteStream> stream) override {
    stream->AttachAudioPlayer(mAudioPlayer);
  }

  void OnServerDisconnected() {
    std::cout << __func__ << ":" << std::endl;
  }

private:
  AudioPlayer mAudioPlayer;
};

static const struct option long_option[] = {
  {"client-id",   required_argument, NULL, 'c'},
  {"device",      required_argument, NULL, 'd'},
  {"resolution",  required_argument, NULL, 'r'},
  {"server-id",   required_argument, NULL, 's'},
  {"url",         required_argument, NULL, 'u'},
  {"video-codec", required_argument, NULL, 'v'},
  {"window-size", required_argument, NULL, 'w'},
  {"help",        no_argument,       NULL, 'h'},
  {NULL,          0,                 NULL,  0 }
};

void help() {
  std::cout << "--client-id/-c <client_id>: Client id" << std::endl;
  std::cout << "--device/-d <sw/hw>: Software decoding or hardware decoding, default: hw" << std::endl;
  std::cout << "--resolution/-r <aic_resolution>: Aic resolution, default: 1280x720" << std::endl;
  std::cout << "--server-id/-s <server_id>: Server id" << std::endl;
  std::cout << "--url/-u <url>: Url of signaling server, for example: http://192.168.17.109:8095" << std::endl;
  std::cout << "--video-codec/-v <h264/h265>: Video codec, default: h264" << std::endl;
  std::cout << "--window-size/-w <window_size>: Window size, default: 352x288" << std::endl;
}

int main(int argc, char* argv[]) {
  signal(SIGPIPE, SIG_IGN);
  std::string signaling_server_url;
  std::string server_id;
  std::string client_id;
  std::string resolution = "1280x720";
  std::string video_codec = "h264";
  std::string device = "hw";
  std::string window_size = "352x288";

  int opt = 0;
  while ((opt = getopt_long(argc, argv, "c:d:r:s:u:v:w:h", long_option, NULL)) != -1) {
    switch (opt) {
      case 'c':
        client_id = optarg;
        break;
      case 'd':
        device = optarg;
        break;
      case 'r':
        resolution = optarg;
        break;
      case 's':
        server_id = optarg;
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
      case 'h':
        help();
        exit(0);
        break;
      default:
        exit(0);
        break;
    }
  }

  if (signaling_server_url.empty() ||
      server_id.empty() ||
      client_id.empty()) {
     std::cout << "Input parameters are not correct!" << std::endl;
     help();
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

  // parse resolution
  pos = resolution.find("x");
  if (pos == std::string::npos) {
    std::cout << "Resolution is not correct!" << std::endl;
    exit(0);
  }

  int width = atoi(resolution.substr(0, pos).c_str());
  int height = atoi(resolution.substr(pos + 1).c_str());

  // parse window_size
  pos = window_size.find("x");
  if (pos == std::string::npos) {
    std::cout << "window size is not correct!" << std::endl;
    exit(0);
  }

  int window_width = atoi(window_size.substr(0, pos).c_str());
  int window_height = atoi(window_size.substr(pos + 1).c_str());

  bool is_sw_decoding;
  if (device == "hw") {
    is_sw_decoding = false;
  } else if (device == "sw") {
    is_sw_decoding = true;
  } else {
    std::cout << "Device parameter is not correct!" << std::endl;
    exit(0);
  }

  SDL_Init(SDL_INIT_VIDEO);
  atexit(SDL_Quit);

  std::string title = ip + "    android-" + server_id + "    " + video_codec;
  auto win = SDL_CreateWindow(title.c_str(), SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED, window_width, window_height, SDL_WINDOW_RESIZABLE);
  if (!win) {
    std::cout << "Failed to create SDL window!" << std::endl;
  }

  std::shared_ptr<CGVideoDecoder> decoder = std::make_shared<CGVideoDecoder>();

  FrameResolution frame_resolution = FrameResolution::k480p;
  if (height == 600) {
    frame_resolution = FrameResolution::k600p;
  } else if (height == 720) {
    frame_resolution = FrameResolution::k720p;
  } else if (height == 1080) {
    frame_resolution = FrameResolution::k1080p;
  }

  uint32_t codec_type = (uint32_t)VideoCodecType::kH264;
  if (video_codec == "h265") {
    codec_type = (uint32_t)VideoCodecType::kH265;
  }

  if (decoder->init(frame_resolution, codec_type, is_sw_decoding ? nullptr : "vaapi", 0) < 0) {
    std::cout << "VideoDecoder init failed. " << std::endl;
    exit(0);
  } else {
    std::cout << "VideoDecoder init done." << std::endl;
  }

  std::shared_ptr<VideoRenderer> renderer =
    std::make_shared<VideoRenderer>(win, width, height, is_sw_decoding ? SDL_PIXELFORMAT_IYUV : SDL_PIXELFORMAT_NV12);

  const int frame_size = width * height * 3 / 2;
  std::vector<uint8_t> buffer = std::vector<uint8_t>(frame_size);

  int out_size = 0;
  auto callback = [&](std::unique_ptr<VideoEncodedFrame> frame) {
    decoder->decode(frame->buffer, (int)frame->length, &buffer[0], &out_size);
    if (out_size > 0) {
      if (out_size != frame_size) {
        std::cout << "Frame size should not be correct! out size: "
                  << out_size << ", frame size: "
                  << frame_size << std::endl;
      }
      renderer->RenderFrame(&buffer[0], out_size);
    }
  };

  GlobalConfiguration::SetEncodedVideoFrameEnabled(true);
  std::unique_ptr<owt::base::VideoDecoderInterface> mEncodedVideoDispatcher = std::make_unique<EncodedVideoDispatcher>(callback);
  GlobalConfiguration::SetCustomizedVideoDecoderEnabled(std::move(mEncodedVideoDispatcher));

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

  PcObserver ob;
  pc->AddObserver(ob);
  pc->AddAllowedRemoteId(server_id);
  pc->Connect(signaling_server_url, client_id, nullptr, nullptr);
  pc->Send(server_id, "start", nullptr, nullptr);

  auto sendCtrl = [&](const char* event, const char* param) {
    char msg[256];

    snprintf(msg, 256, "{\"type\": \"control\", \"data\": { \"event\": \"%s\", \"parameters\": %s }}", event, param);
    //std::cout << "sendCtl: " <<  msg << std::endl;
    pc->Send(server_id, msg, nullptr, nullptr);
  };

  auto onMouseMove = [&](SDL_MouseMotionEvent& e) {
    if (e.state == 1) {
      char param[64];
      int w, h;
      SDL_GetWindowSize(win, &w, &h);

      int x = e.x * 32767 / w;
      int y = e.y * 32767 / h;
      snprintf(param, 64, "{\"x\": %d, \"y\": %d, \"movementX\": %d, \"movementY\": %d }", x, y, e.xrel, e.yrel);
      sendCtrl("mousemove", param);
    }
  };

  auto onMouseButton = [&](SDL_MouseButtonEvent& e) {
    char param[64];
    const char* et = (e.type == SDL_MOUSEBUTTONDOWN) ? "mousedown" : "mouseup";
    int w, h;

    SDL_GetWindowSize(win, &w, &h);

    int x = e.x * 32767 / w;
    int y = e.y * 32767 / h;
    snprintf(param, 64, "{\"which\": %d, \"x\": %d, \"y\": %d }", e.which, x, y);
    sendCtrl(et, param);
  };

  bool fullscreen = false;
  bool running = true;
#if 0
  while (running) {
    SDL_Event e;
    while (SDL_PollEvent(&e)) {
      switch (e.type) {
        case SDL_QUIT:
          running = false;
          break;
        case SDL_MOUSEBUTTONDOWN:
        case SDL_MOUSEBUTTONUP:
          onMouseButton(e.button);
          break;
        case SDL_MOUSEMOTION:
          onMouseMove(e.motion);
          break;
        case SDL_KEYDOWN: {
            if (e.key.keysym.sym == SDLK_F11) {
              uint32_t flags = fullscreen ? 0 : SDL_WINDOW_FULLSCREEN_DESKTOP;
              SDL_SetWindowFullscreen(win, flags);
              fullscreen = !fullscreen;
            }
          }
          break;
        case SDL_WINDOWEVENT:
          break;
        default:
          std::cout << "Unhandled SDL event " << e.type << std::endl;
          break;
      }
    }
  }
#else
  SDL_Event e;
  while (running) {
    SDL_WaitEvent(&e);
    switch (e.type) {
      case SDL_QUIT:
        running = false;
        break;
      case SDL_MOUSEBUTTONDOWN:
      case SDL_MOUSEBUTTONUP:
        onMouseButton(e.button);
        break;
      case SDL_MOUSEMOTION:
        onMouseMove(e.motion);
        break;
      case SDL_KEYDOWN: {
          if (e.key.keysym.sym == SDLK_F11) {
            uint32_t flags = fullscreen ? 0 : SDL_WINDOW_FULLSCREEN_DESKTOP;
            SDL_SetWindowFullscreen(win, flags);
            fullscreen = !fullscreen;
          }
        }
        break;
      case SDL_WINDOWEVENT:
        break;
      default:
        //std::cout << "Unhandled SDL event " << e.type << std::endl;
        break;
    }
  }
#endif

  pc->Stop(server_id, nullptr, nullptr);
  pc->RemoveObserver(ob);
  pc->Disconnect(nullptr, nullptr);
  SDL_DestroyWindow(win);
  SDL_Quit();
  return 0;
}
