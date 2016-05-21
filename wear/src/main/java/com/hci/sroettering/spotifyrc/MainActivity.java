package com.hci.sroettering.spotifyrc;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity implements CommunicationManager.MessageListener {

    private GridViewPagerAdapter pagerAdapter;
    private GridViewPager pager;
    private CommunicationManager commManager;

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

        commManager = CommunicationManager.getInstance();
        commManager.setContext(this);
        commManager.addListener(this);
        commManager.sendDataRequest();
    }

    @Override
    public void onStop() {
        commManager.onStop();
        super.onStop();
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
        commManager.sendPrev();
    }

    public void onPlayBtnClicked(View v) {
        ToggleButton btn = (ToggleButton) v;
        if(btn.isChecked()) {
            commManager.sendResume();
        } else {
            commManager.sendPause();
        }
    }

    public void onNextBtnClicked(View v) {
        commManager.sendNext();
    }

    public void onVolumeDownBtnClicked(View v) {
        commManager.sendVolumeDown();
    }

    public void onShuffleBtnClicked(View v) {
        boolean isEnabled = ((ToggleButton) v).isChecked();
        commManager.sendShuffle(isEnabled);
    }

    public void onVolumeUpBtnClicked(View v) {
        commManager.sendVolumeUp();
    }


    // MessageListener

    @Override
    public void onUpdateMessage(String msg) {
        String[] splitMsg = msg.split(";");
        if(splitMsg[0].equals("play")) {
            TextView infoTV = (TextView) findViewById(R.id.ctrl_info_tv);
            infoTV.setText(splitMsg[1]);
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_play_pause);
            btn.setChecked(true);
        } else if(splitMsg[0].equals("shuffle")) {
            boolean isEnabled = splitMsg[1].equals("1");
            ToggleButton btn = (ToggleButton) findViewById(R.id.ctrl_shuffle);
            btn.setChecked(isEnabled);
        } else if(splitMsg[0].equals("pause")) {
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_play_pause);
            btn.setChecked(false);
        } else if(splitMsg[0].equals("resume")) {
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_play_pause);
            btn.setChecked(true);
        }
    }

    @Override
    public void onDataMessage(String msg) {
        String[] splitMsg = msg.split(";");
        if(splitMsg[0].equals("playlist")) {
            pagerAdapter.setData(splitMsg, 0);
        } else if(splitMsg[0].equals("album")) {
            pagerAdapter.setData(splitMsg, 1);
        } else if(splitMsg[0].equals("song")) {
            pagerAdapter.setData(splitMsg, 2);
        } else if(splitMsg[0].equals("artist")) {
            pagerAdapter.setData(splitMsg, 3);
        } else if(splitMsg[0].equals("category")) {
            pagerAdapter.setData(splitMsg, 4);
        }
    }


    // Util methods

    public static String formatMilliseconds(int millis) {
        String duration = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
        return duration;
    }
}
