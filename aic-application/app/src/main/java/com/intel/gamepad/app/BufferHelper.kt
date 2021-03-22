package com.intel.gamepad.app

import androidx.core.content.edit
import com.jeremy.fastsharedpreferences.FastSharedPreferences

object BufferHelper {
    const val FILE = "buffer"
    fun save(url: String, data: String): Unit = FastSharedPreferences.get(FILE).edit { putString(url, data) }
    fun load(url: String): String? = FastSharedPreferences.get(FILE).getString(url, null)
}
