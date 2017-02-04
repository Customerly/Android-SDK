package io.customerly;

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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.util.Patterns;

import org.intellij.lang.annotations.Subst;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * The singleton representing the Customerly SDK
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Customerly {

    private static final String PREFS_PING_RESPONSE__WIDGET_COLOR = "PREFS_PING_RESPONSE__WIDGET_COLOR";
    private static final String PREFS_PING_RESPONSE__POWERED_BY = "PREFS_PING_RESPONSE__POWERED_BY";
    private static final String PREFS_PING_RESPONSE__WELCOME_USERS = "PREFS_PING_RESPONSE__WELCOME_USERS";
    private static final String PREFS_PING_RESPONSE__WELCOME_VISITORS = "PREFS_PING_RESPONSE__WELCOME_VISITORS";
    private static final long SOCKET_PING_INTERVAL = 60000, SURVEY_DISPLAY_DELAY = 5000L;
    private static final String SOCKET_EVENT__PING = "p";
    private static final String SOCKET_EVENT__PING_ACTIVE = "a";
    private static final String SOCKET_EVENT__TYPING = "typing";
    private static final String SOCKET_EVENT__SEEN = "seen";
    private static final String SOCKET_EVENT__MESSAGE = "message";
    @ColorInt private static final int DEF_WIDGET_COLOR_MALIBU_INT = 0xff65a9e7;//Blue Malibu

    @NonNull final IU_RemoteImageHandler _RemoteImageHandler = new IU_RemoteImageHandler();
    @NonNull private final Handler __Handler = new Handler();

    private boolean initialized = false;
    private @Nullable SharedPreferences _SharedPreferences;
    @Nullable String _AppID, _AppCacheDir;
    @ColorInt private int
            __WidgetColor__Fallback = DEF_WIDGET_COLOR_MALIBU_INT,
            __WidgetColor__HardCoded = Color.TRANSPARENT;

    @Nullable IE_JwtToken _JwtToken;

    private boolean __VerboseLogging = true;

    @Nullable private Socket _Socket;

    private long __PING__next_ping_allowed = 0L;
    @ColorInt int __PING__LAST_widget_color;
    boolean __PING__LAST_powered_by;
    @Nullable private String __PING__LAST_welcome_message_users, __PING__LAST_welcome_message_visitors;
    @Nullable IE_Admin[] __PING__LAST_active_admins;

    @NonNull JSONObject __PING__DeviceJSON = new JSONObject();

    @Nullable private WeakReference<Activity> _CurrentActivity;
    @NonNull private final Application.ActivityLifecycleCallbacks __ActivityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
        private boolean paused = false, foreground = false;
        @NonNull private Runnable checkBackground = () -> {
            if (this.foreground && this.paused) {
                this.foreground = false;
                __SOCKET__disconnect();
            }
        };
        @Override public void onActivityResumed(Activity activity) {
            _CurrentActivity = new WeakReference<>(activity);
            this.paused = false;
            boolean wasBackground = !this.foreground;
            this.foreground = true;
            __Handler.removeCallbacks(this.checkBackground);
            if (wasBackground) {
                __PING__Start(null, null);
            }
        }
        @Override public void onActivityPaused(Activity activity) {
            this.paused = true;
            __Handler.removeCallbacks(this.checkBackground);
            __Handler.postDelayed(this.checkBackground, 500);
        }
        @Override public void onActivityStopped(Activity activity) { }
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
        @Override public void onActivityDestroyed(Activity activity) {
            PW_AlertMessage.onActivityDestroyed(activity);//Need to dismiss the alert or leak window exception comes out
        }
        @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
        @Override public void onActivityStarted(Activity activity) { }
    };

    private class PingResponseConverter implements IApi_Request.ResponseConverter<Void> {
        private final boolean _HandleAlertMessage, _HandleSurvey;
        private PingResponseConverter(boolean handleSurvey, boolean handleAlertMessage) {
            super();
            this._HandleSurvey = handleSurvey;
            this._HandleAlertMessage = handleAlertMessage;
        }
        @Nullable
        @Override
        public final Void convert(@NonNull JSONObject root) throws JSONException {
            __PING__next_ping_allowed = root.optLong("next-ping-allowed", 0);
            __SOCKET__connect(root.optJSONObject("websocket"));
            JSONObject app_config = root.optJSONObject("app_config");
            if(app_config != null) {
                if(__WidgetColor__HardCoded == Color.TRANSPARENT) {
                    String pingWidgetColor = IU_Utils.jsonOptStringWithNullCheck(app_config, "widget_color");
                    if (pingWidgetColor != null && pingWidgetColor.length() != 0) {
                        if (pingWidgetColor.charAt(0) != '#') {
                            pingWidgetColor = '#' + pingWidgetColor;
                        }
                        try {
                            __PING__LAST_widget_color = Color.parseColor(pingWidgetColor);
                        } catch (IllegalArgumentException notCorrectColor) {
                            IEr_CustomerlyErrorHandler.sendError(IEr_CustomerlyErrorHandler.ERROR_CODE__HTTP_RESPONSE_ERROR, String.format("PingResponse:data.apps.app_config.widget_color is an invalid argb color: '%s'", pingWidgetColor), notCorrectColor);
                            __PING__LAST_widget_color = __WidgetColor__Fallback;
                        }
                    }
                }
                __PING__LAST_powered_by = 1 == app_config.optLong("powered_by", 0);
                __PING__LAST_welcome_message_users = IU_Utils.jsonOptStringWithNullCheck(app_config, "welcome_message_users");
                __PING__LAST_welcome_message_visitors = IU_Utils.jsonOptStringWithNullCheck(app_config, "welcome_message_visitors");
            } else {
                __PING__LAST_widget_color = __WidgetColor__Fallback;
                __PING__LAST_powered_by = false;
                __PING__LAST_welcome_message_users = null;
                __PING__LAST_welcome_message_visitors = null;
            }
            __PING__LAST_active_admins = IE_Admin.from(root.optJSONArray("active_admins"));

            final SharedPreferences prefs = _SharedPreferences;
            if(prefs != null) {
                prefs.edit()
                        .putInt(PREFS_PING_RESPONSE__WIDGET_COLOR, __PING__LAST_widget_color)
                        .putBoolean(PREFS_PING_RESPONSE__POWERED_BY, __PING__LAST_powered_by)
                        .putString(PREFS_PING_RESPONSE__WELCOME_USERS, __PING__LAST_welcome_message_users)
                        .putString(PREFS_PING_RESPONSE__WELCOME_VISITORS, __PING__LAST_welcome_message_visitors)
                        .apply();
            }

            if(this._HandleSurvey) {
                IE_Survey[] surveys = IE_Survey.from(root.optJSONArray("last_surveys"));
                if(surveys != null) {
                    for (final IE_Survey survey : surveys) {
                        if (survey != null && !survey.isRejectedOrConcluded) {
                            __Handler.postDelayed(() -> {
                                Activity activity = _CurrentActivity == null ? null : _CurrentActivity.get();
                                if(activity != null) {
                                    try {
                                        IDlgF_Survey.show(activity, survey);
                                    } catch (Exception generic) {
                                        _log("A generic error occurred in Customerly.openSurvey");
                                        IEr_CustomerlyErrorHandler.sendError(IEr_CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.openSurvey", generic);
                                    }
                                }
                            }, SURVEY_DISPLAY_DELAY);
                            return null;
                        }
                    }
                }
            }

            if(this._HandleAlertMessage) {
                JSONArray last_messages_array = root.optJSONArray("last_messages");
                if (last_messages_array != null && last_messages_array.length() != 0) {
                    for (int i = 0; i < last_messages_array.length(); i++) {
                        try {
                            final JSONObject message = last_messages_array.getJSONObject(i);
                            if (message != null) {
                                __Handler.post(() -> {
                                    Activity activity = _CurrentActivity == null ? null : _CurrentActivity.get();
                                    if (activity != null) {
                                        if (activity instanceof SDKActivity) {
                                            ArrayList<IE_Message> list = new ArrayList<>(1);
                                            list.add(new IE_Message(message));
                                            ((SDKActivity) activity).onNewSocketMessages(list);
                                        } else {
                                            PW_AlertMessage.show(activity, new IE_Message(message));
                                        }
                                    }
                                });
                                return null;
                            }
                        } catch (JSONException ignored) {
                        }
                    }
                }
            }
            return null;
        }
    }

    @NonNull private final PingResponseConverter __PING__response_converter__SurveyMessage = new PingResponseConverter(true, true),
            __PING__response_converter__Message = new PingResponseConverter(false, true),
            __PING__response_converter__NaN = new PingResponseConverter(false, false);

    @NonNull static final Customerly _Instance = new Customerly();

    private Customerly() {
        super();
        try {
            this.__PING__DeviceJSON.put("os", "Android")
                    .put("device", String.format("%s %s (%s)", Build.MANUFACTURER, Build.MODEL, Build.DEVICE))
                    .put("os_version", Build.VERSION.SDK_INT)
                    .put("sdk_version", BuildConfig.VERSION_CODE)
                    .put("api_version", BuildConfig.CUSTOMERLY_API_VERSION)
                    .put("socket_version", BuildConfig.CUSTOMERLY_SOCKET_VERSION);
        } catch (JSONException ignored) { }
    }

    boolean _isConfigured() {
        if(this._AppID == null) {
            this._log("You need to configure the SDK ");
            IEr_CustomerlyErrorHandler.sendNotConfiguredError();
            return false;
        } else {
            return true;
        }
    }

    void _log(@NonNull String pLogMessage) {
        if(this.__VerboseLogging) {
            Log.v(BuildConfig.CUSTOMERLY_SDK_NAME, pLogMessage);
        }
    }

    @Nullable
    HtmlMessage _WELCOME__getMessage() {
        IE_JwtToken token = this._JwtToken;
        return this._isConfigured()
                ? IU_Utils.decodeHtmlStringWithEmojiTag(token != null && token.isUser() ? this.__PING__LAST_welcome_message_users : this.__PING__LAST_welcome_message_visitors)
                : null;
    }

    interface __SOCKET__ITyping_listener {   void onTypingEvent(long pConversationID, long account_id, boolean pTyping);   }
    @Nullable __SOCKET__ITyping_listener __SOCKET__Typing_listener = null;
    @Nullable private String __SOCKET__Endpoint = null, __SOCKET__Port = null;
    @Nullable private String __SOCKET__CurrentConfiguration = null;
    @NonNull private final Runnable __SOCKET__ping = () -> {
        Socket socket = this._Socket;
        if(socket != null && socket.connected()) {
            Activity activity = _CurrentActivity == null ? null : _CurrentActivity.get();
            socket.emit(activity != null && activity instanceof SDKActivity ? SOCKET_EVENT__PING_ACTIVE : SOCKET_EVENT__PING);
            this.__Handler.postDelayed(this.__SOCKET__ping, SOCKET_PING_INTERVAL);
        }
    };
    interface SDKActivity {
        void onNewSocketMessages(@NonNull ArrayList<IE_Message> messages);
        void onLogoutUser();
    }
    private void __SOCKET__connect(@Nullable JSONObject webSocket) {
        if (webSocket != null) {
            /*  "webSocket": {
                  "endpoint": "https://ws2.customerly.io",
                  "port": "8080"  }  */
            this.__SOCKET__Endpoint = IU_Utils.jsonOptStringWithNullCheck(webSocket, "endpoint");
            this.__SOCKET__Port = IU_Utils.jsonOptStringWithNullCheck(webSocket, "port");
        }

        if(this._AppID != null && this.__SOCKET__Endpoint != null && this.__SOCKET__Port != null) {
            IE_JwtToken token = this._JwtToken;
            if (token != null && token._UserID != null) {
                if(this.__SOCKET__CurrentConfiguration == null || ! this.__SOCKET__CurrentConfiguration.equals(String.format(Locale.UK, "%s-%s-%d", this.__SOCKET__Endpoint, this.__SOCKET__Port, token._UserID))) {

                    this.__SOCKET__disconnect();
                    this.__SOCKET__CurrentConfiguration = String.format(Locale.UK, "%s-%s-%d", this.__SOCKET__Endpoint, this.__SOCKET__Port, token._UserID);

                    String query;
                    try {
                        query = "json=" + new JSONObject().put("nsp", "user").put("app", this._AppID).put("id", token._UserID).put("socket_version", BuildConfig.CUSTOMERLY_SOCKET_VERSION).toString();
                    } catch (JSONException error) {
                        return;
                    }
                    Socket socket;
                    try {
                        IO.Options options = new IO.Options();
                        options.secure = true;
                        options.forceNew = true;
                        options.reconnectionDelay = 15000;
                        options.reconnectionDelayMax = 60000;
                        options.query = query;

                        socket = IO.socket(String.format("%s:%s/", this.__SOCKET__Endpoint, this.__SOCKET__Port), options);
                        this._Socket = socket;
                        socket.on(SOCKET_EVENT__TYPING, payload -> {
                            if (payload.length != 0) {
                                try {
                                    JSONObject payloadJson = (JSONObject) payload[0];
                                    /*  {   client : {account_id: 82, name: "Gianni"},
                                            conversation : {conversation_id: "173922", account_id: 82, user_id: 55722, is_note: false},
                                            is_typing : "n" } */
                                    JSONObject client = payloadJson.optJSONObject("client");
                                    if(client != null) {
                                        long account_id = client.optLong("account_id", -1L);
                                        if(account_id != -1L) {
                                            try {
                                                this._log(String.format("SOCKET RX: %1$s -> %2$s", SOCKET_EVENT__TYPING, payloadJson.toString(1)));
                                            } catch (JSONException ignored) {
                                            }
                                            boolean is_typing = "y".equals(payloadJson.optString("is_typing"));
                                            payloadJson = payloadJson.getJSONObject("conversation");
                                            if (payloadJson != null) {
                                                IE_JwtToken token2 = this._JwtToken;
                                                if (token2 != null && token2._UserID != null && token2._UserID == payloadJson.getLong("user_id") && !payloadJson.optBoolean("is_note", false)) {
                                                    long conversation_id = payloadJson.optLong("conversation_id", 0);
                                                    __SOCKET__ITyping_listener listener = this.__SOCKET__Typing_listener;
                                                    if (conversation_id != 0 && listener != null) {
                                                        listener.onTypingEvent(conversation_id, account_id, is_typing);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (JSONException ignored) { }
                            }
                        });
                        socket.on(SOCKET_EVENT__MESSAGE, payload -> {
                            if (payload.length != 0) {
                                try {
                                    JSONObject payloadJson = (JSONObject) payload[0];
                                    /*
                                    {user_id: 41897, account_id: 82, timestamp: 1483388854, from_account: true,
                                        conversation : {is_note: false} }
                                     */
                                    if (payloadJson.optBoolean("from_account")) {
                                        try {
                                            this._log(String.format("SOCKET RX: %1$s -> %2$s", SOCKET_EVENT__MESSAGE, payloadJson.toString(1)));
                                        } catch (JSONException ignored) { }
                                        long timestamp = payloadJson.optLong("timestamp", 0L);
                                        long socket_user_id = payloadJson.optLong("user_id", 0L);

                                        IE_JwtToken token2 = this._JwtToken;
                                        if (token2 != null && token2._UserID != null && token2._UserID == socket_user_id
                                                && socket_user_id != 0 && timestamp != 0
                                                && !payloadJson.getJSONObject("conversation").optBoolean("is_note", false)) {
                                            new IApi_Request.Builder<ArrayList<IE_Message>>(IApi_Request.ENDPOINT_MESSAGE_NEWS)
                                                    .opt_converter(data -> IU_Utils.fromJSONdataToList(data, "messages", IE_Message::new))
                                                    .opt_tokenMandatory()
                                                    .opt_receiver((responseState, new_messages) -> {
                                                        if (responseState == IApi_Request.RESPONSE_STATE__OK && new_messages != null && new_messages.size() != 0) {
                                                            Activity activity = _CurrentActivity == null ? null : _CurrentActivity.get();
                                                            if(activity != null) {
                                                                if (activity instanceof SDKActivity) {
                                                                    ((SDKActivity) activity).onNewSocketMessages(new_messages);
                                                                } else {
                                                                    PW_AlertMessage.show(activity, new_messages.get(0));
                                                                }
                                                            }
                                                        }
                                                    })
                                                    .param("timestamp", timestamp)
                                                    .start();
                                        }
                                    }
                                } catch (JSONException ignored) { }
                            }
                        });

                        socket.connect();
                        this.__Handler.postDelayed(this.__SOCKET__ping, SOCKET_PING_INTERVAL);
                    } catch (URISyntaxException ignored) { }
                }
            }
        }
    }
    private void __SOCKET__disconnect() {
        Socket socket = this._Socket;
        if (socket != null) {
            if (socket.connected()) {
                socket.disconnect();
            }
            this._Socket = null;
            this.__SOCKET__CurrentConfiguration = null;
        }
        this.__Handler.removeCallbacks(this.__SOCKET__ping);
    }
    private void __SOCKET__SEND(@NonNull String event, @NonNull JSONObject payloadJson) {
        Socket socket = this._Socket;
        if(socket != null && socket.connected()) {
            try {
                this._log(String.format("SOCKET TX: %1$s -> %2$s", event, payloadJson.toString(1)));
            } catch (JSONException ignored) { }
            socket.emit(event, payloadJson);
        }
    }
    void __SOCKET_SEND_Typing(long pConversationID, boolean pTyping) {
        //{conversation: {conversation_id: 179170, user_id: 63378, is_note: false}, is_typing: "n"}
        IE_JwtToken token = this._JwtToken;
        if(token != null && token._UserID != null) {
            try {
                this.__SOCKET__SEND(SOCKET_EVENT__TYPING, new JSONObject()
                        .put("conversation", new JSONObject()
                                .put("conversation_id", pConversationID)
                                .put("user_id", token._UserID)
                                .put("is_note", false))
                        .put("is_typing", pTyping ? "y" : "n"));
            } catch (JSONException ignored) { }
        }
    }
    void __SOCKET_SEND_Message(long pTimestamp) {
        if(pTimestamp != -1L) {
            IE_JwtToken token = this._JwtToken;
            if (token != null && token._UserID != null) {
                try {
                    this.__SOCKET__SEND(SOCKET_EVENT__MESSAGE, new JSONObject()
                            .put("timestamp", pTimestamp)
                            .put("user_id", token._UserID)
                            .put("conversation", new JSONObject()
                                    .put("is_note", false)));
                } catch (JSONException ignored) { }
            }
        }
    }
    void __SOCKET_SEND_Seen(long pConversationMessageID, long pSeenDate) {
        IE_JwtToken token = this._JwtToken;
        if(token != null && token._UserID != null) {
            try {
                this.__SOCKET__SEND(SOCKET_EVENT__SEEN, new JSONObject()
                        .put("conversation", new JSONObject()
                                .put("conversation_message_id", pConversationMessageID)
                                .put("user_id", token._UserID))
                        .put("seen_date", pSeenDate));
            } catch (JSONException ignored) { }
        }
    }

    @SuppressLint("CommitTransaction")
    private boolean __ExecutingPing = false;
    private synchronized void __PING__Start(@Nullable SuccessCallback pSuccessCallback, @Nullable FailureCallback pFailureCallback) {
        if(this._isConfigured()) {
            this.__ExecutingPing = true;
            //noinspection SpellCheckingInspection
            new IApi_Request.Builder<Void>(IApi_Request.ENDPOINT_PING)
                    .opt_converter(this.__PING__response_converter__SurveyMessage)
                    .opt_receiver((responseState, _null) -> {
                        this.__ExecutingPing = false;
                        if (responseState == IApi_Request.RESPONSE_STATE__OK) {
                            if(pSuccessCallback != null) {
                                pSuccessCallback.onSuccess();
                            }
                        } else {
                            if(pFailureCallback != null) {
                                pFailureCallback.onFailure();
                            }
                        }
                    })
                    .param("email", IU_Utils.getStringSafe(this._SharedPreferences, "regusrml"))
                    .param("user_id", IU_Utils.getStringSafe(this._SharedPreferences, "regusrid"))
                    .start();
        } else {
            if(pFailureCallback != null) {
                pFailureCallback.onFailure();
            }
        }
    }

    void _TOKEN__update(@NonNull JSONObject obj) {
        @Subst("authB64.payloadB64.checksumB64") String token = obj.optString("token");
        if(token != null) {
            try {
                SharedPreferences prefs = this._SharedPreferences;
                if(prefs != null) {
                    this._JwtToken = new IE_JwtToken(token, prefs);
                } else {
                    this._JwtToken = new IE_JwtToken(token);
                }
            } catch (IllegalArgumentException wrongTokenFormat) {
                this._JwtToken = null;
            }
        }
    }

    /* ****************************************************************************************************************************************************************/
    /* ********************************************************************************************************************************************** Public Methods **/
    /* ****************************************************************************************************************************************************************/
    /**
     * A class representing a {@link SpannableStringBuilder} with a method {@link #toPlainTextString()} that convert the formatted text to a plain string
     */
    static class HtmlMessage extends SpannableStringBuilder {
        HtmlMessage(SpannableStringBuilder ssb) {
            super(ssb);
        }
        @NonNull public String toPlainTextString() {
            return super.toString().replace("\uFFFC", "<IMAGE>");
        }
    }

    public interface SuccessCallback {
        /**
         * Implement this interface to obtain async success response from {@link #registerUser(String, String, String, SuccessCallback, FailureCallback)},
         * {@link #registerUser(String, String, String, JSONObject, SuccessCallback, FailureCallback)} or {@link #setAttributes(JSONObject, SuccessCallback, FailureCallback)}
         */
        void onSuccess();
    }

    public interface FailureCallback {
        /**
         * Implement this interface to obtain async failure response from {@link #registerUser(String, String, String, SuccessCallback, FailureCallback)},
         * {@link #registerUser(String, String, String, JSONObject, SuccessCallback, FailureCallback)} or {@link #setAttributes(JSONObject, SuccessCallback, FailureCallback)}
         */
        void onFailure();
    }

    private void __init(@NonNull Context pApplicationContext) {
        Customerly._Instance._AppCacheDir = pApplicationContext.getCacheDir().getPath();
        //APP INFORMATION
        try {
            Customerly._Instance.__PING__DeviceJSON.put("app_name", pApplicationContext.getApplicationInfo().loadLabel(pApplicationContext.getPackageManager()).toString());
        } catch (JSONException | NullPointerException err) {
            try {
                Customerly._Instance.__PING__DeviceJSON.put("app_name", "<Error retrieving the app name>");
            } catch (JSONException ignored) { }
        }
        try {
            PackageInfo pinfo = pApplicationContext.getPackageManager().getPackageInfo(pApplicationContext.getPackageName(), 0);
            Customerly._Instance.__PING__DeviceJSON.put("app_version", pinfo.versionName).put("app_build", pinfo.versionCode);
        } catch (JSONException | PackageManager.NameNotFoundException err) {
            try {
                Customerly._Instance.__PING__DeviceJSON.put("app_version", 0);
            } catch (JSONException ignored) { }
        }

        //PREFS
        final SharedPreferences prefs = pApplicationContext.getSharedPreferences(BuildConfig.APPLICATION_ID + ".SharedPreferences", Context.MODE_PRIVATE);
        Customerly._Instance._SharedPreferences = prefs;

        //WIDGET COLOR
        //noinspection SpellCheckingInspection
        Customerly._Instance.__WidgetColor__HardCoded = IU_Utils.getIntSafe(prefs, "CONFIG_HC_WCOLOR", Color.TRANSPARENT);

        Customerly._Instance.__WidgetColor__Fallback =
                Customerly._Instance.__WidgetColor__HardCoded != Color.TRANSPARENT
                        ? Customerly._Instance.__WidgetColor__HardCoded
                        : DEF_WIDGET_COLOR_MALIBU_INT;

        //JWT TOKEN
        Customerly._Instance._JwtToken = IE_JwtToken.from(prefs);

        //PING
        Customerly._Instance.__PING__LAST_widget_color = IU_Utils.getIntSafe(prefs, PREFS_PING_RESPONSE__WIDGET_COLOR, Customerly._Instance.__WidgetColor__Fallback);
        Customerly._Instance.__PING__LAST_powered_by = IU_Utils.getBooleanSafe(prefs, PREFS_PING_RESPONSE__POWERED_BY, false);
        Customerly._Instance.__PING__LAST_welcome_message_users = IU_Utils.getStringSafe(prefs, PREFS_PING_RESPONSE__WELCOME_USERS);
        Customerly._Instance.__PING__LAST_welcome_message_visitors = IU_Utils.getStringSafe(prefs, PREFS_PING_RESPONSE__WELCOME_VISITORS);
        Customerly._Instance.__PING__LAST_active_admins = null;

        Customerly._Instance._AppID = IU_Utils.getStringSafe(prefs, "CONFIG_APP_ID");

        Customerly._Instance.initialized = true;
    }

    /**
     * Call this method to obtain the reference to the Customerly SDK
     * @return The Customerly SDK instance reference
     */
    @NonNull public static Customerly get() {
        if(! Customerly._Instance.initialized) {//Avoid to perform lock if not needed
            synchronized (Customerly.class) {
                if(! Customerly._Instance.initialized) {//After lock we check again to avoid concurrence
                    WeakReference<Activity> activityWeakRef = Customerly._Instance._CurrentActivity;
                    if(activityWeakRef != null) {
                        Activity activity = activityWeakRef.get();
                        if(activity != null) {
                            Customerly._Instance.__init(activity.getApplicationContext());
                        }
                    }
                }
            }
        }
        return Customerly._Instance;
    }

    /**
     * Call this method to configure the SDK indicating the Customerly App ID before accessing it.<br>
     * Call this from your custom Application {@link Application#onCreate()}
     * @param pApplication The application class reference
     * @param pCustomerlyAppID The Customerly App ID found in your Customerly console
     */
    public static void configure(@NonNull Application pApplication, @NonNull String pCustomerlyAppID) {
        Customerly.configure(pApplication, pCustomerlyAppID, Color.TRANSPARENT);
    }

    /**
     * Call this method to configure the SDK indicating the Customerly App ID before accessing it.<br>
     * Call this from your custom Application {@link Application#onCreate()}<br>
     *     <br>
     * You can choose to ignore the widget_color provided by the Customerly web console for the action bar styling in support activities and use an app-local widget_color instead.
     * @param pCustomerlyAppID The Customerly App ID found in your Customerly console
     * @param pWidgetColor The custom widget_color. If Color.TRANSPARENT, it will be ignored
     */
    public static void configure(@NonNull Application pApplication, @NonNull String pCustomerlyAppID, @ColorInt int pWidgetColor) {
        Customerly customerly = Customerly._Instance;
        customerly.__init(pApplication.getApplicationContext());
        final SharedPreferences prefs = customerly._SharedPreferences;
        if(prefs != null) {
            //noinspection SpellCheckingInspection
            prefs.edit().putString("CONFIG_APP_ID", pCustomerlyAppID).putInt("CONFIG_HC_WCOLOR", pWidgetColor).apply();
        }

        customerly._AppID = pCustomerlyAppID.trim();

        customerly.__WidgetColor__HardCoded = pWidgetColor;
        customerly.__PING__LAST_widget_color = Customerly._Instance.__WidgetColor__Fallback =
                pWidgetColor == Color.TRANSPARENT
                        ? DEF_WIDGET_COLOR_MALIBU_INT
                        : pWidgetColor;

        pApplication.registerActivityLifecycleCallbacks(customerly.__ActivityLifecycleCallbacks);
    }

    /**
     * Call this method to enable error logging in the Console.
     * Avoid to enable it in release app versions, the suggestion is to pass your.application.package.BuildConfig.DEBUG as parameter
     * @param pVerboseLogging true for enable logging, please pass your.application.package.BuildConfig.DEBUG
     */
    public void setVerboseLogging(boolean pVerboseLogging) {
        this.__VerboseLogging = pVerboseLogging;
    }

    /*
     * Call this method to force a check for pending Surveys or Message for the current user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     */
    public void update() {
        this.update(null, null);
    }
    /*
     * Call this method to force a check for pending Surveys or Message for the current user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     */
    public void update(@Nullable SuccessCallback pSuccessCallback, @Nullable FailureCallback pFailureCallback) {
        if(this._isConfigured()) {
            try {
                if (System.currentTimeMillis() > this.__PING__next_ping_allowed) {
                    this.__PING__Start(pSuccessCallback, pFailureCallback);
                } else {
                    if(pFailureCallback != null) {
                        pFailureCallback.onFailure();
                    }
                    this._log("You cannot call twice the update so fast. You have to wait " + (this.__PING__next_ping_allowed - System.currentTimeMillis()) / 1000 + " seconds.");
                }
            } catch (Exception generic) {
                if(pFailureCallback != null) {
                    pFailureCallback.onFailure();
                }
                this._log("A generic error occurred in Customerly.update");
                IEr_CustomerlyErrorHandler.sendError(IEr_CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.update", generic);
            }
        } else {
            if(pFailureCallback != null) {
                pFailureCallback.onFailure();
            }
        }
    }

    /**
     * Call this method to link your app user to the Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param email The mail address of the user, this is mandatory
     * @param user_id The optional user_id of the user, null otherwise
     * @param name The optional name of the user, null otherwise
     */
    public void registerUser(@NonNull String email, @Nullable String user_id, @Nullable String name) {
        this.registerUser(email, user_id, name, null, null, null);
    }

    /**
     * Call this method to link your app user to the Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param email The mail address of the user, this is mandatory
     * @param user_id The optional user_id of the user, null otherwise
     * @param name The optional name of the user, null otherwise
     * @param pSuccessCallback To receive success async response
     * @param pFailureCallback To receive failure async response
     */
    public void registerUser(@NonNull String email, @Nullable String user_id, @Nullable String name, @Nullable SuccessCallback pSuccessCallback, @Nullable FailureCallback pFailureCallback) {
        this.registerUser(email, user_id, name, null, pSuccessCallback, pFailureCallback);
    }

    /**
     * Call this method to link your app user to the Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param email The mail address of the user, this is mandatory
     * @param user_id The optional user_id of the user, null otherwise
     * @param name The optional name of the user, null otherwise
     * @param pAttributes Optional attributes for the user in a single depth json (the root cannot contain other JSONObject or JSONArray)
     * @param pSuccessCallback To receive success async response
     * @param pFailureCallback To receive failure async response
     */
    public void registerUser(@NonNull String email, @Nullable String user_id, @Nullable String name, @Nullable JSONObject pAttributes, @Nullable SuccessCallback pSuccessCallback, @Nullable FailureCallback pFailureCallback) {
        final String trimmedEmail = email.trim();
        if(Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            SharedPreferences pref = this._SharedPreferences;
            if (this._isConfigured() && pref != null) {
                try {
                    if (pAttributes != null) {//Check attributes validity
                        JSONArray keys = pAttributes.names();
                        for (int i = 0; i < keys.length(); i++) {
                            try {
                                Object obj = keys.get(i);
                                if (obj instanceof JSONObject || obj instanceof JSONArray) {
                                    if(pFailureCallback != null) {
                                        pFailureCallback.onFailure();
                                    }
                                    this._log("Attributes JSONObject cannot contain JSONArray or JSONObject");
                                    return;
                                }
                            } catch (JSONException ignored) { }
                        }
                    }

                    final String trimmedUserID = user_id == null || user_id.trim().length() == 0 ? null : user_id.trim();

                    new IApi_Request.Builder<Void>(IApi_Request.ENDPOINT_PING)
                            .opt_converter(root -> {
                                //noinspection SpellCheckingInspection
                                pref.edit().putString("regusrml", trimmedEmail).putString("regusrid", trimmedUserID).apply();
                                return this.__PING__response_converter__Message.convert(root);
                            })
                            .opt_receiver((responseState, _void) -> {
                                if (responseState == IApi_Request.RESPONSE_STATE__OK) {
                                    if(pSuccessCallback != null) {
                                        pSuccessCallback.onSuccess();
                                    }
                                } else {
                                    if(pFailureCallback != null) {
                                        pFailureCallback.onFailure();
                                    }
                                }
                            })

                            .param("email", trimmedEmail)
                            .param("user_id", trimmedUserID)
                            .param("name", name == null || name.trim().length() == 0 ? null : name.trim())

                            .param("attributes", pAttributes)

                            .start();
                } catch (Exception generic) {
                    this._log("A generic error occurred in Customerly.registerUser");
                    IEr_CustomerlyErrorHandler.sendError(IEr_CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.registerUser", generic);
                    if(pFailureCallback != null) {
                        pFailureCallback.onFailure();
                    }
                }
            } else {
                if(pFailureCallback != null) {
                    pFailureCallback.onFailure();
                }
            }
        } else {
            this._log("You are trying to register an user passing a not valid email");
            if(pFailureCallback != null) {
                pFailureCallback.onFailure();
            }
        }
    }

    /**
     * Call this method to add new custom attributes to the user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param pAttributes Optional attributes for the user in a single depth json (the root cannot contain other JSONObject or JSONArray)
     * @param pSuccessCallback To receive success async response
     * @param pFailureCallback To receive failure async response
     */
    public void setAttributes(@Nullable JSONObject pAttributes, @Nullable SuccessCallback pSuccessCallback, @Nullable FailureCallback pFailureCallback) {
        if(this._isConfigured()) {
            IE_JwtToken token = this._JwtToken;
            if(token != null && token.isUser()) {
                try {
                    if (pAttributes != null) {//Check attributes validity
                        JSONArray keys = pAttributes.names();
                        for (int i = 0; i < keys.length(); i++) {
                            try {
                                Object obj = keys.get(i);
                                if (obj instanceof JSONObject || obj instanceof JSONArray) {
                                    if(pFailureCallback != null) {
                                        pFailureCallback.onFailure();
                                    }
                                    this._log("Attributes JSONObject cannot contain JSONArray or JSONObject");
                                    return;
                                }
                            } catch (JSONException ignored) { }
                        }
                    }
                    new IApi_Request.Builder<Void>(IApi_Request.ENDPOINT_PING)
                            .opt_converter(this.__PING__response_converter__NaN)
                            .opt_receiver((responseState, _void) -> {
                                if (responseState == IApi_Request.RESPONSE_STATE__OK) {
                                    if(pSuccessCallback != null) {
                                        pSuccessCallback.onSuccess();
                                    }
                                } else {
                                    if(pFailureCallback != null) {
                                        pFailureCallback.onFailure();
                                    }
                                }
                            })
                            .param("attributes", pAttributes)
                            .start();
                } catch (Exception generic) {
                    this._log("A generic error occurred in Customerly.registerUser");
                    IEr_CustomerlyErrorHandler.sendError(IEr_CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.registerUser", generic);
                    if(pFailureCallback != null) {
                        pFailureCallback.onFailure();
                    }
                }
            } else {
                this._log("Cannot setAttributes for lead users");
                if(pFailureCallback != null) {
                    pFailureCallback.onFailure();
                }
            }
        } else {
            if (pFailureCallback != null) {
                pFailureCallback.onFailure();
            }
        }
    }

    /**
     * Call this method to open the Support Activity.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param activity The current activity
     */
    public void openSupport(@NonNull Activity activity) {
        if(this._isConfigured()) {
            try {
                activity.startActivity(new Intent(activity, IAct_List.class));
            } catch (Exception generic) {
                this._log("A generic error occurred in Customerly.openSupport");
                IEr_CustomerlyErrorHandler.sendError(IEr_CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.openSupport", generic);
            }
        }
    }

    /**
     * Call this method to close the user's Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     */
    public void logoutUser() {
        if(this._isConfigured()) {
            try {
                final SharedPreferences prefs = this._SharedPreferences;

                this._JwtToken = null;
                if (prefs != null) {
                    IE_JwtToken.remove(prefs);
                    //noinspection SpellCheckingInspection
                    prefs.edit().remove("regusrml").remove("regusrid").apply();
                }
                this.__SOCKET__disconnect();
                this.__PING__next_ping_allowed = 0L;

                PW_AlertMessage.onUserLogout();
                Activity current = this._CurrentActivity == null ? null : this._CurrentActivity.get();
                if(current != null) {
                    if(current instanceof SDKActivity) {
                        ((SDKActivity) current).onLogoutUser();
                    }
                    IDlgF_Survey.dismiss(current);
                }
                this.__PING__Start(null, null);
            } catch (Exception ignored) { }
        }
    }

    /**
     * Call this method to keep track of custom labelled events.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param pEventName The event custom label
     */
    public void trackEvent(@NonNull String pEventName) {
        pEventName = pEventName.trim();
        if(this._isConfigured() && pEventName.length() != 0) {
            try {
                IE_JwtToken token = this._JwtToken;
                if(token != null && (token.isUser() || token.isLead())) {
                    new IApi_Request.Builder<IE_Message>(IApi_Request.ENDPOINT_EVENT_TRACKING)
                            .opt_trials(2)
                            .param("name", pEventName)
                            .start();
                }
            } catch (Exception generic) {
                this._log("A generic error occurred in Customerly.trackEvent");
                IEr_CustomerlyErrorHandler.sendError(IEr_CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.trackEvent", generic);
            }
        }
    }
}
