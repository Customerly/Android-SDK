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

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;

import org.json.JSONException;
import org.json.JSONObject;

import io.customerly.R;

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */
class XXXIE_Conversation {

    private static final int /*WRITER_TYPE__USER = 1,*/ WRITER_TYPE__ACCOUNT = 0;

    boolean unread;
    private Spanned last_message_abstract;
    private long last_message_writer, last_message_writer_type;
    long last_message_date;
    @Nullable private String last_account__name;

    final long conversation_id;

    XXXIE_Conversation(long pConversationID, @Nullable Spanned pLastMessageAbstract, long pLastMessageDate, long pLastMessageWriterID, int pLastMessageWriterType, @Nullable String pLastAccountName) {
        super();
        this.conversation_id = pConversationID;
        this.last_message_abstract = pLastMessageAbstract;

        this.last_message_date = pLastMessageDate;
        this.last_message_writer = pLastMessageWriterID;
        this.last_message_writer_type = pLastMessageWriterType;

        this.unread = true;

        this.last_account__name = pLastAccountName;
    }

    XXXIE_Conversation(@NonNull JSONObject pConversationItem) throws JSONException {
        super();

        this.conversation_id = pConversationItem.getLong("conversation_id");
        this.last_message_abstract = XXXIU_Utils.fromHtml(pConversationItem.getString("last_message_abstract"), null, null);

        this.last_message_date = pConversationItem.getLong("last_message_date");
        this.last_message_writer = pConversationItem.optLong("last_message_writer");
        this.last_message_writer_type = pConversationItem.optInt("last_message_writer_type");

        this.unread = pConversationItem.optInt("unread", 0) == 1;

        pConversationItem = pConversationItem.optJSONObject("last_account");
        this.last_account__name = pConversationItem == null ? null : XXXIU_Utils.jsonOptStringWithNullCheck(pConversationItem, "name");
    }

    void onNewMessage(@NonNull XXXIE_Message pNewMessage) {
        this.last_message_abstract = pNewMessage.content_abstract;
        this.last_message_date = pNewMessage.sent_datetime_sec;
        this.last_message_writer = pNewMessage.getWriterID();
        this.last_message_writer_type = pNewMessage.getWriterType();
        this.unread = true;
        this.last_account__name = pNewMessage.if_account__name;
    }

    @NonNull String getImageUrl(int pPixelSize) {
        return this.last_message_writer_type == WRITER_TYPE__ACCOUNT
            ? XXXIE_Account.getAccountImageUrl(this.last_message_writer, pPixelSize)
            : XXXIE_Account.getUserImageUrl(this.last_message_writer, pPixelSize);
    }

    @NonNull String getConversationLastWriter(@NonNull Context pContext) {
        return this.last_account__name != null ? this.last_account__name : pContext.getString(R.string.io_customerly__you);
    }

    @Nullable Spanned getLastMessage() {
        return this.last_message_abstract;
    }

    @NonNull String getFormattedLastMessageTime(@NonNull Resources resources) {
        return XXXIU_TimeAgoUtils.calculate(this.last_message_date,
                seconds -> resources.getString(R.string.io_customerly__XX_sec_ago, seconds),
                minutes -> resources.getString(R.string.io_customerly__XX_min_ago, minutes),
                hours -> resources.getQuantityString(R.plurals.io_customerly__XX_hours_ago, (int)hours, hours),
                days -> resources.getQuantityString(R.plurals.io_customerly__XX_days_ago, (int)days, days),
                weeks -> resources.getQuantityString(R.plurals.io_customerly__XX_weeks_ago, (int)weeks, weeks),
                months -> resources.getQuantityString(R.plurals.io_customerly__XX_months_ago, (int)months, months));
    }

}
