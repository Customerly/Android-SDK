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
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import io.customerly.Customerly
import io.customerly.R
import io.customerly.entity.ERROR_CODE__ATTACHMENT_ERROR
import io.customerly.entity.chat.ClyAttachment
import io.customerly.entity.clySendError
import io.customerly.sxdependencies.*
import io.customerly.sxdependencies.annotations.SXLayoutRes
import io.customerly.sxdependencies.annotations.SXUiThread
import io.customerly.utils.CUSTOMERLY_WEB_SITE
import io.customerly.utils.alterColor
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.getContrastBW
import io.customerly.utils.ggkext.*
import io.customerly.utils.htmlformatter.spannedFromHtml
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.min

/**
 * Created by Gianni on 03/09/16.
 * Project: CustomerlySDK
 */
private val CONNECTIVITY_ACTION_INTENT_FILTER = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
private const val PERMISSION_REQUEST__READ_EXTERNAL_STORAGE = 1234

internal const val CLYINPUT_EXTRA_MUST_SHOW_BACK = "EXTRA_MUST_SHOW_BACK"

internal abstract class ClyIInputActivity : ClyAppCompatActivity() {

    private lateinit var attachmentFileChooserLauncher: ActivityResultLauncher<Intent>

    internal var mustShowBack: Boolean = false
    private var activityThemed = false

    internal var inputLayout: LinearLayout? = null
    internal var inputAttachments: LinearLayout? = null
    internal var inputInput: EditText? = null

    internal val attachments = ArrayList<ClyAttachment>(1)

    private val broadcastReceiver = object : BroadcastReceiver() {
        private var waitingReconnection = false
        private val weakActivity: WeakReference<ClyIInputActivity> = this@ClyIInputActivity.weak()
        override fun onReceive(context: Context, intent: Intent) {
            if (context.checkConnection()) {
                if (this.waitingReconnection) {
                    this.waitingReconnection = false
                    this.weakActivity.get()?.onReconnection()
                }
            } else {
                this.waitingReconnection = true
            }
        }
    }

