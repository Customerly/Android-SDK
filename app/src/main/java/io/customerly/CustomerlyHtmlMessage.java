package io.customerly;

import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;

/**
 * Created by Gianni on 22/12/16.
 * Project: TestApp_CRMHero
 */

public class CustomerlyHtmlMessage extends SpannableStringBuilder {

    CustomerlyHtmlMessage(SpannableStringBuilder ssb) {
        super(ssb);
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString().replace("\uFFFC", "<IMAGE>");
    }
}
