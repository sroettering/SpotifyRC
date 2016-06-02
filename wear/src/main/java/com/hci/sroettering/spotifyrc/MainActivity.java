package com.hci.sroettering.spotifyrc;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

import com.hci.sroettering.spotifyrc.wiigee.control.AndroidWiigee;
import com.hci.sroettering.spotifyrc.wiigee.event.GestureEvent;
import com.hci.sroettering.spotifyrc.wiigee.event.GestureListener;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity implements CommunicationManager.MessageListener, IVoiceControl, GestureListener {

    private GridViewPagerAdapter pagerAdapter;
    private GridViewPager pager;
    private CommunicationManager commManager;

    private SpeechRecognizer speechRecognizer;
    private long listeningStartTime;
    private long currentListeningTime;
    private final long listeningTimeMax = Long.MAX_VALUE;//30000; // ms

    private SensorManager mSensorManager;
    private AndroidWiigee aWiigee;

    private static final int TRAIN_BUTTON = 1;
    private static final int CLOSE_GESTURE_BUTTON = 2;
    private static final int RECOGNITION_BUTTON = 3;

    private boolean trainButtonDown;
    private boolean recognitionButtonDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        // UI
        pager = (GridViewPager) findViewById(R.id.pager);
        pagerAdapter = new GridViewPagerAdapter(this, this, pager);
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageCount(2);
        pager.setCurrentItem(0, 2);
        DotsPageIndicator pageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        pageIndicator.setPager(pager);
        pageIndicator.setDotFadeWhenIdle(false);

        // Handheld Communication
        commManager = CommunicationManager.getInstance();
        commManager.setContext(this);
        commManager.addListener(this);
        commManager.sendDataRequest();

        // Voice Control
        VoiceRecognitionListener.getInstance().setListener(this);
        currentListeningTime = 0;
        listeningStartTime = -1;

        // Gesture Control
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        aWiigee = new AndroidWiigee();
        aWiigee.addGestureListener(this);
        aWiigee.setTrainButton(TRAIN_BUTTON);
        aWiigee.setCloseGestureButton(CLOSE_GESTURE_BUTTON);
        aWiigee.setRecognitionButton(RECOGNITION_BUTTON);
        trainButtonDown = false;
        recognitionButtonDown = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!isAmbient()) {
            restartListeningService();
        }
        Log.d("MainAcitivity", "onResume");
        mSensorManager.registerListener(aWiigee.getDevice(),
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        try {
            aWiigee.getDevice().setAccelerationEnabled(true);
        } catch (IOException e) {
            Log.e(getClass().toString(), e.getMessage(), e);
        }
    }

    @Override
    public void onPause() {
        stopListening();
        mSensorManager.unregisterListener(aWiigee.getDevice());
        try {
            aWiigee.getDevice().setAccelerationEnabled(false);
        } catch (Exception e) {
            Log.e(getClass().toString(), e.getMessage(), e);
        }
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
        //commManager.sendVolumeDown();
        fireWiigeeButtonEvent(TRAIN_BUTTON);
    }

    public void onShuffleBtnClicked(View v) {
        //boolean isEnabled = ((ToggleButton) v).isChecked();
        //commManager.sendShuffle(isEnabled);
        fireWiigeeButtonEvent(RECOGNITION_BUTTON);
    }

    public void onVolumeUpBtnClicked(View v) {
        //commManager.sendVolumeUp();
        fireWiigeeButtonEvent(CLOSE_GESTURE_BUTTON);
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
    private void setListeningButtonChecked(boolean checked) {
        ToggleButton listeningBtn = (ToggleButton) findViewById(R.id.btn_listening);
        listeningBtn.setChecked(checked);
    }

    @Override
    public void onListeningError() {
        setListeningButtonChecked(false);
    }

    private void startListening() {
        if(listeningStartTime != -1) {
            currentListeningTime = System.currentTimeMillis() - listeningStartTime;
        } else {
            listeningStartTime = System.currentTimeMillis();
        }
        //Log.d("MainAcitivity", "listeningStartTime: " + listeningStartTime + "; currentListeningTime: " + currentListeningTime);
//        if(currentListeningTime >= listeningTimeMax) {
//            listeningStartTime = -1;
//            currentListeningTime = 0;
//            return;
//        }

        try {
            initSpeech();
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplication().getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            speechRecognizer.startListening(intent);
            setListeningButtonChecked(true);
        } catch(Exception ex) {
            Log.d("MainActivity", "Bei der SpeechRecognizer Initialisierung ist ein Fehler aufgetreten");
        }
    }

    @Override
    public void stopListening() {
        if (speechRecognizer != null) {
            Log.d("VRListener", "stop listening");
            setListeningButtonChecked(false);
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
        }
        speechRecognizer = null;
    }

    private void initSpeech() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
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
        Log.d("VRListener", "Recorded string: " + s);
        if(s.contains("bitte")) { // politeness keyword
            commManager.sendTextCommand(s);
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
        if(!isAmbient()) {
            startListening();
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

    // AndroidWiigee Gesture Recognition

    @Override
    public void gestureReceived(GestureEvent event) {
        Log.d("AndroidWiigee", "received event: " + event.toString());
    }

    public void fireWiigeeButtonEvent(int buttonType) {
        switch (buttonType) {
            case TRAIN_BUTTON:
                trainButtonDown = !trainButtonDown;
                Log.d("MainActivity", "trainButtonDown: " + trainButtonDown);
                if(trainButtonDown) aWiigee.getDevice().fireButtonPressedEvent(buttonType);
                else aWiigee.getDevice().fireButtonReleasedEvent(buttonType);
                break;
            case RECOGNITION_BUTTON:
                recognitionButtonDown = !recognitionButtonDown;
                if(recognitionButtonDown) aWiigee.getDevice().fireButtonPressedEvent(buttonType);
                else aWiigee.getDevice().fireButtonReleasedEvent(buttonType);
                break;
            case CLOSE_GESTURE_BUTTON:
                aWiigee.getDevice().fireButtonPressedEvent(buttonType);
                break;
        }
    }
}
