package io.customerly.demoapp

import android.app.Application
import android.graphics.Color
import io.customerly.Customerly

/**
 * Created by Gianni on 09/07/18.
 * Project: Customerly-KAndroid-SDK
 */
class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Customerly.configure(application = this, customerlyAppId = "00e85f4b", widgetColorInt = Color.RED)
    }
}