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

package com.intel.gamepad.utils;

import java.math.BigDecimal;

public class FormatUtil {

    public static String transferSize(String size, int decimal) {
        double length = Double.parseDouble(size);
        return transferSize(length, decimal);
    }

    public static String transferSize(double size, int decimal) {
        if (size < 1024) {
            return formatValue(size, decimal) + "B";
        } else {
            size = size / 1024.0;
        }
        if (size < 1024) {
            return formatValue(Math.round(size * 100) / 100.0, decimal) + "KB";
        } else {
            size = size / 1024.0;
        }
        if (size < 1024) {
            return formatValue(Math.round(size * 100) / 100.0, decimal) + "MB";
        } else {
            return formatValue(Math.round(size / 1024 * 100) / 100.0, decimal) + "GB";
        }
    }

    public static String formatValue(double value, int decimal) {
        return new BigDecimal(value).setScale(decimal, BigDecimal.ROUND_DOWN).toString();
    }

}
