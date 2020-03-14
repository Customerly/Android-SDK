package io.customerly.demoapp

import android.app.Application
import android.graphics.Color
import io.customerly.Customerly

/**
 * Created by Gianni on 09/07/18.
 * Project: Customerly-KAndroid-SDK
 */
@Suppress("unused")
class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Customerly.configure(application = this, customerlyAppId = CUSTOMERLY_APP_ID, widgetColorInt = Color.RED)
    }

    companion object {
        const val CUSTOMERLY_APP_ID = ""//TODO
    }
}