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

import android.util.Base64
import io.customerly.Customerly
import io.customerly.sxdependencies.annotations.SXSize
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

internal fun jwtRestore() {
    Customerly.jwtToken = Customerly.preferences?.safeString(key = PREFS_JWT_KEY)?.nullOnException { ClyJwtToken(it) }
}

internal fun jwtRemove() {
    Customerly.preferences?.edit()?.remove(PREFS_JWT_KEY)?.apply()
}

internal fun JSONObject.parseJwtToken() {
    Customerly.jwtToken = this.optTyped<String>(name = "token")?.nullOnException { ClyJwtToken(encodedJwt = it) }
}

fun Customerly.iamUser() = this.jwtToken?.isUser == true
fun Customerly.iamLead() = this.jwtToken?.isLead == true
fun Customerly.iamAnonymous() = this.jwtToken?.isAnonymous == true

internal class ClyJwtToken @Throws(IllegalArgumentException::class)
    constructor(
        @org.intellij.lang.annotations.Pattern(JWT_VALIDATOR_MATCHER)
        @SXSize(min = 5)
        private val encodedJwt: String) {

    internal val userID: Long?

    @UserType
    private val userType: Int
    internal val isUser: Boolean
    internal val isLead: Boolean
    internal val isAnonymous: Boolean

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
            this.isUser = USER_TYPE__USER and this.userType != 0
            this.isLead = USER_TYPE__LEAD and this.userType != 0
            this.isAnonymous = USER_TYPE__ANONYMOUS and this.userType != 0
        } else {
            this.userID = null
            this.userType = USER_TYPE__ANONYMOUS
            this.isUser = false
            this.isLead = false
            this.isAnonymous = true
        }

        Customerly.preferences?.edit()?.putString(PREFS_JWT_KEY, this.encodedJwt)?.apply()
    }

    override fun toString(): String = this.encodedJwt
}