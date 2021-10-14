package com.intel.gamepad.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GameListBean implements Parcelable {
    public static final Creator<GameListBean> CREATOR = new Creator<GameListBean>() {
        @Override
        public GameListBean createFromParcel(Parcel source) {
            return new GameListBean(source);
        }

        @Override
        public GameListBean[] newArray(int size) {
            return new GameListBean[size];
        }
    };
    private int iid;
    private String imageUrl;
    private String title;
    private String ip;
    private int prot;
    private int port;
    private String type;
    private String conf;
    private String intro;
    private String addurl;

    public GameListBean() {
    }

    protected GameListBean(Parcel in) {
        this.iid = in.readInt();
        this.imageUrl = in.readString();
        this.title = in.readString();
        this.ip = in.readString();
        this.prot = in.readInt();
        this.port = in.readInt();
        this.type = in.readString();
        this.conf = in.readString();
        this.intro = in.readString();
        this.addurl = in.readString();
    }

    public static GameListBean objectFromData(String str) {

        return new Gson().fromJson(str, GameListBean.class);
    }

    public static List<GameListBean> arrayGameListBeanFromData(String str) {

        Type listType = new TypeToken<ArrayList<GameListBean>>() {
        }.getType();

        return new Gson().fromJson(str, listType);
    }

    public static String toJson(List<GameListBean> list) {

        Type listType = new TypeToken<ArrayList<GameListBean>>() {
        }.getType();

        return new Gson().toJson(list, listType);
    }

    @Override
    public String toString() {
        return "GameListBean{" +
                "iid=" + iid +
                ", imageUrl='" + imageUrl + '\'' +
                ", title='" + title + '\'' +
                ", ip='" + ip + '\'' +
                ", prot=" + prot +
                ", port=" + port +
                ", type='" + type + '\'' +
                ", conf='" + conf + '\'' +
                ", intro='" + intro + '\'' +
                ", addurl='" + addurl + '\'' +
                '}';
    }

    public String getAddurl() {
        return addurl;
    }

    public void setAddurl(String addurl) {
        this.addurl = addurl;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public int getProt() {
        return prot;
    }

    public void setProt(int prot) {
        this.prot = prot;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.iid);
        dest.writeString(this.imageUrl);
        dest.writeString(this.title);
        dest.writeString(this.ip);
        dest.writeInt(this.prot);
        dest.writeInt(this.port);
        dest.writeString(this.type);
        dest.writeString(this.conf);
        dest.writeString(this.intro);
        dest.writeString(this.addurl);
    }
}
