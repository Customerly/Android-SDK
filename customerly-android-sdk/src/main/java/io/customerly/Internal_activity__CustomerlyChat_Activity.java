package io.customerly;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Base64;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by Gianni on 03/09/16.
 * Project: CustomerlySDK
 */
public final class Internal_activity__CustomerlyChat_Activity extends Internal_activity__AInput_Customerly_Activity {

    static final String EXTRA_CONVERSATION_ID = "EXTRA_CONVERSATION_ID";

    private RecyclerView _ListRecyclerView;
    private ChatAdapter _Adapter;
    private static final long TYPING_NOONE = 0, UNKNOWN_ASSIGNER_ID = 0;
    private long _TypingAccountId = TYPING_NOONE;
    private LinearLayoutManager _LinearLayoutManager;
    @NonNull private ArrayList<Internal_entity__Message> _ChatList = new ArrayList<>(0);
    private long _ConversationID = 0, _AssignerID = UNKNOWN_ASSIGNER_ID;
    @NonNull private final ArrayList<BroadcastReceiver> _BroadcastReceiver = new ArrayList<>(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setResult(Internal_activity__CustomerlyList_Activity.RESULT_CODE_REFRESH_LIST);
        if(this.getIntent() == null) {
            this._ConversationID = 0;
        } else {
            this._ConversationID = this.getIntent().getLongExtra(EXTRA_CONVERSATION_ID, 0);
        }
        if(this._ConversationID == 0) {
            this.finish();
        } else if(this.onCreateLayout(R.layout.io_customerly__activity_chat)) {
            this._ListRecyclerView = (RecyclerView) this.findViewById(R.id.io_customerly__recyclerview);
            this._LinearLayoutManager = new LinearLayoutManager(this.getApplicationContext());
            this._LinearLayoutManager.setStackFromEnd(true);
            this._ListRecyclerView.setLayoutManager(this._LinearLayoutManager);
            this._ListRecyclerView.setItemAnimator(new DefaultItemAnimator());
            this._ListRecyclerView.setHasFixedSize(true);
            this._ListRecyclerView.setAdapter(this._Adapter = new ChatAdapter());

            this.input_input.addTextChangedListener(new TextWatcher() {
                boolean _TypingSent = false;
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void afterTextChanged(Editable s) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if(s.length() == 0) {
                        if(this._TypingSent) {
                            Customerly._Instance.__SOCKET_SEND_Typing(_ConversationID, false);
                            this._TypingSent = false;
                        }
                    } else {
                        if(! this._TypingSent) {
                            Customerly._Instance.__SOCKET_SEND_Typing(_ConversationID, true);
                            this._TypingSent = true;
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.onReconnection();
    }

    @Override
    protected void onReconnection() {
        if(Customerly._Instance._isConfigured()) {
            final ProgressBar progressview = (ProgressBar) this.findViewById(R.id.io_customerly__progressview);
            progressview.getIndeterminateDrawable().setColorFilter(Customerly._Instance.__PING__LAST_widget_color, android.graphics.PorterDuff.Mode.MULTIPLY);

            new Internal_api__CustomerlyRequest.Builder<ArrayList<Internal_entity__Message>>(Internal_api__CustomerlyRequest.ENDPOINT_MESSAGERETRIEVE)
                    .opt_checkConn(this)
                    .opt_converter(data -> Internal_Utils__Utils.fromJSONdataToList(data, "messages", Internal_entity__Message::new))
                    .opt_tokenMandatory()
                    .opt_receiver((responseState, list) -> {
                        if (responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK && list != null) {
                            if(this._AssignerID == UNKNOWN_ASSIGNER_ID) {
                                for (Internal_entity__Message m : list) {
                                    if (! m.isUserMessage() && m.getWriterID() != TYPING_NOONE) {
                                        this._AssignerID = m.getWriterID();
                                        break;
                                    }
                                }
                            }
                            this._ListRecyclerView.post(() -> {
                                progressview.setVisibility(View.GONE);
                                int visibleItemCount = this._LinearLayoutManager.getChildCount();
                                int totalItemCount = this._LinearLayoutManager.getItemCount();
                                int pastVisiblesItems = this._LinearLayoutManager.findFirstVisibleItemPosition();
                                this._ChatList = list;
                                if (pastVisiblesItems + visibleItemCount >= totalItemCount) {
                                    //End of list
                                    this._LinearLayoutManager.scrollToPosition(list.size() - 1);
                                }
                                this._Adapter.notifyDataSetChanged();
                                this._ListRecyclerView.setVisibility(View.VISIBLE);
                            });
                            Customerly._Instance.__SOCKET__Typing_listener = (pConversationID, account_id, pTyping) -> {
                                if(pTyping) {
                                    if(this._TypingAccountId == account_id) {
                                        return;//Se arriva evendo di startTyping e già stavo mostrando il typing per dello stesso account, returno
                                    }
                                } else {
                                    if(this._TypingAccountId == TYPING_NOONE) {
                                        return;//Se arriva evento di stopTyping e già non stavo mostrando il typing returno
                                    }
                                }
                                if (this._ConversationID == pConversationID) {
                                    this._ListRecyclerView.post(() -> {
                                        if (pTyping) {
                                            int visibleItemCount = this._LinearLayoutManager.getChildCount();
                                            int totalItemCount = this._LinearLayoutManager.getItemCount();
                                            int pastVisiblesItems = this._LinearLayoutManager.findFirstVisibleItemPosition();
                                            this._TypingAccountId = account_id;
                                            this._Adapter.notifyItemInserted(this._ChatList.size());
                                            if (pastVisiblesItems + visibleItemCount >= totalItemCount) {
                                                //End of list
                                                this._LinearLayoutManager.scrollToPosition(this._ChatList.size());
                                            }
                                        } else {
                                            this._TypingAccountId = TYPING_NOONE;
                                            this._Adapter.notifyItemRemoved(this._ChatList.size());
                                        }
                                    });
                                }
                            };

                            Customerly._Instance.__SOCKET__Message_listener = pNewMessages -> {
                                final ArrayList<Internal_entity__Message> lista = new ArrayList<>(this._ChatList);

                                //Ordina la lista iniziale (necessario per velocizzare la ricerca duplicati)
                                Collections.sort(lista, (m1, m2) -> (int) (m1.conversation_message_id - m2.conversation_message_id));
                                long mostRecentMessageId = -1;
                                ArrayList<Internal_entity__Message> messaggi_da_aggiungere = new ArrayList<>(pNewMessages.size());
                                messaggio_gia_in_lista__non_viene_aggiunto:
                                for (Internal_entity__Message message : pNewMessages) {
                                    if (message.conversation_id == this._ConversationID) {//Filtra per conversazioneID
                                        if (message.assigner_id != UNKNOWN_ASSIGNER_ID && this._AssignerID == UNKNOWN_ASSIGNER_ID) {
                                            this._AssignerID = message.assigner_id;
                                        }
                                        if (message.conversation_message_id > mostRecentMessageId) {
                                            mostRecentMessageId = message.conversation_message_id;
                                        }
                                        for (int i = lista.size() - 1; i >= 0; i--) {
                                            long esistenteID = lista.get(i).conversation_message_id;
                                            if (esistenteID == message.conversation_message_id) {
                                                continue messaggio_gia_in_lista__non_viene_aggiunto;//Se è un duplicato non lo aggiunge
                                            }
                                            if (esistenteID < message.conversation_message_id) {
                                                break; //Avendo ordinato la lista per id, posso già dire che non è un duplicato
                                            }
                                        }
                                        messaggi_da_aggiungere.add(message);
                                    }
                                }

                                lista.addAll(messaggi_da_aggiungere);
                                Collections.sort(lista, (m1, m2) -> (int) (m1.conversation_message_id - m2.conversation_message_id));

                                this._ListRecyclerView.post(() -> {
                                    int visibleItemCount = this._LinearLayoutManager.getChildCount();
                                    int totalItemCount = this._LinearLayoutManager.getItemCount();
                                    int pastVisiblesItems = this._LinearLayoutManager.findFirstVisibleItemPosition();
                                    this._ChatList = lista;
                                    if (pastVisiblesItems + visibleItemCount >= totalItemCount) {
                                        //End of list
                                        this._LinearLayoutManager.scrollToPosition(lista.size() - 1);
                                    }
                                    this._Adapter.notifyDataSetChanged();
                                });

                                if (mostRecentMessageId != -1) {
                                    this.sendSeen(mostRecentMessageId);
                                }
                            };

                            long messageID_seen = -1;
                            for (Internal_entity__Message message : list) {
                                if (message.isNotSeen() && message.conversation_message_id > messageID_seen) {
                                    messageID_seen = message.conversation_message_id;
                                }
                            }

                            if (messageID_seen != -1) {
                                this.sendSeen(messageID_seen);
                            }
                        } else {
                            this.finish();
                            Toast.makeText(getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .opt_trials(2)
                    .param("conversation_id", this._ConversationID)
                    .start();
        }
    }

    private void sendSeen(final long messageID_seen) {
        final Internal_Utils__ResultUtils.OnNonNullResult<Long> onSuccess = utc -> {
            utc /= 1000;
            if(this._AssignerID != UNKNOWN_ASSIGNER_ID) {
                Customerly._Instance.__SOCKET_SEND_Seen(messageID_seen, utc);
            }
            new Internal_api__CustomerlyRequest.Builder<Void>(Internal_api__CustomerlyRequest.ENDPOINT_MESSAGESEEN)
                    .opt_checkConn(this)
                    .opt_tokenMandatory()
                    .param("conversation_message_id", messageID_seen)
                    .param("seen_date", utc)
                    .start();
        };
        Internal_Utils__NTP_Utils.getSafeNow_fromUiThread(this, onSuccess, () -> onSuccess.onResult(System.currentTimeMillis()));
    }

    @Override
    protected void onDestroy() {
        //noinspection Convert2streamapi
        for(BroadcastReceiver br : this._BroadcastReceiver) {
            this.unregisterReceiver(br);
        }
        Customerly._Instance.__SOCKET__Typing_listener = null;
        if(this._AssignerID != UNKNOWN_ASSIGNER_ID) {
            Customerly._Instance.__SOCKET_SEND_Typing(this._ConversationID, false);
        }
        super.onDestroy();
    }

    @Override
    protected void onInputActionSend_PerformSend(@NonNull String pMessage, @NonNull Internal_entity__Attachment[] pAttachments, @Nullable String ghostToVisitorEmail) {
        Internal__JWTtoken token = Customerly._Instance._JWTtoken;
        if(token != null && token._UserID != null) {
            Internal_entity__Message message = new Internal_entity__Message(token._UserID, this._ConversationID, pMessage, pAttachments);
            this._ListRecyclerView.post(() -> {
                int visibleItemCount = this._LinearLayoutManager.getChildCount();
                int totalItemCount = this._LinearLayoutManager.getItemCount();
                int pastVisiblesItems = this._LinearLayoutManager.findFirstVisibleItemPosition();
                message.setSending();
                this._ChatList.add(message);
                this._Adapter.notifyItemInserted(this._ChatList.size() - 1);
                if (pastVisiblesItems + visibleItemCount >= totalItemCount) {
                    //End of list
                    this._LinearLayoutManager.scrollToPosition(_Adapter.getItemCount() - 1);
                }
                this.startSendMessageRequest(message);
            });
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N)
    private void startSendMessageRequest(@NonNull Internal_entity__Message message) {
        new Internal_api__CustomerlyRequest.Builder<Internal_entity__Message>(Internal_api__CustomerlyRequest.ENDPOINT_MESSAGESEND)
                .opt_checkConn(this)
                .opt_tokenMandatory()
                .opt_converter(data -> {
                    Customerly._Instance.__SOCKET_SEND_Message(data.optLong("timestamp", -1L));
                    return new Internal_entity__Message(data.optJSONObject("message"));
                })
                .opt_receiver((responseState, messageSent) ->
                    this._ListRecyclerView.post(() -> {
                        int pos = this._ChatList.indexOf(message);
                        if (pos != -1) {
                            if(responseState == Internal_api__CustomerlyRequest.RESPONSE_STATE__OK) {
                                this._ChatList.set(pos, messageSent);
                            } else {
                                message.setFailed();
                                Toast.makeText(getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                            }
                            this._Adapter.notifyItemChanged(pos);
                        }
                    }))
                .opt_trials(2)
                .param("conversation_id", this._ConversationID)
                .param("message", message.content == null ? "" : Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? Html.toHtml(message.content, 0) : Html.toHtml(message.content))
                .param("attachments", Internal_entity__Attachment.toSendJSONObject(this, message._Attachments))
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

    private static final int PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE = 99;
    private String _PermissionRequest__pendingFileName, _PermissionRequest__pendingPath;
    private void startAttachmentDownload(@NonNull String filename, @NonNull String fullpath) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            final DownloadManager dm = (DownloadManager) this.getSystemService(DOWNLOAD_SERVICE);
            final long downloadReference = dm.enqueue(
                    new DownloadManager.Request(Uri.parse(fullpath))
                            .setTitle(filename)
                            .setDescription(filename)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                            .setVisibleInDownloadsUi(true)
                            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE));

            BroadcastReceiver br = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(downloadReference == intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)) {

                        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                                .notify(999, new NotificationCompat.Builder(Internal_activity__CustomerlyChat_Activity.this)
                                        .setSmallIcon(Internal_activity__CustomerlyChat_Activity.this.getApplication().getApplicationInfo().icon)
                                        .setContentTitle(filename)
                                        .setContentText(filename)
                                        .setAutoCancel(true)
                                        .setContentIntent(PendingIntent.getActivity(
                                                Internal_activity__CustomerlyChat_Activity.this,
                                                0,
                                                new Intent().setAction(DownloadManager.ACTION_VIEW_DOWNLOADS),
                                                PendingIntent.FLAG_UPDATE_CURRENT
                                        )).build());

                        Toast toast = Toast.makeText(Internal_activity__CustomerlyChat_Activity.this, R.string.io_customerly__download_complete, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.TOP, 25, 400);
                        toast.show();
                    }
                }
            };

            this._BroadcastReceiver.add(br);

            this.registerReceiver(br, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        } else {
            this._PermissionRequest__pendingFileName = filename;
            this._PermissionRequest__pendingPath = fullpath;
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.io_customerly__permission_request)
                        .setMessage(R.string.io_customerly__permission_request_explanation_write)
                        .setPositiveButton(android.R.string.ok, (dlg, which) ->
                                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE))
                            .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(this._PermissionRequest__pendingFileName != null && this._PermissionRequest__pendingFileName.length() != 0
                            && this._PermissionRequest__pendingPath != null && this._PermissionRequest__pendingPath.length() != 0) {
                        this.startAttachmentDownload(this._PermissionRequest__pendingFileName, this._PermissionRequest__pendingPath);
                        this._PermissionRequest__pendingFileName = null;
                        this._PermissionRequest__pendingPath = null;
                    }
                } else {
                    Toast.makeText(this, R.string.io_customerly__permission_denied_write, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private abstract class A_ChatVH extends RecyclerView.ViewHolder {
        @NonNull final TextView _Data, _Content;
        @NonNull final ImageView _Icon;
        final int _IconaSize;
        private A_ChatVH(@LayoutRes int pLayourRes) {
            super(getLayoutInflater().inflate(pLayourRes, _ListRecyclerView, false));
            this._Data = (TextView)this.itemView.findViewById(R.id.io_customerly__time);
            this._Content = (TextView)this.itemView.findViewById(R.id.io_customerly__content);
            this._Icon = (ImageView)this.itemView.findViewById(R.id.io_customerly__icona);
            ViewGroup.LayoutParams lp = this._Icon.getLayoutParams();
            lp.width = lp.height = _IconaSize = getResources().getDimensionPixelSize(R.dimen.io_customerly__chat_li_icon_size);
        }
        void clearAnimation() {
            this.itemView.clearAnimation();
        }

        protected abstract void apply(@Nullable Internal_entity__Message pMessage, @Nullable String pDataDaMostrare, boolean pIsFirstMessageOfSender, boolean pShouldAnimate);
    }

    private class ChatTypingVH extends A_ChatVH {
        private static final long DOTS_SPEED = 500;
        private ChatTypingVH() {
            super(R.layout.io_customerly__li_message_account_typing);
            this.startAnimation();
        }

        protected void apply(@Nullable Internal_entity__Message __null, @Nullable String _NonSiMostraNelTyping, boolean pIsFirstMessageOfSender, boolean pShouldAnimate) {
            long typingAccountID = _TypingAccountId;
            if (pIsFirstMessageOfSender && typingAccountID != TYPING_NOONE) {
                Customerly._Instance._RemoteImageHandler.request(new Internal_Utils__RemoteImageHandler.Request()
                        .fitCenter()
                        .transformCircle()
                        .load(Internal_entity__Account.getAccountImageUrl(typingAccountID, this._IconaSize))
                        .into(this._Icon)
                        .override(this._IconaSize, this._IconaSize)
                        .placeholder(R.drawable.io_customerly__ic_default_admin));
                this._Icon.setVisibility(View.VISIBLE);
            } else {
                this._Icon.setVisibility(View.INVISIBLE);
            }
        }

        private void startAnimation() {
            this._Content.setText(".    ");
            this._Content.requestLayout();
            this._Content.postDelayed(() -> {
                this._Content.setText(". .  ");
                this._Content.requestLayout();
                this._Content.postDelayed(() -> {
                    this._Content.setText(". . .");
                    this._Content.requestLayout();
                    this._Content.postDelayed(this::startAnimation, DOTS_SPEED);
                }, DOTS_SPEED);
            }, DOTS_SPEED);
        }
    }

    private class ChatUserMessageVH extends A_ChatMessageVH {
        @NonNull private final View _IfUserMessage_ContentLayout;
        private ChatUserMessageVH() {
            super(R.layout.io_customerly__li_message_user, R.drawable.io_customerly__bkg_chatmessage_user_rounded, R.drawable.io_customerly__ic_attach_user, 0.9f);
            this._IfUserMessage_ContentLayout = this.itemView.findViewById(R.id.io_customerly__content_layout__onlyuserli);
        }
        @Override protected void setContentVisibility(@ContentVisibility int visibility, boolean isSending) {
            if(visibility == View.VISIBLE) {
                this._IfUserMessage_ContentLayout.setVisibility(View.VISIBLE);
            } else if(isSending) {
                this._Content.setText(null);
                this._IfUserMessage_ContentLayout.setVisibility(View.VISIBLE);
            } else {
                this._IfUserMessage_ContentLayout.setVisibility(View.GONE);
            }
        }
    }

    private class ChatAccountMessageVH extends A_ChatMessageVH {
        private ChatAccountMessageVH() {
            super(R.layout.io_customerly__li_message_account, R.drawable.io_customerly__bkg_chatmessage_account_rounded, R.drawable.io_customerly__ic_attach_account, -0.9f);
        }
        @Override protected void setContentVisibility(@ContentVisibility int visibility, boolean isSending) {
            this._Content.setVisibility(visibility);
        }
    }

    private class ChatAccountRichMessageVH extends A_ChatMessageVH {
        private ChatAccountRichMessageVH() {
            super(R.layout.io_customerly__li_message_account_rich, R.drawable.io_customerly__bkg_chatmessage_account_rounded, R.drawable.io_customerly__ic_attach_account, -0.9f);
        }
        @Override protected void setContentVisibility(@ContentVisibility int visibility, boolean isSending) {
            //Do nothing. Content text defined in xml
        }

        @Override
        protected void apply(@Nullable Internal_entity__Message pMessage, @Nullable String pDataDaMostrare, boolean pIsFirstMessageOfSender, boolean pShouldAnimate) {
            super.apply(pMessage, pDataDaMostrare, pIsFirstMessageOfSender, pShouldAnimate);
            this._Content.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.io_customerly__ic_email_grey_32dp, 0, 0);
            View.OnClickListener clickListener = v -> {
                if(pMessage != null && pMessage.rich_mail_token != null) {
                    Internal_Utils__Utils.intentUrl(Internal_activity__CustomerlyChat_Activity.this,
                            String.format(Locale.UK, "https://app.customerly.io/email/view/%d/%s",
                                    pMessage.conversation_message_id, pMessage.rich_mail_token));
                }
            };
            this.itemView.setOnClickListener(clickListener);
            this._Content.setOnClickListener(clickListener);
        }
    }

    private abstract class A_ChatMessageVH extends A_ChatVH {
        @NonNull private final LinearLayout _AttachmentLayout;
        @Nullable private final View _Sending;
        @DrawableRes private final int _BubbleBkgResID, _IcAttachResID;
        private final float _ItemFromXValueRelative;

        private A_ChatMessageVH(@LayoutRes int pLayourRes, @DrawableRes int pBubbleBkgRedID, @DrawableRes int pIcAttachResID, @FloatRange(from=-1, to=1) float pItemFromXValueRelative) {
            super(pLayourRes);
            this._Sending = this.itemView.findViewById(R.id.io_customerly__content_sending__onlyuserli);
            this._Content.setMovementMethod(LinkMovementMethod.getInstance());
            this._AttachmentLayout = (LinearLayout)this.itemView.findViewById(R.id.io_customerly__attachment_layout);
            this._BubbleBkgResID = pBubbleBkgRedID;
            this._IcAttachResID = pIcAttachResID;
            this._ItemFromXValueRelative = pItemFromXValueRelative;
        }
        @Override protected void apply(@Nullable Internal_entity__Message pMessage, @Nullable String pDataDaMostrare, boolean pIsFirstMessageOfSender, boolean pShouldAnimate) {
            if (pMessage != null) {//Sempre != null per questo ViewHolder
                if(pDataDaMostrare != null) {
                    this._Data.setText(pDataDaMostrare);
                    this._Data.setVisibility(View.VISIBLE);
                } else {
                    this._Data.setVisibility(View.GONE);
                }

                if (pIsFirstMessageOfSender) {
                    Customerly._Instance._RemoteImageHandler.request(new Internal_Utils__RemoteImageHandler.Request()
                            .fitCenter()
                            .transformCircle()
                            .load(pMessage.getImageUrl(this._IconaSize))
                            .into(this._Icon)
                            .override(this._IconaSize, this._IconaSize)
                            .placeholder(R.drawable.io_customerly__ic_default_admin));
                    this._Icon.setVisibility(View.VISIBLE);
                } else {
                    this._Icon.setVisibility(View.GONE);
                }

                if(pMessage.content != null && pMessage.content.length() != 0) {
                    ClickableSpan[] click_spans = pMessage.content.getSpans(0, pMessage.content.length(),  ClickableSpan.class);
                    for (ClickableSpan c_span : click_spans) {
                        pMessage.content.removeSpan(c_span);
                    }
                    ImageSpan[] image_spans = pMessage.content.getSpans(0, pMessage.content.length(), ImageSpan.class);
                    for(final ImageSpan is : image_spans) {
                        if(is.getSource() != null) {
                            pMessage.content.setSpan(new ClickableSpan() {
                                @Override
                                public void onClick(View widget) {
                                    startActivity(new Intent(Internal_activity__CustomerlyChat_Activity.this, Internal_activity__FullScreenImage_Activity.class)
                                            .putExtra(Internal_activity__FullScreenImage_Activity.EXTRA_IMAGESOURCE, is.getSource()));
                                }
                            }, pMessage.content.getSpanStart(is), pMessage.content.getSpanEnd(is), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }

                    this._Content.setText(pMessage.content);
                    this.setContentVisibility(View.VISIBLE, pMessage.isSending());
                } else {
                    this.setContentVisibility(View.GONE, pMessage.isSending());
                }

                if (this._Sending != null) {
                    this._Sending.setVisibility(pMessage.isSending() ? View.VISIBLE : View.GONE);
                }

                if (pMessage.isFailed()) {
                    this._Content.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.io_customerly__ic_error, 0);
                    View.OnClickListener clickListener = v -> {
                        if(Customerly._Instance._isConfigured()) {
                            pMessage.setSending();
                            int pos = _ChatList.indexOf(pMessage);
                            if (pos != -1) {
                                _Adapter.notifyItemChanged(pos);
                            }
                            startSendMessageRequest(pMessage);
                        }
                    };
                    this._Content.setOnClickListener(clickListener);
                    this.itemView.setOnClickListener(clickListener);
                } else {
                    this._Content.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    this._Content.setOnClickListener(null);
                    this.itemView.setOnClickListener(null);
                }

                this._AttachmentLayout.removeAllViews();
                Internal_entity__Attachment[] attachments = pMessage._Attachments;
                if (attachments != null) {
                    for (Internal_entity__Attachment attachment : attachments) {

                        LinearLayout ll = new LinearLayout(Internal_activity__CustomerlyChat_Activity.this);
                        ll.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        ll.setOrientation(LinearLayout.VERTICAL);
                        ll.setGravity(Gravity.CENTER_HORIZONTAL);
                        ll.setBackgroundResource(this._BubbleBkgResID);
                        ImageView iv = new ImageView(Internal_activity__CustomerlyChat_Activity.this);
                        if(attachment.isImage()) {
                            //Immagine
                            iv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Internal_Utils__Utils.px(80)));
                            if (attachment.path != null && attachment.path.length() != 0) {
                                Customerly._Instance._RemoteImageHandler.request(new Internal_Utils__RemoteImageHandler.Request()
                                        .centerCrop()
                                        .load(attachment.path)
                                        .into(iv)
                                        .placeholder(R.drawable.io_customerly__pic_placeholder));
                                ll.setOnClickListener(layout -> startActivity(new Intent(Internal_activity__CustomerlyChat_Activity.this, Internal_activity__FullScreenImage_Activity.class)
                                        .putExtra(Internal_activity__FullScreenImage_Activity.EXTRA_IMAGESOURCE, attachment.path)));
                            } else {
                                try {
                                    String base64 = attachment.loadBase64FromMemory(Internal_activity__CustomerlyChat_Activity.this);
                                    if(base64 != null) {
                                        byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                        iv.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                                    } else {
                                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                        iv.setImageResource(R.drawable.io_customerly__pic_placeholder);
                                    }
                                } catch (OutOfMemoryError outOfMemoryError) {
                                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    iv.setImageResource(R.drawable.io_customerly__pic_placeholder);
                                }
                            }
                        } else { //Allegato non immagine
                            iv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Internal_Utils__Utils.px(80)));
                            iv.setImageResource(this._IcAttachResID);

                            if (attachment.path != null && attachment.path.length() != 0) {
                                ll.setOnClickListener(layout -> new AlertDialog.Builder(Internal_activity__CustomerlyChat_Activity.this)
                                        .setTitle(R.string.io_customerly__download)
                                        .setMessage(R.string.io_customerly__download_the_file_)
                                        .setPositiveButton(android.R.string.ok, (dlg, which) -> Internal_activity__CustomerlyChat_Activity.this.startAttachmentDownload(attachment.name, attachment.path))
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .setCancelable(true)
                                        .show());
                            }
                        }
                        ll.addView(iv);

                        TextView tv = new TextView(Internal_activity__CustomerlyChat_Activity.this);
                        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        tv.setTextColor(Internal_Utils__Utils.getColorStateListFromResource(getResources(), R.color.io_customerly__textcolor_blue2_grey));
                        tv.setLines(1);
                        tv.setSingleLine();
                        tv.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                        tv.setText(attachment.name);
                        tv.setPadding(0, Internal_Utils__Utils.px(10), 0, 0);
                        ll.addView(tv);
                        this._AttachmentLayout.addView(ll);
                        ((LinearLayout.LayoutParams)ll.getLayoutParams()).topMargin = Internal_Utils__Utils.px(3);
                    }
                    this._AttachmentLayout.setVisibility(View.VISIBLE);
                } else {
                    this._AttachmentLayout.setVisibility(View.GONE);
                }
                if(pShouldAnimate) {
                    TranslateAnimation ta = new TranslateAnimation(Animation.RELATIVE_TO_SELF, this._ItemFromXValueRelative, Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0);
                    ta.setDuration(300);
                    ta.setFillBefore(true);
                    ta.setFillAfter(true);
                    this.itemView.startAnimation(ta);
                }
            } else {//Caso impossibile ma tanto per scrupolo...
                this._Icon.setVisibility(View.INVISIBLE);
                this._Content.setText(null);
                if (this._Sending != null) {
                    this._Sending.setVisibility(View.GONE);
                }
                this._Content.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                this._Content.setOnClickListener(null);
                this.itemView.setOnClickListener(null);
                this._AttachmentLayout.removeAllViews();
                this._AttachmentLayout.setVisibility(View.GONE);
            }
        }
        protected abstract void setContentVisibility(@ContentVisibility int visibility, boolean isSending);
    }

    @IntDef({View.VISIBLE, View.GONE})
    @Retention(RetentionPolicy.SOURCE)
    @interface ContentVisibility {}

    private class ChatAdapter extends RecyclerView.Adapter<A_ChatVH> {
        final int _5dp = Internal_Utils__Utils.px(5), _FirstMessageOfSenderTopPadding = this._5dp * 3;
        int lastPositionAnimated = Integer.MAX_VALUE;
        int firstPositionAnimated = -1;
        private final long _TODAY_inSec = (System.currentTimeMillis() / (1000 * 60 * 60 * 24)) * (/*1000**/ 60 * 60 * 24);
        @Override
        public int getItemViewType(int position) {
            if(_TypingAccountId != TYPING_NOONE && position == this.getItemCount() - 1) {
                return R.layout.io_customerly__li_message_account_typing;
            } else {
                Internal_entity__Message message = _ChatList.get(position);
                if(message.isUserMessage()) {
                    return R.layout.io_customerly__li_message_user;
                } else if(message.rich_mail_token == null) {
                    return R.layout.io_customerly__li_message_account;
                } else {
                    return R.layout.io_customerly__li_message_account_rich;
                }
            }
        }
        @Override
        public A_ChatVH onCreateViewHolder(ViewGroup parent, int viewType) {
            return viewType == R.layout.io_customerly__li_message_account_typing ? new ChatTypingVH()
                    : viewType == R.layout.io_customerly__li_message_user ? new ChatUserMessageVH()
                    : viewType == R.layout.io_customerly__li_message_account ? new ChatAccountMessageVH()
                    : /*viewType == R.layout.io_customerly__li_message_account_rich ? */ new ChatAccountRichMessageVH();
        }
        @Override
        public void onBindViewHolder(A_ChatVH holder, @SuppressLint("RecyclerView") int position) {
            Internal_entity__Message thisMessage = null, previousMessage;
            boolean shouldAnimate = false, firstMessageOfSender;
            String dataDaMostrare = null;
            if(_TypingAccountId == TYPING_NOONE || position != this.getItemCount() - 1) {
                if(position < this.lastPositionAnimated) {
                    shouldAnimate = true;
                    this.lastPositionAnimated = position;
                }
                if(position > this.firstPositionAnimated) {
                    shouldAnimate = true;
                    this.firstPositionAnimated = position;
                }
                thisMessage = _ChatList.get(position);
            }
            previousMessage = position == 0 ? null : _ChatList.get(position - 1);

            firstMessageOfSender = previousMessage == null
                    || ( thisMessage == null
                        ? ! previousMessage.isUserMessage()
                        : ! thisMessage.hasSameSenderOf(previousMessage) );

            if(thisMessage != null) {
                dataDaMostrare = thisMessage.toStringDate(_TODAY_inSec);
                if(previousMessage != null && dataDaMostrare.equals(previousMessage.toStringDate(_TODAY_inSec))) {
                    dataDaMostrare = null;
                }
            }

            holder.apply(thisMessage, dataDaMostrare, firstMessageOfSender, shouldAnimate);

            holder.itemView.setPadding(0, firstMessageOfSender ? this._FirstMessageOfSenderTopPadding : 0, 0, position == this.getItemCount() - 1 ? this._5dp : 0);
            //Dà un padding top di 15dp ad ogni primo messaggio dello stesso mittente
            //Dà un padding bottom di 5dp all'ultimo messaggio
        }
        @Override
        public int getItemCount() {
            return _ChatList.size() + (_TypingAccountId == TYPING_NOONE ? 0 : 1);
        }
        @Override
        public void onViewDetachedFromWindow(final A_ChatVH holder) {
            holder.clearAnimation();
        }
    }
}
