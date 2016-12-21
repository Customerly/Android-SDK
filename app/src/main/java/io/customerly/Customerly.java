package io.customerly;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.text.Spannable;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Created by Gianni on 04/06/16.
 * Project: CustomerlySDK
 */
public class Customerly {

    /* ****************************************************************************************************************************************************************/
    /* ************************************************************************************************************************************************ Finals ********/
    /* ****************************************************************************************************************************************************************/
    private static final String PREFS__COOKIES_customerly_cookies = "PREFS__COOKIES_customerly_cookies";
    private static final String PREFS_PINGRESPONSE__WIDGET_COLOR = "PREFS_PINGRESPONSE__WIDGET_COLOR";
    private static final String PREFS_PINGRESPONSE__POWEREDBY = "PREFS_PINGRESPONSE__POWEREDBY";
    private static final String PREFS_PINGRESPONSE__WELCOME_USERS = "PREFS_PINGRESPONSE__WELCOME_USERS";
    private static final String PREFS_PINGRESPONSE__WELCOME_VISITORS = "PREFS_PINGRESPONSE__WELCOME_VISITORS";
    private static final long SOCKET_PING_INTERVAL = 60000;
    private static final String SOCKET_EVENT__PING = "p";
    private static final String SOCKET_EVENT__PINGACTIVE = "a";
    private static final String SOCKET_EVENT__TYPING = "typing";
    private static final String SOCKET_EVENT__SEEN = "seen";
    private static final String SOCKET_EVENT__MESSAGE = "message";
    private static final String SOCKET_EVENT__KEY__conversation = "conversation";
    private static final String SOCKET_EVENT__KEY__conversation_id = "conversation_id";
    private static final String SOCKET_EVENT__KEY__conversation_message_id = "conversation_message_id";
    private static final String SOCKET_EVENT__KEY__is_note = "is_note";
    private static final String SOCKET_EVENT__KEY__is_typing = "is_typing";
    private static final String SOCKET_EVENT__KEY__seen_date = "seen_date";
    private static final String SOCKET_EVENT__KEY__timestamp = "timestamp";
    private static final String SOCKET_EVENT__KEY__user_id = "user_id";
    @NonNull
    final Internal_Utils__RemoteImageHandler _RemoteImageHandler = new Internal_Utils__RemoteImageHandler();
    @NonNull private final Handler _Handler = new Handler();
    @ColorInt private static final int DEF_WIDGETCOLOR_INT = 0xffd60022;

    /* ****************************************************************************************************************************************************************/
    /* ************************************************************************************************************************************************ State fields **/
    /* ****************************************************************************************************************************************************************/

    //Diventano NonNull con la configure
    long _ApplicationVersionCode;
    @ColorInt private int __Fallback_Widget_color = DEF_WIDGETCOLOR_INT, __InApp_Hardcoded_WidgetColor = Color.TRANSPARENT;
    @SuppressWarnings("NullableProblems") @NonNull String _AppID, _ApplicationName;
    @SuppressWarnings("NullableProblems") @NonNull private SharedPreferences _SharedPreferences;
    @SuppressWarnings("NullableProblems") @NonNull private JSONObject cookies;
    @NonNull private WeakReference<Activity> _ActiveActivity = new WeakReference<>(null);

    void setCurrentActivity(@Nullable Activity activity) {
        if(activity == null) {
            this._ActiveActivity.clear();
        } else {
            this._ActiveActivity = new WeakReference<>(activity);
        }
    }

    private boolean __VerboseLogging = true;

    @Nullable private Customerly_User customerly_user;
    @Nullable private Socket _Socket;

    private long __PING__next_ping_allowed = 0L;
    @ColorInt int __PING__LAST_widget_color;
    boolean __PING__LAST_powered_by;
    @Nullable private String __PING__LAST_welcome_message_users, __PING__LAST_welcome_message_visitors;
    @Nullable Internal_entity__Admin[] __PING__LAST_active_admins;
    long __PING__LAST_message_conversation_id = 0;
    private long __PING__LAST_message_account_id = 0;
    @Nullable Internal_entity__Survey[] __PING__LAST_surveys;

    @Nullable private RealTimeMessagesCallback __SOCKET__RealTimeMessagesCallback;

