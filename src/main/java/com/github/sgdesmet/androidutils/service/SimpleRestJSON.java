package com.github.sgdesmet.androidutils.service;

import android.os.Build;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.codec.binary.Base64;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * TODO description
 * <p/>
 * Date: 20/08/12
 * Time: 16:34
 *
 * @author: sgdesmet
 */
public class SimpleRestJSON {

    protected static final String CONTENT_TYPE_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded; charset=UTF-8";
    protected static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";

    private static final String ENCODING = "UTF-8";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_BASIC = "Basic";

    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_JSON = "application/json";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String IF_NONE_MATCH = "If-None-Match";
    private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    private static final String LAST_MODIFIED = "Last-Modified";
    private static final String ETAG = "ETag";
    public static final int TIMEOUT = 20000;
    private static final String TAG = SimpleRestJSON.class.getSimpleName();


    public enum HttpMethod {
        GET, POST, PUT, DELETE
    }

    private Map<String,String> requestHeaders;

    private SSLContext sslContext;
    private boolean gzipEnabled;

    protected Gson gson;

    private static class SingletonHolder{
        public static final SimpleRestJSON INSTANCE= new SimpleRestJSON();
    }

    public static SimpleRestJSON getInstance(){

        return SingletonHolder.INSTANCE;
    }

    protected SimpleRestJSON() {
        //urlconnection support in old apis
        configureConnections();

        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().generateNonExecutableJson();

        gson = gsonBuilder.create();

        requestHeaders = new HashMap<String, String>();
        gzipEnabled = true;
    }

    public Gson getGson() {
        return gson;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public void addRequestHeader(String key, String value){
        if (requestHeaders == null)
            requestHeaders = new HashMap<String, String>();
        requestHeaders.put(key, value);
    }

    public void removeRequestHeader(String key){
        if (requestHeaders != null)
            requestHeaders.remove(key);
    }

    public void setAuthorizationHeader(String authorizationType, String value){
        removeRequestHeader(AUTHORIZATION_HEADER);
        addRequestHeader(AUTHORIZATION_HEADER, authorizationType + " " + value );
    }

    public void setHttpBasicAuthorization(String username, String password){

        String encodedCredentials = new String(
                Base64.encodeBase64(
                        ((username != null ? username : "") + ":" + (password != null ? password : ""))
                                .getBytes()
                )
        );
        setAuthorizationHeader(AUTHORIZATION_BASIC, encodedCredentials);
    }

    public void removeAuthorization(){
        removeRequestHeader(AUTHORIZATION_HEADER);
    }

    private static String getParametersString(Map<String, String> parameters) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (builder.length() > 0)
                builder.append("&");
            builder.append(urlEncode(entry.getKey()));
            builder.append("=");
            if (entry.getValue() != null)
                builder.append(urlEncode(entry.getValue()));
        }

