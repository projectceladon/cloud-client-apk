// Copyright (C) <2019> Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
#include <fcntl.h>
#include <iostream>
#include <mutex>
#include <shared_mutex>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include <assert.h>
#include <va/va_drmcommon.h>

#include "displayutils.h"
#include "class_wayland.h"
#include "vaapi_device.h"
#include "xwindowrenderer.h"


#define MAX_LEN   1024

#define CHECK_VASTATUS(va_status,func)                                      \
  if (va_status != VA_STATUS_SUCCESS) {                                     \
      fprintf(stderr,"%s:%s (%d) failed,exit\n", __func__, func, __LINE__); \
      exit(1);                                                              \
  }
static VADisplay va_dpy = 0;
static VAContextID context_id = 0;
static VAConfigID  config_id = 0;
static VASurfaceID g_in_surface_id = VA_INVALID_ID;
static VASurfaceID g_out_surface_id = VA_INVALID_ID;

static FILE* g_config_file_fd = NULL;
static FILE* g_src_file_fd = NULL;
static FILE* g_dst_file_fd = NULL;

static char g_config_file_name[MAX_LEN];
static char g_src_file_name[MAX_LEN];
static char g_dst_file_name[MAX_LEN];

static uint32_t g_in_pic_width = 352;
static uint32_t g_in_pic_height = 288;
static uint32_t g_out_pic_width = 352;
static uint32_t g_out_pic_height = 288;

static uint32_t g_in_fourcc  = VA_FOURCC('N', 'V', '1', '2');
static uint32_t g_in_format  = VA_RT_FORMAT_YUV420;
static uint32_t g_out_fourcc = VA_FOURCC_BGRA;
static uint32_t g_out_format = VA_RT_FORMAT_RGB32;
vaapiMemId *vaapi_mids = NULL;
unsigned int surfaces_num = 1;

VASurfaceID surfaces[3];
enum eWorkMode {
  MODE_PERFORMANCE,
  MODE_RENDERING,
  MODE_FILE_DUMP
};

CHWDevice* m_hwdev   = NULL;
mfxI32  m_libvaBackend = MFX_LIBVA_WAYLAND;
eWorkMode m_eWorkMode   = MODE_RENDERING;
mfxU32                  m_nRenderWinX = 0;
mfxU32                  m_nRenderWinY = 0;
mfxU32                  m_nRenderWinW = 2560;
mfxU32                  m_nRenderWinH = 1440;
bool                    m_bPerfMode = true;

struct AcquireCtx
{
  int fd;
  VAImage image;
};


