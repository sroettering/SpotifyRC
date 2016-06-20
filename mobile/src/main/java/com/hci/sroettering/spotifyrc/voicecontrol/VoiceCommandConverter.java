package com.hci.sroettering.spotifyrc.voicecontrol;

import android.os.Environment;
import android.util.Log;

import com.hci.sroettering.spotifyrc.SpotifyItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kaaes.spotify.webapi.android.models.AudioFeaturesTrack;
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks;

/**
 * Created by sroettering on 24.05.16.
 *
 * Converting process principle:
 *
 * return
 * ^          +
 * |          | voiceCommand
 * |          |
 * |    +-----+-----+
 * | no |polite     |
 * +----+keyword?   |
 * |    +-----+-----+
 * |          |
 * |          |yes
 * |          |
 * |    +-----v-----+ yes    +-----------------+
 * |    |focused    +-------->convert to       |
 * |    |keywords?  |        |focused command  |
 * |    +-----+-----+        +-----------------+
 * |          |
 * |          |no
 * |          |
 * | no +-----v-----+ yes    +-----------------+
 * +----+casual     +-------->convert to       |
 *      |keywords?  |        |casual command   |
 *      +-----------+        +-----------------+
 *
 *
 *
 */
public class VoiceCommandConverter {

    private List<SpotifyItem>[] spotifyData;

    private String[] types = {"playlist", "song", "album", "artist", "category"};

    public VoiceCommandConverter() {}

    // you must set data before any text can be converted to a command
    public void setSpotifyData(List[] data) {
        spotifyData = data;
    }

    // type: 0 = playlist; 1 = song; 2 = album; 3 = artist; 4 = category
//    public void setSpotifyData(List<SpotifyItem> data, int type) {
//        spotifyData[type] = data;
//    }

    public boolean couldBeCommand(String text) {
        return spotifyData != null && KeywordDictionary.containsPoliteKeyword(text);
    }

    // couldBeCommand() should have been called beforehand
    public String convertCommand(String text) {
        Command command = new Command(text);

        // 1. delete polite keyword and special characters for safety measures
        command.cleanText();
        Log.d("VCC", "clean text: " + command.text);

        // 2. check if command is of focused nature, i.e. text contains a player control command
        if(KeywordDictionary.containsFocusedKeyword(command)) {
            //Log.d("VCC", "text contains focused keyword");
            convertFocusedCommand(command);

        } else {
            // 3. command is of casual nature, i.e. an audio feature is directly mentioned
            // or keywords positivley/negatively describing a specific audio feature are mentioned
            // or user just wants a different song
            Log.d("VCC", "text could be casual");
            convertCasualCommand(command);

        }
        // 4. someone said the polite keyword by accident...do nothing

        return command.resultingCommand;
    }

    private void convertFocusedCommand(Command command) {

        // 2.1 check for simple focused command, i.e. commands without any extra "parameters" (or extras)
        if(command.hasSimpleFocusedToken()) {
            //Log.d("VCC", "text contains simple focused keyword");
            convertSimpleFocusedCommand(command);

        } else if(command.hasComplexFocusedToken()) {
            // 2.2 focused command seems to contain extras (shuffle or play). Convert those extras and append to result

            //Log.d("VCC", cleanText + " contains complex focused keyword");
            if(command.getComplexFocusedToken().type == Token.Type.SHUFFLE) {
                //Log.d("VCC", "text contains shuffle keyword");
                Token onOffToken = command.getOnOrOffToken();
                // when there is just a shuffle token, it is assumed the user wants to activate shuffle
                if(onOffToken != null && onOffToken.type == Token.Type.NEGATIVE) {
                    command.resultingCommand = "shuffle;0";
                } else {
                    command.resultingCommand = "shuffle;1";
                }
            } else if(command.getComplexFocusedToken().type == Token.Type.PLAY) {
                // a play command needs some extra information about what to play
                Token extraToken = command.getExtraToken(); // should not be null because cleanText contains extras
                //Log.d("VCC", "text contains play keyword with extras: " + extraToken.keyword);
                command.resultingCommand = "play";
                if(extraToken != null) {
                    command.resultingCommand += convertExtrasToCommand(extraToken.keyword);
                }
            } else { // no suitable command was found, most likely because play had no extras
                Log.d("VCC", "Unknown complex focused command");
            }
        }

    }

