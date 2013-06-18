package com.github.sgdesmet.android.utils.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * TODO description
 * <p/>
 * Date: 07/09/12
 * Time: 14:41
 *
 * @author: sgdesmet
 */
public class RoundedImageView extends ImageView {


    private float radius = 8;
//    private int borderColor = 0xfffece00;
    private int borderColor = 0xff000000;
    private float borderWidth = 0.0f;

    public RoundedImageView(Context context, float radius) {
        super(context);
        this.radius = radius;
    }

    public RoundedImageView(Context context, AttributeSet attrs, float radius) {
        super(context, attrs);
        this.radius = radius;
    }

    public RoundedImageView(Context context, AttributeSet attrs, int defStyle, float radius) {
        super(context, attrs, defStyle);
        this.radius = radius;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(float borderWidth) {
        this.borderWidth = borderWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //set clipping region to get rounded corners
        Path clipPath = new Path();
        int w = this.getWidth();
        int h = this.getHeight();
        clipPath.addRoundRect(new RectF(0, 0, w , h), radius, radius, Path.Direction.CW);
        canvas.clipPath(clipPath);

        //draw content with rounded borders
        super.onDraw(canvas);

        //draw a border
        final RectF rectF = new RectF(0 + borderWidth/2, 0 + borderWidth/2, w - borderWidth/2, h - borderWidth/2);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(borderColor);
        paint.setStrokeWidth(borderWidth);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(rectF, radius, radius, paint);
    }
}
