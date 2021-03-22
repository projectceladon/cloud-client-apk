// Copyright (C) <2019> Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
#pragma once
//#include "log.h"
#include <fcntl.h>
#include <iostream>
#include <mutex>
#include <shared_mutex>
#include <string.h>
#include <va/va_drmcommon.h>
#include <iostream>
#include <mutex>
#include <shared_mutex>
#include <X11/Xlib.h>
#include <va/va.h>
#include <va/va_x11.h>
#include <memory.h> 

#include <iostream>
#include <memory>
 
 
using namespace std;
// Singleton for VADisplay for X11
shared_ptr<VADisplay> GetVADisplayX11(Display *xdpy);

