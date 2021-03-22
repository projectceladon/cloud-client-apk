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
        }
    }
}
