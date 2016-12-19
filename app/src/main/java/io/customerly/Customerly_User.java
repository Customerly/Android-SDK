package io.customerly;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Gianni on 31/05/16.
 * Project: CustomerlySDK
 */
class Customerly_User {
    static final long UNKNOWN_CUSTOMERLY_USER_ID = 0;
    private final boolean is_user;
    final long internal_user_id;
    private final String user_id;
    @Nullable private final String email;
    @Nullable private final String name;

    Customerly_User(boolean isUser, long internal_user_id, String user_id, @Nullable String email, @Nullable String name) {
        super();
        this.is_user = isUser;
        this.internal_user_id = internal_user_id;
        this.user_id = user_id;
        this.email = email;
        this.name = name;
    }

    @Nullable static Customerly_User from(@NonNull JSONObject pUserData) {
        long internal_user_id = pUserData.optLong("internal_user_id");
        if(internal_user_id == 0) {//TODO per ora è crmhero_user_id ma dovrebbe refactorizzare a internal_user_id
            internal_user_id = pUserData.optLong("crmhero_user_id");
        }
        String user_id = pUserData.optString("user_id", null);
        return internal_user_id == 0 && user_id == null ? null : new Customerly_User(pUserData.optInt("is_user") == 1, internal_user_id, user_id, pUserData.optString("email"), pUserData.optString("name"));
    }

    @Nullable static Customerly_User from(@NonNull SharedPreferences pPrefs) {
        long crmhero_user_id = pPrefs.getLong("internal_user_id", 0);
        String user_id = pPrefs.getString("user_id", null);
        return crmhero_user_id == 0 && user_id == null ? null : new Customerly_User(pPrefs.getBoolean("is_user", true), crmhero_user_id, user_id, pPrefs.getString("email", null), pPrefs.getString("name", null));
    }

    void store(@NonNull SharedPreferences pPrefs) {
        pPrefs.edit()
                .putBoolean("is_user", this.is_user)
                .putLong("internal_user_id", this.internal_user_id)
                .putString("user_id", this.user_id)
                .putString("email", this.email)
                .putString("name", this.name)
                .apply();
    }

    void fillSettingsJSON(@NonNull JSONObject pSettingsJSON) {
        if(this.is_user) {
            if (this.user_id != null) {
                try {
                    pSettingsJSON.put("user_id", this.user_id);
                } catch (JSONException ignored) { }
            }
            if (!TextUtils.isEmpty(this.email)) {
                try {
                    pSettingsJSON.put("email", this.email);
                } catch (JSONException ignored) { }
            }
            if (!TextUtils.isEmpty(this.name)) {
                try {
                    pSettingsJSON.put("name", this.name);
                } catch (JSONException ignored) { }
            }
        }
    }

    void delete(@NonNull SharedPreferences pPrefs) {
        pPrefs.edit()
                .remove("is_user")
                .remove("internal_user_id")
                .remove("user_id")
                .remove("email")
                .remove("name")
                .apply();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other != null && other instanceof Customerly_User && ((Customerly_User)other).internal_user_id == this.internal_user_id;
    }
}