package io.customerly;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Gianni on 02/12/16.
 * Project: CustomerlySDK
 */
class Internal_Utils__RemoteImageHandler extends HandlerThread {
    private static final long MAX_DISK_CACHE_SIZE = 1024 * 1024 * 2;
    @NonNull private final LruCache<String,Bitmap> _LruCache = new LruCache<>(1024 * 1024 * 2);
    @NonNull private final HashMap<ImageView,Request> _PendingRequests = new HashMap<>();
    @Nullable private Handler _WorkerHandler;
    private long _DiskCacheSize = -1;

    Internal_Utils__RemoteImageHandler() {
        super(Internal_Utils__RemoteImageHandler.class.getName());
        this.start();
    }

    @Override
    protected void onLooperPrepared() {
        this._WorkerHandler = new Handler(this.getLooper());
    }

    void request(final @NonNull Request pRequest) {//TODO Creare 3 handler diversi per lru, disk e http?
        if (pRequest.url == null || pRequest.target == null) throw new AssertionError("Image Request not well formed");
        String key = pRequest.toString();
        if(key != null) {
            try {
                //Get Bitmap from LruMemory
                Bitmap bmp = this._LruCache.get(key);
                if (bmp != null && !bmp.isRecycled()) {
                    if (pRequest.scaleType != null) {
                        pRequest.target.setScaleType(pRequest.scaleType);
                    }
                    pRequest.target.setImageBitmap(bmp);
                    return;
                } else {
                    //Set placehoder
                    if (pRequest.placeholderResID != 0) {
                        if (pRequest.scaleType != null) {
                            pRequest.target.setScaleType(pRequest.scaleType);
                        }
                        pRequest.target.setImageResource(pRequest.placeholderResID);
                    } else if (pRequest.placeholderBMP != null && !pRequest.placeholderBMP.isRecycled()) {
                        if (pRequest.scaleType != null) {
                            pRequest.target.setScaleType(pRequest.scaleType);
                        }
                        pRequest.target.setImageBitmap(pRequest.placeholderBMP);
                    } else if (pRequest.placeholderDrawable != null) {
                        if (pRequest.scaleType != null) {
                            pRequest.target.setScaleType(pRequest.scaleType);
                        }
                        pRequest.target.setImageDrawable(pRequest.placeholderDrawable);
                    }
                    //Get Bitmap from Disk
                    File bitmapFile = new File(new File(pRequest.target.getContext().getCacheDir(), BuildConfig.CUSTOMERLY_SDK_NAME).toString(), key);
                    if (bitmapFile.exists()) {
                        if (System.currentTimeMillis() - bitmapFile.lastModified() < 24 * 60 * 60 * 1000) {
                            try {
                                bmp = BitmapFactory.decodeFile(bitmapFile.toString());
                                //Add Bitmap to LruMemory
                                this._LruCache.put(key, bmp);
                                if (pRequest.scaleType != null) {
                                    pRequest.target.setScaleType(pRequest.scaleType);
                                }
                                pRequest.target.setImageBitmap(bmp);
                                return;
                            } catch (OutOfMemoryError ignored) {
                            }
                        } else {
                            //noinspection ResultOfMethodCallIgnored
                            bitmapFile.delete();
                        }
                    }
                }
            } catch (OutOfMemoryError ignored) { }
        }/* else {
            //No sense, deve fallire la java.net.URLEncoder.encode e non supportare la UTF-8
        }*/

        if(this._WorkerHandler != null) {
            synchronized (this) {
                this._PendingRequests.put(pRequest.target, pRequest);
            }
            this._WorkerHandler.post(() -> {
                final Request req;
                synchronized (Internal_Utils__RemoteImageHandler.this) {
                    req = _PendingRequests.remove(pRequest.target);
                /*
                In questo modo anche se nel frattempo c'è stata una successiva richiesta per la stessa ImageView (es: ImageView riciclata da un'adapter di una RecyclerView), viene elaborata la richiesta più recente.
                Atomicamente viene anche rimossa la richiesta, quindi il callback pendente nell'handler quando verrà eseguito a questo punto del codice troverà null e al successivo check req != null interromperà l'esecuzione
                 */
                }

                if(req != null) {
                    try {
                        Bitmap bmp;
                        //Download bitmap da url
                        try {
                            HttpURLConnection connection = (HttpURLConnection) new URL(req.url).openConnection();
                            connection.setReadTimeout(5000);
                            connection.setDoInput(true);
                            connection.connect();

                            int status = connection.getResponseCode();
                            while (status != HttpURLConnection.HTTP_OK &&
                                    (status == HttpURLConnection.HTTP_MOVED_TEMP
                                            || status == HttpURLConnection.HTTP_MOVED_PERM
                                            || status == HttpURLConnection.HTTP_SEE_OTHER)) {
                                //Url Redirect
                                connection = (HttpURLConnection) new URL(connection.getHeaderField("Location")).openConnection();
                                connection.connect();
                                status = connection.getResponseCode();
                            }

                            bmp = BitmapFactory.decodeStream(connection.getInputStream());
                        } catch (Exception e) {
                            bmp = null;
                        }

                        //Bitmap resizing
                        if (bmp != null && req.width != Request.DONT_OVERRIDE_SIZE && req.height != Request.DONT_OVERRIDE_SIZE) {
                            int width = bmp.getWidth();
                            int height = bmp.getHeight();
                            float scaleWidth = ((float) req.width) / width;
                            float scaleHeight = ((float) req.height) / height;
                            // CREATE A MATRIX FOR THE MANIPULATION
                            Matrix matrix = new Matrix();
                            // RESIZE THE BIT MAP
                            matrix.postScale(scaleWidth, scaleHeight);

                            // "RECREATE" THE NEW BITMAP
                            Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, false);

                            if (resizedBitmap != bmp) {
                                bmp.recycle();
                            }
                            bmp = resizedBitmap;
                        }

                        if (bmp != null && req.transformCircle) {
                            int size = Math.min(bmp.getWidth(), bmp.getHeight());
                            Bitmap squared = Bitmap.createBitmap(bmp, (bmp.getWidth() - size) / 2, (bmp.getHeight() - size) / 2, size, size);
                            Bitmap result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                            Paint paint = new Paint();
                            paint.setShader(new BitmapShader(squared, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
                            paint.setAntiAlias(true);
                            float r = size / 2f;
                            new Canvas(result).drawCircle(r, r, r, paint);
                            if (bmp != result) {
                                bmp.recycle();
                            }
                            squared.recycle();
                            bmp = result;
                        }

                        if (bmp != null) {
                            synchronized (_PendingRequests) {
                                if (_PendingRequests.get(req.target) != null) {
                                    // Nel frattempo che scaricava l'immagine da internet ed eventualmente applicava resize e transformCircle è staa fatta una nuova richiesta per la stessa ImageView quindi termina l'esecuzione attuale invalidando la bmp
                                    bmp.recycle();
                                    return;
                                }
                            }
                            final Bitmap post_bmp = bmp;
                            req.target.post(() -> {
                                if (req.scaleType != null) {
                                    req.target.setScaleType(req.scaleType);
                                }
                                req.target.setImageBitmap(post_bmp);
                            });

                            //Add image to LruMemory
                            _LruCache.put(req.toString(), bmp);

                            File fullCacheDir = new File(req.target.getContext().getCacheDir(), BuildConfig.CUSTOMERLY_SDK_NAME);
                            //Initialize cache dir if needed
                            if (!fullCacheDir.exists()) {
                                //noinspection ResultOfMethodCallIgnored
                                fullCacheDir.mkdirs();
                                try {
                                    //noinspection ResultOfMethodCallIgnored
                                    new File(fullCacheDir.toString(), ".nomedia").createNewFile();
                                } catch (IOException ignored) { }
                            }

                            //Store on disk cache
                            String fileName = req.toString();
                            if (fileName != null) {
                                FileOutputStream out = null;
                                try {
                                    File bitmapFile = new File(fullCacheDir.toString(), fileName);
                                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out = new FileOutputStream(bitmapFile));
                                    if(_DiskCacheSize == -1) {
                                        long size = 0;
                                        for (File file : fullCacheDir.listFiles()) {
                                            if (file.isFile()) {
                                                size += file.length();
                                            } /*else {
                                                size += getFolderSize(file); Servirebbe la recursione per sottocartelle ma tanto non ce ne sono
                                            }*/
                                        }
                                        _DiskCacheSize = size;
                                    } else {
                                        _DiskCacheSize += bitmapFile.length();
                                    }
                                    if(_DiskCacheSize > MAX_DISK_CACHE_SIZE) {
                                        long oldestFileLastModifiedDateTime = Long.MAX_VALUE;
                                        File oldestFile = null;
                                        for (File file : fullCacheDir.listFiles()) {
                                            if (file.isFile()) {
                                                long lastModified = file.lastModified();
                                                if (lastModified < oldestFileLastModifiedDateTime) {
                                                    oldestFileLastModifiedDateTime = lastModified;
                                                    oldestFile = file;
                                                }
                                            }
                                        }
                                        if(oldestFile != null) {
                                            long size = oldestFile.length();
                                            if(oldestFile.delete()) {
                                                _DiskCacheSize -= size;
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {
                                } finally {
                                    if (out != null) {
                                        try {
                                            out.close();
                                        } catch (IOException ignored) { }
                                    }
                                }
                            }
                        } else {
                            //Set errorImage
                            if (req.errorResID != 0) {
                                if (req.scaleType != null) {
                                    req.target.setScaleType(req.scaleType);
                                }
                                req.target.setImageResource(req.errorResID);
                            } else if (req.errorBMP != null && !req.errorBMP.isRecycled()) {
                                if (req.scaleType != null) {
                                    req.target.setScaleType(req.scaleType);
                                }
                                req.target.setImageBitmap(req.errorBMP);
                            } else if (req.errorDrawable != null) {
                                if (req.scaleType != null) {
                                    req.target.setScaleType(req.scaleType);
                                }
                                req.target.setImageDrawable(req.errorDrawable);
                            }
                        }
                    } catch (OutOfMemoryError ignored) { }
                }
            });
        }
    }

    @SuppressWarnings("unused")
    static class Request {
        private static final int DONT_OVERRIDE_SIZE = -1;
        private String url;
        private ImageView target;

        private boolean transformCircle = false;
        @DrawableRes private int placeholderResID = 0;
        @Nullable private Bitmap placeholderBMP = null;
        @Nullable private Drawable placeholderDrawable = null;
        @DrawableRes private int errorResID = 0;
        @Nullable private Bitmap errorBMP = null;
        @Nullable private Drawable errorDrawable = null;
        @IntRange(from=DONT_OVERRIDE_SIZE, to=Integer.MAX_VALUE) private int width = DONT_OVERRIDE_SIZE, height = DONT_OVERRIDE_SIZE;
        @Nullable private ImageView.ScaleType scaleType;

        Request load(@NonNull String url) {
            this.url = url;
            return this;
        }
        Request override(@IntRange(from=DONT_OVERRIDE_SIZE, to=Integer.MAX_VALUE)int width, @IntRange(from=DONT_OVERRIDE_SIZE, to=Integer.MAX_VALUE)int height) {
            this.width = width;
            this.height = height;
            return this;
        }
        Request fitCenter() {
            this.scaleType = ImageView.ScaleType.FIT_CENTER;
            return this;
        }
        Request centerCrop() {
            this.scaleType = ImageView.ScaleType.CENTER_CROP;
            return this;
        }
        Request transformCircle() {
            this.transformCircle = true;
            return this;
        }
        Request placeholder(@DrawableRes int placeholderResID) {
            this.placeholderResID = placeholderResID;
            return this;
        }
        Request placeholder(@NonNull Bitmap placeholderBMP) {
            this.placeholderBMP = placeholderBMP;
            return this;
        }
        Request placeholder(@NonNull Drawable placeholderDrawable) {
            this.placeholderDrawable = placeholderDrawable;
            return this;
        }
        Request error(@DrawableRes int errorResID) {
            this.errorResID = errorResID;
            return this;
        }
        Request error(@NonNull Bitmap errorBMP) {
            this.errorBMP = errorBMP;
            return this;
        }
        Request error(@NonNull Drawable errorDrawable) {
            this.errorDrawable = errorDrawable;
            return this;
        }
        Request into(@NonNull ImageView imageView) {
            this.target = imageView;
            return this;
        }

        @Override @Nullable public String toString() {
            try {
                return String.format("%1$s-%2$s", BuildConfig.CUSTOMERLY_SDK_NAME, java.net.URLEncoder.encode(String.format(Locale.UK, "%1$s|%2$b|%3$d|%4$d", this.url, this.transformCircle, this.width, this.height), "UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
                return null;
            }
        }
    }
}