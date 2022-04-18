#include "VideoRender.h"

VideoRenderer::VideoRenderer() {
}

VideoRenderer::~VideoRenderer() {
}

void VideoRenderer::RenderFrame(std::unique_ptr<owt::base::VideoBuffer> video_buffer) {
  std::lock_guard<std::mutex> lock(m_lock);
  video_buffer_ = std::move(video_buffer);
}

std::unique_ptr<owt::base::VideoBuffer> VideoRenderer::getFrame() {
  std::lock_guard<std::mutex> lock(m_lock);
  std::unique_ptr<owt::base::VideoBuffer> buffer = std::move(video_buffer_);
  return buffer;
}