    /* ****************************************************************************************************************************************************************/
    /* ************************************************************************************************************************************************ Singleton *****/
    /* ****************************************************************************************************************************************************************/
    @NonNull static final Customerly _Instance = new Customerly();

    private Customerly() { super(); }

    @Contract(pure = true)
    @NonNull public static Customerly get() {
        return Customerly._Instance;
    }

    /* ****************************************************************************************************************************************************************/
    /* ************************************************************************************************************************************************ Initializer ***/
    /* ****************************************************************************************************************************************************************/
    /**
     * @param pApplicationContext The Application Context
     * @param pCustomerlyAppID The appid found in your Customerly console
     * @param pWidgetColor if Color.TRANSPARENT, it will be ignored
     */
    public void configure(@NonNull Application pApplicationContext, @NonNull String pCustomerlyAppID, @ColorInt int pWidgetColor) {
        this.__InApp_Hardcoded_WidgetColor = pWidgetColor;
        this.configure(pApplicationContext, pCustomerlyAppID);
    }
    /**
     * @param pApplicationContext The Application Context
     * @param pCustomerlyAppID The appid found in your Customerly console
     */
    public void configure(@NonNull Application pApplicationContext, @NonNull String pCustomerlyAppID) {

        this._AppID = pCustomerlyAppID;
        try {
            this._ApplicationName = pApplicationContext.getApplicationInfo().loadLabel(pApplicationContext.getPackageManager()).toString();
        } catch (NullPointerException err) {
            this._ApplicationName = "<Error retrieving the app name>";
        }
        try {
            this._ApplicationVersionCode = pApplicationContext.getPackageManager().getPackageInfo(pApplicationContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
            this._ApplicationVersionCode = 0;
        }
        this._SharedPreferences = pApplicationContext.getSharedPreferences(BuildConfig.APPLICATION_ID + ".SharedPreferences", Context.MODE_PRIVATE);

        if(this.__InApp_Hardcoded_WidgetColor == Color.TRANSPARENT) {
            @ColorInt int color;
            try {
                int colorAttr;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    colorAttr = android.R.attr.colorPrimary;
                } else {
                    //Get colorPrimary defined for AppCompat
                    colorAttr = pApplicationContext.getResources().getIdentifier("colorPrimary", "attr", pApplicationContext.getPackageName());
                }
                TypedValue outValue = new TypedValue();
                pApplicationContext.getTheme().resolveAttribute(colorAttr, outValue, true);
                color = outValue.data;
            } catch (Exception some_error) {
                color = DEF_WIDGETCOLOR_INT;
            }

            this.__PING__LAST_widget_color = this.__Fallback_Widget_color = color;

        } else {
            this.__PING__LAST_widget_color = this.__Fallback_Widget_color = this.__InApp_Hardcoded_WidgetColor;
        }

        this.__USER__onNewUser(Customerly_User.from(this._SharedPreferences));

        this.cookies = this.__COOKIES__restoreFromDisk();

        this.__PING__restoreFromDisk(this._SharedPreferences);

        this.update((isSuccess, newSurvey, newMessage) -> {});
    }

    boolean _isConfigured() {
        //noinspection ConstantConditions
        if(this._AppID == null) {
            this._log("You need to configure the SDK ");
            Internal_errorhandler__CustomerlyErrorHandler.sendNotConfiguredError();
            return false;
        } else {
            return true;
        }
    }

