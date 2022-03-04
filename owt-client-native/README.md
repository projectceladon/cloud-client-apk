Note:
    git clone --recurse-submodules ssh://git@gitlab.devtools.intel.com:29418/android-cloud/aic_application.git

Preconditions:
1. Install Ubuntu (E.g: 14.04, 16.04, 18.04, 19.04, 19.10).
2. Download depot_tools to your $HOME: $ cd ~ && git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
3. Export path in your .bashrc: export PATH=$PATH:$HOME/depot_tools
4. Create boto for proxy:
    $ cat ~/.boto 
    [Boto]
    proxy = <URL>
    proxy_port = <port>
    https_validate_certificate = True
5. Export NO_AUTH_BOTO_CONFIG in your .bashrc: NO_AUTH_BOTO_CONFIG=$HOME/.boto

Download owt-client-native source code:
1. $ cd owt-client-native/src
2. Checkout v5.0:
    $ git checkout v5.0
3. Fetch dependency projects: $ gclient sync
4. Apply patch one by one:
    $ git am -3 ../patches/0001-Add-talk-owt-patches-0015-Adopt-SingletonSurfaceView.patch
    $ git am -3 ../patches/0002-Add-talk-owt-patches-0016-Use-AToU-to-print-trace.pa.patch
    $ git am -3 ../patches/0003-Add-talk-owt-patches-0017-Support-OMX.Intel.hw_ve.h2.patch
    $ git am -3 ../patches/0004-Add-talk-owt-patches-0018-Support-OMX.Intel.hw_ve.h2.patch
    $ git am -3 ../patches/0005-Add-talk-owt-patches-0019-Support-h264-and-hevc-of-M.patch
    $ git am -3 ../patches/0006-Add-talk-owt-patches-0020-Add-atrace-points-for-came.patch
    $ git am -3 ../patches/0007-Remove-talk-owt-patches-0015-Adopt-SingletonSurfaceV.patch
    $ git am -3 ../patches/0008-Add-and-update-talk-owt-patches-0021-Fix-display-is-.patch
    $ git am -3 ../patches/0009-Add-talk-owt-patches-0022-Implemented-the-new-transp.patch
    $ git am -3 ../patches/0010-Add-talk-owt-patches-0023-Add-atrace-point-that-is-a.patch
    $ git am -3 ../patches/0011-Alpha-transparent-is-triggered-by-the-change-of-buff.patch
    $ git am -3 ../patches/0012-Add-talk-owt-patches-0024-Enable-TCAE-in-webrtc.patc.patch
5. Patch the third_party project: $ gclient sync

Compile:
1. Install dependency: $ sudo ./build/install-build-deps-android.sh
2. $ ./scripts/build_android.py

Upload patches:
1. cd owt-client-native/src && git reset --hard v5.0 && git clean -f -x -d
2. Don't upload patch to owt-client-native/src.
3. Put your patches to aic_application/owt-client-native/patches
