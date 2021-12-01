package com.example.googleonetap

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GoogleOneApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}