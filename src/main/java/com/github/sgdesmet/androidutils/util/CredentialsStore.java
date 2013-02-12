package com.github.sgdesmet.androidutils.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * TODO description
 * <p/>
 * Date: 28/09/12
 * Time: 17:11
 *
 * @author: sgdesmet
 */
public class CredentialsStore {

    private static final String TAG = CredentialsStore.class.getSimpleName();

    private static final String PREFS_FILE_NAME = "config";

    private Context context;

    private static class SingletonHolder{
        public static final CredentialsStore INSTANCE= new CredentialsStore();
    }

    public static CredentialsStore getInstance(){

        return SingletonHolder.INSTANCE;
    }

    protected CredentialsStore(){

    }

    public static void init(Context applicationContext){
        getInstance().context = applicationContext;
    }

    /******************
     * Credential storage
     *****************/

    /**
     * Note! Credentials are stored <b>obfuscated</b>. An attacker with root access can still get the key by inspecting
     * this app.
     */
    public void store(String key, String value){
        if (context != null){
            SharedPreferences prefs = new ObscuredSharedPreferences(
                    context, context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE) );
            if (key != null){
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(key, value);
                editor.commit();
            }

        } else {
            Log.e(TAG, "Context was null");
        }
    }

    public String get(String key){
        if (context != null){
            SharedPreferences prefs = new ObscuredSharedPreferences(
                    context, context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE) );
            return prefs.getString(key, null);
        } else {
            Log.e(TAG, "Context was null");
            return null;
        }
    }

    public void delete(String key, Context context){
        if (context != null){
            SharedPreferences prefs = new ObscuredSharedPreferences(
                    context, context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE) );
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(key);
            editor.commit();
        } else {
            Log.e(TAG, "Context was null");
        }
    }

    public void clearAllCredentials(){
        if (context != null){
            SharedPreferences prefs = new ObscuredSharedPreferences(
                    context, context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE) );
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            
            editor.commit();

        }  else {
            Log.e(TAG, "Context was null");
        }
    }

}
