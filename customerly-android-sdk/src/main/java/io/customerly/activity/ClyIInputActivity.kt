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
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Build
import android.support.annotation.LayoutRes
import android.support.annotation.UiThread
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import java.util.ArrayList

import io.customerly.BuildConfig
import io.customerly.XXXXXcancellare.XXXCustomerly
import io.customerly.R
import io.customerly.entity.ClyAttachment
import io.customerly.entity.ERROR_CODE__ATTACHMENT_ERROR
import io.customerly.entity.clySendError
import io.customerly.utils.alterColor
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.getContrastBW
import io.customerly.utils.ggkext.activity
import io.customerly.utils.ggkext.checkConnection
import io.customerly.utils.ggkext.getFileSize
import io.customerly.utils.htmlformatter.spannedFromHtml

/**
 * Created by Gianni on 03/09/16.
 * Project: CustomerlySDK
 */
private val CONNECTIVITY_ACTION_INTENT_FILTER = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
private const val REQUESTCODE_FILE_SELECT = 5
private const val PERMISSION_REQUEST__READ_EXTERNAL_STORAGE = 1234

internal const val CLYINPUT_EXTRA_MUST_SHOW_BACK = "EXTRA_MUST_SHOW_BACK"

internal abstract class ClyIInputActivity : ClyAppCompatActivity() {

    internal var mustShowBack: Boolean = false
    protected var activityThemed = false

    internal var inputLayout: LinearLayout? = null
    internal var inputAttachments: LinearLayout? = null
    internal var inputInput: EditText? = null

    internal val attachments = ArrayList<ClyAttachment>(1)

    private val broadcastReceiver = object : BroadcastReceiver() {
        internal var attendingReconnection = false
        override fun onReceive(context: Context, intent: Intent) {
            if (context.checkConnection()) {
                if (this.attendingReconnection) {
                    this.attendingReconnection = false
                    onReconnection()
                }
            } else {
                this.attendingReconnection = true
            }
        }
    }

