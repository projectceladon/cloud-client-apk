#include "VideoRender.h"

VideoRenderer::VideoRenderer(SDL_Window* w, int width, int height)
 : win_(w), width_(width), height_(height) {
  std::cout << __func__ << ":" << std::endl;
  init();
}

VideoRenderer::~VideoRenderer() {
  std::cout << __func__ << ":" << std::endl;
  if (renderer_) {
    SDL_DestroyTexture(texture_);
    SDL_DestroyRenderer(renderer_);
  }
}

void VideoRenderer::init() {
  std::cout << __func__ << ":" << std::endl;
  renderer_ = SDL_CreateRenderer(win_, -1, 0);
  if (!renderer_) {
    std::cout << "Failed to create renderer" << std::endl;
    return;
  }
  texture_ = SDL_CreateTexture(renderer_, SDL_PIXELFORMAT_NV12, SDL_TEXTUREACCESS_STREAMING, width_, height_);
}

void VideoRenderer::RenderFrame(unsigned char* buffer, int size) {
  if (!renderer_)
    return;

  if (buffer) {
    SDL_UpdateTexture(texture_, nullptr, buffer, width_);
  } else {
    SDL_UpdateTexture(texture_, nullptr, buffer, width_ * 4);
  }

  SDL_RenderCopy(renderer_, texture_, NULL, NULL);
  SDL_RenderPresent(renderer_);
}
