package com.intel.gamepad.activity

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.hardware.input.InputManager
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.SimpleArrayMap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.intel.gamepad.R
import com.intel.gamepad.app.AppConst
import com.intel.gamepad.bean.MotionEventBean
import com.intel.gamepad.bean.MotionEventBean.DataBean
import com.intel.gamepad.bean.MotionEventBean.DataBean.ParametersBean
import com.intel.gamepad.bean.MouseBean
import com.intel.gamepad.controller.impl.DeviceSwitchListtener
import com.intel.gamepad.controller.webrtc.*
import com.intel.gamepad.owt.p2p.P2PHelper
import com.intel.gamepad.owt.p2p.P2PHelper.FailureCallBack
import com.intel.gamepad.utils.AudioHelper
import com.jeremy.fastsharedpreferences.FastSharedPreferences
import com.mcxtzhang.commonadapter.rv.CommonAdapter
import com.mcxtzhang.commonadapter.rv.ViewHolder
import com.mycommonlibrary.utils.DateTimeUtils
import com.mycommonlibrary.utils.DensityUtils
import com.mycommonlibrary.utils.LogEx
import com.mycommonlibrary.utils.StatusBarUtil
import kotlinx.android.synthetic.main.activity_play_game_rtc.*
import kotlinx.coroutines.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.startActivityForResult
import org.json.JSONObject
import org.webrtc.RTCStatsReport
import org.webrtc.SingletonSurfaceView
import owt.base.ActionCallback
import owt.base.OwtError
import owt.p2p.P2PClient
import owt.p2p.RemoteStream
import java.io.DataInputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.Socket
import java.nio.ByteBuffer
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

private const val SERVER_SCREEN_WIDTH = 1920
private const val SERVER_SCREEN_HEIGHT = 1080
private const val DELAY_GET_STATUS = 3000L

