package com.github.sgdesmet.android.utils.list.items;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.github.sgdesmet.android.utils.list.ListItem;
import java.io.Serializable;


/**
 * Simple wrapper to have custom views in listview rows. Important: does *NOT* recycle views!
 * <p/>
 * Date: 19/06/13
 * Time: 13:35
 *
 * @author: sgdesmet
 */
public abstract class ViewWrapperItem implements ListItem {

    Serializable tag;

    @Override
    public void onClick() {

    }

    @Override
    public View inflate(final View convertView, final ViewGroup parent) {

        return getView(convertView, parent);
    }

    public abstract View getView(final View convertView, final ViewGroup parent);

    @Override
    public void configure(final View inflatedView) {
        //noop
    }

    public void setTag(final Serializable tag) {

        this.tag = tag;
    }

    @Override
    public Serializable getTag() {

        return tag;
    }

    @Override
    public boolean enabled() {

        return true;
    }
}
