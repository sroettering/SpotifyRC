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
 *  TODO: use String.matches(regex) for keyword search
 *
 */
public class KeywordDictionary {

    public static final String[] play_keywords = {
            "play", "abspielen", "wechsel mal zu", "spiele", "spiel", "spiel mal",
            "ich wünsche mir etwas von", "ich wünsch mir etwas von", "ich wünsche mir", "ich wünsch mir",
            "spiel mal was von", "spiel mal etwas von",
            "ich möchte etwas von", "ich möchte was von", "ich möchte"
    };

    public static final String[] pause_keywords = {
            "pause", "stop", "stopp", "anhalten", "halt", "aus"
    };

    public static final String[] resume_keywords = {
            "resume", "weiter", "an", "play", "wiedergabe", "start"
    };

    public static final String[] next_keywords = {
            "next", "nächster", "nächstes", "lied weiter", "song weiter", "vor"
    };

    public static final String[] prev_keywords = {
            "previous", "vorheriger", "vorheriges", "zurück", "back"
    };

    public static final String[] shuffle_keywords = {
            "random", "shuffle", "zufällig", "zufällige wiedergabe"
    };

    public static final String[] volumeUp_keywords = {
            "lauter", "louder"
    };

    public static final String[] volumeDown_keywords = {
            "leiser", "ruhiger"
    };

    public static final String[] true_keywords = {
            "an", "on", "ja"
    };

    public static final String[] false_keywords = {
            "aus", "off", "nein", "kein"
    };


    // TODO: maybe its enough to demand, that every casual command has to look like "etwas ... bitte"
    public static final String[] casual_audioFeature_keywords = {
            "tempo", "geschwindigkeit", "speed", "schnelligkeit", // - tempo
            "stimmung", "mood", // - valence (positivity/negativity)
            "lautstärke", // - loudness
            "energie", "power", // - energy
            "tanzbarkeit", "zum tanzen" // - danceability
    };

    public static final String[] casual_negative_general_keywords = {
            "weniger", "zu viel"
    };

    public static final String[] casual_positive_general_keywords = {
            "mehr", "zu wenig", "besser", "bessere"
    };

    public static final String[] casual_indirect_negative_tempo_keywords = {
            "langsamer", "langsameres", "langsamere", "zu schnell"
    };

    public static final String[] casual_indirect_positive_tempo_keywords = {
            "schneller", "schnelleres", "schnellere", "zu langsam", "zu lahm"
    };

    public static final String[] casual_indirect_negative_valence_keywords = {
            "traurigeres", "traurigere", "zu fröhlich", "zu positiv", "negativer", "negativeres"
    };

    public static final String[] casual_indirect_positive_valence_keywords = {
            "fröhlicher", "fröhlicheres", "zu traurig", "zu negativ", "positiver", "positiveres"
    };



    // the user demands a decrease of a certain audio feature
    public static final String[] casual_negative_adjectives_keywords = {
            "chilliger", "chilligeres", "ruhiger", "ruhigeres", "langsamer", "langsameres", "zu schnell",
            "weniger gesang", "leiser", "leiseres", "traurigere", "traurigeres", "trauriger", "zu hart"
    };

    public static final String politeness_keyword = "bitte";

    public static boolean containsTokenOfType(Command command, Token.Type type) {
        for(Token token: command.tokenList) {
            if(token.type == type) return true;
        }
        return false;
    }

    // dont create a token here, because the keyword is never used again
    public static boolean containsPoliteKeyword(String text) {
        return text.contains(politeness_keyword);
    }

    public static boolean containsFocusedKeyword(Command command) {
        // no need for quick exit here (containsTokenOfType())

        // non conflicting checks
        if(isKeywordInList(next_keywords, command, Token.Type.NEXT)) return true;
        if(isKeywordInList(prev_keywords, command, Token.Type.PREV)) return true;
        if(isKeywordInList(volumeUp_keywords, command, Token.Type.VOLUMEUP)) return true;
        if(isKeywordInList(volumeDown_keywords, command, Token.Type.VOLUMEDOWN)) return true;

        // conflicting checks (play - resume conflict is taken care of in isKeywordInList)
        if(isKeywordInList(shuffle_keywords, command, Token.Type.SHUFFLE)) {
            containsOffKeyword(command); // if shuffle is detected, the extra needs to be extracted
            return true; // shuffle an/aus
        }
        if(isKeywordInList(pause_keywords, command, Token.Type.PAUSE)) return true; // aus
        if(isKeywordInList(resume_keywords, command, Token.Type.RESUME)) return true; // an - play
        if(isKeywordInList(play_keywords, command, Token.Type.PLAY)) {
            findExtrasInCommand(command);
            return true; // play
        }
        return false;
    }

    public static void findExtrasInCommand(Command command) {
        if(containsTokenOfType(command, Token.Type.EXTRA)) return;

        if(!command.text.equals("")) {
            command.addToken(command.text, Token.Type.EXTRA);
        }
    }

    public static boolean containsOffKeyword(Command command) {
        if(containsTokenOfType(command, Token.Type.NEGATIVE)) return true;
        return isKeywordInList(false_keywords, command, Token.Type.NEGATIVE);
    }

    public static void tokenizeCasualCommand(Command command) {
        isKeywordInList(casual_audioFeature_keywords, command, Token.Type.CASUAL_AUDIO_FEATURE);
    }

    // Util method
    private static boolean isKeywordInList(String[] list, Command command, Token.Type keywordType) {
        for(String s: list) {
            if(command.text.contains(s)) {
                if(keywordType == Token.Type.RESUME && command.text.equals(s)) {
                    command.addToken(s, Token.Type.RESUME);
                } else if(keywordType == Token.Type.RESUME) {
                    // text apparently contains extras so no token is added
                    return false;
                }else if(keywordType != Token.Type.RESUME) {
                    command.addToken(s, keywordType);
                }
                return true;
            }
        }
        return false;
    }

}

