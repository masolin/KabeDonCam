package com.google.android.gms.samples.vision.face.facetracker;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by prada on 15/1/17.
 */
public class BitmapUtils {

    public static int sampleSize(int width, int reqWidth) {
        int sampleSize = 1;

        if (width > 0 && reqWidth > 0) {
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power
            // of 2 and keeps both height and width larger than the
            // requested height and width.
            while ((halfWidth / sampleSize) > reqWidth) {
                sampleSize *= 2;
            }
        }

        return sampleSize;
    }

    private static Point getBitmapSize(Context ctx, Uri uri) throws FileNotFoundException {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, option);
        return new Point(option.outWidth, option.outHeight);
    }

    public static Bitmap rotateBitmap(Bitmap src, int rotate) {
        return rotateBitmap(src, rotate, false);
    }

    public static Bitmap flip(Bitmap src) {
        if (src == null) {
            return src;
        }
        int w = src.getWidth();
        int h = src.getHeight();

        Matrix mtx = flip(new Matrix());
        Bitmap dst = Bitmap.createBitmap(src, 0, 0, w, h, mtx, false);
        return dst;
    }

    private static Matrix flip(Matrix input) {
        input.postScale(1.0f, -1.0f);
        return(input);
    }

    public static Bitmap rotateBitmap(Bitmap src, int rotate, boolean shouldFlip) {
        if (rotate == 0 || src == null) {
            return src;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        Matrix mtx = new Matrix();
        mtx.postRotate(rotate);
        if (shouldFlip) {
            mtx = flip(mtx);
        }

        return Bitmap.createBitmap(src, 0, 0, w, h, mtx, false);
    }

    public static int getPhotoOrientaion(Context ctx, Uri uri) throws IOException {
        ExifInterface exif = new ExifInterface(uri.toString());
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                Point size = getBitmapSize(ctx, uri);
                return (size.x > size.y) ? 90 : 0;
        }
    }
}