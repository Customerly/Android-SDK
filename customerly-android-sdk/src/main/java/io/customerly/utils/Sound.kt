package io.customerly.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import io.customerly.R

/**
 * Created by Gianni on 18/05/18.
 * Project: DemoCustomerly
 */
internal fun Context.playNotifSound() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        MediaPlayer.create(this,
                R.raw.io_customerly__notif_2,
                AudioAttributes
                        .Builder()
                        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                        .build(),
                (this.getSystemService(Context.AUDIO_SERVICE) as AudioManager).generateAudioSessionId())
    } else {
        MediaPlayer.create(this, R.raw.io_customerly__notif_2).apply {
            @Suppress("DEPRECATION")
            this.setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
        }
    }.apply {
        this.setOnCompletionListener { mp ->
            mp.reset()
            mp.release()
        }
        this.start()
    }
}