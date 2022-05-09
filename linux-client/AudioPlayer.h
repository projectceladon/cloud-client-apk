#ifndef _AUDIO_PLAYER_H
#define _AUDIO_PLAYER_H

#include <string>
#include <iostream>
#include <unistd.h>
#ifdef USE_SDL
#include <SDL2/SDL.h>
#else
#include <alsa/asoundlib.h>
#endif
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
#ifdef USE_SDL
  SDL_AudioDeviceID auddev_ = 0;
#else
  snd_pcm_t* handle_;
  snd_pcm_hw_params_t* params_;
  snd_pcm_uframes_t frames_;
#endif
};

#endif
