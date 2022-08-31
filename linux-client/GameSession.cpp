#include "GameSession.h"

#include "nlohmann/json.hpp"

using json = nlohmann::json;

GameSession::GameSession(std::unique_ptr<GameP2PParams> p2p_params,
              RenderParams* render_params,
              bool render, bool play_audio) {
  p2p_params_ = std::move(p2p_params);
  suspend_ = !render;
  session_desc_ = p2p_params_->server_id;
  initP2P();
  if (play_audio) {
    std::cout << "play_audio " << play_audio << std::endl;
    audio_player_ = std::make_shared<AudioPlayer>();
  }
  if (render) {
    setupRenderEnv(render_params);
  }
}

void GameSession::setupRenderEnv(RenderParams* render_params) {
  rect_.x = render_params->left;
  rect_.y = render_params->top;
  rect_.w = render_params->width;
  rect_.h = render_params->height;
}

void GameSession::initP2P() {
  P2PClientConfiguration configuration;
  IceServer stunServer, turnServer;
  std::string stunUrl = "stun:" + p2p_params_->server_ip + ":3478";
  stunServer.urls.push_back(stunUrl);
  stunServer.username = "username";
  stunServer.password = "password";
  configuration.ice_servers.push_back(stunServer);

  std::string turnUrlTcp =
      "turn:" + p2p_params_->server_ip + ":3478?transport=tcp";
  std::string turnUrlUdp =
      "turn:" + p2p_params_->server_ip + ":3478?transport=udp";
  turnServer.urls.push_back(turnUrlTcp);
  turnServer.urls.push_back(turnUrlUdp);
  turnServer.username = "username";
  turnServer.password = "password";
  configuration.ice_servers.push_back(turnServer);

  VideoCodecParameters videoParam;
  if (p2p_params_->video_codec == "h264") {
    videoParam.name = owt::base::VideoCodec::kH264;
  } else if (p2p_params_->video_codec == "h265") {
    videoParam.name = owt::base::VideoCodec::kH265;
  } else {
    std::cout << "Cannot support this codec!" << std::endl;
  }
  VideoEncodingParameters video_params(videoParam, 0, true);
  configuration.video_encodings.push_back(video_params);

  configuration.suspend_remote_stream = suspend_;
  configuration.identifier = p2p_params_ -> server_id;

  sc_ = std::make_shared<OwtSignalingChannel>();
  pc_ = std::make_shared<P2PClient>(configuration, sc_);
  // ouF.open("./me.yuv", std::ofstream::binary| std::ofstream::out);
}

void GameSession::startSession() {
  ob_ = std::make_unique<PcObserver>(audio_player_);
  pc_->AddObserver(*ob_);
  pc_->AddAllowedRemoteId(p2p_params_->server_id);
  pc_->Connect(p2p_params_->signaling_server_url, p2p_params_->client_id,
               nullptr, nullptr);

  json j;
  j["type"] = "control";
  json jdata;
  jdata["event"] = "sizechange";
  json jparams;
  jparams["rendererSize"] = {{"width", p2p_params_->video_width},
                             {"height", p2p_params_->video_height}};
  // jparams["rendererSize"] = {{"width", 1280}, {"height", 720}};
  jparams["mode"] = "stretch";
  jdata["parameters"] = jparams;
  j["data"] = jdata;
  std::string jsonstr = j.dump();
  pc_->Send(p2p_params_->server_id, jsonstr.c_str(), nullptr, nullptr);
  std::cout << "send: " << jsonstr << std::endl;
  pc_->Send(p2p_params_->server_id, "start", nullptr, nullptr);
}

void GameSession::suspendStream(bool suspend, RenderParams* render_params) {
  suspend_ = suspend;
  if (!suspend) {
    setupRenderEnv(render_params);
  }
  pc_->Suspend(p2p_params_->server_id, suspend, nullptr);
}

/*int count = 0;
bool suspend = true;*/
/*void GameSession::renderFrame() {
    std::unique_ptr<owt::base::VideoBuffer> video_buffer = video_renderer_ ->
getFrame(); if (video_buffer) { if (suspend_) { video_buffer = nullptr;  //
release the buffer return;
      }
      uint8_t* buffer = static_cast<uint8_t *>(video_buffer -> buffer);
     // ouF.write((const char *)buffer, frame_width_ * frame_height_ * 3/2);
     // ouF.flush();
      if (buffer) {
          frame_width_ = video_buffer ->resolution.width;
          frame_height_ = video_buffer ->resolution.height;
          uint32_t w = video_buffer ->resolution.width;
          uint8_t *y = buffer;
          uint8_t *u = y + frame_width_ * frame_height_;
          uint8_t *v = u + frame_width_ * frame_height_ / 4;
          render_rect_.w = frame_width_;
          render_rect_.h = frame_height_;
          SDL_UpdateTexture(texture_, &render_rect_, buffer, video_buffer
->resolution.width);
        }
    }
    //count++;
    //std::cout<< "renderFrame " << count << std::endl;
    //if (count == 300) {
    //  suspend = !suspend;
    //  std::cout<< "count == 300 , change suspend state to " << suspend <<
std::endl;
    //  pc_ -> Suspend(p2p_params_.server_id, suspend, nullptr);
    //  count = 0;
    //}
}*/

// int test = 0;

bool GameSession::inArea(int x, int y) {
  return x >= rect_.x && x <= (rect_.x + rect_.w) && y >= rect_.y &&
         y <= (rect_.y + rect_.h);
}

void GameSession::sendCtrl(const char* event, const char* param) {
  char msg[256];
  snprintf(msg, 256,
           "{\"type\": \"control\", \"data\": { \"event\": \"%s\", "
           "\"parameters\": %s }}",
           event, param);
  pc_->Send(p2p_params_->server_id, msg, nullptr, nullptr);
}

bool GameSession::dispatchEvent(SDL_MouseMotionEvent& e) {
  bool dispatch = inArea(e.x, e.y);
  if (dispatch) {
    char param[64];
    int fixedx = (e.x - rect_.x) * 32767 / rect_.w;
    int fixedy = (e.y - rect_.y) * 32767 / rect_.h;
    snprintf(param, 64,
             "{\"x\": %d, \"y\": %d, \"movementX\": %d, \"movementY\": %d }",
             fixedx, fixedy, e.xrel, e.yrel);
    sendCtrl("mousemove", param);
  }
  return dispatch;
}

bool GameSession::dispatchEvent(SDL_MouseButtonEvent& e) {
  bool dispatch = inArea(e.x, e.y);
  if (dispatch) {
    char param[64];
    const char* et = (e.type == SDL_MOUSEBUTTONDOWN) ? "mousedown" : "mouseup";
    int fixedx = (e.x - rect_.x) * 32767 / rect_.w;
    int fixedy = (e.y - rect_.y) * 32767 / rect_.h;
    snprintf(param, 64, "{\"which\": %d, \"x\": %d, \"y\": %d }", e.which,
             fixedx, fixedy);
    sendCtrl(et, param);
  }
  return dispatch;
}

const std::string& GameSession::getSessionId() {
  return p2p_params_->server_id;
}

void GameSession::freeSession() {
  pc_->Stop(p2p_params_->server_id, nullptr, nullptr);
  pc_->RemoveObserver(*ob_);
  pc_->Disconnect(nullptr, nullptr);
}

GameSession::~GameSession() {}
