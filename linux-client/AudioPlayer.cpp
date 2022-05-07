#include "AudioPlayer.h"

AudioPlayer::AudioPlayer() {
  init();
}

AudioPlayer::~AudioPlayer() {
  if (auddev_ > 0) {
    SDL_CloseAudioDevice(auddev_);
  }
}

void AudioPlayer::init() {
  SDL_AudioSpec desired_spec;
  desired_spec.freq = 48000;
  desired_spec.format = AUDIO_S16SYS;
  desired_spec.channels = 2;
  desired_spec.silence = 0;
  desired_spec.samples = 480;
  desired_spec.callback = NULL;

  if ((auddev_ = SDL_OpenAudioDevice(NULL, 0, &desired_spec, NULL, SDL_AUDIO_ALLOW_ANY_CHANGE)) < 2) {
    std::cout << "SDL_OpenAudioDevice with error deviceID: " << auddev_ << std::endl;
    return;
  }

  SDL_PauseAudioDevice(auddev_, 0);
}

void AudioPlayer::OnData(const void *audio_data, int bits_per_sample,
                         int sample_rate, size_t number_of_channels,
                         size_t number_of_frames) {
  /*
  std::cout << __func__ << ":"
            << " bits:" << bits_per_sample
            << ",sample rate:" << sample_rate
            << ",channels:" << number_of_channels
            << ",frames:" << number_of_frames
            << std::endl;
  */

  if (auddev_ > 0) {
    SDL_QueueAudio(auddev_, audio_data, number_of_frames * number_of_channels * bits_per_sample / 8);
  }
}
