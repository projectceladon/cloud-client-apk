#ifndef _VIDEO_RENDER_H
#define _VIDEO_RENDER_H

#include <SDL2/SDL.h>
#include <unistd.h>

#include <iostream>
#include <memory>
#include <mutex>
#include <string>

#include "owt/base/videorendererinterface.h"

class VideoRendererListener {
 public:
  /// Passes video buffer to renderer.
  virtual ~VideoRendererListener() {}
  virtual void onFrame(
      std::unique_ptr<owt::base::VideoBuffer> video_buffer) = 0;
};

class VideoRenderer : public owt::base::VideoRendererInterface {
 public:
  VideoRenderer(VideoRendererListener* listener);
  virtual ~VideoRenderer();
  void RenderFrame(std::unique_ptr<owt::base::VideoBuffer> buffer) override;
  // std::unique_ptr<owt::base::VideoBuffer> getFrame();
  owt::base::VideoRendererType Type() {
    return owt::base::VideoRendererType::kI420;
  }
  void reset();

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
  // std::unique_ptr<owt::base::VideoBuffer> video_buffer_;
  VideoRendererListener* mListener = nullptr;
};

#endif
