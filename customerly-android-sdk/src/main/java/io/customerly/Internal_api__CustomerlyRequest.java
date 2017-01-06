package io.customerly;

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
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

class Internal_api__CustomerlyRequest<RES> extends AsyncTask<JSONObject, Void, RES> {

    @StringDef({ENDPOINT_PING, ENDPOINT_CONVERSATIONRETRIEVE, ENDPOINT_MESSAGESEEN, ENDPOINT_MESSAGENEWS,
            ENDPOINT_MESSAGERETRIEVE, ENDPOINT_MESSAGESEND, ENDPOINT_EVENTTRACKING, ENDPOINT_REPORT_CRASH,
            ENDPOINT_SURVEY_SUBMIT, ENDPOINT_SURVEY_SEEN, ENDPOINT_SURVEY_BACK, ENDPOINT_SURVEY_REJECT})
    @Retention(RetentionPolicy.SOURCE)
    @interface Endpoint {}

    @IntDef({RESPONSE_STATE__PENDING, RESPONSE_STATE__OK, RESPONSE_STATE__ERROR_NO_CONNECTION,
            RESPONSE_STATE__ERROR_BAD_REQUEST, RESPONSE_STATE__ERROR_NETWORK, RESPONSE_STATE__ERROR_BAD_RESPONSE,
            RESPONSE_STATE__SERVERERROR_USER_NOT_AUTENTICATED, RESPONSE_STATE__NO_TOKEN_AVAILABLE})
    @Retention(RetentionPolicy.SOURCE)
    @interface ResponseState {}

    private static final String ENDPOINT_TRACKING_BASEURL = "https://a011ca30.ngrok.io";//TODO "https://tracking.customerly.io";
    private static final String ENDPOINT_TRACKING_API_VERSION = "/v1";
    static final String ENDPOINT_PING = ENDPOINT_TRACKING_BASEURL + ENDPOINT_TRACKING_API_VERSION + "/ping/index/";
    static final String ENDPOINT_CONVERSATIONRETRIEVE = ENDPOINT_TRACKING_BASEURL + ENDPOINT_TRACKING_API_VERSION + "/conversation/retrieve/";
    static final String ENDPOINT_MESSAGESEEN = ENDPOINT_TRACKING_BASEURL + ENDPOINT_TRACKING_API_VERSION + "/message/seen/";
    static final String ENDPOINT_MESSAGENEWS = ENDPOINT_TRACKING_BASEURL + ENDPOINT_TRACKING_API_VERSION + "/message/news/";
    static final String ENDPOINT_MESSAGERETRIEVE = ENDPOINT_TRACKING_BASEURL + ENDPOINT_TRACKING_API_VERSION + "/message/retrieve/";
    static final String ENDPOINT_MESSAGESEND = ENDPOINT_TRACKING_BASEURL + ENDPOINT_TRACKING_API_VERSION + "/message/send/";
    static final String ENDPOINT_EVENTTRACKING = ENDPOINT_TRACKING_BASEURL + ENDPOINT_TRACKING_API_VERSION + "/event/";
    static final String ENDPOINT_REPORT_CRASH = ENDPOINT_TRACKING_BASEURL + ENDPOINT_TRACKING_API_VERSION + "/crash/";
    static final String ENDPOINT_SURVEY_SUBMIT = ENDPOINT_TRACKING_BASEURL + ENDPOINT_TRACKING_API_VERSION + "/survey/submit/";//TODO Testare
    static final String ENDPOINT_SURVEY_SEEN = ENDPOINT_TRACKING_BASEURL + ENDPOINT_TRACKING_API_VERSION + "/survey/seen/";
    static final String ENDPOINT_SURVEY_BACK = ENDPOINT_TRACKING_BASEURL + ENDPOINT_TRACKING_API_VERSION + "/survey/back/";
    static final String ENDPOINT_SURVEY_REJECT = ENDPOINT_TRACKING_BASEURL + ENDPOINT_TRACKING_API_VERSION + "/survey/reject/";

    @SuppressWarnings("WeakerAccess") static final byte RESPONSE_STATE__PENDING = 0;
    @SuppressWarnings("WeakerAccess") static final byte RESPONSE_STATE__OK = -1;
    @SuppressWarnings("WeakerAccess") static final byte RESPONSE_STATE__ERROR_NO_CONNECTION = -2;
    @SuppressWarnings("WeakerAccess") static final byte RESPONSE_STATE__ERROR_BAD_REQUEST = -3;
    @SuppressWarnings("WeakerAccess") static final byte RESPONSE_STATE__ERROR_NETWORK = -4;
    @SuppressWarnings("WeakerAccess") static final byte RESPONSE_STATE__ERROR_BAD_RESPONSE = -5;
    @SuppressWarnings("WeakerAccess") static final int RESPONSE_STATE__SERVERERROR_USER_NOT_AUTENTICATED = 403;
    @SuppressWarnings("WeakerAccess") static final int RESPONSE_STATE__NO_TOKEN_AVAILABLE = -6;

    @NonNull @Endpoint private final String _Endpoint;
    @NonNull private final ResponseConverter<RES> _ResponseConverter;
    @NonNull private final ResponseReceiver<RES> _ResponseReceiver;
    @IntRange(from=1, to=5) private int _Trials;
    private final boolean _TokenMandatory;

    @ResponseState private int _ResponseState = RESPONSE_STATE__PENDING;

