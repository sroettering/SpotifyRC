package com.hci.sroettering.spotifyrc;

import android.content.Context;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.WearableListView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hci.sroettering.spotifyrc.adapters.MiddleListViewAdapter;

import java.util.List;

/**
 * Created by sroettering on 11.05.16.
 */
public class GridViewPagerAdapter extends GridPagerAdapter {

    private final int PAGE_COUNT = 3;

    private List[] pageData;

    private Context mContext;
    private MainActivity mActivity;
    private WearableListView middleListView;
    private WearableListView rightListView;
    private MiddleListViewAdapter middleListViewAdapter;

    public GridViewPagerAdapter(Context context, MainActivity activity) {
        super();
        mContext = context;
        mActivity = activity;
        pageData = new List[5];
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
        //if(col == 1) {
            middleListViewAdapter = new MiddleListViewAdapter(mContext);
            LayoutInflater inflater = mActivity.getLayoutInflater();
            View view = inflater.inflate(R.layout.listview_layout, viewGroup, false);
            middleListView = (WearableListView) view.findViewById(R.id.middleListView);
            middleListView.setAdapter(middleListViewAdapter);
            middleListView.setGreedyTouchMode(true);
            viewGroup.addView(middleListView);
            return middleListView;
        //}
        //return null;
        /*
            ImageView imageView;
            imageView = new ImageView(mContext);
            imageView.setImageResource(carImageIDs[row][col]);
            imageView.setBackgroundColor(Color.rgb(236, 238, 242));
            viewGroup.addView(imageView);
            return imageView;
         */
    }

    @Override
    public void destroyItem(ViewGroup viewGroup, int i, int i1, Object o) {
        viewGroup.removeView((View) o);
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view.equals(o);
    }
}
