package com.hci.sroettering.spotifyrc;

/**
 * Created by sroettering on 24.05.16.
 */
public class KeywordDictionary {

    private final String[] play_keywords = {"play", "abspielen", "wechsel zu", "mach mal", "spiele", "spiel",
                                            "ich wünsche mir"};

    private final String[] pause_keywords = {"pause", "stop", "stopp", "anhalten", "halt", "aus"};

    private final String[] resume_keywords = {"resume", "weiter", "an", "play", "wiedergabe", "start"};

    private final String[] next_keywords = {"next", "nächster", "nächstes", "lied weiter", "song weiter", "vor"};

    private final String[] prev_keywords = {"previous", "vorheriger", "vorheriges", "zurück", "back"};

    private final String[] shuffle_keywords = {"random", "shuffle", "zufällig", "zufällige wiedergabe"};

    private final String[] volumeUp_keywords = {"lauter", "louder", "zu leise"};

    private final String[] volumeDown_keywords = {"leiser", "zu laut", "ruhiger", "stiller"};

    private final String finish_keyword = "bitte";

    public KeywordDictionary() {

    }

}
