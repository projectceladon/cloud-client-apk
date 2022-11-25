#include "CGCodec.h"

#include <fstream>
#include <mutex>
#include <vector>

#define MAX_DEVICE_NAME_SIZE 21
#define MAX_ALLOWED_PENDING_FRAMES 2

using namespace std;

//////// @class CGVideoFrame ////////

// std::ofstream ouF;

CGPixelFormat CGVideoFrame::format() {
  switch (m_avframe->format) {
    case AV_PIX_FMT_NV12:
      return CGPixelFormat::NV12;
    case AV_PIX_FMT_YUV420P:
    default:
      return CGPixelFormat::I420;
  }
}

int CGVideoFrame::copy_to_buffer(uint8_t *out_buffer, int *size) {
  if (!out_buffer || !size) {
    std::cout << "Bad input parameter." << std::endl;
    return -1;
  }

  int buf_size = av_image_get_buffer_size(
      (AVPixelFormat)m_avframe->format, m_avframe->width, m_avframe->height, 1);

  int ret = av_image_copy_to_buffer(
      out_buffer, buf_size, (const uint8_t *const *)m_avframe->data,
      (const int *)m_avframe->linesize, (AVPixelFormat)m_avframe->format,
      m_avframe->width, m_avframe->height, 1);
  if (ret < 0) {
    std::cout << "Can not copy image to buffer." << std::endl;
    return -1;
  }

  *size = buf_size;
  return 0;
}
//////// @class DecodeContext ////////

struct DecodeContext {
  DecodeContext(int codec_type, int resolution_type);
  virtual ~DecodeContext() = default;

  AVCodecParserContext *parser;
  AVCodecContext *avcodec_ctx;
  AVPacket *packet;

  std::mutex mutex_;
  std::vector<AVFrame *> decoded_frames;

  // parameters by configuration
  int codec_type;
  std::pair<int, int> resolution;
  int frame_size;
};

DecodeContext::DecodeContext(int codec_type, int resolution_type)
    : codec_type(codec_type) {
  parser = nullptr;
  avcodec_ctx = nullptr;
  packet = nullptr;
  if (resolution_type == int(FrameResolution::k480p)) {
    resolution = std::make_pair(640, 480);
  } else if (resolution_type == int(FrameResolution::k600p)) {
    resolution = std::make_pair(1024, 600);
  } else if (resolution_type == int(FrameResolution::k720p)) {
    resolution = std::make_pair(1280, 720);
  } else if (resolution_type == int(FrameResolution::k1080p)) {
    resolution = std::make_pair(1920, 1080);
  }
  frame_size = resolution.first * resolution.second / 2 * 3;
  std::cout << "Config decode type:" << codec_type
            << " width:" << resolution.first << " height:" << resolution.second
            << std::endl;
}

void DecodeContextDeleter::operator()(DecodeContext *p) { delete p; }

//////// @class HWAccelContext ////////

struct HWAccelContext {
  HWAccelContext(const AVCodec *decoder, AVCodecContext *avcodec_ctx,
                 const char *device_name, int extra_frames);
  virtual ~HWAccelContext();

  AVPixelFormat get_hw_pixel_format() { return m_hw_pix_fmt; }
  bool is_hw_accel_valid() { return m_hw_accel_valid; }

 private:
  AVPixelFormat m_hw_pix_fmt = AV_PIX_FMT_NONE;
  AVBufferRef *m_hw_dev_ctx = nullptr;

  bool m_hw_accel_valid = false;
};

static enum AVPixelFormat get_hw_format(AVCodecContext *ctx,
                                        const enum AVPixelFormat *pix_fmts) {
  const enum AVPixelFormat *p;
  HWAccelContext *hw_accel_ctx =
      reinterpret_cast<HWAccelContext *>(ctx->opaque);
  const enum AVPixelFormat hw_pix_fmt = hw_accel_ctx->get_hw_pixel_format();

  for (p = pix_fmts; *p != -1; p++) {
    if (*p == hw_pix_fmt) return *p;
  }

  std::cout << "Failed to get HW pixel format." << std::endl;
  return AV_PIX_FMT_NONE;
}

