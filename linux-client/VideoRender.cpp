#include "VideoRender.h"

VideoRenderer::VideoRenderer(VideoRendererListener* listener):mListener(listener) {
}

VideoRenderer::~VideoRenderer() {
}

void VideoRenderer::RenderFrame(std::unique_ptr<owt::base::VideoBuffer> video_buffer) {
  //std::lock_guard<std::mutex> lock(m_lock);
  //video_buffer_ = std::move(video_buffer);
  if (mListener) {
    mListener -> onFrame(std::move(video_buffer));
  }
}

void VideoRenderer::reset() {
  mListener = nullptr;
}

/*std::unique_ptr<owt::base::VideoBuffer> VideoRenderer::getFrame() {
  std::lock_guard<std::mutex> lock(m_lock);
  std::unique_ptr<owt::base::VideoBuffer> buffer = std::move(video_buffer_);
  return buffer;
}*/
