package io.customerly;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.util.Log;
import android.util.TypedValue;

import org.intellij.lang.annotations.Subst;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Created by Gianni on 04/06/16.
 * Project: CustomerlySDK
 */
public class Customerly {

    private static final String PREFS_PING_RESPONSE__APP_NAME = "PREFS_PING_RESPONSE__APP_NAME";
    private static final String PREFS_PING_RESPONSE__WIDGET_COLOR = "PREFS_PING_RESPONSE__WIDGET_COLOR";
    private static final String PREFS_PING_RESPONSE__POWERED_BY = "PREFS_PING_RESPONSE__POWERED_BY";
    private static final String PREFS_PING_RESPONSE__WELCOME_USERS = "PREFS_PING_RESPONSE__WELCOME_USERS";
    private static final String PREFS_PING_RESPONSE__WELCOME_VISITORS = "PREFS_PING_RESPONSE__WELCOME_VISITORS";
    private static final long SOCKET_PING_INTERVAL = 60000;
    private static final String SOCKET_EVENT__PING = "p";
    private static final String SOCKET_EVENT__PING_ACTIVE = "a";
    private static final String SOCKET_EVENT__TYPING = "typing";
    private static final String SOCKET_EVENT__SEEN = "seen";
    private static final String SOCKET_EVENT__MESSAGE = "message";
    @ColorInt private static final int DEF_WIDGET_COLOR_INT = 0xffd60022;

    @NonNull final Internal_Utils__RemoteImageHandler _RemoteImageHandler = new Internal_Utils__RemoteImageHandler();
    @NonNull private final Handler __SOCKET_PingHandler = new Handler();

    private boolean initialized = false;
    @Nullable SharedPreferences _SharedPreferences;
    long _ApplicationVersionCode;
    @Nullable String _AppID, _ApplicationName, _AppCacheDir;
    @ColorInt private int
            __WidgetColor__fromTheme = DEF_WIDGET_COLOR_INT,
            __WidgetColor__Fallback = DEF_WIDGET_COLOR_INT,
            __WidgetColor__HardCoded = Color.TRANSPARENT;
    @Nullable Internal__jwt_token _JWTtoken;

    @Nullable Class<? extends Activity> _CurrentActivityClass = null;

    private boolean __VerboseLogging = true;

    @Nullable private Socket _Socket;

    private long __PING__next_ping_allowed = 0L;
    @Nullable String __PING__LAST_app_name;
    @ColorInt int __PING__LAST_widget_color;
    boolean __PING__LAST_powered_by;
    @Nullable private String __PING__LAST_welcome_message_users, __PING__LAST_welcome_message_visitors;
    @Nullable Internal_entity__Admin[] __PING__LAST_active_admins;
    long __PING__LAST_message_conversation_id = 0L;
    @Nullable Internal_entity__Survey[] __PING__LAST_surveys;

    @NonNull private final Internal_api__CustomerlyRequest.ResponseConverter<Void> __PING__response_converter = root -> {
        this.__PING__next_ping_allowed = root.optLong("next-ping-allowed", 0);

        JSONObject websocket = root.optJSONObject("websocket");
        if (websocket != null) {
                                /*  "websocket": {
                                      "endpoint": "https://ws2.customerly.io",
                                      "port": "8080"  }  */
            Customerly._Instance.__SOCKET_setEndpoint(Internal_Utils__Utils.jsonOptStringWithNullCheck(websocket, "endpoint"), Internal_Utils__Utils.jsonOptStringWithNullCheck(websocket, "port"));
        }
        Customerly._Instance.__SOCKET__connect();

        JSONObject app_config = root.optJSONObject("app");
        if(app_config != null) {
            String app_name = Internal_Utils__Utils.jsonOptStringWithNullCheck(app_config, "name");
            if(app_name != null) {
                this.__PING__LAST_app_name = app_name;
            }
        }

        app_config = root.optJSONObject("app_config");

        if(app_config != null) {
            if(this.__WidgetColor__HardCoded == Color.TRANSPARENT) {
                String pingWidgetColor = Internal_Utils__Utils.jsonOptStringWithNullCheck(app_config, "widget_color");
                if (pingWidgetColor != null && pingWidgetColor.length() != 0) {
                    if (pingWidgetColor.charAt(0) != '#') {
                        pingWidgetColor = '#' + pingWidgetColor;
                    }
                    try {
                        this.__PING__LAST_widget_color = Color.parseColor(pingWidgetColor);
                    } catch (IllegalArgumentException notCorrectColor) {
                        Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__HTTP_RESPONSE_ERROR, String.format("PingResponse:data.apps.app_config.widget_color is an invalid argb color: '%s'", pingWidgetColor), notCorrectColor);
                        this.__PING__LAST_widget_color = this.__WidgetColor__Fallback;
                    }
                }
            }
            this.__PING__LAST_powered_by = 1 == app_config.optLong("powered_by", 0);
            this.__PING__LAST_welcome_message_users = Internal_Utils__Utils.jsonOptStringWithNullCheck(app_config, "welcome_message_users");
            this.__PING__LAST_welcome_message_visitors = Internal_Utils__Utils.jsonOptStringWithNullCheck(app_config, "welcome_message_visitors");
        } else {
            this.__PING__LAST_widget_color = this.__WidgetColor__Fallback;
            this.__PING__LAST_powered_by = false;
            this.__PING__LAST_welcome_message_users = null;
            this.__PING__LAST_welcome_message_visitors = null;
        }

