package com.intel.gamepad.fragment


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import com.intel.gamepad.R
import com.intel.gamepad.app.MyApp
import com.mycommonlibrary.utils.DensityUtils
import com.mycommonlibrary.utils.LogEx
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.fragment_bluetooth.*
import org.jetbrains.anko.support.v4.toast

class BluetoothFragment : Fragment() {
    private var padLeftTop = -1
    private var padLeftLeft = -1
    private var padRightTop = -1
    private var padRightLeft = -1
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bluetooth, container, false)
    }

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTestButtonListener()
        RxPermissions(this)
            .request(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN
            )
            .subscribe {
                if (it) {
                    LogEx.i("OK")
                    checkBluetoothDevice()
                }
            }
    }

    private fun checkBluetoothDevice() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            toast("当前设备不支持蓝牙")
            tvBlueToothEnable.text = "不支持"
        } else {
            if (adapter.isEnabled) {
                tvBlueToothEnable.text = ""
                registerBleBroadCast()
                adapter.startDiscovery()// 开始搜索蓝牙设备

            } else {
                toast("蓝牙功能能未开启")
                tvBlueToothEnable.text = "未使用"
            }
        }
    }

    private fun initTestButtonListener() {
        layoutPadEvent.setOnGenericMotionListener { view, motionEvent ->
            if (padLeftTop == -1) padLeftTop = tvPadLeft.top
            if (padLeftLeft == -1) padLeftLeft = tvPadLeft.left
            if (padRightTop == -1) padRightTop = tvPadRight.top
            if (padRightLeft == -1) padRightLeft = tvPadRight.left
            LogEx.i("${motionEvent.getAxisValue(MotionEvent.AXIS_X)}")
            LogEx.i("${motionEvent.getAxisValue(MotionEvent.AXIS_Y)}")
            LogEx.i("${motionEvent.getAxisValue(MotionEvent.AXIS_Z)}")
            LogEx.i("${motionEvent.getAxisValue(MotionEvent.AXIS_RZ)}")
            LogEx.i("${motionEvent.getAxisValue(MotionEvent.AXIS_LTRIGGER)}")
            LogEx.i("${motionEvent.getAxisValue(MotionEvent.AXIS_RTRIGGER)}")
            LogEx.i("${motionEvent.getAxisValue(MotionEvent.AXIS_GAS)}")
            LogEx.i("${motionEvent.getAxisValue(MotionEvent.AXIS_BRAKE)}")

            val lx = motionEvent.getAxisValue(MotionEvent.AXIS_X)
            val ly = motionEvent.getAxisValue(MotionEvent.AXIS_Y)
            val rx = motionEvent.getAxisValue(MotionEvent.AXIS_Z)
            val ry = motionEvent.getAxisValue(MotionEvent.AXIS_RZ)

            tvPadLeft.top = (padLeftTop + ly * 10).toInt()
            tvPadLeft.left = (padLeftLeft + lx * 10).toInt()
            tvPadLeft.bottom = tvPadLeft.top + DensityUtils.dp2px(50f)
            tvPadLeft.right = tvPadLeft.left + DensityUtils.dp2px(50f)

            tvPadRight.top = (padRightTop + ry * 10).toInt()
            tvPadRight.left = (padRightLeft + rx * 10).toInt()
            tvPadRight.right = tvPadRight.left + DensityUtils.dp2px(50f)
            tvPadRight.bottom = tvPadRight.top + DensityUtils.dp2px(50f)

            false
        }
        layoutPadEvent.setOnKeyListener { v, keyCode, event ->
            LogEx.i("$keyCode $event")
            when (keyCode) {
                KeyEvent.KEYCODE_BUTTON_L1 -> updateButton(tvL1, event)
                KeyEvent.KEYCODE_BUTTON_L2 -> updateButton(tvL2, event)
                KeyEvent.KEYCODE_BUTTON_THUMBL -> updateButton(tvLThumb, event)
                KeyEvent.KEYCODE_BUTTON_R1 -> updateButton(tvR1, event)
                KeyEvent.KEYCODE_BUTTON_R2 -> updateButton(tvR2, event)
                KeyEvent.KEYCODE_BUTTON_THUMBR -> updateButton(tvRThumb, event)
                KeyEvent.KEYCODE_BUTTON_A -> updateButton(tvA, event)
                KeyEvent.KEYCODE_BUTTON_B -> updateButton(tvB, event)
                KeyEvent.KEYCODE_BUTTON_X -> updateButton(tvX, event)
                KeyEvent.KEYCODE_BUTTON_Y -> updateButton(tvY, event)
                KeyEvent.KEYCODE_BUTTON_SELECT -> updateButton(tvSelect, event)
                KeyEvent.KEYCODE_BUTTON_START -> updateButton(tvStart, event)
                KeyEvent.KEYCODE_DPAD_UP -> updateButton(tvUp, event)
                KeyEvent.KEYCODE_DPAD_DOWN -> updateButton(tvDown, event)
                KeyEvent.KEYCODE_DPAD_LEFT -> updateButton(tvLeft, event)
                KeyEvent.KEYCODE_DPAD_RIGHT -> updateButton(tvRight, event)
            }
            true
        }

    }

    private fun updateButton(btn: View, ev: KeyEvent?) {
        when (ev?.action) {
            KeyEvent.ACTION_DOWN -> btn.setBackgroundResource(R.drawable.bg_test_btn_on)
            KeyEvent.ACTION_UP -> btn.setBackgroundResource(R.drawable.bg_test_btn_off)
        }
    }

    /**
     * 注册蓝牙广播
     */
    private fun registerBleBroadCast() {
        MyApp.context.registerReceiver(bleDevice,
            IntentFilter(BluetoothDevice.ACTION_FOUND).apply {
                this.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                this.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                this.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
                this.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            })
        // 界面销毁时注销广播
        this.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                MyApp.context.unregisterReceiver(bleDevice)
                lifecycle.removeObserver(this)
            }
        })
    }

    private val bleDevice = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            LogEx.i("${intent.action}")
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                val dev =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                dev?.let {
                    LogEx.i("\n${it.address}")
                    LogEx.i("${it.name} ${it.type} ${it.bondState}")
                    LogEx.i("${it.bluetoothClass.majorDeviceClass} ${it.bluetoothClass.majorDeviceClass}")
                }

            }
        }

    }
}
