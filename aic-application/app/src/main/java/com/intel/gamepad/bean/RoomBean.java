package com.intel.gamepad.bean;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RoomBean {
    /**
     * creatroomuserid : 1
     * gameid : 1
     * insertroomuserid : 1
     * roomcount : 1
     * roomid : 1
     * roomname : ghj
     * roomtotalcount : 1
     */

    private int creatroomuserid;
    private int gameid;
    private int insertroomuserid;
    private int roomcount;
    private int roomid;
    private String roomname;
    private int roomtotalcount;

    public static RoomBean objectFromData(String str) {

        return new Gson().fromJson(str, RoomBean.class);
    }

    public static List<RoomBean> arrayRoomBeanFromData(String str) {

        Type listType = new TypeToken<ArrayList<RoomBean>>() {
        }.getType();

        return new Gson().fromJson(str, listType);
    }

    public int getCreatroomuserid() {
        return creatroomuserid;
    }

    public void setCreatroomuserid(int creatroomuserid) {
        this.creatroomuserid = creatroomuserid;
    }

    public int getGameid() {
        return gameid;
    }

    public void setGameid(int gameid) {
        this.gameid = gameid;
    }

    public int getInsertroomuserid() {
        return insertroomuserid;
    }

    public void setInsertroomuserid(int insertroomuserid) {
        this.insertroomuserid = insertroomuserid;
    }

    public int getRoomcount() {
        return roomcount;
    }

    public void setRoomcount(int roomcount) {
        this.roomcount = roomcount;
    }

    public int getRoomid() {
        return roomid;
    }

    public void setRoomid(int roomid) {
        this.roomid = roomid;
    }

    public String getRoomname() {
        return roomname;
    }

    public void setRoomname(String roomname) {
        this.roomname = roomname;
    }

    public int getRoomtotalcount() {
        return roomtotalcount;
    }

    public void setRoomtotalcount(int roomtotalcount) {
        this.roomtotalcount = roomtotalcount;
    }
}
