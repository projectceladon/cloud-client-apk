#ifndef _GAME_SESSION_H
#define _GAME_SESSION_H

#include <SDL2/SDL_ttf.h>
#include <pthread.h>

#include <fstream>
#include <iostream>
#include <mutex>

#include "AudioPlayer.h"
#include "VideoRender.h"
#include "PcObserver.h"
#include "owt/p2p/p2pclient.h"
#include "owt_signalingchannel.h"

struct GameP2PParams {
  std::string signaling_server_url;
  std::string server_id;
  std::string client_id;
  std::string server_ip;
  std::string video_codec;
  bool dr;
  int video_width;
  int video_height;
  bool log;
};

struct RenderParams {
  int left;
  int top;
  int width;
  int height;
};

class GameSession {
 public:
  GameSession(std::unique_ptr<GameP2PParams> p2p_params,
              RenderParams* render_params,
              bool render, bool play_audio);
  void setupRenderEnv(RenderParams* render_params);
  void startSession();
  // void renderFrame();
  void copyFrame();
  virtual ~GameSession();
  bool dispatchEvent(SDL_MouseMotionEvent& e);
  bool dispatchEvent(SDL_MouseButtonEvent& e);
  bool inArea(int x, int y);
  void initP2P();
  void sendCtrl(const char* event, const char* param);
  void suspendStream(bool suspend, RenderParams* render_params);
  void freeSession();
  const std::string& getSessionId();

 private:
  std::shared_ptr<OwtSignalingChannel> sc_;
  std::shared_ptr<P2PClient> pc_;
  std::unique_ptr<PcObserver> ob_;
  std::string session_desc_;
  std::unique_ptr<GameP2PParams> p2p_params_;
  std::shared_ptr<AudioPlayer> audio_player_;
  SDL_Rect rect_{};
  bool suspend_;
 // std::ofstream ouF;
};

#endif
