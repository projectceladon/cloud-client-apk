#ifndef VIDEO_DIRECT_RENDER_
#define VIDEO_DIRECT_RENDER_

#include <va/va_drmcommon.h>
#include <drm_fourcc.h>

#include <X11/Xlib.h>
#include <X11/Xatom.h>
#include <X11/Xutil.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>

#include <GL/gl.h>
#include <GL/glext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include "Common.h"

#define USE_CORE_PROFILE 1

class VideoDirectRender : public VideoDecoderListener {
public:
  VideoDirectRender();
  virtual ~VideoDirectRender();

  void setup_texture();
  int initRender(int window_width, int window_height);
  int renderFrame(VASurfaceID va_surface);
  int handleWindowEvents();

  void OnFrame(VASurfaceID va_surface) {
    renderFrame(va_surface);
  }

private:
  Display *mXDisplay;
  Window mWindow;
  Atom mWMDeleteWindow;

  EGLDisplay egl_display;
  EGLSurface egl_surface;
  EGLContext egl_context;

  PFNEGLCREATEIMAGEKHRPROC eglCreateImageKHR;
  PFNEGLDESTROYIMAGEKHRPROC eglDestroyImageKHR;
  PFNGLEGLIMAGETARGETTEXTURE2DOESPROC glEGLImageTargetTexture2DOES;
#if USE_CORE_PROFILE
  PFNGLGENVERTEXARRAYSPROC glGenVertexArrays;
  PFNGLBINDVERTEXARRAYPROC glBindVertexArray;
#endif

  GLuint prog;
  GLuint textures[2];

  bool texture_size_valid = false;
  float texcoord_x1 = 1.0f;
  float texcoord_y1 = 1.0f;
};

#endif
