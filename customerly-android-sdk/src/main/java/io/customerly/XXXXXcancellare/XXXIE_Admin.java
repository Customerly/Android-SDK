package io.customerly.XXXXXcancellare;

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
class XXXIE_Admin {
    private final long account_id;
    final long last_active;
    @NonNull final String name;

    @NonNull static XXXIE_Admin[] from(@Nullable JSONArray jsonArray) {
        if(jsonArray == null || jsonArray.length() == 0)
            return new XXXIE_Admin[0];
        int adminsCount = Math.min(3, jsonArray.length());//Limit to 3 Admins
        XXXIE_Admin[] adminArray = new XXXIE_Admin[adminsCount];
        int a = 0;
        for(int j = 0; j < adminsCount; j++) {
            adminArray[a] = XXXIE_Admin.from(jsonArray.optJSONObject(j));
            if(adminArray[a] != null)
                a++;
        }
        if(a == adminsCount) {
            return adminArray;
        } else { //Added less Admin than array length
            XXXIE_Admin[] newArray = new XXXIE_Admin[a];
            System.arraycopy(adminArray, 0, newArray, 0, a);
            return newArray;
        }
    }

    @Contract("null -> null")
    private static XXXIE_Admin from(@Nullable JSONObject json) {
        if(json == null)
            return null;
        try {
            return new XXXIE_Admin(json.getLong("account_id"), json.getString("name"), json.getLong("last_active"));
        } catch (JSONException e) {
            return null;
        }
    }

    private XXXIE_Admin(long pAccountId, @NonNull String pName, long pLastActive) {
        super();
        this.account_id = pAccountId;
        this.name = pName;
        this.last_active = pLastActive;
    }

    @NonNull String getImageUrl(int pSizePX) {
        return XXXIE_Account.getAccountImageUrl(this.account_id, pSizePX);
    }
}