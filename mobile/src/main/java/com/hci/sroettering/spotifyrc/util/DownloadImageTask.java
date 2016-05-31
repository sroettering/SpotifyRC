package com.hci.sroettering.spotifyrc.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by sroettering on 22.04.16.
 */
public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

    ImageView imageView;
    HashMap<Integer, Bitmap> bitmaps;
    int position;

    public DownloadImageTask(ImageView imageView, HashMap<Integer, Bitmap> bitmaps, int pos) {
        this.imageView = imageView;
        this.bitmaps = bitmaps;
        this.position = pos;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mIcon11.compress(Bitmap.CompressFormat.JPEG, 100, out);
            mIcon11 = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));
            out.close();
            in.close();
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        bitmaps.put(position, result);
        imageView.setImageBitmap(result);
    }

}
