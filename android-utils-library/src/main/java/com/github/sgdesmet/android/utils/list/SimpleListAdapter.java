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
    private final List<Class<? extends ListItem>> viewTypes;

    /**
     *
     * @param viewTypes the view types used in this class. cannot be changed at runtime
     * @param items
     */
    public SimpleListAdapter(final List<Class<? extends ListItem>> viewTypes, final List<ListItem> items) {

        if (items != null)
            this.items = items;
        else
            this.items = Collections.emptyList();
        if (viewTypes == null || viewTypes.size() ==0 ){
            throw new IllegalArgumentException("ViewTypes must contain at least one type");
        }
        this.viewTypes = viewTypes;
    }

    public List<ListItem> getItems() {

        return items;
    }

    public void setItems(final List<ListItem> items) {

        this.items = items;
    }

    public List<Class<? extends ListItem>> getViewTypes() {

        return viewTypes;
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
        return getViewTypes().indexOf( listItem.getClass() );
    }

    @Override
    public int getViewTypeCount() {

        return getViewTypes().size();
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {

        ListItem item =(ListItem) getItem( position );
        View row = item.inflate( convertView, parent );
        item.configure( row );
        return row;
    }


}
