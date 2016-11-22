package io.customerly;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Base64;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Gianni on 13/09/16.
 * Project: CustomerlySDK
 */
class Internal_entity__Attachment {

    private static final String BASE_URL = "https://s3-eu-west-1.amazonaws.com/storage.customerly.io/";
    @Nullable final Uri uri;
    @NonNull final String name;
    @Nullable private String base64, path;

    Internal_entity__Attachment(@NonNull JSONObject attachment) throws JSONException {
        super();
//        this.attachment_size = attachment.getLong("size");
        this.path = attachment.getString("path");
        this.name = attachment.getString("name");
        this.base64 = null;
        this.uri = null;
    }

    Internal_entity__Attachment(@NonNull Context pContext, @NonNull Uri pUri) throws JSONException {
        super();
        this.uri = pUri;
        this.name = Internal_Utils__Utils.getNameFromUri(pContext, pUri);
        this.base64 = null;
        this.path = null;
    }

    void addAttachmentToInput(@NonNull Internal_activity__A_Customerly_Activity pA_Customerly_Activity) throws JSONException {
        pA_Customerly_Activity._Attachments.add(this);
        TextView tv = new TextView(pA_Customerly_Activity);
        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.io_customerly__ld_chat_attachment, 0, 0, 0);
        int _5dp = Internal_Utils__Utils.px(5);
        tv.setCompoundDrawablePadding(_5dp);
        tv.setPadding(_5dp, 0, 0, 0);
        tv.setTextColor(Internal_Utils__Utils.getColorStateListFromResource(pA_Customerly_Activity.getResources(), R.color.io_customerly__textcolor_blue2_grey));
        tv.setLines(1);
        tv.setSingleLine();
        tv.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        tv.setText(this.name);
        tv.setOnClickListener(textview -> new AlertDialog.Builder(pA_Customerly_Activity)
                .setTitle(R.string.io_customerly__allegato)
                .setMessage(pA_Customerly_Activity.getString(R.string.io_customerly__cancel_allegato, tv.getText()))
                .setNegativeButton(R.string.io_customerly__annulla, null)
                .setPositiveButton(R.string.io_customerly__cancel, (dlg, which) -> {
                    ViewGroup vg = (ViewGroup)tv.getParent();
                    if(vg != null) {
                        vg.removeView(tv);
                    }
                    pA_Customerly_Activity._Attachments.remove(this);
                })
                .setCancelable(true)
                .show());
        pA_Customerly_Activity.input_attachments.addView(tv);
    }

    static @NonNull JSONArray toSendJSONObject(@NonNull Context pContext, @Nullable Internal_entity__Attachment[] pAttachments) {
        JSONArray array = new JSONArray();
        if(pAttachments != null) {
            for (Internal_entity__Attachment attachment : pAttachments) {
                String base64 = attachment.loadBase64FromMemory(pContext);
                if(base64 != null) {
                    try {
                        JSONObject jo = new JSONObject();
                        jo.put("filename", attachment.name);
                        jo.put("base64", base64);
                        array.put(jo);
                    } catch (JSONException ignored) { }
                }
            }
        }
        return array;
    }

    private @Nullable String loadBase64FromMemory(@NonNull Context pContext) throws IllegalStateException {
        if(this.base64 == null && this.uri != null) {
            InputStream is = null;
            try {
                is = pContext.getContentResolver().openInputStream(this.uri);
                if (is != null) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024 * 4];
                    int n;
                    while (-1 != (n = is.read(buffer))) {
                        output.write(buffer, 0, n);
                    }
                    this.base64 = Base64.encodeToString(output.toByteArray(), 0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ignored) { }
                }
            }
        }
        return this.base64;
    }

    boolean hasPath() {
        return this.path != null && this.path.length() != 0;
    }

    @NonNull String getFullPath() {
        return BASE_URL + this.path;
    }

    public boolean isImage() {
        return this.name.endsWith(".jpg") || this.name.endsWith(".png") || this.name.endsWith(".jpeg") || this.name.endsWith(".gif") || this.name.endsWith(".bmp");
    }
}