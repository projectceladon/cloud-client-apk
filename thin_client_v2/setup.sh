#Install ubuntu 18.04
export PrefixPath=/usr
export LibPath=/usr/lib/x86_64-linux-gnu
export nproc=20
export WrkDir=`pwd`
#!/bin/bash
RED='\033[0;31m'
NC='\033[0m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'

#Media Packages
function check_network(){
	echo "Network checking"
        wget --timeout=3 --tries=1 https://github.com/projectceladon/ -q -O /dev/null
        if [ $? -ne 0 ]; then
		echo -e "${RED}access https://github.com/projectceladon/ failed!"
                echo -e "Network not responding. Please make sure proxy are set!${NC}"
                exit -1
	else
		echo -e "${GREEN}Network check passed${NC}"
	fi
}

function install_dependencies(){
	echo "Install apt packages"
	sudo apt install -y gcc g++ net-tools openssh-server git make autoconf libtool meson pkg-config 	\
	libpciaccess-dev cmake python3-pip python3.7 llvm-8-dev libelf-dev bison flex wayland-protocols \
	libwayland-dev libwayland-egl-backend-dev libx11-dev libxext-dev libxdamage-dev libx11-xcb-dev  \
	libxcb-glx0-dev	libxcb-dri2-0-dev libxcb-dri3-dev  libxcb-present-dev libxshmfence-dev 		\
	libxxf86vm-dev libxrandr-dev libkmod-dev libprocps-dev libdw-dev libpixman-1-dev libcairo-dev 	\
	libudev-dev libgudev-1.0 gtk-doc-tools sshfs mesa-utils weston xutils-dev libunwind-dev 	\
	libxml2-dev doxygen xmlto cmake libpciaccess-dev graphviz libjpeg-dev libwebp-dev 		\
	libsystemd-dev libdbus-glib-1-dev libpam0g-dev freerdp2-dev libxkbcommon-dev libinput-dev 	\
	libxcb-shm0-dev	libxcb-xv0-dev libxcb-keysyms1-dev libxcb-randr0-dev libxcb-composite0-dev	\
	libxcursor-dev liblcms2-dev libpango1.0-dev libglfw3-dev libgles2-mesa-dev libgbm-dev 		\
	libxcb-composite0-dev libxcursor-dev libgtk-3-dev libsdl2-dev virtinst virt-viewer virt-manager \
	libspice-server-dev libusb-dev
	if [ $? -ne 0 ]; then
		echo -e "${RED}Apt Packages Installation failed! Please resolved it${NC}"
		exit -1
	else
		echo -e "${GREEN}Dependecy Libaries installed${NC}"
	fi
	
}




function check_build_error()
{
	if [ $? -ne 0 ]; then
		echo -e "${RED}$1: Build Error ${NC}"
		exit -1
	else
		echo -e "${GREEN}$1: Build Success${NC}"
	fi
}

function build_webrtc()
{
        git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
        echo "export PATH=$PWD/depot_tools:$PATH"  | sudo tee -a /etc/environment
        source /etc/environment
        git clone https://github.com/open-webrtc-toolkit/owt-client-native.git src
        gclient sync
	cd src
 	./build/install-build-deps.sh
        cd ../
        gclient sync
        cd src
        git reset --hard 22f5f2f85e9fca025163156660fd36647e1a5590
	patch -p1 < ../webrtc_diff_v2.patch
        python ./scripts/build_linux.py --sdk --msdk_root=/opt/intel/mediasdk --arch=x64 --scheme=release --gn_gen
        #echo "export OWT_CLIENT_HOME=$PWD"  | sudo tee -a /etc/environment
	#source /etc/environment
        # build sample
        cd ../P2PSampleLinux
	mkdir build
        cd build
	cmake ..
 	make
        echo "export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/intel/mediasdk/lib:$PWD/../deps/libs"  | sudo tee -a /etc/environment
        source /etc/environment
        
}

