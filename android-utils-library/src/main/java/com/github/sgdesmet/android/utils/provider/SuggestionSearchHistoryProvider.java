package com.github.sgdesmet.android.utils.provider;

import android.content.SearchRecentSuggestionsProvider;

/**
 * TODO description
 * <p/>
 * Date: 06/11/12
 * Time: 11:52
 *
 * @author: sgdesmet
 */
public class SuggestionSearchHistoryProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "com.github.sgdesmet.androidutils.SuggestionSearchHistoryProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    private String authority = AUTHORITY;
    private int mode = MODE;

    public SuggestionSearchHistoryProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    public SuggestionSearchHistoryProvider(String authority, int mode) {
        this.authority = authority;
        this.mode = mode;
        setupSuggestions(authority, mode);
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}