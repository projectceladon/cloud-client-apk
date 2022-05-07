### Install needed software

please use Ubuntu 20.04

sudo apt-get install build-essential libgl1-mesa-dev libva-dev libdrm-dev libglew-dev libsdl2-dev libsdl2-image-dev libglm-dev libfreetype6-dev libx11-dev libxext-dev libxtst-dev libxrender-dev libxmu-dev libxmuu-dev libsdl2-ttf-dev libasound2-dev libpulse-dev



### How to build aic linux client:

1. mkdir build
2. cd build
3. cmake ..
4. make

### How to Run:

1. cd build
2. export LD_LIBRARY_PATH=../lib
3. cp ../SourceSansPro-Regular.ttf .
4. ulimit -Sn 4096
5. ./aic_linux_client -u http://10.239.93.57:8095 -s s0 -c c0 -r 1280x720 -v h265 -w 640x360 -d hw
   for more than one stream, run
   ./aic_linux_client -u http://10.239.93.57:8095 -s s0-4 -c c0-4 -r 1280x720 -v h264 -d sw



### How to build prebuilt binaries if you need them:

## SDL
1. tar -zxvf SDL2-2.0.20.tar.gz
2. cd SDL2-2.0.20
3. ./configure
4. make
5. sudo make install


## Owt game streaming sdk
1. git clone https://github.com/intel-innersource/libraries.communications.webrtc.owt-sdk-game-streaming.git
2. prepare source code according to README.md in owt game streaming sdk.
3. apply owt-patches/0001-decoded-frame-to-render-on-linux.patch to src
4. python scripts/build_linux.py --gn_gen --sdk --ssl_root openssl_dir --output_path dist --fake_audio --scheme release --arch x64


## ffmpeg
1. git clone https://git.ffmpeg.org/ffmpeg.git ffmpeg
2. cd ffmpeg
3. git checkout release/4.2
4. mkdir dist
5. PKG_CONFIG_PATH=/opt/intel/mediasdk/lib/pkgconfig/ ./configure --prefix=./dist --target-os=linux --arch=x86_64 --disable-postproc --disable-devices --disable-vdpau --disable-cuda --disable-cuvid --disable-videotoolbox --disable-audiotoolbox --disable-sdl2 --disable-nvenc --disable-lzma --enable-shared --extra-cflags=-fPIC --enable-gpl --enable-libdrm
6. make -j8
7. make install
