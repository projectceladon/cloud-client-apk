#include "EncodedVideoDispatcher.h"

EncodedVideoDispatcher::EncodedVideoDispatcher(VideoFrameCallback callback) {
  callback_ = callback;
}

bool EncodedVideoDispatcher::OnEncodedFrame(std::unique_ptr<VideoEncodedFrame> frame) {
  if (callback_) {
    callback_(std::move(frame));
  }
  return true;
}

int EncodedVideoDispatcher::Write(int vhal_sock, const uint8_t* data, size_t size) {
  return 0;
}
