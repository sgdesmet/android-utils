package com.github.sgdesmet.android.utils.util;

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
    private String secret;

    private static class SingletonHolder{
        public static final CredentialsStore INSTANCE= new CredentialsStore();
    }

    public static CredentialsStore getInstance(){

        return SingletonHolder.INSTANCE;
    }

    protected CredentialsStore(){

    }

    public static void init(Context applicationContext, String secret){
        getInstance().context = applicationContext;
        getInstance().secret = secret;
    }

    /******************
     * Credential storage
     *****************/

    /**
     * Note! Credentials are stored <b>obfuscated</b>. An attacker with root access can still get the key by inspecting
     * this app.
     */
    public void store(String key, String value){
        if (context != null && key != null){
            SharedPreferences prefs = getObscuredPreferences(PREFS_FILE_NAME, secret);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(key, value);
            editor.commit();
        } else if (context == null) {
            Log.e(TAG, String.format( "Store: context cannot be null" ));
        } else {
            Log.e(TAG, String.format( "Store: key cannot be null" ));
        }
    }

    public String get(String key){
        if (context != null && key != null){
            SharedPreferences prefs = getObscuredPreferences(PREFS_FILE_NAME, secret);
            return prefs.getString(key, null);
        } else if (context == null) {
            Log.e(TAG, String.format( "Get: context cannot be null" ));
        } else {
            Log.e(TAG, String.format( "Get: key cannot be null" ));
        }
        return null;
    }

    public void delete(String key){
        if (context != null && key != null){
            SharedPreferences prefs = getObscuredPreferences(PREFS_FILE_NAME, secret);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(key);
            editor.commit();
        } else if (context == null) {
            Log.e(TAG, String.format( "Delete: context cannot be null" ));
        } else {
            Log.e(TAG, String.format( "Delete: key cannot be null" ));
        }
    }

    public void clearAllCredentials(){
        if (context != null){
            SharedPreferences prefs = getObscuredPreferences(PREFS_FILE_NAME, secret);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            
            editor.commit();

        } else{
            Log.e(TAG, String.format( "ClearAll: context cannot be null" ));
        }
    }

    private ObscuredSharedPreferences obscuredSharedPreferences;
    protected ObscuredSharedPreferences getObscuredPreferences(String name, String secret){
        if ( null == obscuredSharedPreferences ){
            obscuredSharedPreferences = new ObscuredSharedPreferences(
                    context, context.getSharedPreferences(name, Context.MODE_PRIVATE), secret );
        }
        return obscuredSharedPreferences;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
