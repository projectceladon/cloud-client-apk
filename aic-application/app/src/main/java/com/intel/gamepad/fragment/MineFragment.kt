package com.intel.gamepad.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.intel.gamepad.R
import com.intel.gamepad.activity.LoginActivity
import com.intel.gamepad.activity.SettingActivity
import com.intel.gamepad.app.UserHelper
import kotlinx.android.synthetic.main.fragment_mine.*

class MineFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance() = MineFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mine, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvName.text = UserHelper.getUserName()
        btnSetting.setOnClickListener {
            SettingActivity.actionStartFragment(this@MineFragment)
        }
        btnLogout.setOnClickListener {
            UserHelper.clear()
            LoginActivity.actionStart(it.context)
        }
    }
}
