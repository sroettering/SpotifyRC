package com.hci.sroettering.spotifyrc.adapters;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hci.sroettering.spotifyrc.R;

import java.util.List;

/**
 * Created by sroettering on 13.05.16.
 */
public class MiddleListViewAdapter extends WearableListView.Adapter {

    private String[] titles = {"Playlists", "Albums", "Songs", "Artists", "Categories"};

    private Context mContext;
    private LayoutInflater mInflater;

    public MiddleListViewAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate our custom layout for list items
        Log.d("MiddleListViewAdapter", "onCreateViewHolder");
        return new ItemViewHolder(mInflater.inflate(R.layout.listviewitem_middle_layout, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        // retrieve the text view
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        TextView view = itemHolder.textView;

        // replace text contents
        view.setText(titles[position]);

        // replace list item's metadata
        holder.itemView.setTag(position);

    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    // Provide a reference to the type of views you're using
    public static class ItemViewHolder extends WearableListView.ViewHolder {

        private TextView textView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            // find the text view within the custom item's layout
            textView = (TextView) itemView.findViewById(R.id.title);
        }
    }

}
