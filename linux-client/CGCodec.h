#ifndef CG_CODEC_H
#define CG_CODEC_H

#include <stdint.h>
#include <stdlib.h>

#include <fstream>
#include <iostream>
#include <memory>
#include <mutex>

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/frame.h"
#include "libavutil/imgutils.h"
}

#include "Common.h"

struct CGCodecSettings {
  FrameResolution resolution;
  uint32_t codec_type;
  const char *device_name;
  int frame_size;
};

using namespace std;

class CGVideoFrame {
 public:
  using Ptr = std::shared_ptr<CGVideoFrame>;

  CGVideoFrame() { m_avframe = av_frame_alloc(); }
  virtual ~CGVideoFrame() { av_frame_free(&m_avframe); }

  int ref_frame(const AVFrame *frame) { return av_frame_ref(m_avframe, frame); }
  uint8_t *data(size_t plane) { return m_avframe->data[plane]; }
  int linesize(size_t plane) { return m_avframe->linesize[plane]; }
  int width() { return m_avframe->width; }
  int height() { return m_avframe->height; }
  AVFrame *av_frame() { return m_avframe; }
  CGPixelFormat format();
  int copy_to_buffer(uint8_t *buffer /* out */, int *size /* out */);

 private:
  AVFrame *m_avframe;
};

/*! @class: CGVideoDecoder wraps ffmpeg avcodec for video ES decoding */

struct DecodeContext;
struct DecodeContextDeleter {
  void operator()(DecodeContext *p);
};

struct HWAccelContext;
struct HWAccelContextDeleter {
  void operator()(HWAccelContext *p);
};

typedef std::unique_ptr<DecodeContext, DecodeContextDeleter> CGDecContex;
typedef std::unique_ptr<HWAccelContext, HWAccelContextDeleter> CGHWAccelContex;

/**
 * Required number of additionally allocated bytes at the end of the input
 * bitstream for decoding. This is mainly needed because some optimized
 * bitstream readers read 32 or 64 bit at once and could read over the end.<br>
 * Note: If the first 23 bits of the additional bytes are not 0, then damaged
 * MPEG bitstreams could cause overread and segfault.
 */
#define CG_INPUT_BUFFER_PADDING_SIZE 64

class CGVideoDecoder {
 public:
  CGVideoDecoder() {
    codec_type = int(VideoCodecType::kH265);
    resolution = FrameResolution::k720p;
    device_name = NULL;
  }

  virtual ~CGVideoDecoder();

  /**
   * Checks whether decoder init was successful. If true, then decoding
   * apis can be called, otherwise not.
   */
  bool can_decode() const;

  /**
   * Initialize the CGVideoDecoder
   * @param resolution_type   see @enum camera_video_resolution_t in @file
   * cg_protocol.h
   * @param device_name       the string of hardware acclerator device, such as
   * "vaapi"
   * @param extra_hw_frames   allocate extra frames for hardware acclerator when
   * decoding
   */
  int init(FrameResolution resolution, uint32_t codec_type, int *width,
           int *height, const char *device_name = nullptr,
           int extra_hw_frames = 0);
  /**
   * Send a piece of ES stream data to decoder, the data must have a padding
   * with a lengh of CG_INPUT_BUFFER_PADDING_SIZE
   * @param data      input buffer
   * @param length    buffer size in bytes without the padding. I.e. the full
   * buffer size is assumed to be buf_size + CG_INPUT_BUFFER_PADDING_SIZE.
   */
  int decode(const uint8_t *data, int length, uint8_t *out_buf, int *out_size,
             int *out_width, int *out_height);

  /**
   * Get one decoded video frame
   * @param cg_frame  a shared pointer @class CGVideoFrame which wrap ffmpeg
   * av_frame as the output
   */
  int get_decoded_frame(CGVideoFrame::Ptr cg_frame);

  /**
   * @brief Send flush packet to decoder, indicating end of decoding session.
   *
   * @return Returns 0 if decoder acknowledged Flush packet, non-zero if
   * errored.
   */
  int flush_decoder();

  /**
   * @brief Desotroy cg decoder context and ongoing decode requests.
   *
   * @return int
   */
  int destroy();

 private:
  CGDecContex m_decode_ctx;        ///<! cg decoder internal context
  CGHWAccelContex m_hw_accel_ctx;  ///<! hw decoding accelerator context
  int decode_one_frame(const AVPacket *pkt, uint8_t *out_buf, int *out_size,
                       int *out_width, int *out_height);
  int init_impl(FrameResolution resolution, uint32_t codec_type,
                const char *device_name = nullptr, int extra_hw_frames = 0);
  bool decoder_ready = false;
  std::recursive_mutex pull_lock;  // Guard m_decode_ctx at get_decoded_frame
  std::recursive_mutex
      push_lock;  // Guard m_decode_ctx at decode/decode_one_frame

  CGVideoDecoder(const CGVideoDecoder &cg_video_decoder);
  CGVideoDecoder &operator=(const CGVideoDecoder &) { return *this; }
  uint32_t codec_type;
  FrameResolution resolution;
  const char *device_name;
  // std::vector<uint8_t> nv12buffer_; no, need to transfer, cause webrtc won't
  // change
};

#endif  // CG_CODEC_H