    /* ****************************************************************************************************************************************************************/
    /* ************************************************************************************************************************************************ Utility *******/
    /* ****************************************************************************************************************************************************************/
    public void setVerboseLogging(boolean pVerboseLogging) {
        this.__VerboseLogging = pVerboseLogging;
    }
    void _log(@NonNull String pLogMessage) {
        if(this.__VerboseLogging) {
            Log.v(BuildConfig.CUSTOMERLY_SDK_NAME, pLogMessage);
        }
    }
    /* ****************************************************************************************************************************************************************/
    /* ************************************************************************************************************************************************ Ping Logic ****/
    /* ****************************************************************************************************************************************************************/
    private void __PING__onPingResult(@Nullable JSONObject data) {
        if(data != null) {
            JSONObject obj;

            if((obj = data.optJSONObject("apps")) != null
                    && (obj = obj.optJSONObject("app_config")) != null) {
                if(this.__InApp_Hardcoded_WidgetColor == Color.TRANSPARENT) {
                    String pingWidgetColor = obj.optString("widget_color", null);
                    if (pingWidgetColor != null && pingWidgetColor.length() != 0) {
                        if (pingWidgetColor.charAt(0) != '#') {
                            pingWidgetColor = '#' + pingWidgetColor;
                        }
                        try {
                            this.__PING__LAST_widget_color = Color.parseColor(pingWidgetColor);
                        } catch (IllegalArgumentException notCorrectColor) {
                            Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__HTTP_RESPONSE_ERROR, String.format("PingResponse:data.apps.app_config.widget_color is an invalid argb color: '%s'", pingWidgetColor), notCorrectColor);
                            this.__PING__LAST_widget_color = this.__Fallback_Widget_color;
                        }
                    }
                }
                this.__PING__LAST_powered_by = 1 == obj.optLong("powered_by", 0);
                this.__PING__LAST_welcome_message_users = obj.optString("welcome_message_users", null);
                this.__PING__LAST_welcome_message_visitors = obj.optString("welcome_message_visitors", null);
            } else {
                this.__PING__LAST_widget_color = this.__Fallback_Widget_color;
                this.__PING__LAST_powered_by = false;
                this.__PING__LAST_welcome_message_users = null;
                this.__PING__LAST_welcome_message_visitors = null;
            }

            this.__PING__LAST_active_admins = Internal_entity__Admin.from(data.optJSONArray("active_admins"));

            JSONArray last_messages_array = data.optJSONArray("last_messages");
            this.__PING__LAST_message_conversation_id = 0;
            this.__PING__LAST_message_account_id = 0;
            if(last_messages_array != null && last_messages_array.length() != 0) {
                JSONObject message;
                for (int i = 0; i < last_messages_array.length(); i++) {
                    try {
                        message = last_messages_array.getJSONObject(i);
                        if (message == null)
                            continue;
                        this.__PING__LAST_message_conversation_id = message.optLong("conversation_id");
                        this.__PING__LAST_message_account_id = message.optLong("account_id");
                        break;
                    } catch (JSONException ignored) { }
                }
            }

            this.__PING__LAST_surveys = Internal_entity__Survey.from(data.optJSONArray("last_surveys"));

        } else {
            this.__PING__LAST_widget_color = this.__Fallback_Widget_color;
            this.__PING__LAST_powered_by = false;
            this.__PING__LAST_welcome_message_users = null;
            this.__PING__LAST_welcome_message_visitors = null;
            this.__PING__LAST_active_admins = null;
            this.__PING__LAST_message_conversation_id = 0;
            this.__PING__LAST_message_account_id = 0;
            this.__PING__LAST_surveys = null;
        }

