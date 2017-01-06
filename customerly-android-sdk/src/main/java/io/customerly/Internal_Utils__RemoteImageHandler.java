package io.customerly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.util.LruCache;
import android.util.SparseArray;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import static io.customerly.BuildConfig.CUSTOMERLY_SDK_NAME;

/**
 * Created by Gianni on 02/12/16.
 * Project: CustomerlySDK
 */
class Internal_Utils__RemoteImageHandler {
    private static final int MAX_DISK_CACHE_SIZE = 1024 * 1024 * 2;
    private static final int MAX_LRU_CACHE_SIZE = 1024 * 1024 * 2;
    @NonNull private final LruCache<String,Bitmap> _LruCache = new LruCache<>(MAX_LRU_CACHE_SIZE);
    @NonNull private final SparseArray<Request> _PendingDiskRequests = new SparseArray<>(), _PendingNetworkRequests = new SparseArray<>();
    @Nullable private Handler _DiskHandler, _NetworkHandler;
    private long _DiskCacheSize = -1;

    Internal_Utils__RemoteImageHandler() {
        super();
        new HandlerThread(Internal_Utils__RemoteImageHandler.class.getName() + "-Network") {
            @Override
            protected void onLooperPrepared() {
                _NetworkHandler = new Handler(this.getLooper());
                new HandlerThread(Internal_Utils__RemoteImageHandler.class.getName() + "-Disk") {
                    @Override
                    protected void onLooperPrepared() {
                        _DiskHandler = new Handler(this.getLooper());
                    }
                }.start();
            }
        }.start();
    }

