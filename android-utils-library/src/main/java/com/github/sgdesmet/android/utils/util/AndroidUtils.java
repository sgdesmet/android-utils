/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.sgdesmet.android.utils.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;


/**
 * Class containing some static utility methods.
 */
public class AndroidUtils {

    public static final int IO_BUFFER_SIZE = 8 * 1024;

    private static final String TAG          = AndroidUtils.class.getSimpleName();
    private static final String PICTURES_DIR = "Pictures";
    private static final String MOVIES_DIR   = "Movies";

    private AndroidUtils() {

    }

    /**
     * Workaround for bug pre-Froyo, see here for more info:
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     */
    public static void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (hasHttpConnectionBug()) {
            System.setProperty( "http.keepAlive", "false" );
        }
    }

    /**
     * Check if external storage is built-in or removable.
     *
     * @return True if external storage is removable (like an SD card), false
     *         otherwise.
     */
    public static boolean isExternalStorageRemovable() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    /**
     * Get the external app cache directory.
     *
     * @param context The context to use
     *
     * @return The external cache dir
     */
    public static File getExternalCacheDir(Context context) {

        if (hasExternalCacheDir()) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File( Environment.getExternalStorageDirectory().getPath() + cacheDir );
    }

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /**
     * Create a file Uri for saving an image or video
     */
    public static Uri getOutputMediaFileUri(Context context, String name, int type)
            throws FileNotFoundException {

        return Uri.fromFile( getOutputMediaFile( context, name, type ) );
    }

    /**
     * Create a File for saving an image or video
     */
    public static File getOutputMediaFile(Context context, String name, int type)
            throws FileNotFoundException {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        if (Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED )) {

            File mediaStorageDir;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
                if (type == MEDIA_TYPE_IMAGE)
                    mediaStorageDir = new File( Environment.getExternalStorageDirectory(), PICTURES_DIR );
                else if (type == MEDIA_TYPE_VIDEO)
                    mediaStorageDir = new File( Environment.getExternalStorageDirectory(), MOVIES_DIR );
                else
                    throw new FileNotFoundException( "Unknown file type requested" );
            } else {
                if (type == MEDIA_TYPE_IMAGE)
                    mediaStorageDir = context.getExternalFilesDir( Environment.DIRECTORY_PICTURES );
                else if (type == MEDIA_TYPE_VIDEO)
                    mediaStorageDir = context.getExternalFilesDir( Environment.DIRECTORY_MOVIES );
                else
                    throw new FileNotFoundException( "Unknown file type requested" );
            }
            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d( TAG, "failed to create directory" );
                    throw new FileNotFoundException( "failed to create directory" );
                }
            }

            // Create a media file name
            File mediaFile;
            if (type == MEDIA_TYPE_IMAGE) {
                mediaFile = new File( mediaStorageDir.getPath() + File.separator +
                                      "IMG_" + name + ".jpg" );
            } else if (type == MEDIA_TYPE_VIDEO) {
                mediaFile = new File( mediaStorageDir.getPath() + File.separator +
                                      "VID_" + name + ".mp4" );
            } else {
                return null;
            }

            return mediaFile;
        } else
            throw new FileNotFoundException( "Device storage is not available, state is: " + Environment.getExternalStorageState() );
    }

    /**
     * Check how much usable space is available at a given path.
     *
     * @param path The path to check
     *
     * @return The space available in bytes
     */
    public static long getUsableSpace(File path) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs( path.getPath() );
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    /**
     * Get the memory class of this device (approx. per-app memory limit)
     */
    public static int getMemoryClass(Context context) {

        return ((ActivityManager) context.getSystemService( Context.ACTIVITY_SERVICE )).getMemoryClass();
    }

    /**
     * Check if OS version has a http URLConnection bug. See here for more information:
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     */
    public static boolean hasHttpConnectionBug() {

        return Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO;
    }

    /**
     * Check if OS version has built-in external cache dir method.
     */
    public static boolean hasExternalCacheDir() {

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    /**
     * Check if ActionBar is available.
     */
    public static boolean hasNativeActionBar() {

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isEmail(String text) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            return android.util.Patterns.EMAIL_ADDRESS.matcher( text ).matches();
        } else
            return text.contains( "@" ); //really simple fallback for 2.1
    }

    public static String getString(TextView editText) {

        if (editText == null || editText.getText() == null)
            return null;
        return editText.getText().toString();
    }

    /**
     * Wrapper around Html.fromHtml which checks for null input, and converts line breaks to {@code <br/>}
     * @param source
     * @return
     */
    public static CharSequence fromHtml(String source) {
        if (source == null)
            return null;
        return Html.fromHtml( source.replace( "\r\n", "<br/>" ).replace( "\n", "<br/>" ) );
    }

    public static int getCurrentApplicationVersion(Context context) {

        int v = 0;
        try {
            v = context.getPackageManager().getPackageInfo( context.getPackageName(), 0 ).versionCode;
        }
        catch (PackageManager.NameNotFoundException e) {
            // Huh? Really?
            Log.e( TAG, e.toString() );
        }
        return v;
    }

    public static String getCurrentPackageName(Context context) {

        try {
            return context.getPackageManager().getPackageInfo( context.getPackageName(), 0 ).packageName;
        }
        catch (PackageManager.NameNotFoundException e) {
            // Huh? Really?
            Log.e( TAG, e.toString() );
            throw new RuntimeException( e );
        }
    }

    /**
     * Checks if we are online. Note that an internet connection might be reported,
     * but but that does not necessarily mean it's usable (eg. VPN, no DNS, ...)
     */
    public static boolean hasConnection(Context applicationContext) {

        ConnectivityManager connectivityManager = (ConnectivityManager) applicationContext.getSystemService( Context.CONNECTIVITY_SERVICE );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting())
            return false;
        return true;
    }

    public static boolean isActivityValid(Activity activity) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            return activity != null && !activity.isFinishing() && !activity.isChangingConfigurations();
        else
            return activity != null && !activity.isFinishing();
    }

    public static boolean isLowBandwithNetwork(Context applicationContext) {

        //check for wifi
        ConnectivityManager connMgr = (ConnectivityManager) applicationContext.getSystemService( Context.CONNECTIVITY_SERVICE );
        android.net.NetworkInfo wifi = connMgr.getNetworkInfo( ConnectivityManager.TYPE_WIFI );

        if (!wifi.isConnectedOrConnecting()) {
            //if no wifi, check if we are on GPRS or EDGE
            TelephonyManager tm = (TelephonyManager) applicationContext.getSystemService( Context.TELEPHONY_SERVICE );
            if (tm != null && (tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE
                               || tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_GPRS)) {
                return true;
            }
        }
        return false;
    }

    public static Point getScreenSize(Context applicationContext) {

        WindowManager wm = (WindowManager) applicationContext.getSystemService( Context.WINDOW_SERVICE );
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
            display.getSize( size );
        else {
            size.x = display.getWidth();
            size.y = display.getHeight();
        }
        return size;
    }

    public static boolean isIntentAvailable(Context context, String action) {

        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent( action );
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities( intent, PackageManager.MATCH_DEFAULT_ONLY );
        return resolveInfo != null && !resolveInfo.isEmpty();
    }

    public static boolean stringNotBlank(final String string) {

        return string != null && string.trim().length() != 0;
    }

    public static boolean stringBlank(final String string) {

        return !stringNotBlank( string );
    }
}
