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

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */

internal fun JSONObject?.parseRealtimePayload(): ClyRealtimePayload? {
    if(this != null) {
        try {
            val account = this.optJSONObject("account")
            val user = this.optJSONObject("user")
            if(account != null && user != null) {
                val conversationId = this.optLong("conversation_id", 0L)
                val user_id = user.optLong("user_id", 0L)
                val url = this.optString("url", "")
                val accountId = account.optLong("account_id", 0L)
                val accountName = account.optString("name", "")
                val ts = this.optLong("ts", 0L)

                if (conversationId != 0L && user_id != 0L && url != "" && accountId != 0L && accountName != "" && ts != 0L) {
                    return ClyRealtimePayload(
                            conversationId,
                            ClyRealtimePayloadUser(user_id),
                            url,
                            ClyRealtimePayloadAccount(accountId, accountName),
                            ts
                    )
                }
            }
        } catch (e : JSONException) {
        }
    }
    return null
}

@Parcelize
internal class ClyRealtimePayload internal constructor(
        val conversation_id: Long,
        val user: ClyRealtimePayloadUser,
        val url: String,
        val account: ClyRealtimePayloadAccount,
        val ts: Long
): Parcelable {
    internal fun toJson(): JSONObject {
        return JSONObject()
                .put("conversation_id", this.conversation_id)
                .put("user", this.user)
                .put("url", this.url)
                .put("account", this.account.toJson())
                .put("ts", this.ts)
    }
}

@Parcelize
internal class ClyRealtimePayloadUser internal constructor(
        val user_id: Long
): Parcelable {
    internal fun toJson(): JSONObject {
        return JSONObject()
                .put("user_id", this.user_id)
    }
}

@Parcelize
internal class ClyRealtimePayloadAccount internal constructor(
        val account_id: Long,
        val name: String
): Parcelable {
    internal fun toJson(): JSONObject {
        return JSONObject()
                .put("account_id", this.account_id)
                .put("name", this.name)
    }
}