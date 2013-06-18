package com.github.sgdesmet.android.utils.service;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


/**
 * Class to make synchronous JSON requests
 * <p/>
 * Date: 20/08/12
 * Time: 16:34
 *
 * @author: sgdesmet
 */
public class HttpResource {

    protected static final String CONTENT_TYPE_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded; charset=UTF-8";
    protected static final String CONTENT_TYPE_JSON                  = "application/json; charset=UTF-8";

    private static final String ENCODING = "UTF-8";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_BASIC  = "Basic";

    private static final String ACCEPT          = "Accept";
    private static final String ACCEPT_JSON     = "application/json";
    private static final String CONTENT_TYPE    = "Content-Type";
    private static final String TAG             = HttpResource.class.getSimpleName();
    public static final int    DEFAULT_TIMEOUT = 30000;


    public enum HttpMethod {
        GET, POST, PUT, DELETE, HEAD, OPTIONS
    }


    private Map<String, String> headers;
    private Map<String, String> query;
    private Gson                gson;
    private String              url;
    private int timeout = DEFAULT_TIMEOUT;

    public static HttpResource build() {

        return new HttpResource();
    }

    protected HttpResource() {

        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().generateNonExecutableJson();

        gson = gsonBuilder.create();
        headers = new HashMap<String, String>();
        query = new HashMap<String, String>();
    }

    public HttpResource gson(final Gson gson) {

        if (gson != null)
            this.gson = gson;
        return this;
    }

    public HttpResource headers(final Map<String, String> headers) {

        if (headers != null)
            this.headers = new HashMap<String, String>( headers );
        return this;
    }

    public HttpResource basicAuth(final String username, final String password) {

        String encodedCredentials = new String(
                Base64.encodeBase64( ((username != null? username: "") + ":" + (password != null? password: "")).getBytes() ) );

        headers.put( AUTHORIZATION_HEADER, String.format( "%s %s", AUTHORIZATION_BASIC, encodedCredentials ) );
        return this;
    }

    public HttpResource url(final String url) {

        if (url != null)
            this.url = url;
        return this;
    }

    public HttpResource query(final Map<String, String> query) {

        if (query != null)
            this.query = new HashMap<String, String>( query );
        return this;
    }

    public HttpResource timeout(int timeout) {

        this.timeout = timeout;
        return this;
    }

    public void get( Serializable content, Type expectedResultType, Callback callback)
            throws IOException {
        execute( HttpMethod.GET, content, expectedResultType, callback );
    }

    public void post(Serializable content, Type expectedResultType, Callback callback)
            throws IOException {
        execute( HttpMethod.POST, content, expectedResultType, callback );
    }

    public void put(Serializable content, Type expectedResultType, Callback callback)
            throws IOException {
        execute( HttpMethod.PUT, content, expectedResultType, callback );
    }

    public void delete(Serializable content, Type expectedResultType, Callback callback)
            throws IOException {
        execute( HttpMethod.DELETE, content, expectedResultType, callback );
    }

    /**
     * Note: this method is synchronous, use of threading is recommended
     */
    public void execute(HttpMethod method, Serializable content, Type expectedResultType, Callback callback)
            throws IOException {
        //TODO: add timeout thread + retry multiple times on IOException
        //setup
        HttpURLConnection connection = null;
        try {
            URL urlWithParams = new URL( url + (url.indexOf( '?' ) < 0? '?': '&') + getParametersString( query ) );
            connection = (HttpURLConnection) urlWithParams.openConnection();
            configureConnection( connection );

            connection.setDoOutput( method != HttpMethod.GET && method != HttpMethod.DELETE );
            connection.setDoInput( true );

            if (content != null)
                connection.setRequestProperty( CONTENT_TYPE, CONTENT_TYPE_JSON );
            else
                connection.setRequestProperty( CONTENT_TYPE, CONTENT_TYPE_X_WWW_FORM_URLENCODED );

            connection.setRequestMethod( method.toString() );
            connection.setRequestProperty( ACCEPT, ACCEPT_JSON );

            //headers can override defaults
            if (headers != null) {
                for (String headerName : this.headers.keySet()) {
                    connection.setRequestProperty( headerName, this.headers.get( headerName ) );
                }
            }

            //perform request
            if (content != null) {
                PrintWriter out = new PrintWriter( connection.getOutputStream() );
                try {
                    gson.toJson( content, out );
                }
                catch (JsonSyntaxException e) {
                    //parse error, invalid type
                    throw new IOException( e );
                }
                out.close();
            }

            //receive response & parse
            if (success( connection )) {
                Serializable json = parseJSON( connection.getInputStream(), expectedResultType );
                callback.onResponse( json, connection.getResponseCode(), connection.getHeaderFields() );
            } else {
                String errorBody = toString( connection.getErrorStream() );
                callback.onResponse( errorBody, connection.getResponseCode(), connection.getHeaderFields() );
            }
        }
        finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    /**
     * Parse json object from to input stream
     *
     * @param type the expected class
     *
     * @return the object, or null if there is none, or Type didn't match
     */
    private Serializable parseJSON(InputStream inputStream, Type type) {

        InputStreamReader reader = new InputStreamReader( inputStream );
        try {
            return gson.fromJson( reader, type );
        }
        catch (JsonSyntaxException e) {
            //parse error, invalid type
            Log.e( TAG, "Response does not match expected type: " + e, e );
            return null;
        }
        finally {
            try {
                reader.close();
            }
            catch (IOException e) {
                Log.e( TAG, "Error closing stream: " + e, e );
            }
        }
    }

    /**
     * Override to set default values for HttpUrlConnection
     */
    protected void configureConnection(HttpURLConnection connection) {

        System.setProperty( "http.keepAlive", "false" ); //disable reuse, seems buggy on some devices
        System.setProperty( "http.maxRedirects", "10" );

        connection.setInstanceFollowRedirects( true );

        connection.setConnectTimeout( timeout ); //TODO may need to add a timer to forcibly terminate if necessary?
        connection.setReadTimeout( timeout );
    }

    // ----------------
    // Utility methods
    // ----------------

    private static String getParametersString(Map<String, String> parameters) {

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (builder.length() > 0)
                builder.append( "&" );
            builder.append( urlEncode( entry.getKey() ) );
            builder.append( "=" );
            if (entry.getValue() != null)
                builder.append( urlEncode( entry.getValue() ) );
        }

        return builder.toString();
    }

    private static String urlEncode(String original) {

        try {
            return URLEncoder.encode( original != null? original: "", ENCODING );
        }
        catch (UnsupportedEncodingException e) {
            return original;
        }
    }

    /**
     * Convert the input stream to a string and return it, or null if there is nothing
     */
    private static String toString(InputStream inputStream)
            throws IOException {

        String body = null;
        if (inputStream != null) {
            try {
                Scanner scanner = new Scanner( inputStream, "UTF-8" ).useDelimiter( "\\A" );
                if (scanner.hasNext())
                    body = scanner.next();
            }
            finally {
                inputStream.close();
            }
        }
        return body;
    }

    /**
     * Returns true if status code returned is not an error
     */
    protected boolean success(final HttpURLConnection connection)
            throws IOException {

        return connection.getResponseCode() >= 200 && connection.getResponseCode() < 300;
    }

    /**
     * Callback for a rest call
     */
    public interface Callback<T extends Serializable> {

        public void onResponse(T data, int status, Map<String, List<String>> headers);

        public void onResponse(String errorData, int status, Map<String, List<String>> headers);
    }
}
