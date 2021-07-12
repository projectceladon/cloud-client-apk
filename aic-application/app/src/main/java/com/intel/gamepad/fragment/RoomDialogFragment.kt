package com.intel.gamepad.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.intel.gamepad.R
import com.intel.gamepad.activity.GameDetailActivity
import com.intel.gamepad.app.AppConst
import com.intel.gamepad.app.UserHelper
import com.intel.gamepad.bean.RoomBean
import com.intel.gamepad.utils.IPUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.mcxtzhang.commonadapter.rv.CommonAdapter
import com.mcxtzhang.commonadapter.rv.ViewHolder
import com.mycommonlibrary.utils.LogEx
import com.mycommonlibrary.view.loadingDialog.LoadingDialog
import kotlinx.android.synthetic.main.activity_game_detail.view.*
import kotlinx.android.synthetic.main.dlg_room_list.view.*
import org.jetbrains.anko.support.v4.toast
import java.text.FieldPosition

/**
 * 游戏房间列表框
 * 多人游戏：可以自己创建房间让别人加入，也可以加入别人创建的房间
 * 创建房间会先进入游戏点位节点，点位成功后再创建房间
 * 加入房间会根据房间的节点IP加入游戏。
 */
class RoomDialogFragment : DialogFragment() {
    companion object {
        fun newInstance(gameId: Int) = RoomDialogFragment().apply {
            arguments = Bundle().apply {
                putInt("gameId", gameId)
            }
        }
    }

    private val listRoom = mutableListOf<RoomBean>()
    private var rvRoom: RecyclerView? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        //return super.onCreateDialog(savedInstanceState)
        val viewDialog = LayoutInflater.from(activity).inflate(R.layout.dlg_room_list, null)
        initView(viewDialog)
        val dialog = AlertDialog.Builder(activity).setView(viewDialog).create()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onDestroy() {
        super.onDestroy()
        // 界面销毁时取消正在连接的网络接口（防止内存泄漏）
        OkGo.getInstance().cancelTag(this)
    }

    override fun onStart() {
        super.onStart()
        requestRoomList() // 获取房间列表
    }

    private fun initView(viewDialog: View) {
        viewDialog.btnCancel.isFocusable = true
        viewDialog.btnCancel.setOnClickListener { this.dismiss() }
        viewDialog.btnCreateRoom.isFocusable = true
        viewDialog.btnCreateRoom.setOnClickListener { onCreateRoom() }
        // 初始化列表
        viewDialog.rvRoom.layoutManager = LinearLayoutManager(context)
        viewDialog.rvRoom.adapter =
            object : CommonAdapter<RoomBean>(context, listRoom, R.layout.item_room) {
                override fun convert(vh: ViewHolder, item: RoomBean) {
                    vh.setText(R.id.tvRoomName, item.roomname)
                    //vh.itemView.btnJoin.isFocusable = true
                    vh.setOnClickListener(R.id.btnJoin) {
                        onJoin(vh.adapterPosition)
                    }
                    vh.itemView.setOnClickListener {
                        onJoin(vh.adapterPosition)
                    }
                }
            }
        this.rvRoom = viewDialog.rvRoom
    }

    private fun onJoin(position: Int) {
        activity?.let {
            // 将房间当前人数发给GameDetailActivity
            val rc = listRoom[position].roomcount
            LiveEventBus.get("roomCount").post(rc)
            // 调加入游戏接口
            val act = it as GameDetailActivity
           // act.requestJoinGame()
            // 关对话框
            dismiss()
        }
    }

    private fun onCreateRoom() {
        activity?.let {
           // (it as GameDetailActivity).requestStartGame(true)
            dismiss()
        }
    }

    /**
     * 获取房间列表
     */
    private fun requestRoomList() {
        val gameId = arguments?.getInt("gameId") ?: 0
        LogEx.i(gameId.toString())
        OkGo.post<String>(IPUtils.loadIP() + AppConst.ROOM_LIST)
            .tag(this)
            .params("gameid", gameId)
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>) {
                    LogEx.i(response.body())
                    val list = RoomBean.arrayRoomBeanFromData(response.body())
                    updateRoomList(list)
                }
            })
    }

    /**
     * 刷新列表
     */
    private fun updateRoomList(list: List<RoomBean>) {
        listRoom.clear()
        listRoom += list
        rvRoom?.adapter?.notifyDataSetChanged()
    }
}