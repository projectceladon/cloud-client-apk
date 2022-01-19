// Copyright (C) <2020> Intel Corporation
//
// SPDX-License-Identifier: Apache-2

#ifndef GA_DXRENDERER_H_
#define GA_DXRENDERER_H_

#include "owt/base/videorendererinterface.h"

#include <chrono>
#include <d3d11.h>
#include <dxgi1_2.h>
#include <dxgi1_4.h>
#include <dxgi1_5.h>

// The video renderer implementation that accepts decoded video frame from WebRTC stack.
// Operation process:
//   1. Client subscribe the gaming stream add attach an instance of this renderer, passing the HWND
// handle used for video rendering.
//   2. On each decoded frame, WebRTC stack will pass decoded frame(ID3D11Texture2D instance. FourCC: NV12),
// and the D3D11Device instance associated with that texture frame in the overrided RenderFrame() callback.
//   3. Use D3D11VideoProcessor to do color space conversion from NV12 to ARGB, and render it.
class DXRenderer : public owt::base::VideoRendererInterface {
public:
  DXRenderer() : wnd_(NULL) {
    CreateDXGIFactory(__uuidof(IDXGIFactory2), (void **)(&dxgi_factory_));
    dxgi_allow_tearing_ = DXGIIsTearingSupported();
  }
  void SetWindow(HWND handle) { wnd_ = handle; }
  void SetAllowAsyncFlip(bool async_flip) { allow_async_flip_ = async_flip; }
  void RenderFrame(std::unique_ptr<owt::base::VideoBuffer> buffer);
  owt::base::VideoRendererType Type() {
    return owt::base::VideoRendererType::kD3D11Handle;
  }
  void SetWindowSize(UINT x, UINT y, UINT w, UINT h) {
    x_offset_ = x;
    y_offset_ = y;
    width_ = w;
    height_ = h;
    need_swapchain_recreate = true;
  }
  void Cleanup();
private:
  void FillSwapChainDesc(DXGI_SWAP_CHAIN_DESC1 &scd);
  bool DXGIIsTearingSupported();

  HWND wnd_ = nullptr;
  uint16_t x_offset_ = 0;
  uint16_t y_offset_ = 0;

  uint16_t width_ = 0;
  uint16_t height_ = 0;
  bool need_swapchain_recreate = true;
  bool dxgi_allow_tearing_ = false;
  bool allow_async_flip_ = false;

  // Owner of the d3d11 device/context is decoder.
  ID3D11Device *d3d11_device_ = nullptr;
  ID3D11VideoDevice *d3d11_video_device_ = nullptr;
  ID3D11VideoContext *d3d11_video_context_ = nullptr;
  IDXGIFactory2 *dxgi_factory_ = nullptr;
  ID3D11VideoProcessorEnumerator *video_processors_enum_ = nullptr;
  ID3D11VideoProcessor *video_processor_ = nullptr;
  ID3D11VideoProcessorInputView *input_view_ = nullptr;
  ID3D11VideoProcessorOutputView *output_view_ = nullptr;
  IDXGISwapChain1 *swap_chain_for_hwnd_ = nullptr;
  D3D11_VIDEO_PROCESSOR_STREAM stream_;
  ID3D11Texture2D *prev_back_buffer = nullptr;
  ID3D11Texture2D *prev_texture = nullptr;
  int prev_array_slice_ = -1;
  std::chrono::high_resolution_clock::time_point last_present_ts_;
};

#endif // GA_DXRENDERER_H_
