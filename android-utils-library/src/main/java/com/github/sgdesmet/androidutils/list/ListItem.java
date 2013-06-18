package com.github.sgdesmet.androidutils.list;

import android.view.View;
import android.view.ViewGroup;


/**
 * TODO description
 * <p/>
 * Date: 18/06/13
 * Time: 09:47
 *
 * @author: sgdesmet
 */
public interface ListItem {

    void onClick();

    View inflate(final View convertView, final ViewGroup parent);

    void configure(View inflatedView);

    /**
     * See {@code getItemViewType()} in {@code android.widget.BaseAdapter}.
     * @return
     */
    int viewType();

    boolean enabled();
}
