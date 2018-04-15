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

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.CheckResult;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.Size;
import android.support.annotation.StringDef;
import android.util.Log;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 * Created by Gianni on 29/11/16.
 * Project: Customerly Android SDK
 */
class IApi_Request<RES> extends AsyncTask<JSONObject, Void, RES> {

    @StringDef({ENDPOINT_PING, ENDPOINT_CONVERSATION_RETRIEVE, ENDPOINT_MESSAGE_SEEN, ENDPOINT_MESSAGE_NEWS,
            ENDPOINT_MESSAGE_RETRIEVE, ENDPOINT_MESSAGE_SEND, ENDPOINT_EVENT_TRACKING, ENDPOINT_REPORT_CRASH,
            ENDPOINT_SURVEY_SUBMIT, ENDPOINT_SURVEY_SEEN, ENDPOINT_SURVEY_BACK, ENDPOINT_SURVEY_REJECT})
    @Retention(RetentionPolicy.SOURCE)
    @interface Endpoint {}

    @IntDef({RESPONSE_STATE__PENDING, RESPONSE_STATE__OK, RESPONSE_STATE__ERROR_NO_CONNECTION,
            RESPONSE_STATE__ERROR_BAD_REQUEST, RESPONSE_STATE__ERROR_NETWORK, RESPONSE_STATE__ERROR_BAD_RESPONSE,
            RESPONSE_STATE__SERVERERROR_USER_NOT_AUTHENTICATED, RESPONSE_STATE__NO_TOKEN_AVAILABLE})
    @Retention(RetentionPolicy.SOURCE)
    @interface ResponseState {}

    static final String ENDPOINT_PING =                     BuildConfig.CUSTOMERLY_API_ENDPOINT_BASEURL + BuildConfig.CUSTOMERLY_API_VERSION + "/ping/index/";
    static final String ENDPOINT_CONVERSATION_RETRIEVE =    BuildConfig.CUSTOMERLY_API_ENDPOINT_BASEURL + BuildConfig.CUSTOMERLY_API_VERSION + "/conversation/retrieve/";
    static final String ENDPOINT_MESSAGE_SEEN =             BuildConfig.CUSTOMERLY_API_ENDPOINT_BASEURL + BuildConfig.CUSTOMERLY_API_VERSION + "/message/seen/";
    static final String ENDPOINT_MESSAGE_NEWS =             BuildConfig.CUSTOMERLY_API_ENDPOINT_BASEURL + BuildConfig.CUSTOMERLY_API_VERSION + "/message/news/";
    static final String ENDPOINT_MESSAGE_RETRIEVE =         BuildConfig.CUSTOMERLY_API_ENDPOINT_BASEURL + BuildConfig.CUSTOMERLY_API_VERSION + "/message/retrieve/";
    static final String ENDPOINT_MESSAGE_SEND =             BuildConfig.CUSTOMERLY_API_ENDPOINT_BASEURL + BuildConfig.CUSTOMERLY_API_VERSION + "/message/send/";
    static final String ENDPOINT_EVENT_TRACKING =           BuildConfig.CUSTOMERLY_API_ENDPOINT_BASEURL + BuildConfig.CUSTOMERLY_API_VERSION + "/event/";
    static final String ENDPOINT_REPORT_CRASH =             BuildConfig.CUSTOMERLY_API_ENDPOINT_BASEURL + BuildConfig.CUSTOMERLY_API_VERSION + "/crash/";
    static final String ENDPOINT_SURVEY_SUBMIT =            BuildConfig.CUSTOMERLY_API_ENDPOINT_BASEURL + BuildConfig.CUSTOMERLY_API_VERSION + "/survey/submit/";
    static final String ENDPOINT_SURVEY_SEEN =              BuildConfig.CUSTOMERLY_API_ENDPOINT_BASEURL + BuildConfig.CUSTOMERLY_API_VERSION + "/survey/seen/";
    static final String ENDPOINT_SURVEY_BACK =              BuildConfig.CUSTOMERLY_API_ENDPOINT_BASEURL + BuildConfig.CUSTOMERLY_API_VERSION + "/survey/back/";
    static final String ENDPOINT_SURVEY_REJECT =            BuildConfig.CUSTOMERLY_API_ENDPOINT_BASEURL + BuildConfig.CUSTOMERLY_API_VERSION + "/survey/reject/";

