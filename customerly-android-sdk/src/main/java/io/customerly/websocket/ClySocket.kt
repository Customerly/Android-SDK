package io.customerly.websocket

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

import android.app.Activity
import android.util.Log
import android.view.WindowManager
import io.customerly.Customerly
import io.customerly.activity.ClyAppCompatActivity
import io.customerly.activity.ClyRealtimeActivity
import io.customerly.activity.startClyRealtimeActivity
import io.customerly.alert.showClyAlertMessage
import io.customerly.api.ClyApiRequest
import io.customerly.api.ClyApiResponse
import io.customerly.api.ENDPOINT_MESSAGE_NEWS
import io.customerly.entity.ClyRealtimePayload
import io.customerly.entity.ClySocketParams
import io.customerly.entity.chat.ClyMessage
import io.customerly.entity.chat.parseMessagesList
import io.customerly.entity.parseRealtimePayload
import io.customerly.entity.parseSocketParams
import io.customerly.entity.ping.ClyFormCast
import io.customerly.sxdependencies.annotations.SXStringDef
import io.customerly.sxdependencies.annotations.SXUiThread
import io.customerly.utils.CUSTOMERLY_DEV_MODE
import io.customerly.utils.CUSTOMERLY_SDK_NAME
import io.customerly.utils.ClyActivityLifecycleCallback
import io.customerly.utils.ggkext.STimestamp
import io.customerly.utils.ggkext.ignoreException
import io.customerly.utils.ggkext.nullOnException
import io.customerly.utils.ggkext.optTyped
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONObject

/**
 * Created by Gianni on 30/04/18.
 * Project: Customerly-KAndroid-SDK
 */

private const val SOCKET_EVENT__TYPING = "typing"
private const val SOCKET_EVENT__SEEN = "seen"
private const val SOCKET_EVENT__MESSAGE = "message"
private const val SOCKET_EVENT__ATTRIBUTE_SET = "attribute:set"
private const val SOCKET_EVENT__REALTIME_CALL = "rt-call"
private const val SOCKET_EVENT__REALTIME_CANCEL = "rt-cancel"
private const val SOCKET_EVENT__REALTIME_RINGING = "rt-ringing"
private const val SOCKET_EVENT__REALTIME_UNAVAILABLE = "rt-unavailable"
private const val SOCKET_EVENT__REALTIME_ACCEPT = "rt-accept"
private const val SOCKET_EVENT__REALTIME_REJECT = "rt-reject"

@SXStringDef(SOCKET_EVENT__TYPING, SOCKET_EVENT__SEEN, SOCKET_EVENT__MESSAGE, SOCKET_EVENT__ATTRIBUTE_SET)
@Retention(AnnotationRetention.SOURCE)
private annotation class SocketEvent

internal class ClySocket {

    private var shouldBeConnected: Boolean = false
    private var socket: Socket? = null
    private var activeParams: ClySocketParams? = null
    internal var typingListener: ((conversationId: Long, accountId: Long, accountName: String?, isTyping: Boolean)->Unit)? = null

    @Suppress("PrivatePropertyName")
    private val CONNECT_LOCK = arrayOfNulls<Any>(0)
    private var postConnectWithParams: ClySocketParams? = null
    private var connectingAt: Long = 0L

    internal fun connect(newParams: JSONObject? = null) {
        val params = newParams?.takeIf { Customerly.isSdkAvailable && !Customerly.appInsolvent }?.parseSocketParams()
                ?: this.activeParams
        if(params != null) {
            if (synchronized(this.CONNECT_LOCK) {
                this.postConnectWithParams = params
                this.connectingAt <= System.currentTimeMillis() - 10000
            }) {
                this.connectIfScheduled()
            }
        }
    }


