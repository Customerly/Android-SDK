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
import io.customerly.entity.ping.ClyPingResponse
import io.customerly.entity.ping.lastPingRestore
import io.customerly.sxdependencies.annotations.SXColorInt
import io.customerly.utils.*
import io.customerly.utils.download.CHANNEL_ID_DOWNLOAD
import io.customerly.utils.ggkext.*
import io.customerly.utils.htmlformatter.fromHtml
import io.customerly.utils.network.registerLollipopNetworkReceiver
import io.customerly.websocket.ClySocket
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

interface Callback : Function0<Unit>

/**
 * Created by Gianni on 19/04/18.
 * Project: Customerly-KAndroid-SDK
 */
private const val PREF_KEY_APP_ID = "CUSTOMERLY_APP_ID"

object Customerly {

    ////////////////////////////////////////////////////////////////////////////////
    // Public fields & functions ///////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    /**
     * Call this method to configure the SDK indicating the Customerly App ID before accessing it.<br>
     * Call this from your custom Application {@link Application#onCreate()}<br>
     *     <br>
     * You can choose to ignore the widget_color provided by the Customerly web console for the action bar styling in support activities and use an app-local widget_color instead.
     * @param application The application class reference
     * @param customerlyAppId The Customerly App ID found in your Customerly console
     * @param widgetColorInt Optional, the custom widget color int. If Color.TRANSPARENT, it will be ignored
     */
    @JvmStatic
    @JvmOverloads
    fun configure(application: Application, customerlyAppId: String, @SXColorInt widgetColorInt: Int? = null) {
        initialize(context = application.applicationContext)
        preferences?.edit()
                ?.putString(PREF_KEY_APP_ID, customerlyAppId)
                ?.apply()
        appId = customerlyAppId
        widgetColorHardcoded = widgetColorInt

        ClyActivityLifecycleCallback.registerOn(application = application)

        configured = true

        ping()
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
     * You have to configure the Customerly SDK before using this method with [.configure]
     * @param then Optional. The callback to be called when the task completes
     */
    @JvmStatic
    @JvmOverloads
    fun logoutUser(then: ()->Unit = {}) {
        this.checkConfigured {
            ignoreException {
                jwtToken = null
                currentUser.logout()
                clySocket.disconnect()
                nextPingAllowed = 0
                dismissAlertMessageOnUserLogout()

                ClyActivityLifecycleCallback.getLastDisplayedActivity()?.apply {
                    this.dismissClySurveyDialog()
                    (this as? ClyAppCompatActivity)?.onLogoutUser()
                }
                log(message = "logoutUser completed successfully")
                ping(success = then, failure = then)
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
                log(message = "openSupport completed successfully")
            } catch (exception: Exception) {
                log(message = "A generic error occurred in openSupport")
                clySendError(errorCode = ERROR_CODE__GENERIC, description = "Generic error in openSupport", throwable = exception)
            }
        }
    }

    /**
     * @return Returns true if the SDK is available.
     */
    @JvmStatic
    val isSdkAvailable: Boolean get() = nullOnException { Version.valueOf(BuildConfig.VERSION_NAME).greaterThan(Version.valueOf(this.lastPing.minVersion)) } ?: false

    /**
     * Call this method to set custom attributes to the user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with [.configure]
     * @param attributes Attributes HashMap for the user. Can contain only String, char, int, long, float or double values
     * @param success Optional. The callback to be called when the task completes successfully
     * @param failure Optional. The callback to be called when the task completes with failure
     * @throws IllegalArgumentException is thrown if the attributes check fails
     */
    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    @JvmStatic
    fun setAttributes(attributes: HashMap<String, Any>, success: ()->Unit = {}, failure: ()->Unit = {}) {
        setJsonAttributes(attributes = attributes, success = success, failure = failure)
    }

    /**
     * Call this method to set custom attributes to the user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with [.configure]
     * @param attributes Attributes pairs for the user. Can contain only String, char, int, long, float or double values
     * @param success Optional. The callback to be called when the task completes successfully
     * @param failure Optional. The callback to be called when the task completes with failure
     * @throws IllegalArgumentException is thrown if the attributes check fails
     */
    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun setAttributes(vararg attributes: Pair<String,Any>, success: ()->Unit = {}, failure: ()->Unit = {}) {
        setJsonAttributes(attributes = hashMapOf(*attributes), success = success, failure = failure)
    }

    /**
     * Call this method to set custom attributes to the user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with [.configure]
     * @param company Company attributes map for the user. Can contain only String, char, int, long, float or double values and must contain the company id and name with keys 'company_id' and 'name'
     * @param success Optional. The callback to be called when the task completes successfully
     * @param failure Optional. The callback to be called when the task completes with failure
     * @throws IllegalArgumentException is thrown if the attributes check fails
     */
    @JvmOverloads
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun setCompany(company: HashMap<String,Any>, success: ()->Unit = {}, failure: ()->Unit = {}) {
        company.assertValidCompanyMap()

        if(iamUser()) {
            tryClyTask(failure = failure) {
                ping(trySurvey = false, tryLastMessage = false, company = company,
                        success = success, successLog = "setCompany task completed successfully",
                        failure = failure, failureLog = "A generic error occurred in setCompany")
            }
        } else {
            log(message = "Cannot setCompany for lead users")
            failure()
        }
    }

    /**
     * Call this method to set custom attributes to the user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with [.configure]
     * @param values Company attributes pairs for the user. Can contain only String, char, int, long, float or double values.
     * @param companyId The company ID
     * @param companyName The company name
     * @param success Optional. The callback to be called when the task completes successfully
     * @param failure Optional. The callback to be called when the task completes with failure
     * @throws IllegalArgumentException is thrown if the attributes check fails
     */
    @Throws(IllegalArgumentException::class)
    fun setCompany(vararg values: Pair<String,Any>, companyId: String, companyName: String, success: ()->Unit = {}, failure: ()->Unit = {}) {
        setCompany(company = company(*values, companyId = companyId, companyName = companyName), success = success, failure = failure)
    }

    /**
     * Call this method to link your app user to the Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with [.configure]
     * @param email The user email address
     * @param userId Optional. The user id
     * @param name Optional. The user name
     * @param attributes Optional. The user attributes HashMap<String, Any>
     * @param company Optional. The user company HashMap<String, Any>. Remember a company map must contain a 'company_id' and a 'name'
     * @param success Optional. The callback to be called when the task completes successfully
     * @param failure Optional. The callback to be called when the task completes with failure
     * @throws IllegalArgumentException is thrown if the attributes check fails
     */
    @JvmOverloads
    @JvmStatic
    fun registerUser(email: String, userId: String? = null, name: String? = null,
                     attributes: HashMap<String, Any>? = null, company: HashMap<String,Any>? = null,
                     success: ()->Unit = {}, failure: ()->Unit = {}) {
        val emailTrimmed = email.trim()
        if(! Patterns.EMAIL_ADDRESS.matcher(emailTrimmed).matches()) {
            log(message = "registerUser require a valid email address to be passed")
            failure()
        }

        attributes?.assertValidAttributesMap()
        company?.assertValidCompanyMap()

        tryClyTask(failure = failure) {
            ping(trySurvey = false, tryLastMessage = true,
                    registerEmail = email, registerUserId = userId?.takeIf { it.isNotBlank() }, registerName = name?.takeIf { it.isNotBlank() },
                    attributes = attributes, company = company,
                    success = success, successLog = "registerUser task completed successfully",
                    failure = failure, failureLog = "A generic error occurred in registerUser")
        }
    }

    /**
     * Set to true or false to enable or disable the message receiving. It is ENABLED by default.<br>
     * A call to the method [.openSupport(Activity)] will force the enabling if it is disabled
     */
    @JvmStatic
    var isSupportEnabled: Boolean = true
        set(value) {
            when {
                field && !value -> {
                    field = false
                    clySocket.disconnect()
                }
                !field && value -> {
                    field = true
                    this.checkConfigured {
                        clySocket.connect()
                    }
                }
            }
        }

    /**
     * Set to true or false to enable or disable the survey receiving. It is ENABLED by default.<br>
     * A call to the method [.openSupport(Activity)] will force the enabling if it is disabled
     */
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
            tryClyTask {
                if(iamLead() || iamUser()) {
                    ClyApiRequest(
                            endpoint = ENDPOINT_EVENT_TRACKING,
                            trials = 2,
                            jsonObjectConverter = { log(message = "trackEvent completed successfully for event $eventName") })
                            .p(key = "name", value = eventName)
                            .start()
                } else {
                    log(message = "Only lead and registered users can track events")
                }
            }
        }
    }

