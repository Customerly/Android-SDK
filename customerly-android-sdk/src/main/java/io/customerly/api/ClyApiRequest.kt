package io.customerly.api

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.support.annotation.IntRange
import android.support.annotation.RequiresPermission
import android.util.Log
import android.view.View
import io.customerly.BuildConfig
import io.customerly.Customerly
import io.customerly.checkClyConfigured
import io.customerly.entity.ClyJwtToken
import io.customerly.entity.ERROR_CODE__GENERIC
import io.customerly.entity.JWT_KEY
import io.customerly.utils.ggkext.*
import kotlinx.coroutines.experimental.async
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
        @ClyEndpoint private val endpoint: String,
        context: Context? = null,
        @IntRange(from=1, to=5) private val trials: Int = 1,
        private val requireToken: Boolean = false,
        private val reportingErrorEnabled: Boolean = true,
        private val converter: (JSONObject)->RESPONSE,
        private val onPreExecute: ((Context?)->Unit)? = null,
        private val callback: ((Int, RESPONSE?)->RESPONSE)? = null
    ){
    private val params: JSONObject = JSONObject()

    private var wContext: WeakReference<Context>? = context?.weak()
    internal val context : Context? get() = this.wContext?.get()

    fun p(key: String, value: Boolean) = this.params.skipException { it.put(key, value) }
    fun p(key: String, value: Double) = this.params.skipException { it.put(key, value) }
    fun p(key: String, value: Int) = this.params.skipException { it.put(key, value) }
    fun p(key: String, value: Long) = this.params.skipException { it.put(key, value) }
    fun p(key: String, value: Any) = this.params.skipException { it.putOpt(key, value) }

    fun start() {
        checkClyConfigured(reportingErrorEnabled = this.reportingErrorEnabled) {
            val context = this.context
            if(context?.checkConnection() == false) {
                Customerly.get()._log("Check your connection")
                this.callback?.invoke(RESPONSE_STATE__ERROR_NO_CONNECTION, null)
            } else {
                this.onPreExecute?.invoke(this.context)
                val request = this
                async {
                    @ClyResponseState var responseState: Int = RESPONSE_STATE__PREPARING
                    var responseResult : RESPONSE? = null
                    val appId = Customerly.get()._AppID
                    if(appId != null) {
                        var jwtToken: ClyJwtToken? = Customerly.get()._JwtToken
                        var params:JSONObject? = null

                        if(ENDPOINT_PING == request.endpoint) {
                            params = request.fillParamsWithAppidAndDeviceInfo(params = request.params, appId = appId)
                        } else {
                            when(jwtToken) {
                                null -> {
                                    when(request.requireToken) {
                                        true -> {
                                            //If not token available and token is mandatory,
                                            //first perform first a ping to obtain it
                                            //or kill the request
                                            request.executeRequest(endpoint = ENDPOINT_PING, params = request.fillParamsWithAppidAndDeviceInfo(appId = appId))
                                            jwtToken = Customerly.get()._JwtToken
                                            when(jwtToken) {
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
                            request.executeRequest(jwtToken = jwtToken, params = params).let { (state, result) ->
                                responseState = state
                                responseResult = result?.let { request.converter(it) }
                            }
                        }
                    } else {
                        responseState = RESPONSE_STATE__NO_APPID_AVAILABLE
                    }
                    
                    //TODO run on mainthread
                    request.callback?.invoke(responseState, responseResult)
                }
            }
        }
    }

    private fun executeRequest(endpoint: String = this.endpoint, jwtToken: ClyJwtToken? = null, params: JSONObject? = null) : ClyApiResponse {
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
                                    "\n+ METHOD:          " + connection.getRequestMethod() +
                                    "\n+ Content-Type:    " + connection.getRequestProperty("Content-Type") +
                                    "\n+ Accept-Language: " + connection.getRequestProperty("Accept-Language") +
                                    "\nJSON BODY:\n")
                    (requestBody
                            .nullOnException { it.toString(4) } ?: "Malformed JSON")
                            .chunkedSequence(size = 500)
                            .forEach { Log.e(BuildConfig.CUSTOMERLY_SDK_NAME, it) }
                    Log.e(BuildConfig.CUSTOMERLY_SDK_NAME, "\n-----------------------------------------------------------");
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
                        Log.e(BuildConfig.CUSTOMERLY_SDK_NAME, "\n-----------------------------------------------------------");
                    }

                    if (!response.has("error")) {
                        if (ENDPOINT_PING == endpoint) {
                            Customerly.get()._TOKEN__update(response)
                        }
                        ClyApiResponse(responseState = RESPONSE_STATE__OK, responseResult = response)
                    } else {
                        /* example: {   "error": "exception_title",
                                "message": "Exception_message",
                                "code": "ExceptionCode"     }   */
                        val errorCode = response.optInt("code", -1)
                        Customerly.get()._log("ErrorCode: $errorCode Message: ${response.optTyped(name = "message", fallback = "The server received the request but an error occurred")}")
                        when (errorCode) {
                            RESPONSE_STATE__SERVERERROR_APP_INSOLVENT -> {
                                Customerly.get()._setIsAppInsolvent()
                                ClyApiResponse(responseState = RESPONSE_STATE__SERVERERROR_APP_INSOLVENT)
                            }
                            RESPONSE_STATE__SERVERERROR_USER_NOT_AUTHENTICATED -> {
                                ClyApiResponse(responseState = RESPONSE_STATE__SERVERERROR_USER_NOT_AUTHENTICATED)
                            }
                            /* -1, */ else -> {
                            ClyApiResponse(responseState = RESPONSE_STATE__ERROR_NETWORK)
                            }
                        }
                    }
                }
            } catch (json : JSONException) {
                Customerly.get()._log("The server received the request but an error has come")
                ClyApiResponse(responseState = RESPONSE_STATE__ERROR_BAD_RESPONSE)
            } catch (json : JSONException) {
                Customerly.get()._log("An error occurs during the connection to server")
                ClyApiResponse(responseState = RESPONSE_STATE__ERROR_NETWORK)
            }
        }.withIndex().firstOrNull { iv : IndexedValue<ClyApiResponse> -> iv.value.responseResult != null || iv.index == this.trials -1 }?.value ?: ClyApiResponse(responseState = ERROR_CODE__GENERIC)
    }

    private fun fillParamsWithAppidAndDeviceInfo(params: JSONObject? = null, appId: String): JSONObject {
        return (params ?: JSONObject())
                    .skipException { it.put("app_id", appId)}
                    .skipException { it.put("device", Customerly.get().__PING__DeviceJSON) }
    }
}

private data class ClyApiResponse(@ClyResponseState val responseState: Int, val responseResult: JSONObject? = null)