        this.__PING__LAST_active_admins = Internal_entity__Admin.from(root.optJSONArray("active_admins"));

        JSONArray last_messages_array = root.optJSONArray("last_messages");
        this.__PING__LAST_message_conversation_id = 0;
        if(last_messages_array != null && last_messages_array.length() != 0) {
            JSONObject message;
            for (int i = 0; i < last_messages_array.length(); i++) {
                try {
                    message = last_messages_array.getJSONObject(i);
                    if (message == null)
                        continue;
                    this.__PING__LAST_message_conversation_id = message.optLong("conversation_id");
                    break;
                } catch (JSONException ignored) { }
            }
        }

        this.__PING__LAST_surveys = Internal_entity__Survey.from(root.optJSONArray("last_surveys"));

        final SharedPreferences prefs = this._SharedPreferences;
        if(prefs != null) {
            prefs.edit()
                    .putString(PREFS_PING_RESPONSE__APP_NAME, this.__PING__LAST_app_name)
                    .putInt(PREFS_PING_RESPONSE__WIDGET_COLOR, this.__PING__LAST_widget_color)
                    .putBoolean(PREFS_PING_RESPONSE__POWERED_BY, this.__PING__LAST_powered_by)
                    .putString(PREFS_PING_RESPONSE__WELCOME_USERS, this.__PING__LAST_welcome_message_users)
                    .putString(PREFS_PING_RESPONSE__WELCOME_VISITORS, this.__PING__LAST_welcome_message_visitors)
                    .apply();
        }