    /**
     * Call this method to force a check for pending Surveys or Message for the current user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with [.configure()]
     */
    @JvmOverloads
    @JvmStatic
    fun update(success: ()->Unit = {}, failure: ()->Unit = {}) {
        if(System.currentTimeMillis() > nextPingAllowed) {
            ping(
                    success = success, successLog = "update task completed successfully",
                    failure = failure, failureLog = "A generic error occurred in update")
        } else {
            log(message = "You cannot call twice the update so fast. You have to wait ${(nextPingAllowed - System.currentTimeMillis()).msAsSeconds} seconds.")
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

    /**
    * Set to false to disable the attachment button inside the chat.
    * It is enabled by default
    */
    @JvmStatic
    fun setAttachmentsAvailable(enabled: Boolean) {
        this.isAttachmentsAvailable = enabled
    }

    /**
     * Build the attribute JSON starting from the pairs and companyId and companyName
     * @param companyAttributes Company attributes pairs for the user. Can contain only String, char, int, long, float or double values.
     * @param companyId The user company ID
     * @param companyName The user company name
     */
    //Not visible as static for java developers
    fun company(vararg companyAttributes: Pair<String,Any>, companyId: String, companyName: String): HashMap<String,Any> {
        return HashMap<String,Any>().apply {
            this[JSON_COMPANY_KEY_ID] = companyId
            this[JSON_COMPANY_KEY_NAME] = companyName
            companyAttributes.asSequence().fold(
                    initial = this,
                    operation = { acc,(key,value) ->
                        acc[key] = value
                        acc
                    })
        }
    }

    /**
     * Utility method that converts a JSONObject to an HashMap
     * @param jo The JSONObject to convert
     * @return The HashMap<String></String>,Object> containing the same values of the parameter
     */
    @JvmStatic
    fun json2hashmap(jo: JSONObject): java.util.HashMap<String, Any> {
        val map = java.util.HashMap<String, Any>()
        val keys = jo.keys()
        var key: String
        while (keys.hasNext()) {
            key = keys.next()
            try {
                map[key] = jo.get(key)
            } catch (ignored: JSONException) { }
        }
        return map
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

    internal val widgetColorFallback: Int @SXColorInt get() = this.widgetColorHardcoded ?: COLORINT_BLUE_MALIBU
    @SXColorInt internal var widgetColorHardcoded: Int? = null
            set(value) {
                if(value != 0) {
                    field = value
                }
            }

    internal val currentUser: ClyCurrentUser = ClyCurrentUser()

    internal var lastPing: ClyPingResponse by DefaultInitializerDelegate(constructor = { preferences?.lastPingRestore() ?: ClyPingResponse() })

    internal var nextPingAllowed: Long = 0L

    internal var jwtToken: ClyJwtToken? = null
            set(value) {
                field = value
                if(value == null) {
                    jwtRemove()
                }
            }

    internal var appId: String? by TryOnceDelegate(attempt = { preferences?.safeString(key = PREF_KEY_APP_ID) })

    internal val welcomeMessage: Spanned? get() = fromHtml(message = when {
        iamUser()    ->  this.lastPing.welcomeMessageUsers
        else    ->  this.lastPing.welcomeMessageVisitors
    })

    private lateinit var disabledActivities: ArrayList<String>

    internal var postOnActivity: ((Activity) -> Boolean)? = null

    internal val clySocket = ClySocket()

    private var adminsFullDetails: Array<ClyAdminFull>? = null

    internal var isAttachmentsAvailable: Boolean = true

    ////////////////////////////////////////////////////////////////////////////////
    // Private functions ///////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    private fun initialize(context: Context) {

        DeviceJson.loadContextInfos(context = context)

        this.preferences = context.getSharedPreferences(BuildConfig.LIBRARY_PACKAGE_NAME + ".SharedPreferences", Context.MODE_PRIVATE)
        this.currentUser.restore()

        jwtRestore()

        context.registerLollipopNetworkReceiver()

        tryCrashlyticsSetString(key = BuildConfig.LIBRARY_PACKAGE_NAME + " version", value = BuildConfig.VERSION_NAME)

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
                             registerEmail: String? = null, registerUserId: String? = null, registerName: String? = null,
                             attributes: HashMap<String, Any>? = null, company: HashMap<String,Any>? = null,
                             crossinline success: ()->Unit = {}, successLog: String? = null,
                             crossinline failure: ()->Unit = {}, failureLog: String? = null) {
        checkConfigured(ok = {
            ClyApiRequest(
                    endpoint = ENDPOINT_PING,
                    jsonObjectConverter = {
                        //lastPing = it.parsePing()
                        (trySurvey && lastPing.tryShowSurvey()) || (tryLastMessage && lastPing.tryShowLastMessage())
                    },
                    callback = {
                        when(it) {
                            is ClyApiResponse.Success -> {
                                if(successLog != null) {
                                    log(message = successLog)
                                }
                                if(company != null) {
                                    currentUser.updateCompany(company = company)
                                }
                                success()
                            }
                            is ClyApiResponse.Failure -> {
                                if (failureLog != null) {
                                    log(message = failureLog)
                                }
                                failure()
                            }
                        }
                    })
                    .p(key = "email", value = registerEmail ?: currentUser.userEmail)
                    .p(key = "user_id", value = registerUserId ?: currentUser.userId)
                    .p(key = "name", value = registerName)
                    .p(key = "attributes", value = attributes)
                    .p(key = "company", value = company?.also { currentUser.removeCompany() } ?: currentUser.company)
                    .start()
        }, not = failure)
    }

    private inline fun tryClyTask(failure: (()->Unit) = {}, crossinline task: ()->Unit) {
        checkConfigured(not = failure, ok = {
            try {
                task()
            } catch (exception: Exception) {
                log(message = "Generic error while executing a task")
                exception.printStackTrace()
                clySendError(errorCode = ERROR_CODE__GENERIC, description = "Generic error while executing a task", throwable = exception)
                failure()
            }
        })
    }

    @Throws(IllegalArgumentException::class)
    private fun setJsonAttributes(attributes: HashMap<String, Any>, success: ()->Unit = {}, failure: ()->Unit = {}) {
        attributes.assertValidAttributesMap()
        if(iamUser()) {
            tryClyTask(failure = failure) {
                ping(trySurvey = false, tryLastMessage = false, attributes = attributes,
                        success = success, successLog = "setAttributes task completed successfully",
                        failure = failure, failureLog = "A generic error occurred in setAttributes")
            }
        } else {
            log(message = "Cannot setAttributes for lead users")
            failure()
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun HashMap<String, Any>.assertValidAttributesMap() {
        if(! this.values.asSequence().all {
            when(it) {
                is String,is Int, is Byte,is Long,is Double,is Float,is Char,is Boolean -> true
                else -> false
            }
        }) {
            log(message = "Attributes Map can only contain String, char, byte, int, long, float or double values")
            throw IllegalArgumentException("Attributes Map can contain only Strings, int, float, long, double or char values")
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun HashMap<String,Any>.assertValidCompanyMap() {
        when {
            !this.containsKey(JSON_COMPANY_KEY_ID) -> {
                log(message = "Company Map must contain a String value with key \"$JSON_COMPANY_KEY_ID\" containing the Company ID")
                throw IllegalArgumentException("Company Map must contain a String value with key \"$JSON_COMPANY_KEY_ID\" containing the Company ID")
            }
            !this.containsKey(JSON_COMPANY_KEY_NAME) -> {
                log(message = "Company Map must contain a String value with key \"$JSON_COMPANY_KEY_NAME\" containing the Company Name")
                throw IllegalArgumentException("Company Map must contain a String value with key \"$JSON_COMPANY_KEY_NAME\" containing the Company Name")
            }
            ! this.values.asSequence().all {
                when(it) {
                    is String,is Int, is Byte,is Long,is Double,is Float,is Char,is Boolean -> true
                    else -> false
                }
            } -> {
                log(message = "Company Map can only contain String, char, byte, int, long, float or double values")
                throw IllegalArgumentException("Company Map can contain only Strings, int, float, long, double or char values")
            }
        }
    }

    internal fun getAccountDetails(callback: (Array<ClyAdminFull>?)->Unit) {
        adminsFullDetails?.apply {
            callback(this)
        } ?: checkConfigured(ok = {
            if(iamLead() || iamUser()) {
                ClyApiRequest(
                        endpoint = ENDPOINT_ACCOUNT_DETAILS,
                        requireToken = true,
                        jsonArrayConverter = { ja ->
                            ja.asSequenceOptNotNullMapped<JSONObject, ClyAdminFull> { jo -> jo.nullOnException { jobj -> jobj.parseAdminFull() } }.toList().toTypedArray()
                        },
                        callback = {
                            when (it) {
                                is ClyApiResponse.Success -> {
                                    adminsFullDetails = it.result
                                    callback(it.result)
                                }
                                is ClyApiResponse.Failure -> {
                                    callback(null)
                                }
                            }
                        })
                        .start()
            }
        })
    }
}