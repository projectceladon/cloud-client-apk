Note:
    git clone --recurse-submodules ssh://git@gitlab.devtools.intel.com:29418/android-cloud/aic_application.git

Preconditions:
1. Download Android SDK: https://developer.android.com/studio/intro/update#sdk-manager
2. Set the environment variables ```ANDROID_HOME```:

    >`export ANDROID_HOME=/your/android/sdk/location`

Download owt-client-native source code:
1. $ cd owt-client-android/src/
2. Checkout v5.0:
    $ git checkout -b v5.0-local v5.0
3. $ rm -rf  dependencies/libwebrtc/*
4. $ cp -r ../../owt-client-native/src/out/dist/release/* dependencies/libwebrtc/

Compile:
1. $ python tools/pack.py

Upload patches:
1. cd owt-client-native/src && git reset --hard e508a131d24c3f89d7863116cac217cd29aded92
2. Don't upload patch to owt-client-native/src.
3. Put your patches to aic_application/owt-client-android/patches
