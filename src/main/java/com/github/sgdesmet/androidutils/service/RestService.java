package com.github.sgdesmet.androidutils.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import com.github.sgdesmet.androidutils.util.AndroidUtils;
import com.google.gson.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * IntentService wrapper for SimpleRestJSON. IntentService and ResultReceiver provide background threading and callbacks.
 *
 * @author: sgdesmet
 */
public class RestService extends IntentService {

    private static final String TAG = RestService.class.getSimpleName();
    private static final String BASE = RestService.class.getName();

    //constants for intent extra data
    public static final String EXTRA_CALLBACK = BASE + ".Callback";

    public static final String ACTION_GET = BASE + ".Get";
    public static final String ACTION_CREATE = BASE + ".Create";
    public static final String ACTION_UPDATE = BASE + ".Update";
    public static final String ACTION_DELETE = BASE + ".Delete";

    public static final String EXTRA_REQUEST_OBJECT = BASE + ".RequestObject";
    public static final String EXTRA_REQUEST_OBJECT_ETAG = BASE + ".ETag";
    public static final String EXTRA_REQUEST_OBJECT_LAST_MODIFIED = BASE + ".LastModified";
    public static final String EXTRA_EXPECTED_RESPONSE_TYPE = BASE + ".ResponseType";

    public static final String EXTRA_HTTP_AUTH_USERNAME = BASE + ".Username";
    public static final String EXTRA_HTTP_AUTH_PASSWORD = BASE + ".Password";
    public static final String EXTRA_HTTP_REQUEST_PARAMS = BASE + ".Params";

    public static final String EXTRA_HTTP_CACHE_CONTROL = BASE + ".CacheControl";
    public static final String REVALIDATE = "max-age=0";
    public static final String NO_CACHE = "no-cache";


    public static final String RESULT = BASE + ".Result";
    public static final String RESULT_ETAG = BASE + ".Result.ETag";
    public static final String RESULT_LAST_MODIFIED = BASE + ".Result.LastModified";
    public static final String RESULT_ORIGINAL_INTENT = BASE + ".ResultIntent";

    //todo make enum? but resultreceiver wants int
    public static final int RESULT_CODE_OK = 0;
    public static final int RESULT_CODE_PARSE_ERROR = 1;
    public static final int RESULT_CODE_CONNECTION_ERROR = 2;
    public static final int RESULT_CODE_UNEXPECTED_RESPONSE = 3;
    public static final int RESULT_CODE_NEED_UPDATE_ERROR = 4;
    public static final int RESULT_CODE_OK_NOT_MODIFIED = 6;
    public static final int RESULT_CODE_OK_CREATED = 7;
    public static final int RESULT_CODE_OK_NO_CONTENT = 8;
    public static final int RESULT_CODE_NOT_FOUND = 9;
    public static final int RESULT_CODE_MISSING_CREDENTIALS = 10;
    public static final int RESULT_CODE_FORBIDDEN= 11;

    protected static final String TRUSTSTORE_PASSWORD = "secret";
    protected static final String HTTPS = "https";
    private static final String CACHE_CONTROL = "Cache-Control";

    protected static Gson gson = null;

