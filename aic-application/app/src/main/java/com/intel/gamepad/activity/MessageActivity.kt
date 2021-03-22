package com.intel.gamepad.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.intel.gamepad.R
import org.jetbrains.anko.support.v4.startActivity

class MessageActivity : BaseActvitiy() {
    companion object {
        fun actionStartFragment(frg: Fragment) {
            frg.startActivity<MessageActivity>()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        initView()
    }

    private fun initView() {
        initTitleString(R.id.tvTitle)
        initBackButton(R.id.ibtnBack)
    }
}
