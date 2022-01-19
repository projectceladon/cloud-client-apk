// Copyright (C) <2020> Intel Corporation
//
// SPDX-License-Identifier: Apache-2

// clang-format off
#include "gflags/gflags.h"
#include "gaoption.h"
#include "windowclass.h"
#include "controlhandler.h"
#include "windowhandler.h"
// clang-format on

WindowHandler *WindowHandler::window_handler_ = nullptr;

WindowHandler *WindowHandler::GetInstance(void) {
  if (!window_handler_) {
    window_handler_ = new WindowHandler();
  }
  return window_handler_;
}

void WindowHandler::OnMouseStateChange(bool is_cursor_visible) {
  HWND hwnd;

  hwnd = WindowHandler::GetInstance()->wc_->hwnd_;
  if (!is_cursor_visible) {
    SendMessage(hwnd, WM_GA_CURSOR_VISIBLE, 0, GA_HIDE_CURSOR);
  } else {
    SendMessage(hwnd, WM_GA_CURSOR_VISIBLE, 0, GA_SHOW_CURSOR);
  }
}

int WindowHandler::OnGameServerConnected(std::string &session_id) {
  if (!session_id.empty()) {
    WindowHandler* window_handler = WindowHandler::GetInstance();
    window_handler->session_id_ = session_id;
    window_handler->connected_ = true;
  }
  return 0;
}

int WindowHandler::InitializeGameWindow(HINSTANCE h_instance, int n_cmd_show,
                                       int height, int width,
                                       const char *window_title) {
  int ret_value = 0;
  wc_.reset(
      new WindowClass(h_instance, n_cmd_show, height, width, window_title));
  return ret_value;
}

HWND WindowHandler::GetWindowHandle(void) {
  HWND h_wnd = nullptr;
  if (wc_) {
    h_wnd = wc_->hwnd_;
  }
  return h_wnd;
}

void WindowHandler::Destroy(void) {
  if (!session_id_.empty()) {
    ga::ExitGame(session_id_);
  }
  if (wc_) {
    wc_->Destroy();
  }
  if (window_handler_) {
    delete window_handler_;
	window_handler_ = nullptr;
  }
}