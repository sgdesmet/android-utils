package com.github.sgdesmet.android.utils.list.items;

import android.content.Context;
import android.view.*;
import android.widget.*;
import com.github.sgdesmet.android.utils.R;
import com.github.sgdesmet.android.utils.list.ListItem;
import com.github.sgdesmet.android.utils.service.image.loader.ImageLoader;
import com.github.sgdesmet.android.utils.service.image.loader.ImageLoaderFactory;


/**
 * TODO description
 * <p/>
 * Date: 17/06/13
 * Time: 17:12
 *
 * @author: sgdesmet
 */
public class SimpleCheckedListItem implements ListItem {

    String imageUrl;
    String title;
    String description;
    boolean checked;

    Context applicationContext;

    protected View.OnClickListener onClickListener;
    protected View.OnClickListener checkBoxListener;

    public SimpleCheckedListItem(final Context applicationContext, final String imageUrl, final String title, final String description, boolean checked,
                                 final View.OnClickListener onClickListener, final View.OnClickListener checkBoxListener) {

        this.imageUrl = imageUrl;
        this.title = title;
        this.description = description;
        this.checked = checked;
        this.onClickListener = onClickListener;
        this.applicationContext = applicationContext;
        this.checkBoxListener = checkBoxListener;
    }

    public Context getApplicationContext() {

        return applicationContext;
    }

    public void setApplicationContext(final Context applicationContext) {

        this.applicationContext = applicationContext;
    }

    public String getImageUrl() {

        return imageUrl;
    }

    public void setImageUrl(final String imageUrl) {

        this.imageUrl = imageUrl;
    }

    public String getTitle() {

        return title;
    }

    public void setTitle(final String title) {

        this.title = title;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(final String description) {

        this.description = description;
    }

    public View.OnClickListener getOnClickListener() {

        return onClickListener;
    }

    public void setOnClickListener(final View.OnClickListener onClickListener) {

        this.onClickListener = onClickListener;
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
    public void onClick() {

        if (onClickListener != null) {
            onClickListener.onClick( null );
        }
    }

    @Override
    public View inflate(final View convertView, final ViewGroup parent) {

        ViewHolder holder;
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) applicationContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            holder = new ViewHolder();
            rowView = inflater.inflate( R.layout.simple_row_text_image_checkbox, null );
            holder.imageView = (ImageView) rowView.findViewById( R.id.utils_row_image );
            holder.title = (TextView) rowView.findViewById( R.id.utils_row_text );
            holder.description = (TextView) rowView.findViewById( R.id.utils_row_description );
            holder.checkBox = (CheckBox) rowView.findViewById( R.id.utils_row_checkbox );
            holder.checkBox.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                    if (SimpleCheckedListItem.this.checked != isChecked)
                        SimpleCheckedListItem.this.checked = isChecked;
                }
            } );
            holder.checkBox.setFocusable( false );
            rowView.setTag( holder );
        }
        return rowView;
    }

    @Override
    public void configure(final View inflatedView) {

        if (inflatedView != null) {
            ViewHolder holder = (ViewHolder) inflatedView.getTag();
            if (!this.equals( holder.content )) {
                if (imageUrl != null)
                    ImageLoaderFactory.get()
                                      .loadImage( imageUrl, holder.imageView, ImageLoader.KEEP_CURRENT, ImageLoader.KEEP_CURRENT );
                else
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
    public int viewType() {

        return R.id.utils_row_simple_checkbox;
    }

    @Override
    public boolean enabled() {

        return true;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SimpleCheckedListItem that = (SimpleCheckedListItem) o;

        if (applicationContext != null? !applicationContext.equals( that.applicationContext ): that.applicationContext != null) {
            return false;
        }
        if (description != null? !description.equals( that.description ): that.description != null) {
            return false;
        }
        if (imageUrl != null? !imageUrl.equals( that.imageUrl ): that.imageUrl != null) {
            return false;
        }
        if (onClickListener != null? !onClickListener.equals( that.onClickListener ): that.onClickListener != null) {
            return false;
        }
        if (title != null? !title.equals( that.title ): that.title != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {

        int result = imageUrl != null? imageUrl.hashCode(): 0;
        result = 31 * result + (title != null? title.hashCode(): 0);
        result = 31 * result + (description != null? description.hashCode(): 0);
        result = 31 * result + (applicationContext != null? applicationContext.hashCode(): 0);
        result = 31 * result + (onClickListener != null? onClickListener.hashCode(): 0);
        return result;
    }

    public static ListItem item(final Context applicationContext, final String imageUrl, final String title, final String description, boolean checked,
                                      final View.OnClickListener onClickListener, final View.OnClickListener checkBoxListener) {

        return new SimpleCheckedListItem( applicationContext, imageUrl, title, description, checked, onClickListener, checkBoxListener );
    }
}
