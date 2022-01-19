// Copyright (C) <2020> Intel Corporation
//
// SPDX-License-Identifier: Apache-2

// clang-format off
#include "videorenderer.h"
// clang-format on

using std::chrono::duration_cast;
using std::chrono::high_resolution_clock;
using std::chrono::microseconds;
using std::chrono::milliseconds;

static const UINT kBufferCountWithTearing = 3;
static const UINT kBufferCountWithoutTearing = 2;

bool DXRenderer::DXGIIsTearingSupported() {
  IDXGIFactory4* dxgi_factory4;
  UINT allow_tearing = 0;

  HRESULT hr = CreateDXGIFactory1(IID_PPV_ARGS(&dxgi_factory4));
  if (SUCCEEDED(hr)) {
    IDXGIFactory5* dxgi_factory5;
    hr = dxgi_factory4->QueryInterface(__uuidof(IDXGIFactory5), (void**)&dxgi_factory5);
    if (SUCCEEDED(hr)) {
      hr = dxgi_factory5->CheckFeatureSupport(DXGI_FEATURE_PRESENT_ALLOW_TEARING,
        &allow_tearing, sizeof(allow_tearing));
    }
  }
  return SUCCEEDED(hr) && allow_tearing;
}

void DXRenderer::FillSwapChainDesc(DXGI_SWAP_CHAIN_DESC1 &scd) {
  bool tearing_allowed = dxgi_allow_tearing_ && allow_async_flip_;

  scd.Width = scd.Height = 0; // automatic sizing.
  scd.Format = DXGI_FORMAT_B8G8R8A8_UNORM;
  scd.Stereo = false;
  scd.SampleDesc.Count = 1; // no multi-sampling
  scd.SampleDesc.Quality = 0;
  scd.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
  scd.BufferCount = tearing_allowed ? kBufferCountWithTearing : kBufferCountWithoutTearing;
  scd.Scaling = DXGI_SCALING_STRETCH;
  scd.Flags = DXGI_SWAP_CHAIN_FLAG_ALLOW_MODE_SWITCH;
  if (tearing_allowed){
    scd.SwapEffect = DXGI_SWAP_EFFECT_FLIP_SEQUENTIAL;
    scd.Flags |= DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING;
  }
  else
    scd.SwapEffect = DXGI_SWAP_EFFECT_DISCARD;
}

