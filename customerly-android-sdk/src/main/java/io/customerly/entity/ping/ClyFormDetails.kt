package io.customerly.entity.ping

import io.customerly.Customerly
import io.customerly.activity.chat.ClyChatActivity
import io.customerly.api.ClyApiRequest
import io.customerly.api.ClyApiResponse
import io.customerly.api.ENDPOINT_FORM_ATTRIBUTE
import io.customerly.entity.iamUser
import io.customerly.utils.ggkext.*
import org.json.JSONObject

/**
 * Created by Gianni on 07/07/18.
 * Project: Customerly-KAndroid-SDK
 */

internal enum class ClyFormCast(val intValue: Int) {
    STRING(intValue = 0), NUMERIC(intValue = 1), DATE(intValue = 2), BOOL(intValue = 3)
}

internal val Int.toClyFormCast: ClyFormCast get() = ClyFormCast.values().first { it.intValue == this }

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
                                    Customerly.clySocket.sendAttributeSet(name = this.attributeName, value = answer, cast = this.cast, userData = response.result)
                                    response.result.optJSONObject("data")?.also { data ->
                                        data.optTyped<String>(name = "email")?.also { email ->
                                            Customerly.currentUser.updateUser(
                                                    isUser = Customerly.iamUser(),
                                                    contactEmail = email,
                                                    contactName = data.optTyped<String>(name = "name"),
                                                    userId = data.optTyped<String>(name = "user_id"))
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