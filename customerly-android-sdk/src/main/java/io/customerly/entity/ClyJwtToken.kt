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

import android.content.SharedPreferences
import android.support.annotation.Size
import android.util.Base64
import io.customerly.Cly
import io.customerly.utils.USER_TYPE__ANONYMOUS
import io.customerly.utils.USER_TYPE__LEAD
import io.customerly.utils.USER_TYPE__USER
import io.customerly.utils.UserType
import io.customerly.utils.ggkext.nullOnException
import io.customerly.utils.ggkext.optTyped
import io.customerly.utils.ggkext.safeString
import org.json.JSONObject

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */

internal const val JWT_KEY = "token"

private const val JWT_VALIDATOR_MATCHER = "([^.]+)\\.([^.]+)\\.([^.]+)"
private val JWT_PAYLOAD_MATCHER = java.util.regex.Pattern.compile("\\.(.*?)\\.")
private const val PREFS_JWT_KEY = "PREFS_TOKEN_KEY"

private fun SharedPreferences?.jwtStore(encodedJwt : String) {
    this?.edit()?.putString(PREFS_JWT_KEY, encodedJwt)?.apply()
}

internal fun SharedPreferences.jwtRestore() : ClyJwtToken? {
    return this.safeString(key = PREFS_JWT_KEY)?.let { /* @Subst("authB64.payloadB64.checksumB64") it -> */
        try {
            ClyJwtToken(it)
        } catch (wrongTokenFormat: IllegalArgumentException) {
            this.jwtRemove()
            null
        }
    }
}

internal fun SharedPreferences?.jwtRemove() {
    this?.edit()?.remove(PREFS_JWT_KEY)?.apply()
}

internal fun JSONObject.parseJwtToken(): ClyJwtToken? {
    return this.optTyped<String>(name = "token")?.nullOnException { ClyJwtToken(encodedJwt = it) }
}

internal class ClyJwtToken @Throws(IllegalArgumentException::class)
    constructor(
        @org.intellij.lang.annotations.Pattern(JWT_VALIDATOR_MATCHER)
        @param:Size(min = 5)
        private val encodedJwt: String) {

    internal val userID: Long?

    @UserType
    private val userType: Int
    internal val isUser: Boolean get() = USER_TYPE__USER and this.userType != 0
    internal val isLead: Boolean get() = USER_TYPE__LEAD and this.userType != 0
    internal val isAnonymous: Boolean get() = USER_TYPE__ANONYMOUS and this.userType != 0

    init {
        if (!this.encodedJwt.matches(JWT_VALIDATOR_MATCHER.toRegex())) {
            throw IllegalArgumentException("Wrong token format. Token: ${this.encodedJwt} does not match the regex $JWT_VALIDATOR_MATCHER")
        }

        val payloadJson = JWT_PAYLOAD_MATCHER.matcher(this.encodedJwt)
                .takeIf { it.find() }
                ?.nullOnException {
            JSONObject(String(Base64.decode(it.group(1), Base64.DEFAULT), Charsets.UTF_8))
        }

        if (payloadJson != null) {
            this.userID = payloadJson.optTyped("id", -1L).takeUnless { it == -1L }
            this.userType = payloadJson.optTyped("type", USER_TYPE__ANONYMOUS)
        } else {
            this.userID = null
            this.userType = USER_TYPE__ANONYMOUS
        }

        Cly.preferences?.jwtStore(encodedJwt = this.encodedJwt)
    }

    override fun toString(): String = this.encodedJwt
}