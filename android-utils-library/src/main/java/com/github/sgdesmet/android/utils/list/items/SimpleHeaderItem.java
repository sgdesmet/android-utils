package com.github.sgdesmet.android.utils.list.items;

import android.content.Context;
import android.view.*;
import android.widget.TextView;
import com.github.sgdesmet.android.utils.R;
import com.github.sgdesmet.android.utils.list.ListItem;
import java.io.Serializable;


/**
 * TODO description
 * <p/>
 * Date: 18/06/13
 * Time: 11:01
 *
 * @author: sgdesmet
 */
public class SimpleHeaderItem implements ListItem {

    public static final int ITEM_VIEW_TYPE = 1;

    CharSequence title;
    private Serializable tag;

    public SimpleHeaderItem(final CharSequence title) {

        this.title = title;
    }

    public CharSequence getTitle() {

        return title;
    }

    public void setTitle(final CharSequence title) {

        this.title = title;
    }

    @Override
    public void onClick() {
        //noop
    }

    @Override
    public View inflate(final Context context, final View convertView, final ViewGroup parent) {

        View rowView = convertView;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from( context );
            ViewHolder holder = new ViewHolder();
            rowView = inflater.inflate( R.layout.simple_row_header, null );
            holder.title = (TextView) rowView.findViewById( R.id.utils_row_header );
            rowView.setTag( holder );
        }
        return rowView;
    }

    @Override
    public void configure(final View inflatedView) {

        if (inflatedView != null) {
            ViewHolder holder = (ViewHolder) inflatedView.getTag();
            holder.title.setText( title );
        }
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

        return false;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SimpleHeaderItem that = (SimpleHeaderItem) o;

        if (title != null? !title.equals( that.title ): that.title != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {

        return title != null? title.hashCode(): 0;
    }

    public static ListItem header(final CharSequence title) {

        return new SimpleHeaderItem( title );
    }
}
