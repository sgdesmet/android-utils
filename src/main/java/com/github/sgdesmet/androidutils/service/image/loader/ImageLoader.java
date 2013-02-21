package com.github.sgdesmet.androidutils.service.image.loader;

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
    int NO_RESOURCE = -2;

    void loadImage(String url, ImageView view, int loadingResource, int missingResource);

    void loadImage(String url, ImageCallback callback);

    boolean isUrlpending(String url);
}
