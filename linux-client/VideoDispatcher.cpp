#include "VideoDispatcher.h"

VideoDispatcher::VideoDispatcher(VideoFrameCallback callback) {
  callback_ = callback;
}

bool VideoDispatcher::OnEncodedFrame(
    std::unique_ptr<VideoEncodedFrame> frame) {
  if (callback_) {
    callback_(identifier_, std::move(frame));
  }
  return true;
}

uint8_t* VideoDispatcher::getDecodedFrame(int* frame_width,
                                                 int* frame_height) {
  return nullptr;
}

int VideoDispatcher::Write(int vhal_sock, const uint8_t* data,
                                  size_t size) {
  return 0;
}
