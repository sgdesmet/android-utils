package com.github.sgdesmet.android.utils.service.image.loader.impl;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * TODO description
 * <p/>
 * Date: 21/02/13
 * Time: 16:12
 *
 * @author: sgdesmet
 */
public class IntentServiceImageLoader extends AbstractImageLoader{

    private Context applicationContext;

    public void init(Context applicationContext){
        this.applicationContext = applicationContext;
    }

    public IntentServiceImageLoader(Context applicationContext) {
        super();
        this.applicationContext = applicationContext;
    }

    public IntentServiceImageLoader() {
        super();
    }

    protected void getImage(String url, ImageHandler imageHandler) {
        Intent intent = new Intent(applicationContext, ImageService.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        intent.putExtra(ImageService.EXTRA_CALLBACK, imageHandler);
        applicationContext.startService(intent);
    }

}
