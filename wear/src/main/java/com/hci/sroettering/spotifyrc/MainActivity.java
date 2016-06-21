package com.hci.sroettering.spotifyrc;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.hci.sroettering.spotifyrc.wiigee.control.AndroidWiigee;
import com.hci.sroettering.spotifyrc.wiigee.event.AccelerationEvent;
import com.hci.sroettering.spotifyrc.wiigee.event.AccelerationListener;
import com.hci.sroettering.spotifyrc.wiigee.event.GestureEvent;
import com.hci.sroettering.spotifyrc.wiigee.event.GestureListener;
import com.hci.sroettering.spotifyrc.wiigee.event.GestureTrainedEvent;
import com.hci.sroettering.spotifyrc.wiigee.event.GestureTrainedListener;
import com.hci.sroettering.spotifyrc.wiigee.event.MotionStartEvent;
import com.hci.sroettering.spotifyrc.wiigee.event.MotionStopEvent;
import com.hci.sroettering.spotifyrc.wiigee.logic.TriggeredProcessingUnit;
import com.hci.sroettering.spotifyrc.wiigee.util.FileIO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity implements CommunicationManager.MessageListener,
        IVoiceControl, GestureListener, AccelerationListener, GestureTrainedListener, SensorEventListener {

    private GridViewPagerAdapter pagerAdapter;
    private GridViewPager pager;
    private CommunicationManager commManager;

    private SpeechRecognizer speechRecognizer;
    private ArrayList<String> commandBuffer;
    private boolean isConverting;
    private final long activeTimeMax = 15000; // ms
    private Handler ambientHandler;
    private Runnable ambientRunnable;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private SensorManager mSensorManager;
    private AndroidWiigee aWiigee;

    private HashMap<Integer, GestureCommand> gestureMap;
    private Handler gestureHandler;
    private Runnable gestureRunnable;
    private final long gestureRecognitionStartTime = 2000;
    private final long gestureRecognitionRunTime = 750;
    private boolean isTraining = false;
//    private boolean isCountdown;
    private boolean isRecordingGesture;

    private Vibrator vibrator;
    private long[] gestureWarmupPattern = new long[]{0, 400, 200, 400, 200, 800};

    private static final int MOTION = 0;
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
        commandBuffer = new ArrayList<>();
        isConverting = false;

        // Handler for keeping the screen on for a fixed time
        ambientHandler = new Handler();
        ambientRunnable = new Runnable() {
            @Override
            public void run() {
                setScreenAlwaysOn(false);
            }
        };

        // Acquire WakeLock to keep CPU active
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeUp");
        wakeLock.acquire();

        // Gesture Control
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        aWiigee = new AndroidWiigee();
        //aWiigee.addFilter(new HighPassFilter());
        aWiigee.addGestureListener(this);
        aWiigee.getDevice().addAccelerationListener(this);
        aWiigee.setTrainButton(TRAIN_BUTTON);
        aWiigee.setCloseGestureButton(CLOSE_GESTURE_BUTTON);
        aWiigee.setRecognitionButton(RECOGNITION_BUTTON);
        ((TriggeredProcessingUnit) aWiigee.getDevice().getProcessingUnit()).addGestureTrainedListener(this);
        initGesturesFromFile();
        trainButtonDown = false;
        recognitionButtonDown = false;
//        isCountdown = false;
        isRecordingGesture = false;
        initGestureMap();

        gestureHandler = new Handler();
        gestureRunnable = new Runnable() {
            @Override
            public void run() {

//                if(!isCountdown) return; // user did not hold activation position for the required time

                Log.d("Runnable", "pushing button (outer)");
                if (isTraining) {
                    fireWiigeeButtonEvent(TRAIN_BUTTON);
                } else {
                    fireWiigeeButtonEvent(RECOGNITION_BUTTON);
                }

//                isCountdown = false;

                gestureHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Runnable", "pushing button (inner)");
                        if (isTraining) {
                            fireWiigeeButtonEvent(TRAIN_BUTTON);
                        } else {
                            fireWiigeeButtonEvent(RECOGNITION_BUTTON);
                        }
                        isRecordingGesture = false;
                    }
                }, gestureRecognitionRunTime);
            }
        };

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        setScreenAlwaysOn(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isAmbient()) {
            restartListeningService();
        }
        Log.d("MainAcitivity", "onResume");
        //Log.d("Sensor", "Available Sensors: " + mSensorManager.getSensorList(Sensor.TYPE_ALL).toString());
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);
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
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d("MainAcitivity", "onStop");
        mSensorManager.unregisterListener(this);
        mSensorManager.unregisterListener(aWiigee.getDevice());
        try {
            aWiigee.getDevice().setAccelerationEnabled(false);
        } catch (Exception e) {
            Log.e(getClass().toString(), e.getMessage(), e);
        }
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
        if(wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        Log.d("MainActivity", "Entering ambient mode");
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
        isConverting = false; // reset needed in case no handheld is connected
        restartListeningService();
        updateDisplay();
        setScreenAlwaysOn(true);
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

    private void setScreenAlwaysOn(boolean alwaysOn) {
        if (alwaysOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            ambientHandler.removeCallbacks(ambientRunnable);
            ambientHandler.postDelayed(ambientRunnable, activeTimeMax);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

    }

    // onClick Methods

    public void onPrevBtnClicked(View v) {
        ExperimentLogger.log(new InputEvent("Touch;previous song"));
        prev(v);
    }

    private void prev(View v) {
        commManager.sendPrev();
    }

    public void onPlayBtnClicked(View v) {
        ExperimentLogger.log(new InputEvent("Touch;playbutton"));
        play(v);
    }

    private void play(View v) {
        ToggleButton btn = (ToggleButton) v;
        if (btn.isChecked()) {
            commManager.sendResume();
        } else {
            commManager.sendPause();
        }
    }

    public void onNextBtnClicked(View v) {
        ExperimentLogger.log(new InputEvent("Touch;next song"));
        next(v);
    }

    private void next(View v) {
        commManager.sendNext();
    }

    public void onVolumeDownBtnClicked(View v) {
        if (!isTraining) {
            ExperimentLogger.log(new InputEvent("Touch;volume down"));
            volumeDown(v);
        } else {
            fireWiigeeButtonEvent(TRAIN_BUTTON);
        }
    }

    private void volumeDown(View v) {
        commManager.sendVolumeDown();
    }

    public void onShuffleBtnClicked(View v) {
        if (!isTraining) {
            boolean isEnabled = ((ToggleButton) v).isChecked();
            ExperimentLogger.log(new InputEvent("Touch;shuffle " + isEnabled));
            shuffle(v);
        } else {
            fireWiigeeButtonEvent(RECOGNITION_BUTTON);
        }
    }

    private void shuffle(View v) {
        boolean isEnabled = ((ToggleButton) v).isChecked();
        commManager.sendShuffle(isEnabled);
    }

    public void onVolumeUpBtnClicked(View v) {
        if (!isTraining) {
            ExperimentLogger.log(new InputEvent("Touch;volume up"));
            volumeUp(v);
        } else {
            fireWiigeeButtonEvent(CLOSE_GESTURE_BUTTON);
        }
    }

    private void volumeUp(View v) {
        commManager.sendVolumeUp();
    }


    // MessageListener

    @Override
    public void onUpdateMessage(String msg) {
        String[] splitMsg = msg.split(";");
        if (splitMsg[0].equals("play")) {
            TextView infoTV = (TextView) findViewById(R.id.ctrl_info_tv);
            infoTV.setText(splitMsg[1]);
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_play_pause);
            btn.setChecked(true);
        } else if (splitMsg[0].equals("shuffle")) {
            boolean isEnabled = splitMsg[1].equals("1");
            ToggleButton btn = (ToggleButton) findViewById(R.id.ctrl_shuffle);
            btn.setChecked(isEnabled);
        } else if (splitMsg[0].equals("pause")) {
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_play_pause);
            btn.setChecked(false);
        } else if (splitMsg[0].equals("resume")) {
            ToggleButton btn = (ToggleButton) findViewById(R.id.btn_play_pause);
            btn.setChecked(true);
        }
    }

    @Override
    public void onDataMessage(String msg) {
        String[] splitMsg = msg.split(";");
        if (splitMsg[0].equals("playlist")) {
            pagerAdapter.setData(splitMsg, 0);
        } else if (splitMsg[0].equals("album")) {
            pagerAdapter.setData(splitMsg, 1);
        } else if (splitMsg[0].equals("song")) {
            pagerAdapter.setData(splitMsg, 2);
        } else if (splitMsg[0].equals("artist")) {
            pagerAdapter.setData(splitMsg, 3);
        } else if (splitMsg[0].equals("category")) {
            pagerAdapter.setData(splitMsg, 4);
        }
    }

    @Override
    public void onTextCommandMessage(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
        String logString = "Speech;" + msg;
        ExperimentLogger.log(new InputEvent(logString));
        if (msg.contains("not recognized")) {
            vibrator.vibrate(new long[]{0, 250, 250, 500}, -1);
        } else {
            vibrator.vibrate(500);
        }
//        convertNextVoiceCommand();
    }

    @Override
    public void onGestureNamed(String msg) {
        String[] split = msg.split(";");
        if (split.length == 2) {
            Log.d("MainActivity", "Gesture " + split[0] + " was named: " + split[1]);
            int id = Integer.parseInt(split[0]);
            aWiigee.getDevice().saveGesture(id, split[1]);
        }
    }

    @Override
    public void onExperimentInfo(String msg) {
        String[] split = msg.split(";");
        if(split.length == 2) {
            int subjID = Integer.parseInt(split[0]);
            int scenID = Integer.parseInt(split[1]);
            if(subjID != -1) ExperimentLogger.subjectID = subjID;
            if(scenID != -1) ExperimentLogger.scenarioID = scenID;
            ExperimentLogger.triggerFileLogging(); // log already saved events to file
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
        try {
            initSpeech();
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplication().getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            speechRecognizer.startListening(intent);
            setListeningButtonChecked(true);
        } catch (Exception ex) {
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

    private void convertNextVoiceCommand() {
        if(commandBuffer.size() > 0) {
//            isConverting = true;
            String nextCommand = commandBuffer.remove(0); // retrieves and deletes the first command
            String logString = "Speech;converting: " + nextCommand;
            ExperimentLogger.log(new InputEvent(logString));
            commManager.sendTextCommand(nextCommand);
//            if(commandBuffer.isEmpty()) {
//                isConverting = false;
//            }
        } else {
//            isConverting = false;
        }
    }

    // IVoiceControl
    @Override
    public void processVoiceCommand(String voiceCommand) {
        String s = voiceCommand.toLowerCase();
        Log.d("VRListener", "Recorded string: " + s);
        commandBuffer.add(s); // adds the new command to the end of the list
        /*if(!isConverting)*/ convertNextVoiceCommand(); // start converting the commands on the handheld

        // always restart the listening service at the end if not in ambient mode
        if (!isAmbient()) {
            restartListeningService();
        }
    }

    @Override
    public void restartListeningService() {
        stopListening();
        if (!isAmbient()) {
            startListening();
            Toast toast = Toast.makeText(getApplicationContext(), "Sorry, hast du etwas gesagt?", Toast.LENGTH_SHORT);
            toast.show();
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

    private void initGestureMap() {
        this.gestureMap = new HashMap<>();
        gestureMap.put(0, new GestureCommand() { // pause/resume
            @Override
            public void execute() {
                ToggleButton btn = (ToggleButton) findViewById(R.id.btn_play_pause);
                btn.setChecked(!btn.isChecked());
                play(btn);
            }
        });
        gestureMap.put(1, new GestureCommand() { // previous song
            @Override
            public void execute() {
                prev(findViewById(R.id.btn_prev));
            }
        });
        gestureMap.put(2, new GestureCommand() { // next song
            @Override
            public void execute() {
                next(findViewById(R.id.btn_next));
            }
        });
        gestureMap.put(3, new GestureCommand() { // volumeDown
            @Override
            public void execute() {
                volumeDown(findViewById(R.id.ctrl_volume_down));
            }
        });
        gestureMap.put(4, new GestureCommand() {
            @Override
            public void execute() { // volumeUp
                volumeUp(findViewById(R.id.ctrl_volume_up));
            }
        });
        gestureMap.put(5, new GestureCommand() { // shuffle
            @Override
            public void execute() {
                // This shall function as a "do nothing gesture"
//                ToggleButton btn = (ToggleButton) findViewById(R.id.ctrl_shuffle);
//                btn.setChecked(!btn.isChecked());
//                onShuffleBtnClicked(btn);
//                Log.d("MainActivity", "Everyday I'm Shuffling");
            }
        });
    }

    private void setGestureButtonChecked(boolean checked) {
        ToggleButton gestureBtn = (ToggleButton) findViewById(R.id.btn_gesture);
        gestureBtn.setChecked(checked);
    }

    private void initGesturesFromFile() {
        Log.d("MainActivity", "ExternalStorageDirectory: " + Environment.getExternalStorageDirectory());
        String[] fileNameArray = FileIO.getAllFileNames();
        if (fileNameArray == null) return;
        List<String> fileNames = new ArrayList<>(Arrays.asList(fileNameArray));

        Collections.sort(fileNames);
        String gestureName = "";
        for (String s : fileNames) {
            Log.d("MainActivity", "Filename: " + s);
            gestureName = s.replace(".txt", "");
            aWiigee.getDevice().loadGesture(gestureName);
        }
    }

    //private final double probThreshold = 0.95;
    private final double[] probThresholds = {0.98, 0.90, 0.95, 0.95, 0.95, 0.98};
    private final String[] gestureNames = {"Play-Pause", "previous", "next", "volume down", "volume up", "dummy"};

    @Override
    public void gestureReceived(GestureEvent event) {
        int id = event.getId();
        String logString = "Gesture;";
        Log.d("AndroidWiigee", "received event: " + event.isValid() + "; id: " + id);
        Toast.makeText(getApplicationContext(), "Gesture: " + gestureNames[id] + "\nProbability: " + event.getProbability(),
                Toast.LENGTH_LONG).show();
        if (event.isValid() && event.getProbability() >= probThresholds[id]) {
            Log.d("AndroidWiigee", "Accepted Event");
            if (gestureMap.containsKey(id)) {
                logString += "recognized as " + gestureNames[id] + " with p=" + event.getProbability();
                GestureCommand cmd = gestureMap.get(id);
                if (cmd != null) {
                    vibrator.vibrate(400);
                    cmd.execute();
                }
            }
        } else {
            logString += "not recognized. closest match: " + gestureNames[id] + " with p=" + event.getProbability();
        }
        ExperimentLogger.log(new InputEvent(logString));
    }

    @Override
    public void accelerationReceived(AccelerationEvent event) {
        //Log.d("Acceleration", "X: " + event.getX() + "; Y: " + event.getY() + "; Z: " + event.getZ() + "; Abs: " + event.getAbsValue());
    }

    @Override
    public void motionStartReceived(MotionStartEvent event) {
//        Log.d("AccelerationListener", "Motion Start Received");
    }

    @Override
    public void motionStopReceived(MotionStopEvent event) {
//        Log.d("AccelerationListener", "Motion Stop Received");
//        Log.d("AccelerationListener", "--------------------");
    }

    @Override
    public void gestureTrained(GestureTrainedEvent event) {
        Log.d("MainActivity", "Gesture Trained! Waiting for a name...");
        int id = event.getID();
        commManager.sendGestureTrained("" + id);
    }

    public void fireWiigeeButtonEvent(int buttonType) {
        switch (buttonType) {
            case TRAIN_BUTTON:
                trainButtonDown = !trainButtonDown;
                setGestureButtonChecked(trainButtonDown);
                if (trainButtonDown) aWiigee.getDevice().fireButtonPressedEvent(buttonType);
                else aWiigee.getDevice().fireButtonReleasedEvent(buttonType);
                break;
            case RECOGNITION_BUTTON:
                recognitionButtonDown = !recognitionButtonDown;
                setGestureButtonChecked(recognitionButtonDown);
                if (recognitionButtonDown) aWiigee.getDevice().fireButtonPressedEvent(buttonType);
                else aWiigee.getDevice().fireButtonReleasedEvent(buttonType);
                break;
            case CLOSE_GESTURE_BUTTON:
                aWiigee.getDevice().fireButtonPressedEvent(buttonType);
                break;
        }
    }


    // Listening to sensor (GAME_ROTATION_VECTOR) for recognizing a start gesture

    private float[] mRotationMatrixFromVector = new float[16];
    private float[] mRotationMatrix = new float[16];
    private float[] orientationVals = new float[3];
    private float pitchThreshold = -70f; // every testuser reached -70 pretty easy
    private float minPitch = 0f;
    private boolean pitchReachedBefore = false;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {

            // Convert the rotation-vector to a 4x4 matrix.
            SensorManager.getRotationMatrixFromVector(mRotationMatrixFromVector, event.values);
            SensorManager.remapCoordinateSystem(mRotationMatrixFromVector,
                    SensorManager.AXIS_X, SensorManager.AXIS_Z,
                    mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, orientationVals);

            // Optionally convert the result from radians to degrees
            orientationVals[0] = (float) Math.toDegrees(orientationVals[0]);
            orientationVals[1] = (float) Math.toDegrees(orientationVals[1]);
            orientationVals[2] = (float) Math.toDegrees(orientationVals[2]);

            if (orientationVals[1] < pitchThreshold && orientationVals[1] < minPitch) {
                minPitch = orientationVals[1];
            }

//            if(isAmbient()) {
//                Log.d("MainAcitivity", "SensorEvent in AmbientMode");
//            }

            if (orientationVals[1] < pitchThreshold && !pitchReachedBefore) {
                pitchReachedBefore = true;
                if (!trainButtonDown && !recognitionButtonDown && !isRecordingGesture) {
//                    isCountdown = true;
                    isRecordingGesture = true;
                    setScreenAlwaysOn(true); // prevent app from entering ambient mode while recording
                    ExperimentLogger.log(new InputEvent("Gesture;performed activation"));
                    vibrator.vibrate(gestureWarmupPattern, -1);
                    gestureHandler.postDelayed(gestureRunnable, gestureRecognitionStartTime);
                }
            } else if (orientationVals[1] > pitchThreshold && pitchReachedBefore) {
                pitchReachedBefore = false;
//                isCountdown = false;
                if (isTraining) {
                    //saveMinPitch();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // nothing to do here
    }

    private void saveMinPitch() {
        String fileName = "minPitchValues";
        FileIO.writeToFile(minPitch + "\r\n", fileName);
        minPitch = 0f;
    }

}
