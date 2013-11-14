package com.github.sgdesmet.android.utils.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.Build;
import android.webkit.MimeTypeMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * TODO description
 * <p/>
 * Date: 12/02/13
 * Time: 15:44
 *
 * @author: sgdesmet
 */
public class BitmapUtils {

    /**
     * Get the size in bytes of a bitmap.
     * @param bitmap
     * @return size in bytes
     */
    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        }
        // Pre HC-MR1
        return bitmap.getRowBytes() * bitmap.getHeight();
    }


    /**
     * Warning, expensive operation!
     * @param applicationContext
     * @param originalBitmap
     * @return
     */
    public static Bitmap getBitmapWithShadow(Bitmap originalBitmap, float shadowRadius){
        BlurMaskFilter blurFilter = new BlurMaskFilter(shadowRadius, BlurMaskFilter.Blur.OUTER);
        Paint shadowPaint = new Paint();
        shadowPaint.setMaskFilter(blurFilter);

        int[] offsetXY = new int[2];
        Bitmap shadowImage = originalBitmap.extractAlpha(shadowPaint, offsetXY);
        Bitmap shadowImage32 = shadowImage.copy(Bitmap.Config.ARGB_8888, true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            shadowImage32.setPremultiplied( true );
        }

        Canvas c = new Canvas(shadowImage32);
        c.drawBitmap(originalBitmap, -offsetXY[0], -offsetXY[1], null);

        return shadowImage32;
    }

    public static Bitmap getRoundedCornerBitmap(Context context, Bitmap bitmap, int pixels) {

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final float densityMultiplier = context.getResources().getDisplayMetrics().density;
        final float roundPx = pixels * densityMultiplier;

        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);

        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static Bitmap getThumbnail(Context context, Uri uri, int preferredSize) throws FileNotFoundException, IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;

        int originalSize = (onlyBoundsOptions.outHeight < onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > preferredSize) ? (originalSize / preferredSize) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }

    private static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }

    public static Bitmap.CompressFormat getImageType(Context context, Uri localImageUri){
        ContentResolver cR = context.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String type = mime.getExtensionFromMimeType(cR.getType(localImageUri));
        if ("jpeg".equals(type)){
            return Bitmap.CompressFormat.JPEG;
        } else if ("png".equals(type)){
            return Bitmap.CompressFormat.PNG;
        } else if ("webp".equals(type)){
            return Bitmap.CompressFormat.WEBP;
        } else
            return Bitmap.CompressFormat.JPEG;
    }

    /**
     * Decode image, with considerations for memory usage
     * @param image
     * @return
     */
    public static Bitmap decodeImageMemoryEfficient(byte[] image){
        if (image != null){
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inTempStorage = new byte[16*1024];
            options.inPurgeable = true;
            options.inInputShareable = true;
            return BitmapFactory.decodeByteArray(image, 0, image.length, options);
        }
        return null;
    }

}
