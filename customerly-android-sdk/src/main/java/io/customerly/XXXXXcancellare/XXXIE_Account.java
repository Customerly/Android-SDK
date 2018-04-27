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

import android.support.annotation.NonNull;

import java.util.Locale;

import io.customerly.BuildConfig;

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */
class XXXIE_Account {
    @NonNull static String getAccountImageUrl(long pAccountID, int pSizePX) {
        return String.format(Locale.UK, "%saccounts/%d/%d", BuildConfig.CUSTOMERLY_PICTURE_ENDPOINT_BASEURL, pAccountID, pSizePX);
    }
    @NonNull static String getUserImageUrl(long pAccountID, int pSizePX) {
        return String.format(Locale.UK, "%susers/%d/%d", BuildConfig.CUSTOMERLY_PICTURE_ENDPOINT_BASEURL, pAccountID, pSizePX);
    }
}
