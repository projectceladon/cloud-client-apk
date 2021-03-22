/******************************************************************************\
Copyright (c) 2005-2019, Intel Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

This sample was distributed or derived from the Intel's Media Samples package.
The original version of this sample may be obtained from https://software.intel.com/en-us/intel-media-server-studio
or https://software.intel.com/en-us/media-client-solutions-support.
\**********************************************************************************/

#if defined(LIBVA_DRM_SUPPORT) || defined(LIBVA_X11_SUPPORT) || defined(LIBVA_ANDROID_SUPPORT) || defined(LIBVA_WAYLAND_SUPPORT)

#include <fcntl.h>
#include <dlfcn.h>

#include "vaapi_device.h"

#if defined(LIBVA_WAYLAND_SUPPORT)
#include "class_wayland.h"
#endif

#if defined(LIBVA_X11_SUPPORT)
#include <va/va_x11.h>
#include <X11/Xlib.h>
#endif

#include "vaapi_allocator.h"

#define ALIGN(x, y) (((x) + (y) - 1) & -(y))
#define PAGE_ALIGN(x) ALIGN(x, 4096)

#define VAAPI_GET_X_DISPLAY(_display) (Display*)(_display)
#define VAAPI_GET_X_WINDOW(_window) (Window*)(_window)


#if defined(LIBVA_WAYLAND_SUPPORT)
#include "wayland-drm-client-protocol.h"

CVAAPIDeviceWayland::~CVAAPIDeviceWayland(void)
{
    Close();
}

mfxStatus CVAAPIDeviceWayland::Init(mfxHDL hWindow, mfxU16 nViews, mfxU32 nAdapterNum)
{
    mfxStatus mfx_res = MFX_ERR_NONE;

    if(nViews)
    {
        m_Wayland = (Wayland*)m_WaylandClient.WaylandCreate();
        if(!m_Wayland->InitDisplay()) {
            return MFX_ERR_DEVICE_FAILED;
        }

        if(NULL == m_Wayland->GetDisplay())
        {
            mfx_res = MFX_ERR_UNKNOWN;
            return mfx_res;
        }
       if(-1 == m_Wayland->DisplayRoundtrip())
        {
            mfx_res = MFX_ERR_UNKNOWN;
            return mfx_res;
        }
        if(!m_Wayland->CreateSurface())
        {
            mfx_res = MFX_ERR_UNKNOWN;
            return mfx_res;
        }
    }
    return mfx_res;
}

mfxStatus CVAAPIDeviceWayland::RenderFrame(vaapiMemId *memId)
{
    uint32_t drm_format = 0;
    int offsets[3], pitches[3];
    mfxStatus mfx_res = MFX_ERR_NONE;

   // vaapiMemId * memId = NULL;
    struct wl_buffer *m_wl_buffer = NULL;
    if(NULL==memId) {
        mfx_res = MFX_ERR_UNKNOWN;
        return mfx_res;
    }
    //m_Wayland->Sync();
  //  memId = (vaapiMemId*)(pSurface->Data.MemId);

    if (memId->m_fourcc == MFX_FOURCC_NV12)
    {
        drm_format = WL_DRM_FORMAT_NV12;
    } else if(memId->m_fourcc == MFX_FOURCC_RGB4)
    {
        drm_format = WL_DRM_FORMAT_ARGB8888;

        if (m_isMondelloInputEnabled)
        {
            drm_format = WL_DRM_FORMAT_XBGR8888;
        }
    }

    offsets[0] = memId->m_image.offsets[0];
    offsets[1] = memId->m_image.offsets[1];
    offsets[2] = memId->m_image.offsets[2];
    pitches[0] = memId->m_image.pitches[0];
    pitches[1] = memId->m_image.pitches[1];
    pitches[2] = memId->m_image.pitches[2];
    m_wl_buffer = m_Wayland->CreatePrimeBuffer(memId->m_buffer_info.handle
      , memId->m_crop_w
      , memId->m_crop_h
      , drm_format
      , offsets
      , pitches);
    if(NULL == m_wl_buffer)
    {
            msdk_printf("\nCan't wrap flink to wl_buffer\n");
            mfx_res = MFX_ERR_UNKNOWN;
            return mfx_res;
    }


    m_Wayland->RenderBuffer(m_wl_buffer, memId->m_crop_w, memId->m_crop_h);

    return mfx_res;
}

void CVAAPIDeviceWayland::Close(void)
{
    m_Wayland->FreeSurface();
}

//CHWDevice* CreateVAAPIDevice(void)
//{
//    return new CVAAPIDeviceWayland();
//}

#endif // LIBVA_WAYLAND_SUPPORT


#if defined(LIBVA_DRM_SUPPORT) || defined(LIBVA_X11_SUPPORT) || defined (LIBVA_WAYLAND_SUPPORT)

CHWDevice* CreateVAAPIDevice(const std::string& devicePath, int type)
{
    CHWDevice * device = NULL;

    switch (type)
    {
    case MFX_LIBVA_WAYLAND:
#if defined(LIBVA_WAYLAND_SUPPORT)
        device = new CVAAPIDeviceWayland;
#endif
        break;
    } // switch(type)

    return device;
}

#endif

#endif //#if defined(LIBVA_DRM_SUPPORT) || defined(LIBVA_X11_SUPPORT) || defined(LIBVA_ANDROID_SUPPORT)
