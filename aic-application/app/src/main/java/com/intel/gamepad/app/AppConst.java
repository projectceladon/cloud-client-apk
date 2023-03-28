/* Copyright (C) 2021 Intel Corporation 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *   
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.intel.gamepad.app;

public class AppConst {
    // 消息事件常量
    public static final int MSG_QUIT = 1;
    public static final int MSG_SHOW_CONTROLLER = 2;
    public static final int MSG_UPDATE_CONTROLLER = 3;
    public static final int MSG_NO_STREAM_ADDED = 4;
    public static final int MSG_UNRECOVERABLE = 5;
    public static final int EXIT_NORMAL = 0;
    public static final int EXIT_DISCONNECT = -1;
    public static final String H264 = "video/avc";
    public static final String CODEC_WHITELIST_FILENAME = "mediaCodec.xml";
}
