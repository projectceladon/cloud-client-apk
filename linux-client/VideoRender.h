#ifndef _VIDEO_RENDER_H
#define _VIDEO_RENDER_H

#include <string>
#include <iostream>
#include <unistd.h>
#include <memory>
#include <SDL2/SDL.h>

class VideoRenderer {
public:
  VideoRenderer(SDL_Window *w, int width, int height, Uint32 format);
  virtual ~VideoRenderer();

  void RenderFrame(unsigned char* buffer, int size);

private:
  void init();
 
private:
  SDL_Window* win_ = nullptr;
  SDL_Renderer* renderer_ = nullptr;
  SDL_Texture* texture_ = nullptr;
  int width_ = 0;
  int height_ = 0;
  Uint32 format_ = SDL_PIXELFORMAT_YV12;
};

#endif
