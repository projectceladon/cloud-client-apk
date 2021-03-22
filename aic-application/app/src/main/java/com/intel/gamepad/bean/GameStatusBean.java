package com.intel.gamepad.bean;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GameStatusBean {

    /**
     * addurl : rts
     * closecmd : cmd.exe
     * closeexe : ac_client.exe
     * conf : dota2
     * gamedir : C:\Program Files (x86)\AssaultCube
     * gamexe : assaultcube.bat
     * iid : 1
     * imageUrl : static/images/jiangxuan_4.jpg
     * intro : 像素谷Stardew Valley是经典日式农场RPG游戏
     * ip : 192.168.1.6
     * message : 没有可运行的游戏
     * port : 8554
     * serverid : ga
     * status : 0
     * success : false
     * title : DOTA2
     */

    private String addurl;
    private String closecmd;
    private String closeexe;
    private String conf;
    private String gamedir;
    private String gamexe;
    private int iid;
    private String imageUrl;
    private String intro;
    private String ip;
    private String message;
    private int port;
    private String serverid;
    private int status;
    private boolean success;
    private String title;

    public static GameStatusBean objectFromData(String str) {

        return new Gson().fromJson(str, GameStatusBean.class);
    }

    public static List<GameStatusBean> arrayGameStatusBeanFromData(String str) {

        Type listType = new TypeToken<ArrayList<GameStatusBean>>() {
        }.getType();

        return new Gson().fromJson(str, listType);
    }

    public String getAddurl() {
        return addurl;
    }

    public void setAddurl(String addurl) {
        this.addurl = addurl;
    }

    public String getClosecmd() {
        return closecmd;
    }

    public void setClosecmd(String closecmd) {
        this.closecmd = closecmd;
    }

    public String getCloseexe() {
        return closeexe;
    }

    public void setCloseexe(String closeexe) {
        this.closeexe = closeexe;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public String getGamedir() {
        return gamedir;
    }

    public void setGamedir(String gamedir) {
        this.gamedir = gamedir;
    }

    public String getGamexe() {
        return gamexe;
    }

    public void setGamexe(String gamexe) {
        this.gamexe = gamexe;
    }

    public int getIid() {
        return iid;
    }

    public void setIid(int iid) {
        this.iid = iid;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getServerid() {
        return serverid;
    }

    public void setServerid(String serverid) {
        this.serverid = serverid;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
