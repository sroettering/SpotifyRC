package com.hci.sroettering.spotifyrc;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity implements CommunicationManager.MessageListener, IVoiceControl {

    private GridViewPagerAdapter pagerAdapter;
    private GridViewPager pager;
    private CommunicationManager commManager;

    private SpeechRecognizer speechRecognizer;
    private KeywordDictionary keywordDictionary;

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

        VoiceRecognitionListener.getInstance().setListener(this);
        //startListening();
        keywordDictionary = new KeywordDictionary();
    }

    @Override
    public void onPause() {
        stopListening();
        super.onPause();
    }

    @Override
    public void onStop() {
        commManager.onStop();
        stopListening();
        super.onStop();
    }

    @Override
    public void finish() {
        stopListening();
        super.finish();
    }

    @Override
    public void onDestroy() {
        stopListening();
        super.onDestroy();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
        stopListening();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        restartListeningService();
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            Log.d("MainActivity", "isAmbient() : true");
            BoxInsetLayout layout = (BoxInsetLayout) findViewById(R.id.parent_layout);
            layout.setBackgroundColor(getResources().getColor(R.color.black));
        } else {
            Log.d("MainActivity", "isAmbient() : false");
            BoxInsetLayout layout = (BoxInsetLayout) findViewById(R.id.parent_layout);
            layout.setBackgroundColor(getResources().getColor(R.color.primary));
        }
    }

    public void onDataChanged() {
        List[] data = pagerAdapter.getData();
        if(data != null) {
            keywordDictionary.setSpotifyData(data);
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


    // Voice Recognition Code
    private void startListening() {
        try {
            initSpeech();
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplication().getPackageName());
            speechRecognizer.startListening(intent);
        } catch(Exception ex) {
            Log.d("MainActivity", "Bei der SpeechRecognizer Initialisierung ist ein Fehler aufgetreten");
        }
    }

    private void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
        }
        speechRecognizer = null;
    }

    private void initSpeech() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            if (!SpeechRecognizer.isRecognitionAvailable(getApplicationContext())) {
                Toast.makeText(getApplicationContext(), "Speech Recognition is not available",
                        Toast.LENGTH_LONG).show();
                finish();
            }
            speechRecognizer.setRecognitionListener(VoiceRecognitionListener.getInstance());
        }
    }

    // IVoiceControl
    @Override
    public void processVoiceCommands(String... voiceCommands) {
        String s = voiceCommands[0].toLowerCase();
        if(keywordDictionary.couldBeCommand(s)) {
            String propableCommand = keywordDictionary.getMostPropableCommandForText(s.toLowerCase());
            Log.d("VoiceRecognition", "Recorded: " + s.toLowerCase() + " results in command: " + propableCommand);
        } else {
            Log.d("VoiceRecognition", "Most likely no valid command - does not contain a polite keyword");
        }
        // always restart the listening service at the end if not in ambient mode
        if(!isAmbient()) {
            restartListeningService();
        }
    }

    @Override
    public void restartListeningService() {
        stopListening();
        startListening();
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