    private val attachButtonListener : (View?)->Unit = { btn ->
        if (this.attachments.size >= 10) {
            if(btn != null) {
                Snackbar.make(btn, R.string.io_customerly__attachments_max_count_error, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok) { v -> }.setActionTextColor(XXXCustomerly.get().__PING__LAST_widget_color).show()
            }
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN //Manifest.permission.READ_EXTERNAL_STORAGE has been added in api
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                try {
                    this.startActivityForResult(
                            Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),
                                    this.getString(R.string.io_customerly__choose_a_file_to_attach)), REQUESTCODE_FILE_SELECT)
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(this, this.getString(R.string.io_customerly__install_a_file_manager), Toast.LENGTH_SHORT).show()
                }

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder(this)
                            .setTitle(R.string.io_customerly__permission_request)
                            .setMessage(R.string.io_customerly__permission_request_explanation_read)
                            .setPositiveButton(android.R.string.ok) { _, _ -> ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST__READ_EXTERNAL_STORAGE) }
                            .show()
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST__READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    protected abstract fun onReconnection()

    override fun onResume() {
        super.onResume()
        this.registerReceiver(this.broadcastReceiver, CONNECTIVITY_ACTION_INTENT_FILTER)
    }

    override fun onPause() {
        super.onPause()
        this.unregisterReceiver(this.broadcastReceiver)
    }

    /**
     * Initialize the layout ( must contain layout_powered_by and layout_input_layout )
     * It colors the actionbar, the status bar and initializes the listeners of the input
     * @param pLayoutRes The layout resID
     * @return true if the SDK is configured or false otherwise anc finish is called
     */
    internal fun onCreateLayout(@LayoutRes pLayoutRes: Int): Boolean {
        if (XXXCustomerly.get()._isConfigured()) {
            super.setContentView(pLayoutRes)
            //View binding
            val actionBar = this.supportActionBar
            val powered_by = this.findViewById<View>(R.id.io_customerly__powered_by) as TextView
            this.inputInput = this.findViewById<View>(R.id.io_customerly__input_edit_text) as EditText
            val input_button_attach = this.findViewById<View>(R.id.io_customerly__input_button_attach)
            this.inputLayout = this.findViewById<View>(R.id.io_customerly__input_layout) as LinearLayout
            this.inputAttachments = this.findViewById<View>(R.id.io_customerly__input_attachments) as LinearLayout

            this.mustShowBack = this.intent.getBooleanExtra(CLYINPUT_EXTRA_MUST_SHOW_BACK, false)
            if (actionBar != null) {

                if (XXXCustomerly.get().__PING__LAST_widget_color != 0) {
                    actionBar.setBackgroundDrawable(ColorDrawable(XXXCustomerly.get().__PING__LAST_widget_color))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        this.window.statusBarColor = XXXCustomerly.get().__PING__LAST_widget_color.alterColor(0.8f)
                    }

                    when(XXXCustomerly.get().__PING__LAST_widget_color.getContrastBW()) {
                        Color.BLACK -> {
                            Triple(
                                    R.drawable.io_customerly__ic_arrow_back_black_24dp,
                                    R.drawable.io_customerly__ic_clear_black_24dp,
                                    "#000000")
                        }
                        else -> {
                            Triple(
                                    R.drawable.io_customerly__ic_arrow_back_white_24dp,
                                    R.drawable.io_customerly__ic_clear_white_24dp,
                                    "#ffffff")
                        }
                    }.let { (homeBack, homeClear, titleRGB) ->
                        if (this.intent != null && this.mustShowBack) {
                            homeBack
                        } else {
                            homeClear
                        } to String.format("<font color='$titleRGB'>%1\$s</font>", actionBar.title)
                    }.let { (home, title) ->
                        actionBar.setHomeAsUpIndicator(home)
                        actionBar.setTitle(spannedFromHtml(source = title))
                    }
                }
                actionBar.setDisplayHomeAsUpEnabled(true)
            }

            if (XXXCustomerly.get().__PING__LAST_powered_by) {
                val redBoldSpannable = SpannableString(BuildConfig.CUSTOMERLY_SDK_NAME)
                redBoldSpannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.io_customerly__blue_malibu)), 0, redBoldSpannable.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                redBoldSpannable.setSpan(StyleSpan(Typeface.BOLD), 0, redBoldSpannable.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                powered_by.text = SpannableStringBuilder(this.getString(R.string.io_customerly__powered_by_)).append(redBoldSpannable)
                powered_by.setOnClickListener { btn -> btn.activity?.let { it.startClyWebViewActivity(targetUrl = BuildConfig.CUSTOMERLY_WEB_SITE) } }
                powered_by.visibility = View.VISIBLE
            } else {
                powered_by.visibility = View.GONE
            }

            input_button_attach.setOnClickListener(this.attachButtonListener)

            this.findViewById<View>(R.id.io_customerly__input_button_send).setOnClickListener { btn ->
                if (btn.context.checkConnection()) {
                    (btn.activity as? ClyIInputActivity)?.let { inputActivity ->
                        val content = inputActivity.inputInput?.text?.toString()?.trim { it <= ' ' } ?: ""
                        val attachmentsArray = inputActivity.attachments.toTypedArray()
                        if (content.isNotEmpty() || attachmentsArray.isNotEmpty()) {
                            inputActivity.inputInput?.setText(null)
                            inputActivity.attachments.clear()
                            inputActivity.inputAttachments?.removeAllViews()
                            inputActivity.onSendMessage(content = content, attachments = attachmentsArray)
                        }
                    }
                } else {
                    Toast.makeText(btn.context.applicationContext, R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show()
                }
            }

            val themeUrl = XXXCustomerly.get().__PING__LAST_widget_background_url
            if (themeUrl != null) {
                val themeIV = this.findViewById<View>(R.id.io_customerly__background_theme) as ImageView
                ClyImageRequest(context = this, url = themeUrl)
                        .centerCrop()
                        .into(imageView = themeIV)
                        .start()
                themeIV.visibility = View.VISIBLE
                this.activityThemed = true
            }

            return true
        } else {
            this.finish()
            return false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST__READ_EXTERNAL_STORAGE -> {
                val length = Math.min(grantResults.size, permissions.size)
                if (length > 0) {
                    for (i in 0 until length) {
                        if (Manifest.permission.READ_EXTERNAL_STORAGE == permissions[i] && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            this.attachButtonListener.invoke(null)
                            return
                        }
                    }
                }
                Toast.makeText(this, R.string.io_customerly__permission_denied_read, Toast.LENGTH_LONG).show()
            }
        }
    }

    @UiThread
    protected abstract fun onSendMessage(content: String, attachments: Array<ClyAttachment>, ghostToVisitorEmail: String? = null)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            REQUESTCODE_FILE_SELECT -> if (resultCode == Activity.RESULT_OK) {
                val fileUri = data.data
                if (fileUri != null) {
                    try {
                        for (att in this.attachments) {
                            if (fileUri == att.uri) {
                                this.inputInput?.let {
                                    Snackbar.make(it, R.string.io_customerly__attachments_already_attached_error, Snackbar.LENGTH_INDEFINITE)
                                            .setAction(android.R.string.ok) { _ -> }.setActionTextColor(XXXCustomerly.get().__PING__LAST_widget_color).show()
                                    it.requestFocus()
                                }
                                return
                            }
                        }
                        if (fileUri.getFileSize(context = this) > 5000000) {
                            this.inputInput?.let {
                                Snackbar.make(it, R.string.io_customerly__attachments_max_size_error, Snackbar.LENGTH_INDEFINITE)
                                        .setAction(android.R.string.ok) { _ -> }.setActionTextColor(XXXCustomerly.get().__PING__LAST_widget_color).show()
                                it.requestFocus()
                            }
                            return
                        }

                        ClyAttachment(context = this, uri = fileUri).addAttachmentToInput(this)
                    } catch (exception: Exception) {
                        clySendError(errorCode = ERROR_CODE__ATTACHMENT_ERROR, description = "Error while attaching file: " + exception.message, throwable = exception)
                    }

                }
                this.inputInput?.requestFocus()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    internal fun restoreAttachments() {
        if (this.inputAttachments != null) {
            this.inputAttachments!!.removeAllViews()
        }
        if (this.attachments.size != 0) {
            this.attachments.clear()
        }
    }
}