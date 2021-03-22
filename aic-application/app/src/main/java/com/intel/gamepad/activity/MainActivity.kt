package com.intel.gamepad.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.net.TrafficStats
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.widget.RadioButton
import com.intel.gamepad.R
import com.intel.gamepad.fragment.GameFragment
import com.intel.gamepad.fragment.HomeFragment
import com.intel.gamepad.fragment.MineFragment
import com.intel.gamepad.utils.IPUtils
import com.intel.gamepad.utils.PingUtils
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.mycommonlibrary.utils.DensityUtils
import com.mycommonlibrary.utils.LogEx
import com.mycommonlibrary.utils.NetDeviceUtils
import com.mycommonlibrary.utils.StatusBarUtil
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import kotlin.concurrent.thread

class MainActivity : BaseActvitiy() {
    companion object {
        fun actionStart(act: Activity) {
            act.startActivity<MainActivity>()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        StatusBarUtil.setTranslucentStatus(this)
        initView()
        initPermission()
        val ids = InputDevice.getDeviceIds()
        ids.forEach {
            val dev = InputDevice.getDevice(it)
            LogEx.i("" + dev)
            if (dev.sources in intArrayOf(
                    InputDevice.SOURCE_JOYSTICK,
                    InputDevice.SOURCE_GAMEPAD
                )
            ) {
                LogEx.i("has game pad")
            }
        }
    }

    /**
     * 申请APP需要的权限
     */
    @SuppressLint("CheckResult")
    private fun initPermission() {
        RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .subscribe { }
    }

    private fun initView() {
        switchFragment(R.id.tab_home)
        navBar.setOnCheckedChangeListener { _, checkedId ->
            switchFragment(checkedId)
        }
        tab_home.isChecked = true
    }

    private fun switchFragment(itemId: Int): Boolean {
        navBar.findViewById<RadioButton>(itemId).isChecked = true
        val fragment = when (itemId) {
            R.id.tab_home -> HomeFragment.newInstance()
            R.id.tab_game -> GameFragment.newInstance()
            R.id.tab_mine -> MineFragment.newInstance()
            else -> null
        }
        return fragment?.let { frg ->
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.flContainer, frg)
                .commit()
            true
        } ?: false
    }

    private var lastMillis = 0L
    override fun onBackPressed() {
        if ((System.currentTimeMillis() - lastMillis) < 1000) {
            super.onBackPressed()
            android.os.Process.killProcess(android.os.Process.myPid())
        } else {
            toast("再按一次退出")
            lastMillis = System.currentTimeMillis()
        }
    }

    override fun onStart() {
        super.onStart()
        requestShowNodeInfo()
    }

    private fun requestShowNodeInfo() {
      /*  OkGo.get<String>(IPUtils.load() + "user/shownodeinfor")
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>) {
                    LogEx.i(response.body())
                }
            })*/
    }

}
