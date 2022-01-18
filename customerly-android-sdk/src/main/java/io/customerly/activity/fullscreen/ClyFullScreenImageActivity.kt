package io.customerly.activity.fullscreen

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
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import io.customerly.Customerly
import io.customerly.R
import io.customerly.activity.ClyAppCompatActivity
import io.customerly.entity.ERROR_CODE__GLIDE_ERROR
import io.customerly.entity.chat.ClyMessage
import io.customerly.entity.clySendError
import io.customerly.entity.iamAnonymous
import io.customerly.sxdependencies.annotations.SXColorInt
import io.customerly.sxdependencies.annotations.SXUiThread
import io.customerly.sxdependencies.SXAlertDialogBuilder
import io.customerly.sxdependencies.SXActivityCompat
import io.customerly.sxdependencies.SXContextCompat
import io.customerly.utils.alterColor
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.download.startFileDownload
import io.customerly.utils.getContrastBW
import io.customerly.utils.ggkext.start
import io.customerly.utils.ggkext.statusBarColorInt
import io.customerly.utils.htmlformatter.spannedFromHtml
import java.util.*

/**
 * Created by Gianni on 23/09/16.
 * Project: Customerly Android SDK
 */

private const val EXTRA_IMAGE_SOURCE = "EXTRA_IMAGE_SOURCE"
private const val PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE = 4321

internal fun Activity.startClyFullScreenImageActivity(imageUrl : String) {
    this.start(
            activityClass = ClyFullScreenImageActivity::class,
            extras = Bundle().apply { this.putString(EXTRA_IMAGE_SOURCE, imageUrl) })
}

internal class ClyFullScreenImageActivity : ClyAppCompatActivity() {

    private var sourceUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.intent.getStringExtra(EXTRA_IMAGE_SOURCE)?.let { targetUrl ->

            this.sourceUrl = targetUrl

            val imageView = ClyTouchImageView(this).also {
                it.setBackgroundColor(Color.WHITE)
                it.scaleType = ImageView.ScaleType.FIT_CENTER
                it.adjustViewBounds = true
            }
            try {
                ClyImageRequest(context = this, url = targetUrl)
                        .fitCenter()
                        .into(imageView = imageView)
                        .placeholder(placeholder = R.drawable.io_customerly__pic_placeholder_fullscreen)
                        .start()

                super.setContentView(imageView)

                this.supportActionBar?.let { sActionBar ->
                    @SXColorInt val widgetColor = Customerly.lastPing.widgetColor
                    if (widgetColor != 0) {
                        sActionBar.setBackgroundDrawable(ColorDrawable(widgetColor))

                        this.statusBarColorInt(widgetColor.alterColor(factor = 0.8f))

                        if (widgetColor.getContrastBW() == Color.BLACK) {
                            R.drawable.io_customerly__ic_arrow_back_black_24dp to "#000000"
                        } else {
                            R.drawable.io_customerly__ic_arrow_back_white_24dp to "#ffffff"
                        }.let { (homeIndicator, titleFontColor) ->
                            sActionBar.setHomeAsUpIndicator(homeIndicator)
                            "<font color='$titleFontColor'>${actionBar?.title}</font>"
                        }.let { title ->
                            sActionBar.title = spannedFromHtml(source = title)
                        }
                    }
                    sActionBar.setDisplayHomeAsUpEnabled(true)
                }
                return
            } catch (exception: Exception) {
                clySendError(errorCode = ERROR_CODE__GLIDE_ERROR, description = "Error during image loading in FullScreenImage_Activity", throwable = exception)
            }
        } ?: this.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.io_customerly__menu_download_image, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        if(Customerly.iamAnonymous()) {
            this.onLogoutUser()
        }
    }

    override fun onLogoutUser() {
        this.finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.io_customerly__menu__download -> {
                this.startAttachmentDownload()
                true
            }
            android.R.id.home -> {
                this.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startAttachmentDownload() {
        if (SXContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            this.sourceUrl?.let { url ->
                val lastSlashIndex = url.lastIndexOf('/')
                val lastBackslashIndex = url.lastIndexOf('\\')
                val validateIndex : (Int)->Boolean = { it != -1 && it < url.length - 1 }
                startFileDownload(
                        context = this,
                        filename = when {
                            validateIndex(lastSlashIndex)       -> url.substring(lastSlashIndex + 1)
                            validateIndex(lastBackslashIndex)   -> url.substring(lastBackslashIndex + 1)
                            else                                -> this.getString(R.string.io_customerly__image)
                        },
                        fullPath = url)
            }
        } else {
            if (SXActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                SXAlertDialogBuilder(this)
                        .setTitle(R.string.io_customerly__permission_request)
                        .setMessage(R.string.io_customerly__permission_request_explanation_write)
                        .setPositiveButton(android.R.string.ok) { _, _ -> SXActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE) }
                        .show()
            } else {
                SXActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE -> {
                if(permissions
                        .indexOfFirst { it == Manifest.permission.WRITE_EXTERNAL_STORAGE }
                        .let { it >= 0 && it < grantResults.size && grantResults[it] == PackageManager.PERMISSION_GRANTED }) {
                    this.startAttachmentDownload()
                } else {
                    Toast.makeText(this, R.string.io_customerly__permission_denied_write, Toast.LENGTH_LONG).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @SXUiThread
    override fun onNewSocketMessages(messages: ArrayList<ClyMessage>) {}

    //    private void saveImageToGallery() {
    //        if(this._ImageView != null) {
    //            this._ImageView.setDrawingCacheEnabled(true);
    //            MediaStore.Images.Media.insertImage(this.getContentResolver(), this._ImageView.getDrawingCache(), "Image", "Image");
    //        }
    //    }
}
