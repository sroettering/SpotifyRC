package com.hci.sroettering.spotifyrc.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hci.sroettering.spotifyrc.R;
import com.hci.sroettering.spotifyrc.util.DownloadImageTask;

import java.util.HashMap;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;

/**
 * Created by sroettering on 22.04.16.
 */
public class ArtistsListAdapter extends ArrayAdapter<Artist> {

    private final Context context;
    private LayoutInflater inflater;
    private List<Artist> data;
    private HashMap<Integer, Bitmap> bitmaps;

    public ArtistsListAdapter(Context context, List<Artist> data) {
        super(context, -1, data);
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.data = data;
        this.bitmaps = new HashMap<Integer, Bitmap>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = inflater.inflate(R.layout.playlist_row, parent, false);
        TextView tv = (TextView) rowView.findViewById(R.id.tv_row_name);
        ImageView iv = (ImageView) rowView.findViewById(R.id.iv_row);
        tv.setText(data.get(position).name);
        if(data.get(position).images != null && data.get(position).images.size() != 0) {
            String imageUrl = data.get(position).images.get(0).url;
            if (bitmaps.containsKey(position)) {
                iv.setImageBitmap(bitmaps.get(position));
            } else {
                //new DownloadImageTask(iv, bitmaps, position).execute(imageUrl);
            }
        }
        return rowView;
    }

    @Override
    public int getCount() {
        return this.data.size();
    }

    public void updateData(List<Artist> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

}
