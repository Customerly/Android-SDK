package io.customerly;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.Size;
import android.support.annotation.StringDef;
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
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import static io.customerly.Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__IO_ERROR;

class Internal_api__CustomerlyRequest<RES> extends AsyncTask<JSONObject, Void, RES> {

    @StringDef({ENDPOINT_PINGINDEX, ENDPOINT_CONVERSATIONRETRIEVE, ENDPOINT_MESSAGESEEN, ENDPOINT_MESSAGENEWS,
            ENDPOINT_MESSAGERETRIEVE, ENDPOINT_MESSAGESEND, ENDPOINT_EVENTTRACKING, ENDPOINT_REPORT_CRASH,
            ENDPOINT_SURVEY_SUBMIT, ENDPOINT_SURVEY_SEEN, ENDPOINT_SURVEY_BACK, ENDPOINT_SURVEY_REJECT})
    @Retention(RetentionPolicy.SOURCE)
    @interface Endpoint {}

    @IntDef({RESPONSE_STATE__PENDING, RESPONSE_STATE__OK, RESPONSE_STATE__ERROR_NO_CONNECTION,
            RESPONSE_STATE__ERROR_BAD_REQUEST, RESPONSE_STATE__ERROR_NETWORK, RESPONSE_STATE__ERROR_BAD_RESPONSE,
            RESPONSE_STATE__SERVERERROR_USER_NOT_AUTENTICATED})
    @Retention(RetentionPolicy.SOURCE)
    @interface ResponseState {}

    private static final String ENDPOINT_TRACKING = "https://tracking.customerly.io";
    static final String ENDPOINT_PINGINDEX = ENDPOINT_TRACKING + "/ping/index/";
    static final String ENDPOINT_CONVERSATIONRETRIEVE = ENDPOINT_TRACKING + "/conversation/retrieve/";
    static final String ENDPOINT_MESSAGESEEN = ENDPOINT_TRACKING + "/message/seen/";
    static final String ENDPOINT_MESSAGENEWS = ENDPOINT_TRACKING + "/message/news/";
    static final String ENDPOINT_MESSAGERETRIEVE = ENDPOINT_TRACKING + "/message/retrieve/";
    static final String ENDPOINT_MESSAGESEND = ENDPOINT_TRACKING + "/message/send/";
    static final String ENDPOINT_EVENTTRACKING = ENDPOINT_TRACKING + "/event/";
    static final String ENDPOINT_REPORT_CRASH = ENDPOINT_TRACKING + "/crash/";
    static final String ENDPOINT_SURVEY_SUBMIT = ENDPOINT_TRACKING + "/survey/submit/";
    static final String ENDPOINT_SURVEY_SEEN = ENDPOINT_TRACKING + "/survey/seen/";
    static final String ENDPOINT_SURVEY_BACK = ENDPOINT_TRACKING + "/survey/back/";
    static final String ENDPOINT_SURVEY_REJECT = ENDPOINT_TRACKING + "/survey/reject/";

    static final byte RESPONSE_STATE__PENDING = 0;
    static final byte RESPONSE_STATE__OK = -1;
    static final byte RESPONSE_STATE__ERROR_NO_CONNECTION = -2;
    static final byte RESPONSE_STATE__ERROR_BAD_REQUEST = -3;
    static final byte RESPONSE_STATE__ERROR_NETWORK = -4;
    static final byte RESPONSE_STATE__ERROR_BAD_RESPONSE = -5;
    static final int RESPONSE_STATE__SERVERERROR_USER_NOT_AUTENTICATED = 403;

    @NonNull @Endpoint private String _Endpoint;
    @NonNull private final ResponseConverter<RES> _ResponseConverter;
    @NonNull private final ResponseReceiver<RES> _ResponseReceiver;
    @IntRange(from=1, to=5) private int _Trials;

    @ResponseState private int _ResponseState = RESPONSE_STATE__PENDING;

