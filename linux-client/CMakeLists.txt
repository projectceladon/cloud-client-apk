set(TARGET_OS "linux")
project(aic_linux_client)
cmake_minimum_required(VERSION 2.8)

set(CMAKE_BUILD_TYPE Release)
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wformat -Wformat-security -Werror=format-security -Wno-unknown-pragmas \
                    -D_FORTIFY_SOURCE=2 -fstack-protector-strong -fPIE -fPIC -Wall")
set(CMAKE_CXX_FLAGS_DEBUG "$ENV{CXXFLAGS} -O0 -Wall -g -ggdb")
set(CMAKE_CXX_FLAGS_RELEASE "$ENV{CXXFLAGS} -O3 -Wall")

set(LINK_FLAGS "-pie -Wl,-z,relro -Wl,-z,now -Wl,-z,noexecstack")
set(CMAKE_EXE_LINKER_FLAGS "${LINK_FLAGS}")
set(CMAKE_SHARED_LINKER_FLAGS "${LINK_FLAGS}")
add_definitions(-DUSE_CLANG=0)

find_package(SDL2 REQUIRED)
include_directories(${SDL2_INCLUDE_DIRS})

INCLUDE(FindPkgConfig)
PKG_SEARCH_MODULE(SDL2TTF REQUIRED SDL2_ttf>=2.0.0)
include_directories(${SDL2TTF_INCLUDE_DIRS})

INCLUDE(FindPkgConfig)
include_directories(
  ${CMAKE_SOURCE_DIR}/include
  ${CMAKE_SOURCE_DIR}/include/ffmpeg/include
  /usr/include/libdrm
)

link_directories(${CMAKE_SOURCE_DIR}/lib)

add_executable(aic_linux_client
  aic_linux_client.cpp
  GameSession.cpp
  owt_signalingchannel.cpp
  EncodedVideoDispatcher.cpp
  VideoDecoderDispatcher.cpp
  CGCodec.cpp
  VideoRender.cpp
  VideoDecoder.cpp
  VideoDirectRender.cpp
)

set_target_properties(aic_linux_client PROPERTIES COMPILE_DEFINITIONS "WEBRTC_LINUX;WEBRTC_POSIX;USE_X11=1;")
target_link_libraries(aic_linux_client
  ${SDL2_LIBRARIES}
  ${SDL2TTF_LIBRARIES}
  boost_system
  sioclient
  owt-release
  pthread
  ssl
  crypto
  avutil
  avcodec
  avfilter
  avformat
  swscale
  swresample
  X11
  va-x11
  va-drm
  va
  drm
  EGL
  GL
)