class PlayGameRtcActivity : AppCompatActivity(), DeviceSwitchListtener,
    InputManager.InputDeviceListener,
    CoroutineScope by MainScope() {
    companion object {
        const val RESULT_MSG = "resultMsg"
        fun actionStart(act: Activity, controller: String, gameId: Int, gameName: String) {
            act.startActivityForResult<PlayGameRtcActivity>(
                AppConst.REQUEST_GAME,
                "controller" to controller,
                "gameId" to gameId,
                "gameName" to gameName
            )
        }
    }

    private var peerId: String = ""
    private var inCalling = false
    private var remoteStream: RemoteStream? = null
    private var remoteStreamEnded = false
    private var controller: BaseController? = null
    private var viewWidth = DensityUtils.getmScreenWidth()
    private var viewHeight = DensityUtils.getmScreenHeight()
    private var screenWidth = viewWidth;
    private var screenHeight = viewHeight;
    private var paddingSize = 0
    private var handler: Handler? = null
    private var firstTimestamp = 0L
    private var isFirst = false
    private lateinit var mIm: InputManager

    val JOY_KEY_CODE_MAP_X = 307
    val JOY_KEY_CODE_MAP_Y = 308
    val JOY_KEY_CODE_MAP_A = 304
    val JOY_KEY_CODE_MAP_B = 305
    val JOY_KEY_CODE_MAP_L_ONE = 310
    val JOY_KEY_CODE_MAP_L_TWO = 312
    val JOY_KEY_CODE_MAP_R_ONE = 311
    val JOY_KEY_CODE_MAP_R_TWO = 313
    val JOY_KEY_CODE_MAP_SELECT = 314
    val JOY_KEY_CODE_MAP_START = 315


    override fun onCreate(savedInstanceState: Bundle?) {
        initUIFeature()// 设置窗口特性
        super.onCreate(savedInstanceState)
        LogEx.i("RTC Activity onCreate called");
        setContentView(R.layout.activity_play_game_rtc)
        initAudioManager()
        initP2PClient()
        var outMetrics = DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(outMetrics);
        screenWidth = outMetrics.widthPixels;
        screenHeight = outMetrics.heightPixels;

        controller = selectGamePad()
        onConnectRequest(P2PHelper.serverIP, P2PHelper.peerId, P2PHelper.clientId)
        chkStatusTitle.setOnClickListener {
            tvMyStatus.visibility = if (chkStatusTitle.isChecked) View.VISIBLE else View.GONE
        }
        mIm = getSystemService(Context.INPUT_SERVICE) as InputManager
        mIm.registerInputDeviceListener(this, null)
    }

    override fun onResume() {
        super.onResume()
        hideStatusBar()
        LogEx.e("RTC Activity onResume called");
    }

    override fun onDestroy() {
        super.onDestroy()
        LogEx.e("RTC Activity onDestroy called");
        handler?.removeMessages(AppConst.MSG_SHOW_CONTROLLER)
        cancel()
    }

    override fun onBackPressed() {
        LogEx.e("RTC Activity onBackPressed called");
        Message.obtain(handler, AppConst.MSG_QUIT, AppConst.EXIT_NORMAL).sendToTarget()
    }

    private fun initAudioManager() {
        LogEx.e("initAudioManager");
        AudioHelper.getInstance(this)
    }

    /**
     * 初始化窗口特性
     */
    private fun initUIFeature() {
        StatusBarUtil.setTranslucentStatus(this)
        // 全屏，无状态，无导航栏
        this.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );//去掉信息栏
        hideStatusBar()
    }

    private fun hideStatusBar() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            val v = this.window.decorView
            v.systemUiVisibility = View.GONE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val decorView = window.decorView
            val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            decorView.systemUiVisibility = uiOptions
        }
    }

    /**
     * 初始化P2P客户端
     */
    private fun initP2PClient() {
        LogEx.e("initP2PClient called");
        P2PHelper.init(this, object : P2PClient.P2PClientObserver {
            override fun onDataReceived(peerId: String, message: String) {
                if (!JSONObject(message).isNull("type")) {
                    val type = JSONObject(message).getString("type")
                }
            }

            override fun onStreamAdded(remoteStream: RemoteStream) {
                LogEx.e("onStreamAdded called");
                runOnUiThread { if (!isFirst) fitScreenSize() }
                this@PlayGameRtcActivity.remoteStream = remoteStream
                remoteStream.addObserver(object : owt.base.RemoteStream.StreamObserver {
                    override fun onUpdated() {
                        LogEx.e(" remoteStream updated")
                    }

                    override fun onEnded() {
                        remoteStreamEnded = true
                    }
                })
            }

            override fun onServerDisconnected() {
                LogEx.e("服务连接断开")
                Message.obtain(getHandler(), AppConst.MSG_QUIT, AppConst.EXIT_DISCONNECT)
                    .sendToTarget()
            }

        })

        initFullRender()
    }

    private fun initTCPListener() {
        val thread = Thread(Runnable {
            try {
                //Replace below IP with the IP of that device in which server socket open.
                //If you change port then change the port number in the server side code also.
                // 10.239.93.171
                // 153.35.78.77
                val pTCPPort = Pattern.compile("\\d+")
                val mTCPPort: Matcher = pTCPPort.matcher(P2PHelper.peerId)
                var nTCPPort = 9017
                if (mTCPPort.find()) {
                    nTCPPort = mTCPPort.group().toInt() + 9017;
                }
                val s = Socket(P2PHelper.strIP, nTCPPort)
                s.setTcpNoDelay(true)
                val mInputStream = DataInputStream(s.getInputStream())
                var nCountInput = 0
                while (true) {
                    var i = 0
                    var length = 0
                    while (i < 4) {
                        val res = mInputStream.read()
                        if (res < 0) {
                            Log.d("test", "initTCPListener: read length failed")
                            break
                        }
                        length += res shl i * 8
                        i++
                    }
                    if (length <= 0 || length > 512) {
                        Log.d("test", "initTCPListener: read length is not right")
                        break
                    } else {
                        val buf = ByteArray(length)
                        val ret = mInputStream.read(buf, 0, length)
                        val strRev = String(buf)
                        if (ret < 0) {
                            Log.d("test", "initTCPListener------read failed")
                        } else {
                            //Log.d("test", "From Server :        " + strRev)
                            nCountInput++
                            val strSplit: Array<String> =
                                strRev.split("size ".toRegex()).toTypedArray()
                            if (strSplit.size > 1) {
                                Trace.beginSection("atou C3 ID: " + nCountInput + " size: " + strSplit[1].toInt())
                                Trace.endSection()
                            }
                        }
                    }
                }
                s.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        })

        thread.start()
    }

    /**
     * 解析服务器回传的鼠标数据
     */
    private fun parseMouseCursor(message: String) = launch(Dispatchers.IO) {
        val bean = MouseBean.objectFromData(message)

        var top = 0
        var left = 0
        var curWidth = 0
        var curHeight = 0
        val dst = bean.dstRect
        dst?.let {
            top = it.top
            left = it.left
            curWidth = bean.width
            curHeight = bean.height
        }
        LogEx.i("${message.length}")
        try {
            withContext(Dispatchers.Main) {
                controller?.setMouseData(
                    left + (paddingSize / 2), top,
                    curWidth, curHeight, bean.isVisible
                )
            }
            LogEx.i("${bean.cursorData?.size} ${bean.isNoShapeChange}")
            if (!bean.cursorData.isNullOrEmpty() && !bean.isNoShapeChange) {
                // 解析鼠标光标
                val bmpCursor = buildCursor(bean.width, bean.height, bean.cursorData)
                withContext(Dispatchers.Main) {
                    controller?.setMouseCursor(bmpCursor)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 将服务端回传的鼠标图形数组转换成Bitmap对象
     */
    private fun buildCursor(width: Int, height: Int, curData: List<Byte>): Bitmap {
        val bgraData = ByteArray(width * height * 4)
        System.arraycopy(
            curData.toTypedArray().toByteArray(),
            0,
            bgraData,
            0,
            width * height * 4
        )
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val buf = ByteBuffer.wrap(bgraData)
        bitmap.copyPixelsFromBuffer(buf)
        return bitmap
    }

    /**
     * 初始化全屏渲染
     */
    private fun initFullRender() {
        SingletonSurfaceView.getInstance().setSurfaceView(fullRenderer)
    }

    /**
     * 向信令服务器发登录请求
     * @param server 信令服务器的地址
     * @param myId 给当前设备设置个名称
     */
    private fun onConnectRequest(server: String, peerid: String, myId: String) {
        // 登录信令服的参数
        LogEx.e("onConnectRequest called");
        val jsonLogin = JSONObject(mapOf("host" to server, "token" to myId)).toString()
        LogEx.e("$jsonLogin")

        // 连接信令服
        P2PHelper.getClient()?.let {
            it.addAllowedRemotePeer(peerid)
            it.connect(jsonLogin, object : ActionCallback<String> {
                override fun onSuccess(result: String) {
                    LogEx.e("$result ${Thread.currentThread().name}")
                    runOnUiThread {
                        onCallRequest(P2PHelper.peerId);
                    }

                }

                override fun onFailure(error: OwtError) {
                    LogEx.e("${error.errorMessage} ${error.errorCode}")
                    runOnUiThread {
                        longToast("连接服务器失败 ${error.errorMessage} ${error.errorCode}")
                        finish()
                    }
                }
            })
        }
    }

    /**
     * 获取远程服务端数据流并显示画面
     */
    private fun onCallRequest(peerId: String) {
        inCalling = true
        this.peerId = peerId
        LogEx.e("onCallRequest called");
        // 添加服务端ID
        P2PHelper.getClient()?.addAllowedRemotePeer(peerId)
        P2PHelper.getClient()?.stop(peerId);
        P2PHelper.getClient()?.send(peerId, "start", object : ActionCallback<Void> {
            override fun onSuccess(result: Void?) {
                LogEx.e("start message send success ${Thread.currentThread().name}")
                sendSizeChange() // 发送窗口尺寸消息给信令服
                initJoyStickDevices()

                runOnUiThread {
                    // 实例一个消息机制用于定时刷新信令服状态

                    lifecycle.addObserver(object : LifecycleObserver {
                        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                        fun onDestroy() {
                            LogEx.e(" webrtc onDestroy called");
                            // handler.removeCallbacksAndMessages(null)
                        }
                    })
                }

            }

            override fun onFailure(error: OwtError?) {
                LogEx.e(error?.errorMessage + " " + error?.errorCode)
            }
        })
    }

    private fun getStatus(id: String) {
        P2PHelper.getClient()?.getStats(id, object : ActionCallback<RTCStatsReport> {
            override fun onSuccess(report: RTCStatsReport) {
                LogEx.i("$id:$report")

                var videoBytesR = ""
                var videoLostRate = 0.0
                var audioBytesR = ""
                var audioLostRate = 0.0
                var width = 0
                var height = 0
                report.statsMap.values.forEach {
                    if (it.type == "inbound-rtp") {
                        LogEx.i("${it.timestampUs}")
                        if (firstTimestamp == 0L) firstTimestamp = it.timestampUs.toLong()
                        val timestamp = it.timestampUs.toLong()

                        val br = it.members["bytesReceived"].toString().toLong()
                        val kbps =
                            if (br > 0 && (timestamp - firstTimestamp) > 0)
                                br / 1024 / ((timestamp - firstTimestamp) / 1000000)
                            else
                                0L
                        LogEx.i("$br $kbps}")

                        val pr = it.members["packetsReceived"].toString().toLong()
                        val pl = it.members["packetsLost"].toString().toLong()
                        val fl = it.members["fractionLost"].toString().toDouble()
                        LogEx.i("$pr $pl $fl ${it.members["mediaType"]}")

                        if (it.members["mediaType"] == "video") {
                            videoBytesR = "$kbps kbps"
                            videoLostRate = fl
                        } else {
                            audioBytesR = "$kbps kbps"
                            audioLostRate = fl
                        }

                    }
                    if (it.type == "track") {
                        if (it.members["kind"] == "video" && it.members["frameWidth"] != null && it.members["frameHeight"] != null) {
                            width = it.members["frameWidth"]?.toString()?.toInt() ?: 0
                            height = it.members["frameHeight"]?.toString()?.toInt() ?: 0
                        }
                    }
                }
                val time = DateTimeUtils.long2strDate("HH:mm:ss", System.currentTimeMillis())
                val clientReport = "--- inBound ---" +
                        "\nCurrent Time: $time" +
                        "\nScreen Size: $width X $height" +
                        "\nVideo BytesReceived: $videoBytesR" +
                        "\nVideo PacketLostRate: $videoLostRate" +
                        "\n" +
                        "\nAudio BytesReceived: $audioBytesR" +
                        "\nAudio PacketLostRate: $audioLostRate"
                runOnUiThread {
                    tvMyStatus.text = clientReport
                    val show = FastSharedPreferences.get("show_status").getBoolean("show", false)
                    layoutStatus.visibility = if (show) View.VISIBLE else View.GONE
                    tvMyStatus.visibility =
                        if (chkStatusTitle.isChecked) View.VISIBLE else View.GONE
                }
            }

            override fun onFailure(err: OwtError) {
                LogEx.e(err.errorMessage + " " + err.errorCode)
            }
        })
    }

    private inner class StatusHandler : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }

    /**
     * 调整视频画面的比例
     */
    private fun fitScreenSize() {
        // 获取渲染器的宽和高，并根据服务器屏幕宽和高的比例计算出手机端屏幕的内边距
        viewWidth = fullRenderer.width
        viewHeight = fullRenderer.height
        val paddingWidth =
            if (viewWidth > SERVER_SCREEN_WIDTH)
                viewWidth - (SERVER_SCREEN_WIDTH * viewHeight / SERVER_SCREEN_HEIGHT)
            else 0

        val paddingHeight =
            if (viewHeight > SERVER_SCREEN_HEIGHT)
                viewHeight - SERVER_SCREEN_HEIGHT
            else 0
        LogEx.i(" $paddingWidth $viewWidth $viewHeight ${(SERVER_SCREEN_WIDTH * viewHeight / SERVER_SCREEN_HEIGHT)}")
        // 发送调整后的渲染器尺寸
        sendSizeChange()
        controller?.setViewDimenson(
            viewWidth,
            viewHeight,
            0,
            0
        )
        isFirst = true
    }

    /**
     * 向服务器端发送渲染器的尺寸，服务端在收到鼠标坐标时会根据这个尺寸换算成服务器屏幕的坐标值
     */
    private fun sendSizeChange() {
        val mapScreenSize = mapOf(
            "width" to screenWidth,
            "height" to screenHeight
        )
        val mapRenderSize = mapOf(
            "width" to viewWidth,
            "height" to viewHeight
        )

        val mapParams = mapOf(
            "rendererSize" to mapRenderSize,
            "screenSize" to mapScreenSize,
            "mode" to "stretch"
        )
        val mapData = mapOf(
            "event" to "sizechange",
            "parameters" to mapParams
        )
        val mapKey = mapOf(
            "type" to "control",
            "data" to mapData
        )
        val jsonString = JSONObject(mapKey).toString()
        LogEx.e(jsonString)
        //  longToast("Ben start to play a game server:"+ viewWidth + " client:"+ viewHeight);
        P2PHelper.getClient()
            ?.send(P2PHelper.peerId, jsonString, object : P2PHelper.FailureCallBack<Void>() {
                override fun onFailure(err: OwtError?) {
                    LogEx.e("${err?.errorMessage} ${err?.errorCode}")
                }
            })
    }

    /**
     * 初始化游戏手柄
     */
    private fun selectGamePad(): BaseController {
        return when (intent.getStringExtra("controller").toUpperCase()) {
            RTCControllerXBox.NAME -> RTCControllerXBox(this, getHandler(), this)
            RTCControllerFPS.NAME -> RTCControllerFPS(this, getHandler(), this)
            RTCControllerRAC.NAME -> RTCControllerRAC(this, getHandler(), this)
            RTCControllerACT.NAME -> RTCControllerACT(this, getHandler(), this)
            RTCControllerMouse.NAME -> RTCControllerMouse(this, getHandler(), this)
            RTCControllerAndroid.NAME -> RTCControllerAndroid(this, getHandler(), this)
            else -> RTCControllerXBox(this, getHandler(), this)
        }
    }

    private fun getHandler(): Handler {
        if (handler == null) handler = GameHandler(this)
        return handler as Handler
    }

    /**
     * 自定义消息机制，接收来自GAClient的消息后，根据消息的what值做不同的处理
     */
    class GameHandler internal constructor(act: PlayGameRtcActivity) : Handler() {
        private val ref: WeakReference<PlayGameRtcActivity> = WeakReference(act)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val act = ref.get()
            when (msg.what) {
                AppConst.MSG_QUIT -> {
                    LogEx.i("Exit Result" + msg.arg1)
                    act?.let {
                        val intent = it.intent
                        intent.putExtra(RESULT_MSG, msg.arg1)
                        it.setResult(Activity.RESULT_OK, intent)
                        it.finish()
                    }
                }
                AppConst.MSG_SHOW_CONTROLLER -> act?.showOrHideController()
                AppConst.MSG_UPDATE_CONTROLLER -> act?.updateControllerStatus()
            }
        }
    }

    /**
     * 在指定时长后隐藏游戏控制器（手柄）
     */
    private fun showOrHideController() {
        if (controller == null || handler == null) return
        updateControllerStatus()
        handler?.sendEmptyMessageDelayed(AppConst.MSG_SHOW_CONTROLLER, 1000)
    }

    private fun updateControllerStatus() {
        if ((System.currentTimeMillis() - BaseController.lastTouchMillis) > 10000)
            controller?.view?.alpha = 0f
        else {
            controller?.view?.alpha = 1f
        }
    }

    override fun switchKeyBoard() {
        controller?.let { layoutController.removeView(it.view) }
        controller = RTCControllerKeyBoard(this, getHandler(), this)
    }

    override fun switchMapperPad() {
        onCallRequest("ga");
    }

    override fun switchGamePad() {
        controller?.let { layoutController.removeView(it.view) }
        controller = RTCControllerXBox(this, getHandler(), this)
    }

    override fun showDeviceMenu() {
        controller?.view?.visibility = View.GONE
        // 初始化菜单布局
        val view = LayoutInflater.from(this).inflate(R.layout.game_device_switch, null, false)
        // 实例化对话框并加载布局
        val pw = PopupWindow(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        initRvController(view, pw)
        view.findViewById<View>(R.id.ibtnExit).setOnClickListener { pw.dismiss() }
        // 设置对话框属性
        pw.setOnDismissListener { controller?.view?.visibility = View.VISIBLE }
        pw.contentView = view
        pw.setBackgroundDrawable(ColorDrawable())
        pw.isOutsideTouchable = true
        pw.showAtLocation(layoutContainer, Gravity.CENTER, 0, 0)
    }

    private fun initRvController(view: View, pw: PopupWindow) {
        val mapCtrl = SimpleArrayMap<String, String>()
        mapCtrl.put(RTCControllerXBox.DESC, RTCControllerXBox.NAME)
        mapCtrl.put(RTCControllerFPS.DESC, RTCControllerFPS.NAME)
        mapCtrl.put(RTCControllerRAC.DESC, RTCControllerRAC.NAME)
        mapCtrl.put(RTCControllerACT.DESC, RTCControllerACT.NAME)
        mapCtrl.put(RTCControllerKeyBoard.DESC, RTCControllerKeyBoard.NAME)

        val listDesc = ArrayList<String>()
        for (i in 0 until mapCtrl.size()) {
            listDesc.add(mapCtrl.keyAt(i))
        }

        val rvController = view.findViewById<RecyclerView>(R.id.rvController)
        rvController.layoutManager = LinearLayoutManager(this)
        rvController.adapter =
            object : CommonAdapter<String>(this, listDesc, R.layout.item_controller) {
                override fun convert(vh: ViewHolder, ctrlDesc: String) {
                    vh.setText(R.id.chkController, ctrlDesc)
                    vh.setChecked(R.id.chkController, controller?.description == ctrlDesc)
                    vh.setOnClickListener(R.id.chkController) {
                        pw.dismiss()
                        if (ctrlDesc == RTCControllerKeyBoard.DESC) {
                            pw.dismiss()
                            switchKeyBoard()
                        } else {
                            intent.putExtra("controller", mapCtrl.get(ctrlDesc))
                            switchMapperPad()
                        }
                    }
                }

            }
    }

    /**
     * 在线心跳包
     */
    private fun requestOnline(gameId: Int, gameName: String) {
    }

    private fun initJoyStickDevices() {
        val devices = InputDevice.getDeviceIds()
        for (i in devices.indices) {
            val deviceId = devices[i]
            val device = InputDevice.getDevice(deviceId)
            if (device != null) {
                if (device.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
                    val joyId = RTCControllerAndroid.getDeviceSlotIndex(deviceId)
                    sendJoyStickEvent(BaseController.EV_NON, 0, 0, true, joyId)
                }
            }
        }
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        val device = InputDevice.getDevice(deviceId)
        val source =  device.sources
        if (source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
            || source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) {
            val joyId = RTCControllerAndroid.getDeviceSlotIndex(deviceId)
            sendJoyStickEvent(BaseController.EV_NON, 0, 0, true, joyId)
        } else {
            Log.d(
                RTCControllerAndroid.TAG,
                "Bluetooth Device source:  " + InputDevice.getDevice(deviceId)
                    .sources
            )
        }
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        val joyId = RTCControllerAndroid.updateDeviceSlot(deviceId)
        if (joyId != -1) {
            sendJoyStickEvent(BaseController.EV_NON, 0, 0, false, joyId)
        } else {
            Log.d(RTCControllerAndroid.TAG, "This is not joystick: $deviceId")
        }

        //Log.d(TAG, "Bluetooth Device name: " + InputDevice.getDevice(deviceId).getName());
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        Log.d(
            RTCControllerAndroid.TAG,
            "onInputDeviceChanged" + InputDevice.getDevice(deviceId).name
        )
    }

    fun sendJoyStickEvent(
        type: Int,
        keyCode: Int,
        keyValue: Int,
        enableJoy: Boolean,
        joyId: Int
    ) {
        val meb = MotionEventBean()
        meb.type = "control"
        meb.data = DataBean()
        meb.data.event = "isGamepad"
        meb.data.parameters = ParametersBean()
        meb.data.parameters.setgpID(joyId)
        if (BaseController.EV_NON == type) {
            if (enableJoy) {
                meb.data.parameters.setData("gpEnable")
            } else {
                meb.data.parameters.setData("gpDisable")
            }
        } else {
            var data: String? = null
            if (BaseController.EV_ABS == type) {
                data = "a $keyCode $keyValue\n"
            } else if (BaseController.EV_KEY == type) {
                data = "k $keyCode $keyValue\n"
            }
            if (data != null) {
                meb.data.parameters.setData(data)
            }
        }
        val jsonString = Gson().toJson(meb, MotionEventBean::class.java)
        //LogEx.d(jsonString);
        P2PHelper.getClient()
            .send(P2PHelper.peerId, jsonString, object : FailureCallBack<Void?>() {
                override fun onFailure(owtError: OwtError) {
                    LogEx.e(owtError.errorMessage + " " + owtError.errorCode + " " + jsonString)
                }
            })
    }
}

