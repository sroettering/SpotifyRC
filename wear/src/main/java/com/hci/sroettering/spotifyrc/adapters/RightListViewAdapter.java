package com.hci.sroettering.spotifyrc.adapters;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hci.sroettering.spotifyrc.R;
import com.hci.sroettering.spotifyrc.RightListDataItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sroettering on 14.05.16.
 */
public class RightListViewAdapter extends WearableListView.Adapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private List<RightListDataItem> data;

    public RightListViewAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        data = new ArrayList<RightListDataItem>();
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate our custom layout for list items
        //Log.d("MiddleListViewAdapter", "onCreateViewHolder");
        return new ItemViewHolder(mInflater.inflate(R.layout.listviewitem_right_layout, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        // retrieve the text view
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        TextView text = itemHolder.textTV;
        TextView dur = itemHolder.durationTV;

        // replace text contents
        if(data != null) {
            text.setText(data.get(position).text);
            dur.setText(data.get(position).duration);
        }

        // replace list item's metadata
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        if(data != null) {
            return data.size();
        }
        return 0;
    }

    public void setData(List<RightListDataItem> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }



    // Provide a reference to the type of views you're using
    public static class ItemViewHolder extends WearableListView.ViewHolder {

        private TextView textTV;
        private TextView durationTV;

        public ItemViewHolder(View itemView) {
            super(itemView);
            // find the text view within the custom item's layout
            textTV = (TextView) itemView.findViewById(R.id.title);
            durationTV = (TextView) itemView.findViewById(R.id.duration);
        }
    }


}
