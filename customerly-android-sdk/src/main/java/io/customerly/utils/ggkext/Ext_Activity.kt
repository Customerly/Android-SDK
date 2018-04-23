package eu.appsolutelyapps.quizpatente.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.Window
import android.view.WindowManager
import io.customerly.utils.ggkext.activity
import io.customerly.utils.ggkext.tryCrashlyticsLog
import kotlin.reflect.KClass

/*
 * Copyright (C) 2017 Customerly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Created by Gianni on 11/08/17.
 * Project: QuizPatente3.0
 */
fun Activity.start(activityClass: KClass<out Activity>, extras : Bundle? = null) {
    this.startActivity(Intent(this, activityClass.java)
            .apply {
                if (extras != null) {
                    this.putExtras(extras)
                }
            })
}

fun View.start(activityClass: KClass<out Activity>, extras : Bundle? = null) {
    this.activity?.start(activityClass, extras)
}

fun Activity.api21_finishAfterTransition() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.finishAfterTransition()
    } else {
        this.finish()
    }
}

fun Activity.startUrl(url : String) {
    try {
        this.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse(if (!url.startsWith("https://") && !url.startsWith("http://")) "http://" + url else url))
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
    } catch (ignored : Exception) { }
}

fun Activity.startFacebookScheme(fbSchemeUri : String, fallbackUrl : String) {
    try {
        this.packageManager.getPackageInfo("com.facebook.katana", 0)
        this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fbSchemeUri))
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
    } catch (noFacebookInstalled: Exception) {
        this.startUrl(fallbackUrl)
    }
}

fun Window.navigationBarColorInt(@ColorInt color : Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.navigationBarColor = color
    }
}

fun Window.navigationBarColorRes(context : Context, @ColorRes colorRes : Int) {
    this.navigationBarColorInt(ContextCompat.getColor(context, colorRes))
}

fun Activity.navigationBarColorInt(@ColorInt colorInt : Int) {
    this.window.navigationBarColorInt(colorInt)
}

fun Activity.navigationBarColorRes(@ColorRes colorRes : Int) {
    this.window.navigationBarColorRes(this, colorRes)
}

@ColorInt
fun Window.navigationBarColorInt() : Int? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.navigationBarColor
    } else {
        null
    }
}

fun Activity.navigationBarColorInt() : Int? {
    return this.window.navigationBarColorInt()
}

fun Window.statusBarColorRes(context : Context, @ColorRes colorRes : Int) {
    this.statusBarColorInt(ContextCompat.getColor(context, colorRes))
}

fun Window.statusBarColorInt(@ColorInt colorInt : Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.statusBarColor = colorInt
    }
}

fun Activity.statusBarColorRes(@ColorRes colorRes : Int) {
    this.window.statusBarColorRes(this, colorRes)
}

fun Activity.statusBarColorInt(@ColorInt colorInt : Int) {
    this.window.statusBarColorInt(colorInt)
}

@ColorInt
fun Window.statusBarColorInt() : Int? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.statusBarColor
    } else {
        null
    }
}

fun Activity.statusBarColorInt() : Int? {
    return this.window.statusBarColorInt()
}

fun Activity.openAppSystemSettings() {
    this.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + this.packageName)))
}

fun Activity.launchMaps(latitude : Double?, longitude : Double?, address : String? = null) {
    try {
        val geoLocation = if(latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
            if (!address.isNullOrEmpty()) {
                "geo:0,0?q=$latitude,$longitude($address)"
            } else {
                "geo:0,0?q=$latitude,$longitude"
            }
        } else if(!address.isNullOrEmpty()) {
            "geo:0,0?q=${Uri.encode(address)}"
        } else {
            null
        }
        if(geoLocation != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(geoLocation)).also { it.`package` = "com.google.android.apps.maps" }.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (it.resolveActivity(this.packageManager) != null) {
                    this.startActivity(it)
                }
            }
        }
    } catch (exception : Exception) {
        exception.tryCrashlyticsLog()
    }
}

fun Activity.launchDialer(phoneNumber : String?) {
    try {
        if(! phoneNumber?.trim().isNullOrEmpty()) {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (it.resolveActivity(this.packageManager) != null) {
                    this.startActivity(it)
                }
            }
        }
    } catch (exception : Exception) {
        exception.tryCrashlyticsLog()
    }
}

fun Activity.launchEmail(vararg email : String?, subject : String? = null, body : String? = null) {
    try {
        val notNullEmails = email.filterNotNull().toTypedArray()
        if(notNullEmails.isNotEmpty()) {
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).also {
                it.putExtra(Intent.EXTRA_EMAIL, notNullEmails)
                if(subject != null) {
                    it.putExtra(Intent.EXTRA_SUBJECT, subject)
                }
                if(body != null) {
                    it.putExtra(Intent.EXTRA_TEXT, body)
                }
            }.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (it.resolveActivity(this.packageManager) != null) {
                    this.startActivity(it)
                }
            }
        }
    } catch (exception : Exception) {
        exception.tryCrashlyticsLog()
    }
}

fun Activity.launchWhatsapp(phoneNumber: String?, text : String? = null) {
    try {
        if(phoneNumber != null) {
            Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://api.whatsapp.com/send?phone=${Uri.encode(if(text != null) "$phoneNumber&text=$text" else phoneNumber.toString())}")).let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (it.resolveActivity(this.packageManager) != null) {
                    this.startActivity(it)
                }
            }
        }
    } catch (exception : Exception) {
        exception.tryCrashlyticsLog()
    }
}

fun Activity.blockScreenshot() {
    this.window.blockScreenshot()
}

fun Window.blockScreenshot() {
    this.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
}