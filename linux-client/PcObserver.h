#ifndef _PC_OBSERVER_H
#define _PC_OBSERVER_H

#include "owt/base/audioplayerinterface.h"
#include "owt/p2p/p2pclient.h"
#include <iostream>

class PcObserver : public owt::p2p::P2PClientObserver {
public:
  PcObserver():direct_render(true) {}
  PcObserver(std::shared_ptr<VideoRenderer> renderer):renderer_(renderer), direct_render(false) {}
  virtual ~PcObserver() {}

  void OnMessageReceived(const std::string& remote_user_id,
                         const std::string message) {
    std::cout << __func__ << ":from" << remote_user_id << ", msg:" << message << std::endl;
  }

  virtual void OnStreamAdded(std::shared_ptr<owt::base::RemoteStream> stream) override {
    if (!direct_render) {
      VideoRenderer* render = renderer_.get();
      stream->AttachVideoRenderer(*render);
    }
  }

  void OnServerDisconnected() {
    std::cout << __func__ << ":" << std::endl;
  }

private:
  std::shared_ptr<VideoRenderer> renderer_;
  bool direct_render;
};
#endif