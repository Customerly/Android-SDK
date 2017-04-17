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

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;
import android.text.SpannedString;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.customerly.commons.Handle;

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */
class IE_Message {

    private static final SimpleDateFormat _TIME_FORMATTER = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final DateFormat _DATE_FORMATTER = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG);

    final long conversation_id, conversation_message_id, sent_datetime_sec;
    private final long user_id, account_id, seen_date;

    @Nullable final String content;
    @Nullable private Spanned content_Spanned;
    @Nullable final Spanned content_abstract;
    @Nullable final String if_account__name, rich_mail_link;
    @NonNull final String dateString, timeString;
    @Nullable final IE_Attachment[] _Attachments;

    boolean sameSentDayOf(@NonNull IE_Message other) {
        return (this.sent_datetime_sec / (/*1000**/60 * 60 * 24)) == (other.sent_datetime_sec / (/*1000**/60 * 60 * 24));
    }

    private enum STATE {    COMPLETE, SENDING, FAILED   }
    @NonNull private STATE _STATE = STATE.COMPLETE;

    IE_Message(long pCustomerly_User_ID, long pConversationID, @NonNull String pContent, @Nullable final IE_Attachment[] pAttachments) {
        super();
        this._STATE = STATE.SENDING;
        this.user_id = pCustomerly_User_ID;
        this.conversation_id = pConversationID;
        this.conversation_message_id = 0;
        this.account_id = 0;
        this.sent_datetime_sec = this.seen_date = System.currentTimeMillis() / 1000;
        this.dateString = _DATE_FORMATTER.format(new Date(this.sent_datetime_sec * 1000));
        this.timeString = _TIME_FORMATTER.format(new Date(this.sent_datetime_sec * 1000));
        this.content = pContent;
        this.content_Spanned= IU_Utils.fromHtml(pContent, null, null);
        this.content_abstract = pContent.length() != 0 ? IU_Utils.fromHtml(pContent, null, null) : new SpannedString(pAttachments != null && pAttachments.length != 0 ? "[Attachment]" : "");
        this._Attachments = pAttachments;
        this.if_account__name = null;
        this.rich_mail_link = null;
    }

    @NonNull Spanned getContentSpanned(@NonNull TextView tv, @NonNull Handle.NoNull.Two<Activity, String> pImageClickableSpan) {
        if(this.content_Spanned == null) {
            this.content_Spanned = IU_Utils.fromHtml(this.content, tv, pImageClickableSpan);
        }
        return this.content_Spanned;
    }

    IE_Message(@NonNull JSONObject pMessageItem) {
        super();
        this._STATE = STATE.COMPLETE;
        this.user_id = pMessageItem.optLong("user_id", 0);
        this.conversation_id = pMessageItem.optLong("conversation_id", 0);
        this.conversation_message_id = pMessageItem.optLong("conversation_message_id", 0);
        this.account_id = pMessageItem.optLong("account_id", 0);
        this.seen_date = pMessageItem.optLong("seen_date", 0);
        this.sent_datetime_sec = pMessageItem.optLong("sent_date", 0);
        this.dateString = _DATE_FORMATTER.format(new Date(this.sent_datetime_sec * 1000));
        this.timeString = _TIME_FORMATTER.format(new Date(this.sent_datetime_sec * 1000));
        this.content = IU_Utils.jsonOptStringWithNullCheck(pMessageItem, "content", "");
        this.content_abstract = IU_Utils.fromHtml(IU_Utils.jsonOptStringWithNullCheck(pMessageItem, "abstract", ""), null, null);
        this.rich_mail_link = pMessageItem.optInt("rich_mail", 0) == 0 ? null : IU_Utils.jsonOptStringWithNullCheck(pMessageItem, "rich_mail_link");

        JSONArray attachments = pMessageItem.optJSONArray("attachments");
        if(attachments != null && attachments.length() != 0) {
            IE_Attachment[] attachments_tmp = new IE_Attachment[attachments.length()];
            int i = 0, j = 0;
            for(; i < attachments.length(); i++) {
                JSONObject item = attachments.optJSONObject(i);
                if(item != null) {
                    try {
                        attachments_tmp[j] = new IE_Attachment(item);
                        j++;
                    } catch (JSONException ignored) { }
                }
            }
            if(j == i) {
                this._Attachments = attachments_tmp;
            } else {
                this._Attachments = new IE_Attachment[j];
                System.arraycopy(attachments_tmp, 0, this._Attachments, 0, j);
            }
        } else {
            this._Attachments = null;
        }

        JSONObject account = pMessageItem.optJSONObject("account");
        if(account != null) {
            this.if_account__name = IU_Utils.jsonOptStringWithNullCheck(account, "name");
        } else {
            this.if_account__name = null;
        }
    }

    long getWriterID() {
        return this.getWriterType() == 1 ? this.user_id : this.account_id;
    }

    int getWriterType() {
        return this.user_id != 0 ? 1 : 0;
    }

    @NonNull String getImageUrl(int pPixelSize) {
        return this.account_id != 0 ? IE_Account.getAccountImageUrl(this.account_id, pPixelSize) : IE_Account.getUserImageUrl(this.user_id, pPixelSize);
    }

    boolean isUserMessage() {
        return this.account_id == 0;
    }

    boolean isNotSeen() {
        return (!this.isUserMessage()) && this.seen_date == 0;
    }

    boolean hasSameSenderOf(@Nullable IE_Message pPreviousMessage) {
        if(pPreviousMessage == null)
            return false;
        if(this.isUserMessage()) {
            return pPreviousMessage.isUserMessage();
        } else {
            return this.account_id == pPreviousMessage.account_id;
        }
    }

    boolean isSending() {
        return this._STATE == STATE.SENDING;
    }
    void setSending() {
        this._STATE = STATE.SENDING;
    }
    void setFailed() {
        this._STATE = STATE.FAILED;
    }
    boolean isFailed() {
        return this._STATE == STATE.FAILED;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof IE_Message && ((IE_Message)obj).conversation_message_id == this.conversation_message_id;
    }
}
