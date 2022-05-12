#include "VideoDecoderDispatcher.h"

 VideoDecoderDispatcher::VideoDecoderDispatcher(CGCodecSettings settings) {
  codec_settings_ = settings;
  decoder_ = std::make_shared<CGVideoDecoder>();
}

bool VideoDecoderDispatcher::InitDecodeContext(VideoCodec video_codec, int* width, int* height) {
  std::cout << "InitDecodeContext begin." << std::endl;
  video_codec_ = video_codec;
  if (decoder_->init(codec_settings_.resolution, codec_settings_.codec_type, width, height, codec_settings_.device_name, 0) < 0) {
    std::cout << "VideoDecoder init failed. " << std::endl;
    return false;
  } else {
    buffer_ = std::vector<uint8_t>(codec_settings_.frame_size);
    return true;
  }
}

bool VideoDecoderDispatcher::OnEncodedFrame(std::unique_ptr<VideoEncodedFrame> frame) {
  decoder_->decode(frame->buffer, (int)frame->length, &buffer_[0], &out_size, &frame_width_, &frame_height_);
  if (out_size > 0 && frame_width_ > 0 && frame_height_ > 0) {
    return true;
  } else {
    return false;
  }
}

uint8_t* VideoDecoderDispatcher::getDecodedFrame(int *frame_width, int *frame_height) {
  *frame_width = frame_width_;
  *frame_height = frame_height_;
  return &buffer_[0];
}

int VideoDecoderDispatcher::Write(int vhal_sock, const uint8_t* data, size_t size) {
  return 0;
}