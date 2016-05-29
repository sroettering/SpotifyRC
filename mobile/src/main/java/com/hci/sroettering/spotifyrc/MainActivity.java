package com.hci.sroettering.spotifyrc;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.hci.sroettering.spotifyrc.voicecontrol.VoiceCommandConverter;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements PagerListFragment.OnFragmentInteractionListener, CommunicationManager.MessageListener {

    private SpotifyManager mSpotifyManager;
    private ViewPager pager;
    private PagerListFragmentAdapter plfa;
    private SeekBar seekBar;
    private CommunicationManager commManager;
    private VoiceCommandConverter voiceConverter;

    private String curTrack;
    private String curArtist;

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

        commManager = CommunicationManager.getInstance();
        commManager.setContext(this);
        commManager.addListener(this);
        mSpotifyManager.setCommunicationManager(commManager);

        voiceConverter = new VoiceCommandConverter();

        curArtist = "";
        curTrack = "";
        syncWatchGUI();
    }

    @Override
    protected void onDestroy() {
        mSpotifyManager.onDestroy();
        commManager.onStop();
        super.onDestroy();
    }

    public void updateData(final List data, final int pagerPosition) {
        plfa.updateFragmentListData(data, pagerPosition);

        // when new data arrived the voice command converter has to be updated as well
        // could get data from parameter...
        voiceConverter.setSpotifyData(SpotifyManager.getInstance().getLdc().getSpotifyData());
    }

    public void syncWatchGUI() {
        ToggleButton btnShuffle = (ToggleButton) findViewById(R.id.btn_shuffle);
        ToggleButton btnPlay = (ToggleButton) findViewById(R.id.btn_play);
        commManager.sendGUIState(btnShuffle.isChecked(), btnPlay.isChecked());
        if(!curArtist.equals("") && !curTrack.equals("")) {
            commManager.sendTrackUpdate(curArtist, curTrack);
        }
    }

    // Media Control Display

    public void updateCurrentTrackInfo(String artist, String track, int length) {
        curArtist = artist;
        curTrack = track;

        TextView artistTV = (TextView)findViewById(R.id.tv_cur_artist);
        artistTV.setText(artist.substring(0, Math.min(20, artist.length())) + " -");
        TextView trackTV = (TextView)findViewById(R.id.tv_cur_track);
        trackTV.setText(track.substring(0, Math.min(100, track.length())));
        TextView durationTV = (TextView)findViewById(R.id.tv_cur_duration);
        durationTV.setText(formatMilliseconds(length));

        seekBar.setMax(10000);
        seekBar.setProgress(0);

        commManager.sendTrackUpdate(artist, track);
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
        commManager.sendResume();
    }

    public void pause() {
        mSpotifyManager.pause();
        commManager.sendPause();
    }

    public void nextTrack(View v) {
        //mSpotifyManager.nextTrack();
        String[] testCommands = {
//                "play Gute Laune bitte",
//                "play bitte",
//                "next", "next bitte",
                "play Tiësto secrets bitte",
//                "shuffle bitte", "shuffle aus bitte", "shuffle an bitte",
//                "an bitte", "aus, bitte", "playlist bitte",
//                "ich wünsche mir hardwell bitte"
        };
        for(String s: testCommands) {
            onTextCommandMessage(s);
        }
    }

    public void prevTrack(View v) {
        mSpotifyManager.prevTrack();
    }

    public void onShuffleBtnClick(View v) {
        boolean isEnabled = ((ToggleButton) v).isChecked();
        mSpotifyManager.shuffle(isEnabled);
        commManager.sendShuffle(isEnabled);
    }

    public void volumeUp() {

    }

    public void volumeDown() {

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

    @Override
    public void onCommandMessage(String msg) {
        Log.d("MainActivity", "Got Command Message: " + msg);
        String[] splitMsg = msg.split(";");
        if(msg.equals("dataRequest")) { // watch activity just started
            mSpotifyManager.updateWatchData();
            syncWatchGUI();
        } else if(msg.equals("next")) {
            mSpotifyManager.nextTrack();
        } else if(msg.equals("prev")) {
            mSpotifyManager.prevTrack();
        } else if(msg.equals("pause")) {
            mSpotifyManager.pause();
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_play);
            btn.setChecked(false);
        } else if(msg.equals("resume")) {
            mSpotifyManager.resume();
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_play);
            btn.setChecked(true);
        } else if(msg.equals("volumeUp")) {
            // TODO
        } else if(msg.equals("volumeDown")) {
            // TODO
        } else if(splitMsg[0].equals("shuffle")) {
            mSpotifyManager.shuffle(splitMsg[1].equals("1"));
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_shuffle);
            commManager.sendShuffle(splitMsg[1].equals("1"));
            btn.setChecked(splitMsg[1].equals("1"));
        } else if(splitMsg[0].equals("play")) {
            mSpotifyManager.play(splitMsg[1], splitMsg[2]);
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_play);
            btn.setChecked(true);
        }
    }

    @Override
    public void onTextCommandMessage(String msg) {
        // command is sent from watch if it contains polite keyword, so this will always be true
        if(voiceConverter.couldBeCommand(msg)) {
            String command = voiceConverter.convertCommand(msg);
            Log.d("MainActivity", "Converted Command: " + command);
            //if(!command.equals(""))
                //onCommandMessage(command);
        } else {
            Log.d("MainActivity", "\"" + msg + "\"" + " not recognized as command");
        }
    }

    @Override
    public void onSensorMessage(String msg) {
        // TODO
    }

    @Override
    public void onUpdateMessage(String msg) {
        Log.d("MainActivity", "Got Update Message: " + msg);
        String[] splitMsg = msg.split(";");
        if(splitMsg[0].equals("shuffle")) {
            boolean isEnabled = splitMsg[1].equals("1");
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_shuffle);
            btn.setChecked(isEnabled);
        } else if(splitMsg[0].equals("resume")) {
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_play);
            btn.setChecked(true);
        } else if(splitMsg[0].equals("pause")) {
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_play);
            btn.setChecked(false);
        }
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
