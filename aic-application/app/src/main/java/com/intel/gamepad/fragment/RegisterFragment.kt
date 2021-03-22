package com.intel.gamepad.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.intel.gamepad.R
import com.intel.gamepad.activity.MainActivity
import com.intel.gamepad.app.AppConst
import com.intel.gamepad.app.UserHelper
import com.intel.gamepad.bean.SuccessBean
import com.intel.gamepad.utils.IPUtils
import com.jeremy.fastsharedpreferences.FastSharedPreferences
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.mycommonlibrary.utils.LogEx
import com.mycommonlibrary.view.loadingDialog.LoadingDialog
import kotlinx.android.synthetic.main.fragment_register.*
import org.jetbrains.anko.support.v4.toast


/**
 * 注册
 */
class RegisterFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance() = RegisterFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FastSharedPreferences.clearCache()
        tvGetCode.setOnClickListener { onGetSmsCode() }
        btnCheckCode.setOnClickListener { onCheckCode() }
        btnRegister.setOnClickListener { onRegister() }
        layoutReg.visibility = View.GONE
    }

    private fun onCheckCode() {
        if (etPhone.text.isNullOrEmpty()) {
            toast(etPhone.hint)
            return
        }
        if (etCode.text.isNullOrEmpty()) {
            toast(etCode.hint)
            return
        }
        requestCheckCode()
    }

    private fun onGetSmsCode() {
        if (etPhone.text.isNullOrEmpty()) {
            toast(etPhone.hint)
            return
        }
        requestGetCode()
    }

    private fun onRegister() {
        if (etPhone2.text.isNullOrEmpty()) {
            toast(etPhone.hint)
            return
        }
        if (etPassword.text.isNullOrEmpty() || etPassword2.text.isNullOrEmpty()) {
            toast(etPassword.hint)
            return
        }
        if (etPassword.text.toString() != etPassword2.text.toString()) {
            toast("两次密码输入不相同")
            return
        }
        requestRegister()
    }

    /**
     * 发送短信验证码
     */
    private fun requestGetCode() {
        val dlg = LoadingDialog.show(activity)
        OkGo.get<String>(IPUtils.loadIP() + AppConst.SEND_CODE + "/" + etPhone.text.toString())
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>) {
                    LogEx.i(response.body())
                    val result = SuccessBean.objectFromData(response.body())
                    if (result.isSuccess) {
                        toast("验证码发送成功！")
                        btnCheckCode.isEnabled = true
                    } else {
                        toast("验证码发送失败！")
                        btnCheckCode.isEnabled = false
                    }
                }

                override fun onFinish() {
                    super.onFinish()
                    dlg?.dismiss()
                }
            })
    }

    /**
     * 检查验证码
     */
    private fun requestCheckCode() {
        val dlg = LoadingDialog.show(activity)
        OkGo.get<String>(IPUtils.loadIP() + AppConst.CHECK_CODE + "/" + etPhone.text + "/" + etCode.text.toString())
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>) {
                    LogEx.i(response.body())
                    val result = SuccessBean.objectFromData(response.body())
                    result.isSuccess = true
                    if (result.isSuccess) {
                        layoutGetCode.visibility = View.GONE
                        layoutReg.visibility = View.VISIBLE
                        etPhone2.setText(etPhone.text.toString())
                    } else {
                        toast(result?.message ?: "验证无效")
                    }
                }

                override fun onFinish() {
                    super.onFinish()
                    dlg?.dismiss()
                }
            })
    }

    /**
     * 注册
     */
    private fun requestRegister() {
        val dlg = LoadingDialog.show(activity)
        OkGo.post<String>(IPUtils.loadIP() + AppConst.REGISTER)
            .params("username", etPhone2.text.toString())
            .params("password", etPassword.text.toString())
            .execute(object : StringCallback() {
                override fun onSuccess(response: Response<String>) {
                    LogEx.i(response.body())
                    val result = SuccessBean.objectFromData(response.body())
                    if (result.isSuccess) {
                        toast("注册成功")
                        UserHelper.save(etPhone2.text.toString(), etPassword.text.toString())
                        activity?.let {
                            MainActivity.actionStart(it)
                            it.finish()
                        }
                    } else {
                        toast(result.message)
                    }
                }

                override fun onFinish() {
                    super.onFinish()
                    dlg?.dismiss()
                }
            })
    }
}