VAStatus AcquireVASurface(
    void **pctx,
    VADisplay dpy1,
    VASurfaceID srf1,
    VADisplay dpy2,
    VASurfaceID *srf2)
{
  if (!pctx || !srf2)
    return VA_STATUS_ERROR_OPERATION_FAILED;

  if (dpy1 == dpy2)
  {
    *srf2 = srf1;
    return VA_STATUS_SUCCESS;
  }

  AcquireCtx *ctx;
  unsigned long handle = 0;
  VAStatus va_res;
  VASurfaceAttrib attribs[2];
  VASurfaceAttribExternalBuffers extsrf;
  VABufferInfo bufferInfo;
  uint32_t memtype = VA_SURFACE_ATTRIB_MEM_TYPE_DRM_PRIME;

  memset(&attribs, 0, sizeof(attribs));
  memset(&extsrf, 0, sizeof(extsrf));
  memset(&bufferInfo, 0, sizeof(bufferInfo));
  extsrf.num_buffers = 1;
  extsrf.buffers = &handle;

  attribs[0].type = (VASurfaceAttribType)VASurfaceAttribMemoryType;
  attribs[0].flags = VA_SURFACE_ATTRIB_SETTABLE;
  attribs[0].value.type = VAGenericValueTypeInteger;
  attribs[0].value.value.i = memtype;

  attribs[1].type = (VASurfaceAttribType)VASurfaceAttribExternalBufferDescriptor;
  attribs[1].flags = VA_SURFACE_ATTRIB_SETTABLE;
  attribs[1].value.type = VAGenericValueTypePointer;
  attribs[1].value.value.p = &extsrf;

  ctx = (AcquireCtx *)calloc(1, sizeof(AcquireCtx));
  if (!ctx)
    return VA_STATUS_ERROR_OPERATION_FAILED;

  va_res = vaDeriveImage(dpy1, srf1, &ctx->image);
  if (VA_STATUS_SUCCESS != va_res)
  {
    free(ctx);
    return va_res;
  }

  va_res = vaAcquireBufferHandle(dpy1, ctx->image.buf, &bufferInfo);
  if (VA_STATUS_SUCCESS != va_res)
  {
    vaDestroyImage(dpy1, ctx->image.image_id);
    free(ctx);
    return va_res;
  }

  extsrf.width = ctx->image.width;
  extsrf.height = ctx->image.height;
  extsrf.num_planes = ctx->image.num_planes;
  extsrf.pixel_format = ctx->image.format.fourcc;
  for (int i = 0; i < 3; ++i)
  {
    extsrf.pitches[i] = ctx->image.pitches[i];
    extsrf.offsets[i] = ctx->image.offsets[i];
  }
  extsrf.data_size = ctx->image.data_size;
  extsrf.flags = memtype;
  extsrf.buffers[0] = bufferInfo.handle;

  va_res = vaCreateSurfaces(dpy2,
                            VA_RT_FORMAT_YUV420,
                            extsrf.width, extsrf.height,
                            srf2, 1, attribs, 2);
  if (VA_STATUS_SUCCESS != va_res)
  {
    vaDestroyImage(dpy1, ctx->image.image_id);
    free(ctx);
    return va_res;
  }

  *pctx = ctx;

  return VA_STATUS_SUCCESS;
}

void ReleaseVASurface(
    void *actx,
    VADisplay dpy1,
    VASurfaceID /*srf1*/,
    VADisplay dpy2,
    VASurfaceID srf2)
{
  if (dpy1 != dpy2)
  {
    AcquireCtx *ctx = (AcquireCtx *)actx;
    if (ctx)
    {
      vaDestroySurfaces(dpy2, &srf2, 1);
      close(ctx->fd);
      vaReleaseBufferHandle(dpy1, ctx->image.buf);
      vaDestroyImage(dpy1, ctx->image.image_id);
      free(ctx);
    }
  }
}

static VAStatus
create_surface(VASurfaceID * p_surface_id,
               uint32_t width, uint32_t height,
               uint32_t fourCC, uint32_t format)
{
    VAStatus va_status;
    VASurfaceAttrib    surface_attrib;
    surface_attrib.type =  VASurfaceAttribPixelFormat;
    surface_attrib.flags = VA_SURFACE_ATTRIB_SETTABLE;
    surface_attrib.value.type = VAGenericValueTypeInteger;
    surface_attrib.value.value.i = fourCC;

    printf("create surface: width=%d, height=%d\r\n", width,height);
    va_status = vaCreateSurfaces(va_dpy,
                                 format,
                                 width ,
                                 height,
                                 p_surface_id,
                                 1,
                                 &surface_attrib,
                                 1);
   return va_status;
}

