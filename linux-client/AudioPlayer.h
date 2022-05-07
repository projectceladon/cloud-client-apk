#ifndef _AUDIO_PLAYER_H
#define _AUDIO_PLAYER_H

#include <string>
#include <iostream>
#include <unistd.h>
#include <SDL2/SDL.h>
#include "owt/base/audioplayerinterface.h"

class AudioPlayer : public owt::base::AudioPlayerInterface {
public:
  AudioPlayer();
  virtual ~AudioPlayer();
  void OnData(const void *audio_data,
              int bits_per_sample,
              int sample_rate,
              size_t number_of_channels,
              size_t number_of_frames) override;
private:
  void init();

private:
  SDL_AudioDeviceID auddev_ = 0;
};

#endif
