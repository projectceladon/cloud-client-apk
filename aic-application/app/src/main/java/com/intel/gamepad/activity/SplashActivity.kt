package com.intel.gamepad.activity

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.intel.gamepad.BuildConfig
import com.intel.gamepad.R
import com.intel.gamepad.app.AppConst
import com.intel.gamepad.app.UserHelper
import com.intel.gamepad.utils.IPUtils
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.mycommonlibrary.utils.LogEx
import com.mycommonlibrary.utils.StatusBarUtil
import com.mycommonlibrary.view.loadingDialog.LoadingDialog
import org.jetbrains.anko.longToast
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class SplashActivity : BaseActvitiy() {

    override fun onCreate(savedInstanceState: Bundle?) {
        windowFullScreen()
        super.onCreate(savedInstanceState)
        if(isDeviceRooted()) {
            longToast("This is rooted device!")
        }
        setContentView(R.layout.activity_splash)
    }

    override fun onStart() {
        super.onStart()
        GameDetailActivity.actionStart(this)
    }

    fun isDeviceRooted(): Boolean {
        return checkRootMethodOne() || checkRootMethodTwo() || checkRootMethodThree()
    }

    private fun checkRootMethodOne(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethodTwo(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkRootMethodThree(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val `in` = BufferedReader(InputStreamReader(process.inputStream))
            if (`in`.readLine() != null) true else false
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    private fun windowFullScreen() {
        this.window.requestFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            val v = this.window.decorView
            v.systemUiVisibility = View.GONE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val decorView = window.decorView
            val uiOptions = (
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LOW_PROFILE
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            decorView.systemUiVisibility = uiOptions
        }
    }

    private fun requestLogin(userName: String, password: String) {
        val dlg = LoadingDialog(this)
        GameDetailActivity.actionStart(this@SplashActivity)
        finish()
    }
}
