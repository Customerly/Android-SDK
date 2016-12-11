package io.customerly;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;

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

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** Finals ********/
    /******************************************************************************************************************************************************************/
    private static final String PREF_WELCOME_NEVER_SHOWN = "PREF_WELCOME_NEVER_SHOWN";
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
    @NonNull private final Internal_Utils__RemoteImageHandler _RemoteImageHandler = new Internal_Utils__RemoteImageHandler();
    @NonNull private final Handler _Handler = new Handler();
    @NonNull private final Runnable _HandlePingRun = () -> {
        //TODO dev listeners
    };
    @ColorInt private static final int DEF_WIDGETCOLOR_INT = 0xffd60022;

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** State fields **/
    /******************************************************************************************************************************************************************/

    //Diventano NonNull con la configure
    long _ApplicationVersionCode;
    @ColorInt private int __Fallback_Widget_color = DEF_WIDGETCOLOR_INT;
    @SuppressWarnings("NullableProblems") @NonNull String _AppID, _ApplicationName;
    @SuppressWarnings("NullableProblems") @NonNull private SharedPreferences _SharedPreferences;
    @SuppressWarnings("NullableProblems") @NonNull private JSONObject cookies;
    @NonNull private WeakReference<Activity> _ActiveActivity = new WeakReference<>(null);





    private boolean __VerboseLogging = true;





    @Nullable private Customerly_User customerly_user;
    @Nullable private Socket _Socket;

    @ColorInt int __PING__LAST_widget_color;
    boolean __PING__LAST_powered_by;
    @Nullable private String __PING__LAST_welcome_message_users, __PING__LAST_welcome_message_visitors;
    @Nullable Internal_entity__Admin[] __PING__LAST_active_admins;
    private long __PING__LAST_message_conversation_id;
    private long __PING__LAST_message_account_id;
    @Nullable private SpannableStringBuilder __PING__LAST_message_content;
    @Nullable private Internal_entity__Survey[] __PING__LAST_surveys;
    private boolean __PING__AlreadyPinging = false;
    private boolean __WELCOME__NeverShown = true;


    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** Singleton *****/
    /******************************************************************************************************************************************************************/
    @NonNull static final Customerly _Instance = new Customerly();
    private Customerly() { super(); }

    @NonNull public static Customerly get() {
        return Customerly._Instance;
    }

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** Initializer ***/
    /******************************************************************************************************************************************************************/
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

        @ColorInt int color;
        try {
            int colorAttr;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                colorAttr = android.R.attr.colorAccent;
            } else {
                //Get colorAccent defined for AppCompat
                colorAttr = pApplicationContext.getResources().getIdentifier("colorAccent", "attr", pApplicationContext.getPackageName());
            }
            TypedValue outValue = new TypedValue();
            pApplicationContext.getTheme().resolveAttribute(colorAttr, outValue, true);
            color = outValue.data;
        } catch (Exception some_error) {
            color = DEF_WIDGETCOLOR_INT;
        }
        this.__PING__LAST_widget_color = this.__Fallback_Widget_color = color;

        this.__USER__onNewUser(Customerly_User.from(this._SharedPreferences));

        this.cookies = this.__COOKIES__restoreFromDisk();

        this.__WELCOME__NeverShown = this.__WELCOME__restoreFromDisk();

        this.__PING__restoreFromDisk(this._SharedPreferences);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            pApplicationContext.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    __onActivityResumed(activity);
                }
                @Override public void onActivityPaused(@NonNull Activity activity) {
                    __onActivityPaused(activity);
                }
                @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
                @Override public void onActivityStarted(Activity activity) { }
                @Override public void onActivityStopped(Activity activity) { }
                @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
                @Override public void onActivityDestroyed(Activity activity) { }
            });
        }
    }

    public void onActivityResumed(@NonNull Activity activity) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            this.__onActivityResumed(activity);
        }
    }

    public void onActivityPaused(@NonNull Activity activity) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            this.__onActivityPaused(activity);
        }
    }

    private void __onActivityResumed(@NonNull Activity activity) {
        this._ActiveActivity = new WeakReference<>(activity);
        //TODO
    }
    private void __onActivityPaused(@NonNull Activity activity) {
        this._ActiveActivity.clear();
        //TODO
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

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** Utility *******/
    /******************************************************************************************************************************************************************/
    public void setVerboseLogging(boolean pVerboseLogging) {
        this.__VerboseLogging = pVerboseLogging;
    }
    void _log(@NonNull String pLogMessage) {
        if(this.__VerboseLogging) {
            Log.v(BuildConfig.CUSTOMERLY_SDK_NAME, pLogMessage);
        }
    }
    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** Ping Logic ****/
    /******************************************************************************************************************************************************************/
    @NonNull private final Runnable __PING__PingRun = () ->
            new Internal_api__CustomerlyRequest.Builder<Void>(Internal_api__CustomerlyRequest.ENDPOINT_PINGINDEX)
                .opt_converter(data -> {
                    if(data != null) {
                        this.__PING__onPingResult(data);
                        this.__PING__scheduleHandlePingResult();
                        this.__PING__stopPinging();
                        this._Handler.postDelayed(this.__PING__PingRun, 60000);
                    }
                    return null;
                })
                .opt_receiver((responseState, data) -> {
                    if(responseState != Internal_api__CustomerlyRequest.RESPONSE_STATE__OK) {
                        this.__PING__stopPinging();
                        this._Handler.postDelayed(this.__PING__PingRun, 60000);
                    }
                })
                .start();
    private void __PING__stopPinging() {
        this._Handler.removeCallbacks(this.__PING__PingRun);
    }
    private void __PING__resumePinging() {
        synchronized (this.__PING__PingRun) {
            if (this.__PING__AlreadyPinging)
                return;
            else
                this.__PING__AlreadyPinging = true;
        }
        this.__PING__stopPinging();
        this._Handler.post(this.__PING__PingRun);
    }
    private void __PING__scheduleHandlePingResult() {
        this._Handler.removeCallbacks(this._HandlePingRun);
        this._Handler.postDelayed(this._HandlePingRun, 3000);
    }
    private void __PING__onPingResult(@Nullable JSONObject data) {
        if(data != null) {
            JSONObject obj;

            if((obj = data.optJSONObject("apps")) != null
                    && (obj = obj.optJSONObject("app_config")) != null) {
                String pingWidgetColor = obj.optString("widget_color", null);
                if(pingWidgetColor != null && pingWidgetColor.length() != 0) {
                    if(pingWidgetColor.charAt(0) != '#') {
                        pingWidgetColor = '#' + pingWidgetColor;
                    }
                    try {
                        this.__PING__LAST_widget_color = Color.parseColor(pingWidgetColor);
                    } catch (IllegalArgumentException notCorrectColor) {
                        Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__HTTP_RESPONSE_ERROR, String.format("PingResponse:data.apps.app_config.widget_color is an invalid argb color: '%s'", pingWidgetColor), notCorrectColor);
                        this.__PING__LAST_widget_color = this.__Fallback_Widget_color;
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
            this.__PING__LAST_message_content = null;
            if(last_messages_array != null && last_messages_array.length() != 0) {
                JSONObject message;
                for (int i = 0; i < last_messages_array.length(); i++) {
                    try {
                        message = last_messages_array.getJSONObject(i);
                        if (message == null)
                            continue;
                        this.__PING__LAST_message_conversation_id = message.optLong("conversation_id");
                        this.__PING__LAST_message_account_id = message.optLong("account_id");
                        this.__PING__LAST_message_content = Internal_Utils__Utils.decodeHtmlStringWithEmojiTag(message.optString("content"));
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
            this.__PING__LAST_message_content = null;
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
        this.__PING__LAST_message_content = null;
        this.__PING__LAST_surveys = null;
    }

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** Welcome Msg. **/
    /******************************************************************************************************************************************************************/
    private boolean __WELCOME__restoreFromDisk() {
        return this._SharedPreferences.getBoolean(PREF_WELCOME_NEVER_SHOWN, true);
    }
    boolean __WELCOME__hasNeverShownWelcome() {
        return this.__WELCOME__NeverShown;
    }
    void __WELCOME__setShownWelcome() { this._SharedPreferences.edit().putBoolean(PREF_WELCOME_NEVER_SHOWN, this.__WELCOME__NeverShown = false).apply(); }
    @Nullable Spanned __WELCOME__getMessage() {
        return this._isConfigured()
                ? Internal_Utils__Utils.decodeHtmlStringWithEmojiTag(this.customerly_user == null ? this.__PING__LAST_welcome_message_visitors : this.__PING__LAST_welcome_message_users)
                : null;
    }

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** Cookies *******/
    /******************************************************************************************************************************************************************/
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

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** User **********/
    /******************************************************************************************************************************************************************/
    void __USER__onNewUser(@Nullable Customerly_User user) {
        if(user != null) {
            synchronized (Customerly.class) {
                if(this.customerly_user == null || this.customerly_user.customerly_user_id != user.customerly_user_id) {
                    this.customerly_user = user;
                    user.store(this._SharedPreferences);
                }
            }
            this.__PING__resumePinging();
        }
    }
    @Nullable Customerly_User __USER__get() {
        return this.customerly_user;
    }

    void loadRemoteImage(@NonNull Internal_Utils__RemoteImageHandler.Request request) {
        Customerly.get()._RemoteImageHandler.request(request);
    }

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** Socket ********/
    /******************************************************************************************************************************************************************/
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
            if (user != null && user.customerly_user_id != 0) {
                if(this.__SOCKET__CurrentConfiguration == null || ! this.__SOCKET__CurrentConfiguration.equals(String.format(Locale.UK, "%s-%s-%d", this.__SOCKET__Endpoint, this.__SOCKET__Port, user.customerly_user_id))) {

                    this.__SOCKET__CurrentConfiguration = String.format(Locale.UK, "%s-%s-%d", this.__SOCKET__Endpoint, this.__SOCKET__Port, user.customerly_user_id);

                    String query;
                    try {
                        query = "json=" + new JSONObject().put("nsp", "user").put("app", this._AppID).put("id", user.customerly_user_id).toString();
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
                                        if (usr != null && usr.customerly_user_id == payloadJson.getLong(SOCKET_EVENT__KEY__user_id) && !payloadJson.optBoolean(SOCKET_EVENT__KEY__is_note, false)) {
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

                                    if (timestamp != 0 && crm_user_id != 0 && crm_user_id == usr.customerly_user_id
                                            && !payloadJson.getJSONObject(SOCKET_EVENT__KEY__conversation).optBoolean(SOCKET_EVENT__KEY__is_note, false)) {
                                        final __SOCKET__IMessage_listener listener = this.__SOCKET__Message_listener;
                                        if (listener != null) {
                                            new Internal_api__CustomerlyRequest.Builder<ArrayList<Internal_entity__Message>>(Internal_api__CustomerlyRequest.ENDPOINT_MESSAGENEWS)
                                                    .opt_converter(data -> Internal_Utils__Utils.fromJSONdataToList(data, "messages", Internal_entity__Message::new))
                                                    .opt_receiver((responseState, newsResponse) -> {
                                                        if (responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK && newsResponse != null) {
                                                            listener.onMessageEvent(newsResponse);
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

    public void openSupport(@NonNull Activity activity) {
        if(this._isConfigured()) {
            activity.startActivity(new Intent(activity, Internal_activity__CustomerlyList_Activity.class));
        }
    }

    public void openSupportConversation(@NonNull Activity activity, int pConversationId) {
        if(this._isConfigured()) {
            activity.startActivity(new Intent(activity, Internal_activity__CustomerlyList_Activity.class));
            //TODO pending open chat by conversationId
        }
    }

    public void registerUser(@NonNull Customerly_User userSettings) {//TODO Rimuovere? no vedrai no
        if(this._isConfigured()) {
            this.__USER__onNewUser(userSettings);
        }
    }

    public void logoutUser() {//TODO Rimuovere? altrimenti deve cancellare ultima ping e user da disco
        if(this._isConfigured()) {
            this.customerly_user = null;
            this.__COOKIES__delete();
            Socket socket = this._Socket;
            if (socket != null) {
                if (socket.connected()) {
                    socket.disconnect();
                }
                this._Socket = null;
            }
            //TODO Ripristina icona widget se era admin
            //Cancella task schedulati
            this.__PING__stopPinging();
            this._Handler.removeCallbacks(this._HandlePingRun);
        }
    }

    public void trackEvent(@NonNull String pEventName) {
        //TODO In caso di connessione assente perdiamo l'evento
        new Internal_api__CustomerlyRequest.Builder<Internal_entity__Message>(Internal_api__CustomerlyRequest.ENDPOINT_EVENTTRACKING)
                .opt_trials(2)
                .param("name", pEventName)
                .start();
    }

    public void openSurvey(@NonNull FragmentManager fm) {//TODO
        Internal_entity__Survey[] surveys = this.__PING__LAST_surveys;//Invalidare i surveys altrimenti anche se lo copmleto fino alla successiva ping rimane lì
        if(surveys != null && surveys.length != 0) {
            Internal_dialogfragment__Survey_DialogFragment.open(fm, "SURVEYS", surveys[0]);
        } else {
            this._log("No surveys available");
        }
    }

}
