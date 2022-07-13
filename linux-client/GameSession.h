#ifndef _GAME_SESSION_H
#define _GAME_SESSION_H

#include "VideoRender.h"
#include "AudioPlayer.h"
#include "PcObserver.h"
#include "owt/p2p/p2pclient.h"
#include "owt_signalingchannel.h"
#include <SDL2/SDL_ttf.h>

#include <iostream>
#include <fstream>
#include <mutex>
#include<pthread.h>

struct GameP2PParams
{
  std::string signaling_server_url;
  std::string server_id;
  std::string client_id;
  std::string server_ip;
  std::string video_codec;
  bool dr;
  bool log;
};

struct RenderParams {
  int left;
  int top;
  int width;
  int height;
  int video_width;
  int video_height;
  Uint32 format;
  SDL_Texture* texture;
};


class GameSession : public VideoRendererListener {
public:
  GameSession(std::unique_ptr<GameP2PParams> p2p_params, SDL_Renderer* sdlRenderer, RenderParams* render_params, TTF_Font* font, bool render, bool play_audio, pthread_rwlock_t* lock);
  void setupRenderEnv(RenderParams* render_params);
  void startSession();
  //void renderFrame();
  void copyFrame();
  virtual ~GameSession();
  bool dispatchEvent(SDL_MouseMotionEvent& e);
  bool dispatchEvent(SDL_MouseButtonEvent& e);
  bool inArea(int x, int y);
  void initP2P();
  void sendCtrl(const char* event, const char* param);
  void suspendStream(bool suspend, RenderParams* render_params);
  void onFrame(std::unique_ptr<owt::base::VideoBuffer> video_buffer)override;
  void freeSession();
private:
  std::shared_ptr<OwtSignalingChannel> sc_;
  std::shared_ptr<P2PClient> pc_;
  std::unique_ptr<PcObserver> ob_;
  std::string session_desc_;
  std::unique_ptr<GameP2PParams> p2p_params_;
  RenderParams* render_params_;

  std::shared_ptr<VideoRenderer> video_renderer_;
  std::shared_ptr<AudioPlayer> audio_player_;
  SDL_Renderer* renderer_ = nullptr;
  SDL_Texture* texture_ = nullptr;
  TTF_Font* font_ = nullptr;
  SDL_Rect rect_;
  int video_width_;
  int video_height_;
  SDL_Texture *text_texture_;
  SDL_Rect text_rect_;
  SDL_Surface *text_surface_ = nullptr;
  SDL_Rect render_rect_;
  int frame_width_;
  int frame_height_;
  bool suspend_;
  pthread_rwlock_t* render_lock_;
  std::unique_ptr<owt::base::VideoBuffer> video_buffer_;
  std::ofstream ouF;
  Uint32 render_start_time;
  Uint32 render_lock_time;
  Uint32 render_finish_time;
};

#endif
