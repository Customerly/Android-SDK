package io.customerly.activity

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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import io.customerly.Customerly
import io.customerly.R
import io.customerly.activity.CLYINPUT_EXTRA_MUST_SHOW_BACK
import io.customerly.activity.ClyIInputActivity
import io.customerly.alert.showClyAlertMessage
import io.customerly.api.*
import io.customerly.entity.ClyAdminFull
import io.customerly.entity.ClyRealtimePayload
import io.customerly.entity.chat.*
import io.customerly.entity.iamLead
import io.customerly.entity.ping.ClyNextOfficeHours
import io.customerly.sxdependencies.*
import io.customerly.sxdependencies.annotations.SXColorInt
import io.customerly.sxdependencies.annotations.SXStringRes
import io.customerly.sxdependencies.annotations.SXUiThread
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.download.startFileDownload
import io.customerly.utils.getContrastBW
import io.customerly.utils.ggkext.*
import io.customerly.utils.network.ClySntpClient
import io.customerly.utils.ui.RvProgressiveScrollListener
import kotlinx.android.synthetic.main.io_customerly__activity_chat.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.min

/**
 * Created by Gianni on 03/09/16.
 * Project: Customerly Android SDK
 */

private const val EXTRA_REALTIME_PAYLOAD = "EXTRA_REALTIME_PAYLOAD"

internal fun Activity.startClyRealtimeActivity(realtimePayload: ClyRealtimePayload) {
    this.startActivity(
        Intent(this, ClyRealtimeActivity::class.java)
            .putExtra(EXTRA_REALTIME_PAYLOAD, realtimePayload)
    )
}

internal class ClyRealtimeActivity : ClyAppCompatActivity() {

    lateinit var realtimePayload: ClyRealtimePayload
    var accepted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val realtimePayload: ClyRealtimePayload? = this.intent.getParcelableExtra(EXTRA_REALTIME_PAYLOAD)
        if(realtimePayload != null) {
            this.realtimePayload = realtimePayload
        } else {
            this.finish()
        }


        //TODO load realtime
    }

    override fun onDestroy() {
        if(!this.accepted) {
            Customerly.clySocket.sendRealtimeReject(this.realtimePayload)
        }
        super.onDestroy()
    }

    override fun onLogoutUser() {
        this.finish()
    }

    @SXUiThread
    override fun onNewSocketMessages(messages: ArrayList<ClyMessage>) {
    }
}
