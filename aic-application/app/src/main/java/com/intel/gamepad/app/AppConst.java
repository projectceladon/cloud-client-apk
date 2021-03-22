package com.intel.gamepad.app;

public class AppConst {
    // 网络接口地址
    public static final String LOGIN = "user/login";
    public static final String REGISTER = "user/add"; // 注册
    public static final String SEND_CODE = "user/sendCode"; // 发送验证码
    public static final String CHECK_CODE = "user/checkCode"; // 检查验证码
    public static final String QUERY_LIST = "user/querylist";
    public static final String SERVLET_URL_PORT = "user/servletUrlAndPort";
    public static final String GAME_STATUS = "user/gameStatus";
    public static final String PORTANDIP = "user/portandip";
    public static final String ONLINE = "user/online";
    public static final String START_GAME = "user/servlettest";
    public static final String CLOSE_GAME = "user/closegames";
    public static final String START_GAME2 = "user/startGame";
    public static final String ROOM_LIST = "user/selectroomgames";
    public static final String CREATE_ROOM = "user/creatroom";
    public static final String DEL_ROOM = "user/delroomname";
    public static final String JOIN_GAME = "user/serverIp";

    // 消息事件常量
    public static final int MSG_SHOWTOAST = 1;
    public static final int MSG_QUIT = 2;
    public static final int MSG_RENDER = 3;
    public static final int MSG_SHOW_CONTROLLER = 4;
    public static final int MSG_UPDATE_DEVICE = 5;
    public static final int MSG_UPDATE_CONTROLLER = 6;
    public static final int MSG_UPDATE_STATUS = 7;

    public static final int REQUEST_GAME = 1;
    public static final int EXIT_NORMAL = 0;
    public static final int EXIT_TIMEOUT = -1;
    public static final int EXIT_NOHOST = -2;
    public static final int EXIT_DISCONNECT = -3;

}
