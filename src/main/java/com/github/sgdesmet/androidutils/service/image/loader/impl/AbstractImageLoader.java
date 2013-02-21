package com.github.sgdesmet.androidutils.service.image.loader.impl;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.ImageView;
import com.github.sgdesmet.androidutils.service.image.loader.ImageCallback;
import com.github.sgdesmet.androidutils.service.image.loader.ImageLoader;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

/**
* TODO description
* <p/>
* Date: 21/02/13
* Time: 16:12
*
* @author: sgdesmet
*/
public abstract class AbstractImageLoader implements ImageLoader {

    WeakHashMap<ImageView, String> urlForView;
    WeakHashMap<ImageView, Integer> missingResourceForView;
    HashMap<String,Set<WeakReference<ImageView>>> viewsToNotify;
    HashMap<String, Set<ImageCallback>> callbacksToNotify;

    private static final String TAG = AbstractImageLoader.class.getSimpleName();

    public AbstractImageLoader(){
        urlForView = new WeakHashMap<ImageView, String>();
        missingResourceForView = new WeakHashMap<ImageView, Integer>();
        viewsToNotify = new HashMap<String, Set<WeakReference<ImageView>>>();
        callbacksToNotify = new HashMap<String, Set<ImageCallback>>();
    }

    /**
     * Load an image from the specified url (in the background) and set it on the view. Must be called from the
     * main UI thread.
     * @param url
     * @param view
     * @param loadingResource resource id to show when image is loading
     * @param missingResource resource id to show if url does not contain an image
     */
    @Override
    public void loadImage(String url, ImageView view, int loadingResource, int missingResource){

        if(Looper.myLooper() != Looper.getMainLooper()){
            Log.e(TAG, "Refusing to get image " + url + ": this method *MUST* be called from the main thread!");
            throw new RuntimeException("Refusing to get image " + url + ": this method *MUST* be called from the main thread!");
        }
        if (view == null){
            Log.e(TAG, "View was null");
            return;
        }

        if (url == null || url.trim().equals("") ){
            Log.e(TAG, "URL was null");
            urlForView.remove(view);
            if (missingResource > 0){
                view.setImageResource(missingResource);
            }else if (missingResource == NO_RESOURCE){
                view.setImageBitmap(null);
            }else if (loadingResource > 0){
                view.setImageResource(loadingResource);
            } else if (loadingResource  == NO_RESOURCE) {
                view.setImageBitmap(null);
            }
            return;
        }

        if (loadingResource > 0){
            view.setImageResource(loadingResource);
        } else if (loadingResource  == NO_RESOURCE) {
            view.setImageBitmap(null);
        } //don't do anything for KEEP_CURRENT

        synchronized (this){
            //adjust waiting states
            //synchronization note: we're not using synchronized maps here, as this is not necessary
            //since all manipulation of these maps is done on the main thread (and thus serialized).
            WeakReference<ImageView> reference = new WeakReference<ImageView>(view); //keep weak references, don't keep imageviews around forever!
            //set imageurl as desired image for the view, override previous
            urlForView.put(view, url);

            if (missingResource !=  loadingResource) //don't set missing resource if it's the same as loading resource, no necessary
                missingResourceForView.put(view, missingResource);
            //add view to list of views to notify when image has been downloaded
            if (viewsToNotify.get(url) == null){
                viewsToNotify.put(url, new HashSet<WeakReference<ImageView>>());
                viewsToNotify.get(url).add(reference);
                // nobody was downloading this yet, send intent
                getImage(url, new ImageHandler(new Handler()));
            } else  {
                //download is already queued, just add ourselves to the list of views to be notified when
                //download finishes
                viewsToNotify.get(url).add(reference);
            }
        }
    }

