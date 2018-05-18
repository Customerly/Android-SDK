package io.customerly.utils

import android.content.Context
import android.media.MediaPlayer
import io.customerly.R

/**
 * Created by Gianni on 18/05/18.
 * Project: DemoCustomerly
 */
internal fun Context.playNotifSound() {
    MediaPlayer.create(this, R.raw.notif_2).apply {
        this.setOnCompletionListener { mp ->
            mp.reset()
            mp.release()
        }
    }.start()
}