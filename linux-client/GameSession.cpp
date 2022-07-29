#include "GameSession.h"

#include "nlohmann/json.hpp"

using json = nlohmann::json;

GameSession::GameSession(std::unique_ptr<GameP2PParams> p2p_params,
                         SDL_Renderer* sdlRenderer, RenderParams* render_params,
                         TTF_Font* font, bool render, bool play_audio,
                         pthread_rwlock_t* lock) {
  p2p_params_ = std::move(p2p_params);
  renderer_ = sdlRenderer;
  suspend_ = !render;
  font_ = font;
  session_desc_ = p2p_params_->server_id;
  video_width_ = p2p_params_->video_width;
  video_height_ = p2p_params_->video_height;
  initP2P();
  video_renderer_ = std::make_shared<VideoRenderer>(this);
  if (play_audio) {
    std::cout << "play_audio " << play_audio << std::endl;
    audio_player_ = std::make_shared<AudioPlayer>();
  }

  if (TTF_SizeText(font, session_desc_.c_str(), &text_rect_.w, &text_rect_.h)) {
    std::cout << "GameSession get textsize error" << std::endl;
    text_rect_.h = 30;
    text_rect_.w = 30;
  }
  render_rect_.x = 0;
  render_rect_.y = 0;
  if (render) {
    setupRenderEnv(render_params);
  }
  render_lock_ = lock;
}

void GameSession::setupRenderEnv(RenderParams* render_params) {
  rect_.x = render_params->left;
  rect_.y = render_params->top;
  rect_.w = render_params->width;
  rect_.h = render_params->height;
  text_rect_.y = rect_.y + render_params->height / 2 - text_rect_.h / 2;
  text_rect_.x = rect_.x + render_params->width / 2 - text_rect_.w / 2;
  // texture_ = render_params -> texture;
  render_params_ = render_params;
  texture_ = SDL_CreateTexture(renderer_, render_params_->format,
                               SDL_TEXTUREACCESS_STREAMING, video_width_,
                               video_height_);
  if (text_surface_ == nullptr) {
    SDL_Color textColor = {255, 0, 0};
    text_surface_ =
        TTF_RenderText_Blended(font_, session_desc_.c_str(), textColor);
    text_texture_ = SDL_CreateTextureFromSurface(renderer_, text_surface_);
  }
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

  sc_ = std::make_shared<OwtSignalingChannel>();
  pc_ = std::make_shared<P2PClient>(configuration, sc_);
  // ouF.open("./me.yuv", std::ofstream::binary| std::ofstream::out);
}

void GameSession::startSession() {
  ob_ = std::make_unique<PcObserver>(video_renderer_, audio_player_);
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

void GameSession::onFrame(
    std::unique_ptr<owt::base::VideoBuffer> video_buffer) {
  // std::lock_guard<std::mutex> lock(m_lock);^M
  //  video_buffer_ = std::move(video_buffer);^M
  /*test++;
  if (test == 100) {
    json j;
    j["type"] = "control";
    json jdata;
    jdata["event"] = "sizechange";
    json jparams;
    jparams["rendererSize"] = {{"width", rect_.w}, {"height", rect_.h}};
    //jparams["rendererSize"] = {{"width", 1280}, {"height", 720}};
    jparams["mode"] = "stretch";
    jdata["parameters"] = jparams;
    j["data"] = jdata;
    std::string jsonstr = j.dump();
    pc_->Send(p2p_params_ -> server_id, jsonstr.c_str(), nullptr, nullptr);
    std::cout << "send: " << jsonstr <<std::endl;
  } else if (test == 200) {
    json j;
    j["type"] = "control";
    json jdata;
    jdata["event"] = "sizechange";
    json jparams;
    //jparams["rendererSize"] = {{"width", rect_.w}, {"height", rect_.h}};
    jparams["rendererSize"] = {{"width", 1280}, {"height", 720}};
    jparams["mode"] = "stretch";
    jdata["parameters"] = jparams;
    j["data"] = jdata;
    std::string jsonstr = j.dump();
    pc_->Send(p2p_params_ -> server_id, jsonstr.c_str(), nullptr, nullptr);
    std::cout << "send: " << jsonstr <<std::endl;
  }*/
  if (p2p_params_->log) {
    render_start_time = SDL_GetTicks();
  }
  video_buffer_ = move(video_buffer);
  if (video_buffer_) {
    uint8_t* buffer = static_cast<uint8_t*>(video_buffer_->buffer);
    if (buffer) {
      frame_width_ = video_buffer_->resolution.width;
      frame_height_ = video_buffer_->resolution.height;
      // std::cout << "onFrame " << frame_width_ << "-" << frame_height_ <<
      // std::endl; memcpy(data, buffer, frame_width_ * frame_height_ * 3 / 2);
      // ouF.write((const char *)buffer, frame_width_ * frame_height_ * 3/2);
      render_rect_.w = frame_width_;
      render_rect_.h = frame_height_;
      pthread_rwlock_wrlock(render_lock_);
      if (p2p_params_->log) {
        render_lock_time = SDL_GetTicks();
      }
      if (frame_width_ != video_width_ || frame_height_ != video_height_) {
        SDL_DestroyTexture(texture_);
        texture_ = SDL_CreateTexture(renderer_, render_params_->format,
                                     SDL_TEXTUREACCESS_STREAMING, frame_width_,
                                     frame_height_);
        std::cout << "video_size change from  " << video_width_ << " - "
                  << video_height_ << " to " << frame_width_ << "-"
                  << frame_height_ << std::endl;
        video_width_ = frame_width_;
        video_height_ = frame_height_;
      }
      SDL_SetRenderTarget(renderer_, texture_);
      SDL_UpdateTexture(texture_, &render_rect_, buffer, frame_width_);
      pthread_rwlock_unlock(render_lock_);
    }
  }
  render_finish_time = SDL_GetTicks();
  if (frameCount % 30 == 0) {
    if (last_fps_time != 0) {
      std::cout << render_finish_time << " fps:" << p2p_params_->server_id
                << " fps: " << 30000 / (render_finish_time - last_fps_time)
                << std::endl;
    }
    last_fps_time = render_finish_time;
  }
  frameCount++;
  if (p2p_params_->log) {
    std::cout << p2p_params_->server_id << " onFrame cost " << render_start_time
              << "-" << render_lock_time << "-" << render_finish_time
              << ", totally " << render_finish_time - render_start_time
              << std::endl;
  }
}

void GameSession::copyFrame() {
  if (!suspend_) {
    SDL_RenderCopy(renderer_, texture_, &render_rect_, &rect_);  // &this->rect
    SDL_RenderCopy(renderer_, text_texture_, NULL, &text_rect_);
  }
}

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

void GameSession::freeSession() {
  pc_->Stop(p2p_params_->server_id, nullptr, nullptr);
  pc_->RemoveObserver(*ob_);
  pc_->Disconnect(nullptr, nullptr);
  video_renderer_->reset();
  SDL_DestroyTexture(texture_);
  SDL_DestroyTexture(text_texture_);
  SDL_FreeSurface(text_surface_);
}

GameSession::~GameSession() {}
