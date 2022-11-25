#include "VideoDirectRender.h"

#include <stdio.h>
#include <unistd.h>

#include <iostream>

#define USE_LAYERS 0
#define REUSE_TEXTURES 1
#define SWAP_INTERVAL 2

#define CORE_PROFILE_MAJOR_VERSION 3
#define CORE_PROFILE_MINOR_VERSION 3
#define COMP_PROFILE_MAJOR_VERSION 3
#define COMP_PROFILE_MINOR_VERSION 0

#define LOOKUP_FUNCTION(type, func)           \
  func = (type)eglGetProcAddress(#func);      \
  if (!func) {                                \
    printf("eglGetProcAddress(" #func ")\n"); \
  }

#define DECLARE_YUV2RGB_MATRIX_GLSL                   \
  "const mat4 yuv2rgb = mat4(\n"                      \
  "    vec4(  1.1644,  1.1644,  1.1644,  0.0000 ),\n" \
  "    vec4(  0.0000, -0.2132,  2.1124,  0.0000 ),\n" \
  "    vec4(  1.7927, -0.5329,  0.0000,  0.0000 ),\n" \
  "    vec4( -0.9729,  0.3015, -1.1334,  1.0000 ));"

#if USE_CORE_PROFILE
static const char *vs_src =
    "#version 130"
    "\n"
    "const vec2 coords[4] = vec2[]( vec2(0.,0.), vec2(1.,0.), vec2(0.,1.), "
    "vec2(1.,1.) );"
    "\n"
    "uniform vec2 uTexCoordScale;"
    "\n"
    "uniform vec2 offset;"
    "\n"
    "out vec2 vTexCoord;"
    "\n"
    "void main() {"
    "\n"
    "    vec2 c = coords[gl_VertexID];"
    "\n"
    "    vTexCoord = c;"
    "\n"
    "    gl_Position = vec4((c * vec2(2.,-2.) + vec2(-1.,1.)) / uTexCoordScale + offset, 0., 1.);"
    "\n"
    "}";

static const char *fs_src =
    "#version 130"
    "\n"
    "in vec2 vTexCoord;"
    "\n"
    "uniform sampler2D uTexY, uTexC;"
    "\n" DECLARE_YUV2RGB_MATRIX_GLSL
    "\n"
    "out vec4 oColor;"
    "\n"
    "void main() {"
    "\n"
    "    oColor = yuv2rgb * vec4(texture(uTexY, vTexCoord).x, "
    "texture(uTexC, vTexCoord).xy, 1.);"
    "\n"
    "}";
  
#else
static const char *vs_src =
    "void main() {"
    "\n"
    "    gl_Position = ftransform();"
    "\n"
    "    gl_TexCoord[0] = gl_MultiTexCoord0;"
    "\n"
    "}";

static const char *fs_src =
    "uniform sampler2D uTexY, uTexC;"
    "\n" DECLARE_YUV2RGB_MATRIX_GLSL
    "\n"
    "void main() {"
    "\n"
    "    gl_FragColor = yuv2rgb * vec4(texture2D(uTexY, gl_TexCoord[0].xy).x, "
    "texture2D(uTexC, gl_TexCoord[0].xy).xy, 1.);"
    "\n"
    "}";
#endif

VideoDirectRender::~VideoDirectRender() {
  std::cout << "~VideoDirectRender()" << std::endl;
  eglMakeCurrent(egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
  eglDestroyContext(egl_display, egl_context);
  eglDestroySurface(egl_display, egl_surface);
  eglTerminate(egl_display);
  XDestroyWindow(mXDisplay, mWindow);
  XCloseDisplay(mXDisplay);
}

void VideoDirectRender::setup_texture() {
  /*glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);*/
}

int VideoDirectRender::initRender(int window_width, int window_height) {
  mXDisplay = XOpenDisplay(NULL);
  if (!mXDisplay) {
    std::cerr << "XOpenDisplay failed!" << std::endl;
    return -1;
  }

  XSetWindowAttributes xattr;
  xattr.override_redirect = False;
  xattr.border_pixel = 0;
  mWindow =
      XCreateWindow(mXDisplay, DefaultRootWindow(mXDisplay), 0, 0, window_width,
                    window_height, 0, CopyFromParent, InputOutput,
                    CopyFromParent, CWOverrideRedirect | CWBorderPixel, &xattr);
  if (!mWindow) {
    std::cerr << "XCreateWindow failed!" << std::endl;
    return -1;
  }

  XStoreName(mXDisplay, mWindow, "AIC Linux Native Client");
  XMapWindow(mXDisplay, mWindow);
  XSelectInput(mXDisplay, mWindow,
               ExposureMask | StructureNotifyMask | KeyPressMask |
                   ButtonPressMask | ButtonReleaseMask | PointerMotionMask);
  mWMDeleteWindow = XInternAtom(mXDisplay, "WM_DELETE_WINDOW", True);
  XSetWMProtocols(mXDisplay, mWindow, &mWMDeleteWindow, 1);

  egl_display = eglGetDisplay((EGLNativeDisplayType)mXDisplay);
  if (egl_display == EGL_NO_DISPLAY) {
    std::cerr << "eglGetDisplay failed!" << std::endl;
    return -1;
  }
  if (!eglInitialize(egl_display, NULL, NULL)) {
    std::cerr << "eglInitialize failed!" << std::endl;
    return -1;
  }
  if (!eglBindAPI(EGL_OPENGL_API)) {
    std::cerr << "eglBindAPI failed!" << std::endl;
    return -1;
  }

  EGLint visual_attr[] = {EGL_SURFACE_TYPE,
                          EGL_WINDOW_BIT,
                          EGL_RED_SIZE,
                          8,
                          EGL_GREEN_SIZE,
                          8,
                          EGL_BLUE_SIZE,
                          8,
                          EGL_ALPHA_SIZE,
                          8,
                          EGL_RENDERABLE_TYPE,
                          EGL_OPENGL_BIT,
                          EGL_NONE};

  EGLConfig cfg{};
  EGLint cfg_count = 0;
  if (!eglChooseConfig(egl_display, visual_attr, &cfg, 1, &cfg_count) ||
      (cfg_count < 1)) {
    std::cerr << "eglChooseConfig failed!" << std::endl;
    return -1;
  }

  egl_surface = eglCreateWindowSurface(egl_display, cfg, mWindow, NULL);
  if (egl_surface == EGL_NO_SURFACE) {
    std::cerr << "eglCreateWindowSurface failed!" << std::endl;
    return -1;
  }

  EGLint ctx_attr[] = {
    EGL_CONTEXT_OPENGL_PROFILE_MASK,
#if USE_CORE_PROFILE & 1
    EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT,
    EGL_CONTEXT_MAJOR_VERSION,
    CORE_PROFILE_MAJOR_VERSION,
    EGL_CONTEXT_MINOR_VERSION,
    CORE_PROFILE_MINOR_VERSION,
#else
    EGL_CONTEXT_OPENGL_COMPATIBILITY_PROFILE_BIT,
    EGL_CONTEXT_MAJOR_VERSION,
    COMP_PROFILE_MAJOR_VERSION,
    EGL_CONTEXT_MINOR_VERSION,
    COMP_PROFILE_MINOR_VERSION,
#endif
    EGL_NONE
  };
  egl_context = eglCreateContext(egl_display, cfg, EGL_NO_CONTEXT, ctx_attr);
  if (egl_context == EGL_NO_CONTEXT) {
    std::cerr << "eglCreateContext failed!" << std::endl;
    return -1;
  }

  eglMakeCurrent(egl_display, egl_surface, egl_surface, egl_context);
  eglSwapInterval(egl_display, SWAP_INTERVAL);

  std::cout << "OpenGL vendor:   " << glGetString(GL_VENDOR) << std::endl;
  std::cout << "OpenGL renderer: " << glGetString(GL_RENDERER) << std::endl;
  std::cout << "OpenGL version:  " << glGetString(GL_VERSION) << std::endl;

  LOOKUP_FUNCTION(PFNEGLCREATEIMAGEKHRPROC, eglCreateImageKHR)
  LOOKUP_FUNCTION(PFNEGLDESTROYIMAGEKHRPROC, eglDestroyImageKHR)
  LOOKUP_FUNCTION(PFNGLEGLIMAGETARGETTEXTURE2DOESPROC,
                  glEGLImageTargetTexture2DOES)
#if USE_CORE_PROFILE
  LOOKUP_FUNCTION(PFNGLGENVERTEXARRAYSPROC, glGenVertexArrays);
  LOOKUP_FUNCTION(PFNGLBINDVERTEXARRAYPROC, glBindVertexArray);
#endif

#if USE_CORE_PROFILE
  GLuint vao = 0;
  glGenVertexArrays(1, &vao);
  glBindVertexArray(vao);
#else
  glOrtho(0.0, 1.0, 1.0, 0.0, -1.0, 1.0);
#endif

  prog = glCreateProgram();
  GLuint vs = glCreateShader(GL_VERTEX_SHADER);
  GLuint fs = glCreateShader(GL_FRAGMENT_SHADER);
  if (!prog) {
    std::cerr << "glCreateProgram failed!" << std::endl;
    return -1;
  }
  if (!vs || !fs) {
    std::cerr << "glCreateShader failed!" << std::endl;
    return -1;
  }
  glShaderSource(vs, 1, &vs_src, NULL);
  glShaderSource(fs, 1, &fs_src, NULL);

  while (glGetError()) {
  }
  glCompileShader(vs);

  GLint ok = GL_FALSE;
  glGetShaderiv(vs, GL_COMPILE_STATUS, &ok);

  if (glGetError() || (ok != GL_TRUE)) {
    std::cerr << "glCompileShader GL_VERTEX_SHADER failed!" << std::endl;
    return -1;
  }
  glCompileShader(fs);
  glGetShaderiv(fs, GL_COMPILE_STATUS, &ok);
  if (glGetError() || (ok != GL_TRUE)) {
    std::cerr << "glCompileShader GL_FRAGMENT_SHADER failed!" << std::endl;
    return -1;
  }
  glAttachShader(prog, vs);
  glAttachShader(prog, fs);
  glLinkProgram(prog);
  if (glGetError()) {
    std::cerr << "glLinkProgram failed!" << std::endl;
    return -1;
  }

  glUseProgram(prog);
  glUniform1i(glGetUniformLocation(prog, "uTexY"), 0);
  glUniform1i(glGetUniformLocation(prog, "uTexC"), 1);

#if REUSE_TEXTURES
  glGenTextures(2, textures);
  for (int i = 0; i < 2; ++i) {
    glBindTexture(GL_TEXTURE_2D, textures[i]);
    setup_texture();
  }
  glBindTexture(GL_TEXTURE_2D, 0);
#endif

  GLint vp[4] = {0};
  glGetIntegerv(GL_VIEWPORT, vp);
  glViewport(0, 0, vp[2], vp[3]);
  return 0;
}

int VideoDirectRender::handleWindowEvents() {
  char param[64];
  XWindowAttributes attr{};
  XGetWindowAttributes(mXDisplay, mWindow, &attr);

  while (XPending(mXDisplay)) {
    XEvent ev{};
    XNextEvent(mXDisplay, &ev);
    switch (ev.type) {
      case ClientMessage:
        if (((Atom)ev.xclient.data.l[0]) == mWMDeleteWindow) {
          return -1;
        }
        break;
      case KeyPress:
        switch (XLookupKeysym(&ev.xkey, 0)) {
          case 'q':
            return -1;
          default:
            break;
        }
        break;
      case ConfigureNotify:
        glViewport(0, 0, ((XConfigureEvent *)&ev)->width,
                   ((XConfigureEvent *)&ev)->height);
        break;
      case MotionNotify:
        if (ev.xmotion.state & Button1MotionMask) {
          snprintf(
              param, 64,
              "{\"x\": %d, \"y\": %d, \"movementX\": %d, \"movementY\": %d }",
              ev.xmotion.x * 32767 / attr.width,
              ev.xmotion.y * 32767 / attr.height, ev.xmotion.x_root,
              ev.xmotion.y_root);
          mEventListener("mousemove", param);
        }
        break;
      case ButtonPress:
      case ButtonRelease:
        snprintf(param, 64, "{\"which\": %d, \"x\": %d, \"y\": %d }",
                 ev.xbutton.button, ev.xbutton.x * 32767 / attr.width,
                 ev.xbutton.y * 32767 / attr.height);
        mEventListener((ev.type == ButtonPress) ? "mousedown" : "mouseup",
                       param);
        break;
      default:
        break;
    }
  }
  return 0;
}

int VideoDirectRender::renderFrame(VASurfaceID va_surface) {
  fflush(stdout);
  VADRMPRIMESurfaceDescriptor prime{};
  if (vaExportSurfaceHandle(mVADisplay, va_surface,
                            VA_SURFACE_ATTRIB_MEM_TYPE_DRM_PRIME_2,
                            VA_EXPORT_SURFACE_READ_ONLY |
#if USE_LAYERS
                                VA_EXPORT_SURFACE_SEPARATE_LAYERS,
#else
                                VA_EXPORT_SURFACE_COMPOSED_LAYERS,
#endif
                            &prime) != VA_STATUS_SUCCESS) {
    std::cerr << "vaExportSurfaceHandle failed!" << std::endl;
    return -1;
  }

  if (prime.fourcc != VA_FOURCC_NV12) {
    std::cerr << "export format check error!" << std::endl;
    return -1;
  }
  vaSyncSurface(mVADisplay, va_surface);

  if (!texture_size_valid) {
    std::cout << "prime width: " << prime.width << ", height: " << prime.height
              << std::endl;
#if USE_CORE_PROFILE
    glUniform2f(glGetUniformLocation(prog, "uTexCoordScale"), texcoord_x1,
                texcoord_y1);
    offset = glGetUniformLocation(prog, "offset");
    //glUniform2f(offset, 1 - 1 / texcoord_x1, -1 + 1 / texcoord_y1);
    //glUniform2f(offset,  2 / texcoord_x1 * 0 , - 2 / texcoord_y1 * 0); // *i
    //glUniform2f(0, 0);
#endif
    texture_size_valid = true;
  }

  EGLImage images[2];
#if !REUSE_TEXTURES
  glGenTextures(2, textures);
#endif
  for (int i = 0; i < 2; ++i) {
    static const uint32_t formats[2] = {DRM_FORMAT_R8, DRM_FORMAT_GR88};
#if USE_LAYERS
#define LAYER i
#define PLANE 0
    if (prime.layers[i].drm_format != formats[i]) {
      std::cerr << "expected DRM format check error!" << std::endl;
      return -1;
    }
#else
#define LAYER 0
#define PLANE i
#endif

   std::cout << "img_attr LAYER: " <<  LAYER << ",PLANE "<<PLANE << std::endl;
    EGLint img_attr[] = {
        EGL_LINUX_DRM_FOURCC_EXT,
        (int)formats[i],
        EGL_WIDTH,
        (int)(prime.width / (i + 1)),
        EGL_HEIGHT,
        (int)(prime.height / (i + 1)),
        EGL_DMA_BUF_PLANE0_FD_EXT,
        prime.objects[prime.layers[LAYER].object_index[PLANE]].fd,
        EGL_DMA_BUF_PLANE0_OFFSET_EXT,
        (int)prime.layers[LAYER].offset[PLANE],
        EGL_DMA_BUF_PLANE0_PITCH_EXT,
        (int)prime.layers[LAYER].pitch[PLANE],
        EGL_NONE};

    images[i] = eglCreateImageKHR(egl_display, EGL_NO_CONTEXT,
                                  EGL_LINUX_DMA_BUF_EXT, NULL, img_attr);
    if (!images[i]) {
      std::cerr << "eglCreateImageKHR failed!" << std::endl;
      return -1;
    }

    glActiveTexture(GL_TEXTURE0 + i);
    glBindTexture(GL_TEXTURE_2D, textures[i]);
#if !REUSE_TEXTURES
    setup_texture();
#endif
    while (glGetError()) {
    }
    glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, images[i]);
    if (glGetError()) {
      std::cerr << "glEGLImageTargetTexture2DOES failed!" << std::endl;
      return -1;
    }
  }
  for (int i = 0; i < (int)prime.num_objects; ++i) {
    close(prime.objects[i].fd);
  }

  glClear(GL_COLOR_BUFFER_BIT);
  while (glGetError()) {
  }
#if USE_CORE_PROFILE
  for (int i = 0; i < 9; i++) {
      glUniform2f(glGetUniformLocation(prog, "uTexCoordScale"), texcoord_x1,
                texcoord_y1);
      offset = glGetUniformLocation(prog, "offset");
      //glUniform2f(offset, 1 - 1 / texcoord_x1, -1 + 1 / texcoord_y1);
      glUniform2f(offset,  -1 + 1 / texcoord_x1  + 2 / texcoord_x1 * ( i % 3) , 1 - 1 / texcoord_y1 - 2 / texcoord_y1 *(i / 3)); // *i
      glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
  }
  //glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
 
#else
  glActiveTexture(GL_TEXTURE0);
  glBegin(GL_QUADS);
  glTexCoord2f(0.0f, 0.0f);
  glVertex2i(0, 0);
  glTexCoord2f(texcoord_x1, 0.0f);
  glVertex2i(1, 0);
  glTexCoord2f(texcoord_x1, texcoord_y1);
  glVertex2i(1, 1);
  glTexCoord2f(0.0f, texcoord_y1);
  glVertex2i(0, 1);
  glEnd();
#endif

  if (glGetError()) {
    return -1;
  }

  eglSwapBuffers(egl_display, egl_surface);
  for (int i = 0; i < 2; ++i) {
    glActiveTexture(GL_TEXTURE0 + i);
    glBindTexture(GL_TEXTURE_2D, 0);
    eglDestroyImageKHR(egl_display, images[i]);
  }
#if !REUSE_TEXTURES
  glDeleteTextures(2, textures);
#endif
  return 0;
}
