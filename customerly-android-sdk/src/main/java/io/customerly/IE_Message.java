package io.customerly;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Gianni on 11/09/16.
 * Project: CustomerlySDK
 */
class IE_Message {

    final long conversation_id, conversation_message_id, sent_date, assigner_id;
    private final long user_id, account_id, seen_date;

    @Nullable final Customerly.HtmlMessage content;
    @Nullable final String if_account__name, rich_mail_url;
    @Nullable final IE_Attachment[] _Attachments;

    @Nullable private String _Cached__toStringDate = null;
    @NonNull String toStringDate(long pTODAY_inSec) {
        if(this._Cached__toStringDate == null) {
            this._Cached__toStringDate =
            (pTODAY_inSec == (this.sent_date / (/*1000**/60 * 60 * 24)) * (/*1000*/ 60 * 60 * 24)
                    ? SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.SHORT) : SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG))
                    .format(new Date(this.sent_date * 1000));
        }
        return this._Cached__toStringDate;
    }

    private enum STATE {    COMPLETE, SENDING, FAILED   }
    @NonNull private STATE _STATE = STATE.COMPLETE;

    IE_Message(long pCustomerly_User_ID, long pConversationID, @NonNull String pMessage, @Nullable final IE_Attachment[] pAttachments) {
        super();
        this._STATE = STATE.SENDING;
        this.user_id = pCustomerly_User_ID;
        this.conversation_id = pConversationID;
        this.conversation_message_id = 0;
        this.account_id = this.assigner_id = 0;
        this.sent_date = this.seen_date = System.currentTimeMillis() / 1000;
        this.content = IU_Utils.decodeHtmlStringWithEmojiTag(pMessage);
        this._Attachments = pAttachments;
        this.if_account__name = null;
        this.rich_mail_url = null;
    }

    IE_Message(@NonNull JSONObject pMessageItem) {
        super();
        this._STATE = STATE.COMPLETE;

        this.conversation_id = pMessageItem.optLong("conversation_id", 0);
        this.conversation_message_id = pMessageItem.optLong("conversation_message_id", 0);
        this.user_id = pMessageItem.optLong("user_id", 0);
        this.account_id = pMessageItem.optLong("account_id", 0);
        this.assigner_id = pMessageItem.optLong("assigner_id", 0);
        this.sent_date = pMessageItem.optLong("sent_date", 0);
        this.seen_date = pMessageItem.optLong("seen_date", 0);
        this.content = IU_Utils.decodeHtmlStringWithEmojiTag(IU_Utils.jsonOptStringWithNullCheck(pMessageItem, "content", ""));
        this.rich_mail_url = pMessageItem.optInt("rich_mail", 0) == 0 ? null :

                /* TODO sparisce * */
                pMessageItem.isNull("rich_mail_url") ?
                        String.format(Locale.UK, "https://app.customerly.io/email/view/%d/%s",
                                this.conversation_message_id, IU_Utils.jsonOptStringWithNullCheck(pMessageItem, "rich_mail_token")) :
                /* *************** */

                IU_Utils.jsonOptStringWithNullCheck(pMessageItem, "rich_mail_url");

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
}
