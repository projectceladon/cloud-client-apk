package com.intel.gamepad.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.simplehttpurlconnectiondemo.SimpleHttpUtils
import com.intel.gamepad.R
import com.intel.gamepad.app.*
import com.intel.gamepad.app.MyApp.context
import com.intel.gamepad.bean.GameListBean
import com.intel.gamepad.bean.GameStatusBean
import com.intel.gamepad.controller.webrtc.*
import com.intel.gamepad.fragment.RatingScoreDialogFragment
import com.intel.gamepad.fragment.RoomDialogFragment
import com.intel.gamepad.owt.p2p.P2PHelper
import com.intel.gamepad.utils.IPUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.mcxtzhang.commonadapter.rv.CommonAdapter
import com.mcxtzhang.commonadapter.rv.ViewHolder
import com.mycommonlibrary.utils.DensityUtils
import com.mycommonlibrary.utils.LogEx
import com.mycommonlibrary.utils.StatusBarUtil
import com.mycommonlibrary.view.loadingDialog.LoadingDialog
import kotlinx.android.synthetic.main.activity_game_detail.*
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.include_game_cover_header.*
import kotlinx.android.synthetic.main.include_title_with_statusbar.*
import kotlinx.android.synthetic.main.item_game_image.view.*
import kotlinx.coroutines.*
import org.gaminganywhere.gaclient.PlayGameRtspActivity
import org.jetbrains.anko.longToast
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.support.v4.startActivity
import org.jetbrains.anko.toast

class GameDetailActivity : BaseActvitiy(), CoroutineScope by MainScope() {
    companion object {
        const val PARAM_BEAN = "param_bean"
        fun actionFragment(frg: Fragment, bean: GameListBean) {
            frg.startActivity<GameDetailActivity>(PARAM_BEAN to bean)
        }

        fun actionStart(act: Activity) {
            act.startActivity<GameDetailActivity>(PARAM_BEAN to GameListBean().apply {
                iid = 1
                conf = "rts"
                this.imageUrl = ""
            })
        }
    }

    private var useWebRTC = true
    private var bean: GameListBean? = null
    private var statusBean: GameStatusBean? = null
    private var repeatTimeOut = 5
    private var beginMillis = 0L
    private var nodeIP = "" // 记录需要关闭的节点IP
    private var roomCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtil.setTranslucentStatus(this)
        setContentView(R.layout.activity_game_detail)
        initView()
        initRoomNoEvent()
        loadData()


