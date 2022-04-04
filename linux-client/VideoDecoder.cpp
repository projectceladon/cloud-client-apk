#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#include <iostream>
#include "VideoDecoder.h"

enum AVPixelFormat get_hw_format(AVCodecContext *ctx, const enum AVPixelFormat *pix_fmts) {
  return AV_PIX_FMT_VAAPI;
}

VideoDecoder::VideoDecoder(std::shared_ptr<VideoDecoderListener> listener)
 : mListener(listener) {
  std::cout << "using VA-API device: " << DRM_NODE << std::endl;
  mDrmFd = open(DRM_NODE, O_RDWR);
  if (mDrmFd < 0) {
    std::cerr << "open dri render node failed!" << std::endl;
  }

  mVADisplay = vaGetDisplayDRM(mDrmFd);
  if (!mVADisplay) {
    std::cerr << "vaGetDisplay failed!" << std::endl;
  }

  int major;
  int minor;
  if (vaInitialize(mVADisplay, &major, &minor) != VA_STATUS_SUCCESS) {
    std::cerr << "vaInitialize failed!" << std::endl;
  }

  mListener->setVADisplay(mVADisplay);
}

VideoDecoder::~VideoDecoder() {
  avcodec_free_context(&mCodecContext);
  av_buffer_unref(&mHWDeviceCtx);

  if (mDrmFd >= 0) {
    close(mDrmFd);
  }

  vaTerminate(mVADisplay);
}

int VideoDecoder::initDecoder(uint32_t codec_type) {
  AVCodecID codec_id = (codec_type == int(VideoCodecType::kH265))
                       ? AV_CODEC_ID_H265
                       : AV_CODEC_ID_H264;

  mCodec = avcodec_find_decoder(codec_id);
  mCodecContext = avcodec_alloc_context3(mCodec);
  if (!mCodecContext) {
    std::cerr << "avcodec_alloc_context3 failed!" << std::endl;
    return -1;
  }

  mHWDeviceCtx = av_hwdevice_ctx_alloc(AV_HWDEVICE_TYPE_VAAPI);
  if (!mHWDeviceCtx) {
    std::cerr << "av_hwdevice_ctx_alloc failed!" << std::endl;
    return -1;
  }

  AVHWDeviceContext *hwctx = (AVHWDeviceContext *) mHWDeviceCtx->data;
  AVVAAPIDeviceContext *vactx = (AVVAAPIDeviceContext *)hwctx->hwctx;
  vactx->display = mVADisplay;
  if (av_hwdevice_ctx_init(mHWDeviceCtx) < 0) {
    std::cerr << "av_hwdevice_ctx_init failed!" << std::endl;
    return -1;
  }

  mCodecContext->get_format = get_hw_format;
  mCodecContext->hw_device_ctx = av_buffer_ref(mHWDeviceCtx);
  if (avcodec_open2(mCodecContext, mCodec, NULL) < 0) {
    std::cerr << "avcodec_open2 failed!" << std::endl;
    return -1;
  }
  return 0;
}

int VideoDecoder::decode(AVPacket *pkt) {
  int sent = avcodec_send_packet(mCodecContext, pkt);
  if (sent < 0) {
    std::cerr << "avcodec_send_packet failed!" << std::endl;
    return sent;
  }

  int decode_stat = 0;
  AVFrame *frame = av_frame_alloc();

  while (decode_stat >= 0) {
    decode_stat = avcodec_receive_frame(mCodecContext, frame);
    if (decode_stat == AVERROR(EAGAIN) || decode_stat == AVERROR_EOF) {
      break;
    } else if (decode_stat < 0) {
      std::cerr << "Error during decoding!" << std::endl;
      break;
    }

    if (mListener) {
      VASurfaceID va_surface = (uintptr_t)frame->data[3];
      mListener->OnFrame(va_surface);
    }
  }

  av_frame_free(&frame);
  return 0;
}
