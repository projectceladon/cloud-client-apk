package com.intel.gamepad.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.intel.gamepad.BuildConfig

import com.intel.gamepad.R
import com.intel.gamepad.activity.MainActivity
import com.intel.gamepad.activity.SettingActivity
import com.intel.gamepad.app.UserHelper
import com.intel.gamepad.bean.LoginBean
import com.intel.gamepad.bean.SuccessBean
import com.intel.gamepad.utils.IPUtils
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.mycommonlibrary.utils.LogEx
import com.mycommonlibrary.view.loadingDialog.LoadingDialog
import kotlinx.android.synthetic.main.fragment_login.*
import org.jetbrains.anko.support.v4.toast
import java.lang.Exception


/**
 * 登录
 */
class LoginFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance() = LoginFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnLogin.setOnClickListener { onLogin() }
        btnSetting.setOnClickListener { SettingActivity.actionStartFragment(this) }
    }

    private fun onLogin() {
        if (etPhone.text.isNullOrEmpty()) {
            toast(etPhone.hint)
            return
        }
        if (etPassword.text.isNullOrEmpty()) {
            toast(etPassword.hint)
            return
        }
        requestLogin()
    }

    private fun requestLogin() {
        val dlg = LoadingDialog(this.context)

       /* OkGo.get<String>(IPUtils.load() + "/user/login")
            .params("username", etPhone.text.toString())
            .params("password", etPassword.text.toString())
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>) {
                    LogEx.i(response.body())
                    //dlg.dismiss()
                    try {
                        val result = LoginBean.objectFromData(response.body())
                        UserHelper.saveId(result.id)
                        UserHelper.saveName(result.username)
                        UserHelper.save(etPhone.text.toString(), etPassword.text.toString())
                        activity?.let {
                            MainActivity.actionStart(it)
                            it.finish()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toast("登录时出错")
                    }
                }

                override fun onError(response: Response<String>?) {
                    super.onError(response)
                    dlg.dismiss()
                    response?.exception?.printStackTrace()
                    toast(response?.exception?.message ?: "connect server error")
                }

                override fun onFinish() {
                    super.onFinish()
                    dlg.dismiss()
                }
            })*/
        activity?.let {
            MainActivity.actionStart(it)
            it.finish()
        }
    }
}
