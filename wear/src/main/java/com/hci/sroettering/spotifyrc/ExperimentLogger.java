package com.hci.sroettering.spotifyrc;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by sroettering on 21.06.16.
 */
public class ExperimentLogger {

    private static final String directoryName = "/SRCLog";
    private static ArrayList<InputEvent> events = new ArrayList<>();

    public static int subjectID = -1;
    public static int scenarioID = -1;

    public static void log(InputEvent event) {
        events.add(event);

        if(subjectID == -1 || scenarioID == -1) {
            Log.d("ExperimentLogger", "subjectID or scenarioID is not set; caching event");
        } else {
            triggerFileLogging();
        }

        String logString = event.toString();
        Log.d("ExperimentLogger", logString);
    }

    public static void triggerFileLogging() {
        if(subjectID == -1 || scenarioID == -1) {
            Log.d("ExperimentLogger", "subjectID or scenarioID is not set; not logging to file");
            return;
        }

        if(events.isEmpty()) return; // no events, no logging

        String logText = "";
        String fileName = "subject_"+subjectID + "_scenario_"+scenarioID + ".log";
        for(InputEvent e: events) {
            logText += e.toString() + "\n";
        }

        writeToFile(logText, fileName);
        events.clear();
    }

    private static void writeToFile(String text, String fileName) {
        try {
            File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), directoryName);
            if (!root.exists()) {
                root.mkdirs();
            }
            File file = new File(root, fileName);
            BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
            out.append(text);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
