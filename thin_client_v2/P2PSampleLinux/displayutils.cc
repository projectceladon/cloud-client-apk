// Copyright (C) <2019> Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

#include "displayutils.h"
#include <fcntl.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>


#include <iostream>
#include <memory>
#include <mutex>
#include <shared_mutex>
#include <memory.h> 
using namespace std;
struct VADisplayX11Terminator
{
  VADisplayX11Terminator() {}
  void operator()(VADisplay *display)
  {
    vaTerminate(*display);
    delete display;
  }
};
class DisplayGetter
{
public:
  static shared_ptr<VADisplay> GetVADisplayX11(Display *xdpy)
  {
    shared_lock<shared_timed_mutex> lock(mutex_x11_);
    if (display_x11_)
      return display_x11_;
    if (!xdpy)
    {
     // LOG_DEBUG("Empty X11 display detected.");
      return display_x11_;
    }
    VADisplay vaDisplay = vaGetDisplay(xdpy);
    int majorVersion, minorVersion;
    VAStatus status = vaInitialize(vaDisplay, &majorVersion, &minorVersion);
    if (status != VA_STATUS_SUCCESS)
    {
     // LOG_DEBUG("Failed to vaInitialize for X11 rendering.");
      return display_x11_;
    }
    display_x11_.reset(new VADisplay(vaDisplay), VADisplayX11Terminator());
    return display_x11_;
  }

private:
  static std::shared_timed_mutex mutex_x11_;
  static shared_ptr<VADisplay> display_x11_;
};
std::shared_timed_mutex DisplayGetter::mutex_x11_;
shared_ptr<VADisplay> DisplayGetter::display_x11_;
shared_ptr<VADisplay> GetVADisplayX11(Display *xdpy)
{
  return DisplayGetter::GetVADisplayX11(xdpy);
}
