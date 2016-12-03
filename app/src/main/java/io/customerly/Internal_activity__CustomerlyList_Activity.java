package io.customerly;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spanned;
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

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by Gianni on 03/09/16.
 * Project: CustomerlySDK
 */
public class Internal_activity__CustomerlyList_Activity extends Internal_activity__AInput_Customerly_Activity {

    static final int RESULT_CODE_REFRESH_LIST = 100;

    private View input_email_layout, new_conversation_layout;
    private SwipeRefreshLayout _FirstContact_SRL, _RecyclerView_SRL;
    private RecyclerView _ListRecyclerView;
    private SwipeRefreshLayout.OnRefreshListener _OnRefreshListener;

    private List<Internal_entity__Conversation> _Conversations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(this.onCreateLayout(R.layout.io_customerly__activity_list, true)) {
            this._FirstContact_SRL = (SwipeRefreshLayout)this.findViewById(R.id.io_customerly__first_contact_swiperefresh);
            this.input_email_layout = this.findViewById(R.id.io_customerly__input_email_layout);
            this.new_conversation_layout = this.findViewById(R.id.io_customerly__new_conversation_layout);
            this._RecyclerView_SRL = (SwipeRefreshLayout) this.findViewById(R.id.io_customerly__recyclerview_swiperefresh);
            this._ListRecyclerView = (RecyclerView) this.findViewById(R.id.io_customerly__recyclerview);
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

            this._OnRefreshListener = () ->
                new Internal_api__CustomerlyRequest.Builder<ArrayList<Internal_entity__Conversation>>(Internal_api__CustomerlyRequest.ENDPOINT_CONVERSATIONRETRIEVE)
                    .opt_checkConn(this)
                    .opt_converter(data -> Internal_Utils__Utils.fromJSONdataToList(data, "conversations", Internal_entity__Conversation::new))
                    .opt_receiver((responseState, list) -> {
                        if(responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__SERVERERROR_USER_NOT_AUTENTICATED
                            || responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK) {
                            this.displayInterface(list);
                        } else {
                            this.finish();
                            Toast.makeText(getApplicationContext(), R.string.io_customerly__errore_connessione_probabile, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .opt_trials(2)
                        .start();
            this._RecyclerView_SRL.setOnRefreshListener(this._OnRefreshListener);
            this._FirstContact_SRL.setOnRefreshListener(this._OnRefreshListener);
            this.onReconnection();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Customerly._Instance.__SOCKET_RECEIVE_Message(pNewMessages -> {
            ArrayList<Internal_entity__Message> filtrati = new ArrayList<>(pNewMessages.size());
            nextNuovo: for(Internal_entity__Message nuovo : pNewMessages) {
                for(Internal_entity__Message gia_filtrato : filtrati) {
                    if(gia_filtrato.conversation_id == nuovo.conversation_id) {
                        if(gia_filtrato.conversation_message_id >= nuovo.conversation_message_id) {
                            continue nextNuovo;//Già c'è un nuovo messaggio per quella conversazione più recente
                        } else {
                            filtrati.remove(gia_filtrato);
                            filtrati.add(nuovo);
                            continue nextNuovo;//Già c'è un nuovo messaggio per quella conversazione ma questo è più recente
                        }
                    }
                }
                filtrati.add(nuovo);//E' il primo nuovo messaggio di quella conversazione
            }

            ArrayList<Internal_entity__Conversation> conversazioni = new ArrayList<>(this._Conversations);
            nextNuovoFiltrato: for(Internal_entity__Message nuovo : filtrati) {
                for(Internal_entity__Conversation conversation : conversazioni) {
                    if(conversation.conversation_id == nuovo.conversation_id) { //Nuovo messaggio di una conversazione esistente
                        conversation.onNewMessage(nuovo);
                        continue nextNuovoFiltrato;
                    }
                }
                //Nuovo messaggio di una nuova conversazione
                conversazioni.add(new Internal_entity__Conversation(nuovo.conversation_id, nuovo.content, nuovo.assigner_id, nuovo.sent_date, nuovo.getWriterID(), nuovo.getWriterType(), true, nuovo.if_account__name));
            }
            //Riordino il tutto per data ultimo messaggio INVERSO
            Collections.sort(conversazioni, (c1, c2) -> (int)(c2.last_message_date - c1.last_message_date));
            this._ListRecyclerView.post(() -> {
                this._Conversations = conversazioni;
                this._ListRecyclerView.getAdapter().notifyDataSetChanged();
            });
        });
    }

    @Override
    public void onBackPressed() {
        Customerly._Instance.__SOCKET_RECEIVE_Message(null);
        super.onBackPressed();
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

            LinearLayout layout_first_contact__admincontainer = (LinearLayout) this.findViewById(R.id.io_customerly__layout_first_contact__admincontainer);

            final int adminIconSizePX = Internal_Utils__Utils.px(45/* dp */);
            long last_time_active_in_seconds = Long.MIN_VALUE;
            layout_first_contact__admincontainer.removeAllViews();

            Internal_entity__Admin[] admins = Customerly.get().__PING__LAST_active_admins;
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

                        Customerly.get().loadRemoteImage(new Internal_Utils__RemoteImageHandler.Request()
                                .fitCenter()
                                .transformCircle()
                                .load(admin.getImageUrl(adminIconSizePX))
                                .into(icon)
                                .override(adminIconSizePX, adminIconSizePX)
                                .placeholder(R.drawable.io_customerly__ic_default_admin));

                        layout_first_contact__admincontainer.addView(icon);

                        final TextView name = new TextView(this);
                        name.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        name.setTextColor(Internal_Utils__Utils.getColorFromResource(this.getResources(), R.color.io_customerly__grey99));
                        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                        name.setText(admin.name);
                        layout_first_contact__admincontainer.addView(name);
                    }
                }
            }

            if(last_time_active_in_seconds != Long.MIN_VALUE) {
                final TextView layout_first_contact__welcomecard__lastactivity = (TextView) this.findViewById(R.id.io_customerly__layout_first_contact__welcomecard__lastactivity);
                layout_first_contact__welcomecard__lastactivity.setText(
                        Internal_Utils__TimeAgoUtils.calculate(last_time_active_in_seconds,
                                seconds -> this.getString(R.string.io_customerly__last_activity_now),
                                minutes -> this.getResources().getQuantityString(R.plurals.io_customerly__last_activity_XXm_ago, (int)minutes, minutes),
                                hours -> this.getResources().getQuantityString(R.plurals.io_customerly__last_activity_XXh_ago, (int)hours, hours),
                                days -> this.getResources().getQuantityString(R.plurals.io_customerly__last_activity_XXd_ago, (int)days, days)));
                layout_first_contact__welcomecard__lastactivity.setVisibility(View.VISIBLE);
            }

            final Spanned welcome = Customerly.get().__WELCOME__getMessage();
            if(welcome != null && welcome.length() != 0){
                final TextView layout_first_contact__welcomecard__welcome = (TextView) this.findViewById(R.id.io_customerly__layout_first_contact__welcomecard__welcome);
                layout_first_contact__welcomecard__welcome.setText(welcome);
                layout_first_contact__welcomecard__welcome.setVisibility(View.VISIBLE);
                showWelcomeCard = true;
            }
            if(showWelcomeCard) {
                this.findViewById(R.id.io_customerly__layout_first_contact__welcomecard).setVisibility(View.VISIBLE);
            }

            this._FirstContact_SRL.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onInputActionSend_PerformSend(@NonNull String pMessage, @NonNull Internal_entity__Attachment[] pAttachments, @Nullable String ghostToVisitorEmail) {
        if(Customerly._Instance.__USER__get() != null || ghostToVisitorEmail != null) {
            this.okSend(pMessage, pAttachments, ghostToVisitorEmail);
        } else {
            this.input_layout.setVisibility(View.GONE);
            this.input_email_layout.setVisibility(View.VISIBLE);

            final EditText input_email_edittext = (EditText) this.findViewById(R.id.io_customerly__input_email_edittext);
            input_email_edittext.requestFocus();
            this.findViewById(R.id.io_customerly__input_email_button).setOnClickListener(btn -> {
                final String email = input_email_edittext.getText().toString().trim().toLowerCase(Locale.ITALY);
                if(Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    this.input_email_layout.setVisibility(View.GONE);
                    input_email_edittext.setText(null);
                    this.input_layout.setVisibility(View.VISIBLE);
                    this.onInputActionSend_PerformSend(pMessage, pAttachments, email);
                } else {
                    if(input_email_edittext.getTag() == null) {
                        input_email_edittext.setTag(new Object[0]);
                        final int input_email_edittext__originalColor = input_email_edittext.getTextColors().getDefaultColor();
                        input_email_edittext.setTextColor(Color.RED);
                        input_email_edittext.addTextChangedListener(new TextWatcher() {
                            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                            @Override public void afterTextChanged(Editable s) { }
                            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                                input_email_edittext.setTextColor(input_email_edittext__originalColor);
                                input_email_edittext.removeTextChangedListener(this);
                                input_email_edittext.setTag(null);
                            }
                        });
                    }
                }
            });
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N)
    private void okSend(@NonNull String pMessage, @NonNull Internal_entity__Attachment[] pAttachments, @Nullable String ghostToVisitorEmail) {
        final ProgressDialog progressDialog = ProgressDialog.show(this, this.getString(R.string.io_customerly__nuova_conversazione), this.getString(R.string.io_customerly__invio_messaggio_in_corso), true, false);
        new Internal_api__CustomerlyRequest.Builder<long[]>(Internal_api__CustomerlyRequest.ENDPOINT_MESSAGESEND)
                .opt_checkConn(this)
                .opt_converter(data -> {
                    long timestamp = data.optLong("timestamp");
                    data = data.getJSONObject("conversation");
                    long[] conversationID_assignerID = new long[] { data.getLong("conversation_id"), data.getLong("assigner_id") };
                    Customerly._Instance.__SOCKET_SEND_Message(conversationID_assignerID[1], timestamp);
                    return conversationID_assignerID;
                })
                .opt_receiver((responseState, conversationID_assignerID) -> {
                    if(progressDialog != null) {
                        try {
                            progressDialog.dismiss();
                        } catch (IllegalStateException ignored) { }
                    }
                    if(responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK) {
                        this.startActivityForResult(new Intent(Internal_activity__CustomerlyList_Activity.this, Internal_activity__CustomerlyChat_Activity.class)
                                .putExtra(Internal_activity__CustomerlyChat_Activity.EXTRA_CONVERSATION_ID, conversationID_assignerID[0])
                                .putExtra(Internal_activity__CustomerlyChat_Activity.EXTRA_ASSIGNER_ID, conversationID_assignerID[1]), RESULT_CODE_REFRESH_LIST);
                    } else {
                        this.input_input.setText(pMessage);
                        for(Internal_entity__Attachment a : pAttachments) {
                            try {
                                a.addAttachmentToInput(this);
                            } catch (JSONException ignored) { }
                        }
                        Toast.makeText(Internal_activity__CustomerlyList_Activity.this.getApplicationContext(), R.string.io_customerly__errore_connessione_probabile, Toast.LENGTH_SHORT).show();
                    }
                })
                .opt_trials(2)
                .param("visitor_email", ghostToVisitorEmail)
                .param("message", pMessage)
                .param("attachments", Internal_entity__Attachment.toSendJSONObject(this, pAttachments))
                .start();
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

    private class ConversationVH extends RecyclerView.ViewHolder {
        private long _ConversationID, _AssignerID;
        @NonNull private final ImageView _Icon;
        @NonNull private final TextView _Nome, _LastMessage, _Time;
        private final int _IconaSize = Internal_Utils__Utils.px(50);
        private ConversationVH() {
            super(getLayoutInflater().inflate(R.layout.io_customerly__li_conversation, _ListRecyclerView, false));
            this._Icon = (ImageView)this.itemView.findViewById(R.id.io_customerly__icona);
            ViewGroup.LayoutParams lp = this._Icon.getLayoutParams();
            lp.width = lp.height = _IconaSize;
            this._Nome = (TextView)this.itemView.findViewById(R.id.io_customerly__nome);
            this._LastMessage = (TextView)this.itemView.findViewById(R.id.io_customerly__last_message);
            this._Time = (TextView)this.itemView.findViewById(R.id.io_customerly__time);
            this.itemView.setOnClickListener(itemview -> {
                if(this._ConversationID != 0) {
                    if(Internal_Utils__Utils.checkConnection(Internal_activity__CustomerlyList_Activity.this)) {
                        Internal_activity__CustomerlyList_Activity.this.startActivityForResult(new Intent(Internal_activity__CustomerlyList_Activity.this, Internal_activity__CustomerlyChat_Activity.class)
                                .putExtra(Internal_activity__CustomerlyChat_Activity.EXTRA_CONVERSATION_ID, this._ConversationID)
                                .putExtra(Internal_activity__CustomerlyChat_Activity.EXTRA_ASSIGNER_ID, this._AssignerID), RESULT_CODE_REFRESH_LIST);
                    } else {
                        Toast.makeText(Internal_activity__CustomerlyList_Activity.this.getApplicationContext(), R.string.io_customerly__errore_connessione_probabile, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        private void apply(@NonNull Internal_entity__Conversation pConversation) {
            this._ConversationID = pConversation.conversation_id;
            this._AssignerID = pConversation.assigner_id;
            Customerly.get().loadRemoteImage(new Internal_Utils__RemoteImageHandler.Request()
                    .fitCenter()
                    .transformCircle()
                    .load(pConversation.getImageUrl(this._IconaSize))
                    .into(this._Icon)
                    .override(this._IconaSize, this._IconaSize)
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
