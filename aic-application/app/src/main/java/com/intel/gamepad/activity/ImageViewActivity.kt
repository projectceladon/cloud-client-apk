package com.intel.gamepad.activity

import android.app.Activity
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.intel.gamepad.R
import com.intel.gamepad.app.GlideApp
import kotlinx.android.synthetic.main.activity_image_view.*
import org.jetbrains.anko.startActivity


class ImageViewActivity : BaseActvitiy() {
    companion object {
        fun actionStart(act: Activity, imgUrl: String) {
            act.startActivity<ImageViewActivity>("imgUrl" to imgUrl)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)
        initView()

        val imgUrl = intent.getStringExtra("imgUrl")
        GlideApp.with(this)
            .load(imgUrl)
            .placeholder(R.drawable.icon_place_hold)
            .into(photoView)
    }

    private fun initView() {
        initBackButton(R.id.ibtnBack)
        initTitleString(R.id.tvTitle)
    }

}
