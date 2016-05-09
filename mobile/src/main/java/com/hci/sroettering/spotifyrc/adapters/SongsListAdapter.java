package com.hci.sroettering.spotifyrc.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.hci.sroettering.spotifyrc.R;

import org.w3c.dom.Text;

import java.util.List;
import java.util.concurrent.TimeUnit;

import kaaes.spotify.webapi.android.models.SavedTrack;

/**
 * Created by sroettering on 22.04.16.
 */
public class SongsListAdapter extends ArrayAdapter<SavedTrack> {

    private final Context context;
    private LayoutInflater inflater;
    private List<SavedTrack> data;

    public SongsListAdapter(Context context, List<SavedTrack> data) {
        super(context, -1, data);
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = inflater.inflate(R.layout.tracks_row, parent, false);
        TextView tv_song = (TextView) rowView.findViewById(R.id.tv_row_song);
        TextView tv_artist = (TextView) rowView.findViewById(R.id.tv_row_artist);
        TextView tv_duration = (TextView) rowView.findViewById(R.id.tv_row_duration);
        tv_song.setText(data.get(position).track.name);
        tv_artist.setText(data.get(position).track.artists.get(0).name);

        long millis = data.get(position).track.duration_ms;
        String duration = String.format("%d:%d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
        tv_duration.setText(duration);

        return rowView;
    }

    @Override
    public int getCount() {
        return this.data.size();
    }

    public void updateData(List<SavedTrack> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

}
