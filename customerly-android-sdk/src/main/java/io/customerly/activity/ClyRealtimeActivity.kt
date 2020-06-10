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
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import io.customerly.Customerly
import io.customerly.R
import io.customerly.entity.ClyRealtimePayload
import io.customerly.entity.chat.*
import io.customerly.sxdependencies.*
import io.customerly.sxdependencies.annotations.SXUiThread
import io.customerly.utils.ggkext.*
import kotlinx.android.synthetic.main.io_customerly__activity_realtime.*
import java.util.*

/**
 * Created by Gianni on 03/09/16.
 * Project: Customerly Android SDK
 */

private const val EXTRA_REALTIME_PAYLOAD = "EXTRA_REALTIME_PAYLOAD"

private const val PERMISSION_REQUEST__AUDIO = 12345

internal fun Activity.startClyRealtimeActivity(realtimePayload: ClyRealtimePayload) {
    this.startActivity(
        Intent(this, ClyRealtimeActivity::class.java)
            .putExtra(EXTRA_REALTIME_PAYLOAD, realtimePayload)
    )
}

internal class ClyRealtimeActivity : ClyAppCompatActivity() {

    lateinit var realtimePayload: ClyRealtimePayload
    var accepted: Boolean = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val realtimePayload: ClyRealtimePayload? = this.intent.getParcelableExtra(EXTRA_REALTIME_PAYLOAD)
        if(realtimePayload != null) {
            this.realtimePayload = realtimePayload
        } else {
            this.finish()
        }
        super.setContentView(R.layout.io_customerly__activity_realtime)
        this.io_customerly__realtime_label.text = this.realtimePayload.account.name + " ti sta chiamando"
        this.io_customerly__realtime_reject.setOnClickListener {
            (it.activity as? ClyRealtimeActivity)?.let { that ->
                Customerly.clySocket.sendRealtimeReject(that.realtimePayload)
                that.finish()
            }
        }
        this.io_customerly__realtime_accept.setOnClickListener {
            (it.activity as? ClyRealtimeActivity)?.let { that ->
                that.ensurePermissions {
                    that.accepted = true
                    Customerly.clySocket.sendRealtimeAccept(that.realtimePayload)
                    that.io_customerly__realtime_prompt_layout.visibility = View.GONE
                    that.io_customerly__webview.let { wv ->
                        wv.settings.javaScriptEnabled = true
                        wv.settings.domStorageEnabled = true
                        wv.webChromeClient = object:  WebChromeClient() {
                            // Need to accept permissions to use the camera and audio
                            override fun onPermissionRequest(request: PermissionRequest) {
                                if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP) {
                                    wv.post {
                                        request.grant(request.resources)
                                    }
                                }
                            }
                        }
                        wv.webViewClient = object : WebViewClient() {
                            @Suppress("OverridingDeprecatedMember")
                            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                                Log.e("RT-shouldOverride_api20", url)//TODO
                                return false
                            }

                            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                Log.e("RT-shouldOverride_api21", request.url.toString())//TODO
                                return false
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                Log.e("RT-onPageFinished", url)//TODO
                            }
                        }
                        wv.visibility = View.VISIBLE
                        wv.loadUrl(that.realtimePayload.url)
                    }
                }
            }
        }
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

    private fun ensurePermissions(then: ()->Unit)
    {
        if (SXContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && SXContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            then()
        } else if (SXActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)
                || SXActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.io_customerly__permission_request)
                    .setMessage(R.string.io_customerly__permission_request_explanation_realtime)
                    .setPositiveButton(android.R.string.ok) { _, _ -> SXActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), PERMISSION_REQUEST__AUDIO) }
                    .show()
        } else {
            if (SXContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                SXActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), PERMISSION_REQUEST__AUDIO)
            } else {
                SXActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST__AUDIO)
                then()
            }
        }
    }
}
