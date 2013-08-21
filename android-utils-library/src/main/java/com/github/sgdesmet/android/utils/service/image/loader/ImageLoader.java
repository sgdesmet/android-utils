package com.github.sgdesmet.android.utils.service.image.loader;

import android.widget.ImageView;


/**
 * TODO description
 * <p/>
 * Date: 21/02/13
 * Time: 16:11
 *
 * @author: sgdesmet
 */
public interface ImageLoader {

    int KEEP_CURRENT = -1;
    int NO_RESOURCE  = -2;

    /**
     * Load image from url and set it on the indicated view.
     *
     * @param loadingResource drawable to show when loading
     * @param missingResource drawable to set when given url returned an error
     */
    void loadImage(String url, ImageView view, int loadingResource, int missingResource);

    /**
     * Load image and notify by callback if ready
     */
    void loadImage(String url, ImageCallback callback);

}
