package com.example.edulearn

import android.app.Application

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}
