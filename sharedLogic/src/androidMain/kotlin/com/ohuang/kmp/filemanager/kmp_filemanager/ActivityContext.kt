package com.ohuang.kmp.filemanager.kmp_filemanager

import android.app.Activity
import android.content.Context


object ActivityContext {

    private var mActivity: Activity? = null

    fun init(activity: Activity){
        mActivity=activity
    }

    fun destroy(){
        mActivity=null
    }

    fun get()= mActivity

    fun getApplication() = AppContext.instance





}