    private void convertSimpleFocusedCommand(Command command) {
        switch (command.getSimpleFocusedToken().type) {
            case PAUSE: command.resultingCommand = "pause";
                break;
            case RESUME: command.resultingCommand = "resume";
                break;
            case NEXT: command.resultingCommand = "next";
                break;
            case PREV: command.resultingCommand = "prev";
                break;
            case VOLUMEUP: command.resultingCommand = "volumeUp";
                break;
            case VOLUMEDOWN: command.resultingCommand = "volumeDown";
                break;
        }
    }

    private String convertExtrasToCommand(String text) {
        String extraCommand = ";";

        int minimalDistance = Integer.MAX_VALUE;
        SpotifyItem mostSimilarItem = null;
        // search spotify data for the given text
        outer: for(int i = 0; i < spotifyData.length; i++) {
            if(spotifyData[i] == null) {
                continue;
            }
            for(SpotifyItem item: spotifyData[i]) {
                int distance = computeMinimalDistanceWithSubstrings(item.text, text);
                //Log.d("VoiceCommandConverter", "data[" + i + "] text: " + item.text + "; distance: " + distance);
                if(distance < minimalDistance) {
                    minimalDistance = distance;
                    mostSimilarItem = item;
                }
                if(minimalDistance < 0) break outer; // quick exit
            }
        }
        //Log.d("VoiceCommandConverter", "found item with text: " + mostSimilarItem.text + " and minimal distance: " + minimalDistance);
        extraCommand += types[mostSimilarItem.type] + ";" + mostSimilarItem.spotifyID;
        return extraCommand;
    }

    private void convertCasualCommand(Command command) {
        // 3.1 audio feature is directly mentioned
        KeywordDictionary.tokenizeCasualCommand(command);
        if(command.hasCasualAudioFeatureToken()) {
            Log.d("VCC", "text contains AudioFeature Token");
            command.resultingCommand += "casual;" + command.getCasualAudioFeatureToken().keyword + ";";

            float multiplier = 0f;
            Token multiplierToken = command.getMultiplierToken();
            if(multiplierToken != null) { // should be obsolete, because a neutral token will always be created
                switch (multiplierToken.type.getValue()) {
                    case 12: multiplier = 0.25f;
                        break;
                    case 13: multiplier = 0.75f;
                        break;
                    case 14: multiplier = 0.5f;
                        break;
                    default: multiplier = 0.5f;
                }
            }

            Token directionToken = command.getDirectionToken();
            if(directionToken != null) {
                if(directionToken.type.getValue() == Token.Type.CASUAL_NEGATIVE.getValue()) {
                    multiplier *= -1;
                }
                command.resultingCommand += "" + multiplier;
            } else { // not a valid casual command without directionToken, reset the command
                command.resultingCommand = "";
            }

        } else {
            return; // dont know what the user wanted
        }
    }

    private int computeMinimalDistanceWithSubstrings(String text, String textToLookFor) {
        String cleanText = text.replaceAll("[^a-zA-Z0-9äöüë ]+","").trim();
        cleanText = cleanText.replaceAll(" +", " "); // replace multiple spaces with one

        if(cleanText.equals(textToLookFor)) return -1; // quick exit; -1 to prefer exact matches over others

        int minimumDistance = Integer.MAX_VALUE;
        //Log.d("VoiceCommandConverter", "cleanText: " + cleanText + "; textToLookFor: " + textToLookFor);
        if(text.length() < textToLookFor.length()) {
            return computeLevenshteinDistance(cleanText, textToLookFor);
        }

        String curSubString = "";
        for(int i = 0; i < cleanText.length() - textToLookFor.length() + 1; i++) { // +1 in case of same lengths
            curSubString = cleanText.substring(i, i + textToLookFor.length());
            if(curSubString.equals(textToLookFor)) return 0; // quick exit if item was found
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
