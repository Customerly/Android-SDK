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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
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
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.text.Spanned;
import android.widget.TextView;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import io.customerly.commons.Commons_HtmlFormatter;
import io.customerly.commons.LambdaUtil;

/**
 * Created by Gianni on 31/05/16.
 * Project: Customerly Android SDK
 */
@SuppressWarnings("SpellCheckingInspection")
class IU_Utils {

    @Contract("null -> false")
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static boolean checkConnection(@Nullable Context c) {
        if(c == null)
            return false;
        ConnectivityManager manager = (ConnectivityManager) c
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if(manager != null) {
            NetworkInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
        } else {
            return false;
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

    @ColorInt static int alterColor(@ColorInt int color, @SuppressWarnings("SameParameterValue") @FloatRange(from = 0, to = 255)float factor) {
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

    @NonNull static Spanned fromHtml(@Nullable String message, @Nullable TextView tv, @Nullable LambdaUtil.V__NN_NN<Activity, String> pImageClickableSpan) {
        return Commons_HtmlFormatter.fromHtml(message, tv, pImageClickableSpan, (context, source, handleDrawable) ->
                Customerly.get()._RemoteImageHandler.request(new IU_RemoteImageHandler.Request()
                .load(source)
                .into(context, new IU_RemoteImageHandler.Request.Target() {
                    @Override
                    public void image_placeholder_error(@NonNull Bitmap bmp) {
                        if(tv != null) {
                            tv.post(() -> handleDrawable.lambda(new BitmapDrawable(context.getResources(), bmp)));
                        }
                    }
                    @Override public void placeholder_error(@NonNull Drawable drawable) { }//Never called because no placeholder or error drawable provided in request
                    @Override public void placeholder_error(@DrawableRes int resID) { }//Never called because no placeholder or error DrawableRes provided in request
                })));
    }

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
        return IU_Utils.jsonOptStringWithNullCheck(obj, key, null);
    }

    @Contract(value = "_, _, !null -> !null", pure = true)
    @Nullable static String jsonOptStringWithNullCheck(@NonNull JSONObject obj, @NonNull String key, @Nullable String fallback) {
        return obj.isNull(key) ? fallback : obj.optString(key, fallback);
    }

    @Contract(value = "null,!null -> null; !null,!null -> _", pure = true)
    @Nullable static String getStringSafe(@Nullable SharedPreferences pref, @NonNull String key) {
        return IU_Utils.getStringSafe(pref, key, null);
    }

    @SuppressWarnings("WeakerAccess")
    @Contract(value = "null,!null,_ -> null; !null,!null,null -> _; !null,!null,!null -> !null", pure = true)
    @Nullable static String getStringSafe(@Nullable SharedPreferences pref, @NonNull String key, @SuppressWarnings("SameParameterValue") @Nullable String defValue) {
        try {
            return pref == null ? null : pref.getString(key, defValue);
        } catch (Exception not_string) {
            return defValue;
        }
    }

    @Contract(value = "_,_,true -> !null; null,_,false -> null; !null,_,false -> _", pure = true)
    @Nullable static JSONObject getStringJSONSafe(@Nullable SharedPreferences pref, @NonNull String key, @SuppressWarnings("SameParameterValue") boolean nonNull) {
        try {
            String stringa = pref == null ? null : pref.getString(key, null);
            return stringa == null
                    ? nonNull ? new JSONObject() : null
                    : new JSONObject(stringa);
        } catch (Exception not_string) {
            return nonNull ? new JSONObject() : null;
        }
    }

    @Contract(pure = true)
    static int getIntSafe(@Nullable SharedPreferences pref, @NonNull String key, int default_value) {
        try {
            return pref == null ? default_value : pref.getInt(key, default_value);
        } catch (Exception not_string) {
            return default_value;
        }
    }

    @Contract(pure = true)
    static boolean getBooleanSafe(@Nullable SharedPreferences pref, @NonNull String key, @SuppressWarnings("SameParameterValue") boolean default_value) {
        try {
            return pref == null ? default_value : pref.getBoolean(key, default_value);
        } catch (Exception not_string) {
            return default_value;
        }
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