    static final byte RESPONSE_STATE__PENDING = 0;
    static final byte RESPONSE_STATE__OK = -1;
    static final byte RESPONSE_STATE__ERROR_NO_CONNECTION = -2;
    static final byte RESPONSE_STATE__ERROR_BAD_REQUEST = -3;
    static final byte RESPONSE_STATE__ERROR_NETWORK = -4;
    static final byte RESPONSE_STATE__ERROR_BAD_RESPONSE = -5;
    static final int RESPONSE_STATE__SERVERERROR_USER_NOT_AUTHENTICATED = 403;
    static final int RESPONSE_STATE__SERVERERROR_APP_INSOLVENT = 17;
    static final int RESPONSE_STATE__NO_TOKEN_AVAILABLE = -6;

    @NonNull @Endpoint private final String _Endpoint;
    @NonNull private final ResponseConverter<RES> _ResponseConverter;
    @NonNull private final ResponseReceiver<RES> _ResponseReceiver;
    @IntRange(from=1, to=5) private final int _Trials;
    private final boolean _TokenMandatory;

    @ResponseState private int _ResponseState = RESPONSE_STATE__PENDING;

    @RequiresPermission(Manifest.permission.INTERNET)
    private IApi_Request(@Endpoint @NonNull String pEndpoint, @NonNull ResponseConverter<RES> pResponseConverter, @NonNull ResponseReceiver<RES> pResponseReceiver, @IntRange(from=1, to=5) int pTrials, boolean pTokenMandatory) {
        super();
        this._Endpoint = pEndpoint;
        this._ResponseConverter = pResponseConverter;
        this._ResponseReceiver = pResponseReceiver;
        this._Trials = pTrials;
        this._TokenMandatory = pTokenMandatory;
    }

    @SuppressWarnings("unused")
    static class Builder<RES> {
        @NonNull @Endpoint private final String _Endpoint;
        @Nullable private Context _Context;
        @Nullable private ResponseConverter<RES> _ResponseConverter;
        @Nullable private ResponseReceiver<RES> _ResponseReceiver;
        @IntRange(from=1, to=5) private int _Trials = 1;
        @NonNull private final JSONObject _Params = new JSONObject();
        @Nullable private CharSequence _ProgressDialog_Title, _ProgressDialog_Message;
        @Nullable private WeakReference<View> _ProgressView;
        @Nullable private Runnable _OnPreExecute;
        private boolean _TokenMandatory = false;
        private boolean _ReportingErrorDisabled = false;

        @IntDef({View.GONE, View.INVISIBLE}) @Retention(RetentionPolicy.SOURCE) @interface HiddenVisibilityType {}
        @HiddenVisibilityType private int _ProgressView_HiddenVisibilityType;

