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
import io.customerly.BuildConfig
import io.customerly.Customerly
import io.customerly.utils.ggkext.nullOnException
import io.customerly.utils.ggkext.optTyped
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.Charset

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */

@Throws(JSONException::class)
internal fun JSONObject.parseSocketParams() : ClySocketParams? {
    return this.optTyped<String>(name = "token")?.let { token ->
        val query = nullOnException {
            "token=" + URLEncoder.encode(
                    Base64.encodeToString(
                            JSONObject(
                                    String(
                                            Base64.decode(
                                                    token,
                                                    Base64.DEFAULT),
                                            Charset.forName("UTF-8")
                                    )
                            )
                                    .put("is_mobile", true)
                                    .put("socket_version", BuildConfig.CUSTOMERLY_SOCKET_VERSION)
                                    .toString().toByteArray(Charset.forName("UTF-8")),
                            Base64.NO_WRAP),
                    "UTF-8")
        }
        val endpoint: String? = this.optTyped(name = "endpoint")
        val port: String? = this.optTyped(name = "port")
        val userId = Customerly.jwtToken?.userID
        if(query != null && endpoint != null && port != null && userId != null) {
            ClySocketParams(uri = "$endpoint:$port/", query = query, userId = userId)
        } else {
            null
        }
    }
}

internal data class ClySocketParams (
    internal val uri: String,
    internal val query: String,
    internal val userId: Long)

/*"webSocket": {
  "endpoint": "https://ws2.customerly.io",
  "port": "8080"  }  */