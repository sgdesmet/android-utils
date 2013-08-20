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
import com.github.sgdesmet.android.utils.util.BitmapUtils;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
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

    private Context applicationContext;

    private LruCache<String, Bitmap> memoryCache = null; //lrucache is thread-safe

    WeakHashMap<ImageView, String>                 urlForView;
    WeakHashMap<ImageView, Integer>                missingResourceForView;
    HashMap<String, Set<WeakReference<ImageView>>> viewsToNotify;
    HashMap<String, Set<ImageCallback>>            callbacksToNotify;

    private static final String TAG = ImageLoaderImpl.class.getSimpleName();

    public ImageLoaderImpl(Context applicationContext, int numThreads, int cacheSize) {

        executorService = Executors.newFixedThreadPool( numThreads );

        this.applicationContext = applicationContext;
        applicationContext.registerComponentCallbacks( this );

        if (cacheSize >= 0) {
            memoryCache = new LruCache<String, Bitmap>( cacheSize ) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {

                    return bitmap.getRowBytes() * bitmap.getHeight();
                }
            };
        }

        urlForView = new WeakHashMap<ImageView, String>();
        missingResourceForView = new WeakHashMap<ImageView, Integer>();
        viewsToNotify = new HashMap<String, Set<WeakReference<ImageView>>>();
        callbacksToNotify = new HashMap<String, Set<ImageCallback>>();
    }

    /**
     * Load an image from the specified url (in the background) and set it on the view. Must be called from the
     * main UI thread.
     *
     * @param loadingResource resource id to show when image is loading
     * @param missingResource resource id to show if url does not contain an image
     */
    @Override
    public synchronized void loadImage(String url, ImageView view, int loadingResource, int missingResource) {

        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e( TAG, "Refusing to get image " + url + ": this method *MUST* be called from the main thread!" );
            throw new RuntimeException( "Refusing to get image " + url + ": this method *MUST* be called from the main thread!" );
        }
        if (view == null) {
            Log.w( TAG, "View was null" );
            return;
        }

        if (url == null || url.trim().equals( "" )) {
            Log.d( TAG, "URL was null" );
            urlForView.remove( view );
            if (missingResource > 0) {
                view.setImageResource( missingResource );
            } else if (missingResource == NO_RESOURCE) {
                view.setImageBitmap( null );
            } else if (loadingResource > 0) {
                view.setImageResource( loadingResource );
            } else if (loadingResource == NO_RESOURCE) {
                view.setImageBitmap( null );
            }
            return;
        }

        if (loadingResource > 0) {
            view.setImageResource( loadingResource );
        } else if (loadingResource == NO_RESOURCE) {
            view.setImageBitmap( null );
        } //don't do anything for KEEP_CURRENT

        //adjust waiting states
        WeakReference<ImageView> reference = new WeakReference<ImageView>(
                view ); //keep weak references, don't keep imageviews around forever!
        //set imageurl as desired image for the view, override previous
        urlForView.put( view, url );

        if (missingResource != loadingResource) //don't set missing resource if it's the same as loading resource, no necessary
            missingResourceForView.put( view, missingResource );
        //add view to list of views to notify when image has been downloaded
        if (viewsToNotify.get( url ) == null) {
            viewsToNotify.put( url, new HashSet<WeakReference<ImageView>>() );
            viewsToNotify.get( url ).add( reference );
            // nobody was downloading this yet, send intent
            getImage( url );
        } else {
            //download is already queued, just add ourselves to the list of views to be notified when
            //download finishes
            viewsToNotify.get( url ).add( reference );
        }
    }

    @Override
    public synchronized void loadImage(String url, ImageCallback callback) {

        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e( TAG, "Refusing to get image " + url + ": this method *MUST* be called from the main thread!" );
            throw new RuntimeException( "Refusing to get image " + url + ": this method *MUST* be called from the main thread!" );
        }
        if (url == null) {
            Log.w( TAG, "Must provide all arguments (non-null)" );
            callback.failure( url );
            return;
        }

        //adjust waiting states

        //add callback to list of callbacks to notify when image has been downloaded
        if (callbacksToNotify.get( url ) == null) {
            callbacksToNotify.put( url, new HashSet<ImageCallback>() );
            callbacksToNotify.get( url ).add( callback );
            // nobody was downloading this yet, send intent
            getImage( url );
        } else {
            //download is already queued, just add ourselves to the list of callbacks to be notified when
            //download finishes
            callbacksToNotify.get( url ).add( callback );
        }
    }

    protected void getImage(final String url) {

        executorService.execute( new ImageRunner( url ) );
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

    /**
     * Return true if url is still required
     */
    @Override
    public synchronized boolean pending(String url) {

        //check if there are views that want the url
        Set<WeakReference<ImageView>> pendingViews = viewsToNotify.get( url );
        if (pendingViews != null)
            for (WeakReference<ImageView> viewReference : pendingViews) {
                if (viewReference.get() != null) {
                    ImageView view = viewReference.get();
                    if (urlForView.containsKey( view ) && urlForView.get( view ).equals( url )) {
                        return true;
                    }
                }
            }

        //check if there are images that want the url
        Set<ImageCallback> pendingCallbacks = callbacksToNotify.get( url );
        if (pendingCallbacks != null && !pendingCallbacks.isEmpty())
            return true;

        //nobody wants the url anymore
        return false;
    }

    private synchronized void updateViews(final String url, final Bitmap bitmap) {

        Set<WeakReference<ImageView>> pendingViews = viewsToNotify.get( url );
        viewsToNotify.remove( url );
        if (pendingViews != null)
            for (WeakReference<ImageView> viewReference : pendingViews) {
                if (viewReference != null && viewReference.get() != null) { //is the view still there?
                    //check if the view is really still waiting for this particular image
                    //this could not be the case for ImageViews in ListViews or GridViews, as they are recycled
                    ImageView view = viewReference.get();
                    if (view != null && urlForView.containsKey( view ) && urlForView.get( view ).equals( url )) {
                        //okay, set image
                        if (bitmap != null) {
                            view.setImageBitmap( bitmap );
                        } else if (missingResourceForView.containsKey( view )) {
                            int resource = missingResourceForView.get( view ) != null? missingResourceForView.get( view ): KEEP_CURRENT;
                            if (resource > 0) {
                                view.setImageResource( missingResourceForView.get( view ) );
                            } else if (resource == NO_RESOURCE) {
                                view.setImageBitmap( null );
                            }
                        }
                        urlForView.remove( view );
                    }
                }
            }
    }

    private synchronized void notifyCallbacks(final String url, final Bitmap bitmap) {

        Set<ImageCallback> pendingCallbacks = callbacksToNotify.get( url );
        callbacksToNotify.remove( url );
        if (pendingCallbacks != null)
            for (ImageCallback callback : pendingCallbacks)
                if (bitmap != null)
                    callback.onImageLoaded( url, bitmap );
                else
                    callback.failure( url );
    }

    private synchronized void error(final String url) {

        Set<ImageCallback> pendingCallbacks = callbacksToNotify.get( url );
        callbacksToNotify.remove( url );
        if (pendingCallbacks != null)
            for (ImageCallback callback : pendingCallbacks)
                callback.failure( url );
    }

    private static final Handler uiHandler = new Handler( Looper.getMainLooper() );


    private class ImageRunner implements Runnable {

        String imageUrl;

        private ImageRunner(final String imageUrl) {

            this.imageUrl = imageUrl;
        }

        @Override
        public void run() {

            try {
                final Bitmap bitmap = getBitmap( imageUrl );
                //do updates on main thread
                uiHandler.post( new Runnable() {
                    @Override
                    public void run() {

                        if (bitmap != null) {
                            updateViews( imageUrl, bitmap );
                            notifyCallbacks( imageUrl, bitmap );
                        } else {
                            error( imageUrl );
                        }
                    }
                } );
            }
            catch (IOException e) {
                //error on main thread
                uiHandler.post( new Runnable() {
                    @Override
                    public void run() {

                        error( imageUrl );
                    }
                } );
            }
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
                connection.setConnectTimeout(
                        HttpResource.DEFAULT_TIMEOUT ); //TODO may need to add a timer to forcibly terminate if necessary?
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
    }
}
