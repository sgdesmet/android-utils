package com.github.sgdesmet.android.utils.list.items;

import android.content.Context;
import android.view.*;
import android.widget.*;
import com.github.sgdesmet.android.utils.R;
import com.github.sgdesmet.android.utils.list.ListItem;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;


/**
 * TODO description
 * <p/>
 * Date: 17/06/13
 * Time: 17:12
 *
 * @author: sgdesmet
 */
public class SimpleCheckedListItem extends SimpleListItem {

    public static final int ITEM_VIEW_TYPE = 3;

    boolean checked;

    protected View.OnClickListener checkBoxListener;

    protected CompoundButton.OnCheckedChangeListener checkedChangeListener;

    public SimpleCheckedListItem(final String imageUrl, final String title, final String description, boolean checked,
                                 final View.OnClickListener onClickListener, final View.OnClickListener checkBoxListener) {

        super(imageUrl, title, description, onClickListener );
        this.checked = checked;
        this.checkBoxListener = checkBoxListener;
    }

    public CompoundButton.OnCheckedChangeListener getCheckedChangeListener() {

        if (checkedChangeListener == null){
            checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                    if (SimpleCheckedListItem.this.checked != isChecked)
                        SimpleCheckedListItem.this.checked = isChecked;
                }
            };
        }
        return checkedChangeListener;
    }

    public View.OnClickListener getCheckBoxListener() {

        return checkBoxListener;
    }

    public void setCheckBoxListener(final View.OnClickListener checkBoxListener) {

        this.checkBoxListener = checkBoxListener;
    }

    public boolean isChecked() {

        return checked;
    }

    public void setChecked(final boolean checked) {

        this.checked = checked;
    }

    @Override
    public View inflate(final Context context, final View convertView, final ViewGroup parent) {

        ViewHolder holder;
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from( context );
            holder = new ViewHolder();
            rowView = inflater.inflate( R.layout.simple_row_text_image_checkbox, null );
            holder.imageView = (ImageView) rowView.findViewById( R.id.utils_row_image );
            holder.title = (TextView) rowView.findViewById( R.id.utils_row_text );
            holder.description = (TextView) rowView.findViewById( R.id.utils_row_description );
            holder.checkBox = (CheckBox) rowView.findViewById( R.id.utils_row_checkbox );
            holder.checkBox.setFocusable( false );
            rowView.setTag( holder );
        }
        return rowView;
    }

    @Override
    public void configure(final View inflatedView) {

        if (inflatedView != null) {
            ViewHolder holder = (ViewHolder) inflatedView.getTag();

            holder.checkBox.setOnCheckedChangeListener( getCheckedChangeListener() );

            if (!this.equals( holder.content )) {
                if (imageUrl != null){
                    DisplayImageOptions imageOptions = new DisplayImageOptions.Builder().cacheInMemory( true )
                                                                                        .cacheOnDisc( true )
                                                                                        .resetViewBeforeLoading( true )
                                                                                        .build();
                    ImageLoader.getInstance()
                               .displayImage( imageUrl, holder.imageView, imageOptions );

                }else
                    holder.imageView.setVisibility( View.GONE );
                if (title != null)
                    holder.title.setText( title );
                if (description != null)
                    holder.description.setText( description );
                if (checked != holder.checkBox.isChecked())
                    holder.checkBox.setChecked( checked );
                if (checkBoxListener != null){
                    holder.checkBox.setOnClickListener( checkBoxListener );
                }
                holder.content = this;
            }
        }
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals( o )) {
            return false;
        }

        SimpleCheckedListItem that = (SimpleCheckedListItem) o;

        if (checked != that.checked) {
            return false;
        }
        if (checkBoxListener != null? !checkBoxListener.equals( that.checkBoxListener ): that.checkBoxListener != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {

        int result = super.hashCode();
        result = 31 * result + (checked? 1: 0);
        result = 31 * result + (checkBoxListener != null? checkBoxListener.hashCode(): 0);
        return result;
    }

    public static ListItem item(final String imageUrl, final String title, final String description, boolean checked,
                                      final View.OnClickListener onClickListener, final View.OnClickListener checkBoxListener) {

        return new SimpleCheckedListItem(  imageUrl, title, description, checked, onClickListener, checkBoxListener );
    }
}
