package io.customerly;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
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
 * Project: CustomerlySDK
 */
public final class Internal_activity__CustomerlyList_Activity extends Internal_activity__AInput_Customerly_Activity {

    static final int RESULT_CODE_REFRESH_LIST = 100;

    private View input_email_layout, new_conversation_layout;
    private SwipeRefreshLayout _FirstContact_SRL, _RecyclerView_SRL;
    private RecyclerView _ListRecyclerView;
    private SwipeRefreshLayout.OnRefreshListener _OnRefreshListener;

    private List<Internal_entity__Conversation> _Conversations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Customerly._Instance.__PING__LAST_message_conversation_id = 0;
        if(this.onCreateLayout(R.layout.io_customerly__activity_list)) {
            this._FirstContact_SRL = (SwipeRefreshLayout)this.findViewById(R.id.io_customerly__first_contact_swipe_refresh);
            this.input_email_layout = this.findViewById(R.id.io_customerly__input_email_layout);
            this.new_conversation_layout = this.findViewById(R.id.io_customerly__new_conversation_layout);
            this._RecyclerView_SRL = (SwipeRefreshLayout) this.findViewById(R.id.io_customerly__recycler_view_swipe_refresh);
            this._ListRecyclerView = (RecyclerView) this.findViewById(R.id.io_customerly__recycler_view);
            this._ListRecyclerView.setLayoutManager(new LinearLayoutManager(this.getApplicationContext()));
            this._ListRecyclerView.setItemAnimator(new DefaultItemAnimator());
            this._ListRecyclerView.setHasFixedSize(true);
            this._ListRecyclerView.addItemDecoration(new Internal_Utils__RecyclerView_DividerDecoration._Vertical(this.getResources(), Internal_Utils__RecyclerView_DividerDecoration._Vertical.DIVIDER_WHERE.BOTH));
            this._ListRecyclerView.setAdapter(new ConversationAdapter());

            this.input_layout.setVisibility(View.GONE);
            this.findViewById(R.id.io_customerly__new_conversation_button).setOnClickListener(btn -> {
                this.new_conversation_layout.setVisibility(View.GONE);
                this.restoreAttachments();
                this.input_layout.setVisibility(View.VISIBLE);
            });

            this._OnRefreshListener = () -> {
                Internal__JwtToken token = Customerly._Instance._JwtToken;
                if(token != null && (token.isUser() || token.isLead())) {
                    new Internal_api__CustomerlyRequest.Builder<ArrayList<Internal_entity__Conversation>>(Internal_api__CustomerlyRequest.ENDPOINT_CONVERSATION_RETRIEVE)
                            .opt_checkConn(this)
                            .opt_converter(data -> Internal_Utils__Utils.fromJSONdataToList(data, "conversations", Internal_entity__Conversation::new))
                            .opt_tokenMandatory()
                            .opt_receiver((responseState, list) -> this.displayInterface(list))
                            .opt_trials(2)
                            .start();
                } else {
                    this.displayInterface(null);
                }
            };
            this._RecyclerView_SRL.setOnRefreshListener(this._OnRefreshListener);
            this._FirstContact_SRL.setOnRefreshListener(this._OnRefreshListener);
            this.onReconnection();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Customerly._Instance.__SOCKET__Message_listener = pNewMessages -> {
            ArrayList<Internal_entity__Message> filtered = new ArrayList<>(pNewMessages.size());
            next_new_message: for(Internal_entity__Message new_message : pNewMessages) {
                for(Internal_entity__Message new_message_filtered : filtered) {
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

            ArrayList<Internal_entity__Conversation> conversations = new ArrayList<>(this._Conversations);
            next_new_filtered: for(Internal_entity__Message new_filtered : filtered) {
                for(Internal_entity__Conversation conversation : conversations) {
                    if(conversation.conversation_id == new_filtered.conversation_id) { //New message of an existing conversation
                        conversation.onNewMessage(new_filtered);
                        continue next_new_filtered;
                    }
                }
                //New message of a new conversation
                conversations.add(new Internal_entity__Conversation(new_filtered.conversation_id, new_filtered.content, new_filtered.assigner_id, new_filtered.sent_date, new_filtered.getWriterID(), new_filtered.getWriterType(), new_filtered.if_account__name));
            }
            //Sort the conversation by last message date
            Collections.sort(conversations, (c1, c2) -> (int)(c2.last_message_date - c1.last_message_date));
            this._ListRecyclerView.post(() -> {
                this._Conversations = conversations;
                this._ListRecyclerView.getAdapter().notifyDataSetChanged();
            });
        };
    }

    @Override
    public void onBackPressed() {
        if(this.input_email_layout.getVisibility() == View.VISIBLE) {
            this.input_layout.setVisibility(View.VISIBLE);
            this.input_email_layout.setVisibility(View.GONE);
        } else {
            Customerly._Instance.__SOCKET__Message_listener = null;
            super.onBackPressed();
        }
    }

    @Override
    protected void onReconnection() {
        if(this._RecyclerView_SRL != null && this._OnRefreshListener != null) {
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

    private void displayInterface(@Nullable ArrayList<Internal_entity__Conversation> pConversations) {
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

            final int adminIconSizePX = Internal_Utils__Utils.px(45/* dp */);
            long last_time_active_in_seconds = Long.MIN_VALUE;
            layout_first_contact__admin_container.removeAllViews();

            Internal_entity__Admin[] admins = Customerly._Instance.__PING__LAST_active_admins;
            if(admins != null) {
                for (Internal_entity__Admin admin : admins) {
                    if (admin != null) {
                        showWelcomeCard = true;
                        if (admin.last_active > last_time_active_in_seconds)
                            last_time_active_in_seconds = admin.last_active;

                        final ImageView icon = new ImageView(this);
                        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(adminIconSizePX, adminIconSizePX);
                        lp.topMargin = Internal_Utils__Utils.px(10);
                        lp.bottomMargin = lp.topMargin;
                        icon.setLayoutParams(lp);

                        Customerly._Instance._RemoteImageHandler.request(new Internal_Utils__RemoteImageHandler.Request()
                                .fitCenter()
                                .transformCircle()
                                .load(admin.getImageUrl(adminIconSizePX))
                                .into(icon)
                                .override(adminIconSizePX, adminIconSizePX)
                                .placeholder(R.drawable.io_customerly__ic_default_admin));

                        layout_first_contact__admin_container.addView(icon);

                        final TextView name = new TextView(this);
                        name.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        name.setTextColor(Internal_Utils__Utils.getColorFromResource(this.getResources(), R.color.io_customerly__grey_99));
                        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                        name.setText(admin.name);
                        layout_first_contact__admin_container.addView(name);
                    }
                }
            }

            if(last_time_active_in_seconds != Long.MIN_VALUE) {
                final TextView layout_first_contact__welcome_card__last_activity = (TextView) this.findViewById(R.id.io_customerly__layout_first_contact__welcome_card__last_activity);
                layout_first_contact__welcome_card__last_activity.setText(
                        Internal_Utils__TimeAgoUtils.calculate(last_time_active_in_seconds,
                                seconds -> this.getString(R.string.io_customerly__last_activity_now),
                                minutes -> this.getResources().getQuantityString(R.plurals.io_customerly__last_activity_XXm_ago, (int)minutes, minutes),
                                hours -> this.getResources().getQuantityString(R.plurals.io_customerly__last_activity_XXh_ago, (int)hours, hours),
                                days -> this.getResources().getQuantityString(R.plurals.io_customerly__last_activity_XXd_ago, (int)days, days)));
                layout_first_contact__welcome_card__last_activity.setVisibility(View.VISIBLE);
            }

            final CustomerlyHtmlMessage welcome = Customerly._Instance._WELCOME__getMessage();
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
    protected void onInputActionSend_PerformSend(@NonNull String pMessage, @NonNull Internal_entity__Attachment[] pAttachments, @Nullable String ghostToVisitorEmail) {
        Internal__JwtToken token = Customerly._Instance._JwtToken;
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
                new Internal_api__CustomerlyRequest.Builder<Object[]>(Internal_api__CustomerlyRequest.ENDPOINT_PING)
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
                                for(Internal_entity__Attachment a : pAttachments) {
                                    a.addAttachmentToInput(this);
                                }
                                Toast.makeText(Internal_activity__CustomerlyList_Activity.this.getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                            } else {
                                new Internal_api__CustomerlyRequest.Builder<Long>(Internal_api__CustomerlyRequest.ENDPOINT_MESSAGE_SEND)
                                        .opt_tokenMandatory()
                                        .opt_converter(data -> {
                                            JSONObject conversation = data.optJSONObject("conversation");
                                            JSONObject message = data.optJSONObject("message");
                                            if(conversation != null && message != null) {
                                                Customerly._Instance.__SOCKET_SEND_Message(data.optLong("timestamp", -1L));
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
                                            if(message_send_responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK && message_send_conversationID != null) {
                                                this.openConversationById(message_send_conversationID, true);
                                            } else {
                                                this.input_input.setText(pMessage);
                                                for(Internal_entity__Attachment a : pAttachments) {
                                                    a.addAttachmentToInput(this);
                                                }
                                                Toast.makeText(Internal_activity__CustomerlyList_Activity.this.getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .opt_trials(2)
                                        .param("message", pMessage)
                                        .param("attachments", Internal_entity__Attachment.toSendJSONObject(this, pAttachments))
                                        .start();
                            }
                        })
                        .start();
            }
        } else {
            final ProgressDialog progressDialog = ProgressDialog.show(this, this.getString(R.string.io_customerly__new_conversation), this.getString(R.string.io_customerly__sending_message), true, false);
            new Internal_api__CustomerlyRequest.Builder<Long>(Internal_api__CustomerlyRequest.ENDPOINT_MESSAGE_SEND)
                    .opt_checkConn(this)
                    .opt_tokenMandatory()
                    .opt_converter(data -> {
                        JSONObject conversation = data.optJSONObject("conversation");
                        JSONObject message = data.optJSONObject("message");
                        if(conversation != null && message != null) {
                            Customerly._Instance.__SOCKET_SEND_Message(data.optLong("timestamp", -1L));
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
                        if(responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK && conversationID != null) {
                            this.openConversationById(conversationID, ghostToVisitorEmail != null);
                        } else {
                            this.input_input.setText(pMessage);
                            for(Internal_entity__Attachment a : pAttachments) {
                                a.addAttachmentToInput(this);
                            }
                            Toast.makeText(Internal_activity__CustomerlyList_Activity.this.getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .opt_trials(2)
                    .param("message", pMessage)
                    .param("attachments", Internal_entity__Attachment.toSendJSONObject(this, pAttachments))
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
            if(Internal_Utils__Utils.checkConnection(Internal_activity__CustomerlyList_Activity.this)) {
                Internal_activity__CustomerlyList_Activity.this.startActivityForResult(new Intent(Internal_activity__CustomerlyList_Activity.this, Internal_activity__CustomerlyChat_Activity.class)
                        .putExtra(Internal_activity__AInput_Customerly_Activity.EXTRA_MUST_SHOW_BACK, ! andFinishCurrent)
                        .putExtra(Internal_activity__CustomerlyChat_Activity.EXTRA_CONVERSATION_ID, id), RESULT_CODE_REFRESH_LIST);
                if(andFinishCurrent) {
                    this.finish();
                }
            } else {
                Toast.makeText(Internal_activity__CustomerlyList_Activity.this.getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ConversationVH extends RecyclerView.ViewHolder {
        private long _ConversationID;
        @NonNull private final ImageView _Icon;
        @NonNull private final TextView _Nome, _LastMessage, _Time;
        private final int _Icon_Size = Internal_Utils__Utils.px(50);
        private ConversationVH() {
            super(getLayoutInflater().inflate(R.layout.io_customerly__li_conversation, _ListRecyclerView, false));
            this._Icon = (ImageView)this.itemView.findViewById(R.id.io_customerly__icon);
            ViewGroup.LayoutParams lp = this._Icon.getLayoutParams();
            lp.width = lp.height = _Icon_Size;
            this._Nome = (TextView)this.itemView.findViewById(R.id.io_customerly__nome);
            this._LastMessage = (TextView)this.itemView.findViewById(R.id.io_customerly__last_message);
            this._Time = (TextView)this.itemView.findViewById(R.id.io_customerly__time);
            this.itemView.setOnClickListener(item_view -> openConversationById(this._ConversationID, false));
        }
        private void apply(@NonNull Internal_entity__Conversation pConversation) {
            this._ConversationID = pConversation.conversation_id;
            Customerly._Instance._RemoteImageHandler.request(new Internal_Utils__RemoteImageHandler.Request()
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
