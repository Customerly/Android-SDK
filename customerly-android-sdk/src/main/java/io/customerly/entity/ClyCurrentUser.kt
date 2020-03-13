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
import io.customerly.utils.ggkext.nullOnException
import org.json.JSONObject

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */
private const val CUSTOMERLY_LEAD_EMAIL = "CUSTOMERLY_LEAD_EMAIL"
private const val CUSTOMERLY_LOGGED_EMAIL = "CUSTOMERLY_LOGGED_EMAIL"
private const val CUSTOMERLY_LOGGED_USERID = "CUSTOMERLY_LOGGED_USERID"
private const val CUSTOMERLY_LOGGED_NAME = "CUSTOMERLY_LOGGED_NAME"
private const val CUSTOMERLY_LOGGED_LEAD_HASH = "CUSTOMERLY_LOGGED_LEAD_HASH"
private const val CUSTOMERLY_LOGGED_COMPANYINFO = "CUSTOMERLY_LOGGED_COMPANYINFO"
internal class ClyCurrentUser {
    internal val email: String? get() = this.userEmail ?: this.leadEmail
    private var leadEmail: String? = null
    internal var userEmail: String? = null
        private set
    internal var userId: String? = null
            private set
    internal var name: String? = null
            private set
    internal var company: HashMap<String,Any>? = null
            private set
    internal var leadHash: String? = null

    fun restore() {
        this.leadEmail = Customerly.preferences?.getString(CUSTOMERLY_LEAD_EMAIL, null)
        this.userEmail = Customerly.preferences?.getString(CUSTOMERLY_LOGGED_EMAIL, null)
        this.userId = Customerly.preferences?.getString(CUSTOMERLY_LOGGED_USERID, null)
        this.name = Customerly.preferences?.getString(CUSTOMERLY_LOGGED_NAME, null)
        this.leadHash = Customerly.preferences?.getString(CUSTOMERLY_LOGGED_LEAD_HASH, null)
        this.company = nullOnException {
            HashMap<String,Any>().apply {
                Customerly.preferences?.getString(CUSTOMERLY_LOGGED_COMPANYINFO, null)
                        ?.let { JSONObject(it) }
                        ?.also { json ->
                            json.keys()
                                    .asSequence()
                                    .mapNotNull { key ->
                                        json.opt(key)?.let { value ->
                                            key to value
                                        }
                                    }
                                    .forEach { (key,value) -> this[key] = value }
                        }
            }
        }
    }

    fun logout() {
        this.leadEmail = null
        this.userEmail = null
        this.userId = null
        this.name = null
        this.leadHash = null
        this.company = null
        Customerly.preferences
                ?.edit()
                ?.remove(CUSTOMERLY_LEAD_EMAIL)
                ?.remove(CUSTOMERLY_LOGGED_EMAIL)
                ?.remove(CUSTOMERLY_LOGGED_USERID)
                ?.remove(CUSTOMERLY_LOGGED_NAME)
                ?.remove(CUSTOMERLY_LOGGED_COMPANYINFO)
                ?.remove(CUSTOMERLY_LOGGED_LEAD_HASH)
                ?.apply()
    }

    fun removeCompany() {
        this.company = null
        Customerly.preferences?.edit()?.remove(CUSTOMERLY_LOGGED_COMPANYINFO)?.apply()
    }

    fun updateUser(isUser: Boolean, contactEmail: String, contactName: String?, userId: String?) {
        val prefsEdit = Customerly.preferences?.edit()
        when(isUser) {
            true -> {
                this.leadEmail = null
                this.userEmail = contactEmail
                this.userId = userId
                prefsEdit
                        ?.remove(CUSTOMERLY_LEAD_EMAIL)
                        ?.putString(CUSTOMERLY_LOGGED_EMAIL, contactEmail)
                        ?.putString(CUSTOMERLY_LOGGED_USERID, userId)
            }
            false -> {
                this.leadEmail = contactEmail
                this.userEmail = null
                this.userId = null
                prefsEdit
                        ?.putString(CUSTOMERLY_LEAD_EMAIL, contactEmail)
                        ?.remove(CUSTOMERLY_LOGGED_EMAIL)
                        ?.remove(CUSTOMERLY_LOGGED_USERID)
            }
        }
        prefsEdit
                ?.putString(CUSTOMERLY_LOGGED_NAME, contactName)
                ?.apply()
    }

    fun updateCompany(company: HashMap<String,Any>) {
        val companyId = company[JSON_COMPANY_KEY_ID]
        val companyName = company[JSON_COMPANY_KEY_NAME]
        if(companyId != null && companyName != null) {
            val newCompany = hashMapOf(JSON_COMPANY_KEY_ID to companyId, JSON_COMPANY_KEY_NAME to companyName)
            this.company = newCompany
            Customerly.preferences?.edit()
                    ?.putString(CUSTOMERLY_LOGGED_COMPANYINFO, newCompany.toString())
                    ?.apply()
        }
    }

    fun privacyPolicyAlreadyChecked() = this.email != null
}