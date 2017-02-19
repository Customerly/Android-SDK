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

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
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
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Gianni on 03/09/16.
 * Project: Customerly Android SDK
 */
public final class IAct_Chat extends IAct_AInput implements Customerly.SDKActivity {

    static final String EXTRA_CONVERSATION_ID= "EXTRA_CONVERSATION_ID";
    private static final int MESSAGES_PER_PAGE = 20, TYPING_NO_ONE = 0;

    @Nullable private RecyclerView _ListRecyclerView;
    @Nullable private IU_ProgressiveScrollListener _IU_ProgressiveScrollListener;
    @Nullable private ProgressBar _Progress_view;
    @NonNull private final ChatAdapter _Adapter = new ChatAdapter();
    private long _TypingAccountId = TYPING_NO_ONE, _ConversationID = 0;
    @Nullable private LinearLayoutManager _LinearLayoutManager;
    @NonNull private ArrayList<IE_Message> _ChatList = new ArrayList<>(0);
    @NonNull private final ArrayList<BroadcastReceiver> _BroadcastReceiver = new ArrayList<>(1);
    @NonNull private final IU_ProgressiveScrollListener.OnBottomReachedListener _OnBottomReachedListener = (scrollListener) -> {
        if(Customerly._Instance._isConfigured()) {
            long oldestMessageId = Long.MAX_VALUE;
            for(int i = 0; i < this._ChatList.size(); i++) {
                try {
                    long currentMessageId = this._ChatList.get(i).conversation_message_id;
                    if(currentMessageId < oldestMessageId) {
                        oldestMessageId = currentMessageId;
                    }
                } catch (Exception ignored) { /* concurrence */ }
            }

            new IApi_Request.Builder<ArrayList<IE_Message>>(IApi_Request.ENDPOINT_MESSAGE_RETRIEVE)
                    .opt_checkConn(this)
                    .opt_onPreExecute(() -> IU_NullSafe.setVisibility(this._Progress_view, View.VISIBLE))
                    .opt_converter(data -> IU_Utils.fromJSONdataToList(data, "messages", IE_Message::new))
                    .opt_tokenMandatory()
                    .opt_receiver((responseState, pNewMessages) -> {
                        if (responseState == IApi_Request.RESPONSE_STATE__OK && pNewMessages != null) {

                            final ArrayList<IE_Message> new_messages = new ArrayList<>(this._ChatList);
                            int previoussize = new_messages.size();

                            Collections.sort(pNewMessages, (m1, m2) -> (int) (m2.conversation_message_id - m1.conversation_message_id));//Sorting by conversation_message_id DESC);
                            //noinspection Convert2streamapi
                            for(IE_Message newMsg : pNewMessages) {
                                if(! this._ChatList.contains(newMsg)) {           //Avoid duplicates;
                                    new_messages.add(newMsg);
                                }
                            }
                            int addeditem = new_messages.size() - previoussize;
                            IU_NullSafe.post(this._ListRecyclerView, () -> {
                                IU_NullSafe.setVisibility(this._Progress_view, View.GONE);
                                IU_NullSafe.setVisibility(this._ListRecyclerView, View.VISIBLE);
                                if(addeditem > 0) {
                                    this._ChatList = new_messages;
                                    this.adjustBottomScroll();
                                    if (previoussize != 0) {
                                        this._Adapter.notifyItemChanged(previoussize - 1);
                                    }
                                    this._Adapter.notifyItemRangeInserted(previoussize, addeditem);
                                }
                            });

                            if(new_messages.size() != 0) {
                                IE_Message last = new_messages.get(0);
                                if (last.isNotSeen()) {
                                    this.sendSeen(last.conversation_message_id);
                                }
                            }

                            if(scrollListener != null && pNewMessages.size() >= MESSAGES_PER_PAGE) {
                                scrollListener.onFinishedUpdating();
                            }
                        } else {
                            IU_NullSafe.setVisibility(this._Progress_view, View.GONE);
                            Toast.makeText(getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .opt_trials(2)
                    .param("conversation_id", this._ConversationID)
                    .param("per_page", MESSAGES_PER_PAGE)
                    .param("messages_before_id", oldestMessageId)
                    .start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setResult(IAct_List.RESULT_CODE_REFRESH_LIST);
        if(this.getIntent() == null) {
            this._ConversationID = 0;
        } else {
            this._ConversationID = this.getIntent().getLongExtra(EXTRA_CONVERSATION_ID, 0);
        }
        if(this._ConversationID == 0) {
            this.finish();
        } else if(this.onCreateLayout(R.layout.io_customerly__activity_chat)) {
            this._Progress_view = (ProgressBar) this.findViewById(R.id.io_customerly__progress_view);
            this._Progress_view.getIndeterminateDrawable().setColorFilter(Customerly._Instance.__PING__LAST_widget_color, android.graphics.PorterDuff.Mode.MULTIPLY);
            RecyclerView recyclerView = (RecyclerView) this.findViewById(R.id.io_customerly__recycler_view);
            this._LinearLayoutManager = new LinearLayoutManager(this.getApplicationContext());
            this._LinearLayoutManager.setReverseLayout(true);
            recyclerView.setLayoutManager(this._LinearLayoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setHasFixedSize(true);
            recyclerView.setAdapter(this._Adapter);
            recyclerView.addOnScrollListener(this._IU_ProgressiveScrollListener = new IU_ProgressiveScrollListener(this._LinearLayoutManager, this._OnBottomReachedListener));
            this._ListRecyclerView = recyclerView;

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

            Customerly._Instance.__SOCKET__Typing_listener = (pConversationID, account_id, pTyping) -> {
                if(pTyping) {
                    if(this._TypingAccountId == account_id) {
                        return;
                    }
                } else {
                    if(this._TypingAccountId == TYPING_NO_ONE) {
                        return;
                    }
                }
                if (this._ConversationID == pConversationID) {
                    this._ListRecyclerView.post(() -> {
                        if (pTyping) {
                            this._TypingAccountId = account_id;
                            this._Adapter.notifyItemInserted(0);
                            this.adjustBottomScroll();
                        } else {
                            this._TypingAccountId = TYPING_NO_ONE;
                            this._Adapter.notifyItemRemoved(0);
                        }
                    });
                }
            };
        }
    }

    static void start(@NonNull Activity activity, boolean mustShowBack, long conversationID) {
        IAct_Chat.startForResult(activity, mustShowBack, conversationID, -1);
    }

    static void startForResult(@NonNull Activity activity, boolean mustShowBack, long conversationID, int requestCode) {
        Intent intent = new Intent(activity, IAct_Chat.class).putExtra(IAct_AInput.EXTRA_MUST_SHOW_BACK, mustShowBack);
        if(conversationID > 0) {
            intent.putExtra(IAct_Chat.EXTRA_CONVERSATION_ID, conversationID);
        }
        if(requestCode == -1) {
            if(activity instanceof IAct_Chat) {
                //If i am starting a IAct_Chat Activity from a IAct_Chat activity i'll show the back button only if it is visible in the current IAct_Chat activity.
                //Then i finish the current activity to avoid long stack of IAct_Chat activities
                activity.startActivity(intent.putExtra(IAct_AInput.EXTRA_MUST_SHOW_BACK, ((IAct_Chat)activity)._MustShowBack));
                activity.finish();
            } else {
                activity.startActivity(intent);
            }
        } else {
            activity.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public void onNewSocketMessages(@NonNull ArrayList<IE_Message> messages) {
        final ArrayList<IE_Message> new_messages = new ArrayList<>(this._ChatList);
        IE_Message otherConversationMessage = null;
        //noinspection Convert2streamapi
        for(IE_Message newMsg : messages) {
            if(newMsg.conversation_id == this._ConversationID) {       //Filter by conversation_id
                if(! new_messages.contains(newMsg)) {           //Avoid duplicates;
                    new_messages.add(newMsg);
                }
            } else {
                if(otherConversationMessage == null || otherConversationMessage.conversation_message_id < newMsg.conversation_message_id) {
                    otherConversationMessage = newMsg;
                }
            }
        }

        if(otherConversationMessage != null) {
            try {
                PW_AlertMessage.show(this, otherConversationMessage);
            } catch (WindowManager.BadTokenException ignored) { }
        }

        Collections.sort(new_messages, (m1, m2) -> (int) (m2.conversation_message_id - m1.conversation_message_id));//Sorting by conversation_message_id DESC

        IU_NullSafe.post(this._ListRecyclerView, () -> {
            this._ChatList = new_messages;
            this.adjustBottomScroll();
            this._Adapter.notifyDataSetChanged();
        });

        if(new_messages.size() != 0) {
            IE_Message last = new_messages.get(0);
            if (last.isNotSeen()) {
                this.sendSeen(last.conversation_message_id);
            }
        }
    }

    private void adjustBottomScroll() {
        if (this._LinearLayoutManager != null && this._LinearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
            //Bottom of reversed list
            this._LinearLayoutManager.scrollToPosition(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IE_JwtToken jwt = Customerly._Instance._JwtToken;
        if(jwt == null || jwt.isAnonymous()) {
            this.onLogoutUser();
        } else {
            this._OnBottomReachedListener.onReached(this._IU_ProgressiveScrollListener);
        }
    }

    @Override
    public void onLogoutUser() {
        this.finish();
    }

    @Override
    protected void onReconnection() {
        this._OnBottomReachedListener.onReached(this._IU_ProgressiveScrollListener);
    }

    private void sendSeen(final long messageID_seen) {
        final IU_ResultUtils.OnNonNullResult<Long> onSuccess = utc -> {
            utc /= 1000;
            Customerly._Instance.__SOCKET_SEND_Seen(messageID_seen, utc);
            new IApi_Request.Builder<Void>(IApi_Request.ENDPOINT_MESSAGE_SEEN)
                    .opt_checkConn(this)
                    .opt_tokenMandatory()
                    .param("conversation_message_id", messageID_seen)
                    .param("seen_date", utc)
                    .start();
        };
        IU_NTP_Utils.getSafeNow_fromUiThread(this, onSuccess, () -> onSuccess.onResult(System.currentTimeMillis()));
    }

    @Override
    protected void onDestroy() {
        //noinspection Convert2streamapi
        for(BroadcastReceiver br : this._BroadcastReceiver) {
            this.unregisterReceiver(br);
        }
        Customerly._Instance.__SOCKET__Typing_listener = null;
        Customerly._Instance.__SOCKET_SEND_Typing(this._ConversationID, false);
        super.onDestroy();
    }

    @Override
    protected void onInputActionSend_PerformSend(@NonNull String pMessage, @NonNull IE_Attachment[] pAttachments, @Nullable String ghostToVisitorEmail) {
        IE_JwtToken token = Customerly._Instance._JwtToken;
        if(token != null && token._UserID != null) {
            IE_Message message = new IE_Message(token._UserID, this._ConversationID, pMessage, pMessage, pAttachments);
            IU_NullSafe.post(this._ListRecyclerView, () -> {
                message.setSending();
                this._ChatList.add(0, message);
                this._Adapter.notifyItemInserted(this._TypingAccountId == TYPING_NO_ONE ? 0 : 1);
                this.adjustBottomScroll();
                this.startSendMessageRequest(message);
            });
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N)
    private void startSendMessageRequest(@NonNull IE_Message message) {
        new IApi_Request.Builder<IE_Message>(IApi_Request.ENDPOINT_MESSAGE_SEND)
                .opt_checkConn(this)
                .opt_tokenMandatory()
                .opt_converter(data -> {
                    Customerly._Instance.__SOCKET_SEND_Message(data.optLong("timestamp", -1L));
                    return new IE_Message(data.optJSONObject("message"));
                })
                .opt_receiver((responseState, messageSent) ->
                    IU_NullSafe.post(this._ListRecyclerView, () -> {
                        int pos = this._ChatList.indexOf(message);
                        if (pos != -1) {
                            if(responseState == IApi_Request.RESPONSE_STATE__OK) {
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
                .param("attachments", IE_Attachment.toSendJSONObject(this, message._Attachments))
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

    private static final int PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE = 4321;
    private String _PermissionRequest__pendingFileName, _PermissionRequest__pendingPath;
    private void startAttachmentDownload(@NonNull String filename, @NonNull String full_path) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            final DownloadManager dm = (DownloadManager) this.getSystemService(DOWNLOAD_SERVICE);
            final long downloadReference = dm.enqueue(
                    new DownloadManager.Request(Uri.parse(full_path))
                            .setTitle(this.getString(R.string.io_customerly__download_ongoing))
                            .setDescription(filename)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                            .setVisibleInDownloadsUi(true)
                            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE));

            BroadcastReceiver br = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(downloadReference == intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)) {

                        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                                .notify(999, new NotificationCompat.Builder(IAct_Chat.this)
                                        .setSmallIcon(IAct_Chat.this.getApplication().getApplicationInfo().icon)
                                        .setContentTitle(getString(R.string.io_customerly__download_complete))
                                        .setContentText(filename)
                                        .setAutoCancel(true)
                                        .setContentIntent(PendingIntent.getActivity(
                                                IAct_Chat.this,
                                                0,
                                                new Intent().setAction(DownloadManager.ACTION_VIEW_DOWNLOADS),
                                                PendingIntent.FLAG_UPDATE_CURRENT
                                        )).build());

                        Toast toast = Toast.makeText(IAct_Chat.this, R.string.io_customerly__download_complete, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.TOP, 25, 400);
                        toast.show();
                    }
                }
            };

            this._BroadcastReceiver.add(br);

            this.registerReceiver(br, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        } else {
            this._PermissionRequest__pendingFileName = filename;
            this._PermissionRequest__pendingPath = full_path;
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
                int length = Math.min(grantResults.length, permissions.length);
                if (length > 0) {
                    for(int i = 0; i < length; i++) {
                        if(Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[i])
                                && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            if(this._PermissionRequest__pendingFileName != null && this._PermissionRequest__pendingFileName.length() != 0
                                    && this._PermissionRequest__pendingPath != null && this._PermissionRequest__pendingPath.length() != 0) {
                                this.startAttachmentDownload(this._PermissionRequest__pendingFileName, this._PermissionRequest__pendingPath);
                                this._PermissionRequest__pendingFileName = null;
                                this._PermissionRequest__pendingPath = null;
                            }
                            return;
                        }
                    }
                }
                Toast.makeText(this, R.string.io_customerly__permission_denied_write, Toast.LENGTH_LONG).show();
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    private abstract class A_ChatVH extends RecyclerView.ViewHolder {
        @NonNull final TextView _Date, _Time, _Content;
        @NonNull final ImageView _Icon;
        final int _IconSize;
        private A_ChatVH(@LayoutRes int pLayoutRes) {
            super(getLayoutInflater().inflate(pLayoutRes, _ListRecyclerView, false));
            this._Date = (TextView)this.itemView.findViewById(R.id.io_customerly__date);
            this._Time = (TextView)this.itemView.findViewById(R.id.io_customerly__time);
            this._Content = (TextView)this.itemView.findViewById(R.id.io_customerly__content);
            this._Icon = (ImageView)this.itemView.findViewById(R.id.io_customerly__icon);
            ViewGroup.LayoutParams lp = this._Icon.getLayoutParams();
            lp.width = lp.height = _IconSize = getResources().getDimensionPixelSize(R.dimen.io_customerly__chat_li_icon_size);
        }
        void clearAnimation() {
            this.itemView.clearAnimation();
        }

        protected abstract void apply(@Nullable IE_Message pMessage, @Nullable String pDateToDisplay, boolean pIsFirstMessageOfSender, boolean pShouldAnimate);
    }

    private class ChatTypingVH extends A_ChatVH {
        private static final long DOTS_SPEED = 500;
        private ChatTypingVH() {
            super(R.layout.io_customerly__li_bubble_account_typing);
            this.startAnimation();
        }

        protected void apply(@Nullable IE_Message __null, @Nullable String _NoDatesForTyping, boolean pIsFirstMessageOfSender, boolean pShouldAnimate) {
            long typingAccountID = _TypingAccountId;
            if (pIsFirstMessageOfSender && typingAccountID != TYPING_NO_ONE) {
                Customerly._Instance._RemoteImageHandler.request(new IU_RemoteImageHandler.Request()
                        .fitCenter()
                        .transformCircle()
                        .load(IE_Account.getAccountImageUrl(typingAccountID, this._IconSize))
                        .into(this._Icon)
                        .override(this._IconSize, this._IconSize)
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
        private ChatUserMessageVH() {
            super(R.layout.io_customerly__li_bubble_user, R.drawable.io_customerly__ic_attach_user, 0.9f);
        }
    }

    private class ChatAccountMessageVH extends A_ChatMessageVH {
        private ChatAccountMessageVH() {
            super(R.layout.io_customerly__li_bubble_account, R.drawable.io_customerly__ic_attach_account_40dp, -0.9f);
        }
    }

    private class ChatAccountRichMessageVH extends A_ChatMessageVH {
        private ChatAccountRichMessageVH() {
            super(R.layout.io_customerly__li_bubble_account_rich, R.drawable.io_customerly__ic_attach_account_40dp, -0.9f);
        }
        @Override
        protected void apply(@Nullable IE_Message pMessage, @Nullable String pDateToDisplay, boolean pIsFirstMessageOfSender, boolean pShouldAnimate) {
            super.apply(pMessage, pDateToDisplay, pIsFirstMessageOfSender, pShouldAnimate);
            View.OnClickListener clickListener = v -> {
                if(pMessage != null && pMessage.rich_mail_link != null) {
                    IU_Utils.intentUrl(IAct_Chat.this, pMessage.rich_mail_link);
                }
            };
            this.itemView.setOnClickListener(clickListener);
            this._Content.setOnClickListener(clickListener);
        }
        void onEmptyContent() { }
    }

    private abstract class A_ChatMessageVH extends A_ChatVH {
        @NonNull private final LinearLayout _AttachmentLayout;
        @Nullable private final View _Sending;
        @DrawableRes private final int _IcAttachResID;
        private final float _ItemFromXValueRelative;

        private final int MIN_ATTACHMENT_WIDTH = IU_Utils.px(150);

        private A_ChatMessageVH(@LayoutRes int pLayoutRes, @DrawableRes int pIcAttachResID, @FloatRange(from=-1, to=1) float pItemFromXValueRelative) {
            super(pLayoutRes);
            this._Sending = this.itemView.findViewById(R.id.io_customerly__content_sending__only_user_li);
            this._Content.setMovementMethod(LinkMovementMethod.getInstance());
            this._AttachmentLayout = (LinearLayout)this.itemView.findViewById(R.id.io_customerly__attachment_layout);
            this._IcAttachResID = pIcAttachResID;
            this._ItemFromXValueRelative = pItemFromXValueRelative;
        }
        @Override protected void apply(@Nullable IE_Message pMessage, @Nullable String pDateToDisplay, boolean pIsFirstMessageOfSender, boolean pShouldAnimate) {
            if (pMessage != null) {//Always != null for this ViewHolder
                if(pDateToDisplay != null) {
                    this._Date.setText(pDateToDisplay);
                    this._Date.setVisibility(View.VISIBLE);
                } else {
                    this._Date.setVisibility(View.GONE);
                }
                this._Time.setText(pMessage.timeString);

                if (pIsFirstMessageOfSender) {
                    Customerly._Instance._RemoteImageHandler.request(new IU_RemoteImageHandler.Request()
                            .fitCenter()
                            .transformCircle()
                            .load(pMessage.getImageUrl(this._IconSize))
                            .into(this._Icon)
                            .override(this._IconSize, this._IconSize)
                            .placeholder(R.drawable.io_customerly__ic_default_admin));
                    this._Icon.setVisibility(View.VISIBLE);
                } else {
                    this._Icon.setVisibility(View.INVISIBLE);
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
                                    startActivity(new Intent(IAct_Chat.this, IAct_FullScreenImage.class)
                                            .putExtra(IAct_FullScreenImage.EXTRA_IMAGE_SOURCE, is.getSource()));
                                }
                            }, pMessage.content.getSpanStart(is), pMessage.content.getSpanEnd(is), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }

                    this._Content.setText(pMessage.content);
                    this._Content.setVisibility(View.VISIBLE);
                } else {
                    this.onEmptyContent();
                }

                if (this._Sending != null) {
                    this._Sending.setVisibility(pMessage.isSending() ? View.VISIBLE : View.GONE);
                }

                if (pMessage.isFailed()) {
                    this._Content.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.io_customerly__ic_error, 0);
                    this._Content.setVisibility(View.VISIBLE);
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
                IE_Attachment[] attachments = pMessage._Attachments;
                if (attachments != null) {
                    for (IE_Attachment attachment : attachments) {

                        LinearLayout ll = new LinearLayout(IAct_Chat.this);
                        ll.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        ll.setOrientation(LinearLayout.VERTICAL);
                        ll.setGravity(Gravity.CENTER_HORIZONTAL);
                        ll.setMinimumWidth(MIN_ATTACHMENT_WIDTH);

                        ImageView iv = new ImageView(IAct_Chat.this);
                        if(attachment.isImage()) {
                            //Image attachment
                            iv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, IU_Utils.px(80)));
                            if (attachment.path != null && attachment.path.length() != 0) {
                                Customerly._Instance._RemoteImageHandler.request(new IU_RemoteImageHandler.Request()
                                        .centerCrop()
                                        .load(attachment.path)
                                        .into(iv)
                                        .placeholder(R.drawable.io_customerly__pic_placeholder));
                                ll.setOnClickListener(layout -> startActivity(new Intent(IAct_Chat.this, IAct_FullScreenImage.class)
                                        .putExtra(IAct_FullScreenImage.EXTRA_IMAGE_SOURCE, attachment.path)));
                            } else {
                                try {
                                    String base64 = attachment.loadBase64FromMemory(IAct_Chat.this);
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
                        } else { //No image attachment
                            ll.setBackgroundResource(pMessage.isUserMessage() ? R.drawable.io_customerly__attachmentfile_border_user : R.drawable.io_customerly__attachmentfile_border_account);
                            ll.setPadding(IU_Utils.px(10), 0, IU_Utils.px(10), IU_Utils.px(10));
                            iv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, IU_Utils.px(40)));
                            iv.setPadding(0, IU_Utils.px(15), 0, 0);
                            iv.setImageResource(this._IcAttachResID);

                            if (attachment.path != null && attachment.path.length() != 0) {
                                ll.setOnClickListener(layout -> new AlertDialog.Builder(IAct_Chat.this)
                                        .setTitle(R.string.io_customerly__download)
                                        .setMessage(R.string.io_customerly__download_the_file_)
                                        .setPositiveButton(android.R.string.ok, (dlg, which) -> IAct_Chat.this.startAttachmentDownload(attachment.name, attachment.path))
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .setCancelable(true)
                                        .show());
                            }
                        }
                        ll.addView(iv);
                        TextView tv = new TextView(IAct_Chat.this);
                        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        tv.setTextColor(IU_Utils.getColorStateListFromResource(getResources(), pMessage.isUserMessage() ? R.color.io_customerly__textcolor_white_grey : R.color.io_customerly__textcolor_malibu_grey));
                        tv.setLines(1);
                        tv.setSingleLine();
                        tv.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                        tv.setText(attachment.name);
                        tv.setPadding(0, IU_Utils.px(10), 0, 0);
                        tv.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                        ll.addView(tv);
                        this._AttachmentLayout.addView(ll);
                        ((LinearLayout.LayoutParams)ll.getLayoutParams()).topMargin = IU_Utils.px(10);
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
            } else {//Impossible but ...
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
        void onEmptyContent() {
            this._Content.setText(null);
            this._Content.setVisibility(View.GONE);
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<A_ChatVH> {
        final int _5dp = IU_Utils.px(5), _FirstMessageOfSenderTopPadding = this._5dp * 3;
        long mostRecentAnimatedMessageID = -1, mostOldAnimatedMessageID = Integer.MAX_VALUE;
        @Override
        public int getItemViewType(int position) {
            if(_TypingAccountId != TYPING_NO_ONE) {
                if(position == 0) {
                    return R.layout.io_customerly__li_bubble_account_typing;
                } else {
                    position--;
                }
            }
            IE_Message message = _ChatList.get(position);
            if(message.isUserMessage()) {
                return R.layout.io_customerly__li_bubble_user;
            } else if(message.rich_mail_link == null) {
                return R.layout.io_customerly__li_bubble_account;
            } else {
                return R.layout.io_customerly__li_bubble_account_rich;
            }
        }
        @Override
        public A_ChatVH onCreateViewHolder(ViewGroup parent, int viewType) {
            return viewType == R.layout.io_customerly__li_bubble_account_typing ? new ChatTypingVH()
                    : viewType == R.layout.io_customerly__li_bubble_user ? new ChatUserMessageVH()
                    : viewType == R.layout.io_customerly__li_bubble_account ? new ChatAccountMessageVH()
                    : /*viewType == R.layout.io_customerly__li_message_account_rich ? */ new ChatAccountRichMessageVH();
        }
        @Override
        public void onBindViewHolder(A_ChatVH holder, @SuppressLint("RecyclerView") int position) {
            position -= _TypingAccountId != TYPING_NO_ONE ? 1 : 0;//No typing -> same position. Yes typing -> position reduced by 1 (it becomes -1 if it is the typing item)

            IE_Message thisMessage = null, previousMessage;
            boolean shouldAnimate = false, firstMessageOfSender;
            if(position != -1) { //No typing item
                thisMessage = _ChatList.get(position);
                if(thisMessage.conversation_message_id > this.mostRecentAnimatedMessageID) {
                    shouldAnimate = true;
                    this.mostRecentAnimatedMessageID = thisMessage.conversation_message_id;
                }
                if(thisMessage.conversation_message_id < this.mostOldAnimatedMessageID) {
                    shouldAnimate = true;
                    this.mostOldAnimatedMessageID = thisMessage.conversation_message_id;
                }
            }
            previousMessage = position == _ChatList.size() - 1 ? null : _ChatList.get(position + 1);//Get previous message if the current is not the first of chat

            firstMessageOfSender = previousMessage == null
                    || ( thisMessage == null
                    ? ! previousMessage.isUserMessage()
                    : ! thisMessage.hasSameSenderOf(previousMessage) );

            holder.apply(thisMessage,
                    thisMessage == null || (previousMessage != null && thisMessage.sameSentDayOf(previousMessage)) ? null : thisMessage.dateString
                    , firstMessageOfSender, shouldAnimate);

            holder.itemView.setPadding(0, firstMessageOfSender ? this._FirstMessageOfSenderTopPadding : 0, 0, position <= 0 /* -1 is typing item */ ? this._5dp : 0);
            //paddingTop = 15dp to every first message of the group
            //paddingBottom = 5dp to the last message of the chat
        }
        @Override
        public int getItemCount() {
            return _TypingAccountId == TYPING_NO_ONE ? _ChatList.size() : _ChatList.size() + 1;
        }
        @Override
        public void onViewDetachedFromWindow(final A_ChatVH holder) {
            holder.clearAnimation();
        }
    }
}
