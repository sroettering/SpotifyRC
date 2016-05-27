package com.hci.sroettering.spotifyrc.voicecontrol;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by sroettering on 27.05.16.
 *
 * Dictionary class which contains all keywords for voice command conversion
 * the keyword sets should be pairwise disjoint to function properly,
 * otherwise the order of testing is important!
 *
 * Important casual audio features are:
 * - tempo
 * - valence (mood of a song)
 * - loudness
 * - energy
 * - danceability
 *
 */
public class KeywordDictionary {

    public static final LinkedList<String> play_keywords = new LinkedList<String>(Arrays.asList(
            "play ", "abspielen", "wechsel mal zu", "spiele", "spiel", "spiel mal",
            "ich wünsche mir etwas von", "ich wünsch mir etwas von", "ich wünsche mir", "ich wünsch mir",
            "spiel mal was von", "spiel mal etwas von"
    ));

    public static final LinkedList<String> pause_keywords = new LinkedList<String>(Arrays.asList(
            "pause", "stop", "stopp", "anhalten", "halt", "aus"
    ));

    public static final LinkedList<String> resume_keywords = new LinkedList<String>(Arrays.asList(
            "resume", "weiter", "an", "play ", "wiedergabe", "start"
    ));

    public static final LinkedList<String> next_keywords = new LinkedList<String>(Arrays.asList(
            "next", "nächster", "nächstes", "lied weiter", "song weiter", "vor"
    ));

    public static final LinkedList<String> prev_keywords = new LinkedList<String>(Arrays.asList(
            "previous", "vorheriger", "vorheriges", "zurück", "back"
    ));

    public static final LinkedList<String> shuffle_keywords = new LinkedList<String>(Arrays.asList(
            "random", "shuffle", "zufällig", "zufällige wiedergabe"
    ));

    public static final LinkedList<String> volumeUp_keywords = new LinkedList<String>(Arrays.asList(
            "lauter", "louder"
    ));

    public static final LinkedList<String> volumeDown_keywords = new LinkedList<String>(Arrays.asList(
            "leiser", "ruhiger"
    ));

    public static final LinkedList<String> true_keywords = new LinkedList<String>(Arrays.asList(
            "an", "on", "ja"
    ));

    public static final LinkedList<String> false_keywords = new LinkedList<String>(Arrays.asList(
            "aus", "off", "nein", "kein"
    ));

    public static final LinkedList<String> casual_begin_keywords = new LinkedList<String>(Arrays.asList(
            "mach mal", "ich hätte gern", "ich hätte gerne", "ich hätt gern", "ich hätte gern"
    ));

    public static final LinkedList<String> casual_audioFeature_keywords = new LinkedList<String>(Arrays.asList(
            "tempo", "geschwindigkeit", "speed", "schnelligkeit", // - tempo
            "stimmung", "mood", // - valence
            "lautstärke", // - loudness
            "energie", "power", // - energy
            "tanzbarkeit" // - danceability
    ));

    public static final LinkedList<String> casual_negative_general_keywords = new LinkedList<String>(Arrays.asList(
            "weniger", "zu viel"
    ));

    public static final LinkedList<String> casual_positive_general_keywords = new LinkedList<String>(Arrays.asList(
            "mehr", "zu wenig"
    ));

    // the user demands a decrease of a certain audio feature
    public static final LinkedList<String> casual_negative_adjectives_keywords = new LinkedList<String>(Arrays.asList(
            "chilliger", "chilligeres", "ruhiger", "ruhigeres", "langsamer", "langsameres", "zu schnell",
            "weniger gesang", "leiser", "leiseres", "traurigere", "traurigeres", "trauriger", "zu hart"
    ));

    public static final String politeness_keyword = "bitte";

    public static ArrayList<Token> tokenList = new ArrayList<>();

    public static void resetTokenList() {
        tokenList = new ArrayList<>();
    }

    public static boolean containsTokenOfType(Token.Type type) {
        for(Token token: tokenList) {
            if(token.type == type) return true;
        }
        return false;
    }

    // dont create a token here, because the keyword is never used again
    public static boolean containsPoliteKeyword(String text) {
        return text.contains(politeness_keyword);
    }

    public static boolean containsFocusedKeyword(String text) {
        // no need for quick exit here (containsTokenOfType())

        // non conflicting checks
        if(isKeywordInList(next_keywords, text, Token.Type.NEXT)) return true;
        if(isKeywordInList(prev_keywords, text, Token.Type.PREV)) return true;
        if(isKeywordInList(volumeUp_keywords, text, Token.Type.VOLUMEUP)) return true;
        if(isKeywordInList(volumeDown_keywords, text, Token.Type.VOLUMEDOWN)) return true;

        // conflicting checks (play - resume conflict is taken care of in isKeywordInList)
        if(isKeywordInList(shuffle_keywords, text, Token.Type.SHUFFLE)) {
            containsOffKeyword(text); // if shuffle is detected, the extra needs to be extracted
            return true; // shuffle an/aus
        }
        if(isKeywordInList(pause_keywords, text, Token.Type.PAUSE)) return true; // aus
        if(isKeywordInList(resume_keywords, text, Token.Type.RESUME)) return true; // an - play
        if(isKeywordInList(play_keywords, text, Token.Type.PLAY)) return true; // play
        return false;
    }

    public static boolean containsExtras(String text) {
        if(containsTokenOfType(Token.Type.EXTRA)) return true;

        boolean contains = false;
        if(!text.equals("")) {
            addTokenForKeyword(text, Token.Type.EXTRA);
            //text.replace(text, ""); // necessary?
            contains = true;
        }
        return contains;
    }

    public static boolean containsOffKeyword(String text) {
        if(containsTokenOfType(Token.Type.NEGATIVE)) return true;
        return isKeywordInList(false_keywords, text, Token.Type.NEGATIVE);
    }

    public static boolean containsCasualKeyword(String text) {
        boolean contains = false;

        return contains;
    }

    public static boolean startsWithCasualKeyword(String text) {
        boolean contains = false;
        if(isKeywordInList(casual_begin_keywords, text, Token.Type.CASUAL_BEGIN)) contains = true;
        return contains;
    }

    // Util method
    private static boolean isKeywordInList(LinkedList<String> list, String text, Token.Type keywordType) {
        for(String s: list) {
            if(text.contains(s)) {
                if(keywordType == Token.Type.RESUME && text.equals(s)) {
                    addTokenForKeyword(s, Token.Type.RESUME);
                } else if(keywordType == Token.Type.RESUME) {
                    // text apparently contains extras so no token is added
                    return false;
                }else if(keywordType != Token.Type.RESUME) {
                    addTokenForKeyword(s, keywordType);
                }
                text.replace(s, "");
                return true;
            }
        }
        return false;
    }

    private static void addTokenForKeyword(String keyword, Token.Type keywordType) {
        if(tokenList != null) {
            //Log.d("KeywordDictionary", "adding token to list with type: " + keywordType);
            tokenList.add(new Token(keyword, keywordType));
        }
    }

}

