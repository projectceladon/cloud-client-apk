// Copyright (C) <2020> Intel Corporation
//
// SPDX-License-Identifier: Apache-2

// clang-format off
#include "gaoption.h"
#include "gamesession.h"
#include <windows.h>
#include "controlhandler.h"
// clang-format on

// utility helper functions
void PopulateCommonMouseOptionsLegacy(MouseOptions *ptr_m_options,
  LPARAM l_param);
bool PopulateCommonMouseOptionsRaw(MouseOptions *ptr_m_options,
  RAWINPUT *p_raw_input);
DWORD GetInputTypeFromRawInput(LPARAM l_param);

// globals for getting raw input
static const int kRawInputSize = 1024;
static char g_rawinput[kRawInputSize];

std::unique_ptr<GameSession> g_remote_connection;

int ga::StartGame(const SessionMetaData &session_opts, const ClientSettings client_opts) {
  int ret_code = 0;
  g_remote_connection.reset(new GameSession);
  g_remote_connection->ConfigConnection(session_opts, client_opts);
  g_remote_connection->ConnectPeerServer();
  return ret_code;
}

void ga::SendInputToRemote(UINT input_message, WPARAM w_param,
  LPARAM l_param) {
  std::string m;
  KeyboardOptions key_options;
  MouseOptions m_options;
  DWORD raw_input_type;
  RAWINPUT *p_raw;

  switch (input_message) {
  case WM_KEYDOWN:
  case WM_KEYUP:
    key_options.msg_ = input_message;
    key_options.v_key_ = w_param;
    g_remote_connection->SendKeyboardEvent(&key_options);
    break;

  case WM_INPUT:
    raw_input_type = GetInputTypeFromRawInput(l_param);
    if (raw_input_type == (DWORD)RIM_TYPEMOUSE) {
      p_raw = (RAWINPUT *)g_rawinput;
      if (PopulateCommonMouseOptionsRaw(&m_options, p_raw)) {
        g_remote_connection->SendMouseEvent(&m_options, true);
      }
    } else if (raw_input_type == (DWORD)RIM_TYPEKEYBOARD) {
      p_raw = (RAWINPUT *)g_rawinput;
      key_options.msg_ = p_raw->data.keyboard.Message;
      key_options.v_key_ = p_raw->data.keyboard.VKey;
      g_remote_connection->SendKeyboardEvent(&key_options);
    }
    break;
  case WM_MOUSEMOVE:
    PopulateCommonMouseOptionsLegacy(&m_options, l_param);
    m_options.m_event_ = kMouseMove;
    g_remote_connection->SendMouseEvent(&m_options, false);
    break;
  case WM_LBUTTONUP:
    PopulateCommonMouseOptionsLegacy(&m_options, l_param);
    m_options.m_event_ = kMouseLeftButton;
    m_options.m_button_state_ = kMouseButtonUp;
    g_remote_connection->SendMouseEvent(&m_options, false);
    break;
  case WM_LBUTTONDOWN:
    PopulateCommonMouseOptionsLegacy(&m_options, l_param);
    m_options.m_event_ = kMouseLeftButton;
    m_options.m_button_state_ = kMouseButtonDown;
    g_remote_connection->SendMouseEvent(&m_options, false);
    break;
  case WM_MBUTTONUP:
    PopulateCommonMouseOptionsLegacy(&m_options, l_param);
    m_options.m_event_ = kMouseMiddleButton;
    m_options.m_button_state_ = kMouseButtonUp;
    g_remote_connection->SendMouseEvent(&m_options, false);
    break;
  case WM_MBUTTONDOWN:
    PopulateCommonMouseOptionsLegacy(&m_options, l_param);
    m_options.m_event_ = kMouseMiddleButton;
    m_options.m_button_state_ = kMouseButtonDown;
    g_remote_connection->SendMouseEvent(&m_options, false);
    break;
  case WM_RBUTTONUP:
    PopulateCommonMouseOptionsLegacy(&m_options, l_param);
    m_options.m_event_ = kMouseRightButton;
    m_options.m_button_state_ = kMouseButtonUp;
    g_remote_connection->SendMouseEvent(&m_options, false);
    break;
  case WM_RBUTTONDOWN:
    PopulateCommonMouseOptionsLegacy(&m_options, l_param);
    m_options.m_event_ = kMouseRightButton;
    m_options.m_button_state_ = kMouseButtonDown;
    g_remote_connection->SendMouseEvent(&m_options, false);
    break;
  case WM_MOUSEWHEEL:
    PopulateCommonMouseOptionsLegacy(&m_options, l_param);
    m_options.m_event_ = kMouseWheel;
    m_options.delta_y_ = GET_WHEEL_DELTA_WPARAM(w_param);
    g_remote_connection->SendMouseEvent(&m_options, false);
    break;
  default:
    break;
  }
}