    @WorkerThread
    Drawable getHtmlImageSync(@NonNull String url) {
        final String cache_key = String.format(Locale.UK, "%1$s-HTML-%2$d", CUSTOMERLY_SDK_NAME, url.hashCode());
        try {
            //Get Bitmap from LruMemory
            final Bitmap bmp = this._LruCache.get(cache_key);
            if (bmp != null && !bmp.isRecycled()) {
                return new BitmapDrawable(Resources.getSystem(), bmp);
            }
        } catch (OutOfMemoryError ignored) { }

        final String appCacheDir = Customerly._Instance._AppCacheDir;
        if(appCacheDir != null) {
            try {
                File bitmapFile = new File(new File(appCacheDir, CUSTOMERLY_SDK_NAME).toString(), cache_key);
                if (bitmapFile.exists()) {
                    if (System.currentTimeMillis() - bitmapFile.lastModified() < 24 * 60 * 60 * 1000) {
                        try {
                            Bitmap bmp = BitmapFactory.decodeFile(bitmapFile.toString());
                            //Add Bitmap to LruMemory
                            this._LruCache.put(cache_key, bmp);
                            return new BitmapDrawable(Resources.getSystem(), bmp);
                        } catch (OutOfMemoryError ignored) {
                        }
                    } else {
                        //noinspection ResultOfMethodCallIgnored
                        bitmapFile.delete();
                    }
                }
            } catch (OutOfMemoryError ignored) {
            }
        }

        try {
            Bitmap bmp;
            //Download bitmap da url
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
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

            if (bmp != null) {
                //Add image to LruMemory
                this._LruCache.put(cache_key, bmp);

                if(appCacheDir != null) {
                    File fullCacheDir = new File(appCacheDir, BuildConfig.CUSTOMERLY_SDK_NAME);
                    //Initialize cache dir if needed
                    if (!fullCacheDir.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        fullCacheDir.mkdirs();
                        try {
                            //noinspection ResultOfMethodCallIgnored
                            new File(fullCacheDir.toString(), ".nomedia").createNewFile();
                        } catch (IOException ignored) {
                        }
                    }

                    //Store on disk cache
                    FileOutputStream out = null;
                    try {
                        File bitmapFile = new File(fullCacheDir.toString(), cache_key);
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out = new FileOutputStream(bitmapFile));
                        if (this._DiskCacheSize == -1) {
                            long size = 0;
                            for (File file : fullCacheDir.listFiles()) {
                                if (file.isFile()) {
                                    size += file.length();
                                } /*else {
                                        size += getFolderSize(file); No subfolder or recursion would be needed
                                    }*/
                            }
                            this._DiskCacheSize = size;
                        } else {
                            this._DiskCacheSize += bitmapFile.length();
                        }
                        if (this._DiskCacheSize > MAX_DISK_CACHE_SIZE) {
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
                            if (oldestFile != null) {
                                long size = oldestFile.length();
                                if (oldestFile.delete()) {
                                    this._DiskCacheSize -= size;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    } finally {
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }
                return new BitmapDrawable(Resources.getSystem(), bmp);
            }
        } catch (Exception ignored) { }
        return null;
    }

    @UiThread
    void request(final @NonNull Request pRequest) {
        if (pRequest.url == null || pRequest.target == null)
            throw new AssertionError("Image Request not well formed");

//        this.handleLruWithHandler(pRequest);
        synchronized (this) {
            this._PendingDiskRequests.remove(pRequest.target.hashCode());
            this._PendingNetworkRequests.remove(pRequest.target.hashCode());
        }
        try {
            //Get Bitmap from LruMemory
            final Bitmap bmp = this._LruCache.get(pRequest.toString());
            if (bmp != null && !bmp.isRecycled()) {
                if (pRequest.scaleType != null) {
                    pRequest.target.setScaleType(pRequest.scaleType);
                }
                pRequest.target.setImageBitmap(bmp);
                return;
            }
        } catch (OutOfMemoryError ignored) { }

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

        this.handleDisk(pRequest);
    }

    private void handleDisk(final @NonNull Request pLru_Request) {
        if(this._DiskHandler != null) {
            synchronized (this) {
                this._PendingDiskRequests.put(pLru_Request.target.hashCode(), pLru_Request);
                this._PendingNetworkRequests.remove(pLru_Request.target.hashCode());
            }
            this._DiskHandler.post(() -> {
                final Request disk_req;
                //noinspection SpellCheckingInspection
                synchronized (this) {
                    disk_req = _PendingDiskRequests.get(pLru_Request.target.hashCode());
                    if(disk_req != null) {
                        _PendingDiskRequests.remove(pLru_Request.target.hashCode());
                    }
                /*
                In questo modo anche se nel frattempo c'è stata una successiva richiesta per la stessa ImageView (es: ImageView riciclata da un'adapter di una RecyclerView), viene elaborata la richiesta più recente.
                Atomicamente viene anche rimossa la richiesta, quindi il callback pendente nell'handler quando verrà eseguito a questo punto del codice troverà null e al successivo check req != null interromperà l'esecuzione
                 */
                }

                if (disk_req != null) {
                    String disk_key = disk_req.toString();
                    try {
                        File bitmapFile = new File(new File(disk_req.target.getContext().getCacheDir(), CUSTOMERLY_SDK_NAME).toString(), disk_key);
                        if (bitmapFile.exists()) {
                            if (System.currentTimeMillis() - bitmapFile.lastModified() < 24 * 60 * 60 * 1000) {
                                try {
                                    Bitmap bmp = BitmapFactory.decodeFile(bitmapFile.toString());
                                    //Add Bitmap to LruMemory
                                    this._LruCache.put(disk_key, bmp);
                                    disk_req.target.post(() -> {
                                        if (disk_req.scaleType != null) {
                                            disk_req.target.setScaleType(disk_req.scaleType);
                                        }
                                        disk_req.target.setImageBitmap(bmp);
                                    });
                                    return;
                                } catch (OutOfMemoryError ignored) { }
                            } else {
                                //noinspection ResultOfMethodCallIgnored
                                bitmapFile.delete();
                            }
                        }
                    } catch (OutOfMemoryError ignored) { }
                    this.handleNetwork(disk_req);
                }
            });
        }
    }

    private void handleNetwork(final @NonNull Request pDisk_Request) {
        if(this._NetworkHandler != null) {
            synchronized (this) {
                this._PendingNetworkRequests.put(pDisk_Request.target.hashCode(), pDisk_Request);
            }
            this._NetworkHandler.post(() -> {
                final Request network_req;
                //noinspection SpellCheckingInspection
                synchronized (this) {
                    network_req = _PendingNetworkRequests.get(pDisk_Request.target.hashCode());
                    if(network_req != null) {
                        _PendingNetworkRequests.remove(pDisk_Request.target.hashCode());
                    }
                /*
                In questo modo anche se nel frattempo c'è stata una successiva richiesta per la stessa ImageView (es: ImageView riciclata da un'adapter di una RecyclerView), viene elaborata la richiesta più recente.
                Atomicamente viene anche rimossa la richiesta, quindi il callback pendente nell'handler quando verrà eseguito a questo punto del codice troverà null e al successivo check req != null interromperà l'esecuzione
                 */
                }

                if (network_req != null) {
                    try {
                        Bitmap bmp;
                        //Download bitmap da url
                        try {
                            HttpURLConnection connection = (HttpURLConnection) new URL(network_req.url).openConnection();
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
                        if (bmp != null && network_req.width != Request.DO_NOT_OVERRIDE_SIZE && network_req.height != Request.DO_NOT_OVERRIDE_SIZE) {
                            int width = bmp.getWidth();
                            int height = bmp.getHeight();
                            float scaleWidth = ((float) network_req.width) / width;
                            float scaleHeight = ((float) network_req.height) / height;
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

                        if (bmp != null && network_req.transformCircle) {
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
                            synchronized (this) {
                                if (_PendingDiskRequests.get(network_req.target.hashCode()) != null
                                    ||
                                        _PendingNetworkRequests.get(network_req.target.hashCode()) != null) //noinspection SpellCheckingInspection
                                {
                                    // Nel frattempo che scaricava l'immagine da internet ed eventualmente applicava resize e transformCircle è staa fatta una nuova richiesta per la stessa ImageView quindi termina l'esecuzione attuale invalidando la bmp
                                    bmp.recycle();
                                    return;
                                }
                            }
                            final Bitmap post_bmp = bmp;
                            network_req.target.post(() -> {
                                if (network_req.scaleType != null) {
                                    network_req.target.setScaleType(network_req.scaleType);
                                }
                                if(!post_bmp.isRecycled()) {
                                    network_req.target.setImageBitmap(post_bmp);
                                }
                            });

                            String network_key = network_req.toString();
                            //Add image to LruMemory
                            _LruCache.put(network_key, bmp);

                            File fullCacheDir = new File(network_req.target.getContext().getCacheDir(), CUSTOMERLY_SDK_NAME);
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
                            FileOutputStream out = null;
                            try {
                                File bitmapFile = new File(fullCacheDir.toString(), network_key);
                                bmp.compress(Bitmap.CompressFormat.PNG, 100, out = new FileOutputStream(bitmapFile));
                                if(_DiskCacheSize == -1) {
                                    long size = 0;
                                    for (File file : fullCacheDir.listFiles()) {
                                        if (file.isFile()) {
                                            size += file.length();
                                        } /*else {
                                        size += getFolderSize(file); No subfolder or recursion would be needed
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
                        } else {
                            //Set errorImage
                            if (network_req.errorResID != 0) {
                                network_req.target.post(() -> {
                                    if (network_req.scaleType != null) {
                                        network_req.target.setScaleType(network_req.scaleType);
                                    }
                                    network_req.target.setImageResource(network_req.errorResID);
                                });
                            } else if (network_req.errorBMP != null && !network_req.errorBMP.isRecycled()) {
                                network_req.target.post(() -> {
                                    if (network_req.scaleType != null) {
                                        network_req.target.setScaleType(network_req.scaleType);
                                    }
                                    if(!network_req.errorBMP.isRecycled()) {
                                        network_req.target.setImageBitmap(network_req.errorBMP);
                                    }
                                });
                            } else if (network_req.errorDrawable != null) {
                                network_req.target.post(() -> {
                                    if (network_req.scaleType != null) {
                                        network_req.target.setScaleType(network_req.scaleType);
                                    }
                                    network_req.target.setImageDrawable(network_req.errorDrawable);
                                });
                            }
                        }
                    } catch (OutOfMemoryError ignored) { }
                }
            });
        }
    }

    @SuppressWarnings("unused")
    static class Request {
        private static final int DO_NOT_OVERRIDE_SIZE = -1;
        private String url;
        private ImageView target;

        private boolean transformCircle = false;
        @DrawableRes private int placeholderResID = 0;
        @Nullable private Bitmap placeholderBMP = null;
        @Nullable private Drawable placeholderDrawable = null;
        @DrawableRes private int errorResID = 0;
        @Nullable private Bitmap errorBMP = null;
        @Nullable private Drawable errorDrawable = null;
        @IntRange(from= DO_NOT_OVERRIDE_SIZE, to=Integer.MAX_VALUE) private int width = DO_NOT_OVERRIDE_SIZE, height = DO_NOT_OVERRIDE_SIZE;
        @Nullable private ImageView.ScaleType scaleType;

        Request load(@NonNull String url) {
            this.url = url;
            return this;
        }
        Request override(@IntRange(from= DO_NOT_OVERRIDE_SIZE, to=Integer.MAX_VALUE)int width, @IntRange(from= DO_NOT_OVERRIDE_SIZE, to=Integer.MAX_VALUE)int height) {
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

        @Override @NonNull public String toString() {
            return String.format(Locale.UK, "%1$s-%2$d", CUSTOMERLY_SDK_NAME, String.format(Locale.UK, "%1$s|%2$b|%3$d|%4$d", this.url, this.transformCircle, this.width, this.height).hashCode());
        }
    }
}