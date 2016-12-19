package io.customerly;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Gianni on 11/09/16.
 * Project: CustomerlySDK
 */
class Internal_entity__Conversation {

    private static final int /*WRITER_TYPE__USER = 1,*/ WRITER_TYPE__ACCOUNT = 0;

    boolean unread;
    private SpannableStringBuilder last_message_abstract;
    private long last_message_writer, last_message_writer_type;
    long last_message_date;
    @Nullable private String last_account__name;

    final long conversation_id, assigner_id;

    Internal_entity__Conversation(long pConversationID, @Nullable SpannableStringBuilder pLastMessage, long pAssignerID, long pLastMessageDate, long pLastMessageWriterID, int pLastMessageWriterType, boolean pUnread, @Nullable String pLastAccountName) {
        super();
        this.conversation_id = pConversationID;
        this.last_message_abstract = pLastMessage;
        this.assigner_id = pAssignerID;

        this.last_message_date = pLastMessageDate;
        this.last_message_writer = pLastMessageWriterID;
        this.last_message_writer_type = pLastMessageWriterType;

        this.unread = pUnread;

        this.last_account__name = pLastAccountName;
    }

    Internal_entity__Conversation(@NonNull JSONObject pConversationItem) throws JSONException {
        super();

        this.conversation_id = pConversationItem.getLong("conversation_id");
        this.last_message_abstract = Internal_Utils__Utils.decodeHtmlStringWithEmojiTag(pConversationItem.getString("last_message_abstract"));
        this.assigner_id = pConversationItem.getLong("assigner_id");

        this.last_message_date = pConversationItem.getLong("last_message_date");
        this.last_message_writer = pConversationItem.optLong("last_message_writer");
        this.last_message_writer_type = pConversationItem.optInt("last_message_writer_type");

        this.unread = pConversationItem.optInt("unread", 0) == 1;

        pConversationItem = pConversationItem.optJSONObject("last_account");
//        pConversationItem diventa pConversationItem.last_account
        this.last_account__name = pConversationItem == null ? null : pConversationItem.optString("name", null);
    }

    void onNewMessage(@NonNull Internal_entity__Message pNuovoMessaggio) {
        this.last_message_abstract = pNuovoMessaggio.content;
        this.last_message_date = pNuovoMessaggio.sent_date;
        this.last_message_writer = pNuovoMessaggio.getWriterID();
        this.last_message_writer_type = pNuovoMessaggio.getWriterType();
        this.unread = true;
        this.last_account__name = pNuovoMessaggio.if_account__name;
    }

    @NonNull String getImageUrl(int pPixelSize) {
        return this.last_message_writer_type == WRITER_TYPE__ACCOUNT
            ? Internal_entity__Account.getAccountImageUrl(this.last_message_writer, pPixelSize)
            : Internal_entity__Account.getUserImageUrl(this.last_message_writer, pPixelSize);
    }

    @NonNull String getConversationLastWriter(@NonNull Context pContext) {
        return this.last_account__name != null ? this.last_account__name : pContext.getString(R.string.io_customerly__you);
    }

    @Nullable Spanned getLastMessage() {
        return this.last_message_abstract;
    }

    @NonNull String getFormattedLastMessageTime(@NonNull Resources resources) {
        return Internal_Utils__TimeAgoUtils.calculate(this.last_message_date,
                seconds -> resources.getString(R.string.io_customerly__XXs_ago, seconds),
                minutes -> resources.getString(R.string.io_customerly__XXm_ago, minutes),
                hours -> resources.getString(R.string.io_customerly__XXh_ago, hours),
                days -> resources.getString(R.string.io_customerly__XXd_ago, days));
    }

}
