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
public class Customerly_User {
    static final long UNKNOWN_CUSTOMERLY_USER_ID = 0;
    private final boolean is_user;
    final long customerly_user_id;
    private final long user_id;
    @Nullable private final String email;
    @Nullable private final String name;

    Customerly_User(boolean isUser, long customerly_user_id, long user_id, @Nullable String email, @Nullable String name) {
        super();
        this.is_user = isUser;
        this.customerly_user_id = customerly_user_id;
        this.user_id = user_id;
        this.email = email;
        this.name = name;
    }

    static Customerly_User from(@NonNull JSONObject pUserData) {
        long crmhero_user_id = pUserData.optLong("customerly_user_id"),
                user_id = pUserData.optLong("user_id");
        return crmhero_user_id == 0 && user_id == 0 ? null : new Customerly_User(pUserData.optInt("is_user") == 1, crmhero_user_id, user_id, pUserData.optString("email"), pUserData.optString("name"));
    }

    static Customerly_User from(@NonNull SharedPreferences pPrefs) {
        long crmhero_user_id = pPrefs.getLong("customerly_user_id", 0),
                user_id = pPrefs.getLong("user_id", 0);
        return crmhero_user_id == 0 && user_id == 0 ? null : new Customerly_User(pPrefs.getBoolean("is_user", true), crmhero_user_id, user_id, pPrefs.getString("email", null), pPrefs.getString("name", null));
    }

    void store(@NonNull SharedPreferences pPrefs) {
        pPrefs.edit()
                .putBoolean("is_user", this.is_user)
                .putLong("customerly_user_id", this.customerly_user_id)
                .putLong("user_id", this.user_id)
                .putString("email", this.email)
                .putString("name", this.name)
                .apply();
    }

    void fillSettingsJSON(@NonNull JSONObject pSettingsJSON) {
        if(this.is_user) {
            if (this.customerly_user_id != UNKNOWN_CUSTOMERLY_USER_ID) {
                try {
                    pSettingsJSON.put("customerly_user_id", this.customerly_user_id);
                } catch (JSONException ignored) { }
            }
            if (this.user_id != 0) {
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
                .remove("customerly_user_id")
                .remove("user_id")
                .remove("email")
                .remove("name")
                .apply();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other != null && other instanceof Customerly_User && ((Customerly_User)other).customerly_user_id == this.customerly_user_id;
    }
}