// Copyright (C) <2020> Intel Corporation
//
// SPDX-License-Identifier: Apache-2

#ifndef GA_PEERCONNECTION_H_
#define GA_PEERCONNECTION_H_

#include "videorenderer.h"
#include "owt/base/commontypes.h"
//#include "owt/base/connectionstats.h"
#include "owt/base/globalconfiguration.h"
#include "owt/base/logging.h"
#include "owt/base/network.h"
#include "owt/base/videorendererinterface.h"
#include "owt/p2p/p2pclient.h"
#include "rtcsignaling.h"
#include <memory>

using owt::base::GlobalConfiguration;
using owt::p2p::P2PClient;
using owt::p2p::P2PClientObserver;

class GameSession;

/// This class is the webrtc transport wrapper for gaming client.
class PeerConnection : public P2PClientObserver {
public:
  PeerConnection() : stream_started_(false) {}
  void Init(const std::string &session_token);
  void Connect(const std::string &peer_server_url,
               const std::string &session_token);
  void SetWindowHandle(HWND hwnd);
  void Start();
  void Stop();
  void SendMessage(const std::string &msg);
  void SetWindowSize(UINT x, UINT y, UINT w, UINT h) {
    dx_renderer_.SetWindowSize(x, y, w, h);
  }
  GameSession* session_;

protected:
  // PeerClientObserver impl
  void OnChatStopped(const std::string &remote_user_id) {}
  void OnChatStarted(const std::string &remote_user_id) {}
  void OnStreamAdded(std::shared_ptr<RemoteStream> stream);
  void OnStreamRemoved(std::shared_ptr<RemoteStream> stream);
  void OnDataReceived(const std::string &remote_user_id,
                      const std::string message);

private:
  std::shared_ptr<P2PClient> pc_;
  std::shared_ptr<RemoteStream> remote_stream_;
  owt::base::VideoRenderWindow render_window_;
  std::shared_ptr<P2PSignalingChannel> signaling_channel_;
  DXRenderer dx_renderer_;
  std::string remote_peer_id_;
  static long send_invoke_is_safe_;
  static long send_success_;
  bool stream_started_;
  static long send_timeout_;
  bool connection_active_;
};
#endif // GA_PEERCONNECTION_H_
