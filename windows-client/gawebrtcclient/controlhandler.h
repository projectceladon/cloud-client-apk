// Copyright (C) <2020> Intel Corporation
//
// SPDX-License-Identifier: Apache-2

#ifndef GA_CONTROL_HANDLER_H_
#define GA_CONTROL_HANDLER_H_

#include <map>
#include <string>
#include <windows.h>

#define GA_LEGACY_INPUT 1
#define GA_RAW_INPUT 2

enum MouseEvent {
  kMouseMove = 0,
  kMouseLeftButton = 1,
  kMouseMiddleButton = 2,
  kMouseRightButton = 3,
  kMouseWheel = 4
};

enum MouseButtonState {
  kMouseButtonUp = 1,
  kMouseButtonDown = 2 
};

struct KeyboardOptions {
  WPARAM v_key_;
  UINT msg_;
};

struct MouseOptions {
  int x_pos_;
  int y_pos_;
  int delta_x_;
  int delta_y_;
  int delta_z_;
  int is_cursor_relative_;
  MouseEvent m_event_;
  MouseButtonState m_button_state_;
};

class InputEventHandler {
public:
  static std::string OnKeyboardEvent(KeyboardOptions *p_k_options);
  static std::string OnMouseEvent(MouseOptions *p_m_options, bool is_raw);
};

#endif // GA_CONTROL_HANDLER_H_