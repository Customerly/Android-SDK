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
class Internal_entity__Admin {
    private final long account_id;
    final long last_active;
    @NonNull final String name;

    @NonNull static Internal_entity__Admin[] from(@Nullable JSONArray jsonArray) {
        if(jsonArray == null || jsonArray.length() == 0)
            return new Internal_entity__Admin[0];
        Internal_entity__Admin[] adminArray = new Internal_entity__Admin[jsonArray.length()];
        int a = 0;
        for(int j = 0; j < jsonArray.length(); j++) {
            adminArray[a] = Internal_entity__Admin.from(jsonArray.optJSONObject(j));
            if(adminArray[a] != null)
                a++;
        }
        if(a == jsonArray.length()) {
            return adminArray;
        } else { //Ho aggiunto meno Admin all'array rispetto alla lunghezza
            Internal_entity__Admin[] nuovoArray = new Internal_entity__Admin[a];
            System.arraycopy(adminArray, 0, nuovoArray, 0, a);
            return nuovoArray;
        }
    }

    @Contract("null -> null")
    private static Internal_entity__Admin from(@Nullable JSONObject json) {
        if(json == null)
            return null;
        try {
            return new Internal_entity__Admin(json.getLong("account_id"), json.getString("name"), json.getLong("last_active"));
        } catch (JSONException e) {
            return null;
        }
    }

    private Internal_entity__Admin(long pAccountId, @NonNull String pName, long pLastActive) {
        super();
        this.account_id = pAccountId;
        this.name = pName;
        this.last_active = pLastActive;
    }

    @NonNull String getImageUrl(int pSizePX) {
        return Internal_entity__Account.getAccountImageUrl(this.account_id, pSizePX);
    }
}