package io.customerly;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.Dimension;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Gianni on 31/05/16.
 * Project: CustomerlySDK
 */
@SuppressWarnings("SpellCheckingInspection")
class Internal_Utils__Utils {

    @Contract("null -> false")
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static boolean checkConnection(@Nullable Context c) {
        if(c == null)
            return false;
        ConnectivityManager manager = (ConnectivityManager) c
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] nets = manager.getAllNetworks();
            for (Network net : nets) {
                info = manager.getNetworkInfo(net);
                if (info != null && info.getState() == NetworkInfo.State.CONNECTED)
                    return true;
            }
            return false;
        } else {
            info = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (info != null && info.getState() == NetworkInfo.State.CONNECTED)
                return true;
            info = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (info != null && info.getState() == NetworkInfo.State.CONNECTED)
                return true;
            info = manager.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);
            return info != null && info.getState() == NetworkInfo.State.CONNECTED;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static void intentUrl(@NonNull Activity activity, @SuppressWarnings("SameParameterValue") @NonNull String url) {
        if (!url.startsWith("https://") && !url.startsWith("http://"))
            url = "http://" + url;
        try {
            //noinspection deprecation
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? Intent.FLAG_ACTIVITY_NEW_DOCUMENT : Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET) | Intent.FLAG_ACTIVITY_MULTIPLE_TASK));
        } catch (SecurityException ignored) { }
    }

    @ColorInt static int alterColor(@ColorInt int color, @FloatRange(from = 0, to = 255)float factor) {
        return Color.argb(Color.alpha(color),
                (int) Math.min(255, Color.red(color) * factor),
                (int) Math.min(255, Color.green(color) * factor),
                (int) Math.min(255, Color.blue(color) * factor));
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.M)
    static int getColorFromResource(@NonNull Resources resources, @ColorRes int colorResID) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? resources.getColor(colorResID, null)
                : resources.getColor(colorResID);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.M)
    static ColorStateList getColorStateListFromResource(@NonNull Resources resources, @ColorRes int colorStateListResID) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? resources.getColorStateList(colorStateListResID, null)
                : resources.getColorStateList(colorStateListResID);
    }

    private final static String EMOJI_TAG_START = "<emoji>", EMOJI_TAG_END = "</emoji>";
    @Contract("null -> null")
    @Nullable static CustomerlyHtmlMessage decodeHtmlStringWithEmojiTag(@Nullable String message) {
        if(message == null)
            return null;
        StringBuilder sb = new StringBuilder();
        int start = 0, startEmojiTag, endEmojiTag;
        while(start < message.length()) {
            startEmojiTag = message.indexOf(EMOJI_TAG_START, start);
            if(startEmojiTag != -1) {
                endEmojiTag = message.indexOf(EMOJI_TAG_END, start);
                if(start < startEmojiTag)
                    sb.append(message.substring(start, startEmojiTag));
                sb.append(Character.toChars(Integer.decode("0x" + message.substring(startEmojiTag + EMOJI_TAG_START.length(), endEmojiTag).trim())));
                start = endEmojiTag + EMOJI_TAG_END.length();
            } else {
                sb.append(message.substring(start));
                break;
            }
        }

//        Html.TagHandler tagHandler = new Html.TagHandler() {
//            boolean ol = false;
//            int olCount;
//            @Override
//            public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
//                 /*
//                <ol> -> 1. 2. 3.
//                <li>rf</li>
//                <li>f</li>
//                <li>f</li>
//                </ol>
//
//                <ul> -> • • •
//                <li>f</li>
//                <li>f</li>
//                <li>f</li>
//                </ul>
//            */
//                switch (tag) {
//                    case "ol":
//                        if(opening) {
//                            this.ol = true;
//                            this.olCount = 1;
//                        }
//                        break;
//                    case "ul":
//                        if(opening) {
//                            this.ol = false;
//                        }
//                        break;
//                    case "li":
//                        if(opening) {
//                            if(this.ol) {
//                                output.append(String.format(Locale.UK, this.olCount > 9 ? "\n%d. " : "\n  %d. ", this.olCount++));
//                            } else {
//                                output.append("   •  ");
//                            }
//                        } else {
//                            output.append("\n");
//                        }
//                        break;
//                    default:
//                }
//            }
//        };

        //Gestione tag <ol> <ul> <li> e relative chiusure
        int i = 0;
        boolean in_ol = false, in_ul = false;
        int ol_count = 1;
        final String TAG_OL_OPEN = "<ol>";
        final String TAG_OL_CLOSE = "</ol>";
        final String TAG_UL_OPEN = "<ul>";
        final String TAG_UL_CLOSE = "</ul>";
        final String TAG_LI_OPEN = "<li>";
        final String TAG_LI_CLOSE = "</li>";
        final String NEW_LINE = "<br>";
        final String REPLACE_OL_LI_COUNT_MORE_THAN_9 = NEW_LINE + "%d. ";
        final String REPLACE_OL_LI_COUNT_LESS_THAN_9 = NEW_LINE + "  %d. ";
        final String REPLACE_UL_LI = NEW_LINE + "   •  ";

        while(i < sb.length()) {
            if(in_ol) {
                int end_ol = sb.indexOf(TAG_OL_CLOSE, i);
                int li = sb.indexOf(TAG_LI_OPEN, i);
                if(li != -1 && (li < end_ol || end_ol == -1)) {
                    int li_end = sb.indexOf(TAG_LI_CLOSE, i);
                    sb.replace(li_end, li_end + TAG_LI_CLOSE.length(), NEW_LINE);
                    String replace_string = String.format(Locale.UK, ol_count > 9 ? REPLACE_OL_LI_COUNT_MORE_THAN_9 : REPLACE_OL_LI_COUNT_LESS_THAN_9, ol_count++);
                    sb.replace(li, li + TAG_LI_OPEN.length(), replace_string);
                    i = li_end + NEW_LINE.length() - TAG_LI_OPEN.length() + replace_string.length();
                } else if(end_ol != -1){
                    sb.delete(end_ol, end_ol + TAG_OL_CLOSE.length());
                    i = end_ol;
                    in_ol = false;
                } else {
                    break;
                }
            } else if(in_ul) {
                int end_ul = sb.indexOf(TAG_UL_CLOSE, i);
                int li = sb.indexOf(TAG_LI_OPEN, i);
                if(li != -1 && (li < end_ul || end_ul == -1)) {
                    int li_end = sb.indexOf(TAG_LI_CLOSE, i);
                    sb.replace(li_end, li_end + TAG_LI_CLOSE.length(), NEW_LINE);
                    sb.replace(li, li + TAG_LI_OPEN.length(), REPLACE_UL_LI);
                    i = li_end + NEW_LINE.length() - TAG_LI_OPEN.length() + REPLACE_UL_LI.length();
                } else if(end_ul != -1){
                    sb.delete(end_ul, end_ul + TAG_UL_CLOSE.length());
                    i = end_ul;
                    in_ul = false;
                } else {
                    break;
                }
            } else {
                int start_ol = sb.indexOf(TAG_OL_OPEN, i);
                int start_ul = sb.indexOf(TAG_UL_OPEN, i);
                if(start_ol != -1 && (start_ol < start_ul || start_ul == -1)) {
                    sb.delete(start_ol, start_ol + TAG_OL_OPEN.length());
                    in_ol = true;
                    ol_count = 1;
                    //i = i;//ho cancellato i 4 caratteri
                } else if(start_ul != -1){
                    sb.delete(start_ul, start_ul + TAG_UL_OPEN.length());
                    in_ul = true;
                    //i = i;//ho cancellato i 4 caratteri
                } else {
                    break;
                }
            }

        }

        Spanned spannedMessage;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            spannedMessage = Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY, _SpanImage_ImageGetter, null);
        } else {
            //noinspection deprecation
            spannedMessage = Html.fromHtml(sb.toString(), _SpanImage_ImageGetter, null);
        }

        if(spannedMessage instanceof SpannableStringBuilder) {
            SpannableStringBuilder ssb = (SpannableStringBuilder)spannedMessage;
            int startSSB = 0;
            while(startSSB < ssb.length()) {                            //Scorre carattere per carattere
                if(ssb.charAt(startSSB) == '\n') {                      //Se trova un \n
                    startSSB++;                                         //Si posiziona al carattere successivo
                    int endCut = 0;                                     //Contatore di '\n' successivi al primo
                    while(startSSB + endCut < ssb.length() && ssb.charAt(startSSB + endCut) == '\n') {               //Finchè trova ulteriori '\n' successivi al primo
                        endCut++;                                       //Li conta
                    }
                    if(endCut != 0) {                                   //Se ne ha trovato almeno 1
                        ssb.delete(startSSB, startSSB + endCut);        //Li elimina tutti lasciando quindi solo un '\n'
                    }
                }
                startSSB++;                                             //Se il carattere non era '\n' o comunque anche se lo era, il successivo comunque non lo è altrimenti lo avrebbe eliminato con la delete, quindi vado a quello ancora successivo
            }
            if(ssb.length() != 0 && ssb.charAt(ssb.length() - 1) == '\n') { //Se l'ultimo carattere è un '\n'
                ssb.delete(ssb.length() - 1, ssb.length());             //Lo cancella
            }
            /* Vecchio ciclo che semplicemente eliminava tutti gli '\n' alla fine del messaggio
            int backN_count = 0;
            while(ssb.length() - backN_count - 1 >= 0 && ssb.charAt(ssb.length() - backN_count - 1) == '\n') {
                backN_count++;
            }
            ssb = ssb.delete(ssb.length() - backN_count, ssb.length());
            spannedMessage = ssb; */
            return new CustomerlyHtmlMessage(ssb);
        }
        return null;
    }

    private static final Html.ImageGetter _SpanImage_ImageGetter = source -> {
        Drawable d = Customerly._Instance._RemoteImageHandler.getHtmlImageSync(source);
        if(d != null) {
            d.setBounds(0, 0, px(150), px((int) (150f / d.getIntrinsicWidth() * d.getIntrinsicHeight())));
        }
        return d;

//      private static final Html.ImageGetter _SpanImage_ImageGetter = source -> {
//        Drawable d = null;
//        try {
//            InputStream src = new URL(source).openStream();
//            d = Drawable.createFromStream(src, "src");
//            if(d != null) {
//                d.setBounds(0, 0, px(150), px((int) (150f / d.getIntrinsicWidth() * d.getIntrinsicHeight())));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return d;
    };

    static String getNameFromUri(@NonNull Context context, @NonNull Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if(cursor != null) {
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    if (cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    
    static long getFileSizeFromUri(@NonNull Context context, @NonNull Uri uri) {
        try {
            String filePath = getPath(context, uri);
            return filePath == null ? 0 : new File(filePath).length();
        } catch (Exception no_dovrebbe_succedere_mai) {
            return 0;
        }
    }

    @Nullable
    private static String getPath(@NonNull Context context, @NonNull Uri uri) {
        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TO DO handle non-primary volumes
            }
            // DownloadsProvider
            else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if ("com.google.android.apps.photos.content".equals(uri.getAuthority()))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    @Nullable private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    @Contract(pure = true)
    @Px static int px(@Dimension(unit = Dimension.DP) int dp) {
        float dpi = Resources.getSystem().getDisplayMetrics().density;
        dpi = dpi > 100/*120, 160, 213, 240, 320, 480 or 640 dpi*/ ? dpi / 160f : dpi;
        dpi = dpi == 0 ? 1f : dpi;
        return (int) (dp * dpi);
    }

    @ColorInt
    static int getContrastColor(@ColorInt int color) {
        return color == 0 || ((0.299 * Color.red(color)) + ((0.587 * Color.green(color)) + (0.114 * Color.blue(color)))) > 186
                ? Color.BLACK : Color.WHITE;
    }

    interface JSONObjectTo<OBJ> {    @NonNull OBJ from(@NonNull JSONObject obj) throws JSONException;  }
    @Contract("null, _, _ -> null")
    @Nullable static <OBJ> ArrayList<OBJ> fromJSONdataToList(@Nullable JSONObject data, @NonNull String pArrayKey, @NonNull JSONObjectTo<OBJ> pJSONObjectToOBJ) {
        if (data != null) {
            try {
                JSONArray array = data.getJSONArray(pArrayKey);
                ArrayList<OBJ> list = new ArrayList<>(array.length());
                JSONObject obj;
                for(int i = 0; i < array.length(); i++) {
                    try {
                        obj = array.getJSONObject(i);
                        if(obj != null) {
                            list.add(pJSONObjectToOBJ.from(obj));
                        }
                    } catch (JSONException ignored) { }
                }
                return list;
            } catch (JSONException ignored) { }
        }
        return null;
    }

    @Contract(pure = true)
    @Nullable static String jsonOptStringWithNullCheck(@NonNull JSONObject obj, @NonNull String key) {
        return Internal_Utils__Utils.jsonOptStringWithNullCheck(obj, key, null);
    }

    @Contract(value = "_, _, !null -> !null", pure = true)
    @Nullable static String jsonOptStringWithNullCheck(@NonNull JSONObject obj, @NonNull String key, @Nullable String fallback) {
        return obj.isNull(key) ? fallback : obj.optString(key, fallback);
    }



//    /**
//     * Given a newValues json and a oldValues json, this method will return a diff doing the following:<br>
//     * - Removing and all fields matching in newValues and oldValues json<br>
//     * - Adding all fields existing in newValues but not existing in oldValues<br>
//     * - Updating all fields in oldValues existing in newValues but with different value<br>
//     * <br>
//     * If oldValues is null, newValues is returned
//     * If newValues matches oldValues, null is returned
//     * @param newValues The source json, this value is the most recent
//     * @param oldValues The json that contains the already sent infos
//     */
//    @Contract(pure = true, value="_, null -> !null")
//    @Nullable
//    static JSONObject getJSONdiff(@NonNull JSONObject newValues, @Nullable JSONObject oldValues) {
//        if(oldValues == null) {
//            return newValues;
//        }
//        JSONObject result = new JSONObject();
//        newValues.keys().forEachRemaining(new_key -> {
//            Object newValue = newValues.opt(new_key);
//            if(newValue != null) {
//                Object oldValue = oldValues.opt(new_key);
//                if (oldValue != null) {
//                    if (newValue instanceof JSONObject && oldValue instanceof JSONObject) {
//                        //Se la chiave esiste sia nel nuovo che nel vecchio e sono entrambi json li confronto ricorsivamente
//                        try {
//                            result.put(new_key, Internal_Utils__Utils.getJSONdiff((JSONObject) newValue, (JSONObject) oldValue));
//                        } catch (JSONException ignored) { }
//                    } else if (!newValue.equals(oldValue)) {
//                        try {
//                            result.put(new_key, newValue);
//                        } catch (JSONException ignored) { }
//                    }
//                } else {
//                    try {
//                        result.put(new_key, newValue);
//                    } catch (JSONException ignored) { }
//                }
//            }
//        });
//        return result.length() == 0 ? null : result;
//    }
}
