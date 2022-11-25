#include "aic_client_render.h"

#include <GLES3/gl3.h>
#include <SDL2/SDL_syswm.h>
#include <drm_fourcc.h>
#include <ft2build.h>
#include <unistd.h>

#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>
#include FT_FREETYPE_H

#include <iostream>

#define DECLARE_YUV2RGB_MATRIX_GLSL                   \
  "const mat4 yuv2rgb = mat4(\n"                      \
  "    vec4(  1.1644,  1.1644,  1.1644,  0.0000 ),\n" \
  "    vec4(  0.0000, -0.2132,  2.1124,  0.0000 ),\n" \
  "    vec4(  1.7927, -0.5329,  0.0000,  0.0000 ),\n" \
  "    vec4( -0.9729,  0.3015, -1.1334,  1.0000 ));"

static const char *vert_shader_text =
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
    "    gl_Position = vec4((c * vec2(2.,-2.) + vec2(-1.,1.)) / uTexCoordScale "
    "+ offset, 0., 1.);"
    "\n"
    "}";

static const char *frag_shader_text =
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

const char *text_vs =
    "#version 130\n"
    "in vec4 vertex;\n"  // <vec2 pos, vec2 tex>
    "out vec2 TexCoords;\n"
    "uniform mat4 projection;\n"
    "void main()\n"
    "{\n"
    "  gl_Position = projection * vec4(vertex.xy, 0.0, 1.0);\n"
    "  TexCoords = vertex.zw;\n"
    "}\n";

const char *text_fs =
    "#version 130\n"
    "in vec2 TexCoords;\n"
    "out vec4 color;\n"
    "uniform sampler2D text;\n"
    "uniform vec3 textColor;\n"
    "void main()\n"
    "{\n"
    "   vec4 sampled = vec4(1.0, 1.0, 1.0, texture(text, TexCoords).r);\n"
    "   color = vec4(textColor, 1.0) * sampled;\n"
    "}\n";

struct Character {
  GLuint textureID;
  glm::ivec2 size;
  glm::ivec2 bearing;
  GLuint advance;
};

