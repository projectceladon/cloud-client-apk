// Copyright (C) <2020> Intel Corporation
//
// SPDX-License-Identifier: Apache-2

#ifndef GA_WINDOWCLASS_H_
#define GA_WINDOWCLASS_H_

#include <Windows.h>
#include <Windowsx.h>
#include <string>

#define WM_GA_CURSOR_VISIBLE (WM_USER + 1)
#define GA_HIDE_CURSOR 0
#define GA_SHOW_CURSOR 1

class WindowClass {
public:
  HWND hwnd_;
  UINT client_window_width_;
  UINT client_window_height_;

  WindowClass(HINSTANCE h_instance, int n_cmd_show, int height, int width,
              const char *window_title);
  void Destroy(void);
private:
  static LRESULT CALLBACK PreInitWndProc(HWND h_wnd, UINT msg, WPARAM w_param, LPARAM l_param);
  static LRESULT CALLBACK PostInitWndProc(HWND h_wnd, UINT msg, WPARAM w_param, LPARAM l_param);
  LRESULT CALLBACK InternalWndProc(HWND h_wnd, UINT msg, WPARAM w_param, LPARAM l_param);
  void RegisterRawInput(HWND hwnd);
  void UnregisterRawInput(void);
  void ChangeGameMode(HWND hwnd, bool enable);
  void ChangeWindowedMode(HWND hwnd, bool enable_fullscreen);
  WNDCLASSEX wc_;
  float scale_ratio_w_;
  float scale_ratio_h_;
  bool full_screen_toggle_;
  bool game_mode_toggle_;
  bool in_sys_key_down_;
  UINT initial_window_width_;
  UINT initial_window_height_;
  RECT window_rect_;
  UINT x_render_offset_ = 0;
  UINT y_render_offset_ = 0;

};
#endif // GA_WINDOWCLASS_H_