function build_media(){

	echo -e "Building Media Driver Packages"
	echo -e ${NC}

	echo "export LIBVA_DRIVER_NAME=iHD" | sudo tee -a /etc/environment
	echo "export LIBVA_DRIVERS_PATH=/usr/lib/x86_64-linux-gnu/dri" | sudo tee -a /etc/environment
        echo 'export MFX_HOME=/opt/intel/mediasdk'  | sudo tee -a /etc/environment
 	source /etc/environment

	mkdir media
	cd media
	
	#Libdrm
	git clone https://gitlab.freedesktop.org/mesa/drm
	cd drm
	meson build/ --prefix=/usr/ --libdir=$LibPath
	ninja -C build && sudo ninja -C build install
	check_build_error "LIBDRM"

	cd $WrkDir/media

	#Libva
	git clone https://github.com/intel/libva.git
	cd libva
	./autogen.sh --prefix=$PrefixPath --libdir=$LibPath
	make -j"$(nproc)"
	sudo make install
	check_build_error "LIBVA"

	cd $WrkDir/media

	#Libva utils
	git clone https://github.com/intel/libva-utils.git
	cd libva-utils
	./autogen.sh --prefix=/$PrefixPath --libdir=$LibPath
	make -j "$(nproc)"
	sudo make install
	check_build_error "LIBVA UTILS"

	cd $WrkDir/media

	#Gmmlib
	git clone https://github.com/intel/gmmlib.git
	cd gmmlib
	mkdir build && cd build
	cmake -DCMAKE_INSTALL_PREFIX=$PrefixPath  -DCMAKE_BUILD_TYPE=Release ..
	make -j "$(nproc)"
	sudo make install
	check_build_error "GMMLIB"

	cd $WrkDir/media

	#Media driver
	git clone https://github.com/intel/media-driver.git
	cd media-driver
	cd ..
	mkdir  build_media
	cd build_media
	cmake ../media-driver
	make -j "$(nproc)"
	sudo make install
	check_build_error "iHD Media Driver"

	cd $WrkDir/media

	#Media-SDK
	git clone https://github.com/Intel-Media-SDK/MediaSDK msdk
	cd msdk
	mkdir build && cd build
	cmake .. -DENABLE_WAYLAND=true -DENABLE_X11_DRI3=true
	make -j "$(nproc)"
	sudo make install
	check_build_error "MediaSDK"

	cd $WrkDir/media

	sudo cp -r /opt/intel/mediasdk/lib/* /usr/lib/x86_64-linux-gnu/
	cd $WrkDir

}

function do_reboot(){
	echo 'source /etc/environment' >> ~/.bashrc
	sudo reboot
}

function build_ias()
{
	echo -e "Building Weston, Wayland, IAS, Mesa/IRIS Packages"
	export MESA_LOADER_DRIVER=iris
	export XDG_RUNTIME_DIR=/tmp/temp
	echo "export MESA_LOADER_DRIVER=iris" | sudo tee -a /etc/environment
	echo "export XDG_RUNTIME_DIR=/tmp/temp" | sudo tee -a /etc/environment
	source /etc/environment

	mkdir graphics
	cd graphics

	#Weston-Wayland-IAS
	git clone https://gitlab.freedesktop.org/wayland/wayland.git
	cd wayland
	./autogen.sh -prefix=$PrefixPath --libdir=$LibPath
	make -j "$(nproc)"
	#check_build_error "wayland"
	sudo make install
	cd ..

	git clone https://gitlab.freedesktop.org/wayland/wayland-protocols
	cd wayland-protocols
	./autogen.sh -prefix=$PrefixPath --libdir=$LibPath
	make -j "$(nproc)"
	check_build_error "wayland protocol"
	#sudo make install
	cd ..

	git clone https://gitlab.freedesktop.org/wayland/weston.git
	cd weston
	meson build/ --prefix=/usr/ -Dcolor-management-colord=false -Dpipewire=false
	ninja -C build && sudo ninja -C build install
	#check_build_error "weston"
	cd ..

	git clone https://github.com/intel/ias
	cd ias
	meson build/ --prefix=/usr/ -Dcolor-management-colord=false -Dpipewire=false
	ninja -C build && sudo ninja -C build install
	#check_build_error "ias"
	cd ..
        mkdir ~/.config
        cp ias.conf ~/.config
        cp weston.ini ~/.config
        
}

version=`cat /proc/version`

if [[ $version =~ "Ubuntu" ]]; then
	check_network
	install_dependencies
	build_media
	build_webrtc
        build_ias
	do_reboot
else
	echo "Only Ubunutu 18.04 is supported"
fi

