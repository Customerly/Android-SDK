package io.customerly.activity

import android.support.v7.app.AppCompatActivity
import io.customerly.entity.ClyMessage
import java.util.*

/**
 * Created by Gianni on 16/04/18.
 * Project: Customerly-KAndroid-SDK
 */
internal abstract class ClyAppCompatActivity : AppCompatActivity() {
    internal abstract fun onNewSocketMessages(messages: ArrayList<ClyMessage>)
    internal abstract fun onLogoutUser()
}