package com.hci.sroettering.spotifyrc;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by sroettering on 23.05.16.
 */
public class VoiceRecognitionListener implements RecognitionListener {

    private static VoiceRecognitionListener instance = null;

    private IVoiceControl listener; // Must run in main thread

    // Singleton private constructor
    private VoiceRecognitionListener() {};

    public static VoiceRecognitionListener getInstance() {
        if(instance == null) {
            instance = new VoiceRecognitionListener();
        }
        return instance;
    }

    public void setListener(IVoiceControl voiceControl) {
        listener = voiceControl;
    }

    public void processVoiceCommands(String... voiceCommands) {
        if(listener != null) {
            listener.processVoiceCommands(voiceCommands);
        }
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String[] commands = new String[matches.size()];
        for (String command : matches) {
            System.out.println(command);
        }
        commands = matches.toArray(commands);
        processVoiceCommands(commands);
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d("VRListener", "Starting to listen");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d("VRListener", "Waiting for result...");
    }

    @Override
    public void onError(int error) {
        if (listener != null) {
            listener.restartListeningService();
        }
        Log.d("VRListener", "Got Error: " + error);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d("VRListener", "Ready for speech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }
}