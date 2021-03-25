package com.intel.gamepad.app

import androidx.core.content.edit
import com.intel.gamepad.bean.LoginBean
import com.jeremy.fastsharedpreferences.FastSharedPreferences
import jp.wasabeef.glide.transformations.internal.FastBlur

object UserHelper {
    const val FILE = "userHelper"
    fun save(name: String, password: String) {
        FastSharedPreferences.get(FILE)
            .edit {
                putString("username", name)
                putString("password", password)
            }
    }

    fun getUserName() = FastSharedPreferences.get(FILE).getString("username", "")
    fun getPassword() = FastSharedPreferences.get(FILE).getString("password", "")
    fun clear() = FastSharedPreferences.get(FILE).edit().clear().commit()


    fun getId() = FastSharedPreferences.get(FILE).getInt("id", -1)

    fun saveId(id: Int) {
        FastSharedPreferences.get(FILE)
            .edit {
                putInt("id", id)
            }
    }

    fun saveName(username: String?) {
        FastSharedPreferences.get(FILE)
            .edit { putString("loginName", username) }
    }

    fun loadName() = FastSharedPreferences.get(FILE).getString("loginName", "")
}