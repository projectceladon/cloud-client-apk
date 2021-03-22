// p2psample.cpp : Defines the entry point for the console application.
//

#include <iostream>
#include <thread>
#include <unistd.h>
#include <string>
#include "owt/p2p/p2pclient.h"
#include "owt/base/localcamerastreamparameters.h"
#include "owt/base/stream.h"
#include "owt/base/logging.h"
#include "owt/base/globalconfiguration.h"
#include "owt/base/exception.h"
#include "p2psocketsignalingchannel.h"
#include "xwindowrenderer.h"

using namespace std;
using namespace owt::base;
using namespace owt::p2p;


class LinuxVADisplay:public VideoRendererVaInterface
{
  public:
  void RenderFrame(std::unique_ptr<VaSurface> surface);
};

void LinuxVADisplay::RenderFrame(std::unique_ptr<VaSurface> surface)
{
  std::cout<<"   LinuxVADisplay::RenderFrame Called"<<std::endl;

}

class LinuxP2PClientObserver:public P2PClientObserver
{
   public:
   void OnStreamAdded(std::shared_ptr<owt::base::RemoteStream> stream);
};

XWindowRenderer videoRender;
void LinuxP2PClientObserver::OnStreamAdded(std::shared_ptr<owt::base::RemoteStream> stream)
{
  std::cout<<"*****OnStreamAdded called*****"<<std::endl;
  stream->AttachVideoRenderer(videoRender);
}

int main(int argc, char** argv)
{
  owt::base::LoggingSeverity(owt::base::LoggingSeverity::kError);
  //owt::base::Logging::LogToConsole(owt::base::LoggingSeverity::kError);
   //std::string dir("/home/ylin15/webrtc-client/logs");
 // owt::base::Logging::LogToFileRotate(owt::base::LoggingSeverity::kError, dir, 10*1024*1024);
  std::shared_ptr<P2PSignalingChannelInterface> signaling_channel(new P2PSocketSignalingChannel());
  //GlobalConfiguration::SetEncodedVideoFrameEnabled(false);
  GlobalConfiguration::SetVideoHardwareAccelerationEnabled(true);
  P2PClientConfiguration configuration;
  LinuxP2PClientObserver p2pClientObserver;

  AudioCodecParameters audio_param;
  audio_param.name = AudioCodec::kOpus;
  AudioEncodingParameters audio_encoding_param(audio_param, 0);
  configuration.audio_encodings.push_back(audio_encoding_param);
  VideoCodecParameters video_param;
  video_param.name = VideoCodec::kH264;
  VideoEncodingParameters video_encoding_param(video_param, 0, false);
  configuration.video_encodings.push_back(video_encoding_param);

  std::shared_ptr<P2PClient> pc(new P2PClient(configuration, signaling_channel));
  pc->AddObserver(p2pClientObserver);
 // cout << "Press Enter to connect peerserver." << endl;
 // cin.ignore();

  /*
  string url = argv[1];
  string from = argv[2];
  string to = argv[3];
  */

  //string url = "http://192.168.3.15:8095";
  string url = "http://192.168.1.86:8095";
  //string url = "http://10.239.141.115:8095";
  string to = "ga";
  pc->Connect(url, "client", nullptr, nullptr);
  cout << "Press Enter to publish stream to remote user " << to << endl;
  //cin.ignore();
  std::thread([&pc, &to]() {
    pc->AddAllowedRemoteId(to);
    pc->Send("ga", "start", []() {
      cout << "Send success................................." << endl;
    }, [](std::unique_ptr<owt::base::Exception>){
      cout << "Send failure................................." << endl;
    });
  }).detach();

  while (true) {
    sleep(1);
  }

    

cout << "Press Enter to exit." << endl;
  cin.ignore();
  return 0;
}

