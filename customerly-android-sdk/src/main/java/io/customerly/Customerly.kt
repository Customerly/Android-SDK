@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package io.customerly

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
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.support.annotation.CheckResult
import android.support.annotation.ColorInt
import android.text.Spanned
import android.util.Log
import android.util.Patterns
import com.github.zafarkhaja.semver.Version
import io.customerly.activity.ClyAppCompatActivity
import io.customerly.activity.conversations.startClyConversationsActivity
import io.customerly.alert.dismissAlertMessageOnUserLogout
import io.customerly.api.*
import io.customerly.dialogfragment.dismissClySurveyDialog
import io.customerly.entity.*
import io.customerly.utils.*
import io.customerly.utils.download.CHANNEL_ID_DOWNLOAD
import io.customerly.utils.ggkext.*
import io.customerly.utils.htmlformatter.fromHtml
import io.customerly.utils.network.registerLollipopNetworkReceiver
import io.customerly.websocket.ClySocket
import org.json.JSONObject
import java.util.*

interface Callback : Function0<Unit>

/**
 * Created by Gianni on 19/04/18.
 * Project: Customerly-KAndroid-SDK
 */
private const val PREF_KEY_APP_ID = "CUSTOMERLY_APP_ID"

object Customerly {

    //TODO JAVADOC

    ////////////////////////////////////////////////////////////////////////////////
    // Public fields & functions ///////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    @JvmStatic
    @JvmOverloads
    fun configure(application: Application, customerlyAppId: String, @ColorInt widgetColorInt: Int? = null) {
        Customerly.initialize(context = application.applicationContext)
        Customerly.preferences?.edit()
                ?.putString(PREF_KEY_APP_ID, customerlyAppId)
                ?.apply()
        Customerly.appId = customerlyAppId
        Customerly.widgetColorHardcoded = widgetColorInt

        ClyActivityLifecycleCallback.registerOn(application = application)

        Customerly.configured = true

        Customerly.ping()
    }


    /**
     * Call this method to specify an Activity that will never display a message popup or survey.<br>
     * Every Activity is ENABLED by default
     * @param activityClass The Activity class
     * @see #enableOn(Class)
     */
    @JvmStatic
    fun disableOn(activityClass: Class<out Activity>) {
        if(!this::disabledActivities.isInitialized) {
            this.disabledActivities = ArrayList(1)
        }
        this.disabledActivities.add(activityClass.name)
    }

    /**
     * Call this method to re-enable an Activity previously disabled with [.disableOn].
     * @param activityClass The Activity class
     * @see .disableOn
     */
    @JvmStatic
    fun enableOn(activityClass: Class<out Activity>) {
        if(this::disabledActivities.isInitialized) {
            this.disabledActivities.remove(activityClass.name)
        }
    }

    /**
     * Call this method to close the user's Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     */
    @JvmStatic
    fun logoutUser() {
        this.checkConfigured {
            ignoreException {
                Customerly.jwtToken = null
                Customerly.currentUser.logout()
                Customerly.clySocket.disconnect()
                Customerly.nextPingAllowed = 0
                dismissAlertMessageOnUserLogout()

                ClyActivityLifecycleCallback.getLastDisplayedActivity()?.apply {
                    this.dismissClySurveyDialog()
                    (this as? ClyAppCompatActivity)?.onLogoutUser()
                }
                Customerly.log(message = "logoutUser completed successfully")
                Customerly.ping()
            }
        }
    }

    /**
     * Call this method to open the Support Activity.<br></br>
     * A call to this method will force the enabling if the support logic if it has been previously disabled with [.setSupportEnabled]
     * <br></br>
     * You have to configure the Customerly SDK before using this method with [.configure]
     * @param activity The current activity
     */
    @JvmStatic
    fun openSupport(activity: Activity) {
        this.checkConfigured {
            this.isSupportEnabled = true
            try {
                activity.startClyConversationsActivity()
                Customerly.log(message = "openSupport completed successfully")
            } catch (exception: Exception) {
                Customerly.log(message = "A generic error occurred in openSupport")
                clySendError(errorCode = ERROR_CODE__GENERIC, description = "Generic error in openSupport", throwable = exception)
            }
        }
    }

