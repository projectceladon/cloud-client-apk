#include "AudioPlayer.h"

AudioPlayer::AudioPlayer() { init(); }

AudioPlayer::~AudioPlayer() {
#ifdef USE_SDL
  if (auddev_ > 0) {
    SDL_CloseAudioDevice(auddev_);
  }
#else
  snd_pcm_drain(handle_);
  snd_pcm_close(handle_);
#endif
}

void AudioPlayer::init() {
#ifdef USE_SDL
  std::cout << "play audio by SDL" << std::endl;
  SDL_AudioSpec desired_spec;
  desired_spec.freq = 48000;
  desired_spec.format = AUDIO_S16SYS;
  desired_spec.channels = 2;
  desired_spec.silence = 0;
  desired_spec.samples = 480;
  desired_spec.callback = NULL;

  if ((auddev_ = SDL_OpenAudioDevice(NULL, 0, &desired_spec, NULL,
                                     SDL_AUDIO_ALLOW_ANY_CHANGE)) < 2) {
    std::cout << "SDL_OpenAudioDevice with error deviceID: " << auddev_
              << std::endl;
    return;
  }

  SDL_PauseAudioDevice(auddev_, 0);
#else
  std::cout << "play audio directly" << std::endl;
  int rc = snd_pcm_open(&handle_, "default", SND_PCM_STREAM_PLAYBACK, 0);
  if (rc < 0) {
    std::cout << "open PCM device failed" << std::endl;
    return;
  }

  snd_pcm_hw_params_alloca(&params_);
  snd_pcm_hw_params_any(handle_, params_);
  snd_pcm_hw_params_set_access(handle_, params_, SND_PCM_ACCESS_RW_INTERLEAVED);
  snd_pcm_hw_params_set_format(handle_, params_, SND_PCM_FORMAT_S16_LE);
  snd_pcm_hw_params_set_channels(handle_, params_, 2);

  int dir = 0;
  unsigned int frequency = 48000;
  rc = snd_pcm_hw_params_set_rate_near(handle_, params_, &frequency, &dir);

  snd_pcm_uframes_t frame_count = 480;
  snd_pcm_hw_params_set_period_size_near(handle_, params_, &frame_count, &dir);
  snd_pcm_hw_params(handle_, params_);
  snd_pcm_hw_params_get_period_size(params_, &frames_, &dir);
#endif
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

#ifdef USE_SDL
  if (auddev_ > 0) {
    SDL_QueueAudio(auddev_, audio_data,
                   number_of_frames * number_of_channels * bits_per_sample / 8);
  }
#else
  int rc = snd_pcm_writei(handle_, audio_data, frames_);
  if (rc == -EPIPE) {
    std::cout << "underrun occurred" << std::endl;
    snd_pcm_prepare(handle_);
  } else if (rc < 0) {
    std::cout << "error from writei: " << snd_strerror(rc) << std::endl;
  } else if (rc != (int)frames_) {
    std::cout << "short write, write " << rc << " frames" << std::endl;
  }
#endif
}
