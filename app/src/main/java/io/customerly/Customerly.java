package io.customerly;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.Dimension;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.jetbrains.annotations.Contract;
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

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** Instance ******/
    /******************************************************************************************************************************************************************/
    private static Customerly _Instance;

    interface DO {  void perform(@NonNull Customerly crm); }
    interface DO_AND_RETURN<RETURN> {  @NonNull RETURN performReturn(@NonNull Customerly crm); }
    static void _do(@NonNull DO pPerform) {
        Customerly crm = Customerly._Instance;
        if(crm == null) {
            Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__CUSTOMERLY_IS_NULL, "CRMHero._Instance is null");
        } else {
            pPerform.perform(crm);
        }
    }
    @Contract("_,null -> _; _,!null -> !null")
    @Nullable static <RETURN> RETURN _do(@NonNull DO_AND_RETURN<RETURN> pPerformReturn, @Nullable RETURN pReturnValueInCaseCustomerlyNull) {
        Customerly crm = Customerly._Instance;
        if(crm == null) {
            Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__CUSTOMERLY_IS_NULL, "Customerly._Instance is null");
            return pReturnValueInCaseCustomerlyNull;
        } else {
            return pPerformReturn.performReturn(crm);
        }
    }

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** Finals ********/
    /******************************************************************************************************************************************************************/
    private static final String PREF_WELCOME_NEVER_SHOWN = "PREF_WELCOME_NEVER_SHOWN";
    private static final String PREFS__COOKIES_crmhero_visitor_token = "PREFS__COOKIES_crmhero_visitor_token";
    private static final String PREFS__COOKIES_crmhero_user_token = "PREFS__COOKIES_crmhero_user_token";
    private static final String PREFS_PINGRESPONSE__WIDGET_COLOR = "PREFS_PINGRESPONSE__WIDGET_COLOR";
    private static final String PREFS_PINGRESPONSE__WIDGET_ICONTYPE = "PREFS_PINGRESPONSE__WIDGET_ICONTYPE";
    private static final String PREFS_PINGRESPONSE__POWEREDBY = "PREFS_PINGRESPONSE__POWEREDBY";
    private static final String PREFS_PINGRESPONSE__WELCOME_USERS = "PREFS_PINGRESPONSE__WELCOME_USERS";
    private static final String PREFS_PINGRESPONSE__WELCOME_VISITORS = "PREFS_PINGRESPONSE__WELCOME_VISITORS";
    private static final String JSONKEY_crmhero_visitor_token = "crmhero_visitor_token";
    private static final String JSONKEY_crmhero_user_token = "crmhero_user_token";
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
    private static float _DisplayDensity = 1f;
    final long _ApplicationVersionCode;
    static final int SDK_API_VERSION = 1;
    static final int SDK_SOCKET_VERSION = 1;
    @NonNull final String _AppID;
    @NonNull final String _ApplicationName;
    @NonNull private final SharedPreferences _SharedPreferences;
    @NonNull private final Handler _Handler = new Handler();
    @NonNull private final Runnable _HandlePingRun;

    @ColorInt static final int DEF_WIDGETCOLOR_INT = 0xffd60022;
    private static final int DEF_WIDGET_ICONTYPE = 1;

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** State fields **/
    /******************************************************************************************************************************************************************/
    @Nullable private Customerly_User customerly_user;
    private static boolean _VerboseLogging = true;
    @NonNull private JSONObject cookies;
    @Nullable private Socket _Socket;
    private boolean _IsCRMactivityActive = false;

    @ColorInt int __PING__LAST_widget_color;
    private long __PING__LAST_widget_icon;
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
    /************************************************************************************************************************************************** Constructor ***/
    /******************************************************************************************************************************************************************/
    private Customerly(@NonNull Application pApplicationContext, @NonNull String pCustomerlyAppID) {
        super();
        Customerly._Instance = this;
        this._AppID = pCustomerlyAppID;
        String app_name;
        try {
            app_name = pApplicationContext.getApplicationInfo().loadLabel(pApplicationContext.getPackageManager()).toString();
        } catch (NullPointerException err) {
            app_name = "<Uninitialized>";
        }
        this._ApplicationName = app_name;
        long app_version;
        try {
            app_version = pApplicationContext.getPackageManager().getPackageInfo(pApplicationContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
            app_version = 0;
        }
        this._ApplicationVersionCode = app_version;
        float dpi = pApplicationContext.getResources().getDisplayMetrics().densityDpi;
        dpi = dpi > 100/*120, 160, 213, 240, 320, 480 or 640 dpi*/ ? dpi / 160f : dpi;
        Customerly._DisplayDensity = dpi == 0 ? 1 : dpi;
        this._SharedPreferences = pApplicationContext.getSharedPreferences(BuildConfig.APPLICATION_ID + ".SharedPreferences", Context.MODE_PRIVATE);

        this.__USER__onNewUser(Customerly_User.from(this._SharedPreferences));

        this.cookies = this.__COOKIES__restoreFromDisk();

        this.__WELCOME__NeverShown = this.__WELCOME__restoreFromDisk();

        this.__PING__restoreFromDisk(this._SharedPreferences);

        this._HandlePingRun = () -> {
            //TODO dev listeners
        };
    }

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** Utility *******/
    /******************************************************************************************************************************************************************/
    public static void setVerboseLogging(boolean pVerboseLogging) {
        Customerly._VerboseLogging = pVerboseLogging;
    }
    static boolean isVerboseLogging() {           //TODO Rimuovere
        return Customerly._VerboseLogging;
    }
    static void loadImage(@NonNull ImageView pIV, @NonNull String pImageUrl, int pSquaredSize, @DrawableRes int pPlaceholderResID) {
        try {
            Glide.with(pIV.getContext())
                    .load(pImageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(pSquaredSize, pSquaredSize)
                    .fitCenter()
                    .transform(new Internal_Utils__CircleTransform(pIV.getContext()))
                    .placeholder(pPlaceholderResID)
                    .into(pIV);
        } catch (Exception glideException) {
            Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__GLIDE_ERROR, "Error during Glide loading", glideException);
        }
    }
    @Contract(pure = true)
    @Px static int px(@Dimension(unit = Dimension.DP) int dp) {
        return (int) (dp * Customerly._DisplayDensity);
    }
    void setCustomerlyActivityActive(boolean pIsCustomerlyActivityActive) {
        this._IsCRMactivityActive = pIsCustomerlyActivityActive;
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
                .opt_crm(this)
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
                        this.__PING__LAST_widget_color = Customerly.DEF_WIDGETCOLOR_INT;
                    }
                }
                this.__PING__LAST_widget_icon = obj.optLong("widget_icon", Customerly.DEF_WIDGET_ICONTYPE);
                this.__PING__LAST_powered_by = 1 == obj.optLong("powered_by", 0);
                this.__PING__LAST_welcome_message_users = obj.optString("welcome_message_users", null);
                this.__PING__LAST_welcome_message_visitors = obj.optString("welcome_message_visitors", null);
            } else {
                this.__PING__LAST_widget_color = Customerly.DEF_WIDGETCOLOR_INT;
                this.__PING__LAST_widget_icon = Customerly.DEF_WIDGET_ICONTYPE;
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
            this.__PING__LAST_widget_color = Customerly.DEF_WIDGETCOLOR_INT;
            this.__PING__LAST_widget_icon = Customerly.DEF_WIDGET_ICONTYPE;
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
                .putLong(PREFS_PINGRESPONSE__WIDGET_ICONTYPE, this.__PING__LAST_widget_icon)//TODO non serve più
                .putBoolean(PREFS_PINGRESPONSE__POWEREDBY, this.__PING__LAST_powered_by)
                .putString(PREFS_PINGRESPONSE__WELCOME_USERS, this.__PING__LAST_welcome_message_users)
                .putString(PREFS_PINGRESPONSE__WELCOME_VISITORS, this.__PING__LAST_welcome_message_visitors)
                .apply();
    }
    private void __PING__restoreFromDisk(@NonNull SharedPreferences pSharedPreferences) {
        this.__PING__LAST_widget_color = pSharedPreferences.getInt(PREFS_PINGRESPONSE__WIDGET_COLOR, Customerly.DEF_WIDGETCOLOR_INT);
        this.__PING__LAST_widget_icon = pSharedPreferences.getLong(PREFS_PINGRESPONSE__WIDGET_ICONTYPE, Customerly.DEF_WIDGET_ICONTYPE);
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
        Customerly crm = Customerly._Instance;
        if(crm == null) {
            Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__CUSTOMERLY_IS_NULL, "Customerly._Instance is null");
            return null;
        } else {
            if(crm.customerly_user == null) {
                return Internal_Utils__Utils.decodeHtmlStringWithEmojiTag(this.__PING__LAST_welcome_message_visitors);
            } else {
                return Internal_Utils__Utils.decodeHtmlStringWithEmojiTag(this.__PING__LAST_welcome_message_users);
            }
        }
    }

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** Cookies *******/
    /******************************************************************************************************************************************************************/
    @NonNull JSONObject __COOKIES__get() { return this.cookies;   }
    @NonNull private JSONObject __COOKIES__restoreFromDisk() {
        JSONObject cookies = new JSONObject();
        String crmhero_user_token = this._SharedPreferences.getString(PREFS__COOKIES_crmhero_user_token, null);
        if(crmhero_user_token != null) {
            try {
                cookies.put(JSONKEY_crmhero_user_token, crmhero_user_token);
            } catch (JSONException e) {
                cookies = new JSONObject();
            }
        }
        String crmhero_visitor_token = this._SharedPreferences.getString(PREFS__COOKIES_crmhero_visitor_token, null);
        if(crmhero_visitor_token != null) {
            try {
                cookies.put(JSONKEY_crmhero_visitor_token, crmhero_visitor_token);
            } catch (JSONException e) {
                cookies = new JSONObject();
            }
        }

        return cookies;
    }
    void __COOKIES__update(@Nullable JSONObject pLastCookies) {
        if(pLastCookies != null) {
            JSONObject crmhero_user_token_obj = pLastCookies.optJSONObject(JSONKEY_crmhero_user_token);
            if(crmhero_user_token_obj != null) {
                String crmhero_user_token = crmhero_user_token_obj.optString("value", null);
                if(crmhero_user_token != null) {
                    if(crmhero_user_token_obj.optInt("expire", 0) == 1) {
                        this.cookies.remove(JSONKEY_crmhero_user_token);
                        this._SharedPreferences.edit()
                                .remove(PREFS__COOKIES_crmhero_user_token)
                                .apply();
                    } else {
                        try {
                            this.cookies.put(JSONKEY_crmhero_user_token, crmhero_user_token);
                        } catch (JSONException ignored) { }
                        this._SharedPreferences.edit()
                                .putString(PREFS__COOKIES_crmhero_user_token, crmhero_user_token)
                                .apply();
                    }
                }
            }

            JSONObject crmhero_visitor_token_obj = pLastCookies.optJSONObject(JSONKEY_crmhero_visitor_token);
            if(crmhero_visitor_token_obj != null) {
                String crmhero_visitor_token = crmhero_visitor_token_obj.optString("value", null);
                if(crmhero_visitor_token != null) {
                    if(crmhero_visitor_token_obj.optInt("expire", 0) == 1) {
                        this.cookies.remove(JSONKEY_crmhero_visitor_token);
                        this._SharedPreferences.edit()
                                .remove(PREFS__COOKIES_crmhero_visitor_token)
                                .apply();
                    } else {
                        try {
                            this.cookies.put(JSONKEY_crmhero_visitor_token, crmhero_visitor_token);
                        } catch (JSONException ignored) { }
                        this._SharedPreferences.edit()
                                .putString(PREFS__COOKIES_crmhero_visitor_token, crmhero_visitor_token)
                                .apply();
                    }
                }
            }
        }
    }
    private void __COOKIES__delete() {
        this.cookies.remove(JSONKEY_crmhero_user_token);
        this.cookies.remove(JSONKEY_crmhero_visitor_token);
        this._SharedPreferences.edit()
                .remove(PREFS__COOKIES_crmhero_user_token)
                .remove(PREFS__COOKIES_crmhero_visitor_token)
                .apply();
    }

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** User **********/
    /******************************************************************************************************************************************************************/
    void __USER__onNewUser(@Nullable Customerly_User user) {
        if(user != null) {
            synchronized (Customerly.class) {
                if(this.customerly_user == null || this.customerly_user.crmhero_user_id != user.crmhero_user_id) {
                    this.customerly_user = user;
                    user.store(this._SharedPreferences);
                }
            }
            this.__PING__resumePinging();
        }
    }
    @Nullable
    Customerly_User __USER__get() {
        return this.customerly_user;
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
            socket.emit(this._IsCRMactivityActive ? SOCKET_EVENT__PINGACTIVE : SOCKET_EVENT__PING);
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
            if (user != null && user.crmhero_user_id != 0) {
                if(this.__SOCKET__CurrentConfiguration == null || ! this.__SOCKET__CurrentConfiguration.equals(String.format(Locale.UK, "%s-%s-%d", this.__SOCKET__Endpoint, this.__SOCKET__Port, user.crmhero_user_id))) {

                    this.__SOCKET__CurrentConfiguration = String.format(Locale.UK, "%s-%s-%d", this.__SOCKET__Endpoint, this.__SOCKET__Port, user.crmhero_user_id);

                    String query;
                    try {
                        query = "json=" + new JSONObject().put("nsp", "user").put("app", this._AppID).put("id", user.crmhero_user_id).toString();
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
                                    if (Customerly._VerboseLogging) {
                                        try {
                                            Log.e("SOCKET RIC - " + SOCKET_EVENT__TYPING, payloadJson.toString(1));
                                        } catch (JSONException ignored) {
                                        }
                                    }
                                    boolean is_typing = "y".equals(payloadJson.optString(SOCKET_EVENT__KEY__is_typing));
                                    payloadJson = payloadJson.getJSONObject(SOCKET_EVENT__KEY__conversation);
                                    if (payloadJson != null) {
                                        Customerly_User usr = this.customerly_user;
                                        if (usr != null && usr.crmhero_user_id == payloadJson.getLong(SOCKET_EVENT__KEY__user_id) && !payloadJson.optBoolean(SOCKET_EVENT__KEY__is_note, false)) {
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
                                    if (Customerly._VerboseLogging) {
                                        try {
                                            Log.e("SOCKET RIC - " + SOCKET_EVENT__MESSAGE, payloadJson.toString(1));
                                        } catch (JSONException ignored) {
                                        }
                                    }
                                    long timestamp = payloadJson.optLong(SOCKET_EVENT__KEY__timestamp);
                                    long crm_user_id = payloadJson.optLong(SOCKET_EVENT__KEY__user_id);
                                    Customerly_User usr = this.customerly_user;

                                    if (timestamp != 0 && crm_user_id != 0 && crm_user_id == usr.crmhero_user_id
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
                                                    .opt_crm(this)
                                                    .param("timestamp", timestamp)
                                                    .start();
                                        }
                                    }
                                } catch (JSONException ignored) {
                                }
                            }
                        });

                        socket.connect();
                        this._Handler.postDelayed(this.__SOCKET__ping, SOCKET_PING_INTERVAL);
                    } catch (URISyntaxException ignored) {
                    }
                }
            }
        }
    }
    private void __SOCKET__SEND(@NonNull String event, @NonNull JSONObject payload) {
        Socket socket = this._Socket;
        if(socket != null && socket.connected()) {
            if(Customerly._VerboseLogging) {
                try {
                    Log.e("SOCKET SEND - " + event, payload.toString(1));
                } catch (JSONException ignored) { }
            }
            socket.emit(event, payload);
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

    /******************************************************************************************************************************************************************/
    /************************************************************************************************************************************************** End-Developer */
    /******************************************************************************************************************************************************************/
    public static void onApplicationCreate(@NonNull Application pApplicationContext, @NonNull String pCustomerlyAppID) {
        new Customerly(pApplicationContext, pCustomerlyAppID);
    }
    public static void onActivityDestroy() {//TODO ??? serve? se non serve eliminare le def/Customerly_*Activity
        Customerly._do(crm -> {
            Socket socket = crm._Socket;
            if(socket != null && socket.connected()) {
                socket.disconnect();
            }
        });
    }

    public static void openSupport(@NonNull Activity activity) {
        activity.startActivity(new Intent(activity, Internal_activity__CustomerlyList_Activity.class));
    }

    public static void registerUser(@NonNull Customerly_User userSettings) {//TODO Rimuovere?
        Customerly._do(crm -> crm.__USER__onNewUser(userSettings));
    }

    public static void logoutUser() {//TODO Rimuovere?
        Customerly._do(crm -> {
            crm.customerly_user = null;
            crm.__COOKIES__delete();
            Socket socket = crm._Socket;
            if(socket != null) {
                if(socket.connected()) {
                    socket.disconnect();
                }
                crm._Socket = null;
            }
            //TODO Ripristina icona widget se era admin
            //Cancella task schedulati
            crm.__PING__stopPinging();
            crm._Handler.removeCallbacks(crm._HandlePingRun);
        });
    }

    public static void trackEvent(@NonNull String pEventName) {
        //TODO In caso di connessione assente perdiamo l'evento
        new Internal_api__CustomerlyRequest.Builder<Internal_entity__Message>(Internal_api__CustomerlyRequest.ENDPOINT_EVENTTRACKING)
                .opt_trials(2)
                .param("name", pEventName)
                .start();
    }

}
