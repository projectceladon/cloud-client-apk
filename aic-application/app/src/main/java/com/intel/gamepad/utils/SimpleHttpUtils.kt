package com.android.simplehttpurlconnectiondemo

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder

object SimpleHttpUtils {
    private const val TAG = "SimpleHttpUtils"
    private const val CONNECT_TIME_OUT = 10000
    private const val READ_TIME_OUT = 10000

    /**
     * GET请求
     */
    fun get(url: String, mapParam: Map<String, String>): String {
        // 如果有参数的话就拼接参数到url后面
        val urlParam = if (mapParam == null) url else "${url}?${converMap2String(mapParam)}"
        Log.i(TAG, urlParam)
        // 构建URLConnection实例
        val connection = buildURLConnection(urlParam)
        connection.setContentType()
        connection.connect() // 建立连接
        // 从服务端获取响应码，连接成功是200
        val code = connection.responseCode
        Log.i(TAG, code.toString())
        // 根据响应码获取不同输入流
        val inStream = if (code == 200)
            connection.inputStream
        else
            connection.errorStream
        // 输入流转换成字符串
        val result = inStream.bufferedReader().lineSequence().joinToString()
        Log.i(TAG, result)
        connection.disconnect() //断开连接
        return result
    }

    /**
     * POST请求 - 参数为JSON格式
     */
    fun post(url: String, jsonParam: String?): String {
        val connection = buildURLConnection(url, false)
        connection.setContentType()
        connection.connect() // 建立连接
        // 向服务端发送请求参数
        if (!jsonParam.isNullOrEmpty()) {
            connection.outputStream.let {
                it.write(jsonParam.toByteArray(Charsets.UTF_8))
                it.flush()
                it.close()
            }
        }
        // 从服务端获取响应码，连接成功是200
        val code = connection.responseCode
        Log.i(TAG, code.toString())
        // 根据响应码获取不同输入流
        val inStream = if (code == 200)
            connection.inputStream
        else
            connection.errorStream
        // 输入流转换成字符串
        val result = inStream.bufferedReader().lineSequence().joinToString()
        Log.i(TAG, result)
        connection.disconnect() //断开连接
        return result
    }

    /**
     * POST请求 - 参数为表格格式
     */
    fun post(url: String, mapParam: Map<String, String>): String {
        val connection = buildURLConnection(url, false)
        connection.setContentType(1)
        connection.connect() // 建立连接
        connection.outputStream.let {
            it.write(converMap2String(mapParam).toByteArray())
            it.flush()
            it.close()
        }
        // 从服务端获取响应码，连接成功是200
        val code = connection.responseCode
        Log.i(TAG, code.toString())
        // 根据响应码获取不同输入流
        val inStream = if (code == 200)
            connection.inputStream
        else
            connection.errorStream
        // 输入流转换成字符串
        val result = inStream.bufferedReader().lineSequence().joinToString()
        Log.i(TAG, result)
        connection.disconnect() //断开连接
        return result
    }

    /**
     * 创建一个基于URLConnection类的对象用于
     * @param url 接口地址
     * @param isGet GET请求还是POST
     */
    private fun buildURLConnection(url: String, isGet: Boolean = true): HttpURLConnection {
        val url = URL(url)
        val connection = url.openConnection() as HttpURLConnection
        connection.let {
            it.requestMethod = if (isGet) "GET" else "POST"
            it.connectTimeout = CONNECT_TIME_OUT
            it.readTimeout = READ_TIME_OUT
            it.doInput = true
            it.doOutput = true
            it.useCaches = isGet // POST请求不能使用缓存
            it.instanceFollowRedirects = true // 是否允许HTTP的重定向
        }
        return connection
    }


    /**
     * 设置请求编码格式
     * @param 0用于JSON格式，1用于表单格式，3用于传输JAVA序列化对象
     */
    private fun URLConnection.setContentType(type: Int = 0) {
        val typeString = when (type) {
            1 -> "application/x-www-form-urlencoded"
            2 -> "application/x-java-serialized-object"
            else -> "application/json;charset=UTF-8"
        }
        this.setRequestProperty("Content-Type", typeString)
    }

    /**
     * 把map集合的参数拼接成字符串
     * @param isEncode 是否需要转换码
     */
    private fun converMap2String(mapParam: Map<String, String>, isEncode: Boolean = true): String {
        return mapParam.keys.joinToString(separator = "&") { key ->
            val value = if (isEncode) URLEncoder.encode(mapParam[key], "utf-8") else mapParam[key]
            "$key=$value"
        }
    }
}