package com.intel.gamepad.activity

import android.os.Bundle
import android.telephony.mbms.MbmsErrors
import androidx.fragment.app.Fragment
import com.intel.gamepad.R
import org.jetbrains.anko.support.v4.startActivity

class SearchActivity : BaseActvitiy() {
    companion object {
        fun actionStartFragment(frg: Fragment) {
            frg.startActivity<SearchActivity>()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        initView()
    }

    private fun initView() {
        initBackButton(R.id.ibtnBack)
    }
}