        //Set the Android controller as the default one.
        chkAndroid.setChecked(true)
        etServerIP.setText(IPUtils.loadIP())
        P2PHelper.serverIP = IPUtils.loadIP()
        etServerIP.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    IPUtils.saveip(it.toString())
                    P2PHelper.serverIP = IPUtils.loadIP()
                }
            }
        })


        etPeerID.setText(IPUtils.loadPeerID())
        P2PHelper.peerId = IPUtils.loadPeerID()
        etPeerID.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    IPUtils.savepeerid(it.toString())
                    P2PHelper.peerId = IPUtils.loadPeerID()
                }
            }
        })

        etClientID.setText(IPUtils.loadTokenID())
        P2PHelper.clientId= IPUtils.loadTokenID()
        etClientID.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    IPUtils.savetoken(it.toString())
                    P2PHelper.clientId = IPUtils.loadTokenID()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    /**
     * 在多人游戏时接收由房间列表对话框发来的房间现有人数
     * 根据这个人数选择ga?和client?
     */
    private fun initRoomNoEvent() {
        LiveEventBus.get("roomCount", Int::class.java)
            .observe(this, Observer {
                roomCount = it
                LogEx.i(">>>>>$roomCount")
            })
    }

    override fun onStart() {
        super.onStart()
        bean?.let { updateGameLog(it.iid) }
    }

    private fun loadData() {
        bean = intent.getParcelableExtra(PARAM_BEAN)

        bean?.let {
           // updateHeaderDetail(it.imageUrl)
            tvTitle.text = it.title
            //tvIntro.text = it.intro
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        LogEx.i("$requestCode $resultCode $data")
        if (requestCode == PlayGameRtspActivity.REQUEST_GAME && resultCode == Activity.RESULT_OK && data != null) {
            LogEx.i("result:" + data.getIntExtra(PlayGameRtspActivity.RESULT_MSG, -1))

            val runTime = System.currentTimeMillis() - beginMillis
            GameLogHelper.saveSingleTime(bean?.iid ?: -1, runTime)
            GameLogHelper.saveTotalTime(bean?.iid ?: -1, runTime)
            LogEx.i("run time long:$runTime")

            when (data.getIntExtra(PlayGameRtspActivity.RESULT_MSG, -1)) {
                AppConst.EXIT_NORMAL -> {
                    LogEx.i("normal exit")
                    // 正常退出时关闭游戏，并显示打分界面
                    requestCloseGame()
                    //showRatingScoreDialog()
                }
                AppConst.EXIT_TIMEOUT -> {
                    LogEx.e("time out")
                    // 网络超时或断线时重复进入游戏界面
                    if (repeatTimeOut >= 0) {
                        repeatTimeOut--
                        requestGetGameStatus()
                    } else {
                        toast("该游戏暂时无法运行，建议尝试其它游戏")
                    }
                }
                AppConst.EXIT_NOHOST -> {
                    LogEx.e("no host")
                    toast("RTSP连接HOST时出错")
                }
                AppConst.EXIT_DISCONNECT -> {
                    toast("服务器连接已断开")
                }
            }
        }
    }

    private fun initView() {
        initBackButton(R.id.ibtnBack)
        //initRvGameImage()
        // 显示创建游戏房间对话框
        /* btnJoin.setOnClickListener {
            showGameRoom()
        } */
        // 跳游戏播放界面
        btnPlay.setOnClickListener {
            requestStartGame()
        }
        btnPlay.requestFocus()
    }
/*
    private fun initRvGameImage() {
        val listGameImage = assets.list("img")?.toList()?.map { "file:///android_asset/img/$it" }

        rvGameImage.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvGameImage.horItemDecoration(Color.TRANSPARENT, 6f)
        rvGameImage.adapter =
            object : CommonAdapter<String>(
                context,
                listGameImage,
                R.layout.item_game_image
            ) {
                override fun convert(vh: ViewHolder, imgUrl: String) {
                    loadImage(vh.itemView.ivGame, imgUrl, R.drawable.icon_place_hold2)
                    vh.setOnClickListener(R.id.ivGame) {
                        ImageViewActivity.actionStart(this@GameDetailActivity, imgUrl)
                    }
                }
            }
    }*/

    private fun updateGameLog(gameId: Int) {
       // tvLastTime.text = GameLogHelper.loadLastTime(gameId)
      //  tvRunCount.text = "${GameLogHelper.loadRunCount(gameId)} 次"
      //  tvSingleTime.text = GameLogHelper.loadSingleTime(gameId)
       // tvTotalTime.text = GameLogHelper.getTotalTimeString(gameId)
    }

    private fun updateHeaderDetail(url: String) {
        // 作为背景模糊处理(参数25已是模糊的最大值)
       // this.loadImageBlur(ivHeaderBg, url)
       // this.loadImage(ivGameCover, url)
    }

    /**
     * 跳转到的游戏播放界面
     */
    private fun gotoGamePlay() {
        bean?.let {
            if (useWebRTC) {
                // 根据游戏类型选择游戏手柄的类型
                it.addurl = "fps"

                if (chkAndroid.isChecked) it.addurl = "android"

                val ctrlName = when (it.addurl) {
                    "fps" -> RTCControllerFPS.NAME // 打枪
                    "rts" -> RTCControllerMouse.NAME // mouse+key
                    "rac" -> RTCControllerRAC.NAME
                    "act" -> RTCControllerACT.NAME
                    "android" -> RTCControllerAndroid.NAME
                    else -> RTCControllerXBox.NAME
                }
                // TODO:以后要改信令服的地址
                //P2PHelper.serverIP = "http://192.168.1.244:8095"
                /*when (roomCount) {
                    0 -> {
                        P2PHelper.peerId = "ga2";P2PHelper.clientId = "client2"
                    }
                    in 1..3 -> {
                        P2PHelper.peerId = "ga${roomCount}"
                        P2PHelper.clientId = "client${roomCount}"
                    }
                }
                */
                LogEx.i("ga = ${P2PHelper.peerId} client = ${P2PHelper.clientId}")


                PlayGameRtcActivity.actionStart(this, ctrlName, it.iid, it.conf)
            } else {
                PlayGameRtspActivity.actionStart(this, it.iid, it.ip, it.port)
            }

            GameLogHelper.saveLastTime(it.iid)
            GameLogHelper.saveRunCount(it.iid)
            beginMillis = System.currentTimeMillis()
        }
    }

    private fun showRatingScoreDialog() {
        RatingScoreDialogFragment.newInstance().show(supportFragmentManager, "dialog")
    }

    /**
     * 通知服务器关游戏
     */
    private fun requestCloseGame() {

        P2PHelper.closeP2PClient();
       // longToast("turn off the game")
        /*if (nodeIP.isEmpty()) return
        OkGo.get<String>(IPUtils.loadIP() + AppConst.CLOSE_GAME)
            .params("iid", bean?.iid.toString())
            .params("gamedir", MyApp.pId)
            .params("nodeIP", nodeIP)
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>) {
                    LogEx.i(response.body())
                    nodeIP = ""
                }
            })
        */
    }

    /**
     * 获取游戏的状态，用于判断是否可以玩游戏
     */
    private fun requestGetGameStatus() {
        val dlg = LoadingDialog.show(this)
        OkGo.get<String>(IPUtils.loadIP() + AppConst.GAME_STATUS)
            .params("iid", bean?.iid.toString())
            .params("gamedir", MyApp.pId)
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>) {
                    LogEx.i(response.body())
                    val result = GameStatusBean.objectFromData(response.body())
                    if (result.status == 0) {
                        statusBean = result
                    } else {
                        longToast("该游戏已被占用，请选择别的游戏！")
                    }
                }

                override fun onError(response: Response<String>) {
                    super.onError(response)
                    LogEx.e(response.exception.message)
                    longToast("连接服务器失败！请检查网络是否正常！")
                }

                override fun onFinish() {
                    super.onFinish()
                    dlg?.dismiss()
                }
            })
    }

    /**
     * 加入游戏（多人玩同一个节点的游戏）
     */
    fun requestJoinGame() = launch {
        val dlg = LoadingDialog.show(this@GameDetailActivity)
        withContext(Dispatchers.IO) {
            // 获取游戏节点IP
            nodeIP = SimpleHttpUtils.get(IPUtils.loadIP() + AppConst.JOIN_GAME, emptyMap())
        }
        dlg.dismiss()
        P2PHelper.serverIP = "http://${nodeIP}:8095/"
        gotoGamePlay() // 跳游戏播放界面
    }

    /**
     * 删除游戏房间
     * 只能删除自己创建的房间，而且这个操作最好交由服务端完成，以防断网时无法删除
     */
    fun requestDeleteRoom() = launch(Dispatchers.IO) {
        OkGo.get<String>(IPUtils.loadIP() + AppConst.DEL_ROOM)
            .params("roomname", UserHelper.getUserName())
            .execute()
    }

    /**
     * 创建游戏房间
     * 为防止房间刚创建完成游戏就被别人占了的情况，应该先启动游戏再创建房间
     */
    private fun requestCreateRoom() = launch(Dispatchers.IO) {
        val response = OkGo.post<String>(IPUtils.loadIP() + AppConst.CREATE_ROOM)
            .tag(this)
            .params("roomname", UserHelper.getUserName())
            .params("gameid", bean?.iid ?: 0)
            .params("roomtotalcount", "1234")
            .params("creatroomuserid", UserHelper.getId())
            .params("gameip", nodeIP)  // 游戏的节点IP
            .execute()?.body?.string()
        LogEx.i(response)
    }

    /**
     * 启动游戏
     * @param isCreateRoom 是否在启动成功后创建房间
     */
    fun requestStartGame(isCreateRoom: Boolean = false) {
        val dlg = LoadingDialog.show(this)
        if(isCreateRoom){
            longToast("room is not supported");
        }
        else {
            /* OkGo.get<String>(IPUtils.load() + AppConst.START_GAME)
            .params("iid", bean?.iid ?: 0)
            .params("gamedir", MyApp.pId) // 唯一码
            .params("width", DensityUtils.getmScreenHeight())
            .params("height", DensityUtils.getmScreenWidth())
            .params("fps", 60)
            .params("client", 1)
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>) {
                    LogEx.i(response.body())
                    //val result = GameStatusBean.objectFromData(response.body())
                    //if (result.isSuccess) {
                       // nodeIP = result.ip
                        P2PHelper.serverIP = "http://10.188.197.40:8095/"
                        LogEx.i(">>>> ${P2PHelper.serverIP}")
                        gotoGamePlay()

                       // if (isCreateRoom) requestCreateRoom()

                    //} else {
                        //longToast("" + result.message)
                    //}
                }

                override fun onError(response: Response<String>?) {
                    super.onError(response)
                    dlg.dismiss()
                    response?.exception?.printStackTrace()
                    LogEx.e("Error: " + response?.exception?.message)
                    toast(response?.exception?.message.toString())
                }

                override fun onFinish() {
                    super.onFinish()
                    dlg.dismiss()
                }
            })*/
            gotoGamePlay()
            dlg.dismiss()
        }
    }

    /**
     * 显示游戏房间列表对话框
     * 多人模式，可以创建房间，可以加入房间
     */
    private fun showGameRoom() {
        val iid = bean?.iid ?: 0
        RoomDialogFragment.newInstance(iid).show(supportFragmentManager, "dialog")
    }
}