static VAStatus
video_frame_process(       VASurfaceID in_surface_id,
                    VASurfaceID out_surface_id)
{
    VAStatus va_status;
    VAProcPipelineParameterBuffer pipeline_param;
    VARectangle surface_region, output_region;
    VABufferID pipeline_param_buf_id = VA_INVALID_ID;
    /* Fill pipeline buffer */
    surface_region.x = 0;
    surface_region.y = 0;
    surface_region.width  = g_in_pic_width;
    surface_region.height = g_in_pic_height;
    output_region.x = 0;
    output_region.y = 0;
    output_region.width = g_out_pic_width;
    output_region.height = g_out_pic_height;

    memset(&pipeline_param, 0, sizeof(pipeline_param));
    pipeline_param.surface = in_surface_id;
    pipeline_param.surface_region = &surface_region;
    pipeline_param.output_region = &output_region;

    va_status = vaCreateBuffer(va_dpy,
                               context_id,
                               VAProcPipelineParameterBufferType,
                               sizeof(pipeline_param),
                               1,
                               &pipeline_param,
                               &pipeline_param_buf_id);
    CHECK_VASTATUS(va_status, "vaCreateBuffer");

    va_status = vaBeginPicture(va_dpy,
                               context_id,
                               out_surface_id);
    CHECK_VASTATUS(va_status, "vaBeginPicture");

    va_status = vaRenderPicture(va_dpy,
                                context_id,
                                &pipeline_param_buf_id,
                                1);
    CHECK_VASTATUS(va_status, "vaRenderPicture");

    va_status = vaEndPicture(va_dpy, context_id);
    CHECK_VASTATUS(va_status, "vaEndPicture");

    if (pipeline_param_buf_id != VA_INVALID_ID)
        vaDestroyBuffer(va_dpy,pipeline_param_buf_id);

    return va_status;
}

static VAStatus
vpp_context_create(VADisplay dpy, unsigned int width, unsigned int height)
{
    VAStatus va_status = VA_STATUS_SUCCESS;
    int32_t j;
    g_in_pic_width = g_out_pic_width  = width;
    g_in_pic_height = g_out_pic_height = height;
    va_dpy = dpy;
    /* Check whether VPP is supported by driver */
    VAEntrypoint entrypoints[5];
    int32_t num_entrypoints;
    num_entrypoints = vaMaxNumEntrypoints(va_dpy);
    std::cout<<"num_entrypoints:"<<num_entrypoints<<std::endl;
    va_status = vaQueryConfigEntrypoints(va_dpy,
                                         VAProfileNone,
                                         entrypoints,
                                         &num_entrypoints);
    CHECK_VASTATUS(va_status, "vaQueryConfigEntrypoints");

    for (j = 0; j < num_entrypoints; j++) {
        if (entrypoints[j] == VAEntrypointVideoProc)
            break;
    }

    if (j == num_entrypoints) {
        std::cout<<"VPP is not supported by driver"<<std::endl;
        assert(0);
    }

    /* Render target surface format check */
    VAConfigAttrib attrib;
    attrib.type = VAConfigAttribRTFormat;
    va_status = vaGetConfigAttributes(va_dpy,
                                      VAProfileNone,
                                      VAEntrypointVideoProc,
                                      &attrib,
                                     1);
    CHECK_VASTATUS(va_status, "vaGetConfigAttributes");
    if (!(attrib.value & g_out_format)) {
        std::cout<<"RT format"<< g_out_format <<"is not supported by VPP !"<<std::endl;
        assert(0);
    }
 
    vaapi_mids = (vaapiMemId*)calloc(surfaces_num, sizeof(vaapiMemId));
   for(int i=0; i< surfaces_num; i++)
   {

      va_status = create_surface(&surfaces[i], width, height,
                                 g_out_fourcc, g_out_format);
      CHECK_VASTATUS(va_status, "vaCreateSurfaces for output");

       vaapi_mids[i].m_buffer_info.mem_type = VA_SURFACE_ATTRIB_MEM_TYPE_DRM_PRIME;
       va_status = vaDeriveImage(va_dpy, surfaces[i], &(vaapi_mids[i].m_image));
      
       CHECK_VASTATUS(va_status, "vaDeriveImage for output surface");

       va_status = vaAcquireBufferHandle(va_dpy, vaapi_mids[i].m_image.buf,
                                       &(vaapi_mids[i].m_buffer_info));

       CHECK_VASTATUS(va_status, "vaAcquireBufferHandle for output surface");

       vaapi_mids[i].m_fourcc = MFX_FOURCC_RGB4;
       vaapi_mids[i].m_crop_w = width;
       vaapi_mids[i].m_crop_h = height;

   }


    va_status = vaCreateConfig(va_dpy,
                               VAProfileNone,
                               VAEntrypointVideoProc,
                               &attrib,
                               1,
                               &config_id);
    CHECK_VASTATUS(va_status, "vaCreateConfig");
    
   printf("HelloWorld: va_dpy=0x%x, w=%d, h=%d, outsurfce=%d\r\n",va_dpy,g_out_pic_width,g_out_pic_height,surfaces[0]);
    va_status = vaCreateContext(va_dpy,
                                config_id,
                                g_out_pic_width,
                                g_out_pic_height,
                                VA_PROGRESSIVE,
                                &surfaces[0],
                                1,
                                &context_id);
    CHECK_VASTATUS(va_status, "vaCreateContext");
    return va_status;
}