    @JvmStatic
    val isSdkAvailable: Boolean get() = nullOnException { Version.valueOf(BuildConfig.VERSION_NAME).greaterThan(Version.valueOf(this.lastPing.minVersion)) } ?: false

    @Throws(IllegalArgumentException::class)
    @JvmStatic
    fun setAttributes(attributes: JSONObject, success: ()->Unit = {}, failure: ()->Unit = {}) {
        attributes.assertValidAttributesMap()
        if(Customerly.jwtToken?.isUser == true) {
            Customerly.tryClyTask(failure = failure) {
                Customerly.ping(trySurvey = false, tryLastMessage = false, attributes = attributes,
                        success = success, successLog = "setAttributes task completed successfully",
                        failure = failure, failureLog = "A generic error occurred in setAttributes")
            }
        } else {
            Customerly.log(message = "Cannot setAttributes for lead users")
            failure()
        }
    }

    @Throws(IllegalArgumentException::class)
    @JvmStatic
    fun setAttributes(attributes: HashMap<String, Any>, success: ()->Unit = {}, failure: ()->Unit = {}) {
        Customerly.setAttributes(attributes = Customerly.attributeJson(map = attributes), success = success, failure = failure)
    }

    @Throws(IllegalArgumentException::class)
    fun setAttributes(vararg values: Pair<String,Any>, success: ()->Unit = {}, failure: ()->Unit = {}) {
        Customerly.setAttributes(attributes = Customerly.attributeJson(*values), success = success, failure = failure)
    }

    @JvmOverloads
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun setCompany(company: JSONObject, success: ()->Unit = {}, failure: ()->Unit = {}) {
        company.assertValidCompanyMap()

        if(Customerly.jwtToken?.isUser == true) {
            Customerly.tryClyTask(failure = failure) {
                Customerly.ping(trySurvey = false, tryLastMessage = false, company = company,
                        success = success, successLog = "setCompany task completed successfully",
                        failure = failure, failureLog = "A generic error occurred in setCompany")
            }
        } else {
            Customerly.log(message = "Cannot setCompany for lead users")
            failure()
        }
    }

    @Throws(IllegalArgumentException::class)
    @JvmStatic
    fun setCompany(company: HashMap<String, Any>, companyId: String, companyName: String, success: ()->Unit = {}, failure: ()->Unit = {}) {
        Customerly.setCompany(company = Customerly.companyJson(companyId = companyId, companyName = companyName, map = company), success = success, failure = failure)
    }

    @Throws(IllegalArgumentException::class)
    fun setCompany(vararg values: Pair<String,Any>, companyId: String, companyName: String, success: ()->Unit = {}, failure: ()->Unit = {}) {
        Customerly.setCompany(company = Customerly.companyJson(*values, companyId = companyId, companyName = companyName), success = success, failure = failure)
    }

    @JvmOverloads
    @JvmStatic
    fun registerUser(email: String, userId: String? = null, name: String? = null,
                     attributes: JSONObject? = null, company: JSONObject? = null,
                     success: ()->Unit = {}, failure: ()->Unit = {}) {
        val emailTrimmed = email.trim()
        if(! Patterns.EMAIL_ADDRESS.matcher(emailTrimmed).matches()) {
            Customerly.log(message = "registerUser require a valid email address to be passed")
            failure()
        }

        attributes?.assertValidAttributesMap()
        company?.assertValidCompanyMap()

        Customerly.currentUser.updateUser(email = email, userId = userId?.takeIf { it.isNotBlank() })

        Customerly.tryClyTask(failure = failure) {
            Customerly.ping(trySurvey = false, tryLastMessage = true,
                    name = name?.takeIf { it.isNotBlank() }, attributes = attributes, company = company,
                    success = success, successLog = "registerUser task completed successfully",
                    failure = failure, failureLog = "A generic error occurred in registerUser")
        }
    }

    @JvmOverloads
    @JvmStatic
    fun registerUser(email: String, userId: String? = null, name: String? = null,
                     attributes: HashMap<String, Any>, company: JSONObject? = null,
                     success: ()->Unit = {}, failure: ()->Unit = {}) {
        Customerly.registerUser(email = email, userId = userId, name = name,
                attributes = Customerly.attributeJson(map = attributes), company = company,
                success = success, failure = failure)
    }