void DXRenderer::RenderFrame(std::unique_ptr<owt::base::VideoBuffer> buffer) {

  HRESULT hr = S_FALSE;
  uint16_t width = 0;
  uint16_t height = 0;

  ID3D11Device *render_device = nullptr;
  ID3D11VideoDevice *render_video_device = nullptr;
  ID3D11Texture2D *texture = nullptr;
  ID3D11VideoContext *render_context = nullptr;
  ID3D11Texture2D *current_back_buffer = nullptr;
  int array_slice = -1;

  if (!wnd_ || !dxgi_factory_ || !IsWindow(wnd_)) {
    hr = S_FALSE;
  } else {
    owt::base::D3D11VAHandle *handle =
        reinterpret_cast<owt::base::D3D11VAHandle *>(buffer->buffer);

    if (handle) {
      hr = S_OK;
      width = buffer->resolution.width;
      height = buffer->resolution.height;

      if (width == 0 || height == 0) {
        hr = S_FALSE;
      } else {
        render_device = handle->d3d11_device;
        render_video_device = handle->d3d11_video_device;
        texture = handle->texture;
        render_context = handle->context;
        array_slice = handle->array_index;

        D3D11_TEXTURE2D_DESC texture_desc;
        if (texture) {
          texture->GetDesc(&texture_desc);
        }
        if (render_device == nullptr || render_video_device == nullptr ||
            texture == nullptr || render_context == nullptr) {
          hr = S_FALSE;
        } else {
          if (render_device != d3d11_device_ ||
              render_video_device != d3d11_video_device_ ||
              render_context != d3d11_video_context_) {
            d3d11_device_ = render_device;
            d3d11_video_device_ = render_video_device;
            d3d11_video_context_ = render_context;
            need_swapchain_recreate = true;
          }
        }
      }
    }
  }

  if (hr == S_OK && need_swapchain_recreate) {

    if (swap_chain_for_hwnd_)
      swap_chain_for_hwnd_->Release();

    DXGI_SWAP_CHAIN_DESC1 swap_chain_desc = {0};
    FillSwapChainDesc(swap_chain_desc);

    hr = dxgi_factory_->CreateSwapChainForHwnd(d3d11_device_, wnd_,
                                               &swap_chain_desc, nullptr,
                                               nullptr, &swap_chain_for_hwnd_);

    if (SUCCEEDED(hr)) {
      D3D11_VIDEO_PROCESSOR_CONTENT_DESC content_desc;
      memset(&content_desc, 0, sizeof(content_desc));

      // Non-scaling
      content_desc.InputFrameFormat = D3D11_VIDEO_FRAME_FORMAT_PROGRESSIVE;
      content_desc.InputFrameRate.Numerator = 1000;
      content_desc.InputFrameRate.Denominator = 1;
      content_desc.InputWidth = width;
      content_desc.InputHeight = height;
      content_desc.OutputWidth = width_;
      content_desc.OutputHeight = height_;
      content_desc.OutputFrameRate.Numerator = 1000;
      content_desc.OutputFrameRate.Denominator = 1;
      content_desc.Usage = D3D11_VIDEO_USAGE_OPTIMAL_SPEED;

      HRESULT hr = d3d11_video_device_->CreateVideoProcessorEnumerator(
          &content_desc, &video_processors_enum_);

      if (SUCCEEDED(hr)) {
        if (video_processor_)
          video_processor_->Release();
        hr = d3d11_video_device_->CreateVideoProcessor(video_processors_enum_,
                                                       0, &video_processor_);

        if (SUCCEEDED(hr)) {
          RECT render_rect = {x_offset_, y_offset_, x_offset_ + width_,
                              y_offset_ + height_};
          d3d11_video_context_->VideoProcessorSetOutputTargetRect(
              video_processor_, TRUE, &render_rect);
        } else {
        }
      } else {
      }
    } else {
    }

    need_swapchain_recreate = false;
  }

  if (SUCCEEDED(hr)) {
    hr = swap_chain_for_hwnd_->GetBuffer(0, __uuidof(ID3D11Texture2D),
                                         (void **)&current_back_buffer);

    if (SUCCEEDED(hr)) {
      if (prev_back_buffer != current_back_buffer) {

        // Create output view and input view
        D3D11_VIDEO_PROCESSOR_OUTPUT_VIEW_DESC output_view_desc;
        memset(&output_view_desc, 0, sizeof(output_view_desc));

        output_view_desc.ViewDimension = D3D11_VPOV_DIMENSION_TEXTURE2D;
        output_view_desc.Texture2D.MipSlice = 0;

        hr = d3d11_video_device_->CreateVideoProcessorOutputView(
            current_back_buffer, video_processors_enum_, &output_view_desc,
            &output_view_);

        if (FAILED(hr)) {
        }
      }

      if (SUCCEEDED(hr)) {
        prev_array_slice_ = array_slice;
        D3D11_VIDEO_PROCESSOR_INPUT_VIEW_DESC input_view_desc;
        memset(&input_view_desc, 0, sizeof(input_view_desc));
        input_view_desc.FourCC = 0;
        input_view_desc.ViewDimension = D3D11_VPIV_DIMENSION_TEXTURE2D;
        input_view_desc.Texture2D.MipSlice = 0;
        input_view_desc.Texture2D.ArraySlice = array_slice;

        hr = d3d11_video_device_->CreateVideoProcessorInputView(
            texture, video_processors_enum_, &input_view_desc, &input_view_);

        if (SUCCEEDED(hr)) {
          // Blit NV12 surface to RGB back buffer here.
          RECT rect = {0, 0, width, height};
          memset(&stream_, 0, sizeof(stream_));
          stream_.Enable = true;
          stream_.OutputIndex = 0;
          stream_.InputFrameOrField = 0;
          stream_.PastFrames = 0;
          stream_.ppPastSurfaces = nullptr;
          stream_.ppFutureSurfaces = nullptr;
          stream_.pInputSurface = input_view_;
          stream_.ppPastSurfacesRight = nullptr;
          stream_.ppFutureSurfacesRight = nullptr;
          stream_.pInputSurfaceRight = nullptr;

          d3d11_video_context_->VideoProcessorSetStreamSourceRect(
              video_processor_, 0, true, &rect);
          d3d11_video_context_->VideoProcessorSetStreamFrameFormat(
              video_processor_, 0, D3D11_VIDEO_FRAME_FORMAT_PROGRESSIVE);
        } else {
        }
      }
    } else {
    }
  }

  if (SUCCEEDED(hr)) {
    hr = d3d11_video_context_->VideoProcessorBlt(video_processor_, output_view_,
                                                 0, 1, &stream_);
    if (FAILED(hr)) {
    }
  }

  prev_back_buffer = current_back_buffer;
  prev_texture = texture;

  if (SUCCEEDED(hr)) {
    DXGI_PRESENT_PARAMETERS parameters = {0};
    hr = swap_chain_for_hwnd_->Present1(0,
        (dxgi_allow_tearing_ && allow_async_flip_) ? DXGI_PRESENT_ALLOW_TEARING : 0,
        &parameters);
    auto present_end = high_resolution_clock::now();
    auto delta_since_last_frame =
        duration_cast<milliseconds>(present_end - last_present_ts_).count();
    last_present_ts_ = present_end;
  }

  return;
}

void DXRenderer::Cleanup() {
  if (swap_chain_for_hwnd_) {
    swap_chain_for_hwnd_->Release();
  }
}
