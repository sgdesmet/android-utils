package com.github.sgdesmet.androidutils.list;

import android.content.Context;
import android.view.*;
import android.widget.*;
import com.github.sgdesmet.androidutils.list.items.ViewHolder;
import com.github.sgdesmet.androidutils.service.image.loader.ImageLoader;
import com.github.sgdesmet.androidutils.service.image.loader.ImageLoaderFactory;
import java.util.Collections;
import java.util.List;


/**
* TODO description
* <p/>
* Date: 17/06/13
* Time: 17:11
*
* @author: sgdesmet
*/
public class SimpleListAdapter extends BaseAdapter {

    private List<ListItem> items;
    private Context              applicationContext;

    public SimpleListAdapter(final List<ListItem> items, Context applicationContext) {

        if (items != null)
            this.items = items;
        else
            this.items = Collections.emptyList();
        this.applicationContext = applicationContext;
    }

    public Context getApplicationContext() {

        return applicationContext;
    }

    public void setApplicationContext(final Context applicationContext) {

        this.applicationContext = applicationContext;
    }

    public List<ListItem> getItems() {

        return items;
    }

    public void setItems(final List<ListItem> items) {

        this.items = items;
    }

    @Override
    public int getCount() {

        return items.size();
    }

    @Override
    public Object getItem(final int position) {

        return items.get( position );
    }

    @Override
    public long getItemId(final int position) {

        return position;
    }

    @Override
    public boolean isEnabled(final int position) {

        return !items.get( position ).enabled();
    }

    @Override
    public int getItemViewType(final int position) {

        ListItem ListItem = items.get( position );
        if (!ListItem.enabled())
            return super.getItemViewType( position );    //TODO implement
        else
            return AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
    }

    @Override
    public int getViewTypeCount() {

        return 2;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {

        ListItem item =(ListItem) getItem( position );
        View row = item.inflate( convertView, parent );
        item.configure( row );
        return row;
    }


}
