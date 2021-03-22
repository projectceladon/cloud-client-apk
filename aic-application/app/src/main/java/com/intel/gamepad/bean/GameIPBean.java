package com.intel.gamepad.bean;

public class GameIPBean {
    private int id;
    private String ip;
    private int port;
    private String gameName;

    @Override
    public String toString() {
        return "GameIPBean{" +
                "id=" + id +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", gameName='" + gameName + '\'' +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
}