    @JvmOverloads
    @JvmStatic
    fun registerUser(email: String, userId: String? = null, name: String? = null,
                     attributes: JSONObject?, companyId: String, companyName: String, company: HashMap<String, Any>,
                     success: ()->Unit = {}, failure: ()->Unit = {}) {
        Customerly.registerUser(email = email, userId = userId, name = name,
                attributes = attributes, company = Customerly.companyJson(companyId = companyId, companyName = companyName, map = company),
                success = success, failure = failure)
    }

    @JvmOverloads
    @JvmStatic
    fun registerUser(email: String, userId: String? = null, name: String? = null,
                     attributes: HashMap<String, Any>, companyId: String, companyName: String, company: HashMap<String, Any>,
                     success: ()->Unit = {}, failure: ()->Unit = {}) {
        Customerly.registerUser(email = email, userId = userId, name = name,
                attributes = Customerly.attributeJson(map = attributes), company = Customerly.companyJson(companyId = companyId, companyName = companyName, map = company),
                success = success, failure = failure)
    }

    @JvmStatic
    var isSupportEnabled: Boolean = true
        set(value) {
            when {
                field && !value -> {
                    field = false
                    Customerly.clySocket.disconnect()
                }
                !field && value -> {
                    field = true
                    this.checkConfigured {
                        Customerly.clySocket.connect()
                    }
                }
            }
        }

    @JvmStatic
    var isSurveyEnabled: Boolean = true

    /**
     * Call this method to keep track of custom labeled events.<br></br>
     * <br></br>
     * You have to configure the Customerly SDK before using this method with [.configure]
     * @param eventName The event custom label
     */
    @JvmStatic
    fun trackEvent(eventName: String) {
        if(eventName.isNotEmpty()) {
            Customerly.tryClyTask {
                if(Customerly.jwtToken?.isAnonymous == false) {
                    ClyApiRequest(
                            endpoint = ENDPOINT_EVENT_TRACKING,
                            trials = 2,
                            converter = { Customerly.log(message = "trackEvent completed successfully for event $eventName") })
                            .p(key = "name", value = eventName)
                            .start()
                } else {
                    Customerly.log(message = "Only lead and registered users can track events")
                }
            }
        }
    }

    @JvmOverloads
    @JvmStatic
    fun update(success: ()->Unit = {}, failure: ()->Unit = {}) {
        if(System.currentTimeMillis() > Customerly.nextPingAllowed) {
            Customerly.ping(
                    success = success, successLog = "update task completed successfully",
                    failure = failure, failureLog = "A generic error occurred in update")
        } else {
            Customerly.log(message = "You cannot call twice the update so fast. You have to wait ${(Customerly.nextPingAllowed - System.currentTimeMillis()).msAsSeconds} seconds.")
            failure()
        }
    }

    /**
    * Set to true to enable error logging in the Console.
    * Avoid to enable it in release app versions, the suggestion is to pass your.application.package.BuildConfig.DEBUG as set value
    */
    @JvmStatic
    fun setVerboseLogging(enabled: Boolean) {
        this.isVerboseLogging = enabled
    }

    //Not visible for java developers
    fun attributeJson(vararg values: Pair<String,Any>): JSONObject {
        return values.asSequence()
                .fold(initial = JSONObject(), operation = { acc,(key,value) -> acc.put(key, value) })
    }

    @JvmStatic
    fun attributeJson(map: HashMap<String, Any>): JSONObject {
        return JSONObject(map)
    }

    @JvmStatic
    @JvmOverloads
    fun companyJson(companyId: String, companyName: String, json: JSONObject = JSONObject()): JSONObject {
        return json.put(JSON_COMPANY_KEY_ID, companyId).put(JSON_COMPANY_KEY_NAME, companyName)
    }

    @JvmStatic
    fun companyJson(companyId: String, companyName: String, map: HashMap<String, Any>): JSONObject {
        return companyJson(companyId = companyId, companyName = companyName, json = JSONObject(map))
    }