HWAccelContext::HWAccelContext(const AVCodec *decoder,
                               AVCodecContext *avcodec_ctx,
                               const char *device_name, int extra_frames) {
  const char *device_prefix = "/dev/dri/renderD";
  char device[MAX_DEVICE_NAME_SIZE] = {'\0'};
  // char prop_val[PROPERTY_VALUE_MAX] = {'\0'};

  if (!decoder || !avcodec_ctx || !device_name || extra_frames < 0) {
    std::cout << "Invalid parameters for hw accel context." << std::endl;
    return;
  }

  AVHWDeviceType type = av_hwdevice_find_type_by_name(device_name);
  if (type == AV_HWDEVICE_TYPE_NONE) {
    std::cout << "Device type " << device_name << " is not supported."
              << std::endl;
    return;
  }

  for (int i = 0;; i++) {
    const AVCodecHWConfig *config = avcodec_get_hw_config(decoder, i);
    if (config == nullptr) {
      std::cout << "Decoder " << decoder->name
                << " does not support device type "
                << av_hwdevice_get_type_name(type) << std::endl;
      return;
    }
    if (config->methods & AV_CODEC_HW_CONFIG_METHOD_HW_DEVICE_CTX &&
        config->device_type == type) {
      m_hw_pix_fmt = config->pix_fmt;
      break;
    }
  }

  avcodec_ctx->opaque = this;
  avcodec_ctx->get_format = get_hw_format;
  avcodec_ctx->thread_count = 1;  // FIXME: vaapi decoder multi thread issue
  avcodec_ctx->extra_hw_frames = extra_frames;
  avcodec_ctx->hwaccel_flags |= AV_HWACCEL_FLAG_ALLOW_PROFILE_MISMATCH;

  int count = snprintf(device, sizeof(device), "%s%d", device_prefix, 128);
  if (count < 0 || count > MAX_DEVICE_NAME_SIZE) {
    strncpy(device, "/dev/dri/renderD128", strlen("/dev/dri/renderD128"));
  }
  std::cout << __func__ << " - device: " << device << std::endl;
  if ((av_hwdevice_ctx_create(&m_hw_dev_ctx, type, device, NULL, 0)) < 0) {
    std::cout << "Failed to create specified HW device." << std::endl;
    return;
  }
  avcodec_ctx->hw_device_ctx = av_buffer_ref(m_hw_dev_ctx);

  m_hw_accel_valid = true;
}

HWAccelContext::~HWAccelContext() {
  av_buffer_unref(&m_hw_dev_ctx);
  m_hw_accel_valid = false;
}

void HWAccelContextDeleter::operator()(HWAccelContext *p) { delete p; }

//////// @class CGVideoDecoder ////////

CGVideoDecoder::~CGVideoDecoder() { destroy(); }

bool CGVideoDecoder::can_decode() const { return decoder_ready; }

