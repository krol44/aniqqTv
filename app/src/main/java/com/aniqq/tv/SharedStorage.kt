package com.aniqq.tv

import android.content.Context
import android.content.SharedPreferences

class SharedStorage(contexts: Context?) {
    private val STORAGE_NAME = "TOKEN_STORAGE"
    private var settings: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null
    private var context: Context? = null

    var token: String? = null

    init {
        context = contexts

        settings = context!!.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)
        editor = settings!!.edit()
    }

    fun addProperty(name: String?, value: String?) {
        editor!!.putString(name, value)
        editor!!.apply()
    }

    fun getProperty(name: String): String? {
        return settings!!.getString(name, null)
    }
}