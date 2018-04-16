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

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.appsolutelyapps.quizpatente.extensions.startUrl
import eu.appsolutelyapps.quizpatente.extensions.statusBarColorInt

import io.customerly.Customerly
import io.customerly.R
import io.customerly.utils.alterColor
import io.customerly.utils.getContrastBW
import io.customerly.utils.ggkext.activity
import io.customerly.utils.ggkext.weak
import io.customerly.utils.htmlformatter.spannedFromHtml
import kotlinx.android.synthetic.main.io_customerly__activity_webview.*

/**
 * Created by Gianni on 03/09/16.
 * Project: Customerly Android SDK
 */

private const val EXTRA_TARGET_URL = "EXTRA_TARGET_URL"
private const val EXTRA_HOME_INDICATOR_CLEAR = "EXTRA_HOME_INDICATOR_CLEAR"

internal fun Activity.startClyWebViewActivity(targetUrl: String, showClearInsteadOfBack: Boolean = false) {
    this.startActivity(Intent(this, ClyWebViewActivity::class.java)
            .putExtra(EXTRA_TARGET_URL, targetUrl)
            .putExtra(EXTRA_HOME_INDICATOR_CLEAR, showClearInsteadOfBack))
}

internal class ClyWebViewActivity : AppCompatActivity() {

    private var currentUrl: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.intent.getStringExtra(EXTRA_TARGET_URL)?.let { targetUrl ->
            this.currentUrl = targetUrl
            super.setContentView(R.layout.io_customerly__activity_webview)
            val actionBar = this.supportActionBar
            @ColorInt val widgetColor = Customerly.get().__PING__LAST_widget_color
            if (actionBar != null) {
                if (widgetColor != 0) {

                    actionBar.setBackgroundDrawable(ColorDrawable(widgetColor))

                    this.statusBarColorInt(widgetColor.alterColor(factor = 0.8f))

                    if (widgetColor.getContrastBW() == Color.BLACK) {
                        Triple(R.drawable.io_customerly__ic_arrow_back_black_24dp, R.drawable.io_customerly__ic_clear_black_24dp, "#000000")
                    } else {
                        Triple(R.drawable.io_customerly__ic_arrow_back_white_24dp, R.drawable.io_customerly__ic_clear_white_24dp, "#ffffff")
                    }.let { (backHomeIndicator, clearHomeIndicator, titleFontColor) ->
                        if (this.intent.getBooleanExtra(EXTRA_HOME_INDICATOR_CLEAR, false)) {
                            clearHomeIndicator
                        } else {
                            backHomeIndicator
                        } to titleFontColor
                    }.let { (homeIndicator, titleFontColor) ->
                        actionBar.setHomeAsUpIndicator(homeIndicator)
                        "<font color='$titleFontColor'>${actionBar.title}</font>"
                    }.let { title ->
                        actionBar.setTitle(spannedFromHtml(source = title))
                    }
                }
                actionBar.setDisplayHomeAsUpEnabled(true)
            }

            val weakProgressView = this.io_customerly__progress_view.let {
                it.indeterminateDrawable.setColorFilter(widgetColor, android.graphics.PorterDuff.Mode.MULTIPLY)
                it.weak()
            }

            this.io_customerly__webview.let { wv ->
                wv.settings.javaScriptEnabled = true
                wv.webViewClient = object : WebViewClient() {
                    @Suppress("OverridingDeprecatedMember")
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        (view.activity as? ClyWebViewActivity)?.currentUrl = url
                        return false
                    }
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        (view.activity as? ClyWebViewActivity)?.currentUrl = request.url.toString()
                        return false
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        weakProgressView.get()?.takeIf { it.visibility == View.VISIBLE }?.visibility = View.GONE
                    }
                }
                wv.loadUrl(targetUrl)
            }
        } ?: this.finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK && this.io_customerly__webview.canGoBack()) {
            this.io_customerly__webview.goBack()
            return true
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.io_customerly__menu_webview, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                true
            }
            R.id.io_customerly__menu__open_in_browser -> {
                this.currentUrl?.let {
                    this.startUrl(url = it)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