        return null;
    };

    @NonNull static final Customerly _Instance = new Customerly();

    private Customerly() { super(); }

    boolean _isConfigured() {
        if(this._AppID == null) {
            this._log("You need to configure the SDK ");
            Internal_errorhandler__CustomerlyErrorHandler.sendNotConfiguredError();
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

    @Nullable CustomerlyHtmlMessage _WELCOME__getMessage() {
        Internal__jwt_token token = this._JWTtoken;
        return this._isConfigured()
                ? Internal_Utils__Utils.decodeHtmlStringWithEmojiTag(token != null && token.isUser() ? this.__PING__LAST_welcome_message_users : this.__PING__LAST_welcome_message_visitors)
                : null;
    }

    interface __SOCKET__ITyping_listener {   void onTypingEvent(long pConversationID, long account_id, boolean pTyping);   }
    interface __SOCKET__IMessage_listener {   void onMessageEvent(@NonNull ArrayList<Internal_entity__Message> news);   }
    @Nullable __SOCKET__ITyping_listener __SOCKET__Typing_listener = null;
    @Nullable __SOCKET__IMessage_listener __SOCKET__Message_listener = null;
    @Nullable private RealTimeMessagesCallback __SOCKET__RealTimeMessagesCallback = null;
    @Nullable private String __SOCKET__Endpoint = null, __SOCKET__Port = null;
    @Nullable private String __SOCKET__CurrentConfiguration = null;
    @NonNull private final Runnable __SOCKET__ping = () -> {
        Socket socket = this._Socket;
        if(socket != null && socket.connected()) {
            Class<? extends Activity> currentActivityClass = this._CurrentActivityClass;
            socket.emit(currentActivityClass != null && (currentActivityClass == Internal_activity__CustomerlyChat_Activity.class
                    || currentActivityClass == Internal_activity__CustomerlyList_Activity.class)
                    ? SOCKET_EVENT__PING_ACTIVE

                    : SOCKET_EVENT__PING);
            this.__SOCKET_PingHandler.postDelayed(this.__SOCKET__ping, SOCKET_PING_INTERVAL);
        }
    };
    private void __SOCKET_setEndpoint(@Nullable String endpoint, @Nullable String port) {
        if(endpoint != null && port != null) {
            this.__SOCKET__Endpoint = endpoint;
            this.__SOCKET__Port = port;
        }
    }
    private void __SOCKET__connect() {
        if(this._AppID != null && this.__SOCKET__Endpoint != null && this.__SOCKET__Port != null) {
            Internal__jwt_token token = this._JWTtoken;
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
                                                Internal__jwt_token token2 = this._JWTtoken;
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

                                        Internal__jwt_token token2 = this._JWTtoken;
                                        if (token2 != null && token2._UserID != null && token2._UserID == socket_user_id
                                                && socket_user_id != 0 && timestamp != 0
                                                && !payloadJson.getJSONObject("conversation").optBoolean("is_note", false)) {
                                            final __SOCKET__IMessage_listener listener = this.__SOCKET__Message_listener;
                                            final RealTimeMessagesCallback rtcCallback = this.__SOCKET__RealTimeMessagesCallback;
                                            if (listener != null || rtcCallback != null) {
                                                new Internal_api__CustomerlyRequest.Builder<ArrayList<Internal_entity__Message>>(Internal_api__CustomerlyRequest.ENDPOINT_MESSAGENEWS)
                                                        .opt_converter(data -> Internal_Utils__Utils.fromJSONdataToList(data, "messages", Internal_entity__Message::new))
                                                        .opt_tokenMandatory()
                                                        .opt_receiver((responseState, newsResponse) -> {
                                                            if (responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK && newsResponse != null) {
                                                                if (listener != null) {
                                                                    listener.onMessageEvent(newsResponse);
                                                                } else if (/*rtcCallback != null && */newsResponse.size() != 0) {
                                                                    Internal_entity__Message last_message = newsResponse.get(0);
                                                                    this.__PING__LAST_message_conversation_id = last_message.conversation_id;
                                                                    rtcCallback.onMessage(last_message.content);
                                                                }
                                                            }
                                                        })
                                                        .param("timestamp", timestamp)
                                                        .start();
                                            }
                                        }
                                    }
                                } catch (JSONException ignored) { }
                            }
                        });

                        socket.connect();
                        this.__SOCKET_PingHandler.postDelayed(this.__SOCKET__ping, SOCKET_PING_INTERVAL);
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
        this.__SOCKET_PingHandler.removeCallbacks(this.__SOCKET__ping);
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
        Internal__jwt_token token = this._JWTtoken;
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
            Internal__jwt_token token = this._JWTtoken;
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
        Internal__jwt_token token = this._JWTtoken;
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

    /* ****************************************************************************************************************************************************************/
    /* ********************************************************************************************************************************************** Public Methods **/
    /* ****************************************************************************************************************************************************************/

    public interface Callback {
        void onResponse(boolean isSuccess, boolean newSurvey, boolean newMessage);
    }

    public interface RealTimeMessagesCallback {
        void onMessage(CustomerlyHtmlMessage messageContent);
    }

    @NonNull public static Customerly with(@NonNull Context pContext) {
        if(! Customerly._Instance.initialized) {//Evitiamo di fare lock se non ce n'è bisogno
            synchronized (Customerly.class) {
                if(! Customerly._Instance.initialized) {//Dopo avere rifatto il lock riverifichiamo la condizione per evitare concorrenzialità
                    pContext = pContext.getApplicationContext();
                    Customerly._Instance._AppCacheDir = pContext.getCacheDir().getPath();
                    //APP INFOS
                    try {
                        Customerly._Instance._ApplicationName = pContext.getApplicationInfo().loadLabel(pContext.getPackageManager()).toString();
                    } catch (NullPointerException err) {
                        Customerly._Instance._ApplicationName = "<Error retrieving the app name>";
                    }
                    try {
                        Customerly._Instance._ApplicationVersionCode = pContext.getPackageManager().getPackageInfo(pContext.getPackageName(), 0).versionCode;
                    } catch (PackageManager.NameNotFoundException ignored) {
                        Customerly._Instance._ApplicationVersionCode = 0;
                    }

                    //PREFS
                    final SharedPreferences prefs = pContext.getSharedPreferences(BuildConfig.APPLICATION_ID + ".SharedPreferences", Context.MODE_PRIVATE);
                    Customerly._Instance._SharedPreferences = prefs;

                    //WIDGET COLOR
                    @ColorInt int color;
                    try {
                        int colorAttr;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            colorAttr = android.R.attr.colorPrimary;
                        } else {
                            //Get colorPrimary defined for AppCompat
                            colorAttr = pContext.getResources().getIdentifier("colorPrimary", "attr", pContext.getPackageName());
                        }
                        TypedValue outValue = new TypedValue();
                        pContext.getTheme().resolveAttribute(colorAttr, outValue, true);
                        color = outValue.data;
                    } catch (Exception some_error) {
                        color = DEF_WIDGET_COLOR_INT;
                    }
                    Customerly._Instance.__WidgetColor__fromTheme = color;
                    Customerly._Instance.__WidgetColor__HardCoded = prefs.getInt("CONFIG_HC_WCOLOR", Color.TRANSPARENT);

                    Customerly._Instance.__WidgetColor__Fallback =
                            Customerly._Instance.__WidgetColor__HardCoded == Color.TRANSPARENT
                                    ? Customerly._Instance.__WidgetColor__fromTheme
                                    : Customerly._Instance.__WidgetColor__HardCoded;

                    //JWT TOKEN
                    Customerly._Instance._JWTtoken = Internal__jwt_token.from(prefs);

                    //PING
                    Customerly._Instance.__PING__LAST_app_name = prefs.getString(PREFS_PING_RESPONSE__APP_NAME, Customerly._Instance._ApplicationName);
                    Customerly._Instance.__PING__LAST_widget_color = prefs.getInt(PREFS_PING_RESPONSE__WIDGET_COLOR, Customerly._Instance.__WidgetColor__Fallback);
                    Customerly._Instance.__PING__LAST_powered_by = prefs.getBoolean(PREFS_PING_RESPONSE__POWERED_BY, false);
                    Customerly._Instance.__PING__LAST_welcome_message_users = prefs.getString(PREFS_PING_RESPONSE__WELCOME_USERS, null);
                    Customerly._Instance.__PING__LAST_welcome_message_visitors = prefs.getString(PREFS_PING_RESPONSE__WELCOME_VISITORS, null);
                    Customerly._Instance.__PING__LAST_active_admins = null;
                    Customerly._Instance.__PING__LAST_message_conversation_id = 0;
                    Customerly._Instance.__PING__LAST_surveys = null;

                    Customerly._Instance._AppID = prefs.getString("CONFIG_APP_ID", null);

                    Customerly._Instance.initialized = true;
                }
            }
        }
        return Customerly._Instance;
    }

    /**
     * @param pCustomerlyAppID The appid found in your Customerly console
     */
    public void configure(@NonNull String pCustomerlyAppID) {
        this.configure(pCustomerlyAppID, Color.TRANSPARENT);
    }
    /**
     * @param pCustomerlyAppID The appid found in your Customerly console
     * @param pWidgetColor if Color.TRANSPARENT, it will be ignored
     */
    public void configure(@NonNull String pCustomerlyAppID, @ColorInt int pWidgetColor) {
        final SharedPreferences prefs = this._SharedPreferences;
        if(prefs != null) {
            prefs.edit().putString("CONFIG_APP_ID", pCustomerlyAppID).putInt("CONFIG_HC_WCOLOR", pWidgetColor).apply();
        }

        this._AppID = pCustomerlyAppID;

        this.__WidgetColor__HardCoded = pWidgetColor;
        this.__PING__LAST_widget_color = Customerly._Instance.__WidgetColor__Fallback =
                pWidgetColor == Color.TRANSPARENT
                        ? this.__WidgetColor__fromTheme
                        : pWidgetColor;

        this.update((isSuccess, newSurvey, newMessage) -> {});
    }

    public void setVerboseLogging(boolean pVerboseLogging) {
        this.__VerboseLogging = pVerboseLogging;
    }

    void _TOKEN__update(@NonNull JSONObject obj) {
        @Subst("authB64.payloadB64.checksumB64") String token = obj.optString("token");
        if(token != null) {
            try {
                SharedPreferences prefs = this._SharedPreferences;
                if(prefs != null) {
                    this._JWTtoken = new Internal__jwt_token(token, prefs);
                } else {
                    this._JWTtoken = new Internal__jwt_token(token);
                }
            } catch (IllegalArgumentException wrongTokenFormat) {
                this._JWTtoken = null;
            }
        }
    }

    public void update(final @NonNull Callback pCallback) {
        if(this._isConfigured()) {
            try {
                if (System.currentTimeMillis() < this.__PING__next_ping_allowed) {
                    this._log("You cannot call twice the update so fast. You have to wait " + (this.__PING__next_ping_allowed - System.currentTimeMillis()) / 1000 + " seconds.");
                    pCallback.onResponse(false, this.isSurveyAvailable(), this.__PING__LAST_message_conversation_id != 0);
                } else {
                    new Internal_api__CustomerlyRequest.Builder<Void>(Internal_api__CustomerlyRequest.ENDPOINT_PING)
                            .opt_converter(this.__PING__response_converter)
                            .opt_receiver((responseState, _void) ->
                                    pCallback.onResponse(responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK, this.isSurveyAvailable(), this.__PING__LAST_message_conversation_id != 0))
                            .start();
                }
            } catch (Exception generic) {
                this._log("A generic error occurred in Customerly.update");
                Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.update", generic);
            }
        }
    }

    public void registerUser(@NonNull String email, @Nullable String user_id, @Nullable String name, @NonNull Callback pCallback) {
        this.registerUser(email, user_id, name, null, pCallback);
    }

    public void registerUser(@NonNull String email, @Nullable String user_id, @Nullable String name, @Nullable JSONObject pAttributes, @NonNull Callback pCallback) {
        if(this._isConfigured()) {
            try {
                if(pAttributes != null) {//Check attributes validity
                    JSONArray keys = pAttributes.names();
                    for(int i = 0; i < keys.length(); i++) {
                        try {
                            Object obj = keys.get(i);
                            if(obj instanceof JSONObject || obj instanceof  JSONArray) {
                                pCallback.onResponse(false, false, false);
                                this._log("Attributes JSONObject cannot contain JSONArray or JSONObject");
                                return;
                            }
                        } catch (JSONException ignored) { }
                    }
                }

                new Internal_api__CustomerlyRequest.Builder<Void>(Internal_api__CustomerlyRequest.ENDPOINT_PING)
                        .opt_converter(this.__PING__response_converter)
                        .opt_receiver((responseState, _void) ->
                                pCallback.onResponse(responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK, this.isSurveyAvailable(), this.__PING__LAST_message_conversation_id != 0))

                        .param("email", email)
                        .param("user_id", user_id)
                        .param("name", name)

                        .param("attributes", pAttributes)

                        .start();
            } catch (Exception generic) {
                this._log("A generic error occurred in Customerly.registerUser");
                Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.registerUser", generic);
            }
        }
    }

    public void setAttributes(@Nullable JSONObject pAttributes, @NonNull Callback pCallback) {
        if(this._isConfigured()) {
            Internal__jwt_token token = this._JWTtoken;
            if(token != null && token.isUser()) {
                try {
                    if (pAttributes != null) {//Check attributes validity
                        JSONArray keys = pAttributes.names();
                        for (int i = 0; i < keys.length(); i++) {
                            try {
                                Object obj = keys.get(i);
                                if (obj instanceof JSONObject || obj instanceof JSONArray) {
                                    pCallback.onResponse(false, false, false);
                                    this._log("Attributes JSONObject cannot contain JSONArray or JSONObject");
                                    return;
                                }
                            } catch (JSONException ignored) {
                            }
                        }
                    }
                    new Internal_api__CustomerlyRequest.Builder<Void>(Internal_api__CustomerlyRequest.ENDPOINT_PING)
                            .opt_converter(this.__PING__response_converter)
                            .opt_receiver((responseState, _void) ->
                                    pCallback.onResponse(responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK, this.isSurveyAvailable(), this.__PING__LAST_message_conversation_id != 0))

                            .param("attributes", pAttributes)

                            .start();
                } catch (Exception generic) {
                    this._log("A generic error occurred in Customerly.registerUser");
                    Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.registerUser", generic);
                }
            } else {
                pCallback.onResponse(false, false, false);
                this._log("Cannot setAttributes for lead users");
            }
        }
    }

    public void openSupport(@NonNull Activity activity) {
        if(this._isConfigured()) {
            try {
                activity.startActivity(new Intent(activity, Internal_activity__CustomerlyList_Activity.class));
            } catch (Exception generic) {
                this._log("A generic error occurred in Customerly.openSupport");
                Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.openSupport", generic);
            }
        }
    }

    public boolean isLastSupportConversationAvailable() {
        return this.__PING__LAST_message_conversation_id != 0;
    }

    public void openLastSupportConversation(@NonNull Activity activity) {
        if(this._isConfigured()) {
            try {
                long lastMessage_ConversationID = this.__PING__LAST_message_conversation_id;
                if(lastMessage_ConversationID != 0) {
                    activity.startActivity(new Intent(activity, Internal_activity__CustomerlyChat_Activity.class)
                            .putExtra(Internal_activity__AInput_Customerly_Activity.EXTRA_MUST_SHOW_BACK, false)
                            .putExtra(Internal_activity__CustomerlyChat_Activity.EXTRA_CONVERSATION_ID, lastMessage_ConversationID));
                } else {
                    this._log("No last support conversation available");
                }
            } catch (Exception generic) {
                this._log("A generic error occurred in Customerly.openLastSupportConversation");
                Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.openLastSupportConversation", generic);
            }
        }
    }

    public void logoutUser() {
        if(this._isConfigured()) {
            try {
                final SharedPreferences prefs = this._SharedPreferences;


                this._JWTtoken = null;
                if (prefs != null) {
                    Internal__jwt_token.remove(prefs);
                }

                this.__SOCKET__disconnect();
                this.__PING__next_ping_allowed = 0L;
                this.update((success, survey, message) -> { });
            } catch (Exception ignored) { }
        }
    }

    public void trackEvent(@NonNull String pEventName) {
        if(this._isConfigured()) {
            try {
                Internal__jwt_token token = this._JWTtoken;
                if(token != null && (token.isUser() || token.isLead())) {
                    new Internal_api__CustomerlyRequest.Builder<Internal_entity__Message>(Internal_api__CustomerlyRequest.ENDPOINT_EVENTTRACKING)
                            .opt_trials(2)
                            .param("name", pEventName)
                            .start();
                }
            } catch (Exception generic) {
                this._log("A generic error occurred in Customerly.trackEvent");
                Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.trackEvent", generic);
            }
        }
    }

    public boolean isSurveyAvailable() {
        Internal_entity__Survey[] surveys = this.__PING__LAST_surveys;
        if(surveys != null) {
            for (Internal_entity__Survey survey : surveys) {
                if (survey != null && !survey.isRejectedOrConcluded) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressLint("CommitTransaction")
    public void openSurvey(@NonNull FragmentManager fm) {
        if(this._isConfigured()) {
            if (this.isSurveyAvailable()) {
                try {
                    new Internal_dialogfragment__Survey_DialogFragment().show(fm.beginTransaction().addToBackStack(null), "SURVEYS");
                } catch (Exception generic) {
                    this._log("A generic error occurred in Customerly.openSurvey");
                    Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.openSurvey", generic);
                }
            } else {
                this._log("No surveys available");
            }
        }
    }

    public void registerRealTimeMessagesCallback(@Nullable RealTimeMessagesCallback pRealTimeMessagesCallback) {
        this.__SOCKET__RealTimeMessagesCallback = pRealTimeMessagesCallback;
    }

}
