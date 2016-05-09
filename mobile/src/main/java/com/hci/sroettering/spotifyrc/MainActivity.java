package com.hci.sroettering.spotifyrc;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements PagerListFragment.OnFragmentInteractionListener {

    private SpotifyManager mSpotifyManager;
    private ViewPager pager;
    private PagerListFragmentAdapter plfa;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = (ViewPager) findViewById(R.id.pager);

        FragmentManager fragmentManager = getSupportFragmentManager();
        plfa = new PagerListFragmentAdapter(fragmentManager);
        pager.setAdapter(plfa);
        pager.setOffscreenPageLimit(5);

        seekBar = (SeekBar) findViewById(R.id.seekBar_track);
        seekBar.setProgress(0);

        Log.d("MainActivity", "initializing SpotifyManager");
        mSpotifyManager = SpotifyManager.getInstance();
        mSpotifyManager.setContext(this);
        mSpotifyManager.login();
    }

    @Override
    protected void onDestroy() {
        mSpotifyManager.onDestroy();
        super.onDestroy();
    }

    public void updateData(final List data, final int pagerPosition) {
        plfa.updateFragmentListData(data, pagerPosition);
    }

    // Media Control Display

    public void updateCurrentTrackInfo(String artist, String track, int length) {
        TextView artistTV = (TextView)findViewById(R.id.tv_cur_artist);
        artistTV.setText(artist.substring(0, Math.min(20, artist.length())) + " -");
        TextView trackTV = (TextView)findViewById(R.id.tv_cur_track);
        trackTV.setText(track.substring(0, Math.min(100, track.length())));
        TextView durationTV = (TextView)findViewById(R.id.tv_row_duration);
        durationTV.setText(formatMilliseconds(length));

        seekBar.setMax(10000);
        seekBar.setProgress(0);
    }

    public void updateProgress(int position, int duration) {
        int progress = (int)((float)position / duration * 10000);
        TextView positionTV = (TextView)findViewById(R.id.tv_cur_position);
        positionTV.setText(formatMilliseconds(position));
        seekBar.setProgress(progress);
    }

    public void updatePlayButton(boolean isPlaying) {
        ToggleButton playBtn = (ToggleButton) findViewById(R.id.btn_play);
        playBtn.setChecked(isPlaying);
    }

    // Media Control Functionality

    public void onPlayBtnClick(View v) {
        if(((ToggleButton)v).isChecked()) {
            play();
        } else {
            pause();
        }
    }

    public void play() {
        mSpotifyManager.resume();
    }

    public void pause() {
        mSpotifyManager.pause();
    }

    public void nextTrack(View v) {
        mSpotifyManager.nextTrack();
    }

    public void prevTrack(View v) {
        mSpotifyManager.prevTrack();
    }

    public void onShuffleBtnClick(View v) {
        mSpotifyManager.shuffle(((ToggleButton) v).isChecked());
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mSpotifyManager.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public void onListItemSelected(ListView l, View v, int position, long id, int currentPage) {
        switch (currentPage) {
            case 0: mSpotifyManager.loadPlaylist(position);
                break;
            case 1: mSpotifyManager.loadSong(position);
                break;
            case 2: mSpotifyManager.loadAlbum(position);
                break;
            case 3: mSpotifyManager.loadArtist(position);
                break;
            case 4: mSpotifyManager.loadCategory(position);
                break;
        }
    }

    private String formatMilliseconds(int millis) {
        String duration = String.format("%d:%d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
        return duration;
    }
}