        Builder(@Endpoint @NonNull String pEndpoint) {
            this._Endpoint = pEndpoint;
        }
        @CheckResult Builder<RES> opt_checkConn(@NonNull Context pContext) {
            this._Context = pContext;
            return this;
        }
        @CheckResult Builder<RES> opt_converter(@Nullable ResponseConverter<RES> pResponseConverter) {
            this._ResponseConverter = pResponseConverter;
            return this;
        }
        @CheckResult Builder<RES> opt_receiver(@Nullable ResponseReceiver<RES> pResponseReceiver) {
            this._ResponseReceiver = pResponseReceiver;
            return this;
        }
        @CheckResult Builder<RES> opt_trials(@SuppressWarnings("SameParameterValue") @IntRange(from=1, to=5) int pTrials) {
            this._Trials = pTrials;
            return this;
        }
        @CheckResult Builder<RES> opt_tokenMandatory() {
            this._TokenMandatory = true;
            return this;
        }
        @CheckResult Builder<RES> opt_progress_dialog(@NonNull Context pContext, @NonNull CharSequence title, @NonNull CharSequence message) {
            this._Context = pContext;
            this._ProgressDialog_Title = title;
            this._ProgressDialog_Message = message;
            return this;
        }
        @CheckResult Builder<RES> opt_progress_view(@NonNull View progressView, @SuppressWarnings("SameParameterValue") @HiddenVisibilityType int progressView_hiddenVisibilityType) {
            this._ProgressView = new WeakReference<>(progressView);
            this._ProgressView_HiddenVisibilityType = progressView_hiddenVisibilityType;
            return this;
        }
        @CheckResult Builder<RES> opt_onPreExecute(@NonNull Runnable onPreExecute) {
            this._OnPreExecute = onPreExecute;
            return this;
        }
        @CheckResult Builder<RES> opt__ReportingErrorDisabled() {
            this._ReportingErrorDisabled = true;
            return this;
        }
        @CheckResult Builder<RES> param(@Nullable String pKey, @Nullable Object pValue) {
            try { this._Params.putOpt(pKey, pValue); } catch (JSONException ignored) { }
            return this;
        }
        @CheckResult Builder<RES> param(@Nullable String pKey, boolean pValue) {
            try { this._Params.putOpt(pKey, pValue); } catch (JSONException ignored) { }
            return this;
        }
        @CheckResult Builder<RES> param(@Nullable String pKey, double pValue) {
            try { this._Params.putOpt(pKey, pValue); } catch (JSONException ignored) { }
            return this;
        }
        @CheckResult Builder<RES> param(@Nullable String pKey, int pValue) {
            try { this._Params.putOpt(pKey, pValue); } catch (JSONException ignored) { }
            return this;
        }
        @CheckResult Builder<RES> param(@Nullable String pKey, long pValue) {
            try { this._Params.putOpt(pKey, pValue); } catch (JSONException ignored) { }
            return this;
        }
        void start() {
            if(Customerly.get()._isConfigured(this._ReportingErrorDisabled)) {
                if (this._Context == null || XXXIU_Utils.checkConnection(this._Context)) {
                    ProgressDialog pd_tmp = null;
                    if(this._Context != null && this._ProgressDialog_Title != null && this._ProgressDialog_Message != null) {
                        try {
                            pd_tmp = ProgressDialog.show(this._Context, this._ProgressDialog_Title, this._ProgressDialog_Message, true, false);
                        } catch (Exception ignored) { }
                    }
                    if(this._OnPreExecute != null) {
                        this._OnPreExecute.run();
                    }
                    final View progressView = this._ProgressView == null ? null : this._ProgressView.get();
                    if(progressView != null) {
                        View parent = (View) progressView.getParent();
                        if(parent != null) {//Trick, if i post the runnable on the progressView with visibility gone, it will be never called
                            parent.post(() -> progressView.setVisibility(View.VISIBLE));
                        }
                    }
                    final ProgressDialog pd = pd_tmp;
                    new IApi_Request<>(this._Endpoint,
                            this._ResponseConverter != null ? this._ResponseConverter : data -> null,
                            (statusCode, result) -> {
                                if(pd != null) {
                                    try {
                                        pd.dismiss();
                                    } catch (Exception ignored) { }
                                }
                                if(progressView != null) {
                                    progressView.post(() -> progressView.setVisibility(this._ProgressView_HiddenVisibilityType));
                                }
                                if(this._ResponseReceiver != null) {
                                    this._ResponseReceiver.onResponse(statusCode, result);
                                }
                            },
                            this._Trials, this._TokenMandatory)
                            .execute(this._Params);
                } else if (this._ResponseReceiver != null) {
                    Customerly.get()._log("Check your connection");
                    this._ResponseReceiver.onResponse(RESPONSE_STATE__ERROR_NO_CONNECTION, null);
                }
            }
        }
    }


    @NonNull private JSONObject json_appid_E_device(@NonNull String app_id, @Nullable JSONObject params) throws JSONException {
        return (params != null ? params : new JSONObject())
                .put("app_id", app_id).put("device", Customerly.get().__PING__DeviceJSON);
    }

