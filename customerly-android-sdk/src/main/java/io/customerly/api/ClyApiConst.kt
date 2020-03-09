package io.customerly.api

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

import io.customerly.entity.ERROR_CODE__GENERIC
import io.customerly.sxdependencies.annotations.SXIntDef
import io.customerly.sxdependencies.annotations.SXStringDef
import io.customerly.utils.CUSTOMERLY_API_ENDPOINT_BASEURL
import io.customerly.utils.CUSTOMERLY_API_VERSION

/**
 * Created by Gianni on 19/04/18.
 * Project: Customerly-KAndroid-SDK
 */
@SXStringDef(ENDPOINT_PING, ENDPOINT_CONVERSATION_RETRIEVE, ENDPOINT_MESSAGE_SEEN,
        ENDPOINT_MESSAGE_NEWS, ENDPOINT_MESSAGE_RETRIEVE, ENDPOINT_MESSAGE_SEND,
        ENDPOINT_CONVERSATION_DISCARD, ENDPOINT_EVENT_TRACKING, ENDPOINT_REPORT_CRASH,
        ENDPOINT_SURVEY_SUBMIT, ENDPOINT_SURVEY_SEEN, ENDPOINT_SURVEY_BACK,
        ENDPOINT_SURVEY_REJECT, ENDPOINT_ACCOUNT_DETAILS, ENDPOINT_FORM_ATTRIBUTE)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
internal annotation class ClyEndpoint

@SXIntDef(RESPONSE_STATE__PREPARING, RESPONSE_STATE__PENDING, RESPONSE_STATE__OK,
        RESPONSE_STATE__ERROR_NO_CONNECTION, RESPONSE_STATE__ERROR_BAD_REQUEST,
        RESPONSE_STATE__ERROR_NETWORK, RESPONSE_STATE__ERROR_BAD_RESPONSE,
        RESPONSE_STATE__SERVERERROR_USER_NOT_AUTHENTICATED, RESPONSE_STATE__NO_TOKEN_AVAILABLE,
        RESPONSE_STATE__SERVERERROR_APP_INSOLVENT, ERROR_CODE__GENERIC,
        RESPONSE_STATE__NO_APPID_AVAILABLE)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
internal annotation class ClyResponseState

const val ENDPOINT_PING =                   "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/ping/index/"
const val ENDPOINT_CONVERSATION_RETRIEVE =  "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/conversation/retrieve/"
const val ENDPOINT_MESSAGE_SEEN =           "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/message/seen/"
const val ENDPOINT_MESSAGE_NEWS =           "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/message/news/"
const val ENDPOINT_MESSAGE_RETRIEVE =       "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/message/retrieve/"
const val ENDPOINT_MESSAGE_SEND =           "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/message/send/"
const val ENDPOINT_EVENT_TRACKING =         "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/event/"
const val ENDPOINT_REPORT_CRASH =           "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/crash/"
const val ENDPOINT_SURVEY_SUBMIT =          "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/survey/submit/"
const val ENDPOINT_SURVEY_SEEN =            "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/survey/seen/"
const val ENDPOINT_SURVEY_BACK =            "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/survey/back/"
const val ENDPOINT_SURVEY_REJECT =          "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/survey/reject/"
const val ENDPOINT_ACCOUNT_DETAILS =        "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/account/"
const val ENDPOINT_FORM_ATTRIBUTE =         "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/attribute/"
const val ENDPOINT_CONVERSATION_DISCARD =   "$CUSTOMERLY_API_ENDPOINT_BASEURL$CUSTOMERLY_API_VERSION/conversation/discard/"

internal const val RESPONSE_STATE__PREPARING = 0
internal const val RESPONSE_STATE__PENDING = 1
internal const val RESPONSE_STATE__OK = -1
internal const val RESPONSE_STATE__ERROR_NO_CONNECTION = -2
internal const val RESPONSE_STATE__ERROR_BAD_REQUEST = -3
internal const val RESPONSE_STATE__ERROR_NETWORK = -4
internal const val RESPONSE_STATE__ERROR_BAD_RESPONSE = -5
internal const val RESPONSE_STATE__SERVERERROR_USER_NOT_AUTHENTICATED = 403
internal const val RESPONSE_STATE__SERVERERROR_APP_INSOLVENT = 17
internal const val RESPONSE_STATE__NO_TOKEN_AVAILABLE = -6
internal const val RESPONSE_STATE__NO_APPID_AVAILABLE = -7

internal const val HEADER_X_CUSTOMERLY_SDK_KEY = "X-Customerly-sdk"
internal const val HEADER_X_CUSTOMERLY_SDK_VALUE = "android"

internal const val HEADER_X_CUSTOMERLY_SDK_VERSION_KEY = "X-Customerly-sdk-version"