    @RequiresPermission(Manifest.permission.INTERNET)
    private Internal_api__CustomerlyRequest(@Endpoint @NonNull String pEndpoint, @NonNull ResponseConverter<RES> pResponseConverter, @NonNull ResponseReceiver<RES> pResponseReceiver, @IntRange(from=1, to=5) int pTrials) {
        super();
        this._Endpoint = pEndpoint;
        this._ResponseConverter = pResponseConverter;
        this._ResponseReceiver = pResponseReceiver;
        this._Trials = pTrials;
    }

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

        @IntDef({View.GONE, View.INVISIBLE}) @Retention(RetentionPolicy.SOURCE) @interface HiddenVisibilityType {}
        @HiddenVisibilityType private int _ProgressView_HiddenVisibilityType;

        Builder(@Endpoint @NonNull String pEndpoint) {
            this._Endpoint = pEndpoint;
        }
        @CheckResult Builder<RES> opt_checkConn(@NonNull Context pContext) {
            this._Context = pContext;
            return this;
        }
        @CheckResult Builder<RES> opt_converter(@NonNull ResponseConverter<RES> pResponseConverter) {
            this._ResponseConverter = pResponseConverter;
            return this;
        }
        @CheckResult Builder<RES> opt_receiver(@NonNull ResponseReceiver<RES> pResponseReceiver) {
            this._ResponseReceiver = pResponseReceiver;
            return this;
        }
        @CheckResult Builder<RES> opt_trials(@IntRange(from=1, to=5) int pTrials) {
            this._Trials = pTrials;
            return this;
        }
        @CheckResult Builder<RES> opt_progressdialog(@NonNull Context pContext, @NonNull CharSequence title, @NonNull CharSequence message) {
            this._Context = pContext;
            this._ProgressDialog_Title = title;
            this._ProgressDialog_Message = message;
            return this;
        }
        @CheckResult Builder<RES> opt_progressview(@NonNull View progressView, @HiddenVisibilityType int progressView_hiddenVisibilityType) {
            this._ProgressView = new WeakReference<>(progressView);
            this._ProgressView_HiddenVisibilityType = progressView_hiddenVisibilityType;
            return this;
        }
        @CheckResult Builder<RES> opt_onPreExecute(@NonNull Runnable onPreExecute) {
            this._OnPreExecute = onPreExecute;
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
            if(Customerly._Instance._isConfigured()) {
                if (this._Context == null || Internal_Utils__Utils.checkConnection(this._Context)) {
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
                        progressView.post(() -> progressView.setVisibility(View.VISIBLE));
                    }
                    final ProgressDialog pd = pd_tmp;
                    new Internal_api__CustomerlyRequest<>(this._Endpoint,
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
                            this._Trials)
                            .execute(this._Params);
                } else if (this._ResponseReceiver != null) {
                    this._ResponseReceiver.onResponse(RESPONSE_STATE__ERROR_NO_CONNECTION, null);
                }
            }
        }
    }

    @Override
    protected final RES doInBackground(@Size(value=1) @NonNull JSONObject[] pParams) {
        JSONObject postObject;
        try {
            JSONObject settings = new JSONObject()
                    .put("app_id", Customerly._Instance._AppID)
                    .put("device", new JSONObject()
                            .put("os", "Android")
                            .put("app_name", Customerly._Instance._ApplicationName)
                            .put("app_version", Customerly._Instance._ApplicationVersionCode)
                            .put("device", String.format("%s %s (%s)", Build.MANUFACTURER, Build.MODEL, Build.DEVICE))
                            .put("os_version", Build.VERSION.SDK_INT)
                            .put("sdk_version", BuildConfig.VERSION_CODE)
                            .put("api_version", BuildConfig.CUSTOMERLY_API_VERSION)
                            .put("socket_version", BuildConfig.CUSTOMERLY_SOCKET_VERSION));

            Customerly_User user = Customerly._Instance.__USER__get();
            if(user != null) {
                user.fillSettingsJSON(settings);
            }

            postObject = new JSONObject()
                    .put("settings", settings)
                    .put("params", pParams[0])
                    .put("cookies", Customerly._Instance.__COOKIES__get());

        } catch (JSONException error) {
            this._ResponseState = RESPONSE_STATE__ERROR_BAD_REQUEST;
            Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__HTTP_REQUEST_ERROR, "Http request building error for " + this._Endpoint, error);
            return null;
        }

        while(this._Trials > 0) {
            this._ResponseState = RESPONSE_STATE__PENDING;
            OutputStream os = null;
            try {
                HttpsURLConnection conn = (HttpsURLConnection) new URL(this._Endpoint).openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                //noinspection TryWithIdenticalCatches
                try {
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, null, new java.security.SecureRandom());
                    conn.setSSLSocketFactory(sc.getSocketFactory());
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (KeyManagementException e) {
                    e.printStackTrace();
                }

                os = conn.getOutputStream();
                os.write(postObject.toString().getBytes());
                os.flush();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = br.readLine()) != null) {
                    responseStrBuilder.append(inputStr);
                }
                br.close();
                conn.disconnect();

                try {
                    JSONObject root = new JSONObject(responseStrBuilder.toString());
                    JSONObject data = root.optJSONObject("data");
                    if(data == null && root.has("data")) {
                        data = new JSONObject();
                    }
                    if (data != null) {
                        Customerly._Instance.__COOKIES__update(root.optJSONObject("cookies"));

                        JSONObject user = data.optJSONObject("user");
                        if (user != null) {
                            if (user.has("data") && !user.has("app_id")) {
                                user = user.optJSONObject("data");
                            }
                            if (user != null) {
                                Customerly._Instance.__USER__onNewUser(Customerly_User.from(user));
                            }
                        }

                        JSONObject websocket = data.optJSONObject("websocket");
                        if (websocket != null) {
                            /*
                                "websocket": {
                                  "endpoint": "https://ws2.customerly.io",
                                  "port": "8080"
                                }
                             */
                            Customerly._Instance.__SOCKET_setEndpoint(websocket.optString("endpoint", null), websocket.optString("port", null));
                        }

                        Customerly._Instance.__SOCKET__connect();

                        this._ResponseState = RESPONSE_STATE__OK;
                        return this._ResponseConverter.convert(data);
                    }
                    data = root.optJSONObject("error");
                    if(data != null) {
                        int error_code = data.optInt("code", -1);
                        if(error_code != -1) {
                            switch(error_code) {
                            case RESPONSE_STATE__SERVERERROR_USER_NOT_AUTENTICATED:
                                //{"error":{"code":403,"message":"User not authenticated"}}
                                this._ResponseState = RESPONSE_STATE__SERVERERROR_USER_NOT_AUTENTICATED;
                                return null;
                            }
                        }
                    }
                    this._ResponseState = RESPONSE_STATE__ERROR_BAD_RESPONSE;
                    Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__HTTP_RESPONSE_ERROR, "Http wrong response format for trial " + this._Trials + " of " + this._Endpoint + " -> " + responseStrBuilder.toString());
                } catch (JSONException error) {
                    this._ResponseState = RESPONSE_STATE__ERROR_BAD_RESPONSE;
                    Internal_errorhandler__CustomerlyErrorHandler.sendError(Internal_errorhandler__CustomerlyErrorHandler.ERROR_CODE__HTTP_RESPONSE_ERROR, "Http response error for for trial " + this._Trials + " of " + this._Endpoint + " -> " + responseStrBuilder.toString(), error);
                }
            } catch (IOException error) {
                this._ResponseState = RESPONSE_STATE__ERROR_NETWORK;
                Internal_errorhandler__CustomerlyErrorHandler.sendError(ERROR_CODE__IO_ERROR, "Http IOException error for for trial " + this._Trials + " of " + this._Endpoint, error);
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException ignored) { }
                }
            }
            this._Trials--;
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
