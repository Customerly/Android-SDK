package io.customerly.entity

import android.support.annotation.Px
import io.customerly.BuildConfig

/**
 * Created by Gianni on 09/04/18.
 * Project: Customerly-KAndroid-SDK
 */

fun urlImageAccount(accountId: Long, @Px sizePX: Int): String
        = "${BuildConfig.CUSTOMERLY_PICTURE_ENDPOINT_BASEURL}accounts/$accountId/$sizePX"

fun urlImageUser(userID: Long, @Px sizePX: Int): String
        = "${BuildConfig.CUSTOMERLY_PICTURE_ENDPOINT_BASEURL}users/$userID/$sizePX"