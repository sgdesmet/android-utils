package com.github.sgdesmet.android.utils.list.items;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.github.sgdesmet.android.utils.list.ListItem;


/**
 * Simple wrapper to have custom views in listview rows. Important: does *NOT* recycle views!
 * <p/>
 * Date: 19/06/13
 * Time: 13:35
 *
 * @author: sgdesmet
 */
public abstract class ViewWrapperItem implements ListItem {

    @Override
    public void onClick() {

    }

    @Override
    public View inflate(final View convertView, final ViewGroup parent) {

        return getView(parent);
    }

    public abstract View getView(final ViewGroup parent);

    @Override
    public void configure(final View inflatedView) {
        //noop
    }

    @Override
    public int viewType() {

        return AdapterView.ITEM_VIEW_TYPE_IGNORE;
    }

    @Override
    public boolean enabled() {

        return true;
    }
}
