package io.customerly.XXXXXcancellare;

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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.RestrictTo;

@RestrictTo(android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP)
public class XXXIAct_OpenDownloadedFileActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent input = this.getIntent();
        if(input != null) {
            Uri targetUri = input.getData();
            if(targetUri != null) {
                this.startActivity(new Intent(Intent.ACTION_VIEW, targetUri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            }
        }
        this.finish();
    }
}
