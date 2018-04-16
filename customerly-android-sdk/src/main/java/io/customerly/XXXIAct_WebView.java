package io.customerly;

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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/**
 * Created by Gianni on 03/09/16.
 * Project: Customerly Android SDK
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class XXXIAct_WebView extends AppCompatActivity {

    private static final String EXTRA_TARGET_URL= "EXTRA_TARGET_URL";
    private WebView _WebView;
    private ProgressBar _ProgressView;
    private String _CurrentUrl = null;

    static void start(@NonNull Activity activity, @NonNull String targetUrl) {
        activity.startActivity(new Intent(activity, XXXIAct_WebView.class).putExtra(XXXIAct_WebView.EXTRA_TARGET_URL, targetUrl));
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(this.getIntent() != null) {
            this._CurrentUrl = this.getIntent().getStringExtra(EXTRA_TARGET_URL);
            if(this._CurrentUrl != null) {
                super.setContentView(R.layout.io_customerly__activity_webview);
                final ActionBar actionBar = this.getSupportActionBar();
                if (actionBar != null) {
                    String title;
                    if(Customerly.get().__PING__LAST_widget_color != 0) {
                        actionBar.setBackgroundDrawable(new ColorDrawable(Customerly.get().__PING__LAST_widget_color));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            this.getWindow().setStatusBarColor(XXXIU_Utils.alterColor(Customerly.get().__PING__LAST_widget_color, 0.8f));
                        }
                        if (XXXIU_Utils.getContrastColor(Customerly.get().__PING__LAST_widget_color) == Color.BLACK) {
                            actionBar.setHomeAsUpIndicator(this.getIntent() != null ? R.drawable.io_customerly__ic_arrow_back_black_24dp : R.drawable.io_customerly__ic_clear_black_24dp);
                            title = String.format("<font color='#000000'>%1$s</font>", actionBar.getTitle());
                        } else {
                            actionBar.setHomeAsUpIndicator(this.getIntent() != null ? R.drawable.io_customerly__ic_arrow_back_white_24dp : R.drawable.io_customerly__ic_clear_white_24dp);
                            title = String.format("<font color='#ffffff'>%1$s</font>", actionBar.getTitle());
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            actionBar.setTitle(Html.fromHtml(title, 0));
                        } else {
                            //noinspection deprecation
                            actionBar.setTitle(Html.fromHtml(title));
                        }
                    }
                    actionBar.setDisplayHomeAsUpEnabled(true);
                }
                this._ProgressView = (ProgressBar)this.findViewById(R.id.io_customerly__progress_view);
                this._ProgressView.getIndeterminateDrawable().setColorFilter(Customerly.get().__PING__LAST_widget_color, android.graphics.PorterDuff.Mode.MULTIPLY);
                this._WebView = (WebView) this.findViewById(R.id.io_customerly__webview);
                this._WebView.getSettings().setJavaScriptEnabled(true);
                this._WebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        _CurrentUrl = url;
                        return false;
                    }
                    @Override
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        _CurrentUrl = request.getUrl().toString();
                        return false;
                    }
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        if(_ProgressView.getVisibility() == View.VISIBLE) {
                            _ProgressView.setVisibility(View.GONE);
                        }
                    }
                });
                this._WebView.loadUrl(this._CurrentUrl);
                return;
            }
        }
        this.finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && this._WebView.canGoBack()) {
            this._WebView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.io_customerly__menu_webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                if(item.getItemId() == R.id.io_customerly__menu__open_in_browser) {
                    if(this._CurrentUrl != null) {
                        XXXIU_Utils.intentUrl(this, this._CurrentUrl);
                    }
                    return true;
                } else {
                    return super.onOptionsItemSelected(item);
                }
        }
    }
}
