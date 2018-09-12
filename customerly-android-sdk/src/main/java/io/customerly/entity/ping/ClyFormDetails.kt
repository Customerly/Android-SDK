package io.customerly.entity.ping

import io.customerly.Customerly
import io.customerly.activity.chat.ClyChatActivity
import io.customerly.api.ClyApiRequest
import io.customerly.api.ClyApiResponse
import io.customerly.api.ENDPOINT_FORM_ATTRIBUTE
import io.customerly.utils.ggkext.*
import org.json.JSONObject

/**
 * Created by Gianni on 07/07/18.
 * Project: Customerly-KAndroid-SDK
 */

internal enum class ClyFormCast {
    STRING, NUMERIC, DATE, BOOL
}

internal val Int.toClyFormCast: ClyFormCast get() = when(this) {
    0 -> ClyFormCast.STRING
    1 -> ClyFormCast.NUMERIC
    2 -> ClyFormCast.DATE
    3 -> ClyFormCast.BOOL
    else -> throw Exception()
}

internal fun JSONObject.parseAppState(): ClyFormDetails? {
    return nullOnException {
        ClyFormDetails(
                attributeName = it.getTyped(name = "id"),
                hint = it.optTyped(name = "state_displayable_name"),
                label = it.optTyped(name = "description", fallback = ""),
//                type = it.optTyped(name = "type", fallback = "state"),
//                editable = it.optTyped(name = "editable", fallback = 0) == 1,
                cast = it.optTyped(name = "cast", fallback = 0).toClyFormCast)
    }
}

internal data class ClyFormDetails(
        internal val attributeName: String,
        internal val hint: String?,
        internal val label: String,
//        internal val type: String,
//        private val editable: Boolean,
        internal val cast: ClyFormCast
) {
    fun sendAnswer(chatActivity: ClyChatActivity?, callback:()->Unit) {
        if(chatActivity != null) {
            this.normalizedAnswer?.also { answer ->
                val weakChatActivity = chatActivity.weak()
                ClyApiRequest(
                        context = chatActivity,
                        endpoint = ENDPOINT_FORM_ATTRIBUTE,
                        jsonObjectConverter = { jo -> jo },
                        requireToken = true,
                        callback = { response ->
                            when (response) {
                                is ClyApiResponse.Success -> {
                                    Customerly.lastPing.setFormAnswered(form = this)
                                    Customerly.clySocket.sendAttributeSet(name = this.attributeName, value = answer)
                                    Customerly.clySocket.sendUserChanged(user = response.result)
                                    response.result.optJSONObject("data")?.also { data ->
                                        data.optString("email")?.also { email ->
                                            Customerly.currentUser.updateUser(email = email, userId = data.optString("user_id"), name = data.optString("name"))
                                        }
                                    }
                                    callback()
                                }
                            }
                            weakChatActivity.get()?.tryLoadForm()
                        })
                        .p(key = "name", value = this.attributeName)
                        .p(key = "value", value = answer).start()
            } ?: callback()
        }
    }

    private val normalizedAnswer: Any? get() = when (this.cast) {
        ClyFormCast.NUMERIC -> (this.answer as? Double)?.let { answer ->
            if(answer - answer.toInt() == 0.0) {
                @Suppress("IMPLICIT_CAST_TO_ANY")
                answer.toInt()
            } else {
                @Suppress("IMPLICIT_CAST_TO_ANY")
                answer
            }
        }
        ClyFormCast.STRING -> this.answer as? String
        ClyFormCast.DATE -> (this.answer as? Long)?.msAsSeconds
        ClyFormCast.BOOL -> when (this.answer) {
            true -> Integer.valueOf(1)
            false -> Integer.valueOf(0)
            else -> null
        }
    }

    internal var answer: Any? = null
    internal var answerConfirmed:Boolean = false
            set(value) {
                if(this.answer != null) {
                    field = value
                }
            }
}