    private val attachButtonListener : (View?)->Unit = { btn ->
        if (this.attachments.size >= 10) {
            if(btn != null) {
                SXSnackbar.make(btn, R.string.io_customerly__attachments_max_count_error, SXSnackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok) {}.setActionTextColor(Customerly.lastPing.widgetColor).show()
            }
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN //Manifest.permission.READ_EXTERNAL_STORAGE has been added in api
                    || SXContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                try {
                    this.attachmentFileChooserLauncher.launch(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),
                            this.getString(R.string.io_customerly__choose_a_file_to_attach))
                    )
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(this, this.getString(R.string.io_customerly__install_a_file_manager), Toast.LENGTH_SHORT).show()
                }

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (SXActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder(this)
                            .setTitle(R.string.io_customerly__permission_request)
                            .setMessage(R.string.io_customerly__permission_request_explanation_read)
                            .setPositiveButton(android.R.string.ok) { _, _ -> SXActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST__READ_EXTERNAL_STORAGE) }
                            .show()
                } else {
                    SXActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST__READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.attachmentFileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onRequestFileSelected(result.resultCode, result.data)
            }
        }
        super.onCreate(savedInstanceState)
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
    internal fun onCreateLayout(@SXLayoutRes pLayoutRes: Int): Boolean {
        super.setContentView(pLayoutRes)

        this.findViewById<SXToolbar>(R.id.io_customerly__toolbar)?.also { this.setSupportActionBar(it) }
        val actionBar = this.supportActionBar
        val poweredBy = this.findViewById<TextView>(R.id.io_customerly__powered_by)
        this.inputInput = this.findViewById(R.id.io_customerly__input_edit_text)
        this.inputLayout = this.findViewById(R.id.io_customerly__input_layout)
        this.inputAttachments = this.findViewById(R.id.io_customerly__input_attachments)
        val tintColor = Customerly.lastPing.widgetColor.let { widgetColor ->
            this.inputLayout?.setBackgroundColor(widgetColor)
            widgetColor.getContrastBW()
        }
        this.inputInput?.setTextColor(tintColor)
        this.inputInput?.setHintTextColor(tintColor)

        this.mustShowBack = this.intent.getBooleanExtra(CLYINPUT_EXTRA_MUST_SHOW_BACK, false)
        if (actionBar != null) {

            if (Customerly.lastPing.widgetColor != 0) {
                actionBar.setBackgroundDrawable(ColorDrawable(Customerly.lastPing.widgetColor))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    this.window.statusBarColor = Customerly.lastPing.widgetColor.alterColor(0.8f)
                }

                when(Customerly.lastPing.widgetColor.getContrastBW()) {
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
                    } to "<font color='$titleRGB'>${actionBar.title}</font>"
                }.let { (home, title) ->
                    actionBar.setHomeAsUpIndicator(home)
                    actionBar.title = spannedFromHtml(source = title)
                }
            }
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        if (Customerly.lastPing.brandedWidget) {
            poweredBy.setOnClickListener { it.activity?.startClyWebViewActivity(targetUrl = CUSTOMERLY_WEB_SITE) }
            poweredBy.visibility = View.VISIBLE
        } else {
            poweredBy.visibility = View.GONE
        }

        this.findViewById<ImageView>(R.id.io_customerly__input_button_attach).also {
            SXImageViewCompat.setImageTintMode(it, PorterDuff.Mode.SRC_ATOP)
            SXImageViewCompat.setImageTintList(it, ColorStateList.valueOf(tintColor))
            if (Customerly.isAttachmentsAvailable) {
                it.setOnClickListener(this.attachButtonListener)
            } else {
                it.visibility = View.GONE
            }
        }

        this.findViewById<ImageView>(R.id.io_customerly__input_button_send).apply {
            SXImageViewCompat.setImageTintMode(this, PorterDuff.Mode.SRC_ATOP)
            SXImageViewCompat.setImageTintList(this, ColorStateList.valueOf(tintColor))
            this.setOnClickListener { btn ->
                if (btn.context.checkConnection()) {
                    (btn.activity as? ClyIInputActivity)?.let { inputActivity ->
                        val content = inputActivity.inputInput?.text?.toString()?.trim { it <= ' ' }
                                ?: ""
                        val attachmentsArray = inputActivity.attachments.toTypedArray()
                        if (content.isNotEmpty() || attachmentsArray.isNotEmpty()) {
                            inputActivity.inputInput?.text = null
                            inputActivity.attachments.clear()
                            inputActivity.inputAttachments?.removeAllViews()
                            btn.dismissKeyboard()
                            inputActivity.onSendMessage(content = content, attachments = attachmentsArray)
                        }
                    }
                } else {
                    Toast.makeText(btn.context.applicationContext, R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val themeUrl = Customerly.lastPing.widgetBackgroundUrl
        if (themeUrl != null) {
            val themeIV = this.findViewById<View>(R.id.io_customerly__background_theme) as ImageView
            ClyImageRequest(context = this, url = themeUrl)
                    .centerCrop()
                    .into(imageView = themeIV)
                    .start()
            this.activityThemed = true
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST__READ_EXTERNAL_STORAGE -> {
                val length = min(grantResults.size, permissions.size)
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
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    @SXUiThread
    protected abstract fun onSendMessage(content: String, attachments: Array<ClyAttachment>)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onRequestFileSelected(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val fileUri = data?.data
            if (fileUri != null) {
                try {
                    for (att in this.attachments) {
                        if (fileUri == att.uri) {
                            this.inputInput?.let {
                                SXSnackbar.make(it, R.string.io_customerly__attachments_already_attached_error, SXSnackbar.LENGTH_INDEFINITE)
                                        .setAction(android.R.string.ok) {}.setActionTextColor(Customerly.lastPing.widgetColor).show()
                                it.requestFocus()
                            }
                            return
                        }
                    }
                    if (fileUri.getFileSize(context = this) > 5000000) {
                        this.inputInput?.let {
                            SXSnackbar.make(it, R.string.io_customerly__attachments_max_size_error, SXSnackbar.LENGTH_INDEFINITE)
                                    .setAction(android.R.string.ok) {}.setActionTextColor(Customerly.lastPing.widgetColor).show()
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

    internal fun restoreAttachments() {
        if (this.inputAttachments != null) {
            this.inputAttachments!!.removeAllViews()
        }
        if (this.attachments.size != 0) {
            this.attachments.clear()
        }
    }
}
