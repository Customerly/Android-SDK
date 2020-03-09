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
import android.util.Log
import io.customerly.BuildConfig
import io.customerly.Customerly
import io.customerly.entity.ClyJwtToken
import io.customerly.entity.ERROR_CODE__GENERIC
import io.customerly.entity.JWT_KEY
import io.customerly.entity.parseJwtToken
import io.customerly.entity.ping.parsePing
import io.customerly.sxdependencies.annotations.SXIntRange
import io.customerly.sxdependencies.annotations.SXRequiresPermission
import io.customerly.utils.*
import io.customerly.utils.ggkext.*
import org.json.JSONArray
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
import kotlin.math.max

/**
 * Created by Gianni on 19/04/18.
 * Project: Customerly-KAndroid-SDK
 */
internal class ClyApiRequest<RESPONSE: Any>
    @SXRequiresPermission(Manifest.permission.INTERNET)
    internal constructor(
            context: Context? = null,
            @ClyEndpoint private val endpoint: String,
            private val requireToken: Boolean = false,
            @SXIntRange(from=1, to=5) private val trials: Int = 1,
            private val onPreExecute: ((Context?)->Unit)? = null,
            private val jsonObjectConverter: ((JSONObject)->RESPONSE?)? = null,
            private val jsonArrayConverter: ((JSONArray)->RESPONSE?)? = null,
            private val callback: ((ClyApiResponse<RESPONSE>)->Unit)? = null,
            private val reportingErrorEnabled: Boolean = true
    ){
    private val params: JSONObject = JSONObject()

    private var wContext: WeakReference<Context>? = context?.weak()
    private val context : Context? get() = this.wContext?.get() ?: ClyActivityLifecycleCallback.getLastDisplayedActivity()

    internal fun p(key: String, value: Boolean?) = this.apply { if(value != null) this.params.skipException { it.put(key, value) } }
    internal fun p(key: String, value: Double?) = this.apply { if(value != null) this.params.skipException { it.put(key, value) } }
    internal fun p(key: String, value: Int?) = this.apply { if(value != null) this.params.skipException { it.put(key, value) } }
    internal fun p(key: String, value: Long?) = this.apply { if(value != null) this.params.skipException { it.put(key, value) } }
    internal fun p(key: String, value: HashMap<*,*>?) = this.apply { if(value != null) this.params.skipException { it.putOpt(key, JSONObject(value)) } }
    internal fun p(key: String, value: Any?) = this.apply { if(value != null) this.params.skipException { it.putOpt(key, value) } }

    internal fun start() {
        Customerly.checkConfigured(reportingErrorEnabled = this.reportingErrorEnabled) {
            val context = this.context
            if(context?.checkConnection() == false) {
                Customerly.log(message = "Check your connection")
                ClyApiResponse.Failure<RESPONSE>(errorCode = RESPONSE_STATE__ERROR_NO_CONNECTION).also { errorResponse ->
                    this.callback?.doOnUiThread {
                        it.invoke(errorResponse)
                    }
                }
            } else {
                this.onPreExecute?.invoke(this.context)
                val request = this
                doOnBackground {
                    @ClyResponseState var responseState: Int = RESPONSE_STATE__PREPARING
                    var responseResult: RESPONSE? = null
                    val appId = Customerly.appId
                    if (appId != null) {
                        val params: JSONObject? = when (request.endpoint) {
                            ENDPOINT_PING, ENDPOINT_REPORT_CRASH -> request.fillParamsWithAppidAndDeviceInfo(params = request.params, appId = appId)
                            else -> {
                                when (Customerly.jwtToken) {
                                    null/* no jwt */ -> when (request.requireToken) {
                                        true/* jwt is required */ -> {
                                            //If not token available and token is mandatory, first perform first a ping to obtain it or kill the request
                                            request.executeRequest(endpoint = ENDPOINT_PING, params = request.fillParamsWithAppidAndDeviceInfo(appId = appId))
                                            when (Customerly.jwtToken) {
                                                null/* failed retrieving jwt */ -> {
                                                    responseState = RESPONSE_STATE__NO_TOKEN_AVAILABLE
                                                    null
                                                }
                                                else/* jwt ok */ -> request.fillParamsWithAppidAndDeviceInfo(params = request.params, appId = appId)
                                            }
                                        }
                                        false/* jwt is not required */ -> request.params
                                    }
                                    else/* jwt ok */ -> request.params
                                }
                            }
                        }

                        if (responseState == RESPONSE_STATE__PREPARING) {
                            //No errors
                            request.executeRequest(jwtToken = Customerly.jwtToken, params = params).let { (state, resultJsonObject, resultJsonArray) ->
                                responseState = state
                                if (request.endpoint == ENDPOINT_PING && resultJsonObject != null) {
                                    Customerly.lastPing = resultJsonObject.parsePing()
                                    Customerly.nextPingAllowed = resultJsonObject.optLong("next-ping-allowed", 0)
                                    Customerly.clySocket.connect(newParams = resultJsonObject.optJSONObject("websocket"))
                                }
                                responseResult = when {
                                    resultJsonObject != null -> request.jsonObjectConverter?.invoke(resultJsonObject)
                                    resultJsonArray != null -> request.jsonArrayConverter?.invoke(resultJsonArray)
                                    else -> null
                                }
                            }
                        }
                    } else {
                        responseState = RESPONSE_STATE__NO_APPID_AVAILABLE
                    }

                    val localResult: RESPONSE? = responseResult
                    request.callback?.doOnUiThread {
                            it.invoke(
                                when (responseState) {
                                    RESPONSE_STATE__OK -> if (localResult != null) {
                                        ClyApiResponse.Success(result = localResult)
                                    } else {
                                        ClyApiResponse.Failure(errorCode = RESPONSE_STATE__ERROR_BAD_RESPONSE)
                                    }
                                    else -> ClyApiResponse.Failure(errorCode = responseState)
                                })
                    }
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
        return (0 until max(1, this.trials)).asSequence().map {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                this.doOutput = true
                this.requestMethod = "POST"
                this.setRequestProperty(HEADER_X_CUSTOMERLY_SDK_KEY, HEADER_X_CUSTOMERLY_SDK_VALUE)
                this.setRequestProperty(HEADER_X_CUSTOMERLY_SDK_VERSION_KEY, BuildConfig.VERSION_NAME)
                this.setRequestProperty("Content-Type", "application/json")
                this.setRequestProperty("Accept-Language", Locale.getDefault().toString())//es: "it_IT"
                this.connectTimeout = 10000

                if (this is HttpsURLConnection) {
                    try {
                        this.sslSocketFactory = SSLContext.getInstance("TLS").let { ssl ->
                            ssl.init(null, null, SecureRandom())
                            ssl.socketFactory
                        }
                    } catch (e: NoSuchAlgorithmException) {
                        e.printStackTrace()
                    } catch (e: KeyManagementException) {
                        e.printStackTrace()
                    }
                }
            }.also { connection ->
                @Suppress("ConstantConditionIf")
                if (CUSTOMERLY_DEV_MODE) {
                    Log.e(CUSTOMERLY_SDK_NAME,
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
                            .nullOnException { jo -> jo.toString(4) } ?: "Malformed JSON")
                            .chunkedSequence(size = 500)
                            .forEach { row -> Log.e(CUSTOMERLY_SDK_NAME, row) }
                    Log.e(CUSTOMERLY_SDK_NAME, "\n-----------------------------------------------------------")
                }
            }
            try {
                conn.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray())
                    os.flush()

                    val responseString = BufferedReader(
                            InputStreamReader(
                                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                                        conn.inputStream
                                    } else {
                                        conn.errorStream
                                    })).use { br -> br.lineSequence().joinToString { line -> line } }

                    nullOnException { JSONObject(responseString) }?.let { responseJO -> //JSONObject Response
                        @Suppress("ConstantConditionIf")
                        if (CUSTOMERLY_DEV_MODE) {
                            Log.e(CUSTOMERLY_SDK_NAME,
                                    "-----------------------------------------------------------" +
                                            "\nHTTP RESPONSE" +
                                            "\n+ Endpoint:        " + endpoint +
                                            "\nJSON BODY:\n")
                            (responseJO
                                    .nullOnException { jo -> jo.toString(4) } ?: "Malformed JSON")
                                    .chunkedSequence(size = 500)
                                    .forEach { row -> Log.e(CUSTOMERLY_SDK_NAME, row) }
                            Log.e(CUSTOMERLY_SDK_NAME, "\n-----------------------------------------------------------")
                        }

                        if (!responseJO.has("error")) {
                            if (ENDPOINT_PING == endpoint) {
                                responseJO.parseJwtToken()
                            }
                            ClyApiInternalResponse(responseState = RESPONSE_STATE__OK, responseResultJsonObject = responseJO)
                        } else {
                            /* example: {   "error": "exception_title",
                                    "message": "Exception_message",
                                    "code": "ExceptionCode"     }   */
                            val errorCode = responseJO.optInt("code", -1)
                            Customerly.log(message = "ErrorCode: $errorCode Message: ${responseJO.optTyped(name = "message", fallback = "The server received the request but an error occurred")}")
                            when (errorCode) {
                                RESPONSE_STATE__SERVERERROR_APP_INSOLVENT -> {
                                    Customerly.appInsolvent = true
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
                    } ?: nullOnException { JSONArray(responseString) }?.let { responseJA -> //JSONArray response
                        ClyApiInternalResponse(responseState = RESPONSE_STATE__OK, responseResultJsonArray = responseJA)
                    } ?: ClyApiInternalResponse(responseState = RESPONSE_STATE__ERROR_NETWORK)
                }
            } catch (json : JSONException) {
                Customerly.log(message = "The server received the request but an error has come")
                ClyApiInternalResponse(responseState = RESPONSE_STATE__ERROR_BAD_RESPONSE)
            } catch (json : JSONException) {
                Customerly.log(message = "An error occurs during the connection to server")
                ClyApiInternalResponse(responseState = RESPONSE_STATE__ERROR_NETWORK)
            }
        }.withIndex().firstOrNull { iv : IndexedValue<ClyApiInternalResponse> -> iv.value.responseResultJsonObject != null || iv.index == this.trials -1 }?.value ?: ClyApiInternalResponse(responseState = ERROR_CODE__GENERIC)
    }

    private fun fillParamsWithAppidAndDeviceInfo(params: JSONObject? = null, appId: String): JSONObject {
        return (params ?: JSONObject())
                    .skipException { it.put("app_id", appId)}
                    .skipException { it.put("device", DeviceJson.json) }
    }
}

private data class ClyApiInternalResponse(@ClyResponseState val responseState: Int, val responseResultJsonObject: JSONObject? = null, val responseResultJsonArray: JSONArray? = null)

@Suppress("unused")
internal sealed class ClyApiResponse<RESPONSE: Any> {
    internal data class Success<RESPONSE: Any>(
            internal val result: RESPONSE) : ClyApiResponse<RESPONSE>()
    internal data class Failure<RESPONSE: Any>(
            @ClyResponseState internal val errorCode: Int) : ClyApiResponse<RESPONSE>()
}

