package com.hci.sroettering.spotifyrc;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity {

    private GridViewPagerAdapter pagerAdapter;
    private GridViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        pagerAdapter = new GridViewPagerAdapter(this, this);
        pager = (GridViewPager) findViewById(R.id.pager);
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageCount(2);
        DotsPageIndicator pageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        pageIndicator.setPager(pager);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            // TODO
        } else {
            // TODO
        }
    }
}
