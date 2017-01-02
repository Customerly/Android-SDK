package io.customerly;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.util.Base64;

import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Contract;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Gianni on 11/09/16.
 * Project: CustomerlySDK
 */
class Internal__jwt_token {
    @NonNull private static final String TOKEN_VALIDATOR_MATCHER = "([^.]+)\\.([^.]+)\\.([^.]+)";
    @NonNull private static final Pattern TOKEN_PAYLOAD_MATCHER = Pattern.compile("\\.(.*?)\\.");
    @NonNull private static final String PREFS_TOKEN_KEY = "PREFS_TOKEN_KEY";
    @NonNull static final String PAYLOAD_KEY = "token";

    static final int USER_TYPE__ANONYMOUS = 0b00000001; //hex 0x01 dec: 1
    static final int USER_TYPE__LEAD = 0b00000010; //hex 0x02 dec: 2
    static final int USER_TYPE__USER = 0b00000100; //hex 0x04 dec: 4

    @NonNull private final String _EncodedToken;
    @Nullable final Long _UserID;
    final int _UserType;
    Internal__jwt_token(@org.intellij.lang.annotations.Pattern(TOKEN_VALIDATOR_MATCHER) @Size(min = 5) @NonNull String pEncodedToken) throws IllegalArgumentException {
        super();
        this._EncodedToken = pEncodedToken;

        if(! this._EncodedToken.matches(TOKEN_VALIDATOR_MATCHER)) {
            throw new IllegalArgumentException(String.format("Wrong token format. Token: %s does not match the regex %s", pEncodedToken, TOKEN_VALIDATOR_MATCHER));
        }
        JSONObject payloadJSON = null;
        try {
            Matcher matcher = TOKEN_PAYLOAD_MATCHER.matcher(pEncodedToken);
            if (matcher.find()) {
                payloadJSON = new JSONObject(
                        new String(Base64.decode(matcher.group(1), Base64.DEFAULT), "UTF-8"));
            }
        } catch (Exception ignored) { }

        if(payloadJSON != null) {
            long tmpUserID = payloadJSON.optLong("id", -1L);
            this._UserID = tmpUserID == -1L ? null : tmpUserID;
            this._UserType = payloadJSON.optInt("type", USER_TYPE__ANONYMOUS);
        } else {
            this._UserID = null;
            this._UserType = USER_TYPE__ANONYMOUS;
        }
    }

    Internal__jwt_token(@Subst("authB64.payloadB64.checksumB64") @org.intellij.lang.annotations.Pattern(TOKEN_VALIDATOR_MATCHER) @Size(min = 5) @NonNull String pEncodedToken, @NonNull SharedPreferences prefs) {
        this(pEncodedToken);
        prefs.edit().putString(PREFS_TOKEN_KEY, pEncodedToken).apply();
    }

    @Override
    @Contract(pure=true)
    public String toString() {
        return this._EncodedToken;
    }

    boolean isUser() {
        return (USER_TYPE__USER & _UserType) != 0;
    }

    boolean isLead() {
        return (USER_TYPE__LEAD & _UserType) != 0;
    }

    boolean isAnonymous() {
        return (USER_TYPE__ANONYMOUS & _UserType) != 0;
    }

    @Nullable
    public static Internal__jwt_token from(@NonNull SharedPreferences prefs) {
        @Subst("authB64.payloadB64.checksumB64") String tokenFromPrefs = prefs.getString(PREFS_TOKEN_KEY, null);
        if(tokenFromPrefs != null) {
            try {
                return new Internal__jwt_token(tokenFromPrefs);
            } catch (IllegalArgumentException wrongTokenFormat) {
                Internal__jwt_token.remove(prefs);
            }
        }
        return null;
    }

    static void remove(@NonNull SharedPreferences prefs) {
        prefs.edit().remove(PREFS_TOKEN_KEY).apply();
    }
}