void PopulateCommonMouseOptionsLegacy(MouseOptions *ptr_m_options,
  LPARAM l_param) {
  ptr_m_options->x_pos_ = l_param & 0xFFFF;
  ptr_m_options->y_pos_ = (l_param >> 16) & 0xFFFF;
  ptr_m_options->is_cursor_relative_ = 0;
}

DWORD GetInputTypeFromRawInput(LPARAM l_param) {
  UINT dwSize;
  RAWINPUT *raw;

  GetRawInputData((HRAWINPUT)l_param, RID_INPUT, NULL, &dwSize,
    sizeof(RAWINPUTHEADER));
  if (GetRawInputData((HRAWINPUT)l_param, RID_INPUT, g_rawinput, &dwSize,
    sizeof(RAWINPUTHEADER)) != dwSize) {
  }

  raw = (RAWINPUT *)g_rawinput;
  return (raw->header.dwType);
}

bool PopulateCommonMouseOptionsRaw(MouseOptions *ptr_m_options,
  RAWINPUT *p_raw_input) {

  bool ret_value = TRUE;
  ptr_m_options->is_cursor_relative_ = 1;
  ptr_m_options->x_pos_ = p_raw_input->data.mouse.lLastX;
  ptr_m_options->y_pos_ = p_raw_input->data.mouse.lLastY;
  switch (p_raw_input->data.mouse.usButtonFlags) {
  case RI_MOUSE_LEFT_BUTTON_DOWN:
    ptr_m_options->m_event_ = kMouseLeftButton;
    ptr_m_options->m_button_state_ = kMouseButtonDown;
    break;
  case RI_MOUSE_LEFT_BUTTON_UP:
    ptr_m_options->m_event_ = kMouseLeftButton;
    ptr_m_options->m_button_state_ = kMouseButtonUp;
    break;
  case RI_MOUSE_MIDDLE_BUTTON_DOWN:
    ptr_m_options->m_event_ = kMouseMiddleButton;
    ptr_m_options->m_button_state_ = kMouseButtonDown;
    break;
  case RI_MOUSE_MIDDLE_BUTTON_UP:
    ptr_m_options->m_event_ = kMouseMiddleButton;
    ptr_m_options->m_button_state_ = kMouseButtonUp;
    break;
  case RI_MOUSE_RIGHT_BUTTON_DOWN:
    ptr_m_options->m_event_ = kMouseRightButton;
    ptr_m_options->m_button_state_ = kMouseButtonDown;
    break;
  case RI_MOUSE_RIGHT_BUTTON_UP:
    ptr_m_options->m_event_ = kMouseRightButton;
    ptr_m_options->m_button_state_ = kMouseButtonUp;
    break;
  case RI_MOUSE_BUTTON_4_DOWN:
  case RI_MOUSE_BUTTON_4_UP:
  case RI_MOUSE_BUTTON_5_DOWN:
  case RI_MOUSE_BUTTON_5_UP:
    ret_value = FALSE;
    break;
  case RI_MOUSE_WHEEL:
    ptr_m_options->m_event_ = kMouseWheel;
    ptr_m_options->delta_y_ = p_raw_input->data.mouse.usButtonData;
    break;
  default:
    if (ptr_m_options->x_pos_ != 0 || ptr_m_options->y_pos_ != 0) {
      ptr_m_options->m_event_ = kMouseMove;
    }
    else {
      ret_value = FALSE;
    }
    break;
  }
  return ret_value;
}


int ga::ExitGame(const string &session_id) {
  int err_code = 0;
  err_code = g_remote_connection->StopConnection();
  return err_code;
}

void ga::SetWindowSize(UINT x_offset, UINT y_offset, UINT width,
  UINT height) {
  g_remote_connection->SetWindowSize(x_offset, y_offset, width, height);
}