AicClientRender::AicClientRender(int x, int y, int *w, int *h, int n) {
  scale = n;
  scale_width = 1.0f / n;
  EGLint egl_config_attr[] = {EGL_SURFACE_TYPE,
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

  EGLint numConfigs = 0, majorVersion = 0, minorVersion = 0;
  glWindow = SDL_CreateWindow("multi-stream player", 0, 0, 0, 0,
                              SDL_WINDOW_OPENGL | SDL_WINDOW_MAXIMIZED);
  glDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
  eglInitialize(glDisplay, &majorVersion, &minorVersion);
  if (!eglBindAPI(EGL_OPENGL_API)) {
    std::cerr << "eglBindAPI failed!" << std::endl;
    exit(-1);
  }
  eglChooseConfig(glDisplay, egl_config_attr, &glConfig, 1, &numConfigs);
  SDL_SysWMinfo sysInfo{};
  SDL_VERSION(&sysInfo.version);
  SDL_GetWindowWMInfo(glWindow, &sysInfo);
  glContext = eglCreateContext(glDisplay, glConfig, EGL_NO_CONTEXT, NULL);
  if(sysInfo.subsystem != SDL_SYSWM_X11) {
    std::cout << "please use x11 window" << std::endl;
    exit(-1);
  }
  glSurface = eglCreateWindowSurface(
      glDisplay, glConfig, (EGLNativeWindowType)sysInfo.info.x11.window, 0);
  eglMakeCurrent(glDisplay, glSurface, glSurface, glContext);
  std::cout << "major version:  " << majorVersion
            << "minorVersion: " << minorVersion << std::endl;
  std::cout << "OpenGL vendor:   " << glGetString(GL_VENDOR) << std::endl;
  std::cout << "OpenGL renderer: " << glGetString(GL_RENDERER) << std::endl;
  std::cout << "OpenGL version:  " << glGetString(GL_VERSION) << std::endl;

  eglQuerySurface(glDisplay, glSurface, EGL_WIDTH, &mWidth);
  eglQuerySurface(glDisplay, glSurface, EGL_HEIGHT, &mHeight);
  *w = mWidth;
  *h = mHeight;
  std::cout << "mWidth: " << mWidth << ", mHeight: " << mHeight
            << ", scale: " << scale << std::endl;
  eglCreateImageKHR =
      (PFNEGLCREATEIMAGEKHRPROC)eglGetProcAddress("eglCreateImageKHR");
  if (eglCreateImageKHR == NULL) {
    std::cout << "query eglCreateImageKHR failed" << std::endl;
  }
  eglDestroyImageKHR =
      (PFNEGLDESTROYIMAGEKHRPROC)eglGetProcAddress("eglDestroyImageKHR");
  if (eglDestroyImageKHR == NULL) {
    std::cout << "query eglDestroyImageKHR failed" << std::endl;
  }
  glEGLImageTargetTexture2DOES =
      (PFNGLEGLIMAGETARGETTEXTURE2DOESPROC)eglGetProcAddress(
          "glEGLImageTargetTexture2DOES");
  if (glEGLImageTargetTexture2DOES == NULL) {
    std::cout << "query glEGLImageTargetTexture2DOES failed" << std::endl;
  }

  glEnable(GL_BLEND);
  glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
  glProgram = loadProgram(vert_shader_text, frag_shader_text);

  glUseProgram(glProgram);

  glUniform1i(glGetUniformLocation(glProgram, "uTexY"), 0);
  glUniform1i(glGetUniformLocation(glProgram, "uTexC"), 1);
  glUniform2f(glGetUniformLocation(glProgram, "uTexCoordScale"), scale, scale);
  offset = glGetUniformLocation(glProgram, "offset");
  glUniform2f(offset, 0, 0);

  initTextGL();
  initFreeType();
  beginFrame();
  endFrame();
}

void AicClientRender::renderUpdate(int n) {
  if (scale != n) {
    scale = n;
    scale_width = 1.0f / n;
    glUseProgram(glProgram);
    glUniform2f(glGetUniformLocation(glProgram, "uTexCoordScale"), scale, scale);
  }
}

GLuint AicClientRender::loadProgram(const char *vert_source,
                                    const char *frag_source) {
  GLuint vert_shader = 0;
  GLuint frag_shader = 0;
  GLuint program = 0;
  GLint status = 0;

  vert_shader = loadShader(vert_source, GL_VERTEX_SHADER);
  frag_shader = loadShader(frag_source, GL_FRAGMENT_SHADER);
  program = glCreateProgram();
  glAttachShader(program, vert_shader);
  glAttachShader(program, frag_shader);
  glLinkProgram(program);

  glGetProgramiv(program, GL_LINK_STATUS, &status);
  if (!status) {
    char log[1000] = {'\0'};
    GLsizei len;
    glGetProgramInfoLog(program, 1000, &len, log);
    std::cout << "get program info log: " << log << std::endl;
  }
  return program;
}

GLuint AicClientRender::loadShader(const char *source, GLenum shaderType) {
  GLuint shader;
  GLint status = 0;
  shader = glCreateShader(shaderType);
  if (!shader) {
    std::cout << "Error: create shader failed"<< std::endl;
    exit(-1);
  } else {
    glShaderSource(shader, 1, (const char **)&source, NULL);
    glCompileShader(shader);
    glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
    if (!status) {
      char log[1000] = {'\0'};
      GLsizei len;
      glGetShaderInfoLog(shader, 1000, &len, log);
      std::cout << "Error: compile shader failed " << log << std::endl;
    }
  }
  return shader;
}

void AicClientRender::beginFrame() {
  glViewport(0, 0, mWidth, mHeight);
  glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
  glClear(GL_COLOR_BUFFER_BIT);
}

void AicClientRender::endFrame() { eglSwapBuffers(glDisplay, glSurface); }

int AicClientRender::generateTexture(AVFrame *frame, GLuint *textures,
                                     EGLImage *images, VADisplay vaDisplay) {
  VASurfaceID va_surface = (uintptr_t)frame->data[3];
  VADRMPRIMESurfaceDescriptor prime{};
  if (vaExportSurfaceHandle(
          vaDisplay, va_surface, VA_SURFACE_ATTRIB_MEM_TYPE_DRM_PRIME_2,
          VA_EXPORT_SURFACE_READ_ONLY | VA_EXPORT_SURFACE_SEPARATE_LAYERS,
          &prime) != VA_STATUS_SUCCESS) {
    std::cerr << "vaExportSurfaceHandle failed!" << std::endl;
    return -1;
  } else {
    vaSyncSurface(vaDisplay, va_surface);
    std::cout << "prime width: " << prime.width << ", height: " << prime.height
              << std::endl;
  }
  for (int i = 0; i < 2; ++i) {
    EGLint img_attr[] = {
        EGL_LINUX_DRM_FOURCC_EXT,
        (int)prime.layers[i].drm_format,
        EGL_WIDTH,
        (int)(prime.width / (i + 1)),
        EGL_HEIGHT,
        (int)(prime.height / (i + 1)),
        EGL_DMA_BUF_PLANE0_FD_EXT,
        prime.objects[prime.layers[i].object_index[0]].fd,
        EGL_DMA_BUF_PLANE0_OFFSET_EXT,
        (int)prime.layers[i].offset[0],
        EGL_DMA_BUF_PLANE0_PITCH_EXT,
        (int)prime.layers[i].pitch[0],
        EGL_DMA_BUF_PLANE0_MODIFIER_LO_EXT,
        prime.objects[prime.layers[i].object_index[0]].drm_format_modifier &
            0xFFFFFFFF,
        EGL_DMA_BUF_PLANE0_MODIFIER_HI_EXT,
        prime.objects[prime.layers[i].object_index[0]].drm_format_modifier >>
            32,
        EGL_NONE};

    images[i] = eglCreateImageKHR(glDisplay, EGL_NO_CONTEXT,
                                  EGL_LINUX_DMA_BUF_EXT, NULL, img_attr);
    if (!images[i]) {
      std::cerr << "eglCreateImageKHR failed!" << std::endl;
      return -1;
    }

    glActiveTexture(GL_TEXTURE0 + i);
    glBindTexture(GL_TEXTURE_2D, textures[i]);
    while (glGetError()) {
    }
    glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, images[i]);
    if (glGetError()) {
      std::cerr << "glEGLImageTargetTexture2DOES failed!" << std::endl;
      return -2;
    }
  }
  for (int i = 0; i < (int)prime.num_objects; ++i) {
    close(prime.objects[i].fd);
  }
  return 0;
}

