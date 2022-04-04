#ifndef VIDEO_DECODER_
#define VIDEO_DECODER_

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/hwcontext.h>
#include <libavutil/hwcontext_vaapi.h>
}

#include <va/va.h>
#include <va/va_drm.h>
#include <va/va_drmcommon.h>

#include <memory>

#include "Common.h"

class VideoDecoder {
public:
  VideoDecoder(std::shared_ptr<VideoDecoderListener> listener);
  virtual ~VideoDecoder();

  int initDecoder(uint32_t codec_type);
  int decode(AVPacket *pkt);

private:
  const char *DRM_NODE = "/dev/dri/renderD128";
  int mDrmFd = -1;
  VADisplay mVADisplay = 0;

  AVCodec *mCodec = nullptr;
  AVCodecContext *mCodecContext = nullptr;
  AVBufferRef *mHWDeviceCtx = nullptr;

  std::shared_ptr<VideoDecoderListener> mListener = nullptr;
};

#endif
