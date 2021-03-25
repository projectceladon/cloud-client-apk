package com.intel.gamepad.app

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.intel.gamepad.BuildConfig
import com.intel.gamepad.R
import com.intel.gamepad.bean.GameListBean
import com.intel.gamepad.utils.IPUtils
import com.mycommonlibrary.utils.DensityUtils
import com.mycommonlibrary.utils.LogEx
import com.mycommonlibrary.view.decoration.CommItemDecoration
import jp.wasabeef.glide.transformations.BlurTransformation

inline fun Context.loadImage(iv: ImageView, url: String, resHolder: Int = -1) {
    val urlImage =
        if (url.contains("static/")) url.replace("static/", IPUtils.loadIP())
        else url
    GlideApp.with(this)
        .load(urlImage)
        .placeholder(if (resHolder == -1) R.drawable.icon_place_hold else resHolder)
        .into(iv)
}

inline fun Fragment.loadImage(iv: ImageView, url: String, resHolder: Int = -1) {
    val urlImage =
        if (url.contains("static/")) url.replace("static/", IPUtils.loadIP())
        else url
    GlideApp.with(this)
        .load(urlImage)
        .placeholder(if (resHolder == -1) R.drawable.icon_place_hold else resHolder)
        .into(iv)
}

inline fun Activity.loadImage(iv: ImageView, url: String, resHolder: Int = -1) {
    val urlImage =
        if (url.contains("static/")) url.replace("static/", IPUtils.loadIP())
        else url
    GlideApp.with(this)
        .load(urlImage)
        .placeholder(if (resHolder == -1) R.drawable.icon_place_hold else resHolder)
        .into(iv)
}

inline fun Context.loadImageBlur(iv: ImageView, url: String, resHolder: Int = -1) {
    val urlImage =
        if (url.contains("static/")) url.replace("static/", IPUtils.loadIP())
        else url
    GlideApp.with(this)
        .load(urlImage)
        .placeholder(if (resHolder == -1) R.drawable.icon_place_hold else resHolder)
        .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 2)))
        .into(iv)
}

inline fun RecyclerView.horItemDecoration(color: Int = Color.TRANSPARENT, side: Float) {
    this.addItemDecoration(
        CommItemDecoration.createHorizontal(
            MyApp.context,
            color,
            DensityUtils.dp2px(side)
        )
    )
}

inline fun RecyclerView.verItemDecoration(color: Int = Color.TRANSPARENT, side: Float) {
    this.addItemDecoration(
        CommItemDecoration.createVertical(
            MyApp.context,
            color,
            DensityUtils.dp2px(side)
        )
    )
}

inline fun attachLocalImage(list: List<GameListBean>) {
    list.filterNot { it.conf.isNullOrEmpty() }
        .forEach {
            val name = it.conf.toLowerCase()
            it.imageUrl = when {
                name.contains("dota") -> "file:///android_asset/img/dota.jpg"
                name.contains("trine") -> "file:///android_asset/img/trine2.jpg"
                name.contains("csgo") -> "file:///android_asset/img/cs.jpg"
                name.contains("rainbow") -> "file:///android_asset/img/rainbow_six.jpg"
                name.contains("aspha") -> "file:///android_asset/img/asphalt.jpg"
                else -> ""
            }
        }
}
