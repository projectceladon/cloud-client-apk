#include <iostream>
#include <algorithm>
#include "p2psocketsignalingchannel.h"
using namespace owt::p2p;

P2PSocketSignalingChannel::P2PSocketSignalingChannel()
    : io_(new sio::client()) {}

void P2PSocketSignalingChannel::AddObserver(
    P2PSignalingChannelObserver& observer) {
  observers_.push_back(&observer);
}

void P2PSocketSignalingChannel::RemoveObserver(
    P2PSignalingChannelObserver& observer) {
  observers_.erase(remove(observers_.begin(), observers_.end(), &observer),
                   observers_.end());
}

void P2PSocketSignalingChannel::Connect(
    const std::string& host,
    const std::string& token,
    std::function<void(const std::string&)> on_success,
    std::function<void(std::unique_ptr<Exception>)> on_failure) {
  std::map<std::string, std::string> query;
  query.insert(std::pair<std::string, std::string>("clientVersion", "4.2"));
  query.insert(std::pair<std::string, std::string>("clientType", "cpp"));
  query.insert(std::pair<std::string, std::string>("token", token));  // TODO: parse token to get actual token.
  sio::socket::ptr socket = io_->socket();
  std::string name = "owt-message";
  socket->on(
      name, sio::socket::event_listener_aux([&](
                std::string const& name, sio::message::ptr const& data,
                bool has_ack, sio::message::list& ack_resp) {
        if (data->get_flag() == sio::message::flag_object) {
          std::string msg = data->get_map()["data"]->get_string().data();
          std::string from = data->get_map()["from"]->get_string().data();
          for (auto it = observers_.begin(); it != observers_.end(); ++it) {
            (*it)->OnSignalingMessage(msg, from);
          };
        }
      }));
  io_->connect(host, query);
}

void P2PSocketSignalingChannel::Disconnect(std::function<void()> on_success,
                                           std::function<void(
                                               std::unique_ptr<Exception>)>
                                               on_failure) {}

void P2PSocketSignalingChannel::SendMessage(
    const std::string& message,
    const std::string& target_id,
    std::function<void()> on_success,
    std::function<void(std::unique_ptr<Exception>)> on_failure) {
  sio::message::ptr jsonObject = sio::object_message::create();
  jsonObject->get_map()["to"] = sio::string_message::create(target_id);
  jsonObject->get_map()["data"] = sio::string_message::create(message);
  io_->socket()->emit("owt-message", jsonObject,
                      [=](const sio::message::list& msg) {
                        if (on_success) {
                          on_success();
                        }
                      });
}
