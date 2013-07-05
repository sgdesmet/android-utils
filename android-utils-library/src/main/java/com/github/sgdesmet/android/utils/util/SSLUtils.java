package com.github.sgdesmet.android.utils.util;

import android.content.Context;
import android.util.Log;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.*;


/**
 * TODO description
 * <p/>
 * Date: 05/07/13
 * Time: 15:03
 *
 * @author: sgdesmet
 */
public class SSLUtils {

    private static final String TAG = SSLUtils.class.getSimpleName();

    /**
     * Set up a system-wide SSLSocketFactory with a custom trust store.
     * If trustStoreName is null or blank, no SSLSocketFactory will be set.
     *
     * @param trustStoreName name of the trust store file (BKS format) residing in the "raw" resources directory. Name must NOT contain the
     *                       file extension.
     */
    public static void initSsl(final Context applicationContext, final String trustStoreName, final String trustStorePass) {

        if (AndroidUtils.stringNotBlank( trustStoreName )) {
            try {
                HttpsURLConnection.setDefaultSSLSocketFactory(
                        createSslContext( loadTrustStore( applicationContext, trustStoreName, trustStorePass ) ).getSocketFactory() );
            }
            catch (GeneralSecurityException e) {
                Log.e( TAG, "Could not set custom SSL factory" + e );
            }
        }
    }

    public static KeyStore loadTrustStore(final Context applicationContext, final String trustStoreName, final String trustStorePass) {

        try {
            KeyStore localTrustStore = KeyStore.getInstance( "BKS" );
            int trustStoreResource = applicationContext.getResources()
                                                       .getIdentifier( trustStoreName, "raw", applicationContext.getPackageName() );
            InputStream in = applicationContext.getResources().openRawResource( trustStoreResource );
            try {
                localTrustStore.load( in, trustStorePass.toCharArray() );
            }
            finally {
                in.close();
            }

            return localTrustStore;
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }

    public static SSLContext createSslContext(final KeyStore trustStore)
            throws GeneralSecurityException {

        TrustManagerFactory tmf = TrustManagerFactory.getInstance( "X509" );
        tmf.init( trustStore );

        KeyManager[] kms = null;
        SSLContext context = SSLContext.getInstance( "TLS" );
        context.init( kms, tmf.getTrustManagers(), null );

        return context;
    }
}
