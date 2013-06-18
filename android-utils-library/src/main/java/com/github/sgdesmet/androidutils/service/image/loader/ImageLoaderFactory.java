package com.github.sgdesmet.androidutils.service.image.loader;

import android.content.Context;
import com.github.sgdesmet.androidutils.service.image.loader.impl.AbstractImageLoader;
import com.github.sgdesmet.androidutils.service.image.loader.impl.IntentServiceImageLoader;

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

    public static void init(Context applicationContext){
        context = applicationContext;
    }

    private static class SingletonHolder{
        public static final AbstractImageLoader INSTANCE= new IntentServiceImageLoader(context);
    }

    public static synchronized AbstractImageLoader get() {
        return SingletonHolder.INSTANCE;
    }

}
