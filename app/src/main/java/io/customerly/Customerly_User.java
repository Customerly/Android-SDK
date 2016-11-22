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
    private final boolean is_user;
    final long customerly_user_id;
    private final long user_id;
    @Nullable private final String email;
    @Nullable private final String name;
    @Nullable private JSONObject custom_data;
    private Customerly_User(boolean isUser, long customerly_user_id, long user_id, @Nullable String email, @Nullable String name) {
        this(isUser, customerly_user_id, user_id, email, name, null);
    }
    public Customerly_User(boolean isUser, long customerly_user_id, long user_id, @Nullable String email, @Nullable String name, @Nullable JSONObject custom_data) {
        super();
        this.is_user = isUser;
        this.customerly_user_id = customerly_user_id;
        this.user_id = user_id;
        this.email = email;
        this.name = name;
        this.custom_data = custom_data;
    }

    static Customerly_User from(@NonNull JSONObject pUserData) {
        long crmhero_user_id = pUserData.optLong("customerly_user_id"),
                user_id = pUserData.optLong("user_id");
        return crmhero_user_id == 0 && user_id == 0 ? null : new Customerly_User(pUserData.optInt("is_user") == 1, crmhero_user_id, user_id, pUserData.optString("email"), pUserData.optString("name"));
    }

    static Customerly_User from(@NonNull SharedPreferences pPrefs) {
        long crmhero_user_id = pPrefs.getLong("customerly_user_id", 0),
                user_id = pPrefs.getLong("user_id", 0);
        JSONObject customData = null;
        try {
            String customDataString = pPrefs.getString("custom_data", null);
            if(customDataString != null) {
                customData = new JSONObject(pPrefs.getString("custom_data", null));
            }
        } catch (JSONException ignored) { }
        return crmhero_user_id == 0 && user_id == 0 ? null : new Customerly_User(pPrefs.getBoolean("is_user", true), crmhero_user_id, user_id, pPrefs.getString("email", null), pPrefs.getString("name", null), customData);
    }

    void store(@NonNull SharedPreferences pPrefs) {
        pPrefs.edit()
                .putBoolean("is_user", this.is_user)
                .putLong("customerly_user_id", this.customerly_user_id)
                .putLong("user_id", this.user_id)
                .putString("email", this.email)
                .putString("name", this.name)
                .putString("custom_data", this.custom_data == null ? null : this.custom_data.toString())
                .apply();
    }

    void fillSettingsJSON(@NonNull JSONObject pSettingsJSON) {
        if(this.is_user) {
            if (this.customerly_user_id != 0) {
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
            if (this.custom_data != null && this.custom_data.length() != 0) {
                try {
                    pSettingsJSON.put("custom_data", this.custom_data);
                } catch (JSONException ignored) { }
            }
        }
    }
}