        return builder.toString();
    }

    private static String urlEncode(String original) {
        try {
            return URLEncoder.encode(original != null ? original : "", ENCODING);
        } catch (UnsupportedEncodingException e) {
            return original;
        }
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public boolean isGzipEnabled() {
        return gzipEnabled;
    }

    public void setGzipEnabled(boolean gzipEnabled) {
        this.gzipEnabled = gzipEnabled;
    }

    /**
     * Get JSON
     */
    public void doGet(URL url, Map<String, String> params, Type expectedResultType, RestCallback callback) throws IOException {
        executeRest(HttpMethod.GET, url, params, null, null, expectedResultType,callback);
    }

    /**
     * Conditional GET call, based on ETag, or Modified-Since values. Calls RestCallback.onNotModifiedResponse when the
     * object has not changed
     * @param url
     * @param params
     * @param eTagValue
     * @param lastModifiedValue
     * @param expectedResultType
     * @param callback
     * @throws IOException
     */
    public void doGet(URL url, Map<String, String> params,
                      String eTagValue, String lastModifiedValue,
                      Type expectedResultType, RestCallback callback) throws IOException {
        executeRest(HttpMethod.GET, url, params, null, null, eTagValue, lastModifiedValue, expectedResultType,callback);
    }

    /**
     * Post JSON
     */
    public void doPost(URL url, Map<String,String> params, Serializable objectToPost, Type typeOfObject, Type expectedResultType, RestCallback callback) throws IOException {
        executeRest(HttpMethod.POST, url, params, objectToPost, typeOfObject,expectedResultType,callback);
    }


    /**
     * Put JSON
     */
    public void doPut(URL url, Map<String,String> params, Serializable objectToPut, Type typeOfObject, Type expectedResultType, RestCallback callback) throws IOException {
        executeRest(HttpMethod.PUT, url, params, objectToPut, typeOfObject,expectedResultType,callback);
    }


    /**
     * Delete JSON
     */
    public void doDelete(URL url, Map<String,String> params, Serializable objectToPost, Type typeOfObject, Type expectedResultType, RestCallback callback) throws IOException {
        executeRest(HttpMethod.DELETE, url, params, objectToPost, typeOfObject,expectedResultType,callback);
    }

    public void executeRest(HttpMethod method, URL url, Map<String,String> params,
                            Serializable objectToPost, Type typeOfObject,
                            Type expectedResultType, RestCallback callback) throws IOException {
        executeRest(method,url, params, objectToPost, typeOfObject, null, null, expectedResultType, callback);
    }

    /**
     * Note: this method is synchronous, use of threading is recommended
     */
    public void executeRest(HttpMethod method, URL url, Map<String,String> params,
                            Serializable objectToPost, Type typeOfObject,
                            String eTagValue, String lastModifiedValue,
                            Type expectedResultType, RestCallback callback) throws IOException {
        //setup
        HttpURLConnection connection = null;
        try {
            String baseURL = url.toString();
            URL urlWithParams = new URL(baseURL + (baseURL.indexOf('?') < 0 ? '?' : '&') + getParametersString(params));
            connection = (HttpURLConnection)urlWithParams.openConnection();
            connection.setInstanceFollowRedirects(true);
            if (connection instanceof HttpsURLConnection && sslContext != null){
                Log.d(TAG, "Setting custom SSL Context");
                ((HttpsURLConnection)connection).setSSLSocketFactory(sslContext.getSocketFactory());
            }
            connection.setDoOutput(method != HttpMethod.GET && method != HttpMethod.DELETE);
            connection.setConnectTimeout(TIMEOUT); //TODO may need to add a timer to forcibly terminate if necessary?
            connection.setReadTimeout(TIMEOUT);
            connection.setDoInput(true);

            if (objectToPost != null)
                connection.setRequestProperty(CONTENT_TYPE, CONTENT_TYPE_JSON);
            else
                connection.setRequestProperty(CONTENT_TYPE, CONTENT_TYPE_X_WWW_FORM_URLENCODED);

            connection.setRequestMethod(method.toString());
            connection.setRequestProperty(ACCEPT, ACCEPT_JSON);

            if (!gzipEnabled){
                connection.setRequestProperty("Accept-Encoding", "identity");
            }

            if (requestHeaders != null ){
                for (String headerName : this.requestHeaders.keySet()) {
                    connection.setRequestProperty(headerName, this.requestHeaders.get(headerName));
                }
            }

            if (eTagValue != null)
                connection.setRequestProperty(IF_NONE_MATCH, eTagValue);

            if (lastModifiedValue != null){
                connection.setRequestProperty(IF_MODIFIED_SINCE, lastModifiedValue);
            }

            //request

            if (objectToPost != null){
                PrintWriter out = new PrintWriter(connection.getOutputStream());
                try{
                    gson.toJson(objectToPost, typeOfObject, out);
                } catch (JsonSyntaxException e){
                    //parse error, invalid type
                    callback.onJsonParseError("Incorrect type specified for object to post: " + e);
                }
                out.close();
            }

            switch (connection.getResponseCode()){
                case HttpURLConnection.HTTP_NO_CONTENT:
                    callback.onNoContentResponse();
                    break;
                case HttpURLConnection.HTTP_NOT_MODIFIED:
                    callback.onNotModifiedResponse();
                    break;
                case HttpURLConnection.HTTP_NOT_FOUND:
                    callback.onResoureNotFound();
                    break;
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    callback.onUnauthorized();
                    break;
                case HttpURLConnection.HTTP_FORBIDDEN:
                    callback.onForbidden(getErrorBody(connection));
                    break;
                case HttpURLConnection.HTTP_CREATED:
                case HttpURLConnection.HTTP_OK:
                    if (expectedResultType != null){

                        String etag = connection.getHeaderField(ETAG);
                        String lastModified = connection.getHeaderField(LAST_MODIFIED);
                        InputStreamReader reader  = new InputStreamReader( connection.getInputStream() );
                        try{
                            Serializable result = gson.fromJson(reader, expectedResultType);
                            if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED)
                                callback.onCreatedResponse(result, etag, lastModified);
                            else
                                callback.onResponse(result, etag, lastModified);
                        } catch (JsonSyntaxException e){
                            //parse error, invalid type
                            callback.onJsonParseError("Response does not match expected type: " + e);
                        }
                        reader.close();
                    }else {
                        callback.onResponse(null, null, null);
                    }
                    break;
                default:
                    String body = getErrorBody(connection);
                    callback.onUnexpectedResponse(connection.getResponseCode(), connection.getResponseMessage(), body);
                    break;
            }

        }finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    private String getErrorBody(HttpURLConnection connection) throws IOException {
        String body = null;
        InputStream errorStream = connection.getErrorStream();
        if (errorStream != null){
            Scanner scanner = new Scanner(connection.getErrorStream(), "UTF-8").useDelimiter("\\A");
            if (scanner.hasNext())
                body = scanner.next();
        }
        errorStream.close();
        return body;
    }

    public interface RestCallback<T extends Serializable>{

        public void onUnexpectedResponse(int httpCode, String httpMessage, String errorBody);

        public void onNotModifiedResponse();

        public void onNoContentResponse();

        public void onCreatedResponse(T response, String ETag, String lastModified);

        public void onResponse(T response, String ETag, String lastModified);

        public void onJsonParseError(String message);

        public void onResoureNotFound();

        public void onUnauthorized();

        public void onForbidden(String reason);
    }

    protected void configureConnections() {
        // Work around pre-Froyo bugs in HTTP connection reuse.
//        Utils.disableConnectionReuseIfNecessary();
        System.setProperty("http.keepAlive", "false"); //disable reuse anyway, seems buggy on some devices

        System.setProperty("http.maxRedirects", "10");
        HttpURLConnection.setFollowRedirects(true);
        HttpsURLConnection.setFollowRedirects(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            CookieManager cookieManager = new CookieManager();
            CookieHandler.setDefault(cookieManager);
        }
    }


}
