package com.intel.gamepad.app

import androidx.core.content.edit
import com.jeremy.fastsharedpreferences.FastSharedPreferences
import com.mycommonlibrary.utils.DateTimeUtils
import com.mycommonlibrary.utils.LogEx

object GameLogHelper {
    const val FILE = "game_log"
    fun saveLastTime(gameId: Int): Unit =
        FastSharedPreferences.get(FILE).edit {
            putLong(
                "lastTime_$gameId",
                System.currentTimeMillis()
            )
        }

    fun loadLastTime(gameId: Int): String {
        val time = FastSharedPreferences.get(FILE).getLong("lastTime_$gameId", 0)
        return if (time == 0L) "未运行过"
        else DateTimeUtils.long2strDate("yyyy-MM-dd HH:mm:ss", time)
    }

    fun loadRunCount(gameId: Int): Int {
        return FastSharedPreferences.get(FILE).getInt("runCount_$gameId", 0)
    }

    fun saveRunCount(gameId: Int) {
        val count = loadRunCount(gameId) + 1
        FastSharedPreferences.get(FILE).edit { putInt("runCount_$gameId", count) }
    }

    fun saveSingleTime(gameId: Int, millis: Long) {
        FastSharedPreferences.get(FILE).edit { putLong("singleTime_$gameId", millis) }
    }

    fun loadSingleTime(gameId: Int): String {
        val millis = FastSharedPreferences.get(FILE).getLong("singleTime_$gameId", 0)
        return DateTimeUtils.millis2DayHourMinuteSec(millis, false)
    }

    fun saveTotalTime(gameId: Int, millis: Long) {
        val time = loadTotalTime(gameId) + millis
        FastSharedPreferences.get(FILE).edit { putLong("totalTime_$gameId", time) }
    }

    fun loadTotalTime(gameId: Int): Long {
        return FastSharedPreferences.get(FILE).getLong("totalTime_$gameId", 0)
    }

    fun getTotalTimeString(gameId: Int): String {
        val time = loadTotalTime(gameId)
        return DateTimeUtils.millis2DayHourMinuteSec(time, false)
    }

}