    public RestService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service created");
        configure();
    }

    protected void configure(){
    }

    protected Gson getGsonConfig(){
        if (gson == null){
            GsonBuilder gsonBuilder = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .setDateFormat("yyyy-MM-dd HH:mm:ss Z");
            gson = gsonBuilder.create();
        }
        return gson;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(TAG, "received intent: " + intent);
        Bundle extras = intent.getExtras();
        String action = intent.getAction();
        Uri data =  intent.getData();
        URL url = null;
        try {
            url = new URL(data.toString());
        } catch (MalformedURLException e) {
            Log.e(TAG, "Error parsing intent data do URL: " + e.toString());
        }

        if (extras == null || action == null || url == null || !extras.containsKey(EXTRA_CALLBACK)) {
            Log.e(TAG, "You did not pass the necessary params with the Intent.");
            return;
        }

        ResultReceiver callback = extras.getParcelable(EXTRA_CALLBACK);

        Serializable objectToPost = extras.getSerializable(EXTRA_REQUEST_OBJECT);
        Class requestType = objectToPost != null ? objectToPost.getClass() : null;
        Class resultType = (Class) extras.getSerializable(EXTRA_EXPECTED_RESPONSE_TYPE);

        String username = extras.getString(EXTRA_HTTP_AUTH_USERNAME);
        String password = extras.getString(EXTRA_HTTP_AUTH_PASSWORD);
        Bundle extraParams = extras.getBundle(EXTRA_HTTP_REQUEST_PARAMS);
        HashMap<String,String> params = new HashMap<String, String>();
        if (extraParams != null)
            for (String key : extraParams.keySet()){
                params.put(key, extraParams.getString(key));
            }


        SimpleRestJSON rest = SimpleRestJSON.getInstance();
        if (getGsonConfig() != null)
            rest.setGson(gson);
        SimpleRestJSON.RestCallback resultHandler = getResultHandler(callback, intent);

        if (username != null || password != null){
            rest.setHttpBasicAuthorization(username, password);
        }

        String cacheControl = extras.getString(EXTRA_HTTP_CACHE_CONTROL);
        if (AndroidUtils.stringNotBlank(cacheControl))
            rest.addRequestHeader(CACHE_CONTROL, cacheControl);
        else
            rest.removeRequestHeader(CACHE_CONTROL);

        try {
            if (action.equals(ACTION_GET)){
                String etag = extras.getString(EXTRA_REQUEST_OBJECT_ETAG);
                String modifiedSince = extras.getString(EXTRA_REQUEST_OBJECT_LAST_MODIFIED);
                rest.doGet(url, params,etag, modifiedSince, resultType, resultHandler);
            }else if (action.equals(ACTION_CREATE)){
                rest.doPost(url, params, objectToPost, requestType, resultType, resultHandler);
            }else if (action.equals(ACTION_DELETE)){
                rest.doDelete(url, params, objectToPost, requestType, resultType, resultHandler);
            }else if (action.equals(ACTION_UPDATE)){
                rest.doPut(url, params, objectToPost, requestType, resultType, resultHandler);
            } else {
                Log.e(TAG, "Unknown action received!");
            }
        } catch (IOException e){
            Log.e(TAG, "I/O Error: " + e);
            Bundle bundle = new Bundle();
            bundle.putParcelable(RESULT_ORIGINAL_INTENT, intent);
            callback.send(RESULT_CODE_CONNECTION_ERROR, bundle);
        }
    }

    protected SimpleRestJSON.RestCallback getResultHandler(ResultReceiver callback, Intent intent){
        return new CallbackResultHandler<Serializable>(callback, intent);
    }

    protected class CallbackResultHandler<R extends Serializable> implements SimpleRestJSON.RestCallback<R> {

        protected final ResultReceiver callback;
        protected final Intent originalIntent;

        protected CallbackResultHandler(ResultReceiver callback, Intent originalIntent) {
            this.callback = callback;
            this.originalIntent = originalIntent;
        }

        public ResultReceiver getCallback() {
            return callback;
        }

        protected Bundle getBundleWithOriginalIntent(){
            Bundle bundle = new Bundle();
            bundle.putParcelable(RESULT_ORIGINAL_INTENT, originalIntent);
            return bundle;
        }

        protected String getOriginalIntentString(){
            if (originalIntent != null)
                return "Intent: "
                        + originalIntent.getAction()
                        + " " + originalIntent.getData()
                        + " (" + originalIntent.getExtras() + ")";
            return "Request data not available (intent is null)";
        }

        @Override
        public void onNoContentResponse() {
            Log.d(TAG, "Received not modified response");
            callback.send(RESULT_CODE_OK_NO_CONTENT, getBundleWithOriginalIntent());
        }

        @Override
        public void onCreatedResponse(R response, String etag, String lastModified) {
            Bundle bundle =  getBundleWithOriginalIntent();
            bundle.putSerializable(RESULT, response);
            bundle.putString(RESULT_ETAG, etag);
            bundle.putString(RESULT_LAST_MODIFIED, lastModified);
            callback.send(RESULT_CODE_OK_CREATED, bundle);
        }

        @Override
        public void onJsonParseError(String message) {
            String errMessage = "JSON parse error: " + message + " for " + getOriginalIntentString();
            Log.e(TAG, errMessage);
            Bundle bundle = getBundleWithOriginalIntent();
            bundle.putString(RESULT, errMessage);
            callback.send(RESULT_CODE_PARSE_ERROR, getBundleWithOriginalIntent());
        }

        @Override
        public void onUnexpectedResponse(int httpCode, String httpMessage, String errorBody) {
            String message = "Received http code: " + httpCode
                    + " (" + httpMessage + ") with body: " + errorBody
                    + " for " + getOriginalIntentString();
            Log.e(TAG, message);
            Bundle bundle = getBundleWithOriginalIntent();
            bundle.putString(RESULT, message);
            callback.send(RESULT_CODE_UNEXPECTED_RESPONSE, bundle);
        }

        @Override
        public void onNotModifiedResponse() {
            Log.d(TAG, "Received not modified response");
            callback.send(RESULT_CODE_OK_NOT_MODIFIED, getBundleWithOriginalIntent());
        }

        @Override
        public void onResponse(R response,  String etag, String lastModified) {
            Bundle bundle =  getBundleWithOriginalIntent();
            bundle.putSerializable(RESULT, response);
            bundle.putString(RESULT_ETAG, etag);
            bundle.putString(RESULT_LAST_MODIFIED, lastModified);
            callback.send(RESULT_CODE_OK, bundle);
        }

        @Override
        public void onResoureNotFound() {
            Log.e(TAG, "Resource not found for " + getOriginalIntentString());
            callback.send(RESULT_CODE_NOT_FOUND, getBundleWithOriginalIntent());
        }

        @Override
        public void onUnauthorized() {
            Log.e(TAG, "Missing credentials for " + getOriginalIntentString());
            callback.send(RESULT_CODE_MISSING_CREDENTIALS, getBundleWithOriginalIntent());
        }

        @Override
        public void onForbidden(String reason) {
            Log.e(TAG, "Forbidden " + getOriginalIntentString());
            Bundle bundle = getBundleWithOriginalIntent();
            bundle.putString(RESULT, reason);
            callback.send(RESULT_CODE_FORBIDDEN, bundle);
        }
    }
}
