package com.hci.sroettering.spotifyrc;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sroettering on 03.06.16.
 *
 * This class is responsible for caching certain spotify data in files,
 * so that network load on app start is reduced.
 * Would be better to cache the spotify requests directly
 * Cache is lazy initialized. Check with hasXXX() wether data is available before requesting it
 *
 */
public class SpotifyCache {

    // paths and file names
    private static final String directoryName = "/SpotifyRC";
    private static final String fileName_audioFeatures = "AudioFeatures.csv";
    private static final String fileName_lastPlayed = "lastPlayed.csv";

    // file contents
    private static List<String> audioFeatures_content;
    private static List<String> lastPlayed_content;



    // ---------------- Methods -------------------------

    /*
     * Audio Features
     */
    public static boolean hasAudioFeatures() {
        if(audioFeatures_content == null || audioFeatures_content.size() <= 0) {
            audioFeatures_content = readFromFile(fileName_audioFeatures);
        }
        return audioFeatures_content != null && audioFeatures_content.size() > 0;
    }

    public static List<String> getAudioFeatures() {
        return audioFeatures_content;
    }

    public static void saveAudioFeatures(String features) {
        writeToFile(fileName_audioFeatures, features);
    }


    /*
     * Last Track Queue
     */
    public static boolean hasLastPlayedQueue() {
        if(lastPlayed_content == null || lastPlayed_content.size() <= 0) {
            lastPlayed_content = readFromFile(fileName_lastPlayed);
        }
        return lastPlayed_content != null && lastPlayed_content.size() > 0;
    }

    public static List<String> getLastPlayedQueue() {
        return lastPlayed_content;
    }

    public static void saveLastPlayedQueue(String lastPlayed) {
        writeToFile(fileName_lastPlayed, lastPlayed);
    }



    // ----------------- File operations -----------------

    /*
     * Writes a single String to the specified file
     */
    private static void writeToFile(String fileName, String content) {
        try {
            File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), directoryName);
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, fileName);
            FileOutputStream f = new FileOutputStream(gpxfile);
            PrintWriter pw = new PrintWriter(f);
            pw.println(content);
            pw.flush();
            pw.close();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * Reads a file line by line and saves each in a list
     */
    private static List<String> readFromFile(String fileName) {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + directoryName, fileName);
        if(!file.exists()) {
            Log.d("SpotifyCache", "file not found");
            return null;
        }

        List<String> fileContent = new ArrayList<>();
        String input = "";
        try {
            BufferedReader bReader = new BufferedReader(new FileReader(file));
            while((input = bReader.readLine()) != null) {
                fileContent.add(input);
            }
            bReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileContent;
    }

}
