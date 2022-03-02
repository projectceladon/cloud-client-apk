## Install SDL

1. tar -zxvf SDL2-2.0.20.tar.gz
2. cd SDL2-2.0.20
3. ./configure
4. make
5. sudo make install


## How to build owt

1. git clone source code of owt game streaming sdk
2. python scripts/build_linux.py --gn_gen --sdk --ssl_root openssl_dir --output_path dist --fake_audio --scheme release --arch x64


## How to build:

1. mkdir build
2. cd build
3. cmake ..
4. make


## How to Run:

1. cd build
2. export LD_LIBRARY_PATH=../lib
3. ./owt_linux_client 10.112.240.116 0 1280 720 h265