void AicClientRender::renderFrame(int index, GLuint *textures) {
  glUseProgram(glProgram);
  for (int i = 0; i < 2; i++) {
    glActiveTexture(GL_TEXTURE0 + i);
    glBindTexture(GL_TEXTURE_2D, textures[i]);
  }
  glUniform2f(offset, -1 + scale_width + 2.0 * scale_width * (index % scale),
              1 - scale_width - 2 * scale_width * (index / scale));  // *i
  glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
}

void AicClientRender::destroyImage(EGLImage *images) {
  for (int i = 0; i < 2; ++i) {
    if (images[i] != nullptr) {
      glActiveTexture(GL_TEXTURE0 + i);
      glBindTexture(GL_TEXTURE_2D, 0);
      eglDestroyImageKHR(glDisplay, images[i]);
    }
  }
}

void AicClientRender::initTextGL() {
  textProgram = loadProgram(text_vs, text_fs);
  glUseProgram(textProgram);
  glBindAttribLocation(textProgram, 0, "vertex");
  glm::mat4 projection = glm::ortho(0.0f, static_cast<GLfloat>(mWidth), 0.0f,
                                    static_cast<GLfloat>(mHeight));
  glUniformMatrix4fv(glGetUniformLocation(textProgram, "projection"), 1,
                     GL_FALSE, glm::value_ptr(projection));

  glUniform3f(glGetUniformLocation(textProgram, "textColor"), 1.0f, 0.0f, 0.0f);
  glActiveTexture(GL_TEXTURE0);
  glGenVertexArrays(1, &VAO);
  glGenBuffers(1, &VBO);
  glBindVertexArray(VAO);
  glBindBuffer(GL_ARRAY_BUFFER, VBO);
  glBufferData(GL_ARRAY_BUFFER, sizeof(GLfloat) * 6 * 4, NULL, GL_DYNAMIC_DRAW);
  glEnableVertexAttribArray(0);
  glVertexAttribPointer(0, 4, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), 0);
  glBindBuffer(GL_ARRAY_BUFFER, 0);
  glBindVertexArray(0);
}

