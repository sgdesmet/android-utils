package com.github.sgdesmet.androidutils.service.image.loader;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.util.LruCache;
import android.util.Log;
import com.github.sgdesmet.androidutils.service.SimpleRestJSON;
import com.github.sgdesmet.androidutils.util.AndroidUtils;
import com.github.sgdesmet.androidutils.util.BitmapUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;

/**
 * TODO It's stupid to put this in a service, migrate this code to imageloaderfactory, and use thread pools
 * <p/>
 * Date: 04/10/12
 * Time: 17:13
 *
 * @author: sgdesmet
 */
public class ImageService extends IntentService {

    private static final String TAG = ImageService.class.getSimpleName();
    private static final String BASE = ImageService.class.getName();

    public static final int RESULT_CODE_OK = 0;
    public static final int RESULT_CODE_ERROR = 1;

    public static final String RESULT = BASE + ".Result";
    public static final String RESULT_URI = BASE + ".Uri";

    public static final String EXTRA_CALLBACK = BASE + ".Callback";

    private static final int MEMORY_CACHE = 2 * 1024 * 1024;
    private static LruCache<String,Bitmap> memoryCache;

    public ImageService() {
        super(TAG);
    }

    protected synchronized LruCache<String,Bitmap> getMemoryCache(){
        if (memoryCache == null){
            memoryCache = new LruCache<String, Bitmap>(MEMORY_CACHE){
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getRowBytes() * bitmap.getHeight();
                }
            };
        }
        return memoryCache;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Bundle extras = intent.getExtras();
        String action = intent.getAction();
        Uri uri =  intent.getData();

        if (action == null || uri == null || !extras.containsKey(EXTRA_CALLBACK)) {
            Log.e(TAG, "You did not pass the necessary params with the Intent.");
            return;
        }

        ResultReceiver callback = extras.getParcelable(EXTRA_CALLBACK);

        Bitmap bitmap = null;

        if (getMemoryCache().get(uri.toString()) != null){
            bitmap = getMemoryCache().get(uri.toString());
        } else {
            try {
                //see if we still need the imagefactory
                if (ImageLoaderFactory.get().wantsUrl(uri.toString())){
                    byte[] image = getImage(uri.toString());
                    if (image != null && image.length != 0){
                        bitmap = BitmapUtils.decodeImageMemoryEfficient(image); //also do decoding into a bitmap here, so we don't have to do it on the main thread
                        getMemoryCache().put(uri.toString(), bitmap);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error while getting image (from " + uri + "): " + e);
                Bundle args = new Bundle();
                args.putString(RESULT_URI, uri.toString());
                callback.send(RESULT_CODE_ERROR, args);
            } catch (GeneralSecurityException e) {
                Log.e(TAG, "Error while getting image (from " + uri + "): " + e);
                Bundle args = new Bundle();
                args.putString(RESULT_URI, uri.toString());
                callback.send(RESULT_CODE_ERROR, args);
            }
        }

        Bundle args = new Bundle();
        args.putParcelable(RESULT, bitmap);
        args.putString(RESULT_URI, uri.toString());
        callback.send(RESULT_CODE_OK, args);
    }

    protected byte[] getImage(String imageUrl) throws IOException, GeneralSecurityException {
        HttpURLConnection connection = null;
        InputStream is = null;
        try {
            connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setConnectTimeout(SimpleRestJSON.TIMEOUT); //TODO may need to add a timer to forcibly terminate if necessary?
            connection.setReadTimeout(SimpleRestJSON.TIMEOUT);
            //TODO cache-control?

            is = connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            is.close();
            return buffer.toByteArray();
        } finally {
            if (is != null)
                is.close();
            if (connection != null)
                connection.disconnect();
        }

    }
}
