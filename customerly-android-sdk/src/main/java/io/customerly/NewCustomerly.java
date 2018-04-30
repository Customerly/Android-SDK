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

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.WindowManager;

import com.github.zafarkhaja.semver.Version;

import org.intellij.lang.annotations.Subst;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import io.customerly.XXXXXcancellare.XXXIAct_List;
import io.customerly.XXXXXcancellare.XXXIApi_Request;
import io.customerly.XXXXXcancellare.XXXIDlgF_Survey;
import io.customerly.XXXXXcancellare.XXXIE_Admin;
import io.customerly.XXXXXcancellare.XXXIE_JwtToken;
import io.customerly.XXXXXcancellare.XXXIE_Message;
import io.customerly.XXXXXcancellare.XXXIE_Survey;
import io.customerly.XXXXXcancellare.XXXIEr_CustomerlyErrorHandler;
import io.customerly.XXXXXcancellare.XXXIU_RemoteImageHandler;
import io.customerly.XXXXXcancellare.XXXIU_Utils;
import io.customerly.XXXXXcancellare.XXXNetworkReceiver;
import io.customerly.XXXXXcancellare.XXXPW_AlertMessage;
import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * The singleton representing the Customerly SDK
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class NewCustomerly {

    private static final String PREF_CURRENT_EMAIL = "regusrml", PREF_CURRENT_ID = "regusrid", PREF_CURRENT_COMPANY_INFO = "cmpnynfo";
    private static final long SURVEY_DISPLAY_DELAY = 5000L;

    @NonNull private final Handler __Handler = new Handler();

    private long __PING__next_ping_allowed = 0L;

    @Nullable private ArrayList<Class<? extends Activity>> _DisabledActivities = null;
    @Nullable private Runnable _PendingRunnableForNotDisabledActivity = null;

    @Nullable private WeakReference<Activity> _CurrentActivity;

    private class PingResponseConverter implements XXXIApi_Request.ResponseConverter<Void> {
        private final boolean _HandleAlertMessage, _HandleSurvey;
        private PingResponseConverter(boolean handleSurvey, boolean handleAlertMessage) {
            super();
            this._HandleSurvey = handleSurvey;
            this._HandleAlertMessage = handleAlertMessage;
        }
        @Nullable
        @Override
        public final Void convert(@NonNull JSONObject root) throws JSONException {
            //__PING__LAST_min_version = root.optString("min-version-android", "0.0.0");
            __PING__next_ping_allowed = root.optLong("next-ping-allowed", 0);
            __SOCKET__connect(root.optJSONObject("websocket"));
//            JSONObject app_config = root.optJSONObject("app_config");
//            if(app_config != null) {
//                if(__WidgetColor__HardCoded == Color.TRANSPARENT) {
//                    String pingWidgetColor = XXXIU_Utils.jsonOptStringWithNullCheck(app_config, "widget_color");
//                    if (pingWidgetColor != null && pingWidgetColor.length() != 0) {
//                        if (pingWidgetColor.charAt(0) != '#') {
//                            pingWidgetColor = '#' + pingWidgetColor;
//                        }
//                        try {
//                            __PING__LAST_widget_color = Color.parseColor(pingWidgetColor);
//                        } catch (IllegalArgumentException notCorrectColor) {
//                            XXXIEr_CustomerlyErrorHandler.sendError(XXXIEr_CustomerlyErrorHandler.ERROR_CODE__HTTP_RESPONSE_ERROR, String.format("PingResponse:data.apps.app_config.widget_color is an invalid argb color: '%s'", pingWidgetColor), notCorrectColor);
//                            __PING__LAST_widget_color = __WidgetColor__Fallback;
//                        }
//                    }
//                }
//                __PING__LAST_widget_background_url = XXXIU_Utils.jsonOptStringWithNullCheck(app_config, "widget_background_url");
//                __PING__LAST_powered_by = 1 == app_config.optLong("powered_by", 0);
//                __PING__LAST_welcome_message_users = XXXIU_Utils.jsonOptStringWithNullCheck(app_config, "welcome_message_users");
//                __PING__LAST_welcome_message_visitors = XXXIU_Utils.jsonOptStringWithNullCheck(app_config, "welcome_message_visitors");
//            } else {
//                __PING__LAST_widget_color = __WidgetColor__Fallback;
//                __PING__LAST_widget_background_url = null;
//                __PING__LAST_powered_by = false;
//                __PING__LAST_welcome_message_users = null;
//                __PING__LAST_welcome_message_visitors = null;
//            }
//            __PING__LAST_active_admins = XXXIE_Admin.from(root.optJSONArray("active_admins"));

            final SharedPreferences prefs = _SharedPreferences;
            if(prefs != null) {
                prefs.edit()
                        .putInt(PREFS_PING_RESPONSE__WIDGET_COLOR, __PING__LAST_widget_color)
                        .putString(PREFS_PING_RESPONSE__BACKGROUND_THEME_URL, __PING__LAST_widget_background_url)
                        .putBoolean(PREFS_PING_RESPONSE__POWERED_BY, __PING__LAST_powered_by)
                        .putString(PREFS_PING_RESPONSE__WELCOME_USERS, __PING__LAST_welcome_message_users)
                        .putString(PREFS_PING_RESPONSE__WELCOME_VISITORS, __PING__LAST_welcome_message_visitors)
                        .apply();
            }

            _PendingRunnableForNotDisabledActivity = null;
            if(this._HandleSurvey && _SurveyEnabled) {
                _log("Attempting to display a Survey");
                XXXIE_Survey[] surveys = XXXIE_Survey.from(root.optJSONArray("last_surveys"));
                if(surveys != null) {
                    for (final XXXIE_Survey survey : surveys) {
                        if (survey != null && !survey.isRejectedOrConcluded) {
                            __Handler.postDelayed(() -> {
                                Activity activity = _CurrentActivity == null ? null : _CurrentActivity.get();
                                ArrayList<Class<? extends Activity>> disabledActivities = _DisabledActivities;
                                if (activity != null && (disabledActivities == null || ! disabledActivities.contains(activity.getClass()))) {
                                    try {
                                        try {
                                            XXXIDlgF_Survey.show(activity, survey);
                                        } catch (WindowManager.BadTokenException changedActivityWhileRunning) {
                                            activity = _CurrentActivity == null ? null : _CurrentActivity.get();
                                            if (activity != null) {
                                                try {
                                                    XXXIDlgF_Survey.show(activity, survey);
                                                    _log("Survey successfully displayed");
                                                } catch (WindowManager.BadTokenException ignored) {
                                                    //Second failure.
                                                }
                                            }
                                        }
                                    } catch (Exception generic) {
                                        _log("A generic error occurred in Customerly.openSurvey");
                                        XXXIEr_CustomerlyErrorHandler.sendError(XXXIEr_CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.openSurvey", generic);
                                    }
                                } else {
                                    _PendingRunnableForNotDisabledActivity = () -> __Handler.postDelayed(() -> {
                                        Activity p_activity = _CurrentActivity == null ? null : _CurrentActivity.get();
                                        ArrayList<Class<? extends Activity>> p_disabledActivities = _DisabledActivities;
                                        if (p_activity != null && (p_disabledActivities == null || ! p_disabledActivities.contains(p_activity.getClass()))) {
                                            _PendingRunnableForNotDisabledActivity = null;
                                            try {
                                                try {
                                                    XXXIDlgF_Survey.show(p_activity, survey);
                                                } catch (WindowManager.BadTokenException changedActivityWhileRunning) {
                                                    p_activity = _CurrentActivity == null ? null : _CurrentActivity.get();
                                                    if (p_activity != null) {
                                                        try {
                                                            XXXIDlgF_Survey.show(p_activity, survey);
                                                            _log("Survey successfully displayed");
                                                        } catch (WindowManager.BadTokenException ignored) {
                                                            //Second failure.
                                                        }
                                                    }
                                                }
                                            } catch (Exception generic) {
                                                _log("A generic error occurred in Customerly.openSurvey");
                                                XXXIEr_CustomerlyErrorHandler.sendError(XXXIEr_CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.openSurvey", generic);
                                            }
                                        }
                                    }, SURVEY_DISPLAY_DELAY);
                                }
                            }, SURVEY_DISPLAY_DELAY);
                            return null;
                        }
                    }
                }
                _log("No Survey to display");
            }

            if(this._HandleAlertMessage && _SupportEnabled) {
                _log("Attempting to display an unread message");
                JSONArray last_messages_array = root.optJSONArray("last_messages");
                if (last_messages_array != null && last_messages_array.length() != 0) {
                    for (int i = 0; i < last_messages_array.length(); i++) {
                        try {
                            final JSONObject message = last_messages_array.getJSONObject(i);
                            if (message != null) {
                                __Handler.post(() -> {
                                    Activity activity = _CurrentActivity == null ? null : _CurrentActivity.get();
                                    ArrayList<Class<? extends Activity>> disabledActivities = _DisabledActivities;
                                    if (activity != null && (disabledActivities == null || ! disabledActivities.contains(activity.getClass()))) {
                                        if (activity instanceof SDKActivity) {
                                            ArrayList<XXXIE_Message> list = new ArrayList<>(1);
                                            list.add(new XXXIE_Message(message));
                                            ((SDKActivity) activity).onNewSocketMessages(list);
                                        } else {
                                            try {
                                                XXXPW_AlertMessage.show(activity, new XXXIE_Message(message));
                                                _log("Message alert displayed successfully");
                                            } catch (WindowManager.BadTokenException changedActivityWhileExecuting) {
                                                activity = _CurrentActivity == null ? null : _CurrentActivity.get();
                                                if (activity != null) {
                                                    if (activity instanceof SDKActivity) {
                                                        ArrayList<XXXIE_Message> list = new ArrayList<>(1);
                                                        list.add(new XXXIE_Message(message));
                                                        ((SDKActivity) activity).onNewSocketMessages(list);
                                                    } else {
                                                        try {
                                                            XXXPW_AlertMessage.show(activity, new XXXIE_Message(message));
                                                            _log("Message alert displayed successfully");
                                                        } catch (WindowManager.BadTokenException ignored) {
                                                            _log("An error occours while attaching the alertmessage to the window. Activity: " + activity.toString());
                                                            //Second try failure.
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        _PendingRunnableForNotDisabledActivity = () -> __Handler.postDelayed(() -> {
                                            Activity p_activity = _CurrentActivity == null ? null : _CurrentActivity.get();
                                            ArrayList<Class<? extends Activity>> p_disabledActivities = _DisabledActivities;
                                            if (p_activity != null && (p_disabledActivities == null || ! p_disabledActivities.contains(p_activity.getClass()))) {
                                                _PendingRunnableForNotDisabledActivity = null;
                                                if (p_activity instanceof SDKActivity) {
                                                    ArrayList<XXXIE_Message> list = new ArrayList<>(1);
                                                    list.add(new XXXIE_Message(message));
                                                    ((SDKActivity) p_activity).onNewSocketMessages(list);
                                                } else {
                                                    try {
                                                        XXXPW_AlertMessage.show(p_activity, new XXXIE_Message(message));
                                                        _log("Message alert displayed successfully");
                                                    } catch (WindowManager.BadTokenException changedActivityWhileExecuting) {
                                                        p_activity = _CurrentActivity == null ? null : _CurrentActivity.get();
                                                        if (p_activity != null) {
                                                            if (p_activity instanceof SDKActivity) {
                                                                ArrayList<XXXIE_Message> list = new ArrayList<>(1);
                                                                list.add(new XXXIE_Message(message));
                                                                ((SDKActivity) p_activity).onNewSocketMessages(list);
                                                            } else {
                                                                try {
                                                                    XXXPW_AlertMessage.show(p_activity, new XXXIE_Message(message));
                                                                    _log("Message alert displayed successfully");
                                                                } catch (WindowManager.BadTokenException ignored) {
                                                                    _log("An error occours while attaching the alertmessage to the window. Activity: " + p_activity.toString());
                                                                    //Second try failure.
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }, 500);//Delay needed or popup is not showed
                                    }
                                });
                                return null;
                            }
                        } catch (JSONException ignored) {
                        }
                    }
                }
                _log("no messages to display");
            }
            return null;
        }
    }

    @NonNull private final PingResponseConverter __PING__response_converter__SurveyMessage = new PingResponseConverter(true, true),
            __PING__response_converter__Message = new PingResponseConverter(false, true),
            __PING__response_converter__NaN = new PingResponseConverter(false, false);



    private synchronized void __PING__Start(@Nullable NewCustomerly.Callback pSuccessCallback, @Nullable Callback pFailureCallback) {
        if(this._isConfigured()) {
            //noinspection SpellCheckingInspection
            new XXXIApi_Request.Builder<Void>(XXXIApi_Request.ENDPOINT_PING)
                    .opt_converter(this.__PING__response_converter__SurveyMessage)
                    .opt_receiver((responseState, _null) -> {
                        if (responseState == XXXIApi_Request.RESPONSE_STATE__OK) {
                            if(pSuccessCallback != null) {
                                pSuccessCallback.callback();
                            }
                        } else {
                            if(pFailureCallback != null) {
                                pFailureCallback.callback();
                            }
                        }
                    })
                    .param("email", XXXIU_Utils.getStringSafe(this._SharedPreferences, PREF_CURRENT_EMAIL))
                    .param("user_id", XXXIU_Utils.getStringSafe(this._SharedPreferences, PREF_CURRENT_ID))
                    .param("company", XXXIU_Utils.getStringJSONSafe(this._SharedPreferences, PREF_CURRENT_COMPANY_INFO, false))
                    .start();
        } else {
            if(pFailureCallback != null) {
                pFailureCallback.callback();
            }
        }
    }

    /* ****************************************************************************************************************************************************************/
    /* ********************************************************************************************************************************************** Public Methods **/
    /* ****************************************************************************************************************************************************************/


    public interface Callback {
        /**
         * Implement this interface to obtain async success or failure response from {@link #registerUser(String)},
         * {@link #setCompany(HashMap)} or {@link #setAttributes(HashMap)}
         */
        void callback();
    }

    public interface Task {
        /**
         * @param successCallback To receive success async response
         * @return The Task itself for method chaining
         */
        @CheckResult @NonNull Task successCallback(@Nullable Callback successCallback);
        /**
         * @param failureCallback To receive failure async response
         * @return The Task itself for method chaining
         */
        @CheckResult @NonNull Task failureCallback(@Nullable Callback failureCallback);
        /**
         * Don't forget to call this method to start the task
         */
        void start();
    }

    private abstract class __Task implements Task{
        @Nullable protected NewCustomerly.Callback successCallback;
        @Nullable protected Callback failureCallback;
        /**
         * @param successCallback To receive success async response
         * @return The Task itself for method chaining
         */
        @CheckResult @NonNull public Task successCallback(@Nullable Callback successCallback) {
            this.successCallback = successCallback;
            return this;
        }
        /**
         * @param failureCallback To receive failure async response
         * @return The Task itself for method chaining
         */
        @CheckResult @NonNull public Task failureCallback(@Nullable Callback failureCallback) {
            this.failureCallback = failureCallback;
            return this;
        }
        /**
         * Don't forget to call this method to start the task
         */
        public final void start() {
            if(_isConfigured()) {
                try {
                    this._executeTask();
                } catch (Exception generic) {
                    _log("A generic error occurred in " + this.getClass().getSimpleName());
                    XXXIEr_CustomerlyErrorHandler.sendError(XXXIEr_CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in " + this.getClass().getSimpleName(), generic);
                    if(this.failureCallback != null) {
                        this.failureCallback.callback();
                    }
                }
            } else {
                if(this.failureCallback != null) {
                    this.failureCallback.callback();
                }
            }
        }
        protected abstract void _executeTask();
    }

    public final class UpdateTask extends __Task {
        private UpdateTask() {
            super();
        }
        @Override
        protected void _executeTask() {
            if (System.currentTimeMillis() > __PING__next_ping_allowed) {
                _log("Customerly.update task started");
                __PING__Start(() -> {
                    _log("Customerly.update task completed successfully");
                    if(this.successCallback != null) {
                        this.successCallback.callback();
                    }
                }, () -> {
                    _log("A generic error occurred in Customerly.update");
                    if(this.failureCallback != null) {
                        this.failureCallback.callback();
                    }
                });
            } else {
                _log("You cannot call twice the update so fast. You have to wait " + (__PING__next_ping_allowed - System.currentTimeMillis()) / 1000 + " seconds.");
                if(this.failureCallback != null) {
                    this.failureCallback.callback();
                }
            }
        }
    }

    public final class RegisterUserTask extends __Task {
        @NonNull private final String email;
        @Nullable private String user_id, name;
        @Nullable private JSONObject attributes, company;

        private RegisterUserTask(@NonNull String email) {
            super();
            this.email = email.trim();
        }
        /**
         * Optionally you can specify the user ID
         * @param user_id The ID of the user
         * @return The Task itself for method chaining
         */
        @CheckResult @NonNull public RegisterUserTask user_id(@Nullable String user_id) {
            if(user_id != null) {
                user_id = user_id.trim();
                if(user_id.length() != 0) {
                    this.user_id = user_id;
                }
            } else {
                this.user_id = null;
            }
            return this;
        }
        /**
         * Optionally you can specify the user name
         * @param name The name of the user
         * @return The Task itself for method chaining
         */
        @CheckResult @NonNull public RegisterUserTask name(@Nullable String name) {
            if(name != null) {
                name = name.trim();
                if (name.length() != 0) {
                    this.name = name;
                }
            } else {
                this.name = null;
            }
            return this;
        }
        /**
         * Optionally you can specify the user attributes
         * @param pAttributes The attributes of the user. Can contain only String, char, byte, int, long, float or double values
         * @return The Task itself for method chaining
         * @throws IllegalArgumentException if the attributes map check fails
         */
        @CheckResult @NonNull public RegisterUserTask attributes(@Nullable HashMap<String,Object> pAttributes) throws IllegalArgumentException {
            if(pAttributes != null) {
                Collection<Object> attrs = pAttributes.values();
                for (Object attr : attrs) {
                    if (attr instanceof String ||
                            attr instanceof Integer ||
                            attr instanceof Byte ||
                            attr instanceof Long ||
                            attr instanceof Double ||
                            attr instanceof Float ||
                            attr instanceof Character ||
                            attr instanceof Boolean) {
                        continue;
                    }
                    _log("Attributes HashMap can contain only String, char, byte, int, long, float or double values");
                    throw new IllegalArgumentException("Attributes HashMap can contain only Strings, int, float, long, double or char values");
                }
                this.attributes = new JSONObject(pAttributes);
            } else {
                this.attributes = null;
            }
            return this;
        }
        /**
         * Optionally you can specify the user attributes
         * @param pAttributes The attributes of the user. Can contain only String, char, byte, int, long, float or double values
         * @return The Task itself for method chaining
         * @throws IllegalArgumentException if the attributes map check fails
         */
        @CheckResult @NonNull public RegisterUserTask attributes(@Nullable JSONObject pAttributes) throws IllegalArgumentException {
            if(pAttributes != null) {
                Iterator<String> keysIterator = pAttributes.keys();
                String key;
                while(keysIterator.hasNext()) {
                    Object attr = pAttributes.opt(keysIterator.next());
                    if(     attr != null && (
                            attr instanceof String ||
                            attr instanceof Integer ||
                            attr instanceof Byte ||
                            attr instanceof Long ||
                            attr instanceof Double ||
                            attr instanceof Float ||
                            attr instanceof Character ||
                            attr instanceof Boolean)) {
                        continue;
                    }
                    _log("Attributes HashMap can contain only String, char, byte, int, long, float or double values");
                    throw new IllegalArgumentException("Attributes HashMap can contain only Strings, int, float, long, double or char values");
                }
            } else {
                this.attributes = null;
            }
            return this;
        }

        /**
         * Optionally you can specify the user company
         * @param pCompany The company of the user. The map must contain a String value with key "company_id" containing to the Company ID and a String value with key "name" containing the Company name. Can contain only String, char, int, long, float or double values
         * @return The Task itself for method chaining
         * @throws IllegalArgumentException if the company map check fails
         */
        @CheckResult @NonNull public RegisterUserTask company(@Nullable HashMap<String,Object> pCompany) throws IllegalArgumentException{
            if(pCompany != null) {
                Collection<Object> attrs = pCompany.values();
                for(Object attr : attrs) {
                    if(     attr instanceof String ||
                            attr instanceof Integer ||
                            attr instanceof Byte ||
                            attr instanceof Long ||
                            attr instanceof Double ||
                            attr instanceof Float ||
                            attr instanceof Character ||
                            attr instanceof Boolean) {
                        continue;
                    }
                    _log("Company HashMap can contain only String, char, byte, int, long, float or double values");
                    throw new IllegalArgumentException("Company HashMap can contain only String, char, byte, int, long, float or double values");
                }
                if(! pCompany.containsKey("company_id") && ! pCompany.containsKey("name")) {
                    _log("Company HashMap must contain a String value with key \"company_id\" containing to the Company ID and a String value with key \"name\" containing the Company name");
                    throw new IllegalArgumentException(
                            "Company HashMap must contain a String value with key \"company_id\" containing to the Company ID and a String value with key \"name\" containing the Company name"
                    );
                }
                this.company = new JSONObject(pCompany);
            } else {
                this.company = null;
            }
            return this;
        }

        /**
         * Optionally you can specify the user company
         * @param pCompany The company of the user. The map must contain a String value with key "company_id" containing to the Company ID and a String value with key "name" containing the Company name. Can contain only String, char, int, long, float or double values
         * @return The Task itself for method chaining
         * @throws IllegalArgumentException if the company map check fails
         */
        @CheckResult @NonNull public RegisterUserTask company(@Nullable JSONObject pCompany) throws IllegalArgumentException {
            if(pCompany != null) {
                Iterator<String> keysIterator = pCompany.keys();
                String key;
                while(keysIterator.hasNext()) {
                    Object attr = pCompany.opt(keysIterator.next());
                    if(     attr != null && (
                            attr instanceof String ||
                                    attr instanceof Integer ||
                                    attr instanceof Byte ||
                                    attr instanceof Long ||
                                    attr instanceof Double ||
                                    attr instanceof Float ||
                                    attr instanceof Character ||
                                    attr instanceof Boolean)) {
                        continue;
                    }
                    _log("Company HashMap can contain only String, char, byte, int, long, float or double values");
                    throw new IllegalArgumentException("Company HashMap can contain only String, char, byte, int, long, float or double values");
                }
                if(! pCompany.has("company_id") && ! pCompany.has("name")) {
                    _log("Company HashMap must contain a String value with key \"company_id\" containing to the Company ID and a String value with key \"name\" containing the Company name");
                    throw new IllegalArgumentException(
                            "Company HashMap must contain a String value with key \"company_id\" containing to the Company ID and a String value with key \"name\" containing the Company name"
                    );
                }
                this.company = pCompany;
            } else {
                this.company = null;
            }
            return this;
        }

        protected void _executeTask() {
            SharedPreferences pref = _SharedPreferences;
            if(pref != null && Patterns.EMAIL_ADDRESS.matcher(this.email).matches()) {
                new XXXIApi_Request.Builder<Void>(XXXIApi_Request.ENDPOINT_PING)
                        .opt_converter(root -> {
                            SharedPreferences.Editor editor = pref.edit()
                                    .putString(PREF_CURRENT_EMAIL, this.email)
                                    .putString(PREF_CURRENT_ID, this.user_id);
                            if(this.company != null) {
                                try {
                                    editor.putString(PREF_CURRENT_COMPANY_INFO,
                                            new JSONObject()
                                                    .put("company_id", this.company.getString("company_id"))
                                                    .put("name", this.company.getString("name"))
                                                    .toString());
                                } catch (JSONException ignored) { }
                            } else {
                                editor.remove(PREF_CURRENT_COMPANY_INFO).apply();
                            }
                            editor.apply();
                            return __PING__response_converter__Message.convert(root);
                        })
                        .opt_receiver((responseState, _void) -> {
                            if (responseState == XXXIApi_Request.RESPONSE_STATE__OK) {
                                if(this.successCallback != null) {
                                    _log("Customerly.registerUser task completed successfully");
                                    this.successCallback.callback();
                                }
                            } else {
                                _log("A generic error occurred in Customerly.registerUser task");
                                if(this.failureCallback != null) {
                                    this.failureCallback.callback();
                                }
                            }
                        })

                        .param("email", this.email)
                        .param("user_id", this.user_id)
                        .param("name", this.name)

                        .param("attributes", this.attributes)
                        .param("company", this.company)

                        .start();
                _log("Customerly.registerUser task started");
            } else {
                _log("A generic error occurred in Customerly.registerUser task");
                if(this.failureCallback != null) {
                    this.failureCallback.callback();
                }
            }
        }
    }

    public final class SetAttributesTask extends __Task {
        @NonNull private final JSONObject attributes;
        /**
         * @param attributes The attributes of the user. Can contain only String, char, byte, int, long, float or double values
         * @throws IllegalArgumentException is thrown if the attributes check fails
         */
        private SetAttributesTask (@NonNull HashMap<String,Object> attributes) throws IllegalArgumentException {
            Collection<Object> attrs = attributes.values();
            for(Object attr : attrs) {
                if(     attr instanceof String ||
                        attr instanceof Integer ||
                        attr instanceof Byte ||
                        attr instanceof Long ||
                        attr instanceof Double ||
                        attr instanceof Float ||
                        attr instanceof Character ||
                        attr instanceof Boolean) {
                    continue;
                }
                _log("Attributes HashMap can contain only String, char, byte, int, long, float or double values");
                throw new IllegalArgumentException("Attributes HashMap can contain only Strings, int, float, long, double or char values");
            }
            this.attributes = new JSONObject(attributes);
        }
        /**
         * @param pAttributes The attributes of the user. Can contain only String, char, byte, int, long, float or double values
         * @throws IllegalArgumentException is thrown if the attributes check fails
         */
        private SetAttributesTask (@NonNull JSONObject pAttributes) throws IllegalArgumentException {
            Iterator<String> keysIterator = pAttributes.keys();
            String key;
            while(keysIterator.hasNext()) {
                Object attr = pAttributes.opt(keysIterator.next());
                if(     attr != null && (
                        attr instanceof String ||
                                attr instanceof Integer ||
                                attr instanceof Byte ||
                                attr instanceof Long ||
                                attr instanceof Double ||
                                attr instanceof Float ||
                                attr instanceof Character ||
                                attr instanceof Boolean)) {
                    continue;
                }
                _log("Attributes HashMap can contain only String, char, byte, int, long, float or double values");
                throw new IllegalArgumentException("Attributes HashMap can contain only Strings, int, float, long, double or char values");
            }
            this.attributes = pAttributes;
        }
        @Override
        protected void _executeTask() {
            XXXIE_JwtToken token = _JwtToken;
            if(token != null && token.isUser()) {
                XXXIApi_Request.Builder<Void> builder = new XXXIApi_Request.Builder<Void>(XXXIApi_Request.ENDPOINT_PING)
                        .opt_converter(__PING__response_converter__NaN)
                        .opt_receiver((responseState, _void) -> {
                            if (responseState == XXXIApi_Request.RESPONSE_STATE__OK) {
                                if (this.successCallback != null) {
                                    _log("Customerly.setAttributes task completed successfully");
                                    this.successCallback.callback();
                                }
                            } else {
                                _log("A generic error occurred in Customerly.setAttributes");
                                if (this.failureCallback != null) {
                                    this.failureCallback.callback();
                                }
                            }
                        })
                        .param("attributes", this.attributes);

                SharedPreferences pref = _SharedPreferences;
                if(pref != null) {
                    builder = builder.param("email", XXXIU_Utils.getStringSafe(pref, PREF_CURRENT_EMAIL))
                            .param("user_id", XXXIU_Utils.getStringSafe(pref, PREF_CURRENT_ID))
                            .param("company", XXXIU_Utils.getStringJSONSafe(pref, PREF_CURRENT_COMPANY_INFO, false));
                }
                _log("Customerly.setCompany task started");
                builder.start();
            } else {
                _log("Cannot setAttributes for lead users");
                if(this.failureCallback != null) {
                    this.failureCallback.callback();
                }
            }
        }
    }

    /**
     * Utility builder for Company Map
     */
    public static class CompanyBuilder {
        @NonNull private final JSONObject company = new JSONObject();
        public CompanyBuilder(@NonNull String company_id, @NonNull String name) {
            super();
            try {
                this.company.put("company_id", company_id);
                this.company.put("name", name);
            } catch (JSONException ignored) { }
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, @NonNull String value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, int value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, byte value) {
            return this.put(key, (Object)value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, long value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, double value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, float value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, char value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, boolean value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult private CompanyBuilder put(@NonNull String key, Object value) {
            if(!("company_id".equals(key) || "name".equals(key))) {
                try {
                    this.company.put(key, value);
                } catch (JSONException ignored) { }
            }
            return this;
        }
        @NonNull public JSONObject build() {
            return this.company;
        }
    }

    /**
     * Utility builder for Attributes Map
     */
    public static class AttributesBuilder {
        @NonNull private final JSONObject attrs = new JSONObject();
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, @NonNull String value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, int value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, byte value) {
            return this.put(key, (Object)value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, long value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, double value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, float value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, char value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, boolean value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult private AttributesBuilder put(@NonNull String key, Object value) {
            if(!("company_id".equals(key) || "name".equals(key))) {
                try {
                    this.attrs.put(key, value);
                } catch (JSONException ignored) { }
            }
            return this;
        }
        @NonNull public JSONObject build() {
            return this.attrs;
        }
    }

    public final class SetCompanyTask extends __Task {
        @NonNull private final JSONObject company;
        /**
         * @param pCompany The company of the user. The map must contain a String value with key "company_id" containing to the Company ID and a String value with key "name" containing the Company name. Can contain only String, char, int, long, float or double values.
         * @throws IllegalArgumentException is thrown if company map check fails
         */
        private SetCompanyTask(@NonNull HashMap<String,Object> pCompany) throws IllegalArgumentException {
            Collection<Object> attrs = pCompany.values();
            for(Object attr : attrs) {
                if(     attr instanceof String ||
                        attr instanceof Integer ||
                        attr instanceof Byte ||
                        attr instanceof Long ||
                        attr instanceof Double ||
                        attr instanceof Float ||
                        attr instanceof Character ||
                        attr instanceof Boolean) {
                    continue;
                }
                _log("Company HashMap can contain only String, char, byte, int, long, float or double values");
                throw new IllegalArgumentException("Company HashMap can contain only String, char, byte, int, long, float or double values");
            }
            if(! pCompany.containsKey("company_id") && ! pCompany.containsKey("name")) {
                _log("Company HashMap must contain a String value with key \"company_id\" containing to the Company ID and a String value with key \"name\" containing the Company name");
                throw new IllegalArgumentException(
                        "Company HashMap must contain a String value with key \"company_id\" containing to the Company ID and a String value with key \"name\" containing the Company name"
                );
            }
            this.company = new JSONObject(pCompany);
        }
        /**
         * @param pCompany The company of the user. The map must contain a String value with key "company_id" containing to the Company ID and a String value with key "name" containing the Company name. Can contain only String, char, int, long, float or double values.
         * @throws IllegalArgumentException is thrown if company map check fails
         */
        private SetCompanyTask(@NonNull JSONObject pCompany) throws IllegalArgumentException {
            Iterator<String> keysIterator = pCompany.keys();
            String key;
            while(keysIterator.hasNext()) {
                Object attr = pCompany.opt(keysIterator.next());
                if( attr != null && (
                    attr instanceof String ||
                    attr instanceof Integer ||
                    attr instanceof Byte ||
                    attr instanceof Long ||
                    attr instanceof Double ||
                    attr instanceof Float ||
                    attr instanceof Character ||
                    attr instanceof Boolean)) {
                    continue;
                }
                _log("Company HashMap can contain only String, char, byte, int, long, float or double values");
                throw new IllegalArgumentException("Company HashMap can contain only String, char, byte, int, long, float or double values");
            }
            if(! pCompany.has("company_id") && ! pCompany.has("name")) {
                _log("Company HashMap must contain a String value with key \"company_id\" containing to the Company ID and a String value with key \"name\" containing the Company name");
                throw new IllegalArgumentException(
                        "Company HashMap must contain a String value with key \"company_id\" containing to the Company ID and a String value with key \"name\" containing the Company name"
                );
            }
            this.company = pCompany;
        }
        @Override
        protected void _executeTask() {
            XXXIE_JwtToken token = _JwtToken;
            if(token != null && token.isUser()) {
                try {
                    SharedPreferences pref = _SharedPreferences;
                    if(pref != null) {
                        pref.edit().remove(PREF_CURRENT_COMPANY_INFO).apply();
                    }
                    XXXIApi_Request.Builder<Void> builder = new XXXIApi_Request.Builder<Void>(XXXIApi_Request.ENDPOINT_PING)
                            .opt_converter(__PING__response_converter__NaN)
                            .opt_receiver((responseState, _void) -> {
                                if (responseState == XXXIApi_Request.RESPONSE_STATE__OK) {
                                    if (this.successCallback != null) {
                                        _log("Customerly.setCompany task completed successfully");
                                        this.successCallback.callback();
                                        if(pref != null) {
                                            try {
                                                pref.edit()
                                                        .putString(PREF_CURRENT_COMPANY_INFO,
                                                new JSONObject()
                                                        .put("company_id", this.company.getString("company_id"))
                                                        .put("name", this.company.getString("name"))
                                                        .toString())
                                                        .apply();
                                            } catch (JSONException ignored) { }
                                        }
                                    }
                                } else {
                                    _log("A generic error occurred in Customerly.setCompany");
                                    if (this.failureCallback != null) {
                                        this.failureCallback.callback();
                                    }
                                }
                            })
                            .param("company", this.company);

                    if(pref != null) {
                        builder = builder.param("email", XXXIU_Utils.getStringSafe(pref, PREF_CURRENT_EMAIL))
                                .param("user_id", XXXIU_Utils.getStringSafe(pref, PREF_CURRENT_ID));
                    }

                    _log("Customerly.setCompany task started");
                    builder.start();
                } catch (Exception generic) {
                    _log("A generic error occurred in Customerly.setCompany");
                    XXXIEr_CustomerlyErrorHandler.sendError(XXXIEr_CustomerlyErrorHandler.ERROR_CODE__GENERIC, "Generic error in Customerly.setCompany", generic);
                    if(this.failureCallback != null) {
                        this.failureCallback.callback();
                    }
                }
            } else {
                _log("Cannot setCompany for lead users");
                if(this.failureCallback != null) {
                    this.failureCallback.callback();
                }
            }
        }
    }

    /**
     * Call this method to build a task that force a check for pending Surveys or Message for the current user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @return The builded task that has to be started with his method {@link UpdateTask#start()}
     */
    @CheckResult @NonNull public UpdateTask update() {
        return new UpdateTask();
    }

    /**
     * Call this method to build a task that links your app user to the Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param email The mail address of the user
     * @return The builded task that has to be started with his method {@link RegisterUserTask#start()}
     */
    @CheckResult @NonNull public RegisterUserTask registerUser(@NonNull String email) {
        return new RegisterUserTask(email);
    }

    /**
     * Call this method to build a task that add new custom attributes to the user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param pAttributes Optional attributes for the user. Can contain only String, char, int, long, float or double values
     * @return The builded task that has to be started with his method {@link SetAttributesTask#start()}
     * @throws IllegalArgumentException is thrown if the attributes check fails
     */
    @CheckResult @NonNull public SetAttributesTask setAttributes(@NonNull HashMap<String, Object> pAttributes) throws IllegalArgumentException {
        return new SetAttributesTask(pAttributes);
    }

    /**
     * Call this method to build a task that add new custom attributes to the user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param pAttributes Optional attributes for the user. Can contain only String, char, int, long, float or double values
     * @return The builded task that has to be started with his method {@link SetAttributesTask#start()}
     * @throws IllegalArgumentException is thrown if the attributes check fails
     */
    @CheckResult @NonNull public SetAttributesTask setAttributes(@NonNull JSONObject pAttributes) throws IllegalArgumentException {
        return new SetAttributesTask(pAttributes);
    }

    /**
     * Call this method to build a task that add company attributes to the user.<br><br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param pCompany Optional company for the user. The map must contain a String value with key "company_id" containing to the Company ID and a String value with key "name" containing the Company name
     * @return The builded task that has to be started with his method {@link SetCompanyTask#start()}
     * @throws IllegalArgumentException is thrown if company map check fails
     */
    @CheckResult @NonNull public SetCompanyTask setCompany(@NonNull HashMap<String, Object> pCompany) throws IllegalArgumentException {
        return new SetCompanyTask(pCompany);
    }

    /**
     * Call this method to build a task that add company attributes to the user.<br><br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param pCompany Optional company for the user. The map must contain a String value with key "company_id" containing to the Company ID and a String value with key "name" containing the Company name
     * @return The builded task that has to be started with his method {@link SetCompanyTask#start()}
     * @throws IllegalArgumentException is thrown if company map check fails
     */
    @CheckResult @NonNull public SetCompanyTask setCompany(@NonNull JSONObject pCompany) throws IllegalArgumentException {
        return new SetCompanyTask(pCompany);
    }

    /**
     * Call this method to specify an Activity that will never display a message popup or survey.<br>
     * Every Activity is ENABLED by default
     * @param activityClass The Activity class
     * @see #enableOn(Class)
     */
    public void disableOn(Class<? extends Activity> activityClass) {
        ArrayList<Class<? extends Activity>> disabledActivities = this._DisabledActivities;
        if(disabledActivities == null) {
            this._DisabledActivities = disabledActivities = new ArrayList<>(1);
        }
        disabledActivities.add(activityClass);
    }

    /**
     * Call this method to re-enable an Activity previously disabled with {@link #disableOn(Class)}.
     * @param activityClass The Activity class
     * @see #disableOn(Class)
     */
    public void enableOn(Class<? extends Activity> activityClass) {
        ArrayList<Class<? extends Activity>> disabledActivities = this._DisabledActivities;
        if(disabledActivities != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                disabledActivities.removeIf(c -> c == activityClass);
            } else {
                disabledActivities.remove(activityClass);
            }
        }
    }
}
