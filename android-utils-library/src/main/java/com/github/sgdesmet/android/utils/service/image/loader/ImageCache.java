package com.github.sgdesmet.android.utils.service.image.loader;

import android.content.Context;
import android.os.Environment;
import android.support.v4.util.LruCache;
import android.util.Log;
import com.github.sgdesmet.android.utils.util.AndroidUtils;
import com.jakewharton.DiskLruCache;
import java.io.*;

/**
 * TODO description
 * <p/>
 * Date: 17/10/12
 * Time: 17:35
 *
 * @author: sgdesmet
 */
@Deprecated
public class ImageCache implements Closeable {

    private static final String TAG = ImageCache.class.getSimpleName();

    private static final int MEMORY_CACHE = 1 * 1024 * 1024;
    private static final int DISK_CACHE = 2 * 1024 * 1024;
    private static final String IMAGE_CACHE = "image_cache";
    private static final int CACHE_VERSION = 1;

    private LruCache<String,byte[]> memoryCache;
    private DiskLruCache diskCache;

    private Context applicationContext;

    private static class SingletonHolder{
        public static final ImageCache INSTANCE= new ImageCache();
    }

    public static ImageCache getInstance(){

        return SingletonHolder.INSTANCE;
    }

    protected ImageCache() {
    }


    public synchronized void open(Context applicationContext) {

        this.applicationContext = applicationContext;

        //memory cache with a max specified memory usage
        if (memoryCache == null)
            memoryCache = new LruCache<String, byte[]>(MEMORY_CACHE){
                @Override
                protected int sizeOf(String key, byte[] value) {
                    return value.length;
                }
            };
        if (diskCache == null || diskCache.isClosed()){
            try {
                diskCache = getDiskCache(applicationContext);
            } catch (IOException e) {
                Log.e(TAG, "Unable to open disk cache part for images: " + e.toString());
            }
        }
    }

    public synchronized boolean isClosed(){
        return memoryCache == null || diskCache == null || diskCache.isClosed();
    }

    @Override
    public synchronized void close() throws IOException {
        if (diskCache != null){
            diskCache.close();
        }
    }

    /**********
     * Handling our caches
     **********/

    public synchronized void put(String url, byte[] data) {

        Log.d(TAG, "Need to add image " + url + " to cache. " +
                "\t-Memory cache size is now: " + memoryCache.size() / 1024 + "kB, image data is:" + data.length / 1024 + "kB" +
                "\t-Disk cache size is: " + diskCache.size()/1024 + "kB");
        String key = getHash(url);

        //memory
        memoryCache.put(key, data);

        try {
            if (diskCache.get(key) == null){
                DiskLruCache.Editor editor = null;
                try {
                    editor = diskCache.edit( key );
                    if ( editor == null ) {
                        return;
                    }

                    if( writeDataToFile(data, editor) ) {
                        diskCache.flush();
                        editor.commit();

                    } else {
                        editor.abort();

                    }
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                    try {
                        if ( editor != null ) {
                            editor.abort();
                        }
                    } catch (IOException ignored) {
                        Log.e(TAG, ignored.toString());
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

    }

    public synchronized byte[] get(String url) {

        String key = getHash(url);

        //is the bitmap in memory?
        byte[] bitmap = memoryCache.get(key);

        if (bitmap == null){
            //try getting it from disk then
            DiskLruCache.Snapshot snapshot = null;
            try {
                snapshot = diskCache.get( key );
                if ( snapshot == null ) {
                    return null;
                }
                final InputStream in = snapshot.getInputStream( 0 );
                if ( in != null ) {
                    final BufferedInputStream buffIn =
                            new BufferedInputStream( in, AndroidUtils.IO_BUFFER_SIZE );

                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[1024];
                    while ((nRead = buffIn.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    buffer.flush();

                    bitmap = buffer.toByteArray();

                    Log.d(TAG, "Got image " + url + " (hashCode: " + key + ") from disk");

                    //now, also put it in memory for faster access next time
                    memoryCache.put(key, bitmap);
                }
            } catch ( IOException e ) {
                e.printStackTrace();
            } finally {
                if ( snapshot != null ) {
                    snapshot.close();
                }
            }

        }else {
            Log.d(TAG, "Got image " + url + " (hashCode: " + key + ") from memory");
        }

        return bitmap;
    }

    public synchronized boolean contains(String url) {

        String key = getHash(url);

        if (memoryCache.get(key) != null)
            return true;
        boolean containedInDisk = false;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = diskCache.get( key );
            containedInDisk = snapshot != null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if ( snapshot != null ) {
                snapshot.close();
            }
        }

        return containedInDisk;

    }

    /**
     * Purge the cache
     */
    public synchronized void clear() {

        memoryCache.evictAll();

        try {
            diskCache.delete();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**********
     * Utility methods for handling our caches
     **********/

    private String getHash(String key){
        return Integer.toString(key.hashCode());
    }

    private DiskLruCache getDiskCache(Context applicationContext) throws IOException{

        File diskCacheDir = getDiskCacheDir(applicationContext, IMAGE_CACHE);
        DiskLruCache diskCache = DiskLruCache.open( diskCacheDir, CACHE_VERSION, 1, DISK_CACHE );
        return diskCache;
    }

    private File getDiskCacheDir(Context context, String uniqueName) {

        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||
                        !AndroidUtils.isExternalStorageRemovable() ?
                        AndroidUtils.getExternalCacheDir(context).getPath() :
                        context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    private boolean writeDataToFile( byte[] data, DiskLruCache.Editor editor )
            throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream( editor.newOutputStream( 0 ), AndroidUtils.IO_BUFFER_SIZE );
            out.write(data);
            return true;
        } finally {
            if ( out != null ) {
                out.close();
            }
        }
    }
}
