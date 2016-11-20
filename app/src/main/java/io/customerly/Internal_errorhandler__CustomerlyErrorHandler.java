package io.customerly;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static io.customerly.Internal_api__CustomerlyRequest.ENDPOINT_REPORT_CRASH;

/**
 * Created by Gianni on 09/09/16.
 * Project: CustomerlySDK
 */
class Internal_errorhandler__CustomerlyErrorHandler {
    @IntDef({ERROR_CODE__CUSTOMERLY_IS_NULL, ERROR_CODE__IO_ERROR, ERROR_CODE__HTTP_REQUEST_ERROR,
            ERROR_CODE__HTTP_RESPONSE_ERROR, ERROR_CODE__GLIDE_ERROR, ERROR_CODE__ATTACHMENT_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    @interface ErrorCode {}

    @ErrorCode static final int ERROR_CODE__CUSTOMERLY_IS_NULL = 1;
    @ErrorCode static final int ERROR_CODE__IO_ERROR = 2;
    @ErrorCode static final int ERROR_CODE__HTTP_REQUEST_ERROR = 3;
    @ErrorCode static final int ERROR_CODE__HTTP_RESPONSE_ERROR = 4;
    @ErrorCode static final int ERROR_CODE__GLIDE_ERROR = 5;
    @ErrorCode static final int ERROR_CODE__ATTACHMENT_ERROR = 6;

    static void sendError(@ErrorCode int pErrorCode, @NonNull String pDescription) {
        Internal_errorhandler__CustomerlyErrorHandler.sendError(pErrorCode, pDescription, null);
    }
    static void sendError(@ErrorCode int pErrorCode, @NonNull String pDescription, @Nullable Throwable pThrowable) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stacktrace = pThrowable != null ? pThrowable.getStackTrace() : Thread.currentThread().getStackTrace();
        for(StackTraceElement sse : stacktrace) {
            sb.append(sse.toString()).append('\n');
        }
        sb.setLength(sb.length() - 1);

        Customerly._do(crm ->
                new Internal_api__CustomerlyRequest.Builder<Void>(ENDPOINT_REPORT_CRASH)
                .opt_crm(crm)
                /*.param("os", "android")
                .param("os_version", Build.VERSION.SDK_INT)
                .param("sdk_version", BuildConfig.VERSION_CODE)
                .param("app_id", crm._AppID)
                .param("app_version", crm._ApplicationVersionCode)*/
                .param("error_code", pErrorCode)
                .param("error_message", pDescription)
                .param("fullstacktrace", sb.toString())
                .start());

        if(Customerly.isVerboseLogging()) {
            Log.e("CRMHero error sent", "code: " + pErrorCode + " ||| message: " + pDescription + " ||| stack:\n" + sb.toString());
        }
    }
}
