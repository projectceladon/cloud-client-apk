#ifndef COMMON_H_
#define COMMON_H_

#include <va/va.h>

enum CGPixelFormat {
  I420 = 0,
  NV12 = 1
};

enum class VideoCodecType {
  kH264 = 1,
  kH265 = 2,
  kAll = 3
};

enum class FrameResolution {
  k480p = 1,
  k600p = 2,
  k720p = 4,
  k1080p = 8,
  kAll = 15
};

#endif
