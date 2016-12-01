package io.customerly;

import android.support.annotation.NonNull;

import java.util.Locale;

/**
 * Created by Gianni on 11/09/16.
 * Project: CustomerlySDK
 */
class Internal_entity__Account {
    @NonNull static String getAccountImageUrl(long pAccountID, int pSizePX) {
        return String.format(Locale.UK, "http://pictures.cdn.customerly.io/accounts/%d/%d", pAccountID, pSizePX);
    }
    @NonNull static String getUserImageUrl(long pAccountID, int pSizePX) {
        return String.format(Locale.UK, "http://pictures.cdn.customerly.io/users/%d/%d", pAccountID, pSizePX);
    }
}
