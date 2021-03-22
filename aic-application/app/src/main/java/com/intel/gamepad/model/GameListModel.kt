package com.intel.gamepad.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intel.gamepad.app.AppConst
import com.intel.gamepad.bean.GameListBean
import com.intel.gamepad.utils.IPUtils
import com.lzy.okgo.OkGo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import java.lang.Exception

class GameListModel : ViewModel() {
    var listData: MutableLiveData<List<GameListBean>> = MutableLiveData()
    val errorMessage: MutableLiveData<String> = MutableLiveData()
    val showLoading = MutableLiveData<Boolean>()

    fun requestGameList() = viewModelScope.launch {
        showLoading.value = true
        var response: Response? = null
        withContext(Dispatchers.IO) {
            response = try {
                OkGo.post<String>(IPUtils.loadIP() + AppConst.SERVLET_URL_PORT).execute()
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage.postValue(e.message)
                showLoading.postValue(false)
                null
            }
        }
        response?.let {
            if (it.code == 200) {
                val json = it.body?.string()
                val list = GameListBean.arrayGameListBeanFromData(json)
                listData.value = list
            }
        }
        showLoading.value = false
    }
}