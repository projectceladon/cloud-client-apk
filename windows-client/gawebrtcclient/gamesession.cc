// Copyright (C) <2020> Intel Corporation
//
// SPDX-License-Identifier: Apache-2

// clang-format off
#include "gflags/gflags.h"
#include "rapidjson/document.h"
#include "rapidjson/stringbuffer.h"
#include "rapidjson/writer.h"
#include "gamesession.h"
#include "peerconnection.h"

// clang-format on
GameSession::GameSession() {
  pc_.reset(new PeerConnection());
}

void GameSession::SendMouseEvent(MouseOptions *p_m_options,
                                                     bool is_raw) {
  std::string m;
  m = InputEventHandler::OnMouseEvent(p_m_options, is_raw);
  pc_->SendMessage(m);
}

void GameSession::SendKeyboardEvent(
    KeyboardOptions *p_key_options) {
  std::string m;
  m = InputEventHandler::OnKeyboardEvent(p_key_options);
  pc_->SendMessage(m);
}

int GameSession::ConnectPeerServer() {
  pc_->Init(session_id_);
  pc_->SetWindowHandle(connect_settings_.hwnd_);
  pc_->SetWindowSize(0, 0, connect_settings_.width_,
                       connect_settings_.height_);
  pc_->Connect(peer_server_url_, session_id_, client_session_id_);
  pc_->Start();
  return 0;
}

void GameSession::ConfigConnection(const ga::SessionMetaData &session_info,
  const ga::ClientSettings client_settings) {
  int error_code = 0;
  peer_server_url_ = session_info.peer_server_url_;
  session_id_ = session_info.session_id_;
  client_session_id_ = session_info.client_session_id_;
  connect_settings_.mousestate_callback_ = client_settings.mousestate_callback_;
  connect_settings_.connection_callback_ = client_settings.connection_callback_;
  connect_settings_.hwnd_ = client_settings.hwnd_;
  connect_settings_.width_ = client_settings.width_;
  connect_settings_.height_ = client_settings.height_;
  pc_->session_ = this;
}

int GameSession::OnServerConnected(string &game_session_id) {
  int err_num = 0;
  if (connect_settings_.connection_callback_) {
    err_num = connect_settings_.connection_callback_(game_session_id);
  }
  else {
  }
  return err_num;
}

void GameSession::OnDataReceivedHandler(
  const std::string &message) {
  rapidjson::Document msg;
  msg.Parse(message.c_str());
  if (msg.HasMember("cursor_valid")) {
    auto visible = msg["cursor_valid"].GetInt();
    if (connect_settings_.mousestate_callback_) {
      connect_settings_.mousestate_callback_(visible);
    }
    else {
    }
  }
  if (msg.HasMember("show_cursor")) {
    auto visible = msg["show_cursor"].GetInt();
    if (connect_settings_.mousestate_callback_) {
      connect_settings_.mousestate_callback_(visible);
    }
    else {
    }
  }
}

int GameSession::StopConnection(void) {
  pc_->Stop();
  return 0;
}

void GameSession::SetWindowSize(UINT x_offset, UINT y_offset,
                                            UINT width, UINT height) {
  pc_->SetWindowSize(x_offset, y_offset, width, height);
}