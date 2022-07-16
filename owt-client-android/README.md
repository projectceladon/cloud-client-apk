Note:
    git config --global url."https://github.com/".insteadOf git@github.com:
    git clone --recurse-submodules https://github.com/intel-innersource/os.android.cloud.aic-application.git

Preconditions:
1. Download Android SDK: https://developer.android.com/studio/intro/update#sdk-manager
2. If local.properties file with the following line doesn't exist in project root directory,  please create local.properties file and add a line
    >sdk.dir=/your/android/sdk/location

    Otherwise, the environment variables ANDROID_HOME can be used instead of local.properties file.
    >export ANDROID_HOME=/your/android/sdk/location

Download owt-client-android source code:
1. $ cd owt-client-android/src/
2. Checkout v5.0:
    $ git checkout v5.0
3. $ rm -rf  dependencies/libwebrtc/*
4. $ cp -r ../../owt-client-native/src/out/dist/release/* dependencies/libwebrtc/
5. $ git am ../patches/0001-Enable-second-data-channel-for-owt-android-sdk.patch
6. $ git am ../patches/0002-Fix-the-garbled-message-from-the-streamer-by-getting.patch
7. $ git am ../patches/0003-Remove-offerToReceiveAudio-and-offerToReceiveVideo.patch
8. $ git am ../patches/0004-Upgrade-socket.io-client-from-1.0.1-to-2.0.1.patch

Compile:
1. $ python tools/pack.py
2. $ rm -rf ../../aic-application/app/libs/*
3. $ cp dist/libs/*.aar ../../aic-application/app/libs/
4. $ cp -r dist/libs/webrtc/* ../../aic-application/app/libs/
5. $ mv ../../aic-application/app/libs/x64 ../../aic-application/app/libs/x86_64

Upload patches:
1. cd owt-client-android/src && git reset --hard v5.0 && git clean -f -x -d
2. Don't upload patch to owt-client-android/src.
3. Put your patches to aic_application/owt-client-android/patches
