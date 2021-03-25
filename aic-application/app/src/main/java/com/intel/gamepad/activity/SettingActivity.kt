package com.intel.gamepad.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.intel.gamepad.R
import com.intel.gamepad.utils.ActivityManager
import com.intel.gamepad.utils.IPUtils
import com.intel.gamepad.utils.LanguageUtils
import com.jeremy.fastsharedpreferences.FastSharedPreferences
import com.mycommonlibrary.utils.DialogUtils
import com.mycommonlibrary.utils.PackageUtils
import kotlinx.android.synthetic.main.activity_setting.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.support.v4.startActivity
import java.util.*

class SettingActivity : BaseActvitiy(), CompoundButton.OnCheckedChangeListener {
    companion object {
        fun actionStart(act: AppCompatActivity) {
            act.startActivity<SettingActivity>()
        }

        fun actionStartFragment(frg: Fragment) {
            frg.startActivity<SettingActivity>()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        initView()
    }

    private fun initView() {
        initTitleString(R.id.tvTitle)
        initBackButton(R.id.ibtnBack)

        if (LanguageUtils.getLocale() == null) tvLanguage.text = "English"
        else
            tvLanguage.text = if (LanguageUtils.getLocale().toString() == "zh") "中文" else "English"
        layoutLanguage.setOnClickListener { onChooseLanguage() }

        chkShowGetStatus.setOnCheckedChangeListener(this)
        chkShowGetStatus.isChecked =
            FastSharedPreferences.get("show_status").getBoolean("show", false)

        etIP.setText(IPUtils.loadIP())
        etIP.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { IPUtils.saveip(it.toString()) }
            }

        })
    }

    private fun onChooseLanguage() {
        val items = arrayOf("中文", "English")
        DialogUtils.showSingleSelectDialog(
            this, resources.getString(R.string.choose_language),
            items, tvLanguage,
            @SuppressLint("HandlerLeak")
            object : Handler() {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    switchLanguage(msg.obj.toString())
                }
            }
        )
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (buttonView == chkShowGetStatus) saveShowStatus(isChecked)
    }

    private fun saveShowStatus(isShow: Boolean) {
        FastSharedPreferences.get("show_status").edit { putBoolean("show", isShow) }
    }

    private fun switchLanguage(lang: String) {
        when (lang) {
            "中文" -> LanguageUtils.updateLocale(this, Locale.CHINESE)
            "English" -> LanguageUtils.updateLocale(this, Locale.ENGLISH)
        }
        PackageUtils.resetApp(this)
    }
}
