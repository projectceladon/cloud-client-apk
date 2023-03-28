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

package com.intel.gamepad.bean;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MotionEventBean {
    /**
     * data : {"event":"mousemove","parameters":{"movementY":0,"movementX":0,"touchx":794.72107,"touchy":809.72107}}
     * type : control
     */

    private DataBean data;
    private String type;

    public static MotionEventBean objectFromData(String str) {

        return new Gson().fromJson(str, MotionEventBean.class);
    }

    public static List<MotionEventBean> arrayEventBeanFromData(String str) {

        Type listType = new TypeToken<ArrayList<MotionEventBean>>() {
        }.getType();

        return new Gson().fromJson(str, listType);
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static class DataBean {
        /**
         * event : mousemove
         * parameters : {"movementY":0,"movementX":0,"touchx":794.72107,"touchy":809.72107}
         */

        private String event;
        private ParametersBean parameters;

        public static DataBean objectFromData(String str) {

            return new Gson().fromJson(str, DataBean.class);
        }

        public static List<DataBean> arrayDataBeanFromData(String str) {

            Type listType = new TypeToken<ArrayList<DataBean>>() {
            }.getType();

            return new Gson().fromJson(str, listType);
        }

        public String getEvent() {
            return event;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public ParametersBean getParameters() {
            return parameters;
        }

        public void setParameters(ParametersBean parameters) {
            this.parameters = parameters;
        }

        public static class ParametersBean {
            private int action;
            private float touchx;
            private float touchy;
            private int keycode;
            private int jID;
            private int fingerId;
            private String data;
            private String file_name;
            private int tID = 0;
            private long E2ELatency = 0;

            public static ParametersBean objectFromData(String str) {

                return new Gson().fromJson(str, ParametersBean.class);
            }

            public static List<ParametersBean> arrayParametersBeanFromData(String str) {

                Type listType = new TypeToken<ArrayList<ParametersBean>>() {
                }.getType();

                return new Gson().fromJson(str, listType);
            }

            public int getAction() {
                return action;
            }

            public void setAction(int action) {
                this.action = action;
            }

            public float getTouchx() {
                return touchx;
            }

            public void setTouchx(float touchx) {
                this.touchx = touchx;
            }

            public float getTouchy() {
                return touchy;
            }

            public void setTouchy(float touchy) {
                this.touchy = touchy;
            }

            public int getKeycode() {
                return keycode;
            }

            public void setKeycode(int keycode) {
                this.keycode = keycode;
            }

            public int getjID() {
                return jID;
            }

            public void setjID(int jID) {
                this.jID = jID;
            }

            public int getFingerId() {
                return fingerId;
            }

            public void setFingerId(int fingerId) {
                this.fingerId = fingerId;
            }

            public String getData() {
                return data;
            }

            public void setData(String msg) {
                this.data = msg;
            }

            public String getFile_name() {
                return file_name;
            }

            public void setFile_name(String file_name) {
                this.file_name = file_name;
            }

            public int gettID() {
                return tID;
            }

            public void settID(int tID) {
                this.tID = tID;
            }

            public long getE2ELatency() {
                return E2ELatency;
            }

            public void setE2ELatency(long time) {
                this.E2ELatency = time;
            }
        }
    }
}
