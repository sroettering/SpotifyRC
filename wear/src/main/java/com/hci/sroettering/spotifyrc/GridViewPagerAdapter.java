package com.hci.sroettering.spotifyrc;

import android.content.Context;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hci.sroettering.spotifyrc.adapters.MiddleListViewAdapter;
import com.hci.sroettering.spotifyrc.adapters.RightListViewAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sroettering on 11.05.16.
 */
public class GridViewPagerAdapter extends GridPagerAdapter implements WearableListView.ClickListener {

    private final int PAGE_COUNT = 3;

    private List[] pageData;
    private String[] types = {"playlist", "album", "song", "artist", "category"};
    private int currentList;

    private Context mContext;
    private MainActivity mActivity;
    private GridViewPager mPager;
    private View leftView;
    private WearableListView middleListView;
    private WearableListView rightListView;
    private MiddleListViewAdapter middleListViewAdapter;
    private RightListViewAdapter rightListViewAdapter;
    private TextView titleView;

    public GridViewPagerAdapter(Context context, MainActivity activity, GridViewPager pager) {
        super();
        mContext = context;
        mActivity = activity;
        mPager = pager;
        pageData = new List[5];
        currentList = 0;
        //createDummyData();
    }

    // 0 = playlist; 1 = album; 2 = song; 3 = artist; 4 = category
    public void setData(String[] data, int type) {
        List<RightListDataItem> dataList = new ArrayList<>();
        String[] splitItem;
        if(type == 2) {
            for(int i = 1; i < data.length; i++) {
                splitItem = data[i].split("--");
                RightListDataItem item =
                        new RightListDataItem(splitItem[0], splitItem[1], splitItem[2], 2);
                dataList.add(item);
            }
        } else {
            for(int i = 1; i < data.length; i++) {
                splitItem = data[i].split("--");
                RightListDataItem item =
                        new RightListDataItem(splitItem[0], -1, splitItem[1], type);
                dataList.add(item);
            }
        }
        pageData[type] = dataList;
        //rightListViewAdapter.setData(pageData[currentList]);
        rightListViewAdapter.notifyDataSetChanged();
    }

    private void createDummyData() {
        for(int j = 0; j < 5; j++) {
            List<RightListDataItem> list = new ArrayList<RightListDataItem>();
            for (int i = 0; i < j+5; i++) {
                RightListDataItem item = new RightListDataItem("Item " + i, -1, "-1", j);
                list.add(item);
            }
            pageData[j] = list;
        }
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public int getColumnCount(int i) {
        return PAGE_COUNT;
    }

    @Override
    public Object instantiateItem(ViewGroup viewGroup, int row, int col) {
        if(col == 0) {
            LayoutInflater inflater = mActivity.getLayoutInflater();
            leftView = inflater.inflate(R.layout.media_control, viewGroup, false);
            viewGroup.addView(leftView);
            return leftView;
        } else if(col == 1) {
            middleListViewAdapter = new MiddleListViewAdapter(mContext);
            LayoutInflater inflater = mActivity.getLayoutInflater();
            View view = inflater.inflate(R.layout.listview_middle_layout, viewGroup, false);
            middleListView = (WearableListView) view.findViewById(R.id.middleListView);
            middleListView.setAdapter(middleListViewAdapter);
            middleListView.setGreedyTouchMode(true);
            middleListView.setClickListener(this);
            viewGroup.addView(middleListView);
            return middleListView;
        } else if(col == 2) {
            rightListViewAdapter = new RightListViewAdapter(mContext);
            LayoutInflater inflater = mActivity.getLayoutInflater();
            View view = inflater.inflate(R.layout.listview_right_layout, viewGroup, false);
            rightListView = (WearableListView) view.findViewById(R.id.rightListView);
            rightListView.setAdapter(rightListViewAdapter);
            rightListView.setGreedyTouchMode(true);
            rightListView.setClickListener(this);
            rightListViewAdapter.setData(pageData[currentList]);
            titleView = (TextView) view.findViewById(R.id.list_title);
            titleView.setText("Title");
            viewGroup.addView(view);
            return view;
        }
        return null;
    }

    @Override
    public void destroyItem(ViewGroup viewGroup, int i, int i1, Object o) {
        if(o != null) {
            viewGroup.removeView((View) o);
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view.equals(o);
    }


    // Clicklistener for both listviews

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        Integer tag = (Integer) viewHolder.itemView.getTag();
        Log.d("GridViewPagerAdapter", "onClick: " + tag);
        if(viewHolder instanceof MiddleListViewAdapter.ItemViewHolder) {
            // click happened in middle listview
            currentList = tag;
            rightListViewAdapter.setData(pageData[tag]);
            rightListView.smoothScrollToPosition(0);
            titleView.setText(middleListViewAdapter.getTitle(tag));
            mPager.setCurrentItem(0, 2); // scroll to the right
        } else if(viewHolder instanceof RightListViewAdapter.ItemViewHolder) {
            // click happened in right listview
            RightListViewAdapter.ItemViewHolder holder = (RightListViewAdapter.ItemViewHolder) viewHolder;
            int position = (int)holder.itemView.getTag();
            RightListDataItem item = (RightListDataItem) pageData[currentList].get(position);
            CommunicationManager.getInstance().sendPlay(types[item.parentCategory], item.spotifyID);
            mPager.setCurrentItem(0, 0); // scroll to control page
        }
    }

    @Override
    public void onTopEmptyRegionClick() {
        // do nothing
    }
}
