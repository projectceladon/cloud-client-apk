// Copyright (C) <2020> Intel Corporation
//
// SPDX-License-Identifier: Apache-2


#ifndef GA_OPTION_H_
#define GA_OPTION_H_

#include <functional>
#include <string>
#include <vector>
#include <windows.h>

using std::function;
using std::string;

namespace ga {
  typedef function<int(string &)> ConnectionCallback;
  typedef function<void(bool is_visible)> MouseStateCallback;

  struct SessionMetaData {
    string session_id_;
    string peer_server_url_;
  };

  // All options for starting the client. Provides interfaces for client application
  // to register all neccessary callbacks to handle audio/video and register callbacks
  // for delivering input, as well as cursor things, etc.
  struct ClientSettings {
    HWND hwnd_;
    UINT width_;
    UINT height_;
    ConnectionCallback connection_callback_;
    MouseStateCallback mousestate_callback_;
  };
  int StartGame(const SessionMetaData &session_opts, const ClientSettings opts);
  int ExitGame(const string &sessionid);
  void SendInputToRemote(UINT input_message, WPARAM w_param, LPARAM l_param);
  void SetWindowSize(UINT x_offset, UINT y_offset, UINT width, UINT height);
} // namespace ga

#endif GA_OPTION_H_