        this._SharedPreferences.edit()
                .putInt(PREFS_PINGRESPONSE__WIDGET_COLOR, this.__PING__LAST_widget_color)
                .putBoolean(PREFS_PINGRESPONSE__POWEREDBY, this.__PING__LAST_powered_by)
                .putString(PREFS_PINGRESPONSE__WELCOME_USERS, this.__PING__LAST_welcome_message_users)
                .putString(PREFS_PINGRESPONSE__WELCOME_VISITORS, this.__PING__LAST_welcome_message_visitors)
                .apply();
    }

    private void __PING__restoreFromDisk(@NonNull SharedPreferences pSharedPreferences) {
        this.__PING__LAST_widget_color = pSharedPreferences.getInt(PREFS_PINGRESPONSE__WIDGET_COLOR, this.__Fallback_Widget_color);
        this.__PING__LAST_powered_by = pSharedPreferences.getBoolean(PREFS_PINGRESPONSE__POWEREDBY, false);
        this.__PING__LAST_welcome_message_users = pSharedPreferences.getString(PREFS_PINGRESPONSE__WELCOME_USERS, null);
        this.__PING__LAST_welcome_message_visitors = pSharedPreferences.getString(PREFS_PINGRESPONSE__WELCOME_VISITORS, null);
        this.__PING__LAST_active_admins = null;
        this.__PING__LAST_message_conversation_id = 0;
        this.__PING__LAST_message_account_id = 0;
        this.__PING__LAST_surveys = null;
    }

    /* ****************************************************************************************************************************************************************/
    /* ************************************************************************************************************************************************ Welcome Msg. **/
    /* ****************************************************************************************************************************************************************/
    @Nullable Spanned __WELCOME__getMessage() {
        return this._isConfigured()
                ? Internal_Utils__Utils.decodeHtmlStringWithEmojiTag(this.customerly_user == null ? this.__PING__LAST_welcome_message_visitors : this.__PING__LAST_welcome_message_users)
                : null;
    }

    /* ****************************************************************************************************************************************************************/
    /* ************************************************************************************************************************************************ Cookies *******/
    /* ****************************************************************************************************************************************************************/
    @NonNull JSONObject __COOKIES__get() { return this.cookies;   }
    @NonNull private JSONObject __COOKIES__restoreFromDisk() {
        try {
            return new JSONObject(this._SharedPreferences.getString(PREFS__COOKIES_customerly_cookies, "{}"));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
    void __COOKIES__update(@Nullable JSONObject pLastCookies) {
        if(pLastCookies != null) {
            Iterator<String> keys = pLastCookies.keys();
            while(keys.hasNext()) {
                try {
                    String key = keys.next();
                    JSONObject cookie_values = pLastCookies.getJSONObject(key);
                    String value = cookie_values.optString("value", null);
                    if(cookie_values.optInt("expire", 0) == 1) {
                        this.cookies.remove(key);
                    } else {
                        try {
                            this.cookies.put(key, value);
                        } catch (JSONException ignored) { }
                    }
                    this._SharedPreferences.edit()
                            .putString(PREFS__COOKIES_customerly_cookies, this.cookies.toString())
                            .apply();
                } catch (JSONException ignored) { }
            }
        }
    }
    private void __COOKIES__delete() {
        this.cookies = new JSONObject();
        this._SharedPreferences.edit()
                .remove(PREFS__COOKIES_customerly_cookies)
                .apply();
    }

    /* ****************************************************************************************************************************************************************/
    /* ************************************************************************************************************************************************ User **********/
    /* ****************************************************************************************************************************************************************/
    void __USER__onNewUser(@Nullable Customerly_User user) {
        if(user != null && ! user.equals(this.customerly_user)) {
            this.customerly_user = user;
            user.store(this._SharedPreferences);
        }
    }
    private void __USER__delete() {
        Customerly_User user = this.customerly_user;
        if(user != null) {
            this.customerly_user = null;
            user.delete(this._SharedPreferences);
        }
    }
    @Nullable Customerly_User __USER__get() {
        return this.customerly_user;
    }

    private void registerUser(@NonNull Customerly_User pUser, @Nullable JSONObject pAttributes, @NonNull Callback pCallback) {
        if(this._isConfigured()) {
            this.__USER__onNewUser(pUser);
            if(this.__ATTRIBUTES__check_and_setPending(pAttributes, pCallback)) {
                this.update(pCallback);
            }
        }
    }

    /* ****************************************************************************************************************************************************************/
    /* ************************************************************************************************************************************************ Attribute *****/
    /* ****************************************************************************************************************************************************************/
    @Nullable JSONObject __ATTRIBUTES_pending;

    @Contract("null, _ -> true")
    private boolean __ATTRIBUTES__check_and_setPending(@Nullable JSONObject pAttributes, @NonNull Callback pCallback) {
        if(pAttributes != null) {
            JSONArray keys = pAttributes.names();
            for(int i = 0; i < keys.length(); i++) {
                try {
                    Object obj = keys.get(i);
                    if(obj instanceof JSONObject || obj instanceof  JSONArray) {
                        pCallback.onResponse(false, false, false);
                        return false;
                    }
                } catch (JSONException ignored) { }
            }
            this.__ATTRIBUTES_pending = pAttributes;
        }
        return true;
    }

    /* ****************************************************************************************************************************************************************/
    /* ************************************************************************************************************************************************ Socket ********/
    /* ****************************************************************************************************************************************************************/
    interface __SOCKET__ITyping_listener {   void onTypingEvent(long pConversationID, boolean pTyping);   }
    interface __SOCKET__IMessage_listener {   void onMessageEvent(@NonNull ArrayList<Internal_entity__Message> news);   }
    @Nullable private __SOCKET__ITyping_listener __SOCKET__Typing_listener = null;
    @Nullable private __SOCKET__IMessage_listener __SOCKET__Message_listener = null;
    @Nullable private String __SOCKET__Endpoint = null, __SOCKET__Port = null;
    @Nullable private String __SOCKET__CurrentConfiguration = null;
    @NonNull private final Runnable __SOCKET__ping = () -> {
        Socket socket = this._Socket;
        if(socket != null && socket.connected()) {
            Activity activity = this._ActiveActivity.get();
            socket.emit(activity instanceof Internal_activity__CustomerlyChat_Activity
                    || activity instanceof Internal_activity__CustomerlyList_Activity
                    ? SOCKET_EVENT__PINGACTIVE : SOCKET_EVENT__PING);
            this._Handler.postDelayed(this.__SOCKET__ping, SOCKET_PING_INTERVAL);
        }
    };
    void __SOCKET_setEndpoint(@Nullable String endpoint, @Nullable String port) {
        if(endpoint != null && port != null) {
            this.__SOCKET__Endpoint = endpoint;
            this.__SOCKET__Port = port;
        }
    }
    void __SOCKET__connect() {
        if(this.__SOCKET__Endpoint != null && this.__SOCKET__Port != null) {
            Customerly_User user = this.customerly_user;
            if (user != null && user.internal_user_id != 0) {
                if(this.__SOCKET__CurrentConfiguration == null || ! this.__SOCKET__CurrentConfiguration.equals(String.format(Locale.UK, "%s-%s-%d", this.__SOCKET__Endpoint, this.__SOCKET__Port, user.internal_user_id))) {

                    this.__SOCKET__disconnect();
                    this.__SOCKET__CurrentConfiguration = String.format(Locale.UK, "%s-%s-%d", this.__SOCKET__Endpoint, this.__SOCKET__Port, user.internal_user_id);

                    String query;
                    try {
                        query = "json=" + new JSONObject().put("nsp", "user").put("app", this._AppID).put("id", user.internal_user_id).toString();
                    } catch (JSONException errore) {
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
                                    try {
                                        this._log(String.format("SOCKET RX: %1$s -> %2$s", SOCKET_EVENT__TYPING, payloadJson.toString(1)));
                                    } catch (JSONException ignored) { }
                                    boolean is_typing = "y".equals(payloadJson.optString(SOCKET_EVENT__KEY__is_typing));
                                    payloadJson = payloadJson.getJSONObject(SOCKET_EVENT__KEY__conversation);
                                    if (payloadJson != null) {
                                        Customerly_User usr = this.customerly_user;
                                        if (usr != null && usr.internal_user_id == payloadJson.getLong(SOCKET_EVENT__KEY__user_id) && !payloadJson.optBoolean(SOCKET_EVENT__KEY__is_note, false)) {
                                            long conversation_id = payloadJson.optLong(SOCKET_EVENT__KEY__conversation_id, 0);
                                            __SOCKET__ITyping_listener listener = this.__SOCKET__Typing_listener;
                                            if (conversation_id != 0 && listener != null) {
                                                listener.onTypingEvent(conversation_id, is_typing);
                                            }
                                        }
                                    }
                                } catch (JSONException ignored) {
                                }
                            }
                        });
                        socket.on(SOCKET_EVENT__MESSAGE, payload -> {
                            if (payload.length != 0) {
                                try {
                                    JSONObject payloadJson = (JSONObject) payload[0];
                                    try {
                                        this._log(String.format("SOCKET RX: %1$s -> %2$s", SOCKET_EVENT__MESSAGE, payloadJson.toString(1)));
                                    } catch (JSONException ignored) { }
                                    long timestamp = payloadJson.optLong(SOCKET_EVENT__KEY__timestamp);
                                    long crm_user_id = payloadJson.optLong(SOCKET_EVENT__KEY__user_id);
                                    Customerly_User usr = this.customerly_user;

                                    if (timestamp != 0 && crm_user_id != 0 && crm_user_id == usr.internal_user_id
                                            && !payloadJson.getJSONObject(SOCKET_EVENT__KEY__conversation).optBoolean(SOCKET_EVENT__KEY__is_note, false)) {
                                        final __SOCKET__IMessage_listener listener = this.__SOCKET__Message_listener;
                                        final RealTimeMessagesCallback rtcCallback = this.__SOCKET__RealTimeMessagesCallback;
                                        if (listener != null || rtcCallback != null) {
                                            new Internal_api__CustomerlyRequest.Builder<ArrayList<Internal_entity__Message>>(Internal_api__CustomerlyRequest.ENDPOINT_MESSAGENEWS)
                                                    .opt_converter(data -> Internal_Utils__Utils.fromJSONdataToList(data, "messages", Internal_entity__Message::new))
                                                    .opt_receiver((responseState, newsResponse) -> {
                                                        if (responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK && newsResponse != null) {
                                                            if(listener != null) {
                                                                listener.onMessageEvent(newsResponse);
                                                            } else if(/*rtcCallback != null && */newsResponse.size() != 0) {
                                                                Internal_entity__Message last_message = newsResponse.get(0);
                                                                this.__PING__LAST_message_conversation_id = last_message.conversation_id;
                                                                this.__PING__LAST_message_account_id = last_message.assigner_id;
                                                                rtcCallback.onMessage(last_message.content);
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
                        this._Handler.postDelayed(this.__SOCKET__ping, SOCKET_PING_INTERVAL);
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
        }
        this._Handler.removeCallbacks(this.__SOCKET__ping);
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
    void __SOCKET_RECEIVE_Typing(@Nullable __SOCKET__ITyping_listener p__SOCKET__Typing_listener) {
        this.__SOCKET__Typing_listener = p__SOCKET__Typing_listener;
    }
    void __SOCKET_RECEIVE_Message(@Nullable __SOCKET__IMessage_listener p__SOCKET__Message_listener) {
        this.__SOCKET__Message_listener = p__SOCKET__Message_listener;
    }
    void __SOCKET_SEND_Typing(long pDestinationUserID, long pConversationID, boolean pTyping) {
        try {
            this.__SOCKET__SEND(SOCKET_EVENT__TYPING, new JSONObject()
                    .put(SOCKET_EVENT__KEY__conversation, new JSONObject()
                            .put(SOCKET_EVENT__KEY__conversation_id, pConversationID)
                            .put(SOCKET_EVENT__KEY__user_id, pDestinationUserID)
                            .put(SOCKET_EVENT__KEY__is_note, false))
                    .put(SOCKET_EVENT__KEY__is_typing, pTyping ? "y" : "n"));
        } catch (JSONException ignored) { }
    }
    void __SOCKET_SEND_Message(long pDestinationUserID, long pTimestamp) {
        try {
            this.__SOCKET__SEND(SOCKET_EVENT__MESSAGE, new JSONObject()
                    .put(SOCKET_EVENT__KEY__timestamp, pTimestamp)
                    .put(SOCKET_EVENT__KEY__user_id, pDestinationUserID)
                    .put(SOCKET_EVENT__KEY__conversation, new JSONObject()
                            .put(SOCKET_EVENT__KEY__is_note, false)));
        } catch (JSONException ignored) { }

    }
    void __SOCKET_SEND_Seen(long pDestinationUserID, long pConversationMessageID, long pSeenDate) {
        try {
            this.__SOCKET__SEND(SOCKET_EVENT__SEEN, new JSONObject()
                    .put(SOCKET_EVENT__KEY__conversation, new JSONObject()
                            .put(SOCKET_EVENT__KEY__conversation_message_id, pConversationMessageID)
                            .put(SOCKET_EVENT__KEY__user_id, pDestinationUserID))
                    .put(SOCKET_EVENT__KEY__seen_date, pSeenDate));
        } catch (JSONException ignored) { }
    }

    public interface Callback {
        void onResponse(boolean isSuccess, boolean newSurvey, boolean newMessage);
    }
    public void update(@NonNull Callback pCallback) {
        if(System.currentTimeMillis() < this.__PING__next_ping_allowed) {
            this._log("You cannot call twice the update so fast. You have to wait " + (this.__PING__next_ping_allowed - System.currentTimeMillis()) /1000 + " seconds.");
            Internal_entity__Survey[] surveys = this.__PING__LAST_surveys;
            pCallback.onResponse(false, surveys != null && surveys.length != 0, this.__PING__LAST_message_conversation_id != 0);
        } else {
            new Internal_api__CustomerlyRequest.Builder<Void>(Internal_api__CustomerlyRequest.ENDPOINT_PINGINDEX)
                    .opt_converter(data -> {
                        this.__PING__next_ping_allowed = data.optLong("next-ping-allowed", 0);
                        this.__PING__onPingResult(data);
                        return null;
                    })
                    .opt_receiver((responseState, _void) -> {
                        Internal_entity__Survey[] surveys = this.__PING__LAST_surveys;
                        pCallback.onResponse(responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK, surveys != null && surveys.length != 0, this.__PING__LAST_message_conversation_id != 0);
                    })
                    .start();
        }
    }

    public void registerUser(String user_id, @NonNull String email, @Nullable String name, @NonNull Callback pCallback) {
        this.registerUser(user_id, email, name, null, pCallback);
    }

    public void registerUser(String user_id, @NonNull String email, @Nullable String name, @Nullable JSONObject pAttributes, @NonNull Callback pCallback) {
        this.registerUser(new Customerly_User(true, Customerly_User.UNKNOWN_CUSTOMERLY_USER_ID, user_id, email, name), pAttributes, pCallback);
    }

    public void setAttributes(@Nullable JSONObject pAttributes, @NonNull Callback pCallback) {
        if(this._isConfigured()) {
            if(this.__ATTRIBUTES__check_and_setPending(pAttributes, pCallback)) {
                this.__PING__next_ping_allowed = 0L;
                this.update(pCallback);
            }
        }
    }

    public void openSupport(@NonNull Activity activity) {
        if(this._isConfigured()) {
            activity.startActivity(new Intent(activity, Internal_activity__CustomerlyList_Activity.class));
        }
    }

    public void openLastSupportConversation(@NonNull Activity activity) {
        if(this._isConfigured()) {
            long lastMessage_ConversationID = this.__PING__LAST_message_conversation_id;
            long lastMessage_AssignerID = this.__PING__LAST_message_account_id;
            activity.startActivity(new Intent(activity, Internal_activity__CustomerlyList_Activity.class)
                .putExtra(Internal_activity__CustomerlyList_Activity.EXTRA_OPEN_CONVERSATION__CONVERSATION_ID, lastMessage_ConversationID)
                .putExtra(Internal_activity__CustomerlyList_Activity.EXTRA_OPEN_CONVERSATION__ASSIGNER_ID, lastMessage_AssignerID));
        }
    }

    public void logoutUser() {
        if(this._isConfigured()) {
            this.__USER__delete();
            this.__ATTRIBUTES_pending = null;
            this.__COOKIES__delete();
            this.__SOCKET__disconnect();
        }
    }

    public void trackEvent(@NonNull String pEventName) {
        new Internal_api__CustomerlyRequest.Builder<Internal_entity__Message>(Internal_api__CustomerlyRequest.ENDPOINT_EVENTTRACKING)
                .opt_trials(2)
                .param("name", pEventName)
                .start();
    }

    @SuppressLint("CommitTransaction")
    public void openSurvey(@NonNull FragmentManager fm) {
        Internal_entity__Survey[] surveys = this.__PING__LAST_surveys;
        if(surveys != null) {
            for (Internal_entity__Survey survey : surveys) {
                if (survey != null && !survey.isRejected) {
                    new Internal_dialogfragment__Survey_DialogFragment().show(fm.beginTransaction().addToBackStack(null), "SURVEYS");
                    return;
                }
            }
        }
        this._log("No surveys available");
    }

    public interface RealTimeMessagesCallback {
        void onMessage(Spannable messageContent);
    }

    public void registerRealTimeMessagesCallback(@Nullable RealTimeMessagesCallback pRealTimeMessagesCallback) {
        this.__SOCKET__RealTimeMessagesCallback = pRealTimeMessagesCallback;
    }

}
