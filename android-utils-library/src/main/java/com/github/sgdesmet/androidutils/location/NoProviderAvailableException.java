package com.github.sgdesmet.androidutils.location;

/**
 * TODO description
 * <p/>
 * Date: 30/01/13
 * Time: 14:21
 *
 * @author: sgdesmet
 */
public class NoProviderAvailableException extends Exception {
    public NoProviderAvailableException() {
    }

    public NoProviderAvailableException(String detailMessage) {
        super(detailMessage);
    }

    public NoProviderAvailableException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public NoProviderAvailableException(Throwable throwable) {
        super(throwable);
    }
}
