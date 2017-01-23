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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */
class IE_Admin {
    private final long account_id;
    final long last_active;
    @NonNull final String name;

    @NonNull static IE_Admin[] from(@Nullable JSONArray jsonArray) {
        if(jsonArray == null || jsonArray.length() == 0)
            return new IE_Admin[0];
        int adminsCount = Math.min(3, jsonArray.length());//Limit to 3 Admins
        IE_Admin[] adminArray = new IE_Admin[adminsCount];
        int a = 0;
        for(int j = 0; j < adminsCount; j++) {
            adminArray[a] = IE_Admin.from(jsonArray.optJSONObject(j));
            if(adminArray[a] != null)
                a++;
        }
        if(a == adminsCount) {
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