    @Nullable
    @Override
    protected final RES doInBackground(@Size(value=1) @NonNull JSONObject[] pParams) {
        String app_id = Customerly.get()._AppID;
        if(app_id == null) {
            return null;
        }
        JSONObject request_root = new JSONObject();

        XXXIE_JwtToken token = Customerly.get()._JwtToken;
        boolean tokenSent = false;
        if(token != null) {
            try {
                request_root.put(XXXIE_JwtToken.PAYLOAD_KEY, token.toString());
                tokenSent = true;
            } catch (JSONException ignored) { }
        }

        JSONObject params;
        if(ENDPOINT_PING.equals(this._Endpoint)) {
            try {
                params = json_appid_E_device(app_id, pParams[0]);
            } catch (JSONException error) {
                this._ResponseState = RESPONSE_STATE__ERROR_BAD_REQUEST;
                return null;
            }
        } else {
            params = pParams[0];
            if(!tokenSent) {
                if(this._TokenMandatory) {//If not token available and token is mandatory, perform first a ping to obtain it or kill the requests
                    try {
                        this.executeRequest(ENDPOINT_PING, new JSONObject().put("params", this.json_appid_E_device(app_id, null)));
                    } catch (JSONException error) {
                        this._ResponseState = RESPONSE_STATE__NO_TOKEN_AVAILABLE;
                        return null;
                    }
                    token = Customerly.get()._JwtToken;
                    if (token != null) {
                        try {
                            request_root.put(XXXIE_JwtToken.PAYLOAD_KEY, token.toString());
                        } catch (JSONException ignored) { }
                        if(ENDPOINT_REPORT_CRASH.equals(this._Endpoint)) {
                            try {
                                params = json_appid_E_device(app_id, params);
                            } catch (JSONException ignored) { }
                        }
                    } else {
                        this._ResponseState = RESPONSE_STATE__NO_TOKEN_AVAILABLE;
                        return null;
                    }
                } else {//If not token available and token is not mandatory, i send the app_id and device
                    try {
                        params = json_appid_E_device(app_id, params);
                    } catch (JSONException ignored) { }
                }
            } else {
                if(ENDPOINT_REPORT_CRASH.equals(this._Endpoint)) {
                    try {
                        params = json_appid_E_device(app_id, params);
                    } catch (JSONException ignored) { }
                }
            }
        }

        if(params != null && params.length() != 0) {
            try {
                request_root.put("params", params);
            } catch (JSONException not_params) {
                return null;
            }
        }

        JSONObject result = this.executeRequest(this._Endpoint, request_root);
        if(result != null) {
            try {
                return this._ResponseConverter.convert(result);
            } catch (JSONException ignored) { }
        }
        return null;
    }

