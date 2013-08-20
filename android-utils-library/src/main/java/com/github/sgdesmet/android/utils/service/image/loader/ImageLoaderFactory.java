package com.github.sgdesmet.android.utils.service.image.loader;

import android.content.Context;
import com.github.sgdesmet.android.utils.service.image.loader.impl.*;


/**
 * TODO description
 * <p/>
 * Date: 05/10/12
 * Time: 10:46
 *
 * @author: sgdesmet
 */
public class ImageLoaderFactory {

    private static final String TAG = ImageLoaderFactory.class.getSimpleName();

    private static Context context;
    private static int     numThreads;
    private static int     cacheSize;

    public static void init(final Context applicationContext, final int numThreads, final int cacheSize) {

        context = applicationContext;
        ImageLoaderFactory.numThreads = numThreads;
        ImageLoaderFactory.cacheSize = cacheSize;
    }

    private static class SingletonHolder {

        public static final ImageLoader INSTANCE = new ImageLoaderImpl( context, numThreads, cacheSize );
    }

    public static synchronized ImageLoader get() {

        return SingletonHolder.INSTANCE;
    }
}
