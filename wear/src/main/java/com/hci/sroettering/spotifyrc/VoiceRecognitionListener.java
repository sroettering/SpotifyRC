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

    public boolean hasRecorded;

    private String[] errorMessages = {
            "Unknown Error", "Network operation timed out", "Other network related errors",
            "Audio recording error", "Server sends error status", "Other client side errors",
            "No speech input", "No recognition result matched", "RecognitionService busy",
            "Insufficient permissions"
    };

    // Singleton private constructor
    private VoiceRecognitionListener() { hasRecorded = false; }

    public static VoiceRecognitionListener getInstance() {
        if(instance == null) {
            instance = new VoiceRecognitionListener();
        }
        return instance;
    }

    public void setListener(IVoiceControl voiceControl) {
        listener = voiceControl;
    }

    public void processVoiceCommand(String voiceCommand) {
        if(listener != null) {
            listener.processVoiceCommand(voiceCommand);
        }
    }

    @Override
    public void onResults(Bundle results) {
//        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
//        processVoiceCommand(matches.get(0));
        if(listener != null) {
            listener.restartListeningService();
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d("VRListener", "Starting to listen");
        hasRecorded = false;
    }

    @Override
    public void onEndOfSpeech() {
        listener.onListeningError();
        Log.d("VRListener", "Waiting for result...");
    }

    @Override
    public void onError(int error) {
        if(error >= 0 && error <= 9)
            Log.d("VRListener", "Got Error: " + errorMessages[error]);
        if (listener != null) {
            listener.onListeningError();
            listener.restartListeningService();
        }
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
        //Log.d("VRListener", "onPartialResults: " + partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0));
        ArrayList<String> results = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String command = results.get(0);
        if(command.endsWith("bitte")) {
            hasRecorded = false;
            processVoiceCommand(command);
        } else {
            hasRecorded = true;
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d("VRListener", "onEvent: " + eventType);
    }
}