static void
vpp_context_destroy()
{
    /* Release resource */
    vaDestroySurfaces(va_dpy, &g_in_surface_id, 1);
    vaDestroySurfaces(va_dpy, &g_out_surface_id, 1);
    vaDestroyContext(va_dpy, context_id);
    vaDestroyConfig(va_dpy, config_id);

    vaTerminate(va_dpy);
    //va_close_display(va_dpy);
}
XWindowRenderer::XWindowRenderer()
    : is_window_ready_(false) {


}

XWindowRenderer::~XWindowRenderer()
{

}

 



void XWindowRenderer::RenderFrame(std::unique_ptr<VaSurface> va_surface)
{
  //std::cout<< "RenderFrame called: is_window_ready="<<is_window_ready_<<" va_surfaceID:"<<va_surface->surface << " va_display:"<<va_surface->display<<" w="<<va_surface->width << std::endl;
  if (!is_window_ready_)
  {
      VAStatus vaStatus = vpp_context_create(va_surface->display,va_surface->width, va_surface->height);
      if(vaStatus != VA_STATUS_SUCCESS){
         std::cout<<" failed to create vpp context"<<std::endl;
         return ;
      }
      //Initialize a display Window
      int m_monitorType = 0;
      mfxStatus sts = MFX_ERR_NONE;
      m_hwdev = CreateVAAPIDevice("",MFX_LIBVA_WAYLAND);
      if( NULL == m_hwdev){
         std::cout<<" Failed to create VAAPI device w/ Wayland support!!!"<<std::endl;
      }

      sts = m_hwdev->Init(&m_monitorType, (m_eWorkMode == MODE_RENDERING) ? 1 : 0, 0);
      MSDK_CHECK_STATUS(sts, "m_hwdev->Init failed");


      if (m_eWorkMode == MODE_RENDERING && m_libvaBackend == MFX_LIBVA_WAYLAND)
      {
          std::cout<<" WAYLAND: eworkMode is rendering" <<std::endl;
          CVAAPIDeviceWayland* w_dev = dynamic_cast<CVAAPIDeviceWayland*>(m_hwdev);
	  if (!w_dev)
	  {
	        MSDK_CHECK_STATUS(MFX_ERR_DEVICE_FAILED, "Failed to reach Wayland VAAPI device");
	  }
	  Wayland *wld = w_dev->GetWaylandHandle();
	  if (!wld)
	  {
		MSDK_CHECK_STATUS(MFX_ERR_DEVICE_FAILED, "Failed to reach Wayland VAAPI device");
	  }

	  wld->SetRenderWinPos(m_nRenderWinX, m_nRenderWinY);
	  wld->SetPerfMode(m_bPerfMode);
    }

    is_window_ready_ = true;
  }

  video_frame_process(va_surface->surface, surfaces[0]);
   //vaSyncSurfac);

  m_hwdev->RenderFrame(&vaapi_mids[0]);

  return;
}
