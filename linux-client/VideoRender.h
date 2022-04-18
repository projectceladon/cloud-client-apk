#ifndef _VIDEO_RENDER_H
#define _VIDEO_RENDER_H

#include <string>
#include <iostream>
#include <unistd.h>
#include <memory>
#include <SDL2/SDL.h>
#include "owt/base/videorendererinterface.h"
#include <mutex>

class VideoRenderer : public owt::base::VideoRendererInterface{
public:
  VideoRenderer();
  virtual ~VideoRenderer();
  void RenderFrame(std::unique_ptr<owt::base::VideoBuffer> buffer)override;
  std::unique_ptr<owt::base::VideoBuffer> getFrame();
  owt::base::VideoRendererType Type() {
    return owt::base::VideoRendererType::kI420;
  }
private:
  void init();
 
private:
  /*SDL_Renderer* renderer_ = nullptr;
  SDL_Texture* texture_ = nullptr;
  int width_ = 0;
  int height_ = 0;
  Uint32 format_ = SDL_PIXELFORMAT_YV12;
  FILE *fid;
  int count = 0;*/
  std::unique_ptr<owt::base::VideoBuffer> video_buffer_;
  std::mutex m_lock;
};

#endif