    @RequiresPermission(Manifest.permission.INTERNET)
    private Internal_api__CustomerlyRequest(@Endpoint @NonNull String pEndpoint, @NonNull ResponseConverter<RES> pResponseConverter, @NonNull ResponseReceiver<RES> pResponseReceiver, @IntRange(from=1, to=5) int pTrials, boolean pTokenMandatory) {
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
        @CheckResult Builder<RES> opt_progressdialog(@NonNull Context pContext, @NonNull CharSequence title, @NonNull CharSequence message) {
            this._Context = pContext;
            this._ProgressDialog_Title = title;
            this._ProgressDialog_Message = message;
            return this;
        }
        @CheckResult Builder<RES> opt_progressview(@NonNull View progressView, @SuppressWarnings("SameParameterValue") @HiddenVisibilityType int progressView_hiddenVisibilityType) {
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
                        View parent = (View) progressView.getParent();
                        if(parent != null) {//Trick, if i post the runnable on the progressView with visibility gone, it will be never called
                            parent.post(() -> progressView.setVisibility(View.VISIBLE));
                        }
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
                            this._Trials, this._TokenMandatory)
                            .execute(this._Params);
                } else if (this._ResponseReceiver != null) {
                    Customerly._Instance._log("Check your connection");
                    this._ResponseReceiver.onResponse(RESPONSE_STATE__ERROR_NO_CONNECTION, null);
                }
            }
        }
    }


    @NonNull private JSONObject json_appid_E_device(@NonNull String app_id, @Nullable JSONObject params) throws JSONException {
        return params != null ? params : new JSONObject().put("app_id", app_id).put("device", Customerly._Instance.__PING__DeviceJSON);
    }

    @Nullable
    @Override
    protected final RES doInBackground(@Size(value=1) @NonNull JSONObject[] pParams) {
        String app_id = Customerly._Instance._AppID;
        if(app_id == null) {
            return null;
        }
        JSONObject request_root = new JSONObject();

        Internal__JWTtoken token = Customerly._Instance._JWTtoken;
        boolean tokenSent = false;
        if(token != null) {
            try {
                request_root.put(Internal__JWTtoken.PAYLOAD_KEY, token.toString());
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
                if(this._TokenMandatory) {//Se non sto inviando il token ma è necessario prima effettuo una ping per ottenerlo se non lo ottengo killo la request
                    try {
                        this.executeRequest(ENDPOINT_PING, new JSONObject().put("params", this.json_appid_E_device(app_id, null)));
                    } catch (JSONException error) {
                        this._ResponseState = RESPONSE_STATE__NO_TOKEN_AVAILABLE;
                        return null;
                    }
                    token = Customerly._Instance._JWTtoken;
                    if (token != null) {
                        try {
                            request_root.put(Internal__JWTtoken.PAYLOAD_KEY, token.toString());
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
                } else {//Se Non sto inviando il token ma non è mandatory mando però l'app_id e le device infos
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

    @Nullable private JSONObject executeRequest(@NonNull String pEndpoint, @NonNull JSONObject pJSONpayload) {
        int trials = this._Trials;
        while(trials > 0) {
            this._ResponseState = RESPONSE_STATE__PENDING;
            OutputStream os = null;
            try {
                HttpsURLConnection conn = (HttpsURLConnection) new URL(pEndpoint).openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept-Language", Locale.getDefault().toString());//es: "it_IT"

                if(BuildConfig.CUSTOMERLY_DEV_MODE) {
                    String postObjectToString;
                    try {
                        postObjectToString = pJSONpayload.toString(4);
                    } catch (JSONException error) {
                        postObjectToString = "Malformed JSON";
                    }
                    //noinspection ConstantConditions
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
                os.write(pJSONpayload.toString().getBytes());
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
                    JSONObject response_root = new JSONObject(responseStrBuilder.toString());

                    if(BuildConfig.CUSTOMERLY_DEV_MODE) {
                        String rootToString;
                        try {
                            rootToString = response_root.toString(1);
                        } catch (JSONException error) {
                            rootToString = "Malformed JSON";
                        }
                        Log.e(BuildConfig.CUSTOMERLY_SDK_NAME,
                                "-----------------------------------------------------------" +
                                        "\nHTTP RESPONSE" +
                                        "\n+ Endpoint:        " + this._Endpoint +
                                        "\nJSON BODY:\n" +
                                        rootToString +
                                        "\n-----------------------------------------------------------");

                    }

                    if(response_root.has("error")) {
                        /*  {   "error": "exception_title",
                                "message": "Exception_message",
                                "code": "ExceptionCode"     }   */
                        int error_code = response_root.optInt("code", -1);
                        Customerly._Instance._log(String.format(Locale.UK, "Message: %s ErrorCode: %s",
                                Internal_Utils__Utils.jsonOptStringWithNullCheck(response_root, "message", "The server received the request but an error has come"),
                                error_code));
                        if(error_code != -1) {
                            switch(error_code) {
                                case RESPONSE_STATE__SERVERERROR_USER_NOT_AUTENTICATED:
                                    //{"error": "..title..", "message":"User not authenticated", "code":403}
                                    this._ResponseState = RESPONSE_STATE__SERVERERROR_USER_NOT_AUTENTICATED;
                                    return null;
                            }
                        }
                    }

                    this._ResponseState = RESPONSE_STATE__OK;
                    if(ENDPOINT_PING.equals(pEndpoint)) {
                        Customerly._Instance._TOKEN__update(response_root);
                    }
                    return response_root;
                } catch (JSONException error) {
                    Customerly._Instance._log("The server received the request but an error has come");
                    this._ResponseState = RESPONSE_STATE__ERROR_BAD_RESPONSE;
                    return null;
                }
            } catch (IOException error) {
                Customerly._Instance._log("An error occours during the connection to server");
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
                            "\n+ Endpoint:        " + this._Endpoint +
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
