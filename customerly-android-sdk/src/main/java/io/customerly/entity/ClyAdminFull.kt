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

import io.customerly.utils.ggkext.optTyped
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */

@Throws(JSONException::class)
internal fun JSONObject.parseAdminFull() = ClyAdminFull(adminFullJson = this)

internal class ClyAdminFull
@Throws(JSONException::class) constructor(adminFullJson: JSONObject)
    : ClyAdmin(adminJson = adminFullJson) {

    val email: String? = adminFullJson.optTyped(name = "email")
    val description: String? = adminFullJson.optTyped(name = "description")

    val socialProfileLinkedin: String? = adminFullJson.optTyped(name = "social_profile_linkedin")
    val socialProfileInstagram: String? = adminFullJson.optTyped(name = "social_profile_instagram")
    val socialProfileTwitter: String? = adminFullJson.optTyped(name = "social_profile_twitter")
    val socialProfileFacebook: String? = adminFullJson.optTyped(name = "social_profile_facebook")

    val jobTitle: String? = adminFullJson.optTyped(name = "job_title")

    //val timezone: String? = adminFullJson.optTyped(name = "timezone")
    val location: String? = adminFullJson.optTyped(name = "location")

}