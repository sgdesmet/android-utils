package com.github.sgdesmet.android.utils.list;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.*;


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

        return getItems().size();
    }

    @Override
    public Object getItem(final int position) {

        return getItems().get( position );
    }

    @Override
    public long getItemId(final int position) {

        return position;
    }

    @Override
    public boolean isEnabled(final int position) {

        return getItems().get( position ).enabled();
    }

    @Override
    public int getItemViewType(final int position) {

        ListItem listItem = getItems().get( position );
        return listItem.viewType();
    }

    @Override
    public int getViewTypeCount() {

        Set<Integer> types = new HashSet<Integer>(  );
        for (ListItem item : getItems()){
            types.add( item.viewType() );
        }
        return types.size();
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {

        ListItem item =(ListItem) getItem( position );
        View row = item.inflate( convertView, parent );
        item.configure( row );
        return row;
    }


}