void AicClientRender::initFreeType() {
  FT_Library ft = nullptr;
  if (FT_Init_FreeType(&ft))
    std::cout << "ERROR::FREETYPE: Could not init FreeType Library"
              << std::endl;
  FT_Face face = nullptr;
  if (FT_New_Face(ft, "SourceSansPro-Regular.ttf", 0, &face))
    std::cout << "ERROR::FREETYPE: Failed to load font" << std::endl;

  FT_Set_Pixel_Sizes(face, 0, 20);
  glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
  for (GLubyte c = 0; c < 128; c++) {
    if (FT_Load_Char(face, c, FT_LOAD_RENDER)) {
      std::cout << "ERROR::FREETYTPE: Failed to load Glyph" << std::endl;
      continue;
    }
    GLuint texture = 0;
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, face->glyph->bitmap.width,
                 face->glyph->bitmap.rows, 0, GL_RED, GL_UNSIGNED_BYTE,
                 face->glyph->bitmap.buffer);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    Character character = {
        texture,
        glm::ivec2(face->glyph->bitmap.width, face->glyph->bitmap.rows),
        glm::ivec2(face->glyph->bitmap_left, face->glyph->bitmap_top),
        face->glyph->advance.x};
    characters.insert(std::pair<GLchar, Character>(c, character));
  }
  glBindTexture(GL_TEXTURE_2D, 0);
  FT_Done_Face(face);
  FT_Done_FreeType(ft);
}

void AicClientRender::renderText(std::string text, int index) {
  GLfloat x = 10 + mWidth / scale * (index % scale);
  GLfloat y = mHeight - (30 + mHeight / scale * (index / scale));

  glUseProgram(textProgram);
  glActiveTexture(GL_TEXTURE0);
  glBindVertexArray(VAO);

  std::string::const_iterator c;
  for (c = text.begin(); c != text.end(); c++) {
    Character ch = characters[*c];

    GLfloat xpos = x + ch.bearing.x;
    GLfloat ypos = y - (ch.size.y - ch.bearing.y);

    GLfloat w = ch.size.x;
    GLfloat h = ch.size.y;
    GLfloat vertices[6][4] = {
        {xpos, ypos + h, 0.0, 0.0},    {xpos, ypos, 0.0, 1.0},
        {xpos + w, ypos, 1.0, 1.0},

        {xpos, ypos + h, 0.0, 0.0},    {xpos + w, ypos, 1.0, 1.0},
        {xpos + w, ypos + h, 1.0, 0.0}};
    glBindTexture(GL_TEXTURE_2D, ch.textureID);
    glBindBuffer(GL_ARRAY_BUFFER, VBO);
    glBufferSubData(GL_ARRAY_BUFFER, 0, sizeof(vertices), vertices);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDrawArrays(GL_TRIANGLES, 0, 6);
    x += (ch.advance >> 6);
  }
  glBindVertexArray(0);
  glBindTexture(GL_TEXTURE_2D, 0);
}
