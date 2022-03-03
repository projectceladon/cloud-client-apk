#include <string>
#include <iostream>
#include <unistd.h>
#include <SDL2/SDL.h>

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
    std::cout << __func__ << ":" << std::endl;
    init();
  }

  virtual ~AudioPlayer() {
    std::cout << __func__ << ":" << std::endl;
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

int main(int argc, char* argv[]) {
  if (argc < 6) {
    std::cout << "usage:" << argv[0] << " <ip> <port> <serverId> <clientId> <width> <height> <codec>" << std::endl;
    std::cout << "example:" << argv[0] << " 10.112.240.116 0 1280 720 h265" << std::endl;
    exit(0);
  }

  std::string ip = argv[1];
  std::string port = argv[2];
  std::string serverId = argv[3];
  std::string clientId = argv[4];

  int width = atoi(argv[5]);
  int height = atoi(argv[6]);

  std::string codec = argv[7];

  std::string url = "http://" + ip + ":" + port;

  SDL_Init(SDL_INIT_VIDEO);
  atexit(SDL_Quit);

  std::string title = ip + "    android " + serverId + "    " + codec;
  auto win = SDL_CreateWindow(title.c_str(), SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED, width, height, SDL_WINDOW_RESIZABLE);
  if (!win) {
    std::cout << "Failed to create SDL window!" << std::endl;
  }

  std::shared_ptr<VideoRenderer> renderer = std::make_shared<VideoRenderer>(win, width, height);
  std::shared_ptr<CGVideoDecoder> decoder = std::make_shared<CGVideoDecoder>();
  const char *device_name = "vaapi";

  FrameResolution resolution = FrameResolution::k480p;
  if (height == 600)
    resolution = FrameResolution::k600p;
  else if (height == 720)
    resolution = FrameResolution::k720p;
  else if (height == 1080) {
    resolution = FrameResolution::k1080p;
  }

  uint32_t codec_type = (uint32_t)VideoCodecType::kH264;
  if (codec == "h265") {
    codec_type = (uint32_t)VideoCodecType::kH265;
  }

  if (decoder->init(resolution, codec_type, device_name, 0) < 0) {
    std::cout << "VideoDecoder init failed. " << device_name << " decoding" << std::endl;
  } else {
    std::cout << "VideoDecoder init done. Device: " << device_name << std::endl;
  }

  int size = width * height * 3 / 2;
  std::vector<uint8_t> buffer = std::vector<uint8_t>(size);

  auto callback = [&](std::unique_ptr<VideoEncodedFrame> frame) {
    decoder->decode(frame->buffer, (int)frame->length, &buffer[0]);
    renderer->RenderFrame(&buffer[0], size);
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
  if (codec == "h264") {
    videoParam.name = owt::base::VideoCodec::kH264;
  } else if (codec == "h265") {
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
  pc->AddAllowedRemoteId(serverId);
  pc->Connect(url, clientId, nullptr, nullptr);
  pc->Send(serverId, "start", nullptr, nullptr);

  auto sendCtrl = [&](const char* event, const char* param) {
    char msg[256];

    snprintf(msg, 256, "{\"type\": \"control\", \"data\": { \"event\": \"%s\", \"parameters\": %s }}", event, param);
    //std::cout << "sendCtl: " <<  msg << std::endl;
    pc->Send(serverId, msg, nullptr, nullptr);
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

  pc->Stop(serverId, nullptr, nullptr);
  pc->RemoveObserver(ob);
  pc->Disconnect(nullptr, nullptr);
  SDL_DestroyWindow(win);
  SDL_Quit();
  return 0;
}
