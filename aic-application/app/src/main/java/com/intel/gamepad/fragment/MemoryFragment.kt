package com.intel.gamepad.fragment


import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import com.intel.gamepad.R
import com.intel.gamepad.app.AppConst
import com.mycommonlibrary.utils.MemoryUtils
import kotlinx.android.synthetic.main.fragment_memory.*
import java.lang.ref.WeakReference

class MemoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_memory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateMemoryInfo()
        val handler = WatcherHandler(this)
        handler.sendEmptyMessage(AppConst.MSG_UPDATE_DEVICE)
        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                handler.removeMessages(AppConst.MSG_UPDATE_DEVICE)
            }
        })
    }

    private class WatcherHandler(frg: MemoryFragment) : Handler() {
        private val ref: WeakReference<MemoryFragment> = WeakReference(frg)
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            ref.get()?.updateMemoryInfo()
            sendEmptyMessageDelayed(AppConst.MSG_UPDATE_DEVICE, 1000)
        }
    }

    private fun updateMemoryInfo() {
        val totalMem = MemoryUtils.getTotalMemory()
        val freeMem = MemoryUtils.getAvailMemory()
        val usedMem = MemoryUtils.getUsedMemory()

        val appMaxMem = Runtime.getRuntime().maxMemory()
        val appTotalMem = Runtime.getRuntime().totalMemory()
        val appFreeMem = Runtime.getRuntime().freeMemory()

        dialProcMemory.setValue(((appMaxMem - appTotalMem) * 100 / appMaxMem).toFloat())
        tvSysMem.text = String.format(
            "%.1fG / %.1fG",
            usedMem.toFloat() / 1024 / 1024 / 1024,
            totalMem.toFloat() / 1024 / 1024 / 1024
        )
        tvAppMaxMem.text = String.format("%.1fM", appMaxMem.toFloat() / 1024 / 1024)
        tvAppTotal.text = String.format("%.1fM", appTotalMem.toFloat() / 1024 / 1024)
        tvAppFreeMem.text = String.format("%.1fM", appFreeMem.toFloat() / 1024 / 1024)
    }
}
