package io.customerly;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Gianni on 11/09/16.
 * Project: CustomerlySDK
 */
class IE_Admin {
    private final long account_id;
    final long last_active;
    @NonNull final String name;

    @NonNull static IE_Admin[] from(@Nullable JSONArray jsonArray) {
        if(jsonArray == null || jsonArray.length() == 0)
            return new IE_Admin[0];
        IE_Admin[] adminArray = new IE_Admin[jsonArray.length()];
        int a = 0;
        for(int j = 0; j < jsonArray.length(); j++) {
            adminArray[a] = IE_Admin.from(jsonArray.optJSONObject(j));
            if(adminArray[a] != null)
                a++;
        }
        if(a == jsonArray.length()) {
            return adminArray;
        } else { //Added less Admin than array length
            IE_Admin[] newArray = new IE_Admin[a];
            System.arraycopy(adminArray, 0, newArray, 0, a);
            return newArray;
        }
    }

    @Contract("null -> null")
    private static IE_Admin from(@Nullable JSONObject json) {
        if(json == null)
            return null;
        try {
            //TODO Admin name scorciato da rimuovere
            String[] nameSplit = json.getString("name").split(" ");
            String name = nameSplit[0];
            if(name.length() <= 4 && nameSplit.length > 1) {
                name += " " + nameSplit[1];
            }
            return new IE_Admin(json.getLong("account_id"), name, json.getLong("last_active"));
        } catch (JSONException e) {
            return null;
        }
    }

    private IE_Admin(long pAccountId, @NonNull String pName, long pLastActive) {
        super();
        this.account_id = pAccountId;
        this.name = pName;
        this.last_active = pLastActive;
    }

    @NonNull String getImageUrl(int pSizePX) {
        return IE_Account.getAccountImageUrl(this.account_id, pSizePX);
    }
}