========================
目录结构
========================
项目目录app
webrtc外部库目录libs
代码目录app/src/main/java
    主界面com/intel/gamepad/activity
    子界面com/intel/gamepad/fragment
    实体类com/intel/gamepad/bean
    手柄类com/intel/gamepad/controller
    工具类com/intel/gamepad/controller
    RTC相关com/intel/gamepad/owt.p2p
    常量类com/intel/gamepad/bean
资源目录app/src/main/res

========================
界面结构
========================
封面页SplashActivity.kt

主界面MainActivity.kt
    首页HomeFragment.kt
    游戏GameFragment.kt
        游戏列表GameListFragment.kt
    我的MineFragment.kt

登录界面LoginActivity.kt
    登录LoginFragment.kt
    注册RegisterFragment.kt

游戏详情GameDetailActivity.kt
    这个页面主要是显示游戏的详情和启动游戏的作用，启动游戏时涉及到的调接口的任务就是在这个页面做的。

游戏播放PlayGameRtcActivity.kt
    这个页面主要是用于游戏串流和操作游戏的，WebRTC的连接和操作都是在这个页面的。

设置页面SettingActivity.kt

========================
游戏操作控制
========================
所有控制类都在controller/webrtc目录下。
BaseController是父类，其它类都是继承自这个类。
所有往GA端发送的游戏操作事件都是在BaseController有对应的方法sendXXXX方法。

控件接收事件的前提是设置监听器：
setOnTouchListener：监听触屏事件
setOnGenericMotionListener：监听物理设备的摇杆事件
setOnKeyListener：监听物理物理设备的按钮事件

注意三个函数：
1）onTouch         所有的触屏操作都会回调这个方法。
2）onGenericMotion 所有的摇杆操作都会回调这个方法。
3）onKey           所有的物理按钮操作都会回调这个方法。
以上三个方法都需要return一个Boolean的返回值，返回true表示事件已经被处理了，返回false表示事件不处理，事件会被回退给父控件。


========================
网络接口
========================
在代码中以request开头的函数都是在调网络接口，不管成功还是失败都会有对应的回调方法。
onSuccess：接口调用成功是回调
onError：  接口调用失败是回调，出现这种情况多数是网络不通、或者服务器没开之类的问题
onFinish： 总是在最后被回调的方法，这个方法里面一般是做关闭loading对话框的工作。