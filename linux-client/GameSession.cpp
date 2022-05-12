#include "GameSession.h"

GameSession::GameSession(GameP2PParams p2p_params, SDL_Renderer* sdlRenderer, RenderParams render_params, TTF_Font* font) {
    p2p_params_ = p2p_params;
    renderer_ = sdlRenderer;
    rect_.x = render_params.left;
	  rect_.y = render_params.top;
	  rect_.w = render_params.width;
	  rect_.h = render_params.height;
    text_rect_.y = rect_.y + render_params.height /2 - 15;
    text_rect_.h = 30;
    if (120 < render_params.width) {
      text_rect_.x = rect_.x + render_params.width / 2 -60;
      text_rect_.w = 120;
    } else {
      text_rect_.x = 0;
      text_rect_.w = render_params.width;
    }
    session_desc_ = p2p_params.server_ip + ":" + p2p_params.server_id;
    initP2P();
    video_width_ = render_params.video_width;
    video_height_ = render_params.video_height;
    texture_ = SDL_CreateTexture(renderer_, render_params.format, SDL_TEXTUREACCESS_STREAMING, video_width_, video_height_);
	  SDL_Color textColor = {255, 0, 0};
    text_surface_ = TTF_RenderText_Blended(font, session_desc_.c_str(), textColor);
    text_texture_ = SDL_CreateTextureFromSurface(renderer_, text_surface_);
    render_rect_.x = 0;
    render_rect_.y = 0;
}


void GameSession::initP2P() {
  P2PClientConfiguration configuration;
  IceServer stunServer, turnServer;
  std::string stunUrl = "stun:" + p2p_params_.server_ip + ":3478";
  stunServer.urls.push_back(stunUrl);
  stunServer.username = "username";
  stunServer.password = "password";
  configuration.ice_servers.push_back(stunServer);

  std::string turnUrlTcp = "turn:" + p2p_params_.server_ip + ":3478?transport=tcp";
  std::string turnUrlUdp = "turn:" + p2p_params_.server_ip + ":3478?transport=udp";
  turnServer.urls.push_back(turnUrlTcp);
  turnServer.urls.push_back(turnUrlUdp);
  turnServer.username = "username";
  turnServer.password = "password";
  configuration.ice_servers.push_back(turnServer);

  VideoCodecParameters videoParam;
  if (p2p_params_.video_codec == "h264") {
    videoParam.name = owt::base::VideoCodec::kH264;
  } else if (p2p_params_.video_codec == "h265") {
    videoParam.name = owt::base::VideoCodec::kH265;
  } else {
    std::cout << "Cannot support this codec!" << std::endl;
  }
  VideoEncodingParameters video_params(videoParam, 0, true);
  configuration.video_encodings.push_back(video_params);

  sc_ = std::make_shared<OwtSignalingChannel>();
  pc_ = std::make_shared<P2PClient>(configuration, sc_);
  video_renderer_ = std::make_shared<VideoRenderer>();
  audio_player_ = std::make_shared<AudioPlayer>();
}

void GameSession::startSession() {
  ob_ = std::make_unique<PcObserver>(video_renderer_, audio_player_);
  pc_->AddObserver(*ob_);
  pc_->AddAllowedRemoteId(p2p_params_.server_id);
  pc_->Connect(p2p_params_.signaling_server_url, p2p_params_.client_id, nullptr, nullptr);
  pc_->Send(p2p_params_.server_id, "start", nullptr, nullptr);
}

void GameSession::renderFrame() {
    std::unique_ptr<owt::base::VideoBuffer> video_buffer = video_renderer_ -> getFrame();
    if (video_buffer) {
      uint8_t* buffer = static_cast<uint8_t *>(video_buffer -> buffer);
      if (buffer) {
          frame_width_ = video_buffer ->resolution.width;
          frame_height_ = video_buffer ->resolution.height;
          uint32_t w = video_buffer ->resolution.width;
          uint8_t *y = buffer;
          uint8_t *u = y + frame_width_ * frame_height_;
          uint8_t *v = u + frame_width_ * frame_height_ / 4;
          render_rect_.w = frame_width_;
          render_rect_.h = frame_height_;
          SDL_UpdateTexture(texture_, &render_rect_, buffer, video_buffer ->resolution.width);
        }
    }
}

void GameSession::copyFrame() {
  SDL_RenderCopy(renderer_, texture_, &render_rect_, &rect_);// &this->rect
  SDL_RenderCopy(renderer_, text_texture_, NULL, &text_rect_);
}

bool GameSession::inArea(int x, int y) {
  return x >= rect_.x && x <= (rect_.x + rect_.w) && y >= rect_.y && y <= (rect_.y + rect_.h);
}

void GameSession::sendCtrl(const char* event, const char* param) {
  char msg[256];
  snprintf(msg, 256, "{\"type\": \"control\", \"data\": { \"event\": \"%s\", \"parameters\": %s }}", event, param);
  pc_->Send(p2p_params_.server_id, msg, nullptr, nullptr);
}

bool GameSession::dispatchEvent(SDL_MouseMotionEvent& e) {
  bool dispatch = inArea(e.x, e.y);
  if (dispatch) {
    char param[64];
    int fixedx = (e.x - rect_.x) * 32767 / rect_.w;
    int fixedy = (e.y - rect_.y) * 32767 / rect_.h;
    snprintf(param, 64, "{\"x\": %d, \"y\": %d, \"movementX\": %d, \"movementY\": %d }", fixedx, fixedy, e.xrel, e.yrel);
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
    snprintf(param, 64, "{\"which\": %d, \"x\": %d, \"y\": %d }", e.which, fixedx, fixedy);
    sendCtrl(et, param);
  }
  return dispatch;
}

GameSession::~GameSession() {
  pc_->Stop(p2p_params_.server_id, nullptr, nullptr);
  pc_->RemoveObserver(*ob_);
  pc_->Disconnect(nullptr, nullptr);

  SDL_DestroyTexture(text_texture_);
  SDL_FreeSurface(text_surface_);
  SDL_DestroyTexture(texture_);
}
