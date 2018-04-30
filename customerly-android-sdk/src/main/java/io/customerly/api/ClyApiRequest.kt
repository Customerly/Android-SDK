@file:Suppress("unused")

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

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.annotation.IntRange
import android.support.annotation.RequiresPermission
import android.util.Log
import io.customerly.BuildConfig
import io.customerly.Cly
import io.customerly.entity.ClyJwtToken
import io.customerly.entity.ERROR_CODE__GENERIC
import io.customerly.entity.JWT_KEY
import io.customerly.utils.ggkext.*
import org.jetbrains.anko.AnkoAsyncContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

/**
 * Created by Gianni on 19/04/18.
 * Project: Customerly-KAndroid-SDK
 */
internal class ClyApiRequest<RESPONSE: Any>
    @RequiresPermission(Manifest.permission.INTERNET)
    internal constructor(
        context: Context? = null,
        @ClyEndpoint private val endpoint: String,
        private val requireToken: Boolean = false,
        @IntRange(from=1, to=5) private val trials: Int = 1,
        private val onPreExecute: ((Context?)->Unit)? = null,
        private val converter: (JSONObject)->RESPONSE?,
        private val callback: ((ClyApiResponse<RESPONSE>)->Unit)? = null,
        private val reportingErrorEnabled: Boolean = true
    ){
    private val params: JSONObject = JSONObject()

    private var wContext: WeakReference<Context>? = context?.weak()
    private val context : Context? get() = this.wContext?.get() ?: Cly.activityLifecycleCallbacks.getLastDisplayedActivity()

    internal fun p(key: String, value: Boolean) = this.apply { this.params.skipException { it.put(key, value) } }
    internal fun p(key: String, value: Double) = this.apply { this.params.skipException { it.put(key, value) } }
    internal fun p(key: String, value: Int) = this.apply { this.params.skipException { it.put(key, value) } }
    internal fun p(key: String, value: Long) = this.apply { this.params.skipException { it.put(key, value) } }
    internal fun p(key: String, value: Any) = this.apply { this.params.skipException { it.putOpt(key, value) } }

    internal fun start() {
        Cly.ifConfigured(reportingErrorEnabled = this.reportingErrorEnabled) {
            val context = this.context
            if(context?.checkConnection() == false) {
                Cly.log(message = "Check your connection")
                ClyApiResponse.Failure<RESPONSE>(errorCode = RESPONSE_STATE__ERROR_NO_CONNECTION).also { errorResponse ->
                    if(if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                Looper.getMainLooper().isCurrentThread
                            } else {
                                Looper.getMainLooper() == Looper.myLooper()
                            }) {
                        this.callback?.invoke(errorResponse)
                    } else {
                        Handler(Looper.getMainLooper()).post { this.callback?.invoke(errorResponse) }
                    }
                }
            } else {
                this.onPreExecute?.invoke(this.context)
                val request = this
                val async : AnkoAsyncContext<*>.()->Unit = {
                    @ClyResponseState var responseState: Int = RESPONSE_STATE__PREPARING
                    var responseResult : RESPONSE? = null
                    val appId = Cly.appId
                    if(appId != null) {
                        var params:JSONObject? = null

                        if(ENDPOINT_PING == request.endpoint) {
                            params = request.fillParamsWithAppidAndDeviceInfo(params = request.params, appId = appId)
                        } else {
                            when(Cly.jwtToken) {
                                null -> {
                                    when(request.requireToken) {
                                        true -> {
                                            //If not token available and token is mandatory,
                                            //first perform first a ping to obtain it
                                            //or kill the request
                                            request.executeRequest(endpoint = ENDPOINT_PING, params = request.fillParamsWithAppidAndDeviceInfo(appId = appId))
                                            when(Cly.jwtToken) {
                                                null -> responseState = RESPONSE_STATE__PREPARING
                                                else -> if(ENDPOINT_REPORT_CRASH == request.endpoint) {
                                                    params = request.fillParamsWithAppidAndDeviceInfo(appId = appId)
                                                }
                                            }
                                        }
                                        false -> {
                                            //If not token available and token is not mandatory, i send the app_id and device
                                            params = request.fillParamsWithAppidAndDeviceInfo(appId = appId)
                                        }
                                    }
                                }
                                else/* jwt ok */ -> if(ENDPOINT_REPORT_CRASH == request.endpoint) {
                                    params = request.fillParamsWithAppidAndDeviceInfo(appId = appId)
                                }
                            }
                        }

                        if(responseState == RESPONSE_STATE__PREPARING) {
                            //No errors
                            request.executeRequest(jwtToken = Cly.jwtToken, params = params).let { (state, result) ->
                                responseState = state
                                responseResult = result?.let { request.converter(it) }
                            }
                        }
                    } else {
                        responseState = RESPONSE_STATE__NO_APPID_AVAILABLE
                    }

                    uiThread {
                        val localResult : RESPONSE? = responseResult
                        request.callback?.invoke(
                                when(responseState) {
                                    RESPONSE_STATE__OK -> if(localResult != null) {
                                        ClyApiResponse.Success(result = localResult)
                                    } else {
                                        ClyApiResponse.Failure(errorCode = RESPONSE_STATE__ERROR_BAD_RESPONSE)
                                    }
                                    else -> ClyApiResponse.Failure(errorCode = responseState)
                                })
                    }
                }
                if(context != null) {
                    context.doAsync { async() }
                } else {
                    request.doAsync { async() }
                }
            }
        }
    }

    private fun executeRequest(endpoint: String = this.endpoint, jwtToken: ClyJwtToken? = null, params: JSONObject? = null) : ClyApiInternalResponse {
        val requestBody = JSONObject()
                .also {
                    if (jwtToken != null) try {
                        it.put(JWT_KEY, jwtToken.toString())
                    } catch (exception: Exception) { }
                }
                .also {
                    if (params != null) try {
                        it.put("params", params)
                    } catch (exception: Exception) { }
                }
        return (0 until Math.max(1, this.trials)).asSequence().map {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                this.doOutput = true
                this.requestMethod = "POST"
                this.setRequestProperty("Content-Type", "application/json")
                this.setRequestProperty("Accept-Language", Locale.getDefault().toString())//es: "it_IT"
                this.connectTimeout = 10000

                if (this is HttpsURLConnection) {
                    try {
                        this.sslSocketFactory = SSLContext.getInstance("TLS").let {
                            it.init(null, null, SecureRandom())
                            it.socketFactory
                        }
                    } catch (e: NoSuchAlgorithmException) {
                        e.printStackTrace()
                    } catch (e: KeyManagementException) {
                        e.printStackTrace()
                    }
                }
            }.also { connection ->
                @Suppress("ConstantConditionIf")
                if (BuildConfig.CUSTOMERLY_DEV_MODE) {
                    Log.e(BuildConfig.CUSTOMERLY_SDK_NAME,
                            "-----------------------------------------------------------" +
                                    "\nNEW HTTP REQUEST" +
                                    "\n+ Endpoint:        " + endpoint +
                                    "\n+ SSL:             " + if (connection is HttpsURLConnection) {
                                "Active"
                            } else {
                                "Not Active"
                            } +
                                    "\n+ METHOD:          " + connection.requestMethod +
                                    "\n+ Content-Type:    " + connection.getRequestProperty("Content-Type") +
                                    "\n+ Accept-Language: " + connection.getRequestProperty("Accept-Language") +
                                    "\nJSON BODY:\n")
                    (requestBody
                            .nullOnException { it.toString(4) } ?: "Malformed JSON")
                            .chunkedSequence(size = 500)
                            .forEach { Log.e(BuildConfig.CUSTOMERLY_SDK_NAME, it) }
                    Log.e(BuildConfig.CUSTOMERLY_SDK_NAME, "\n-----------------------------------------------------------")
                }
            }
            try {
                conn.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray())
                    os.flush()

                    val response = BufferedReader(
                            InputStreamReader(
                                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                                        conn.inputStream
                                    } else {
                                        conn.errorStream
                                    })).use { it.lineSequence().joinToString { it } }
                            .let { JSONObject(it) }

                    @Suppress("ConstantConditionIf")
                    if (BuildConfig.CUSTOMERLY_DEV_MODE) {
                        Log.e(BuildConfig.CUSTOMERLY_SDK_NAME,
                                "-----------------------------------------------------------" +
                                        "\nHTTP RESPONSE" +
                                        "\n+ Endpoint:        " + endpoint +
                                        "\nJSON BODY:\n")
                        (response
                                .nullOnException { it.toString(4) } ?: "Malformed JSON")
                                .chunkedSequence(size = 500)
                                .forEach { Log.e(BuildConfig.CUSTOMERLY_SDK_NAME, it) }
                        Log.e(BuildConfig.CUSTOMERLY_SDK_NAME, "\n-----------------------------------------------------------")
                    }

                    if (!response.has("error")) {
                        if (ENDPOINT_PING == endpoint) {
                            Cly.jwtTokenUpdate(pingResponse = response)
                        }
                        ClyApiInternalResponse(responseState = RESPONSE_STATE__OK, responseResult = response)
                    } else {
                        /* example: {   "error": "exception_title",
                                "message": "Exception_message",
                                "code": "ExceptionCode"     }   */
                        val errorCode = response.optInt("code", -1)
                        Cly.log(message = "ErrorCode: $errorCode Message: ${response.optTyped(name = "message", fallback = "The server received the request but an error occurred")}")
                        when (errorCode) {
                            RESPONSE_STATE__SERVERERROR_APP_INSOLVENT -> {
                                Cly.appInsolvent = true
                                ClyApiInternalResponse(responseState = RESPONSE_STATE__SERVERERROR_APP_INSOLVENT)
                            }
                            RESPONSE_STATE__SERVERERROR_USER_NOT_AUTHENTICATED -> {
                                ClyApiInternalResponse(responseState = RESPONSE_STATE__SERVERERROR_USER_NOT_AUTHENTICATED)
                            }
                            /* -1, */ else -> {
                            ClyApiInternalResponse(responseState = RESPONSE_STATE__ERROR_NETWORK)
                            }
                        }
                    }
                }
            } catch (json : JSONException) {
                Cly.log(message = "The server received the request but an error has come")
                ClyApiInternalResponse(responseState = RESPONSE_STATE__ERROR_BAD_RESPONSE)
            } catch (json : JSONException) {
                Cly.log(message = "An error occurs during the connection to server")
                ClyApiInternalResponse(responseState = RESPONSE_STATE__ERROR_NETWORK)
            }
        }.withIndex().firstOrNull { iv : IndexedValue<ClyApiInternalResponse> -> iv.value.responseResult != null || iv.index == this.trials -1 }?.value ?: ClyApiInternalResponse(responseState = ERROR_CODE__GENERIC)
    }

    private fun fillParamsWithAppidAndDeviceInfo(params: JSONObject? = null, appId: String): JSONObject {
        return (params ?: JSONObject())
                    .skipException { it.put("app_id", appId)}
                    .skipException { it.put("device", Cly.clyDeviceJson) }
    }
}

private data class ClyApiInternalResponse(@ClyResponseState val responseState: Int, val responseResult: JSONObject? = null)

@Suppress("unused")
internal sealed class ClyApiResponse<RESPONSE: Any> {
    internal data class Success<RESPONSE: Any>(
            internal val result: RESPONSE) : ClyApiResponse<RESPONSE>()
    internal data class Failure<RESPONSE: Any>(
            @ClyResponseState internal val errorCode: Int) : ClyApiResponse<RESPONSE>()
}

