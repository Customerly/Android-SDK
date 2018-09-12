package io.customerly.utils

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

import android.support.annotation.ColorInt
import android.support.annotation.IntDef
import io.customerly.Customerly

/**
 * Created by Gianni on 11/04/18.
 * Project: Customerly-KAndroid-SDK
 */

const val USER_TYPE__ANONYMOUS = 1 //hex 0x01 bin: 0001
const val USER_TYPE__LEAD = 2 //hex 0x02 dec: 0010
//const val USER_TYPE__MAILLESS_LEAD = 8 //hex 0x08 bin: 1000 //but a maillessLead is 10dec, 0x0a, 1010b because needs to match LEAD MASK
const val USER_TYPE__USER = 4 //hex 0x04 bin: 0100

@IntDef(USER_TYPE__ANONYMOUS, USER_TYPE__LEAD, USER_TYPE__USER)
@Retention(value = AnnotationRetention.SOURCE)
internal annotation class UserType

const val WRITER_TYPE__BOT = -1//Local value
const val WRITER_TYPE__ACCOUNT = 0
const val WRITER_TYPE__USER = 1

@IntDef(WRITER_TYPE__BOT, WRITER_TYPE__ACCOUNT, WRITER_TYPE__USER)
@Retention(value = AnnotationRetention.SOURCE)
internal annotation class WriterType

@ColorInt internal const val COLORINT_BLUE_MALIBU = -0x9a5619//Blue Malibu

internal const val JSON_COMPANY_KEY_ID = "company_id"
internal const val JSON_COMPANY_KEY_NAME = "name"

internal const val CUSTOMERLY_WEB_SITE = "https://www.customerly.io/"
internal const val CUSTOMERLY_SDK_NAME = "Customerly"
internal const val CUSTOMERLY_PICTURE_ENDPOINT_BASEURL = "http://pictures.customerly.io/"
internal const val CUSTOMERLY_API_ENDPOINT_BASEURL = "https://tracking.customerly.io/"
internal const val CUSTOMERLY_API_VERSION = "v1"
internal const val CUSTOMERLY_SOCKET_VERSION = "1"
internal const val CUSTOMERLY_DEV_MODE = false

internal val CUSTOMERLY_SURVEY_SITE: String get() = "http://www.customerly.io/go/survey?utm_source=sdk&utm_medium=widget&utm_campaign=${Customerly.appId}"