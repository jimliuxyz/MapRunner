package com.jimliuxyz.maprunner

import android.app.Application

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance_ = this
    }

    companion object {
        private lateinit var instance_: MyApplication

        val instance: Application
            get() = instance_!!
    }
}
