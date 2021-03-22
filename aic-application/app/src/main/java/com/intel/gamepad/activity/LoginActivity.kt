package com.intel.gamepad.activity

import android.content.Context
import android.net.TrafficStats
import android.os.Bundle
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import com.intel.gamepad.R
import com.intel.gamepad.fragment.LoginFragment
import com.intel.gamepad.fragment.RegisterFragment
import com.mycommonlibrary.utils.LogEx
import com.mycommonlibrary.utils.NetDeviceUtils
import com.mycommonlibrary.utils.StatusBarUtil
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.startActivity
import kotlin.concurrent.thread

class LoginActivity : BaseActvitiy() {
    companion object {
        fun actionStart(ctx: Context) {
            ctx.startActivity<LoginActivity>()
        }
// 13661679014
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        StatusBarUtil.setStatusBarColor(this, ContextCompat.getColor(this, R.color.white))
        StatusBarUtil.setStatusBarDarkTheme(this, true)
        initView()
    }

    private fun initView() {
        initViewPager()
        ivLogo.setOnLongClickListener {
            SettingActivity.actionStart(this)
            false
        }
    }

    private fun initViewPager() {
        tabLayout.setupWithViewPager(viewPager)
        viewPager.adapter =
            object : FragmentStatePagerAdapter(
                supportFragmentManager,
                BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
            ) {
                override fun getItem(position: Int): Fragment {
                    if (position == 1)
                        return RegisterFragment.newInstance()
                    if (position == 0)
                        return LoginFragment.newInstance()
                    return Fragment()
                }

                override fun getCount(): Int {
                    return 2
                }

                override fun getPageTitle(position: Int): CharSequence? {
                    return when (position) {
                        0 -> getString(R.string.login)
                        1 -> getString(R.string.register)
                        else -> ""
                    }
                }
            }
    }
}
