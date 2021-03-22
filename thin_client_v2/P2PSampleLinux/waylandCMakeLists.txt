cmake_minimum_required(VERSION 3.1)
project(ics_p2p_sample)
set(configuration "Release")
set(builddir "${CMAKE_CURRENT_SOURCE_DIR}/out")
SET(CMAKE_CXX_COMPILER "/usr/bin/g++")
set(ICS_P2P_SAMPLE_VERSION_MAJOR 0)
set(ICS_P2P_SAMPLE_VERSION_MINOR 1)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC -fno-exceptions -Wno-deprecated -std=c++17 -fexceptions -fpermissive")
set(CMAKE_OWT_PATH "$OWT_CLIENT_HOME")

file(GLOB ICS_P2P_SAMPLE_SOURCES
  "${CMAKE_CURRENT_SOURCE_DIR}/p2psample.cc"
  "${CMAKE_CURRENT_SOURCE_DIR}/p2psocketsignalingchannel.cc"
  "${CMAKE_CURRENT_SOURCE_DIR}/p2psocketsignalingchannel.h"
  "${CMAKE_CURRENT_SOURCE_DIR}/displayutils.cc"
  "${CMAKE_CURRENT_SOURCE_DIR}/xwindowrenderer.cc"
  "${CMAKE_CURRENT_SOURCE_DIR}/wayland/class_wayland.cpp"
  "${CMAKE_CURRENT_SOURCE_DIR}/wayland/listener_wayland.cpp"
  "${CMAKE_CURRENT_SOURCE_DIR}/wayland-drm-protocol.c"
  "${CMAKE_CURRENT_SOURCE_DIR}/vaapi_utils.cpp"
  "${CMAKE_CURRENT_SOURCE_DIR}/vaapi_utils_drm.cpp"
  "${CMAKE_CURRENT_SOURCE_DIR}/vaapi_device.cpp"
  "${CMAKE_CURRENT_SOURCE_DIR}/sample_utils.cpp"
  )
add_executable(ics_p2p_sample ${ICS_P2P_SAMPLE_SOURCES})

set_target_properties(ics_p2p_sample PROPERTIES RUNTIME_OUTPUT_DIRECTORY "${builddir}")
set_target_properties(ics_p2p_sample PROPERTIES PREFIX "")
set_target_properties(ics_p2p_sample PROPERTIES RUNTIME_OUTPUT_NAME "owt_p2p_sample")
set_target_properties(ics_p2p_sample PROPERTIES SUFFIX "")
set_source_files_properties(${builddir}/ics_p2p_sample PROPERTIES GENERATED "TRUE")

include_directories("${CMAKE_CURRENT_SOURCE_DIR}" "../src/talk/owt/sdk/include/cpp/" "/opt/intel/mediasdk/include/mfx" "wayland/include" "/usr/include/libdrm")

set_target_properties(ics_p2p_sample PROPERTIES COMPILE_DEFINITIONS "WEBRTC_LINUX;WEBRTC_POSIX;USE_X11=1;USE_LIBPCI=1;USE_GLIB=1;USE_NSS_CERTS=1;USE_NSS_VERIFIER=1;DYNAMIC_ANNOTATIONS_ENABLED=1;WTF_USE_DYNAMIC_ANNOTATIONS=1;LIBVA_WAYLAND_SUPPORT=1;LIBVA_SUPPORT=1;LIBVA_DRM_SUPPORT=1")
set_target_properties(ics_p2p_sample PROPERTIES COMPILE_FLAGS " -pthread")

target_link_libraries(ics_p2p_sample
  "-Wl,--start-group"
  "${CMAKE_CURRENT_SOURCE_DIR}/deps/libs/libowt.a"
  "${CMAKE_CURRENT_SOURCE_DIR}/deps/libs/libowt_sdk_base.a"
  "${CMAKE_CURRENT_SOURCE_DIR}/deps/libs/libowt_sdk_p2p.a"
  "${CMAKE_CURRENT_SOURCE_DIR}/deps/libs/libboost_date_time.a"
  "${CMAKE_CURRENT_SOURCE_DIR}/deps/libs/libboost_random.a"
  "${CMAKE_CURRENT_SOURCE_DIR}/deps/libs/libboost_system.a"
  #"${CMAKE_CURRENT_SOURCE_DIR}/libs/libavutil.a"
  #"${CMAKE_CURRENT_SOURCE_DIR}/libs/libavformat.a"
  #"${CMAKE_CURRENT_SOURCE_DIR}/libs/libavcodec.a"
  #"${CMAKE_CURRENT_SOURCE_DIR}/libs/libavdevice.a"
  #"${CMAKE_CURRENT_SOURCE_DIR}/libs/libavfilter.a"
  #"${CMAKE_CURRENT_SOURCE_DIR}/libs/libswscale.a"
  #"${CMAKE_CURRENT_SOURCE_DIR}/libs/libswresample.a"
  "-Wl,--end-group"
  "-lz"
  "-lasound"
  "-lbz2"
  "-lrt"
  "-lm"
  "-ldl"
  "-lxcb"
  "-lxcb-xv"
  "-lX11"
  "-lXext"
  "-lXcomposite"
  "-lXrender"
  "-lpthread"
  "-lva"
  "-lva-x11"
  "-lva-drm"
  "-ldl"
  "-lmfx -L/opt/intel/mediasdk/lib"
  "-lboost_system -lsioclient -L../deps/libs")

add_compile_options(-std=c++17)
