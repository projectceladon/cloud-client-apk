#ifndef VIDEO_DECODER_DISPATCHER_H
#define VIDEO_DECODER_DISPATCHER_H

#include "owt/base/videodecoderinterface.h"
#include "CGCodec.h"

using namespace owt::base;

class  VideoDecoderDispatcher : public owt::base::VideoDecoderInterface {
public:
  VideoDecoderDispatcher(CGCodecSettings settings);
  virtual ~ VideoDecoderDispatcher() {}

  bool InitDecodeContext(VideoCodec video_codec) override;

  bool OnEncodedFrame(std::unique_ptr<VideoEncodedFrame> frame) override;

  uint8_t* getDecodedFrame() override;

  bool Release() override {
    return true;
  }

  VideoDecoderInterface* Copy() override {
    return new  VideoDecoderDispatcher(codec_settings_);
  }

private:
  int Write(int vhal_sock, const uint8_t* data, size_t size);

private:
  VideoCodec video_codec_ = VideoCodec::kH264;
  std::shared_ptr<CGVideoDecoder> decoder_;
  CGCodecSettings codec_settings_;
  std::vector<uint8_t> buffer_;
  int out_size;
  FILE *fid;
  int count = 0;
};

#endif /* VIDEO_DECODER_DISPATCHER_H */
