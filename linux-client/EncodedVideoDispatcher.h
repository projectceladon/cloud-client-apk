#ifndef ENCODED_VIDEO_DISPATCHER_H
#define ENCODED_VIDEO_DISPATCHER_H

#include <functional>

#include "owt/base/videodecoderinterface.h"

using namespace owt::base;
using VideoFrameCallback =
    std::function<void(std::unique_ptr<VideoEncodedFrame> frame)>;

class EncodedVideoDispatcher : public owt::base::VideoDecoderInterface {
 public:
  EncodedVideoDispatcher(VideoFrameCallback callback);
  virtual ~EncodedVideoDispatcher() {}

  bool InitDecodeContext(VideoCodec video_codec, int* width, int* height,
                         const std::string& identifier) override {
    video_codec_ = video_codec;
    identifier_ = identifier;
    return true;
  }

  bool OnEncodedFrame(std::unique_ptr<VideoEncodedFrame> frame) override;

  uint8_t* getDecodedFrame(int* frame_width, int* frame_height) override;

  bool Release() override { return true; }

  VideoDecoderInterface* Copy() override {
    return new EncodedVideoDispatcher(callback_);
  }

 private:
  int Write(int vhal_sock, const uint8_t* data, size_t size);

 private:
  VideoCodec video_codec_ = VideoCodec::kH264;
  VideoFrameCallback callback_ = nullptr;
  std::string identifier_;
};

#endif /* ENCODED_VIDEO_DISPATCHER_H */
