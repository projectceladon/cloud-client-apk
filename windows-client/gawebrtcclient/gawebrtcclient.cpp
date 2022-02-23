// Copyright (C) <2020> Intel Corporation
//
// SPDX-License-Identifier: Apache-2

#include <iostream>
#include <windows.h>
#include <stdint.h>
#include "gflags/gflags.h"
#include "windowhandler.h"
#include "gaoption.h"
#include <map>

// peer_server_url: must be specified to indicate the p2p siganling server(not
// gaming server) to connect to.
DEFINE_string(peer_server_url, "",
  "Peer server to be connected to by native client.");
// sessionid: must be specified. specifies the user id of gaming server side.
DEFINE_string(sessionid, "",
  "ID used by game server to login to peer server.");
DEFINE_string(clientsessionid, "",
    "ID used by game client to login to peer server.");
// default window width
DEFINE_int32(w, 512, "game window width");
//DEFINE_int32(w, 1280, "game window width");
// default window height
DEFINE_int32(h, 288, "game window height");
//DEFINE_int32(h, 720, "game window height");

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance,
  LPSTR lpCmdLine, int nCmdShow) {
  MSG Msg;
  ga::SessionMetaData session_info;
  ga::ClientSettings client_settings;
  WindowHandler *window_handler = nullptr;

  gflags::ParseCommandLineFlags(&__argc, &__argv, true);
  session_info.peer_server_url_ = FLAGS_peer_server_url;
  session_info.session_id_ = FLAGS_sessionid;
  session_info.client_session_id_ = FLAGS_clientsessionid;

  client_settings.mousestate_callback_ = WindowHandler::OnMouseStateChange;
  client_settings.connection_callback_ = WindowHandler::OnGameServerConnected;

  if ((window_handler = WindowHandler::GetInstance()) == nullptr) {
    goto Done;
  }

  if (window_handler->InitializeGameWindow(hInstance, nCmdShow, FLAGS_h, 
    FLAGS_w, "GaWebRTCClient") != 0) {
    goto Done;
  }

  client_settings.hwnd_ = window_handler->GetWindowHandle();
  client_settings.width_ = FLAGS_w;
  client_settings.height_ = FLAGS_h;

  ga::StartGame(session_info, client_settings);

  while (GetMessage(&Msg, NULL, 0, 0) > 0) {
    TranslateMessage(&Msg);
    DispatchMessage(&Msg);
  }

Done:
  if (window_handler) {
    window_handler->Destroy();
  }
  return Msg.wParam;
}
