package com.github.sgdesmet.android.utils.service;

import android.util.Log;
import com.google.gson.*;
import com.sun.istack.internal.NotNull;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;
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
public class RestResource {

    protected static final String CONTENT_TYPE_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded; charset=UTF-8";
    protected static final String CONTENT_TYPE_JSON                  = "application/json; charset=UTF-8";

    private static final String ENCODING = "UTF-8";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_BASIC  = "Basic";

    private static final String ACCEPT                    = "Accept";
    private static final String ACCEPT_JSON               = "application/json";
    private static final String CONTENT_TYPE              = "Content-Type";
    private static final String TAG                       = RestResource.class.getSimpleName();
    public static final  int    DEFAULT_TIMEOUT           = 30000;
    public static final  int    DEFAULT_FORCED_DISCONNECT = 60000;
    private static final int    DEFAULT_RETRIES           = 3;

    private static ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor( 2 );


    public enum HttpMethod {
        GET, POST, PUT, DELETE, HEAD, OPTIONS
    }


    private Map<String, String> headers;
    private Map<String, String> query;
    private Gson                gson;
    private String              url;
    private int timeout         = DEFAULT_TIMEOUT;
    private int forceDisconnect = DEFAULT_FORCED_DISCONNECT;

    private int retries = DEFAULT_RETRIES;

    public static RestResource build() {

        return new RestResource();
    }

    protected RestResource() {

        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().generateNonExecutableJson();

        gson = gsonBuilder.create();
        headers = new HashMap<String, String>();
        query = new HashMap<String, String>();
    }

    public RestResource gson(final Gson gson) {

        if (gson != null)
            this.gson = gson;
        return this;
    }

    public RestResource headers(final Map<String, String> headers) {

        if (headers != null)
            this.headers = new HashMap<String, String>( headers );
        return this;
    }

    public RestResource basicAuth(final String username, final String password) {

        String encodedCredentials = new String(
                Base64.encodeBase64( ((username != null? username: "") + ":" + (password != null? password: "")).getBytes() ) );

        headers.put( AUTHORIZATION_HEADER, String.format( "%s %s", AUTHORIZATION_BASIC, encodedCredentials ) );
        return this;
    }

    public RestResource url(final String url) {

        if (url != null)
            this.url = url;
        return this;
    }

    public RestResource query(final Map<String, String> query) {

        if (query != null)
            this.query = new HashMap<String, String>( query );
        return this;
    }

    /**
     * Connection timeout. Default 30s.
     */
    public RestResource timeout(final int timeout) {

        this.timeout = timeout;
        return this;
    }

    /**
     * HttpUrlConnection can hang, causing setConnectTimeout and setReadTimeout to have no effect.
     * This configures a thread which forcibly terminates the connection if it has not completed after the specified time.
     * Default is 60s
     */
    public RestResource forcedDisconnect(final int timeout) {

        this.forceDisconnect = timeout;
        return this;
    }

    /**
     * Number of retries if a connection fails. Default is 3.
     */
    public RestResource retries(final int retries) {

        this.retries = retries;
        return this;
    }

    public void get(Serializable content, Type expectedResultType, Callback callback)
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

        //setup
        for (int attempt = 1; attempt <= retries; attempt++) {
            HttpURLConnection connection = null;
            ScheduledFuture forceDisconnect = null;
            try {
                URL urlWithParams = new URL( url + (url.indexOf( '?' ) < 0? '?': '&') + getParametersString( query ) );
                connection = (HttpURLConnection) urlWithParams.openConnection();
                configureConnection( connection );

                forceDisconnect = scheduleForcedDisconnect( connection );

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
                    //reset disconnect timer
                    if (forceDisconnect != null)
                        forceDisconnect.cancel( true );
                    forceDisconnect = scheduleForcedDisconnect( connection );

                    Serializable json = parseJSON( connection.getInputStream(), expectedResultType );
                    callback.onResponse( json, connection.getResponseCode(), connection.getHeaderFields() );
                } else {
                    //reset disconnect timer
                    if (forceDisconnect != null)
                        forceDisconnect.cancel( true );
                    forceDisconnect = scheduleForcedDisconnect( connection );

                    forceDisconnect = scheduler.schedule( new ForceDisconnect( connection ), timeout * 2, TimeUnit.MILLISECONDS );

                    String errorBody = toString( connection.getErrorStream() );
                    callback.onResponse( errorBody, connection.getResponseCode(), connection.getHeaderFields() );
                }
            }
            catch (JsonParseException e) {
                if (attempt == retries) {
                    //can't retry anymore, throw error upwards
                    Log.e( TAG, "Max retries reached, throwing error" );
                    throw new IOException( e );
                } else {
                    // log message, we'll retry
                    Log.e( TAG, String.format( "Retrying request (%d) after error %s ", attempt, e ) );
                }
            }
            catch (IOException e) {
                if (attempt == retries) {
                    //can't retry anymore, throw error upwards
                    Log.e( TAG, "Max retries reached, throwing error" );
                    throw e;
                } else {
                    // log message, we'll retry
                    Log.e( TAG, String.format( "Retrying request (%d) after error %s ", attempt, e ) );
                }
            }
            finally {
                if (forceDisconnect != null)
                    forceDisconnect.cancel( true );
                if (connection != null)
                    connection.disconnect();
            }
        }
    }

    private ScheduledFuture scheduleForcedDisconnect(@NotNull final HttpURLConnection urlConnection) {

        if (forceDisconnect > 0)
            return scheduler.schedule( new ForceDisconnect( urlConnection ), forceDisconnect, TimeUnit.MILLISECONDS );
        return null;
    }

    private static class ForceDisconnect implements Runnable {

        WeakReference<HttpURLConnection> urlConnectionReference;

        public ForceDisconnect(@NotNull final HttpURLConnection con) {

            this.urlConnectionReference = new WeakReference<HttpURLConnection>( con );
        }

        public void run() {

            if (urlConnectionReference != null && urlConnectionReference.get() != null) {
                Log.e( TAG, "Timer thread forcing parent to quit connection" );
                try {
                    urlConnectionReference.get().disconnect();
                }
                catch (Exception e) {
                    Log.e( TAG, "Error forcibly closing connection: " + e );
                }
                Log.e( TAG, "Timer thread closed connection held by parent, exiting" );
            }
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