int CGVideoDecoder::init_impl(FrameResolution resolution, uint32_t codec_type,
                              const char *device_name, int extra_hw_frames) {
  std::lock_guard<std::recursive_mutex> decode_push_lock(push_lock);
  std::lock_guard<std::recursive_mutex> decode_pull_lock(pull_lock);
  decoder_ready = false;
  // ouF.open("./raw.h264", std::ofstream::binary| std::ofstream::out);

  // Update current init parameters which would be used during re-init.
  this->codec_type = codec_type;
  this->resolution = resolution;
  this->device_name = device_name;

  m_decode_ctx =
      CGDecContex(new DecodeContext(int(codec_type), int(resolution)));

  AVCodecID codec_id = (codec_type == int(VideoCodecType::kH265))
                           ? AV_CODEC_ID_H265
                           : AV_CODEC_ID_H264;

  const AVCodec *codec = avcodec_find_decoder(codec_id);
  if (codec == nullptr) {
    std::cout << "Codec id: " << codec_id << " not found!" << std::endl;
    return -1;
  }

  AVCodecParserContext *parser = av_parser_init(codec->id);
  if (parser == nullptr) {
    std::cout << "Parser not found!" << std::endl;
    return -1;
  }

  AVCodecContext *c = avcodec_alloc_context3(codec);
  if (c == nullptr) {
    std::cout << "Could not allocate video codec context!" << std::endl;
    av_parser_close(parser);
    return -1;
  }

  if (device_name != nullptr) {
    m_hw_accel_ctx = CGHWAccelContex(
        new HWAccelContext(codec, c, device_name, extra_hw_frames));
    if (m_hw_accel_ctx->is_hw_accel_valid()) {
      std::cout << "Use device " << device_name << " to accelerate decoding!"
                << std::endl;
    } else {
      std::cout << "System doesn't support VAAPI(Video Acceleration API). "
                   "Please use SW Decoding."
                << std::endl;
      return -1;
    }
  } else {
    std::cout << "Use SW decoding!" << std::endl;
  }

  AVPacket *pkt = av_packet_alloc();
  if (pkt == nullptr) {
    std::cout << "Could not allocate packet!" << std::endl;
    av_parser_close(parser);
    avcodec_free_context(&c);
    return -1;
  }

  if (avcodec_open2(c, codec, NULL) < 0) {
    std::cout << "Could not open codec!" << std::endl;
    av_parser_close(parser);
    avcodec_free_context(&c);
    av_packet_free(&pkt);
    return -1;
  }

  m_decode_ctx->parser = parser;
  m_decode_ctx->avcodec_ctx = c;
  m_decode_ctx->packet = pkt;
  decoder_ready = true;
  return 0;
}

int CGVideoDecoder::init(FrameResolution resolution, uint32_t codec_type,
                         int *width, int *height, const char *device_name,
                         int extra_hw_frames) {
  init_impl(resolution, codec_type, device_name, extra_hw_frames);
  *width = m_decode_ctx->resolution.first;
  *height = m_decode_ctx->resolution.second;
  std::cout << "init end" << std::endl;
  return 0;
}

int CGVideoDecoder::decode(const uint8_t *data, int data_size, uint8_t *out_buf,
                           int *out_size, int *out_width, int *out_height) {
  // ouF.write((const char *) data, data_size);
  std::lock_guard<std::recursive_mutex> decode_access_lock(push_lock);
  if (!can_decode()) {
    std::cout << "Decoder not initialized!" << std::endl;
    return -1;
  }

  if (data == nullptr || data_size <= 0) {
    return -1;
  }

  AVPacket *pkt = m_decode_ctx->packet;
#if 0
  AVCodecParserContext *parser = m_decode_ctx->parser;
  while (data_size > 0) {
    int ret = av_parser_parse2(parser,
                               m_decode_ctx->avcodec_ctx,
                               &pkt->data,
                               &pkt->size,
                               data,
                               data_size,
                               AV_NOPTS_VALUE,
                               AV_NOPTS_VALUE,
                               0);
    if (ret < 0) {
      std::cout << __func__ << "Error while parsing" << std::endl;
      return -1;
    }

    data += ret;
    data_size -= ret;
    if (pkt->size) {
      if (decode_one_frame(pkt, out_buf, out_size) == AVERROR_INVALIDDATA) {
        std::cout << "re-init" << std::endl;
        flush_decoder();
        destroy();
        if (init_impl((FrameResolution)this->resolution,
                 this->codec_type,
                 this->device_name, 0) < 0) {
          std::cout << "re-init failed. "
                    << device_name << " decoding"
                    << std::endl;
          return -1;
        } else {
          pkt = m_decode_ctx->packet;
          parser = m_decode_ctx->parser;
          continue;
        }
      }
    }
  }
#else
  pkt->data = const_cast<uint8_t *>(data);
  pkt->size = data_size;
  if (pkt->size && decode_one_frame(pkt, out_buf, out_size, out_width,
                                    out_height) == AVERROR_INVALIDDATA) {
    std::cout << "re-init" << std::endl;
    flush_decoder();
    destroy();
    if (init_impl((FrameResolution)this->resolution, this->codec_type,
                  this->device_name, 0) < 0) {
      std::cout << "re-init failed. " << device_name << " decoding"
                << std::endl;
      return -1;
    }
  }
#endif

  return 0;
}

