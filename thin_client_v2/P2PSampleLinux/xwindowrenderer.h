// Copyright (C) <2019> Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
#pragma once
#include "displayutils.h"
#include "owt/p2p/p2pclient.h"
#include "owt/base/localcamerastreamparameters.h"
#include "owt/base/stream.h"
#include "owt/base/logging.h"
#include "owt/base/globalconfiguration.h"
#include "owt/base/exception.h"
#include "vaapi_device.h"

using namespace owt::base;

class XWindowRenderer : public VideoRendererVaInterface {
 public:
  explicit XWindowRenderer();
  void RenderFrame(std::unique_ptr<VaSurface> surface) override;
  ~XWindowRenderer() override;

 private:
  bool is_window_ready_;
  // The default X11 rendering area and window
 // Display* xdisplay_;
  //Window xwindow_;

  CHWDevice *m_hwdev;
};


