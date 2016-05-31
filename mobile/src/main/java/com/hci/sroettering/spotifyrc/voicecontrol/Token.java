package com.hci.sroettering.spotifyrc.voicecontrol;

/**
 * Created by sroettering on 27.05.16.
 */
public class Token {

    public String keyword;
    public Type type;

    public Token(String keyword, Type type) {
        this.keyword = keyword;
        this.type = type;
    }

    public enum Type {

        PAUSE(0), RESUME(1), NEXT(2), PREV(3), VOLUMEUP(4), VOLUMEDOWN(5), // simple focused types
        NEGATIVE(6), POSITIVE(7), // on/off types
        SHUFFLE(8), PLAY(9), EXTRA(10), // complex focused types
        CASUAL_AUDIO_FEATURE(11), // casual types
        CASUAL_WEAK(12), CASUAL_STRONG(13), CASUAL_NEUTRAL(14),
        CASUAL_NEGATIVE(15), CASUAL_POSITIVE(16);


        private final int value;

        Type(final int value) {
            this.value = value;
        }

        public int getValue() { return value; }

    }

}