int CGVideoDecoder::decode_one_frame(const AVPacket *pkt, uint8_t *out_buf,
                                     int *out_size, int *out_width,
                                     int *out_height) {
  AVCodecContext *c = m_decode_ctx->avcodec_ctx;
  int sent = avcodec_send_packet(c, pkt);
  if (sent < 0) {
    std::cout << "Error sending a packet for decoding" << std::endl;
    return sent;
  }

  int decode_stat = 0;
  AVFrame *frame = nullptr;
  while (decode_stat >= 0) {
    if (frame == nullptr) {
      frame = av_frame_alloc();
      if (frame == nullptr) {
        std::cout << "Could not allocate video frame" << std::endl;
        return -1;
      }
    }
    decode_stat = avcodec_receive_frame(c, frame);
    if (decode_stat == AVERROR(EAGAIN) || decode_stat == AVERROR_EOF) {
      // std::cout << __func__ << " avcodec_receive_frame returned error" <<
      // std::endl;
      break;
    } else if (decode_stat < 0) {
      std::cout << "Error during decoding" << std::endl;
      av_frame_free(&frame);
      return -1;
    }

    if (frame->format != AV_PIX_FMT_YUV420P &&
        frame->format != AV_PIX_FMT_VAAPI) {
      if (frame->format != AV_PIX_FMT_YUV420P &&
          frame->format != AV_PIX_FMT_VAAPI)
        std::cout << "Input frame format " << frame->format
                  << " is not matching with Decoder format" << std::endl;

      av_frame_free(&frame);
      return -1;
    }

    if (m_hw_accel_ctx.get() && m_hw_accel_ctx->is_hw_accel_valid()) {
      AVFrame *sw_frame = av_frame_alloc();
      if (sw_frame == nullptr) {
        std::cout << "Could not allocate video frame" << std::endl;
        return -1;
      }

      if (frame->format != m_hw_accel_ctx->get_hw_pixel_format()) {
        std::cout << "Decoder HW format mismatch" << std::endl;
        return -1;
      }

      /* retrieve data from GPU to CPU */
      int ret = av_hwframe_transfer_data(sw_frame, frame, 0);
      if (ret < 0) {
        std::cout << "Error transferring the data to system memory"
                  << std::endl;
        return -1;
      }

      av_frame_free(&frame);
      frame = sw_frame;
    } else {
      std::cout << "Uses SW decoding" << std::endl;
    }

    // push decoded frame
    {
#if 0
      std::lock_guard<std::mutex> lock(m_decode_ctx->mutex_);
      m_decode_ctx->decoded_frames.push_back(frame);
      frame = nullptr;
#else
      int buf_size = av_image_get_buffer_size((AVPixelFormat)frame->format,
                                              frame->width, frame->height, 1);
      int ret = av_image_copy_to_buffer(
          out_buf,
          // m_decode_ctx->frame_size,
          buf_size, (const uint8_t *const *)frame->data,
          (const int *)frame->linesize, (AVPixelFormat)frame->format,
          // m_decode_ctx->resolution.first,
          frame->width,
          // m_decode_ctx->resolution.second,
          frame->height, 1);
      if (ret < 0) {
        std::cout << "Can not copy image to buffer" << std::endl;
        return -1;
      }
      *out_size = buf_size;
      *out_width = frame->width;
      *out_height = frame->height;
      std::cout << "frame_size" << frame->width << "---" << frame->height
                << std::endl;
      std::cout << "buffer_size" << buf_size << ", frame_size"
                << m_decode_ctx->frame_size << std::endl;
      // ouF.write((const char*)out_buf, buf_size);

      // untransfer, cause we can get it correctly
      /*if (!nv12buffer_.empty()) {
        // nv12toi420
        int step = m_decode_ctx->resolution.first *
      m_decode_ctx->resolution.second; memcpy(&nv12buffer_[0], out_buf, step * 3
      / 2); for(int i = 0,j = 0; i < step / 4; i++,j += 2)
        {
           memcpy(out_buf + step + i, &nv12buffer_[0] + step + j, 1);//u
           memcpy(out_buf + step + step / 4 +i, &nv12buffer_[0]+ step + j +
      1,1);// v
        }

        // i420tonv12
        memcpy(&nv12buffer_[0], out_buf, step * 3 / 2);//y
        for(int i = 0,j = 0;i<step/4;i++,j+=2)
        {
          memcpy(&nv12buffer_[0]+step+j,out_buf+step+i,1);//u
          memcpy(&nv12buffer_[0]+step+j+1,out_buf+step+i+step/4,1);//v
        }
        memcpy(out_buf, &nv12buffer_[0], step * 3 / 2);
      }*/

#endif
    }
  }

  av_frame_free(&frame);
  return 0;
}

