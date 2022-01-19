# How to build the sample

Open gawebrtcclient.sln (VS2017), Set configuration to Release|x64 and build it. Please be noted due to
github source file size limitation of 50MB for each file, the owt lib and sioclient lib under lib\x64 dir
is compressed and you need to uncompress them to the same dir before building the sample.

# How to run:
.\gawebrtcclient.exe --peer_server_url https://127.0.0.1:8096 --sessionid ga

Here peer_server_url is the signaling server address (not game server address). Port must be 8096 as we're using secure socket.io. Session id is the id of gaming server, by default it is "ga" so you don't need to change it.

client can be either started prior to server, or after server started.

# How to replace the owt binary by yourself.
The owt source tree for client is at: https://github.com/taste1981/owt-client-native/tree/cloudgaming-83-client
(be noted server is on https://github.com/taste1981/owt-client-native/tree/cloudgaming-83 instead).

To build cloudgaming-83-client branch, make sure you set below gn args (might replace the path).
````
is_clang = false
rtc_use_h264 = true
rtc_use_h265 = true
is_component_build = false
use_lld = false
use_rtti = true
rtc_include_tests = false
owt_include_tests = false
rtc_build_examples = false
rtc_enable_protobuf = false
treat_warnings_as_errors = false
target_cpu = "x64"
is_debug = false
ffmpeg_branding = "Chrome"
owt_use_openssl = true
owt_openssl_header_root = "C:\\ssl_110h_64\\include"
owt_openssl_lib_root = "C:\\ssl_110h_64\\lib"
owt_msdk_header_root = "c:\\Program Files (x86)\\IntelSWTools\\Intel(R) Media SDK 2020 R1\\Software Development Kit\\include"
owt_msdk_lib_root = "c:\\Program Files (x86)\\IntelSWTools\\Intel(R) Media SDK 2020 R1\\Software Development Kit\\lib\\x64"
````

After you build it, copy the header files to the include and libs dir of the sample, and then rebuild the sample.

# How to build the socket.io library the sample depends on
- Follow the instructions https://github.com/open-webrtc-toolkit/owt-client-native/wiki/Build-socket.io-library-for-native-SDKs.
- Make sure after the .sln file is generated, select the sioclient_tls project, right click on it and go to the "properties" page,
switch to "x64|release", and in "Configuration Properties -> C/C++ -> All Options", change "Runtime library" from default /MT" to
"/MD" which is required by the sample.

# How to build the boost library required for the sample.
- Boost build is using its seperate tool "b2". If you're going to build /MD version of boost, use this command:
````
.\b2.exe runtime-link=shared variant=release --with-system --with-date_time"
````

# How to build gflags library
- Get gflags source from https//github.com/gflags.git;
- Create a directory named "gflags_build" under the root dir of the source; Go to "gflags_build" directory.
- run `cmake ../` to build the library.

# How to build openssl library
- Get openssl 1.1.1 library source from https://www.openssl.org/source/openssl-1.1.1l.tar.gz, and follow the INSTALL file for building
this for Windows x64.