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
import android.graphics.Point;
import android.net.*;
import android.os.*;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import org.jetbrains.annotations.Nullable;


/**
 * Class containing some static utility methods.
 */
public class AndroidUtils {

    public static final int IO_BUFFER_SIZE = 8 * 1024;

    private static final String TAG          = AndroidUtils.class.getSimpleName();
    private static final String PICTURES_DIR = "Pictures";
    private static final String MOVIES_DIR   = "Movies";

    protected AndroidUtils() {

    }

    /**
     * Check if external storage is built-in or removable.
     *
     * @return True if external storage is removable (like an SD card), false
     * otherwise.
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
    public static Uri getOutputMediaFileUri(final Context context, final String name, final int type)
            throws FileNotFoundException {

        File file = getOutputMediaFile( context, name, type );
        return file != null? Uri.fromFile( file ): null;
    }

    /**
     * Create a File for saving an image or video, which is accessible by other apps
     */
    @Nullable
    public static File getOutputMediaFile(final Context context, final String name, final int type)
            throws FileNotFoundException {

        File mediaStorageDir;
        if (Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED )) {

            if (type == MEDIA_TYPE_IMAGE)
                mediaStorageDir = context.getExternalFilesDir( Environment.DIRECTORY_PICTURES );
            else if (type == MEDIA_TYPE_VIDEO)
                mediaStorageDir = context.getExternalFilesDir( Environment.DIRECTORY_MOVIES );
            else
                throw new FileNotFoundException( "Unknown file type requested" );

            if (mediaStorageDir == null)
                return null;

            // Create a media file name
            File mediaFile;
            if (type == MEDIA_TYPE_IMAGE) {
                mediaFile = new File( mediaStorageDir, "IMG_" + name + ".jpg" );
            } else if (type == MEDIA_TYPE_VIDEO) {
                mediaFile = new File( mediaStorageDir, "VID_" + name + ".mp4" );
            } else {
                throw new FileNotFoundException( "Unknown file type requested" );
            }
            if (!mediaFile.setWritable( true, false ))
                Log.w( TAG, "Can't set output media file writable" );
            if (!mediaFile.setReadable( true, false ))
                Log.w( TAG, "Can't set output media file readable" );
            return mediaFile;
        }

        return null;
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
     */
    public static CharSequence fromHtml(@Nullable final String source) {

        if (source == null)
            return null;
        return Html.fromHtml( source.replace( "\r\n", "<br/>" ).replace( "\n", "<br/>" ) );
    }

    /**
     * Wrapper around Html.toHtml. Null input is allowed (returns null), and line breaks are removed.
     */
    public static CharSequence toHtml(@Nullable final Spanned text) {

        if (text == null)
            return null;
        String converted = Html.toHtml( text );
        return converted.replace( "\n", "" );
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

    public static void dismissKeyboard(@Nullable final Activity activity) {

        if (activity != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService( Context.INPUT_METHOD_SERVICE );
            if (activity.getCurrentFocus() != null)
                imm.hideSoftInputFromWindow( activity.getCurrentFocus().getWindowToken(), 0 );
        }
    }
}
