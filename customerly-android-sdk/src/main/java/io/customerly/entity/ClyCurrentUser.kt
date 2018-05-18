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

import io.customerly.Customerly
import io.customerly.utils.JSON_COMPANY_KEY_ID
import io.customerly.utils.JSON_COMPANY_KEY_NAME
import org.json.JSONObject

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */
private const val CUSTOMERLY_LOGGED_EMAIL = "CUSTOMERLY_LOGGED_EMAIL"
private const val CUSTOMERLY_LOGGED_USERID = "CUSTOMERLY_LOGGED_USERID"
private const val CUSTOMERLY_LOGGED_COMPANYINFO = "CUSTOMERLY_LOGGED_COMPANYINFO"
internal class ClyCurrentUser(userId: String? = null, email: String? = null, company: JSONObject? = null) {
    internal var email: String? = email
        private set
    internal var userId: String? = userId
            private set
    internal var company: JSONObject? = company
        private set

    fun restore() {
        this.email = Customerly.preferences?.getString(CUSTOMERLY_LOGGED_EMAIL, null)
        this.userId = Customerly.preferences?.getString(CUSTOMERLY_LOGGED_USERID, null)
        this.company = try {
            JSONObject(Customerly.preferences?.getString(CUSTOMERLY_LOGGED_COMPANYINFO, null))
        } catch (exception: Exception) {
            null
        }
    }

    fun logout() {
        this.email = null
        this.userId = null
        this.company = null
        Customerly.preferences?.edit()?.remove(CUSTOMERLY_LOGGED_EMAIL)?.remove(CUSTOMERLY_LOGGED_USERID)?.remove(CUSTOMERLY_LOGGED_COMPANYINFO)?.apply()
    }

    fun removeCompany() {
        this.company = null
        Customerly.preferences?.edit()?.remove(CUSTOMERLY_LOGGED_COMPANYINFO)?.apply()
    }

    fun updateUser(email: String, userId: String?) {
        this.email = email
        this.userId = userId
        Customerly.preferences?.edit()
                ?.putString(CUSTOMERLY_LOGGED_EMAIL, email)
                ?.putString(CUSTOMERLY_LOGGED_USERID, userId)
                ?.apply()
    }

    fun updateCompany(company: JSONObject) {
        val newCompany = JSONObject()
                .put(JSON_COMPANY_KEY_ID, company.getString("company_id"))
                .put(JSON_COMPANY_KEY_NAME, company.getString("name"))
        this.company = newCompany
        Customerly.preferences?.edit()
                ?.putString(CUSTOMERLY_LOGGED_COMPANYINFO,newCompany.toString())
                ?.apply()
    }
}