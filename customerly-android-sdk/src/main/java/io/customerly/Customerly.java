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
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;

import org.intellij.lang.annotations.Subst;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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

//    private static final String PREFS_PING_RESPONSE__APP_NAME = "PREFS_PING_RESPONSE__APP_NAME";
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
    private @Nullable SharedPreferences _SharedPreferences;
    @Nullable String _AppID, _AppCacheDir;
    @ColorInt private int
            __WidgetColor__fromTheme = DEF_WIDGET_COLOR_INT,
            __WidgetColor__Fallback = DEF_WIDGET_COLOR_INT,
            __WidgetColor__HardCoded = Color.TRANSPARENT;
    @Nullable
    Internal__JwtToken _JwtToken;

    @Nullable Class<? extends Activity> _CurrentActivityClass = null;

    private boolean __VerboseLogging = true;

    @Nullable private Socket _Socket;

    private long __PING__next_ping_allowed = 0L;
//    @Nullable String __PING__LAST_app_name;
    @ColorInt int __PING__LAST_widget_color;
    boolean __PING__LAST_powered_by;
    @Nullable private String __PING__LAST_welcome_message_users, __PING__LAST_welcome_message_visitors;
    @Nullable Internal_entity__Admin[] __PING__LAST_active_admins;
    long __PING__LAST_message_conversation_id = 0L;
    @Nullable Internal_entity__Survey[] __PING__LAST_surveys;
    @NonNull JSONObject __PING__DeviceJSON = new JSONObject();

    @NonNull private final Internal_api__CustomerlyRequest.ResponseConverter<Void> __PING__response_converter = root -> {
        this.__PING__next_ping_allowed = root.optLong("next-ping-allowed", 0);

        JSONObject webSocket = root.optJSONObject("websocket");
        if (webSocket != null) {
                                /*  "webSocket": {
                                      "endpoint": "https://ws2.customerly.io",
                                      "port": "8080"  }  */
            Customerly._Instance.__SOCKET_setEndpoint(Internal_Utils__Utils.jsonOptStringWithNullCheck(webSocket, "endpoint"), Internal_Utils__Utils.jsonOptStringWithNullCheck(webSocket, "port"));
        }
        Customerly._Instance.__SOCKET__connect();

//        JSONObject app_config = root.optJSONObject("app");
//        if(app_config != null) {
//            String app_name = Internal_Utils__Utils.jsonOptStringWithNullCheck(app_config, "name");
//            if(app_name != null) {
//                this.__PING__LAST_app_name = app_name;
//            }
//        }

        JSONObject app_config = root.optJSONObject("app_config");

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
                        Internal_ErrorHandler__CustomerlyErrorHandler.sendError(Internal_ErrorHandler__CustomerlyErrorHandler.ERROR_CODE__HTTP_RESPONSE_ERROR, String.format("PingResponse:data.apps.app_config.widget_color is an invalid argb color: '%s'", pingWidgetColor), notCorrectColor);
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
//                    .putString(PREFS_PING_RESPONSE__APP_NAME, this.__PING__LAST_app_name)
                    .putInt(PREFS_PING_RESPONSE__WIDGET_COLOR, this.__PING__LAST_widget_color)
                    .putBoolean(PREFS_PING_RESPONSE__POWERED_BY, this.__PING__LAST_powered_by)
                    .putString(PREFS_PING_RESPONSE__WELCOME_USERS, this.__PING__LAST_welcome_message_users)
                    .putString(PREFS_PING_RESPONSE__WELCOME_VISITORS, this.__PING__LAST_welcome_message_visitors)
                    .apply();
        }

        return null;
    };

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
            Internal_ErrorHandler__CustomerlyErrorHandler.sendNotConfiguredError();
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
        Internal__JwtToken token = this._JwtToken;
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
            Internal__JwtToken token = this._JwtToken;
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
                                                Internal__JwtToken token2 = this._JwtToken;
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

                                        Internal__JwtToken token2 = this._JwtToken;
                                        if (token2 != null && token2._UserID != null && token2._UserID == socket_user_id
                                                && socket_user_id != 0 && timestamp != 0
                                                && !payloadJson.getJSONObject("conversation").optBoolean("is_note", false)) {
                                            final __SOCKET__IMessage_listener listener = this.__SOCKET__Message_listener;
                                            final RealTimeMessagesCallback rtcCallback = this.__SOCKET__RealTimeMessagesCallback;
                                            if (listener != null || rtcCallback != null) {
                                                new Internal_api__CustomerlyRequest.Builder<ArrayList<Internal_entity__Message>>(Internal_api__CustomerlyRequest.ENDPOINT_MESSAGE_NEWS)
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
        Internal__JwtToken token = this._JwtToken;
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
            Internal__JwtToken token = this._JwtToken;
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
        Internal__JwtToken token = this._JwtToken;
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

    private void __PING__Start(@Nullable Callback.Success pSuccessCallback, @Nullable Callback.Failure pFailureCallback) {
        SharedPreferences pref = this._SharedPreferences;
        //noinspection SpellCheckingInspection
        new Internal_api__CustomerlyRequest.Builder<Void>(Internal_api__CustomerlyRequest.ENDPOINT_PING)
                .opt_converter(this.__PING__response_converter)
                .opt_receiver((responseState, _void) -> {
                    if(responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK) {
                        if (pSuccessCallback != null) {
                            pSuccessCallback.onSuccess(this.isSurveyAvailable(), this.__PING__LAST_message_conversation_id != 0);
                        }
                    } else {
                        if (pFailureCallback != null) {
                            pFailureCallback.onFailure();
                        }
                    }
                })
                .param("email", pref == null ? null : pref.getString("regusrml", null))
                .param("user_id", pref == null ? null : pref.getString("regusrid", null))
                .start();
    }

    void _TOKEN__update(@NonNull JSONObject obj) {
        @Subst("authB64.payloadB64.checksumB64") String token = obj.optString("token");
        if(token != null) {
            try {
                SharedPreferences prefs = this._SharedPreferences;
                if(prefs != null) {
                    this._JwtToken = new Internal__JwtToken(token, prefs);
                } else {
                    this._JwtToken = new Internal__JwtToken(token);
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
    public static class HtmlMessage extends SpannableStringBuilder {
        HtmlMessage(SpannableStringBuilder ssb) {
            super(ssb);
        }
        @NonNull public String toPlainTextString() {
            return super.toString().replace("\uFFFC", "<IMAGE>");
        }
    }

    public interface Callback {
        /**
         * Implement this interface to obtain async success response from {@link #update(Callback.Success, Callback.Failure)}, {@link #registerUser(String, String, String, Callback.Success, Callback.Failure)},
         * {@link #registerUser(String, String, String, JSONObject,Callback.Success, Callback.Failure)} or {@link #setAttributes(JSONObject, Callback.Success, Callback.Failure)}
         */
        interface Success {
            /**
             * Invoked on the async success response from {@link #update(Callback.Success, Callback.Failure)}, {@link #registerUser(String, String, String, Callback.Success, Callback.Failure)},
             * {@link #registerUser(String, String, String, JSONObject,Callback.Success, Callback.Failure)} or {@link #setAttributes(JSONObject, Callback.Success, Callback.Failure)}
             * @param newSurvey true if at least one Survey is available, false otherwise
             * @param newMessage true there is at least one unread message from the support, false otherwise
             */
            void onSuccess(boolean newSurvey, boolean newMessage);
        }
        /**
         * Implement this interface to obtain async failure response from {@link #update(Callback.Success, Callback.Failure)}, {@link #registerUser(String, String, String, Callback.Success, Callback.Failure)},
         * {@link #registerUser(String, String, String, JSONObject,Callback.Success, Callback.Failure)} or {@link #setAttributes(JSONObject, Callback.Success, Callback.Failure)}
         */
        interface Failure {
            /**
             * Invoked on the async failure response from {@link #update(Callback.Success, Callback.Failure)}, {@link #registerUser(String, String, String, Callback.Success, Callback.Failure)},
             * {@link #registerUser(String, String, String, JSONObject,Callback.Success, Callback.Failure)} or {@link #setAttributes(JSONObject, Callback.Success, Callback.Failure)}
             */
            void onFailure();
        }
    }

    public interface SurveyListener {
        /**
         * Implement this interface to obtain async event of the effective displaying of a Survey Dialog started with {@link #openSurvey(FragmentManager, SurveyListener.OnShow, SurveyListener.OnDismiss)}
         */
        interface OnShow {
            /**
             * Invoked when the Survey has actually been displayed to the user
             */
            void onShow();
        }
        /**
         * Implement this interface to obtain async event of the disposing of a Survey Dialog started with {@link #openSurvey(FragmentManager, SurveyListener.OnShow, SurveyListener.OnDismiss)}
         */
        interface OnDismiss {
            int POSTPONED = 0x00, COMPLETED = 0x01, REJECTED = 0x02;
            /**
             * Indicates the reason of the disposing of the Survey<br>
             *     {@link #COMPLETED} - survey closed without invalidation (tap out of alert)
             *     {@link #REJECTED} - survey closed after that user completed it (user closed survey in "thank you message" step)
             *     {@link #POSTPONED} - survey closed and reject (user closed survey before "thank you message" step)
             */
            @IntDef({POSTPONED, COMPLETED, REJECTED})
            @Retention(value = RetentionPolicy.SOURCE)
            @interface DismissMode {}
            /**
             * Invoked when the Survey has been dismissed.
             * @param pDismissMode Indicate the state of the dismissed survey. {@link #POSTPONED} for a Survey just dismissed but still pending and available for a next openSurvey, {@link #COMPLETED} for a completed Survey and {@link #REJECTED} for a rejected Survey
             */
            void onDismiss(@DismissMode int pDismissMode);
        }
    }

    /**
     * Implement this interface to register a callback for incoming real time chat messages with {@link #registerRealTimeMessagesCallback(RealTimeMessagesCallback)}.<br>
     * This callback won't be invoked if the Customerly Support or Chat Activity is currently displayed
     */
    public interface RealTimeMessagesCallback {
        /**
         * Invoked when the user receive a message from the support.
         * This callback won't be invoked if the Customerly Support or Chat Activity is currently displayed
         */
        void onMessage(HtmlMessage messageContent);
    }

    /**
     * Call this method to obtain the reference to the Customerly SDK
     * @param pContext A Context
     * @return The Customerly SDK
     */
    @NonNull public static Customerly with(@NonNull Context pContext) {
        if(! Customerly._Instance.initialized) {//Avoid to perform lock if not needed
            synchronized (Customerly.class) {
                if(! Customerly._Instance.initialized) {//After lock we check again to avoid concurrence
                    pContext = pContext.getApplicationContext();
                    Customerly._Instance._AppCacheDir = pContext.getCacheDir().getPath();
                    //APP INFORMATION
                    try {
                        Customerly._Instance.__PING__DeviceJSON.put("app_name", pContext.getApplicationInfo().loadLabel(pContext.getPackageManager()).toString());
                    } catch (JSONException | NullPointerException err) {
                        try {
                            Customerly._Instance.__PING__DeviceJSON.put("app_name", "<Error retrieving the app name>");
                        } catch (JSONException ignored) { }
                    }
                    try {
                        Customerly._Instance.__PING__DeviceJSON.put("app_version", pContext.getPackageManager().getPackageInfo(pContext.getPackageName(), 0).versionCode);
                    } catch (JSONException | PackageManager.NameNotFoundException err) {
                        try {
                            Customerly._Instance.__PING__DeviceJSON.put("app_version", 0);
                        } catch (JSONException ignored) { }
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
                    //noinspection SpellCheckingInspection
                    Customerly._Instance.__WidgetColor__HardCoded = prefs.getInt("CONFIG_HC_WCOLOR", Color.TRANSPARENT);

                    Customerly._Instance.__WidgetColor__Fallback =
                            Customerly._Instance.__WidgetColor__HardCoded == Color.TRANSPARENT
                                    ? Customerly._Instance.__WidgetColor__fromTheme
                                    : Customerly._Instance.__WidgetColor__HardCoded;

                    //JWT TOKEN
                    Customerly._Instance._JwtToken = Internal__JwtToken.from(prefs);

                    //PING
//                    Customerly._Instance.__PING__LAST_app_name = prefs.getString(PREFS_PING_RESPONSE__APP_NAME, Customerly._Instance._ApplicationName);
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
     * Call this method to configure the SDK indicating the Customerly App ID before accessing it.<br>
     * Call this from your custom Application {@link Application#onCreate()}
     * @param pCustomerlyAppID The Customerly App ID found in your Customerly console
     */
    public void configure(@NonNull String pCustomerlyAppID) {
        this.configure(pCustomerlyAppID, Color.TRANSPARENT);
    }

    /**
     * Call this method to configure the SDK indicating the Customerly App ID before accessing it.<br>
     * Call this from your custom Application {@link Application#onCreate()}<br>
     *     <br>
     * You can choose to ignore the widget_color provided by the Customerly web console for the action bar styling in support activities and use an app-local widget_color instead.
     * @param pCustomerlyAppID The Customerly App ID found in your Customerly console
     * @param pWidgetColor The custom widget_color. If Color.TRANSPARENT, it will be ignored
     */
    public void configure(@NonNull String pCustomerlyAppID, @ColorInt int pWidgetColor) {
        final SharedPreferences prefs = this._SharedPreferences;
        if(prefs != null) {
            //noinspection SpellCheckingInspection
            prefs.edit().putString("CONFIG_APP_ID", pCustomerlyAppID).putInt("CONFIG_HC_WCOLOR", pWidgetColor).apply();
        }

        this._AppID = pCustomerlyAppID;

        this.__WidgetColor__HardCoded = pWidgetColor;
        this.__PING__LAST_widget_color = Customerly._Instance.__WidgetColor__Fallback =
                pWidgetColor == Color.TRANSPARENT
                        ? this.__WidgetColor__fromTheme
                        : pWidgetColor;

        this.__PING__Start(null, null);
    }

    /**
     * Call this method to enable error logging in the Console.
     * Avoid to enable it in release app versions, the suggestion is to pass your.application.package.BuildConfig.DEBUG as parameter
     * @param pVerboseLogging true for enable logging, please pass your.application.package.BuildConfig.DEBUG
     */
    public void setVerboseLogging(boolean pVerboseLogging) {
        this.__VerboseLogging = pVerboseLogging;
    }

    /**
     * Call this method to check for pending Surveys or Message for the current user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @param pSuccessCallback To receive success async response
     * @param pFailureCallback To receive failure async response
     *
     * @see Customerly.Callback
     */
    public void update(@Nullable Callback.Success pSuccessCallback, @Nullable Callback.Failure pFailureCallback) {
        if(this._isConfigured()) {
            try {
                if (System.currentTimeMillis() < this.__PING__next_ping_allowed) {
                    this._log("You cannot call twice the update so fast. You have to wait " + (this.__PING__next_ping_allowed - System.currentTimeMillis()) / 1000 + " seconds.");
                    if(pFailureCallback != null) {
                        pFailureCallback.onFailure();
                    }
                } else {
                    this.__PING__Start(pSuccessCallback, pFailureCallback);
                }
            } catch (Exception generic) {
                this._log("A generic error occurred in Customerly.update");
                Internal_ErrorHandler__CustomerlyErrorHandler.sendError(Internal_ErrorHandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.update", generic);
                if(pFailureCallback != null) {
                    pFailureCallback.onFailure();
                }
            }
        }
    }

    /**
     * Call this method to link your app user to the Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @param email The mail address of the user, this is mandatory
     * @param user_id The optional user_id of the user, null otherwise
     * @param name The optional name of the user, null otherwise
     * @param pSuccessCallback To receive success async response
     * @param pFailureCallback To receive failure async response
     */
    public void registerUser(@NonNull String email, @Nullable String user_id, @Nullable String name, @Nullable Callback.Success pSuccessCallback, @Nullable Callback.Failure pFailureCallback) {
        this.registerUser(email, user_id, name, null, pSuccessCallback, pFailureCallback);
    }

    /**
     * Call this method to link your app user to the Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @param email The mail address of the user, this is mandatory
     * @param user_id The optional user_id of the user, null otherwise
     * @param name The optional name of the user, null otherwise
     * @param pAttributes Optional attributes for the user in a single depth json (the root cannot contain other JSONObject or JSONArray)
     * @param pSuccessCallback To receive success async response
     * @param pFailureCallback To receive failure async response
     */
    public void registerUser(@NonNull String email, @Nullable String user_id, @Nullable String name, @Nullable JSONObject pAttributes, @Nullable Callback.Success pSuccessCallback, @Nullable Callback.Failure pFailureCallback) {
        if(Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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

                    new Internal_api__CustomerlyRequest.Builder<Void>(Internal_api__CustomerlyRequest.ENDPOINT_PING)
                            .opt_converter(this.__PING__response_converter)
                            .opt_receiver((responseState, _void) -> {
                                if (responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK) {
                                    //noinspection SpellCheckingInspection
                                    pref.edit().putString("regusrml", email).putString("regusrid", user_id).apply();
                                    if(pSuccessCallback != null) {
                                        pSuccessCallback.onSuccess(this.isSurveyAvailable(), this.__PING__LAST_message_conversation_id != 0);
                                    }
                                } else {
                                    if(pFailureCallback != null) {
                                        pFailureCallback.onFailure();
                                    }
                                }
                            })

                            .param("email", email)
                            .param("user_id", user_id)
                            .param("name", name)

                            .param("attributes", pAttributes)

                            .start();
                } catch (Exception generic) {
                    this._log("A generic error occurred in Customerly.registerUser");
                    Internal_ErrorHandler__CustomerlyErrorHandler.sendError(Internal_ErrorHandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.registerUser", generic);
                    if(pFailureCallback != null) {
                        pFailureCallback.onFailure();
                    }
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
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @param pAttributes Optional attributes for the user in a single depth json (the root cannot contain other JSONObject or JSONArray)
     * @param pSuccessCallback To receive success async response
     * @param pFailureCallback To receive failure async response
     */
    public void setAttributes(@Nullable JSONObject pAttributes, @Nullable Callback.Success pSuccessCallback, @Nullable Callback.Failure pFailureCallback) {
        if(this._isConfigured()) {
            Internal__JwtToken token = this._JwtToken;
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
                    new Internal_api__CustomerlyRequest.Builder<Void>(Internal_api__CustomerlyRequest.ENDPOINT_PING)
                            .opt_converter(this.__PING__response_converter)
                            .opt_receiver((responseState, _void) -> {
                                if (responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK) {
                                    pSuccessCallback.onSuccess(this.isSurveyAvailable(), this.__PING__LAST_message_conversation_id != 0);
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
                    Internal_ErrorHandler__CustomerlyErrorHandler.sendError(Internal_ErrorHandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.registerUser", generic);
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
        }
    }

    /**
     * Call this method to open the Support Activity.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @param activity The current activity
     */
    public void openSupport(@NonNull Activity activity) {
        if(this._isConfigured()) {
            try {
                activity.startActivity(new Intent(activity, Internal_activity__CustomerlyList_Activity.class));
            } catch (Exception generic) {
                this._log("A generic error occurred in Customerly.openSupport");
                Internal_ErrorHandler__CustomerlyErrorHandler.sendError(Internal_ErrorHandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.openSupport", generic);
            }
        }
    }

    /**
     * Call this method to indicate if from a previous {@link #update(Callback.Success, Callback.Failure)}, {@link #registerUser(String, String, String, Callback.Success, Callback.Failure)} or {@link #setAttributes(JSONObject, Callback.Success, Callback.Failure)} an unread message is available.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @return If an unread message is available
     */
    public boolean isLastSupportConversationAvailable() {
        return this.__PING__LAST_message_conversation_id != 0;
    }

    /**
     * Call this method to open directly the Chat Activity if an unread message is available.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @param activity The current activity
     */
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
                Internal_ErrorHandler__CustomerlyErrorHandler.sendError(Internal_ErrorHandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.openLastSupportConversation", generic);
            }
        }
    }

    /**
     * Call this method to close the user's Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     */
    public void logoutUser() {
        if(this._isConfigured()) {
            try {
                final SharedPreferences prefs = this._SharedPreferences;

                this._JwtToken = null;
                if (prefs != null) {
                    Internal__JwtToken.remove(prefs);
                    //noinspection SpellCheckingInspection
                    prefs.edit().remove("regusrml").remove("regusrid").apply();
                }

                this.__SOCKET__disconnect();
                this.__PING__next_ping_allowed = 0L;
                this.__PING__Start(null, null);
            } catch (Exception ignored) { }
        }
    }

    /**
     * Call this method to keep track of custom labelled events.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @param pEventName The event custom label
     */
    public void trackEvent(@NonNull String pEventName) {
        if(this._isConfigured()) {
            try {
                Internal__JwtToken token = this._JwtToken;
                if(token != null && (token.isUser() || token.isLead())) {
                    new Internal_api__CustomerlyRequest.Builder<Internal_entity__Message>(Internal_api__CustomerlyRequest.ENDPOINT_EVENT_TRACKING)
                            .opt_trials(2)
                            .param("name", pEventName)
                            .start();
                }
            } catch (Exception generic) {
                this._log("A generic error occurred in Customerly.trackEvent");
                Internal_ErrorHandler__CustomerlyErrorHandler.sendError(Internal_ErrorHandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.trackEvent", generic);
            }
        }
    }

    /**
     * Call this method to indicate if from a previous {@link #update(Callback.Success, Callback.Failure)}, {@link #registerUser(String, String, String, Callback.Success, Callback.Failure)} or {@link #setAttributes(JSONObject, Callback.Success, Callback.Failure)} a survey is available.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @return If a survey is available
     */
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

    /**
     * Call this method to start a survey in a DialogFragment, if available.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @param fm The SupportFragmentManager to show the DialogFragment
     */
    public void openSurvey(@NonNull FragmentManager fm) {
        this.openSurvey(fm, null, null);
    }

    /**
     * Call this method to start a survey in a DialogFragment, if available.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @param fm The SupportFragmentManager to show the DialogFragment
     * @param pSurveyShowListener If a survey is actually displayed this callback will be invoked
     */
    public void openSurvey(@NonNull FragmentManager fm, @Nullable SurveyListener.OnShow pSurveyShowListener) {
        this.openSurvey(fm, pSurveyShowListener, null);
    }

    /**
     * Call this method to start a survey in a DialogFragment, if available.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @param fm The SupportFragmentManager to show the DialogFragment
     * @param pSurveyDismissListener This callback will be invoked when the Survey Dialog will be dismissed.
     */
    public void openSurvey(@NonNull FragmentManager fm, @Nullable SurveyListener.OnDismiss pSurveyDismissListener) {
        this.openSurvey(fm, null, pSurveyDismissListener);
    }

    /**
     * Call this method to start a survey in a DialogFragment, if available.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @param fm The SupportFragmentManager to show the DialogFragment
     * @param pSurveyShowListener If a survey is actually displayed this callback will be invoked
     * @param pSurveyDismissListener This callback will be invoked when the Survey Dialog will be dismissed.
     */
    @SuppressLint("CommitTransaction")
    public void openSurvey(@NonNull FragmentManager fm, @Nullable SurveyListener.OnShow pSurveyShowListener, @Nullable SurveyListener.OnDismiss pSurveyDismissListener) {
        if(this._isConfigured()) {
            if (this.isSurveyAvailable()) {
                try {
                    Internal_DialogFragment__Survey_DialogFragment.newInstance(pSurveyShowListener, pSurveyDismissListener).show(fm.beginTransaction().addToBackStack(null), "SURVEYS");
                } catch (Exception generic) {
                    this._log("A generic error occurred in Customerly.openSurvey");
                    Internal_ErrorHandler__CustomerlyErrorHandler.sendError(Internal_ErrorHandler__CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.openSurvey", generic);
                }
            } else {
                this._log("No surveys available");
            }
        }
    }

    /**
     * Call this method to register a callback for incoming real time chat messages when no support activities are displayed.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(String)}
     * @param pRealTimeMessagesCallback The callback
     */
    public void registerRealTimeMessagesCallback(@Nullable RealTimeMessagesCallback pRealTimeMessagesCallback) {
        this.__SOCKET__RealTimeMessagesCallback = pRealTimeMessagesCallback;
    }
}