    private fun connectIfScheduled() {
        val params: ClySocketParams? = synchronized(this.CONNECT_LOCK) {
            if (this.connectingAt > System.currentTimeMillis() - 10000) {
                null
            } else {
                val params = this.postConnectWithParams
                this.postConnectWithParams = null
                if (params == null) {
                    null
                } else {
                    this.connectingAt = System.currentTimeMillis()
                    params
                }
            }
        }

        if (params != null && Customerly.isSupportEnabled) {
            this.shouldBeConnected = true
            Customerly.checkConfigured {
                val currentSocket = this.socket
                if (currentSocket?.connected() != true || this.activeParams?.equals(params) != true) {
                        //if socket is null or disconnected or connected with different params
                        this.disconnect(reconnecting = true, socket = currentSocket)

                        try {
                            IO.socket(params.uri, IO.Options().apply {
                                this.secure = true
                                this.forceNew = true
                                this.reconnection = true
                                //this.reconnectionDelay = 15000;
                                //this.reconnectionDelayMax = 60000;
                                this.query = params.query
                                this.transports = arrayOf(WebSocket.NAME)
                            }).also { socket ->
                                socket.on(SOCKET_EVENT__TYPING) { payload ->
                                    (payload?.firstOrNull() as? JSONObject)?.ignoreException { payloadJson ->
                                        /*  {   "conversation":{"conversation_id":"327298","account_id":82,"user_id":310083,"is_note":false},
                                            "is_typing":"y",
                                            "client":{"account_id":82,"name":"Gianni"}
                                        }   */
                                        val (accountId, accountName) = payloadJson.optTyped<JSONObject>(name = "client")?.let { client ->
                                            client.optTyped<Long>(name = "account_id") to client.optTyped<String>(name = "name")
                                        } ?: null to null
                                        if (accountId != null) {
                                            this.logSocket(event = SOCKET_EVENT__TYPING, payloadJson = payloadJson)

                                            payloadJson.optTyped<JSONObject>(name = "conversation")?.let { conversation ->
                                                if (Customerly.jwtToken?.userID == conversation.optTyped(name = "user_id", fallback = -1L)
                                                        && !conversation.optTyped(name = "is_note", fallback = false)) {
                                                    conversation.optTyped<Long>(name = "conversation_id")?.let { conversationId ->
                                                        this.typingListener?.invoke(conversationId, accountId, accountName, "y" == payloadJson.optTyped<String>("is_typing"))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                socket.on(SOCKET_EVENT__MESSAGE) { payload ->
                                    (payload?.firstOrNull() as? JSONObject)?.ignoreException { payloadJson ->
                                        //  {   user_id: 41897, account_id: 82, timestamp: 1483388854, from_account: true, conversation : {is_note: false} }
                                        if (payloadJson.optTyped(name = "from_account", fallback = false)) {
                                            this.logSocket(event = SOCKET_EVENT__MESSAGE, payloadJson = payloadJson)

                                            payloadJson.optTyped<Long>("timestamp")?.let { timestamp ->

                                                if (Customerly.jwtToken?.userID == payloadJson.optTyped(name = "user_id", fallback = -1L)
                                                        && payloadJson.optTyped<JSONObject>(name = "conversation")?.optTyped(name = "is_note", fallback = false) == false) {

                                                    ClyApiRequest(
                                                            endpoint = ENDPOINT_MESSAGE_NEWS,
                                                            requireToken = true,
                                                            jsonObjectConverter = { it.parseMessagesList() },
                                                            callback = { response ->
                                                                when (response) {
                                                                    //is ClyApiResponse.Failure -> { }
                                                                    is ClyApiResponse.Success -> this.onMessageNewsCallbackWithRetry(messages = response.result)
                                                                }
                                                            })
                                                            .p(key = "timestamp", value = timestamp)
                                                            .p(key = "lead_hash", value = Customerly.currentUser.leadHash)
                                                            .start()
                                                }
                                            }
                                        }
                                    }
                                }

                                    socket.on(SOCKET_EVENT__REALTIME_CALL) { payload ->
                                        (payload?.firstOrNull() as? JSONObject)?.ignoreException { payloadJson ->
                                            this.logSocket(event = SOCKET_EVENT__REALTIME_CALL, payloadJson = payloadJson)
                                            payloadJson.parseRealtimePayload()?.let { realtimePayload ->
                                                this.sendRealtimeRinging(realtimePayload)
                                                this.onRealtimeCall(realtimePayload)
                                            }
                                        }
                                    }

                                    socket.on(SOCKET_EVENT__REALTIME_CANCEL) { payload ->
                                        (payload?.firstOrNull() as? JSONObject)?.ignoreException { payloadJson ->
                                            this.logSocket(event = SOCKET_EVENT__REALTIME_CANCEL, payloadJson = payloadJson)
                                            payloadJson.parseRealtimePayload()?.let { realtimePayload ->
                                                this.onRealtimeCancel(realtimePayload)
                                            }
                                        }
                                    }

                                    val connectTime = System.currentTimeMillis()
                                    socket.on(Socket.EVENT_DISCONNECT) {
                                        if (System.currentTimeMillis() > connectTime + 15000L) {
                                            //Trick to avoid reconnection loop caused by disconnection after connection
                                            this.check()
                                        }
                                    }

                                socket.on(Socket.EVENT_CONNECT){
                                    synchronized(this.CONNECT_LOCK) {
                                        this.activeParams = params
                                        this.connectingAt = 0L
                                        setCurrentSocket(newSocket = socket)
                                    }
                                    this.connectIfScheduled()
                                }

                                socket.on(Socket.EVENT_CONNECT_ERROR) {
                                    synchronized(this.CONNECT_LOCK) {
                                        this.activeParams = params
                                        this.connectingAt = 0L
                                    }
                                    this.connectIfScheduled()
                                }

                            }.connect()

                        } catch (ignored: Exception) {
                        }
                    }
            }
        }
    }

    @Suppress("PrivatePropertyName")
    private val LOCK = arrayOfNulls<Any>(0)
    private fun setCurrentSocket(newSocket: Socket) {
        synchronized(this.LOCK) {
            this.disposeSocketCallUnderLOCK(socket = this.socket)
            this.socket = newSocket
        }
    }
    private fun disposeSocketCallUnderLOCK(socket: Socket?) {
        if (socket != null) {
            if (this.socket == socket) {
                this.socket = null
                this.activeParams = null
            }
            socket.off()
            socket.disconnect()
        }
    }

    internal fun disconnect(reconnecting: Boolean = false, socket: Socket? = this.socket) {
        if(!reconnecting) {
            this.shouldBeConnected = false
        }
        synchronized(this.LOCK) {
            this.disposeSocketCallUnderLOCK(socket = socket)
        }
    }

    @SXUiThread
    private fun onMessageNewsCallbackWithRetry(messages: ArrayList<ClyMessage>, retryOnBadTokenException: Boolean = true) {
        if(messages.isNotEmpty()) {
            val currentActivity: Activity? = ClyActivityLifecycleCallback.getLastDisplayedActivity()
            if (currentActivity != null && Customerly.isEnabledActivity(activity = currentActivity)) {
                when {
                    currentActivity is ClyAppCompatActivity -> currentActivity.onNewSocketMessages(messages = messages)
                    Customerly.isSupportEnabled -> {
                        try {
                            currentActivity.showClyAlertMessage(message = messages.first())
                        } catch (changedActivityWhileExecuting: WindowManager.BadTokenException) {
                            if (retryOnBadTokenException) {
                                this.onMessageNewsCallbackWithRetry(messages = messages, retryOnBadTokenException = false)
                            }
                        }
                    }
                }
            } else {
                Customerly.postOnActivity = { activity ->
                    when {
                        activity is ClyAppCompatActivity -> {
                            activity.onNewSocketMessages(messages = messages)
                            true
                        }
                        Customerly.isSupportEnabled -> {
                            try {
                                activity.showClyAlertMessage(message = messages.first())
                                true
                            } catch (changedActivityWhileExecuting: WindowManager.BadTokenException) {
                                false
                            }
                        }
                        else -> false
                    }
                }
            }
        }
    }

    @SXUiThread
    private fun onRealtimeCall(realtimePayload: ClyRealtimePayload) {
        val currentActivity: Activity? = ClyActivityLifecycleCallback.getLastDisplayedActivity()
        if (currentActivity != null) {
            when (currentActivity) {
                is ClyRealtimeActivity -> {
                    if(currentActivity.realtimePayload.conversation_id != realtimePayload.conversation_id) {
                        this.sendRealtimeUnavaliable(realtimePayload = realtimePayload)
                    }
                }
                else -> {
                    currentActivity.startClyRealtimeActivity(realtimePayload = realtimePayload)
                }
            }
        }
    }

    @SXUiThread
    private fun onRealtimeCancel(realtimePayload: ClyRealtimePayload) {
        val currentActivity: Activity? = ClyActivityLifecycleCallback.getLastDisplayedActivity()
        if (currentActivity != null && currentActivity is ClyRealtimeActivity && currentActivity.realtimePayload.conversation_id == realtimePayload.conversation_id) {
            currentActivity.finish()
        }
    }

    internal fun check() {
        if (this.shouldBeConnected && this.socket?.connected() != true) {
            this.connect()
        }
    }

    private fun logSocket(@SocketEvent event: String, payloadJson: JSONObject, receiving: Boolean = true) {
        @Suppress("ConstantConditionIf")
        if(CUSTOMERLY_DEV_MODE) {
            Log.e(CUSTOMERLY_SDK_NAME, "SOCKET ${ if(receiving) "RX" else "TX" } : $event -> ${payloadJson.nullOnException { it.toString(1) } ?: "malformed json payload" }")
        }
    }

    private fun send(@SocketEvent event: String, payloadJson: JSONObject) {
        this.check()
        this.socket?.also {
            this.logSocket(event = event, payloadJson = payloadJson, receiving = false)
        }?.emit(event, payloadJson)
    }

    internal fun sendStartTyping(conversationId: Long, previewText: String)
            = this.sendTyping(conversationId = conversationId, isTyping = true, previewText = previewText)
    internal fun sendStopTyping(conversationId: Long)
            = this.sendTyping(conversationId = conversationId, isTyping = false, previewText = null)
    private fun sendTyping(conversationId: Long, isTyping: Boolean, previewText: String?) {
        //{conversation: {conversation_id: 179170, user_id: 63378, is_note: false}, is_typing: "y", typing_preview: "I am writ"}
        Customerly.jwtToken?.userID?.ignoreException { userId ->
            this.send(
                    event = SOCKET_EVENT__TYPING,
                    payloadJson = JSONObject()
                            .put("conversation", JSONObject()
                                    .put("conversation_id", conversationId)
                                    .put("user_id", userId)
                                    .put("is_note", false))
                            .put("is_typing", if (isTyping) "y" else "n")
                            .put("typing_preview", previewText))
        }
    }

    internal fun sendSeen(messageId: Long, @STimestamp seenTimestamp: Long) {
        Customerly.jwtToken?.userID?.ignoreException { userId ->
            this.send(
                    event = SOCKET_EVENT__SEEN,
                    payloadJson = JSONObject()
                            .put("conversation", JSONObject()
                                    .put("conversation_message_id", messageId)
                                    .put("user_id", userId))
                            .put("seen_date", seenTimestamp))
        }
    }

    internal fun sendMessage(@STimestamp timestamp: Long) {
        if (timestamp != -1L) {
            Customerly.jwtToken?.userID?.ignoreException { userId ->
                this.send(
                        event = SOCKET_EVENT__MESSAGE,
                        payloadJson = JSONObject()
                                .put("timestamp", timestamp)
                                .put("user_id", userId)
                                .put("conversation", JSONObject()
                                        .put("is_note", false)))
            }
        }
    }

    internal fun sendAttributeSet(name: String, value: Any, cast: ClyFormCast, userData: JSONObject) {
        Customerly.jwtToken?.userID?.ignoreException { userId ->
            this.send(
                    event = SOCKET_EVENT__ATTRIBUTE_SET,
                    payloadJson = JSONObject()
                            .put("changed_by_user", true)
                            .put("user_id", userId)
                            .put("attribute_name", name)
                            .put("attribute_value", value)
                            .put("attribute_cast", cast.intValue)
                            .put("user_data", userData))
        }
    }

    private fun sendRealtimeRinging(realtimePayload: ClyRealtimePayload) {
        this.send(event = SOCKET_EVENT__REALTIME_RINGING, payloadJson = realtimePayload.toJson())
    }

    private fun sendRealtimeUnavaliable(realtimePayload: ClyRealtimePayload) {
        this.send(event = SOCKET_EVENT__REALTIME_UNAVAILABLE, payloadJson = realtimePayload.toJson())
    }

    internal fun sendRealtimeReject(realtimePayload: ClyRealtimePayload) {
        this.send(event = SOCKET_EVENT__REALTIME_REJECT, payloadJson = realtimePayload.toJson())
    }

    internal fun sendRealtimeAccept(realtimePayload: ClyRealtimePayload) {
        this.send(event = SOCKET_EVENT__REALTIME_ACCEPT, payloadJson = realtimePayload.toJson())
    }
}