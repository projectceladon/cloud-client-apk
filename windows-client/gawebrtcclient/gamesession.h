
#ifndef GA_REMOTE_CONECTION_HANDLER_H_
#define GA_REMOTE_CONECTION_HANDLER_H_

#include "peerconnection.h"
#include "gaoption.h"
#include "stdlib.h"
#include "controlhandler.h"
#include <memory>

class GameSession {
public:
  explicit GameSession();
  void OnDataReceivedHandler(const std::string &message);
  int OnServerConnected(string &session_id);
  void ConfigConnection(const ga::SessionMetaData &opts, const ga::ClientSettings launch_options);
  void SendMouseEvent(MouseOptions *p_m_options, bool is_raw);
  void SendKeyboardEvent(KeyboardOptions *p_key_options);
  int ConnectPeerServer();
  int StopConnection(void);
  void SetWindowSize(UINT x_offset, UINT y_offset, UINT width, UINT height);

private:
  std::string session_id_;
  std::string client_session_id_;
  std::string peer_server_url_;
  std::unique_ptr<PeerConnection> pc_;
  ga::ClientSettings connect_settings_;
};
#endif