    //Not visible for java developers
    fun companyJson(vararg values: Pair<String,Any>, companyId: String, companyName: String): JSONObject {
        return companyJson(companyId = companyId, companyName = companyName, json = values.asSequence()
                .fold(initial = JSONObject(), operation = { acc,(key,value) -> acc.put(key, value) }))
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Java SDK Deprecated Fields and Methods //////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    @JvmStatic
    @Deprecated(
            message = "You can access directly to Customerly object. So simply remove this `.get()` calling.",
            replaceWith = ReplaceWith("Customerly"))
    fun get() = this

    @Deprecated(
            message = "Use Customerly.companyJson(companyId,companyName) instead",
            replaceWith = ReplaceWith("Customerly.companyJson(companyId,companyName)"))
    class CompanyBuilder
    @Deprecated(
            message = "Use Customerly.companyJson(companyId,companyName) instead",
            replaceWith = ReplaceWith("Customerly.companyJson(companyId,companyName)")) constructor(companyId: String, companyName: String) {
        private val company = Customerly.companyJson(companyId = companyId, companyName = companyName)

        @Deprecated(
                message = "Use Customerly.companyJson(companyId,companyName) instead",
                replaceWith = ReplaceWith("Customerly.companyJson(companyId,companyName)"))
        @CheckResult fun put(key: String, value: String) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.companyJson(companyId,companyName) instead",
                replaceWith = ReplaceWith("Customerly.companyJson(companyId,companyName)"))
        @CheckResult fun put(key: String, value: Int) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.companyJson(companyId,companyName) instead",
                replaceWith = ReplaceWith("Customerly.companyJson(companyId,companyName)"))
        @CheckResult fun put(key: String, value: Byte) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.companyJson(companyId,companyName) instead",
                replaceWith = ReplaceWith("Customerly.companyJson(companyId,companyName)"))
        @CheckResult fun put(key: String, value: Long) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.companyJson(companyId,companyName) instead",
                replaceWith = ReplaceWith("Customerly.companyJson(companyId,companyName)"))
        @CheckResult fun put(key: String, value: Double) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.companyJson(companyId,companyName) instead",
                replaceWith = ReplaceWith("Customerly.companyJson(companyId,companyName)"))
        @CheckResult fun put(key: String, value: Float) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.companyJson(companyId,companyName) instead",
                replaceWith = ReplaceWith("Customerly.companyJson(companyId,companyName)"))
        @CheckResult fun put(key: String, value: Char) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.companyJson(companyId,companyName) instead",
                replaceWith = ReplaceWith("Customerly.companyJson(companyId,companyName)"))
        @CheckResult fun put(key: String, value: Boolean) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.companyJson(companyId,companyName) instead",
                replaceWith = ReplaceWith("Customerly.companyJson(companyId,companyName)"))
        @CheckResult private fun put(key: String, value: Any): CompanyBuilder {
            if (!(JSON_COMPANY_KEY_ID == key || JSON_COMPANY_KEY_NAME == key)) {
                ignoreException {
                    this.company.put(key, value)
                }
            }
            return this
        }
        @Deprecated(
                message = "Use Customerly.companyJson(companyId,companyName) instead",
                replaceWith = ReplaceWith("Customerly.companyJson(companyId,companyName)"))
        fun build(): JSONObject {
            return this.company
        }
    }

    @Deprecated(
            message = "Use Customerly.attributeJson() instead",
            replaceWith = ReplaceWith("Customerly.attributeJson()"))
    class AttributesBuilder
    @Deprecated(
            message = "Use Customerly.attributeJson()",
            replaceWith = ReplaceWith("Customerly.attributeJson()")) constructor( ) {
        private val attributes = Customerly.attributeJson()

        @Deprecated(
                message = "Use Customerly.attributeJson() instead",
                replaceWith = ReplaceWith("Customerly.attributeJson()"))
        @CheckResult fun put(key: String, value: String) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.attributeJson() instead",
                replaceWith = ReplaceWith("Customerly.attributeJson()"))
        @CheckResult fun put(key: String, value: Int) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.attributeJson() instead",
                replaceWith = ReplaceWith("Customerly.attributeJson()"))
        @CheckResult fun put(key: String, value: Byte) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.attributeJson() instead",
                replaceWith = ReplaceWith("Customerly.attributeJson()"))
        @CheckResult fun put(key: String, value: Long) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.attributeJson() instead",
                replaceWith = ReplaceWith("Customerly.attributeJson()"))
        @CheckResult fun put(key: String, value: Double) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.attributeJson() instead",
                replaceWith = ReplaceWith("Customerly.attributeJson()"))
        @CheckResult fun put(key: String, value: Float) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.attributeJson() instead",
                replaceWith = ReplaceWith("Customerly.attributeJson()"))
        @CheckResult fun put(key: String, value: Char) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.attributeJson() instead",
                replaceWith = ReplaceWith("Customerly.attributeJson()"))
        @CheckResult fun put(key: String, value: Boolean) = this.put(key, value as Any)
        @Deprecated(
                message = "Use Customerly.attributeJson() instead",
                replaceWith = ReplaceWith("Customerly.attributeJson()"))
        @CheckResult private fun put(key: String, value: Any): AttributesBuilder {
            ignoreException {
                this.attributes.put(key, value)
            }
            return this
        }
        @Deprecated(
                message = "Use Customerly.attributeJson() instead",
                replaceWith = ReplaceWith("Customerly.attributeJson()"))
        fun build(): JSONObject {
            return this.attributes
        }
    }



    ////////////////////////////////////////////////////////////////////////////////
    // Private fields //////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    private var initialized: Boolean = false
    private var configured: Boolean = false

    internal var isVerboseLogging: Boolean = false

    internal var appInsolvent: Boolean = false

    internal var preferences: SharedPreferences? = null
            private set

    internal val widgetColorFallback: Int @ColorInt get() = this.widgetColorHardcoded ?: COLORINT_BLUE_MALIBU
    @ColorInt internal var widgetColorHardcoded: Int? = null
            set(value) {
                if(value != 0) {
                    field = value
                }
            }

    internal var currentUser: ClyCurrentUser = ClyCurrentUser()

    internal var lastPing: ClyPingResponse by DefaultInitializerDelegate(constructor = { Customerly.preferences?.lastPingRestore() ?: ClyPingResponse() })

    internal var nextPingAllowed: Long = 0L

    internal var jwtToken: ClyJwtToken? = null
            set(value) {
                field = value
                if(value == null) {
                    jwtRemove()
                }
            }

    internal var appId: String? by TryOnceDelegate(attempt = { Customerly.preferences?.safeString(key = PREF_KEY_APP_ID) })

    internal val welcomeMessage: Spanned? get() = fromHtml(message = if(this.jwtToken?.isUser == true) {
                this.lastPing.welcomeMessageUsers
            } else {
                this.lastPing.welcomeMessageVisitors
            })

    private lateinit var disabledActivities: ArrayList<String>

    internal var postOnActivity: ((Activity) -> Boolean)? = null

    internal val clySocket = ClySocket()

    ////////////////////////////////////////////////////////////////////////////////
    // Private functions ///////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    private fun initialize(context: Context) {

        DeviceJson.loadContextInfos(context = context)

        this.preferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID + ".SharedPreferences", Context.MODE_PRIVATE)
        this.currentUser.restore()

        jwtRestore()

        context.registerLollipopNetworkReceiver()

        tryCrashlyticsSetString(key = BuildConfig.APPLICATION_ID + " version", value = BuildConfig.VERSION_NAME)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID_DOWNLOAD,"Attachment download", NotificationManager.IMPORTANCE_DEFAULT).apply {
                            this.description = "Notification of downloaded attachment success"
                            this.enableLights(true)
                            this.lightColor = Color.BLUE
                            this.enableVibration(true)
                            this.vibrationPattern = longArrayOf(0, 300)
                        })
        }

        this.initialized = true
    }

    internal inline fun checkConfigured(reportingErrorEnabled : Boolean = false, not: (()->Unit) = {}, ok: ()->Unit) {
        when(this.configured) {
            false -> {
                this.log(message = "You need to configure the SDK, first")
                if (reportingErrorEnabled) {
                    clySendUnconfiguredError()
                }
                not.invoke()
            }
            true -> when {
                !this.isSdkAvailable  -> this.log(message = "This version of Customerly SDK is not supported anymore. Please update your dependency", error = true)
                this.appInsolvent   -> this.log(message = "Your app has some pending payment. Please visit app.customerly.io", error = true)
                else                -> ok()
            }
        }
    }

    internal fun log(message: String, error: Boolean = false) {
        if (this.isVerboseLogging) {
            if(error) {
                Log.e(CUSTOMERLY_SDK_NAME, message)
            } else {
                Log.v(CUSTOMERLY_SDK_NAME, message)
            }
        }
    }

    internal fun isEnabledActivity(activity: Activity): Boolean {
        return !this::disabledActivities.isInitialized || !this.disabledActivities.contains(activity.javaClass.name)
    }

    internal inline fun ping(trySurvey: Boolean = true, tryLastMessage: Boolean = true,
                             name: String? = null, attributes: JSONObject? = null, company: JSONObject? = null,
                             crossinline success: ()->Unit = {}, successLog: String? = null,
                             crossinline failure: ()->Unit = {}, failureLog: String? = null) {
        Customerly.checkConfigured(ok = {
            ClyApiRequest(
                    endpoint = ENDPOINT_PING,
                    converter = {
                        //Customerly.lastPing = it.parsePing()
                        (trySurvey && Customerly.lastPing.tryShowSurvey()) || (tryLastMessage && Customerly.lastPing.tryShowLastMessage())
                    },
                    callback = {
                        when(it) {
                            is ClyApiResponse.Success -> {
                                if(successLog != null) {
                                    Customerly.log(message = successLog)
                                }
                                if(company != null) {
                                    Customerly.currentUser.updateCompany(company = company)
                                }
                                success()
                            }
                            is ClyApiResponse.Failure -> {
                                if (failureLog != null) {
                                    Customerly.log(message = failureLog)
                                }
                                failure()
                            }
                        }
                    })
                    .p(key = "name", value = name)
                    .p(key = "attributes", value = attributes)
                    .fillParamsWithCurrentUser(overrideCompany = company)
                    .start()
        }, not = failure)
    }

    private inline fun tryClyTask(failure: (()->Unit) = {}, crossinline task: ()->Unit) {
        Customerly.checkConfigured(not = failure, ok = {
            try {
                task()
            } catch (exception: Exception) {
                Customerly.log(message = "Generic error while executing a task")
                exception.printStackTrace()
                clySendError(errorCode = ERROR_CODE__GENERIC, description = "Generic error while executing a task", throwable = exception)
                failure()
            }
        })
    }

    @Throws(IllegalArgumentException::class)
    private fun JSONObject.assertValidAttributesMap() {
        if(! this.asValuesSequence().all {
            when(it) {
                is String,is Int, is Byte,is Long,is Double,is Float,is Char,is Boolean -> true
                else -> false
            }
            }) {
            Customerly.log(message = "Attributes Map can only contain String, char, byte, int, long, float or double values")
            throw IllegalArgumentException("Attributes Map can contain only Strings, int, float, long, double or char values")
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun JSONObject.assertValidCompanyMap() {
        when {
            !this.has(JSON_COMPANY_KEY_ID) -> {
                Customerly.log(message = "Company Map must contain a String value with key \"$JSON_COMPANY_KEY_ID\" containing the Company ID")
                throw IllegalArgumentException("Company Map must contain a String value with key \"$JSON_COMPANY_KEY_ID\" containing the Company ID")
            }
            !this.has(JSON_COMPANY_KEY_NAME) -> {
                Customerly.log(message = "Company Map must contain a String value with key \"$JSON_COMPANY_KEY_NAME\" containing the Company Name")
                throw IllegalArgumentException("Company Map must contain a String value with key \"$JSON_COMPANY_KEY_NAME\" containing the Company Name")
            }
            ! this.asValuesSequence().all {
                when(it) {
                    is String,is Int, is Byte,is Long,is Double,is Float,is Char,is Boolean -> true
                    else -> false
                }
            } -> {
                Customerly.log(message = "Company Map can only contain String, char, byte, int, long, float or double values")
                throw IllegalArgumentException("Company Map can contain only Strings, int, float, long, double or char values")
            }
        }
    }
}