package io.customerly

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Spanned
import android.util.Log
import com.github.zafarkhaja.semver.Version
import io.customerly.activity.ClyAppCompatActivity
import io.customerly.activity.conversations.startClyConversationsActivity
import io.customerly.alert.dismissAlertMessageOnActivityDestroyed
import io.customerly.alert.dismissAlertMessageOnUserLogout
import io.customerly.api.ClyApiRequest
import io.customerly.api.ENDPOINT_EVENT_TRACKING
import io.customerly.dialogfragment.dismissClySurveyDialog
import io.customerly.entity.*
import io.customerly.utils.*
import io.customerly.utils.download.CHANNEL_ID_DOWNLOAD
import io.customerly.utils.ggkext.ignoreException
import io.customerly.utils.ggkext.nullOnException
import io.customerly.utils.ggkext.safeString
import io.customerly.utils.htmlformatter.fromHtml
import io.customerly.utils.network.registerLollipopNetworkReceiver
import io.customerly.websocket.ClySocket
import org.json.JSONObject

/**
 * Created by Gianni on 19/04/18.
 * Project: Customerly-KAndroid-SDK
 */
private const val PREF_KEY_APP_ID = "CUSTOMERLY_APP_ID"
private const val PREF_KEY_HARDCODED_WIDGETCOLOR = "CUSTOMERLY_HARDCODED_WIDGETCOLOR"

//TODO Rename to Customerly
object Cly {

    /**
     * Set to true to enable error logging in the Console.
     * Avoid to enable it in release app versions, the suggestion is to pass your.application.package.BuildConfig.DEBUG as set value
     */
    var verboseLogging: Boolean = false

    var surveyEnabled: Boolean = true
    var supportEnabled: Boolean = true
        set(value) {
            when {
                field && !value -> {
                    field = false
                    Cly.clySocket.disconnect()
                }
                !field && value -> {
                    field = true
                    this.ifConfigured {
                        Cly.clySocket.connect()
                    }
                }
            }
        }

    fun configure(application: Application, customerlyAppId: String, widgetColor: Int? = null) {
        Cly.initialize(context = application.applicationContext)
        Cly.preferences?.edit()
                ?.putString(PREF_KEY_APP_ID, customerlyAppId)
                ?.putInt(PREF_KEY_HARDCODED_WIDGETCOLOR, widgetColor ?: 0)
                ?.apply()
        Cly.appId = customerlyAppId
        Cly.widgetColorHardcoded = widgetColor

        application.registerActivityLifecycleCallbacks(this.activityLifecycleCallbacks)
        Cly.configured = true
    }

    /**
     * Call this method to close the user's Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     */
    fun logoutUser() {
        this.ifConfigured {
            ignoreException {
                this.preferences?.jwtRemove()
                //TODO this.preferences?.edit()?.remove(PREF_CURRENT_EMAIL)?.remove(PREF_CURRENT_ID)?.remove(PREF_CURRENT_COMPANY_INFO)?.apply()
                Cly.clySocket.disconnect()
                Cly.nextPingAllowed = 0
                dismissAlertMessageOnUserLogout()

                Cly.activityLifecycleCallbacks.getLastDisplayedActivity()?.apply {
                    this.dismissClySurveyDialog()
                    (this as? ClyAppCompatActivity)?.onLogoutUser()
                }
                Cly.log(message = "Customerly.logoutUser completed successfully")
                //TODO this.__PING__Start(null, null)
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
    fun openSupport(activity: Activity) {
        this.ifConfigured {
            this.supportEnabled = true
            try {
                activity.startClyConversationsActivity()
                Cly.log(message = "Customerly.openSupport completed successfully")
            } catch (exception: Exception) {
                Cly.log(message = "A generic error occurred in Customerly.openSupport")
                clySendError(errorCode = ERROR_CODE__GENERIC, description = "Generic error in Customerly.openSupport", throwable = exception)
            }
        }
    }

    /**
     * Call this method to keep track of custom labeled events.<br></br>
     * <br></br>
     * You have to configure the Customerly SDK before using this method with [.configure]
     * @param eventName The event custom label
     */
    fun trackEvent(eventName: String) {
        if(eventName.isNotEmpty()) {
            this.ifConfigured {
                try {
                    if(Cly.jwtToken?.isAnonymous == false) {
                        ClyApiRequest(
                                endpoint = ENDPOINT_EVENT_TRACKING,
                                trials = 2,
                                converter = { Cly.log(message = "Customerly.trackEvent completed successfully for event $eventName") })
                                .p(key = "name", value = eventName)
                                .start()
                    } else {
                        Cly.log(message = "Only lead and registered users can track events")
                    }
                } catch (generic: Exception) {
                    Cly.log(message = "A generic error occurred in Customerly.trackEvent")
                    clySendError(errorCode = ERROR_CODE__GENERIC, description = "Generic error in Customerly.trackEvent", throwable = generic)
                }
            }
        }
    }







    private var initialized: Boolean = false
    private var configured: Boolean = false
    internal val clyDeviceJson: JSONObject by lazy {
        JSONObject()
                .put("os", "Android")
                .put("device", String.format("%s %s (%s)", Build.MANUFACTURER, Build.MODEL, Build.DEVICE))
                .put("os_version", Build.VERSION.SDK_INT)
                .put("sdk_version", BuildConfig.VERSION_NAME)
                .put("api_version", BuildConfig.CUSTOMERLY_API_VERSION)
                .put("socket_version", BuildConfig.CUSTOMERLY_SOCKET_VERSION)
    }

    internal var preferences: SharedPreferences? = null
            private set

    internal val widgetColorFallback: Int get() = this.widgetColorHardcoded ?: COLORINT_BLUE_MALIBU
    internal var widgetColorHardcoded: Int? = null
            set(value) {
                field = value?.takeIf { it != 0 } ?: field
            }

    internal var lastPing: ClyPingResponse by DefaultInitializerDelegate(constructor = { Cly.preferences?.lastPingRestore() ?: ClyPingResponse() })

    private var nextPingAllowed: Long = 0L

    internal var jwtToken: ClyJwtToken? = null
            private set
    internal fun jwtTokenUpdate(pingResponse: JSONObject) {
        this.jwtToken = pingResponse.parseJwtToken()
    }

    internal var appId: String? by TryOnceDelegate(attempt = { Cly.preferences?.safeString(key = PREF_KEY_APP_ID) })

    internal val welcomeMessage: Spanned? get() = fromHtml(message = if(this.jwtToken?.isUser == true) {
                this.lastPing.welcomeMessageUsers
            } else {
                this.lastPing.welcomeMessageVisitors
            })

    internal var postOnActivity: ((Activity) -> Boolean)? = null

    internal val clySocket = ClySocket()

    internal val activityLifecycleCallbacks = object : ForegroundAppChecker() {
        override fun doOnAppGoBackground(applicationContext: Context) {
            Cly.clySocket.disconnect()
        }
        override fun doOnActivityResumed(activity: Activity, fromBackground: Boolean) {
            if(fromBackground) {
                Cly.ifConfigured {
                    Cly.clySocket.connect()
                    //TODO __PING__Start(null, null);
                }
            }

            Cly.postOnActivity?.let { postOnActivity ->
                if(Cly.isEnabledActivity(activity = activity)) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if(postOnActivity(activity) && Cly.postOnActivity == postOnActivity) {
                            Cly.postOnActivity = null
                        }
                    }, 500)
                }
            }
        }
        override fun doOnActivityDestroyed(activity: Activity) {
            activity.dismissAlertMessageOnActivityDestroyed()//Need to dismiss the alert or leak window exception comes out
        }
    }