int CGVideoDecoder::get_decoded_frame(CGVideoFrame::Ptr cg_frame) {
  std::lock_guard<std::recursive_mutex> decode_access_lock(pull_lock);
  if (!can_decode()) {
    std::cout << "Decoder not initialized" << std::endl;
    return -1;
  }
  std::lock_guard<std::mutex> lock(m_decode_ctx->mutex_);

  if (m_decode_ctx->decoded_frames.empty()) return -1;

  while (m_decode_ctx->decoded_frames.size() > MAX_ALLOWED_PENDING_FRAMES) {
    auto it = m_decode_ctx->decoded_frames.begin();
    AVFrame *frame = *it;
    av_frame_free(&frame);
    m_decode_ctx->decoded_frames.erase(it);
  }
  // return the frame in the front
  auto it = m_decode_ctx->decoded_frames.begin();
  AVFrame *frame = *it;
  cg_frame->ref_frame(frame);
  av_frame_free(&frame);
  m_decode_ctx->decoded_frames.erase(it);
  return 0;
}

/* flush the decoder */
int CGVideoDecoder::flush_decoder() {
  std::lock_guard<std::recursive_mutex> decode_push_lock(push_lock);
  AVCodecContext *c = m_decode_ctx->avcodec_ctx;
  AVPacket *packet = m_decode_ctx->packet;

  packet->data = NULL;
  packet->size = 0;
  packet->buf = NULL;
  packet->side_data = NULL;

  int sent = avcodec_send_packet(c, packet);
  if (sent < 0) {
    std::cout << "Error sending a flush packet to decoder" << std::endl;
    return -1;
  }
  std::cout << "Successfully sent flush packet to decoder" << std::endl;
  return 0;
}

int CGVideoDecoder::destroy() {
  std::cout << "CGVideoDecoder destroy·" << std::endl;
  std::lock_guard<std::recursive_mutex> decode_push_lock(push_lock);
  std::lock_guard<std::recursive_mutex> decode_pull_lock(pull_lock);

  if (decoder_ready) {
    av_parser_close(m_decode_ctx->parser);
    avcodec_free_context(&m_decode_ctx->avcodec_ctx);
    av_packet_free(&m_decode_ctx->packet);
    if (!m_decode_ctx->decoded_frames.empty()) {
      std::lock_guard<std::mutex> lock(m_decode_ctx->mutex_);
      for (auto frame : m_decode_ctx->decoded_frames) {
        av_frame_free(&frame);
      }
      m_decode_ctx->decoded_frames.clear();
    }
  }
  decoder_ready = false;
  m_hw_accel_ctx.reset();
  m_decode_ctx.reset();
  return 0;
}