    @Nullable private JSONObject executeRequest(@NonNull @Endpoint String pEndpoint, @NonNull JSONObject pJsonPayload) {
        int trials = this._Trials;
        while(trials > 0) {
            this._ResponseState = RESPONSE_STATE__PENDING;
            OutputStream os = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(pEndpoint).openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept-Language", Locale.getDefault().toString());//es: "it_IT"
                conn.setConnectTimeout(10000);

                if(BuildConfig.CUSTOMERLY_DEV_MODE) {
                    @SuppressWarnings("UnusedAssignment") String postObjectToString;
                    try {
                        postObjectToString = pJsonPayload.toString(4);
                    } catch (JSONException error) {
                        postObjectToString = "Malformed JSON";
                    }

                    String more = null;//TODO
                    if(postObjectToString != null && postObjectToString.length() > 500) {
                        more = postObjectToString.substring(500);
                    }
                    Log.e(BuildConfig.CUSTOMERLY_SDK_NAME,
                            "-----------------------------------------------------------" +
                                    "\nNEW HTTP REQUEST" +
                                    "\n+ Endpoint:        " + pEndpoint +
                                    "\n+ SSL:             " + (conn instanceof HttpsURLConnection ? "Active" : "Not Active") +
                                    "\n+ METHOD:          " + conn.getRequestMethod() +
                                    "\n+ Content-Type:    " + conn.getRequestProperty("Content-Type") +
                                    "\n+ Accept-Language: " + conn.getRequestProperty("Accept-Language") +
                                    "\nJSON BODY:\n" +
                                    postObjectToString +
                                    "\n-----------------------------------------------------------");
                }

                if(conn instanceof HttpsURLConnection) {
                    try {
                        SSLContext sc = SSLContext.getInstance("TLS");
                        sc.init(null, null, new java.security.SecureRandom());
                        ((HttpsURLConnection)conn).setSSLSocketFactory(sc.getSocketFactory());
                    } catch (NoSuchAlgorithmException | KeyManagementException e) {
                        e.printStackTrace();
                    }
                }

                os = conn.getOutputStream();
                os.write(pJsonPayload.toString().getBytes());
                os.flush();

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        conn.getResponseCode() == HttpURLConnection.HTTP_OK ? conn.getInputStream() : conn.getErrorStream()
                ));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = br.readLine()) != null) {
                    responseStrBuilder.append(inputStr);
                }
                br.close();
                conn.disconnect();

                try {
                    JSONObject response_root = new JSONObject(responseStrBuilder.toString());

                    if(BuildConfig.CUSTOMERLY_DEV_MODE) {
                        @SuppressWarnings("UnusedAssignment") String rootToString;
                        try {
                            rootToString = response_root.toString(1);
                        } catch (JSONException error) {
                            rootToString = "Malformed JSON";
                        }
                        Log.e(BuildConfig.CUSTOMERLY_SDK_NAME,
                                "-----------------------------------------------------------" +
                                        "\nHTTP RESPONSE" +
                                        "\n+ Endpoint:        " + pEndpoint +
                                        "\nJSON BODY:\n" +
                                        rootToString +
                                        "\n-----------------------------------------------------------");

                    }

                    if(! response_root.has("error")) {
                        this._ResponseState = RESPONSE_STATE__OK;
                        if (ENDPOINT_PING.equals(pEndpoint)) {
                            Customerly.get()._TOKEN__update(response_root);
                        }
                        return response_root;
                    } else {
                        /*  {   "error": "exception_title",
                                "message": "Exception_message",
                                "code": "ExceptionCode"     }   */
                        int error_code = response_root.optInt("code", -1);
                        Customerly.get()._log(String.format(Locale.UK, "Error: %s Message: %s ErrorCode: %s",
                                response_root.has("error"),
                                XXXIU_Utils.jsonOptStringWithNullCheck(response_root, "message", "The server received the request but an error has come"),
                                error_code));
                        if(error_code != -1) {
                            switch(error_code) {
                                case RESPONSE_STATE__SERVERERROR_USER_NOT_AUTHENTICATED:
                                    //{"error": "..title..", "message":"User not authenticated", "code":403}
                                    this._ResponseState = RESPONSE_STATE__SERVERERROR_USER_NOT_AUTHENTICATED;
                                    return null;
                                case RESPONSE_STATE__SERVERERROR_APP_INSOLVENT:
                                    //{ "error": "App is temporary deactived", "message": "Subscription is expired, please contact Customerly.io", "code": 17 }
                                    this._ResponseState = RESPONSE_STATE__SERVERERROR_APP_INSOLVENT;
                                    Customerly.get()._setIsAppInsolvent();
                                    return null;
                            }
                        }
                        this._ResponseState = RESPONSE_STATE__ERROR_NETWORK;
                    }
                } catch (JSONException error) {
                    Customerly.get()._log("The server received the request but an error has come");
                    this._ResponseState = RESPONSE_STATE__ERROR_BAD_RESPONSE;
                    return null;
                }
            } catch (IOException error) {
                Customerly.get()._log("An error occurs during the connection to server");
                this._ResponseState = RESPONSE_STATE__ERROR_NETWORK;
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException ignored) { }
                }
            }
            trials--;
        }
        if(BuildConfig.CUSTOMERLY_DEV_MODE) {
            Log.e(BuildConfig.CUSTOMERLY_SDK_NAME,
                    "-----------------------------------------------------------" +
                            "\nHTTP RESPONSE" +
                            "\n+ Endpoint:        " + pEndpoint +
                            "\n!!!ERROR!!!\n" +
                            "\n-----------------------------------------------------------");

        }
        return null;
    }

    @Override
    protected final void onPostExecute(@Nullable RES pResponse) {
        this._ResponseReceiver.onResponse(this._ResponseState, pResponse);
    }

    interface ResponseConverter<RES> {
        @Nullable RES convert(@NonNull JSONObject pData) throws JSONException;
    }

    interface ResponseReceiver<RES> {
        void onResponse(@ResponseState int pResponseState, @Nullable RES pResponse);
    }
}
