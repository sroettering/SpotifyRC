package com.hci.sroettering.spotifyrc;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sroettering on 20.04.16.
 */
public class PagerListFragmentAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 5;

    private FragmentManager fragmentManager;

    private List[] data;

    public PagerListFragmentAdapter(FragmentManager fm) {
        super(fm);
        fragmentManager = fm;
        data = new List[PAGE_COUNT];
    }

    @Override
    public ListFragment getItem(int position) {
        PagerListFragment plf = PagerListFragment.newInstance(position);
        if(data[position] != null) {
            plf.updateListViewData(data[position]);
        }
        Log.d("PLFA", "getting fragment " + position);

        return plf;
    }

    public void updateFragmentListData(List data, int pagerPosition) {
        this.data[pagerPosition] = data;
        notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object) {
        PagerListFragment plf = (PagerListFragment) object;
        int position = plf.getmCurrentPage();
        if(data[position] != null) {
            plf.updateListViewData(data[position]);
        }
        return super.getItemPosition(object);
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0: return "Playlists";
            case 1: return "Songs";
            case 2: return "Albums";
            case 3: return "Artists";
            case 4: return "Genres";
            default: return "";
        }
    }
}
