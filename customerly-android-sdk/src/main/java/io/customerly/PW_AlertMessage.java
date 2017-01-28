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
import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Gianni on 28/01/17.
 * Project: CustomerlyAndroidSDK
 */
class PW_AlertMessage extends PopupWindow {

    @SuppressLint("InflateParams")
    private PW_AlertMessage(@NonNull Activity activity) {
        super(activity.getLayoutInflater().inflate(R.layout.io_customerly__alert_message, null), WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
    }

    static void show(@NonNull Activity activity, @NonNull IE_Message message) {
        PW_AlertMessage alert = new PW_AlertMessage(activity);

        int _50dp = IU_Utils.px(50);
        Customerly._Instance._RemoteImageHandler.request(new IU_RemoteImageHandler.Request()
                .fitCenter()
                .transformCircle()
                .load(message.getImageUrl(_50dp))
                .into((ImageView)alert.getContentView().findViewById(R.id.io_customerly__icon))
                .override(_50dp, _50dp)
                .placeholder(R.drawable.io_customerly__ic_default_admin));

        ((TextView)alert.getContentView().findViewById(R.id.io_customerly__name))
                .setText(message.if_account__name != null ? message.if_account__name : activity.getString(R.string.io_customerly__support));
        ((TextView)alert.getContentView().findViewById(R.id.io_customerly__content))
                .setText(message.content);

        alert.showAtLocation(activity.getWindow().getDecorView().getRootView(), Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);



        Toast.makeText(activity.getApplicationContext(), "MESSAGGIO: " + message.content, Toast.LENGTH_SHORT).show();//TODO MESSAGE ALERT
    }
}
