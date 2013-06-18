package com.github.sgdesmet.android.utils.list;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockListFragment;
import java.util.Collections;


/**
 * TODO description
 * <p/>
 * Date: 18/06/13
 * Time: 16:56
 *
 * @author: sgdesmet
 */
public class SimpleSherlockListFragment extends SherlockListFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {

        super.onCreate( savedInstanceState );
        if (getListAdapter() == null)
            setListAdapter( new SimpleListAdapter( Collections.<ListItem>emptyList() ) );
    }


    public SimpleListAdapter getSimpleListAdapter(){
        if (getListAdapter() != null && getListAdapter() instanceof SimpleListAdapter)
            return (SimpleListAdapter) getListAdapter();
        return null;
    }

    @Override
    public void onListItemClick(final ListView l, final View v, final int position, final long id) {

        super.onListItemClick( l, v, position, id );
        if (getSimpleListAdapter() != null && getSimpleListAdapter().getItems() != null)
            getSimpleListAdapter().getItems().get( position ).onClick();
    }
}
