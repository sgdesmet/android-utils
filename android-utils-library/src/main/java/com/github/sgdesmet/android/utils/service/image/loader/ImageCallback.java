package com.github.sgdesmet.android.utils.service.image.loader;

import android.graphics.Bitmap;


/**
 * TODO description
 * <p/>
 * Date: 21/02/13
 * Time: 16:12
 *
 * @author: sgdesmet
 */
public interface ImageCallback {

    public void onImageLoaded(String url, Bitmap bitmap);

    public void failure(String url);
}
