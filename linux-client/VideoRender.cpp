#include "VideoRender.h"

VideoRenderer::VideoRenderer(SDL_Window* w, int width, int height, Uint32 format)
 : win_(w), width_(width), height_(height), format_(format) {
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
  texture_ = SDL_CreateTexture(renderer_, format_, SDL_TEXTUREACCESS_STREAMING, width_, height_);
}

void VideoRenderer::RenderFrame(unsigned char* buffer, int size) {
  if (!renderer_)
    return;

  if (buffer) {
    if (format_ == SDL_PIXELFORMAT_IYUV) {
      uint32_t w = width_;
      uint8_t *y = buffer;
      uint8_t *u = y + width_ * height_;
      uint8_t *v = u + width_ * height_ / 4;
      SDL_UpdateYUVTexture(texture_, nullptr, y, w, u, w / 2, v, w / 2);
    } else if (format_ == SDL_PIXELFORMAT_NV12) {
      SDL_UpdateTexture(texture_, nullptr, buffer, width_);
    } else {
      std::cout << "Cannot support this format!" << std::endl;
    }
  } else {
    SDL_UpdateTexture(texture_, nullptr, buffer, width_ * 4);
  }

  SDL_RenderCopy(renderer_, texture_, NULL, NULL);
  SDL_RenderPresent(renderer_);
}
