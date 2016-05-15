package com.hci.sroettering.spotifyrc;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.view.View;

import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity {

    private GridViewPagerAdapter pagerAdapter;
    private GridViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        pager = (GridViewPager) findViewById(R.id.pager);
        pagerAdapter = new GridViewPagerAdapter(this, this, pager);
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageCount(2);
        pager.setCurrentItem(0, 2);
        DotsPageIndicator pageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        pageIndicator.setPager(pager);
        pageIndicator.setDotFadeWhenIdle(false);
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

    // onClick Methods

    public void onPrevBtnClicked(View v) {

    }

    public void onPlayBtnClicked(View v) {

    }

    public void onNextBtnClicked(View v) {

    }

    public void onVolumeDownBtnClicked(View v) {

    }

    public void onShuffleBtnClicked(View v) {

    }

    public void onVolumeUpBtnClicked(View v) {

    }

    public static String formatMilliseconds(int millis) {
        String duration = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
        return duration;
    }

}
