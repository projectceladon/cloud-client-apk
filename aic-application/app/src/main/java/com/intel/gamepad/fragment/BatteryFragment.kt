package com.intel.gamepad.fragment


import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import com.intel.gamepad.R
import com.intel.gamepad.app.MyApp
import com.mycommonlibrary.utils.LogEx
import kotlinx.android.synthetic.main.fragment_battery.*

/**
 * A simple [Fragment] subclass.
 */
class BatteryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_battery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBatteryReceiver()
    }

    private fun initBatteryReceiver() {
        val battery = BatteryReceiver()
        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onCreate() {
                MyApp.context.registerReceiver(battery, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                MyApp.context.unregisterReceiver(battery)
            }
        })
    }

    inner class BatteryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val current = intent.extras?.getInt("level") ?: 0// 获得当前电量
            val total = intent.extras?.getInt("scale") ?: 0// 获得总电量
            val percent = current * 100 / total
            dialProgressBar.setValue(percent.toFloat())
            ///获取电池技术支持
            val tech = intent.getStringExtra("technology")
            tvTech.text = tech

            ///获取电池状态
            val status = intent.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN
            )
            tvBatteryStatus.text = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING..."
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING..."
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT_CHARGING"
                BatteryManager.BATTERY_STATUS_UNKNOWN -> "UNKNOWN"
                else -> "--"
            }
            ///获取充电模式信息
            val plugged = intent.getIntExtra(
                BatteryManager.EXTRA_PLUGGED, 0
            )
            tvPluggle.text = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
                else -> "未充电"
            }

            val health = intent.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                BatteryManager.BATTERY_HEALTH_UNKNOWN
            )  ///获取电池健康度
            tvHealth.text = when (health) {
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "UNSPECIFIED_FAILURE"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
                BatteryManager.BATTERY_HEALTH_UNKNOWN -> "UNKNOWN"
                else -> "--"
            }
            ///获取电池电压
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            tvVolt.text = "${voltage / 1000}.${voltage % 1000}V"

            ///获取电池温度
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            tvBatteryTemp.text = "${temperature / 10}.${temperature % 10}°C"
            // LogEx.i("battery:$percent%")
        }
    }

}
