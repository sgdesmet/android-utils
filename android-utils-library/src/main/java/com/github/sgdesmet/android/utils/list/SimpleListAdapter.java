package com.github.sgdesmet.android.utils.list;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import java.util.List;
import org.jetbrains.annotations.NotNull;


/**
 * TODO description
 * <p/>
 * Date: 17/06/13
 * Time: 17:11
 *
 * @author: sgdesmet
 */
public class SimpleListAdapter extends ArrayAdapter<ListItem> {

    private final List<Class<? extends ListItem>> viewTypes;

    public SimpleListAdapter(final Context context, final List<Class<? extends ListItem>> viewTypes, @NotNull final List<ListItem> objects) {

        super( context, -1, objects );

        if (viewTypes == null || viewTypes.size() == 0) {
            throw new IllegalArgumentException( "ViewTypes must contain at least one type" );
        }
        this.viewTypes = viewTypes;
    }

    public List<Class<? extends ListItem>> getViewTypes() {

        return viewTypes;
    }

    @Override
    public boolean isEnabled(final int position) {

        return getItem( position ).enabled();
    }

    @Override
    public int getItemViewType(final int position) {

        return getViewTypes().indexOf( getItem( position ).getClass() );
    }

    @Override
    public int getViewTypeCount() {

        return getViewTypes().size();
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {

        ListItem item = getItem( position );
        View row = item.inflate( getContext(), convertView, parent );
        item.configure( row );
        return row;
    }
}
