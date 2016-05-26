package com.hci.sroettering.spotifyrc;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sroettering on 24.05.16.
 */
public class VoiceCommandConverter {

    private final ArrayList<String> play_keywords = new ArrayList<String>(Arrays.asList(
            "play", "abspielen", "wechsel zu", "mach mal", "spiele", "spiel", "ich wünsche mir"
    ));

    private final ArrayList<String> pause_keywords = new ArrayList<String>(Arrays.asList(
            "pause", "stop", "stopp", "anhalten", "halt", "aus"
    ));

    private final ArrayList<String> resume_keywords = new ArrayList<String>(Arrays.asList(
            "resume", "weiter", "an", "play", "wiedergabe", "start"
    ));

    private final ArrayList<String> next_keywords = new ArrayList<String>(Arrays.asList(
            "next", "nächster", "nächstes", "lied weiter", "song weiter", "vor"
    ));

    private final ArrayList<String> prev_keywords = new ArrayList<String>(Arrays.asList(
            "previous", "vorheriger", "vorheriges", "zurück", "back"
    ));

    private final ArrayList<String> shuffle_keywords = new ArrayList<String>(Arrays.asList(
            "random", "shuffle", "zufällig", "zufällige wiedergabe"
    ));

    private final ArrayList<String> volumeUp_keywords = new ArrayList<String>(Arrays.asList(
            "lauter", "louder", "zu leise"
    ));

    private final ArrayList<String> volumeDown_keywords = new ArrayList<String>(Arrays.asList(
            "leiser", "zu laut", "ruhiger", "stiller"
    ));

    private final ArrayList<String> true_keywords = new ArrayList<String>(Arrays.asList(
            "an", "on", "ja"
    ));

    private final ArrayList<String> false_keywords = new ArrayList<String>(Arrays.asList(
            "aus", "off", "nein", "kein"
    ));

    private final String politeness_keyword = "bitte";

    private List<SpotifyItem>[] spotifyData;

    private String[] types = {"playlist", "album", "song", "artist", "category"};

    public VoiceCommandConverter() {}

    public void setSpotifyData(List[] data) {
        spotifyData = data;
    }

    public boolean couldBeCommand(String text) {
        return text.contains(politeness_keyword);
    }

    // order of for loops (keyword handling) is important!
    // next before resume before play
    public String getMostPropableCommandForText(String text) {
        String command = "";
        for(String keyword: pause_keywords) {
            if(text.contains(keyword)) { command = "pause";}
        }
        for(String keyword: next_keywords) {
            if(text.contains(keyword)) { command = "next";}
        }
        for(String keyword: resume_keywords) {
            if(text.contains(keyword)) { command = "resume";}
        }
        for(String keyword: play_keywords) {
            if(text.contains(keyword)) {
                if(containsExtrasToKeyword(text, keyword)) {
                    command = "play";
                    command += convertExtrasToCommand(text, keyword);
                }
                else command = "resume";
            }
        }
        for(String keyword: prev_keywords) {
            if(text.contains(keyword)) { command = "prev";}
        }
        for(String keyword: shuffle_keywords) {
            if(text.contains(keyword)) {
                command = "shuffle";
                if(containsFalseKeyword(text)) command += ";0";
                else command += ";1";
            }
        }
        for(String keyword: volumeUp_keywords) {
            if(text.contains(keyword)) { command = "volumeUp";}
        }
        for(String keyword: volumeDown_keywords) {
            if(text.contains(keyword)) { command = "volumeDown";}
        }
        return command;
    }

    /* // Not needed right now
    private boolean containsTrueKeyword(String text) {
        for(String keyword: true_keywords) {
            if(text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    */

    private boolean containsFalseKeyword(String text) {
        for(String keyword: false_keywords) {
            if(text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsExtrasToKeyword(String text, String keyword) {
        String replaceText = text.replace(keyword, "").replace(politeness_keyword, "").trim();
        return replaceText.length() > 0;
    }

    private String convertExtrasToCommand(String text, String keyword) {
        String extraCommand = ";";
        String rText = text.replace(keyword, "").replace(politeness_keyword, "").trim();
        //Log.d("VoiceCommandConverter", "found extras: " + rText);

        int minimalDistance = Integer.MAX_VALUE;
        SpotifyItem mostSimilarItem = null;
        // search spotify data for the given text
        for(int i = 0; i < spotifyData.length; i++) {
            if(spotifyData[i] == null) {
                continue;
            }
            for(SpotifyItem item: spotifyData[i]) {
                int distance = computeMinimalDistanceWithSubstrings(item.text.toLowerCase(), rText);
                //Log.d("VoiceCommandConverter", "data[" + i + "] text: " + item.text.toLowerCase() + "; distance: " + distance);
                if(distance < minimalDistance) {
                    minimalDistance = distance;
                    mostSimilarItem = item;
                }
            }
        }
        //Log.d("VoiceCommandConverter", "found item with text: " + mostSimilarItem.text + " and minimal distance: " + minimalDistance);
        extraCommand += types[mostSimilarItem.type] + ";" + mostSimilarItem.spotifyID;
        return extraCommand;
    }

    private int computeMinimalDistanceWithSubstrings(String text, String textToLookFor) {
        String cleanText = text.replaceAll("[^a-zA-Z0-9 ]+","").trim();
        int minimumDistance = Integer.MAX_VALUE;
        //Log.d("VoiceCommandConverter", "Substrings cleanText: " + cleanText);
        if(text.length() < textToLookFor.length()) {
            return computeLevenshteinDistance(cleanText, textToLookFor);
        }

        String curSubString = "";
        for(int i = 0; i < cleanText.length() - textToLookFor.length() + 1; i++) { // +1 in case of same lengths
            curSubString = cleanText.substring(i, i+textToLookFor.length());
            minimumDistance = Math.min(minimumDistance, computeLevenshteinDistance(curSubString, textToLookFor));
        }
        return minimumDistance;
    }


    // Levenshtein Distance
    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    public static int computeLevenshteinDistance(CharSequence lhs, CharSequence rhs) {
        int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];

        for (int i = 0; i <= lhs.length(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= rhs.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= lhs.length(); i++)
            for (int j = 1; j <= rhs.length(); j++)
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1));

        return distance[lhs.length()][rhs.length()];
    }

}
