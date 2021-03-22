/* Copyright (c) 2011 The WebM project authors. All Rights Reserved. */
/*  */
/* Use of this source code is governed by a BSD-style license */
/* that can be found in the LICENSE file in the root of the source */
/* tree. An additional intellectual property rights grant can be found */
/* in the file PATENTS.  All contributing project authors may */
/* be found in the AUTHORS file in the root of the source tree. */
#include "vpx/vpx_codec.h"
static const char* const cfg = "--disable-unit-tests --disable-docs --disable-examples --enable-pic --enable-vp8 --enable-vp9 --enable-postproc --enable-vp9-postproc --enable-vp9-highbitdepth --enable-vp9-temporal-denoising --enable-multi-res-encoding --enable-webm-io --enable-libyuv --extra-cflags=-fvisibility=hidden";
const char *vpx_codec_build_config(void) {return cfg;}
