package com.hci.sroettering.spotifyrc.adapters;

import android.content.Context;
import android.graphics.Bitmap;
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

import kaaes.spotify.webapi.android.models.SavedAlbum;

/**
 * Created by sroettering on 22.04.16.
 */
public class AlbumsListAdapter extends ArrayAdapter<SavedAlbum> {

    private LayoutInflater inflater;
    private List<SavedAlbum> data;
    private HashMap<Integer, Bitmap> bitmaps;

    public AlbumsListAdapter(Context context, List<SavedAlbum> data) {
        super(context, -1, data);
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.data = data;
        this.bitmaps = new HashMap<Integer, Bitmap>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = inflater.inflate(R.layout.playlist_row, parent, false);
        TextView tv = (TextView) rowView.findViewById(R.id.tv_row_name);
        ImageView iv = (ImageView) rowView.findViewById(R.id.iv_row);
        tv.setText(data.get(position).album.name);
        if(data.get(position).album.images != null && data.get(position).album.images.size() != 0) {
            String imageUrl = data.get(position).album.images.get(0).url;
            if (bitmaps.containsKey(position)) {
                iv.setImageBitmap(bitmaps.get(position));
            } else {
                new DownloadImageTask(iv, bitmaps, position).execute(imageUrl);
            }
        }
        return rowView;
    }

    @Override
    public SavedAlbum getItem(int position) {
        if(data != null) {
            return data.get(position);
        }
        return null;
    }

    @Override
    public int getCount() {
        return this.data.size();
    }

    public void updateData(List<SavedAlbum> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

}
