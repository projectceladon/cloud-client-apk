#ifndef AIC_CLIENT_RENDER_
#define AIC_CLIENT_RENDER_
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GL/gl.h>
#include <GL/glext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <SDL2/SDL.h>
#include <va/va.h>
#include <va/va_drmcommon.h>

#include <map>
extern "C" {
#include "libavutil/frame.h"
}

struct Character;

class AicClientRender {
 public:
  AicClientRender(int x, int y, int* w, int* h, int n);
  GLuint loadProgram(const char* vert_source, const char* frag_source);
  void beginFrame();
  void endFrame();
  GLuint loadShader(const char* source, GLenum shaderType);
  int generateTexture(AVFrame* frame, GLuint* textures, EGLImage* images,
                      VADisplay vaDisplay);
  void renderFrame(int index, GLuint* textures);
  void destroyImage(EGLImage* images);
  void initTextGL();
  void initFreeType();
  void renderText(std::string text, int index);
  void renderUpdate(int n);

 private:
  SDL_Window* glWindow = NULL;
  EGLDisplay glDisplay;
  EGLConfig glConfig{};
  EGLContext glContext;
  EGLSurface glSurface;
  unsigned int glProgram;
  GLuint offset;

  unsigned int textProgram;
  unsigned int VBO = 0, VAO = 0;
  std::map<GLchar, Character> characters;

  int scale;
  int mWidth = 0;
  int mHeight = 0;
  float scale_width;

  PFNEGLCREATEIMAGEKHRPROC eglCreateImageKHR;
  PFNEGLDESTROYIMAGEKHRPROC eglDestroyImageKHR;
  PFNGLEGLIMAGETARGETTEXTURE2DOESPROC glEGLImageTargetTexture2DOES;
};
#endif