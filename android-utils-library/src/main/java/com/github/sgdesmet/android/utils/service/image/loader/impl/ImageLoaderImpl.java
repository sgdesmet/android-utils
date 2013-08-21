package com.github.sgdesmet.android.utils.service.image.loader.impl;

import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.*;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;
import com.github.sgdesmet.android.utils.service.HttpResource;
import com.github.sgdesmet.android.utils.service.image.loader.*;
import com.github.sgdesmet.android.utils.util.AndroidUtils;
import com.github.sgdesmet.android.utils.util.BitmapUtils;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * TODO description
 * <p/>
 * Date: 19/08/13
 * Time: 11:58
 *
 * @author: sgdesmet1
 */
public class ImageLoaderImpl implements ImageLoader, ComponentCallbacks {

    private static ExecutorService executorService;

    private LruCache<String, Bitmap> memoryCache = null; //lrucache is thread-safe

    Map<ImageView, String> urlForView;

    private static final String TAG = ImageLoaderImpl.class.getSimpleName();

    public ImageLoaderImpl(Context applicationContext, int numThreads, int cacheSize) {

        if (executorService == null)
            executorService = Executors.newFixedThreadPool( numThreads );

        applicationContext.registerComponentCallbacks( this );

        if (cacheSize >= 0) {
            memoryCache = new LruCache<String, Bitmap>( cacheSize ) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {

                    return Math.max(( bitmap.getRowBytes() * bitmap.getHeight() ) / 1024, 1); //in kB, an item is at least 1kb
                }
            };
        }

        urlForView = Collections.synchronizedMap( new WeakHashMap<ImageView, String>() ); //weak references, don't leak views
    }

    /**
     * Load an image from the specified url (in the background) and set it on the view. Must be called from the
     * main UI thread.
     *
     * @param loadingResource resource id to show when image is loading
     * @param missingResource resource id to show if url does not contain an image
     */
    @Override
    public void loadImage(final String url, final ImageView view, final int loadingResource, final int missingResource) {

        if (view == null) {
            Log.w( TAG, "View was null" );
            return;
        }

        urlForView.put( view, url );
        executorService.execute( new ImageViewRunner( url, loadingResource, missingResource, view ) );
    }

    @Override
    public void loadImage(final String url, final ImageCallback callback) {

        if (url == null) {
            Log.w( TAG, "Must provide all arguments (non-null)" );
            callback.failure( url );
            return;
        }

        executorService.execute( new ImageCallbackRunner( url, callback ) );
    }

    @Override
    public void onConfigurationChanged(final Configuration configuration) {
        //noop
    }

    @Override
    public void onLowMemory() {

        if (memoryCache != null)
            memoryCache.evictAll();
    }

    protected Bitmap getBitmap(String url)
            throws IOException {

        Bitmap bitmap = memoryCache != null? memoryCache.get( url ): null;
        if (bitmap == null) {
            byte[] image = getImage( url );
            if (image != null && image.length != 0) {
                bitmap = BitmapUtils.decodeImageMemoryEfficient(
                        image ); //also do decoding into a bitmap here, so we don't have to do it on the main thread
                if (memoryCache != null)
                    memoryCache.put( url, bitmap );
            }
        }
        return bitmap;
    }

    protected byte[] getImage(String url)
            throws IOException {

        HttpURLConnection connection = null;
        InputStream is = null;
        try {
            connection = (HttpURLConnection) new URL( url ).openConnection();
            connection.setConnectTimeout( HttpResource.DEFAULT_TIMEOUT ); //TODO may need to add a timer to forcibly terminate if necessary?
            connection.setReadTimeout( HttpResource.DEFAULT_TIMEOUT );

            is = connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read( data, 0, data.length )) != -1) {
                buffer.write( data, 0, nRead );
            }
            buffer.flush();
            is.close();
            return buffer.toByteArray();
        }
        finally {
            if (is != null)
                is.close();
            if (connection != null)
                connection.disconnect();
        }
    }

    private static final Handler uiHandler = new Handler( Looper.getMainLooper() );


    private class ImageViewRunner implements Runnable {

        String                   imageUrl;
        int                      loadingResource;
        int                      missingResource;
        WeakReference<ImageView> view;

        private ImageViewRunner(final String imageUrl, final int loadingResource, final int missingResource, final ImageView view) {

            this.imageUrl = imageUrl;
            this.loadingResource = loadingResource;
            this.missingResource = missingResource;
            this.view = new WeakReference<ImageView>( view );
        }

        private ImageView getView() {

            return view != null? view.get(): null;
        }

        @Override
        public void run() {

            if (AndroidUtils.stringBlank( imageUrl )) {

                setResourceImage( missingResource );
                return;
            }

            setResourceImage( loadingResource );

            if (getView() != null && urlForView.get( getView() ).equals( imageUrl )) {
                try {
                    final Bitmap bitmap = getBitmap( imageUrl );
                    setImage( bitmap );
                }
                catch (IOException e) {
                    setResourceImage( missingResource );
                }
            }
        }

        private void setImage(final Bitmap bitmap) {

            uiHandler.post( new Runnable() {
                @Override
                public void run() {

                    if (bitmap != null) {
                        // if the ImageView still exists, and it still needs this image, set the image
                        ImageView imageView = getView();
                        if (imageView != null && urlForView.get( imageView ).equals( imageUrl )) {
                            imageView.setImageBitmap( bitmap );
                        }
                    } else {
                        setResourceImage( missingResource );
                    }
                }
            } );
        }

        private void setResourceImage(final int resource) {

            uiHandler.post( new Runnable() {
                @Override
                public void run() {

                    ImageView imageView = getView();
                    if (imageView != null) {
                        if (resource > 0) {
                            imageView.setImageResource( resource );
                        } else if (resource == NO_RESOURCE) {
                            imageView.setImageBitmap( null );
                        } // don't set anything if KEEP_CURRENT
                    }
                }
            } );
        }
    }


    private class ImageCallbackRunner implements Runnable {

        String        imageUrl;
        ImageCallback callback;

        private ImageCallbackRunner(final String imageUrl, final ImageCallback callback) {

            this.imageUrl = imageUrl;
            this.callback = callback;
        }

        private ImageCallbackRunner(final String imageUrl) {

            this.imageUrl = imageUrl;
        }

        @Override
        public void run() {

            if (AndroidUtils.stringBlank( imageUrl )) {

                callback.failure( imageUrl );
                return;
            }

            try {
                final Bitmap bitmap = getBitmap( imageUrl );

                //do updates on main thread
                uiHandler.post( new Runnable() {
                    @Override
                    public void run() {

                        if (bitmap != null)
                            callback.onImageLoaded( imageUrl, bitmap );
                        else
                            callback.failure( imageUrl );
                    }
                } );
            }
            catch (IOException e) {
                //error on main thread
                uiHandler.post( new Runnable() {
                    @Override
                    public void run() {

                        callback.failure( imageUrl );
                    }
                } );
            }
        }
    }
}
