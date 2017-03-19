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

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by Gianni on 03/09/16.
 * Project: Customerly Android SDK
 */
public final class IAct_List extends IAct_AInput implements Customerly.SDKActivity {

    static final int RESULT_CODE_REFRESH_LIST = 100;

    private View input_email_layout, new_conversation_layout;
    private SwipeRefreshLayout _FirstContact_SRL, _RecyclerView_SRL;
    private RecyclerView _ListRecyclerView;
    @NonNull private List<IE_Conversation> _Conversations = new ArrayList<>();
    @NonNull private final SwipeRefreshLayout.OnRefreshListener _OnRefreshListener = () -> {
        IE_JwtToken token = Customerly.get()._JwtToken;
        if(token != null && (token.isUser() || token.isLead())) {
            new IApi_Request.Builder<ArrayList<IE_Conversation>>(IApi_Request.ENDPOINT_CONVERSATION_RETRIEVE)
                    .opt_checkConn(this)
                    .opt_converter(data -> IU_Utils.fromJSONdataToList(data, "conversations", IE_Conversation::new))
                    .opt_tokenMandatory()
                    .opt_receiver((responseState, list) -> this.displayInterface(list))
                    .opt_trials(2)
                    .start();
        } else {
            this.displayInterface(null);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(this.onCreateLayout(R.layout.io_customerly__activity_list)) {
            this._FirstContact_SRL = (SwipeRefreshLayout)this.findViewById(R.id.io_customerly__first_contact_swipe_refresh);
            this.input_email_layout = this.findViewById(R.id.io_customerly__input_email_layout);
            this.new_conversation_layout = this.findViewById(R.id.io_customerly__new_conversation_layout);
            this._RecyclerView_SRL = (SwipeRefreshLayout) this.findViewById(R.id.io_customerly__recycler_view_swipe_refresh);
            this._ListRecyclerView = (RecyclerView) this.findViewById(R.id.io_customerly__recycler_view);
            this._ListRecyclerView.setLayoutManager(new LinearLayoutManager(this.getApplicationContext()));
            this._ListRecyclerView.setItemAnimator(new DefaultItemAnimator());
            this._ListRecyclerView.setHasFixedSize(true);
            this._ListRecyclerView.addItemDecoration(new IU_RecyclerView_DividerDecoration._Vertical(this.getResources(), R.color.io_customerly__li_conversation_divider_color, IU_RecyclerView_DividerDecoration._Vertical.DIVIDER_WHERE.BOTTOM));
            this._ListRecyclerView.setAdapter(new ConversationAdapter());

            this.input_layout.setVisibility(View.GONE);
            this.findViewById(R.id.io_customerly__new_conversation_button).setOnClickListener(btn -> {
                this.new_conversation_layout.setVisibility(View.GONE);
                this.restoreAttachments();
                this.input_layout.setVisibility(View.VISIBLE);
            });

            this._RecyclerView_SRL.setOnRefreshListener(this._OnRefreshListener);
            this._FirstContact_SRL.setOnRefreshListener(this._OnRefreshListener);
            this.onReconnection();
        }
    }

    @Override
    public void onNewSocketMessages(@NonNull ArrayList<IE_Message> messages) {
        ArrayList<IE_Message> filtered = new ArrayList<>(messages.size());
        next_new_message: for(IE_Message new_message : messages) {
            for(IE_Message new_message_filtered : filtered) {
                if(new_message_filtered.conversation_id == new_message.conversation_id) {
                    if(new_message_filtered.conversation_message_id >= new_message.conversation_message_id) {
                        continue next_new_message;//Already found a most recent message for that conversation
                    } else {
                        filtered.remove(new_message_filtered);
                        filtered.add(new_message);
                        continue next_new_message;//Already found a message for that conversation but this is most recent
                    }
                }
            }
            filtered.add(new_message);//This is the first message found for that conversation
        }

        if(! filtered.isEmpty()) {
            ArrayList<IE_Conversation> conversations = new ArrayList<>(this._Conversations);
            next_new_filtered:
            for (IE_Message new_filtered : filtered) {
                for (IE_Conversation conversation : conversations) {
                    if (conversation.conversation_id == new_filtered.conversation_id) { //New message of an existing conversation
                        conversation.onNewMessage(new_filtered);
                        continue next_new_filtered;
                    }
                }
                //New message of a new conversation
                conversations.add(new IE_Conversation(new_filtered.conversation_id, new_filtered.content, new_filtered.sent_datetime_sec, new_filtered.getWriterID(), new_filtered.getWriterType(), new_filtered.if_account__name));
            }
            //Sort the conversation by last message date
            Collections.sort(conversations, (c1, c2) -> (int) (c2.last_message_date - c1.last_message_date));
            this._ListRecyclerView.post(() -> {
                this._Conversations = conversations;
                this._ListRecyclerView.getAdapter().notifyDataSetChanged();
                MediaPlayer mp = MediaPlayer.create(this, R.raw.notif_2);
                mp.setOnCompletionListener(mp1 -> {
                    mp1.reset();
                    mp1.release();
                });
                mp.start();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IE_JwtToken jwt = Customerly.get()._JwtToken;
        if (jwt == null) {
            this.onLogoutUser();
        }
    }

    @Override
    public void onLogoutUser() {
        this.finish();
    }

    @Override
    public void onBackPressed() {
        if(this.input_email_layout.getVisibility() == View.VISIBLE) {
            this.input_layout.setVisibility(View.VISIBLE);
            this.input_email_layout.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onReconnection() {
        if(this._RecyclerView_SRL != null) {
            this._RecyclerView_SRL.setRefreshing(true);
            this._FirstContact_SRL.setRefreshing(true);
            this._OnRefreshListener.onRefresh();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_CODE_REFRESH_LIST) {
            this.onReconnection();
        }
    }

    private void displayInterface(@Nullable ArrayList<IE_Conversation> pConversations) {
        if(pConversations != null && pConversations.size() != 0) {
            this._FirstContact_SRL.setVisibility(View.GONE);
            this._ListRecyclerView.post(() -> {
                this._Conversations = pConversations;
                this._ListRecyclerView.getAdapter().notifyDataSetChanged();
            });
            this.input_layout.setVisibility(View.GONE);
            this.input_email_layout.setVisibility(View.GONE);
            this.new_conversation_layout.setVisibility(View.VISIBLE);
            this._RecyclerView_SRL.setVisibility(View.VISIBLE);
            this._RecyclerView_SRL.setRefreshing(false);
            this._FirstContact_SRL.setRefreshing(false);
        } else { //Layout first contact
            this._RecyclerView_SRL.setVisibility(View.GONE);
            this.input_email_layout.setVisibility(View.GONE);
            this.new_conversation_layout.setVisibility(View.GONE);
            this.restoreAttachments();
            this.input_layout.setVisibility(View.VISIBLE);
            this._RecyclerView_SRL.setRefreshing(false);
            this._FirstContact_SRL.setRefreshing(false);
            boolean showWelcomeCard = false;

            LinearLayout layout_first_contact__admin_container = (LinearLayout) this.findViewById(R.id.io_customerly__layout_first_contact__admin_container);

            final int adminIconSizePX = IU_Utils.px(45/* dp */);
            long last_time_active_in_seconds = Long.MIN_VALUE;
            layout_first_contact__admin_container.removeAllViews();

            IE_Admin[] admins = Customerly.get().__PING__LAST_active_admins;
            if(admins != null) {
                for (IE_Admin admin : admins) {
                    if (admin != null) {
                        showWelcomeCard = true;
                        if (admin.last_active > last_time_active_in_seconds) {
                            last_time_active_in_seconds = admin.last_active;
                        }

                        LinearLayout ll = new LinearLayout(this);
                        ll.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                        ll.setGravity(Gravity.CENTER_HORIZONTAL);
                        ll.setOrientation(LinearLayout.VERTICAL);
                        int _10dp = IU_Utils.px(10);
                        ll.setPadding(_10dp, 0, _10dp, 0);

                        final ImageView icon = new ImageView(this);
                        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(adminIconSizePX, adminIconSizePX);
                        lp.bottomMargin = lp.topMargin = IU_Utils.px(10);
                        icon.setLayoutParams(lp);

                        Customerly.get()._RemoteImageHandler.request(new IU_RemoteImageHandler.Request()
                                .fitCenter()
                                .transformCircle()
                                .load(admin.getImageUrl(adminIconSizePX))
                                .into(icon)
                                .override(adminIconSizePX, adminIconSizePX)
                                .placeholder(R.drawable.io_customerly__ic_default_admin));

                        ll.addView(icon);

                        final TextView name = new TextView(this);
                        name.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        name.setTextColor(IU_Utils.getColorFromResource(this.getResources(), R.color.io_customerly__welcomecard_texts));
                        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                        name.setText(admin.name);
                        name.setSingleLine(false);
                        name.setMinLines(2);
                        name.setMaxLines(3);
                        name.setGravity(Gravity.CENTER_HORIZONTAL);
                        name.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                        ll.addView(name);

                        layout_first_contact__admin_container.addView(ll);
                    }
                }
            }

            if(last_time_active_in_seconds != Long.MIN_VALUE) {
                final TextView layout_first_contact__welcome_card__last_activity = (TextView) this.findViewById(R.id.io_customerly__layout_first_contact__welcome_card__last_activity);
                layout_first_contact__welcome_card__last_activity.setText(
                        IU_TimeAgoUtils.calculate(last_time_active_in_seconds,
                                seconds -> this.getString(R.string.io_customerly__last_activity_now),
                                minutes -> this.getResources().getQuantityString(R.plurals.io_customerly__last_activity_XXm_ago, (int)minutes, minutes),
                                hours -> this.getResources().getQuantityString(R.plurals.io_customerly__last_activity_XXh_ago, (int)hours, hours),
                                days -> this.getResources().getQuantityString(R.plurals.io_customerly__last_activity_XXd_ago, (int)days, days)));
                layout_first_contact__welcome_card__last_activity.setVisibility(View.VISIBLE);
            }

            final Customerly.HtmlMessage welcome = Customerly.get()._WELCOME__getMessage();
            if(welcome != null && welcome.length() != 0){
                final TextView layout_first_contact__welcome_card__welcome = (TextView) this.findViewById(R.id.io_customerly__layout_first_contact__welcome_card__welcome);
                layout_first_contact__welcome_card__welcome.setText(welcome);
                layout_first_contact__welcome_card__welcome.setVisibility(View.VISIBLE);
                showWelcomeCard = true;
            }
            if(showWelcomeCard) {
                this.findViewById(R.id.io_customerly__layout_first_contact__welcome_card).setVisibility(View.VISIBLE);
            }

            this._FirstContact_SRL.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onInputActionSend_PerformSend(@NonNull String pMessage, @NonNull IE_Attachment[] pAttachments, @Nullable String ghostToVisitorEmail) {
        IE_JwtToken token = Customerly.get()._JwtToken;
        if((token == null || token.isAnonymous())) {
            if(ghostToVisitorEmail == null) {
                this.input_layout.setVisibility(View.GONE);
                this.input_email_layout.setVisibility(View.VISIBLE);

                final EditText input_email_edit_text = (EditText) this.findViewById(R.id.io_customerly__input_email_edit_text);
                input_email_edit_text.requestFocus();
                this.findViewById(R.id.io_customerly__input_email_button).setOnClickListener(btn -> {
                    final String email = input_email_edit_text.getText().toString().trim().toLowerCase(Locale.ITALY);
                    if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        this.input_email_layout.setVisibility(View.GONE);
                        input_email_edit_text.setText(null);
                        this.input_layout.setVisibility(View.VISIBLE);
                        this.onInputActionSend_PerformSend(pMessage, pAttachments, email);
                    } else {
                        if (input_email_edit_text.getTag() == null) {
                            input_email_edit_text.setTag(new Object[0]);
                            final int input_email_edit_text__originalColor = input_email_edit_text.getTextColors().getDefaultColor();
                            input_email_edit_text.setTextColor(Color.RED);
                            input_email_edit_text.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    input_email_edit_text.setTextColor(input_email_edit_text__originalColor);
                                    input_email_edit_text.removeTextChangedListener(this);
                                    input_email_edit_text.setTag(null);
                                }
                            });
                        }
                    }
                });
            } else {
                final ProgressDialog progressDialog = ProgressDialog.show(this, this.getString(R.string.io_customerly__new_conversation), this.getString(R.string.io_customerly__sending_message), true, false);
                new IApi_Request.Builder<Object[]>(IApi_Request.ENDPOINT_PING)
                        .opt_checkConn(this)
                        .param("lead_email", ghostToVisitorEmail)
                        .opt_converter(root -> new Object[0])
                        .opt_receiver((responseState, _void) -> {
                            if(_void == null) {
                                if(progressDialog != null) {
                                    try {
                                        progressDialog.dismiss();
                                    } catch (IllegalStateException ignored) { }
                                }
                                this.input_input.setText(pMessage);
                                for(IE_Attachment a : pAttachments) {
                                    a.addAttachmentToInput(this);
                                }
                                Toast.makeText(IAct_List.this.getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                            } else {
                                new IApi_Request.Builder<Long>(IApi_Request.ENDPOINT_MESSAGE_SEND)
                                        .opt_tokenMandatory()
                                        .opt_converter(data -> {
                                            JSONObject conversation = data.optJSONObject("conversation");
                                            JSONObject message = data.optJSONObject("message");
                                            if(conversation != null && message != null) {
                                                Customerly.get().__SOCKET_SEND_Message(data.optLong("timestamp", -1L));
                                                long conversation_id = message.optLong("conversation_id", -1L);
                                                return conversation_id != -1L ? conversation_id : null;
                                            } else {
                                                return null;
                                            }
                                        })
                                        .opt_receiver((message_send_responseState, message_send_conversationID) -> {
                                            if(progressDialog != null) {
                                                try {
                                                    progressDialog.dismiss();
                                                } catch (IllegalStateException ignored) { }
                                            }
                                            if(message_send_responseState == IApi_Request.RESPONSE_STATE__OK && message_send_conversationID != null) {
                                                this.openConversationById(message_send_conversationID, true);
                                            } else {
                                                this.input_input.setText(pMessage);
                                                for(IE_Attachment a : pAttachments) {
                                                    a.addAttachmentToInput(this);
                                                }
                                                Toast.makeText(IAct_List.this.getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .opt_trials(2)
                                        .param("message", pMessage)
                                        .param("attachments", IE_Attachment.toSendJSONObject(this, pAttachments))
                                        .start();
                            }
                        })
                        .start();
            }
        } else {
            final ProgressDialog progressDialog = ProgressDialog.show(this, this.getString(R.string.io_customerly__new_conversation), this.getString(R.string.io_customerly__sending_message), true, false);
            new IApi_Request.Builder<Long>(IApi_Request.ENDPOINT_MESSAGE_SEND)
                    .opt_checkConn(this)
                    .opt_tokenMandatory()
                    .opt_converter(data -> {
                        JSONObject conversation = data.optJSONObject("conversation");
                        JSONObject message = data.optJSONObject("message");
                        if(conversation != null && message != null) {
                            Customerly.get().__SOCKET_SEND_Message(data.optLong("timestamp", -1L));
                            long conversation_id = message.optLong("conversation_id", -1L);
                            return conversation_id != -1L ? conversation_id : null;
                        } else {
                            return null;
                        }
                    })
                    .opt_receiver((responseState, conversationID) -> {
                        if(progressDialog != null) {
                            try {
                                progressDialog.dismiss();
                            } catch (IllegalStateException ignored) { }
                        }
                        if(responseState == IApi_Request.RESPONSE_STATE__OK && conversationID != null) {
                            this.openConversationById(conversationID, ghostToVisitorEmail != null);
                        } else {
                            this.input_input.setText(pMessage);
                            for(IE_Attachment a : pAttachments) {
                                a.addAttachmentToInput(this);
                            }
                            Toast.makeText(IAct_List.this.getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .opt_trials(2)
                    .param("message", pMessage)
                    .param("attachments", IE_Attachment.toSendJSONObject(this, pAttachments))
                    .start();
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openConversationById(long id, boolean andFinishCurrent) {
        if(id != 0) {
            if(IU_Utils.checkConnection(IAct_List.this)) {
                IAct_Chat.startForResult(IAct_List.this, ! andFinishCurrent, id, RESULT_CODE_REFRESH_LIST);
                if(andFinishCurrent) {
                    this.finish();
                }
            } else {
                Toast.makeText(IAct_List.this.getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ConversationVH extends RecyclerView.ViewHolder {
        private long _ConversationID;
        @NonNull private final ImageView _Icon;
        @NonNull private final TextView _Nome, _LastMessage, _Time;
        private final int _Icon_Size = IU_Utils.px(50);
        private ConversationVH() {
            super(getLayoutInflater().inflate(R.layout.io_customerly__li_conversation, _ListRecyclerView, false));
            this._Icon = (ImageView)this.itemView.findViewById(R.id.io_customerly__icon);
            ViewGroup.LayoutParams lp = this._Icon.getLayoutParams();
            lp.width = lp.height = _Icon_Size;
            this._Nome = (TextView)this.itemView.findViewById(R.id.io_customerly__name);
            this._LastMessage = (TextView)this.itemView.findViewById(R.id.io_customerly__last_message);
            this._Time = (TextView)this.itemView.findViewById(R.id.io_customerly__time);
            this.itemView.setOnClickListener(item_view -> openConversationById(this._ConversationID, false));
        }
        private void apply(@NonNull IE_Conversation pConversation) {
            this._ConversationID = pConversation.conversation_id;
            Customerly.get()._RemoteImageHandler.request(new IU_RemoteImageHandler.Request()
                    .fitCenter()
                    .transformCircle()
                    .load(pConversation.getImageUrl(this._Icon_Size))
                    .into(this._Icon)
                    .override(this._Icon_Size, this._Icon_Size)
                    .placeholder(R.drawable.io_customerly__ic_default_admin));
            this._Nome.setText(pConversation.getConversationLastWriter(this._Nome.getContext()));
            this._LastMessage.setText(pConversation.getLastMessage());
            this._Time.setText(pConversation.getFormattedLastMessageTime(getResources()));
            this.itemView.setSelected(pConversation.unread);
        }
    }

    private class ConversationAdapter extends RecyclerView.Adapter<ConversationVH> {
        @Override
        public ConversationVH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ConversationVH();
        }
        @Override
        public void onBindViewHolder(ConversationVH holder, int position) {
            holder.apply(_Conversations.get(position));
        }
        @Override
        public int getItemCount() {
            return _Conversations.size();
        }
    }

}
