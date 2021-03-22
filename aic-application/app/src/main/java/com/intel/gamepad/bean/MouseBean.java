package com.intel.gamepad.bean;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MouseBean {
    /**
     * dstRect : {"bottom":716,"left":1415,"right":1447,"top":684}
     * height : 32
     * noShapeChange : true
     * pitch : 2097152
     * srcRect : {"bottom":32,"left":0,"right":32,"top":0}
     * type : cursor
     * visible : true
     * width : 32
     */
    private List<Byte> cursorData;
    private DstRectBean dstRect;
    private int height;
    private boolean noShapeChange;
    private int pitch;
    private SrcRectBean srcRect;
    private String type;
    private boolean visible;
    private int width;

    @Override
    public String toString() {
        return "MouseBean{" +
                "cursorData=" + cursorData +
                ", dstRect=" + dstRect +
                ", height=" + height +
                ", noShapeChange=" + noShapeChange +
                ", pitch=" + pitch +
                ", srcRect=" + srcRect +
                ", type='" + type + '\'' +
                ", visible=" + visible +
                ", width=" + width +
                '}';
    }

    public static MouseBean objectFromData(String str) {

        return new Gson().fromJson(str, MouseBean.class);
    }

    public static List<MouseBean> arrayMouseBeanFromData(String str) {

        Type listType = new TypeToken<ArrayList<MouseBean>>() {
        }.getType();

        return new Gson().fromJson(str, listType);
    }

    public List<Byte> getCursorData() {
        return cursorData;
    }

    public void setCursorData(List<Byte> cursorData) {
        this.cursorData = cursorData;
    }

    public DstRectBean getDstRect() {
        return dstRect;
    }

    public void setDstRect(DstRectBean dstRect) {
        this.dstRect = dstRect;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isNoShapeChange() {
        return noShapeChange;
    }

    public void setNoShapeChange(boolean noShapeChange) {
        this.noShapeChange = noShapeChange;
    }

    public int getPitch() {
        return pitch;
    }

    public void setPitch(int pitch) {
        this.pitch = pitch;
    }

    public SrcRectBean getSrcRect() {
        return srcRect;
    }

    public void setSrcRect(SrcRectBean srcRect) {
        this.srcRect = srcRect;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public static class DstRectBean {
        /**
         * bottom : 716
         * left : 1415
         * right : 1447
         * top : 684
         */

        private int bottom;
        private int left;
        private int right;
        private int top;

        public static DstRectBean objectFromData(String str) {

            return new Gson().fromJson(str, DstRectBean.class);
        }

        public static List<DstRectBean> arrayDstRectBeanFromData(String str) {

            Type listType = new TypeToken<ArrayList<DstRectBean>>() {
            }.getType();

            return new Gson().fromJson(str, listType);
        }

        public int getBottom() {
            return bottom;
        }

        public void setBottom(int bottom) {
            this.bottom = bottom;
        }

        public int getLeft() {
            return left;
        }

        public void setLeft(int left) {
            this.left = left;
        }

        public int getRight() {
            return right;
        }

        public void setRight(int right) {
            this.right = right;
        }

        public int getTop() {
            return top;
        }

        public void setTop(int top) {
            this.top = top;
        }
    }

    public static class SrcRectBean {
        /**
         * bottom : 32
         * left : 0
         * right : 32
         * top : 0
         */

        private int bottom;
        private int left;
        private int right;
        private int top;

        public static SrcRectBean objectFromData(String str) {

            return new Gson().fromJson(str, SrcRectBean.class);
        }

        public static List<SrcRectBean> arraySrcRectBeanFromData(String str) {

            Type listType = new TypeToken<ArrayList<SrcRectBean>>() {
            }.getType();

            return new Gson().fromJson(str, listType);
        }

        public int getBottom() {
            return bottom;
        }

        public void setBottom(int bottom) {
            this.bottom = bottom;
        }

        public int getLeft() {
            return left;
        }

        public void setLeft(int left) {
            this.left = left;
        }

        public int getRight() {
            return right;
        }

        public void setRight(int right) {
            this.right = right;
        }

        public int getTop() {
            return top;
        }

        public void setTop(int top) {
            this.top = top;
        }
    }
}