    private fun initialize(context: Context) {
        this.clyDeviceJson.apply {
            val pm = context.packageManager
            this.put("app_name", try {
                context.applicationInfo.loadLabel(pm).toString()
            } catch (exception: Exception) {
                "<Error retrieving the app name>"
            })
            this.put("app_package", context.packageName)
            this.put("app_version", try {
                pm.getPackageInfo(context.packageName, 0).also { packageInfo ->
                    this.put("app_version", packageInfo.versionName)
                    this.put("app_build", packageInfo.versionCode)
                }
                context.applicationInfo.loadLabel(pm).toString()
            } catch (exception: Exception) {
                "<Error retrieving the app name>"
            })
        }

        this.preferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID + ".SharedPreferences", Context.MODE_PRIVATE)

        this.widgetColorHardcoded = this.preferences?.getInt(PREF_KEY_HARDCODED_WIDGETCOLOR, 0)

        this.jwtToken = this.preferences?.jwtRestore()

        context.registerLollipopNetworkReceiver()

        ignoreException {
            Class.forName("com.crashlytics.android.Crashlytics")
                    .getDeclaredMethod("setString", String::class.java, String::class.java)(null, BuildConfig.APPLICATION_ID + " version:", BuildConfig.VERSION_NAME)
        }

        fromApiO {
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

    internal fun ifConfigured(reportingErrorEnabled : Boolean = false, then: ()->Unit) {
        when(this.configured) {
            false -> {
                this.log(message = "You need to configure the SDK, first")
                if (reportingErrorEnabled) {
                    clySendUnconfiguredError()
                }
            }
            true -> when {
                !this.sdkAvailable  -> this.log(message = "This version of Customerly SDK is not supported anymore. Please update your dependency", error = true)
                this.appInsolvent   -> this.log(message = "Your app has some pending payment. Please visit app.customerly.io", error = true)
                else                -> then()
            }
        }
    }

    internal fun log(message: String, error: Boolean = false) {
        if (this.verboseLogging) {
            if(error) {
                Log.e(BuildConfig.CUSTOMERLY_SDK_NAME, message)
            } else {
                Log.v(BuildConfig.CUSTOMERLY_SDK_NAME, message)
            }
        }
    }

    internal fun isEnabledActivity(activity: Activity): Boolean {
        //TODO return false if activity is in disabled activities list
        return true
    }

    internal var appInsolvent: Boolean = false

    internal val sdkAvailable: Boolean get() = nullOnException { Version.valueOf(BuildConfig.VERSION_NAME).greaterThan(Version.valueOf(this.lastPing.minVersion)) } ?: false

}