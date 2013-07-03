package com.github.sgdesmet.android.utils.list;

import android.view.View;
import android.view.ViewGroup;
import java.io.Serializable;


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

    boolean enabled();

    Serializable getTag();
}
