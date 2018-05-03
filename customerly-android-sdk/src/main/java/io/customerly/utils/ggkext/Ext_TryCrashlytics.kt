package io.customerly.utils.ggkext

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

/**
 * Created by Gianni on 16/04/18.
 * Project: Customerly-KAndroid-SDK
 */

private const val CRASHLYTICS_PACKAGE = "com.crashlytics.android.Crashlytics"

internal fun Throwable.tryCrashlyticsLog() {
    try {
        Class.forName(CRASHLYTICS_PACKAGE)
                .getDeclaredMethod("logException", Throwable::class.java)
                .invoke(null, this)
    } catch (ignored: Exception) { }
}

//internal fun tryCrashlyticsSetString(key: String, value: String) {
//    try {
//        Class.forName(CRASHLYTICS_PACKAGE)
//                .getDeclaredMethod("setString", String::class.java, String::class.java)
//                .invoke(null, key, value)
//    } catch (ignored: Exception) { }
//}
