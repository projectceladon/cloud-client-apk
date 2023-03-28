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

import android.annotation.SuppressLint;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class LocationUtils {

    public static String buildComposedNmeaMessage(double latitudeVal, double longitudeVal) {
        double latitude = latitudeVal;
        double longitude = longitudeVal;
        String fakeNmeaMessage = "$GPGGA,061120.00,2922.413610,N,10848.823876,E,1,04,1.5,0,M,-27.8,M,,*4D"; //fake
        // $GPGGA,051502.002,3101.431360,N,12126.974320,E,1,04,1.5,0,M,-27.8,M,,*6e
        String[] str = fakeNmeaMessage.split(",");
        Date date = new Date();
        @SuppressLint("SimpleDateFormat") // See #117
        SimpleDateFormat mTimeFormat = new SimpleDateFormat("HHmmss.sss");
        mTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateNowStr = mTimeFormat.format(date);
        str[1] = dateNowStr;

        if (latitude < 0) {
            latitude = Math.abs(latitude);
            str[5] = "W";
        } else {
            str[5] = "E";
        }

        if (longitude < 0) {
            longitude = Math.abs(longitude);
            str[3] = "S";
        } else {
            str[3] = "N";
        }

        int int_lat = (int) latitude;
        double nmeaLatitude = (latitude - int_lat) * 60 + int_lat * 100;
        int int_long = (int) longitude;
        double nmeaLongtitude = (longitude - int_long) * 60 + int_long * 100;
        DecimalFormat df6 = new DecimalFormat("###.000000");
        String latStr = df6.format(nmeaLatitude);
        String longStr = df6.format(nmeaLongtitude); //keep decimal 6
        str[4] = longStr;
        str[2] = latStr;
        str[6] = "1"; // gps state, nmea sometimes is 0 (means no gps)
        String fixedNmeaMessage = StringUtils.join(str, ",");
        String checkValue = nmeaCheckSum(fixedNmeaMessage);
        str[14] = "*" + checkValue + "\r\n"; //check segment
        fixedNmeaMessage = StringUtils.join(str, ",");
        return fixedNmeaMessage;

    }

    public static String nmeaCheckSum(String nmea) {
        char[] ch = nmea.toCharArray();
        int i, result;
        for (result = ch[1], i = 2; ch[i] != '*'; i++) {
            result ^= ch[i];
        }
        return Integer.toHexString(result);
    }
}
