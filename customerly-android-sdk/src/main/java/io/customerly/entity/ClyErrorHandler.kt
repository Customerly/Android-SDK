package io.customerly.entity

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

import io.customerly.sxdependencies.annotations.SXIntDef
import io.customerly.Customerly
import io.customerly.api.ClyApiRequest
import io.customerly.api.ENDPOINT_REPORT_CRASH
import io.customerly.utils.CUSTOMERLY_SDK_NAME

/**
 * Created by Gianni on 09/09/16.
 * Project: Customerly Android SDK
 */

@ErrorCode private const val ERROR_CODE__CUSTOMERLY_NOT_CONFIGURED = 1
@ErrorCode const val ERROR_CODE__IO_ERROR = 2
@ErrorCode const val ERROR_CODE__HTTP_REQUEST_ERROR = 3
@ErrorCode const val ERROR_CODE__HTTP_RESPONSE_ERROR = 4
@ErrorCode const val ERROR_CODE__GLIDE_ERROR = 5
@ErrorCode const val ERROR_CODE__ATTACHMENT_ERROR = 6
@ErrorCode const val ERROR_CODE__GENERIC = 7

@SXIntDef(ERROR_CODE__CUSTOMERLY_NOT_CONFIGURED, ERROR_CODE__IO_ERROR, ERROR_CODE__HTTP_REQUEST_ERROR, ERROR_CODE__HTTP_RESPONSE_ERROR, ERROR_CODE__GLIDE_ERROR, ERROR_CODE__ATTACHMENT_ERROR, ERROR_CODE__GENERIC)
@Retention(AnnotationRetention.SOURCE)
internal annotation class ErrorCode

internal fun clySendUnconfiguredError() {
    clySendError(errorCode = ERROR_CODE__CUSTOMERLY_NOT_CONFIGURED, description = CUSTOMERLY_SDK_NAME + "is not configured")
}

internal fun clySendError(@ErrorCode errorCode : Int, description : String, throwable : Throwable? = null) {
    val stacktraceDump = (throwable?.stackTrace ?: Thread.currentThread().stackTrace)
            .fold(StringBuilder()) { sb, ste ->
                sb.append(ste.toString()).append('\n')
            }.also {
                it.setLength(it.length - 1)
            }.toString()

    ClyApiRequest(
            endpoint = ENDPOINT_REPORT_CRASH,
            reportingErrorEnabled = false,
            jsonObjectConverter = {})
            .p(key = "error_code", value = errorCode)
            .p(key = "error_message", value = description)
            .p(key = "fullstacktrace", value = stacktraceDump)
            .start()

    Customerly.log(message = "Error sent -> code: $errorCode ||| message: $description ||| stack:\n$stacktraceDump")
}