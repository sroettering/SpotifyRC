package com.hci.sroettering.spotifyrc.voicecontrol;

import android.util.Log;

import com.hci.sroettering.spotifyrc.SpotifyItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * +----+fuzzy      +-------->convert to       |
 *      |keywords?  |        |fuzzy command    |
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
        String result = "";
        KeywordDictionary.resetTokenList();

        // 1. delete polite keyword and special characters for safety measures
        String cleanText = text.replace(KeywordDictionary.politeness_keyword, "")
                .replaceAll("[^a-zA-Z0-9üöä ]+","")
                .toLowerCase()
                .trim();
        Log.d("VCC", "clean text: " + cleanText);

        // 2. check if command is of focused nature, i.e. text contains a player control command
        if(KeywordDictionary.containsFocusedKeyword(cleanText)) {
            //Log.d("VCC", "text contains focused keyword");

            // 2.1 check for simple focused command, i.e. commands without any extra "parameters" (or extras)
            Token simpleFocusedToken = getSimpleFocusedTokenIfAny();
            if(simpleFocusedToken != null) {
                //Log.d("VCC", "text contains simple focused keyword");
                result = getCommandForSimpleToken(simpleFocusedToken);
                return result;
            }
            // 2.2 focused command seems to contain extras (shuffle or play). Convert those extras and append to result
            Token complexFocusedToken = getComplexFocusedTokenIfAny();
            if(complexFocusedToken != null) {
                //Log.d("VCC", cleanText + " contains complex focused keyword");
                if(complexFocusedToken.type == Token.Type.SHUFFLE) {
                    //Log.d("VCC", "text contains shuffle keyword");
                    Token onOffToken = getOnOrOffTokenIfAny();
                    // when there is just a shuffle token, it is assumed the user wants to activate shuffle
                    if(onOffToken != null && onOffToken.type == Token.Type.NEGATIVE) {
                        result = "shuffle;0";
                    } else {
                        result = "shuffle;1";
                    }
                } else if(complexFocusedToken.type == Token.Type.PLAY && KeywordDictionary.containsExtras(cleanText)) {
                    // a play command needs some extra information about what to play
                    Token extraToken = getExtraTokenIfAny(); // should not be null because cleanText contains extras
                    //Log.d("VCC", "text contains play keyword with extras: " + extraToken.keyword);
                    result = "play";
                    result += convertExtrasToCommand(extraToken.keyword);
                    return result;
                } else { // no suitable command was found, most likely because play had no extras
                    return result;
                }
            }


        } else if(KeywordDictionary.containsCasualKeyword(cleanText)) {
            Log.d("VCC", "text contains casual keyword");
            // 3. check if command is of casual nature, i.e. an audio feature is directly mentioned
            // or keywords positivley/negatively describing a specific audio feature are mentioned
            // or user just wants a different song

            // 3.1 audio feature is directly mentioned

            // 3.2 audio feature is indirectly mentioned

            // 3.3 no audio feature is mentioned -> just play next track or random other spotifyItem
        }
        // 4. someone said the polite keyword by accident...do nothing

        return result;
    }

    private Token getSimpleFocusedTokenIfAny() {
        for(Token token: KeywordDictionary.tokenList) {
            // token.type values from 0 to 5 belong to simple commands
            if(token.type.getValue() >= 0 && token.type.getValue() < 6) return token;
        }
        return null;
    }

    private Token getComplexFocusedTokenIfAny() {
        for(Token token: KeywordDictionary.tokenList) {
            // token.type values from 8 to 9 belong to complex commands (play and shuffle)
            if(token.type.getValue() >= 8 && token.type.getValue() < 10) return token;
        }
        return null;
    }

    private Token getOnOrOffTokenIfAny() {
        for(Token token: KeywordDictionary.tokenList) {
            // token.type values from 6 to 7 belong to complex commands (play and shuffle)
            if(token.type.getValue() >= 6 && token.type.getValue() < 8) return token;
        }
        return null;
    }

    private Token getExtraTokenIfAny() {
        for(Token token: KeywordDictionary.tokenList) {
            if(token.type == Token.Type.EXTRA) return token;
        }
        return null;
    }

    private String getCommandForSimpleToken(Token simpleToken) {
        String command = "";
        switch (simpleToken.type) {
            case PAUSE: command = "pause";
                break;
            case RESUME: command = "resume";
                break;
            case NEXT: command = "next";
                break;
            case PREV: command = "prev";
                break;
            case VOLUMEUP: command = "volumeUp";
                break;
            case VOLUMEDOWN: command = "volumeDown";
                break;
        }
        return command;
    }

    private String convertExtrasToCommand(String text) {
        String extraCommand = ";";
        //Log.d("VoiceCommandConverter", "found extras: " + rText);

        int minimalDistance = Integer.MAX_VALUE;
        SpotifyItem mostSimilarItem = null;
        // search spotify data for the given text
        for(int i = 0; i < spotifyData.length; i++) {
            if(spotifyData[i] == null) {
                continue;
            }
            for(SpotifyItem item: spotifyData[i]) {
                int distance = computeMinimalDistanceWithSubstrings(item.text.toLowerCase(), text);
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
