// Copyright (C) <2020> Intel Corporation
//
// SPDX-License-Identifier: Apache-2

#ifndef GA_WINDOW_HANDLER_H_
#define GA_WINDOW_HANDLER_H_

// clang-format off
#include <string>
#include <thread>
#include <windows.h>
#include "windowclass.h"
// clang-format on

class WindowHandler {
public:
  static void OnMouseStateChange(bool is_cursor_visible);
  static int OnGameServerConnected(std::string &session_id);
  static WindowHandler *GetInstance(void);
  int InitializeGameWindow(HINSTANCE h_instance, int n_cmd_show, int height,
                           int width, const char *window_title);
  HWND GetWindowHandle();
  void Destroy();
private:
  static WindowHandler* window_handler_;
  bool connected_;
  std::unique_ptr<WindowClass> wc_;
  std::string session_id_;

  WindowHandler() {}
  ~WindowHandler() {}
};

#endif // GA_WINDOW_HANDLER_H_