    @Override
    public void loadImage(String url, ImageCallback callback){

        if(Looper.myLooper() != Looper.getMainLooper()){
            Log.e(TAG, "Refusing to get image " + url + ": this method *MUST* be called from the main thread!");
            throw new RuntimeException("Refusing to get image " + url + ": this method *MUST* be called from the main thread!");
        }
        if (url == null ){
            Log.w(TAG, "Must provide all arguments (non-null)");
            callback.failure(url);
            return;
        }

        //adjust waiting states
        //synchronization note: we're not using synchronized maps here, as this is not necessary
        //since all manipulation of these maps is done on the main thread (and thus serialized).
        synchronized (this){
        //add callback to list of callbacks to notify when image has been downloaded
            if (callbacksToNotify.get(url) == null){
                callbacksToNotify.put(url, new HashSet<ImageCallback>());
                callbacksToNotify.get(url).add(callback);
                // nobody was downloading this yet, send intent
                getImage(url, new ImageHandler(new Handler()));
            } else  {
                //download is already queued, just add ourselves to the list of callbacks to be notified when
                //download finishes
                callbacksToNotify.get(url).add(callback);
            }
        }
    }

    abstract protected void getImage(String url, ImageHandler imageHandler);


    /**
     * Return true if url is still required
     * @param url
     * @return
     */
    @Override
    public boolean isUrlpending(String url){
        synchronized (this){
            //check if there are views that want the url
            Set<WeakReference<ImageView>> pendingViews = viewsToNotify.get(url);
            for(WeakReference<ImageView> viewReference : pendingViews){
                if (viewReference.get() != null){
                    ImageView view = viewReference.get();
                    if (urlForView.containsKey(view)
                            && urlForView.get(view).equals(url)){
                        return true;
                    }
                }
            }

            //check if there are images that want the url
            Set<ImageCallback> pendingCallbacks = callbacksToNotify.get(url);
            if (pendingCallbacks != null && !pendingCallbacks.isEmpty())
                return true;

            //nobody wants the url anymore
            return false;
        }
    }

    protected class ImageHandler extends ResultReceiver {

        public ImageHandler(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            switch (resultCode){
                case ImageService.RESULT_CODE_OK:{

                    //check which views are waiting for the image
                    String url = resultData.getString(ImageService.RESULT_URI);
                    Bitmap bitmap = resultData.getParcelable(ImageService.RESULT);

                    Set<WeakReference<ImageView>> pendingViews = viewsToNotify.get(url);
                    viewsToNotify.remove(url);
                    if (pendingViews != null)
                        for(WeakReference<ImageView> viewReference : pendingViews){
                            if (viewReference != null && viewReference.get() != null){ //is the view still there?
                                //check if the view is really still waiting for this particular image
                                //this could not be the case for ImageViews in ListViews or GridViews, as they are recycled
                                ImageView view = viewReference.get();
                                if (urlForView.containsKey(view)
                                        && urlForView.get(view).equals(url)){
                                    //okay, set image
                                    if (bitmap != null){
                                        view.setImageBitmap(bitmap);
                                    }
                                    else if(missingResourceForView.containsKey(view)){
                                        Integer resource = missingResourceForView.get(view);
                                        if (resource != null && resource > 0){
                                            view.setImageResource(missingResourceForView.get(view));
                                        }
                                        else if (resource != null && resource == NO_RESOURCE){
                                            view.setImageBitmap(null);
                                        }
                                    }
                                    urlForView.remove(view);
                                } else {
//                                        Log.d(TAG, "Received image " + url + " for view, but the view now wants: "
//                                                + urlForView.get(view));
                                }
                            } else {
//                                    Log.d(TAG, "A view waiting for "+ url +" dissappeared on us.");
                            }
                        }
                    Set<ImageCallback> pendingCallbacks = callbacksToNotify.get(url);
                    callbacksToNotify.remove(url);
                    if (pendingCallbacks != null)
                        for (ImageCallback callback : pendingCallbacks)
                            if (bitmap != null)
                                callback.onImageLoaded(url, bitmap);
                            else
                                callback.failure(url);
                    break;
                }
                case ImageService.RESULT_CODE_ERROR:{
                    Log.e(TAG, "Image not received");
                    String url = resultData.getString(ImageService.RESULT_URI);
                    Set<ImageCallback> pendingCallbacks = callbacksToNotify.get(url);
                    callbacksToNotify.remove(url);
                    if (pendingCallbacks != null)
                        for (ImageCallback callback : pendingCallbacks)
                            callback.failure(url);
                    break;
                }
            }
        }
    }
}
