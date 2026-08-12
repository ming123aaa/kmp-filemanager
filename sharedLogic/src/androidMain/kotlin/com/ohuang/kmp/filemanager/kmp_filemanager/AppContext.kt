package com.ohuang.kmp.filemanager.kmp_filemanager

import android.content.Context

object AppContext {
    lateinit var instance: Context
        private set

    fun init(context: Context) {
        instance = context.applicationContext
    }
}