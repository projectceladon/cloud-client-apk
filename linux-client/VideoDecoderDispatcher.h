#ifndef VIDEO_DECODER_DISPATCHER_H
#define VIDEO_DECODER_DISPATCHER_H

#include "CGCodec.h"
#include "owt/base/videodecoderinterface.h"

using namespace owt::base;

class VideoDecoderDispatcher : public owt::base::VideoDecoderInterface {
 public:
  VideoDecoderDispatcher(CGCodecSettings settings);
  virtual ~VideoDecoderDispatcher() {}

  bool InitDecodeContext(VideoCodec video_codec, int* width, int* height,
                         const std::string& identifier) override;

  bool OnEncodedFrame(std::unique_ptr<VideoEncodedFrame> frame) override;

  uint8_t* getDecodedFrame(int* frame_width, int* frame_height) override;

  bool Release() override { return true; }

  VideoDecoderInterface* Copy() override {
    return new VideoDecoderDispatcher(codec_settings_);
  }

 private:
  int Write(int vhal_sock, const uint8_t* data, size_t size);

 private:
  VideoCodec video_codec_ = VideoCodec::kH264;
  std::shared_ptr<CGVideoDecoder> decoder_;
  CGCodecSettings codec_settings_;
  std::vector<uint8_t> buffer_;
  int out_size = 0;
  int frame_width_ = 0;
  int frame_height_ = 0;
  int count = 0;
  std::string identifier_;
};

#endif /* VIDEO_DECODER_DISPATCHER_H */
