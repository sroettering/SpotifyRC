package com.hci.sroettering.spotifyrc.voicecontrol;

import java.util.ArrayList;

/**
 * Created by sroettering on 29.05.16.
 *
 * This class serves as a data holder during command conversion
 *
 */
public class Command {

    public String text;
    public ArrayList<Token> tokenList;
    public String resultingCommand;

    public Command() {
        this("");
    }

    public Command(String text) {
        this.text = text;
        tokenList = new ArrayList<>();
        resultingCommand = "";
    }

    public boolean hasSimpleFocusedToken() {
        return getSimpleFocusedToken() != null;
    }

    public Token getSimpleFocusedToken() {
        for(Token token: tokenList) {
            // token.type values from 0 to 5 belong to simple commands
            if(token.type.getValue() >= 0 && token.type.getValue() < 6) return token;
        }
        return null;
    }

    public boolean hasComplexFocusedToken() {
        return getComplexFocusedToken() != null;
    }

    public Token getComplexFocusedToken() {
        for(Token token: tokenList) {
            // token.type values from 8 to 9 belong to complex commands (play and shuffle)
            if(token.type.getValue() >= 8 && token.type.getValue() < 10) return token;
        }
        return null;
    }

    public Token getOnOrOffToken() {
        for(Token token: tokenList) {
            // token.type values from 6 to 7 belong to complex commands (play and shuffle)
            if(token.type.getValue() >= 6 && token.type.getValue() < 8) return token;
        }
        return null;
    }

    public Token getExtraToken() {
        for(Token token: tokenList) {
            if(token.type == Token.Type.EXTRA) return token;
        }
        return null;
    }

    public boolean hasCasualAudioFeatureToken() {
        return getCasualAudioFeatureToken() != null;
    }

    public Token getCasualAudioFeatureToken() {
        for(Token token: tokenList) {
            if(token.type == Token.Type.CASUAL_AUDIO_FEATURE) return token;
        }
        return null;
    }

    public Token getMultiplierToken() {
        for(Token token: tokenList) {
            // token.type values from 12 to 14 belong to casual multiplier token (weak, strong and neutral)
            if(token.type.getValue() >= 12 && token.type.getValue() < 15) return token;
        }
        return null;
    }

    public Token getDirectionToken() {
        for(Token token: tokenList) {
            // token.type values from 15 to 16 belong to casual direction token (positive or negative)
            if(token.type.getValue() >= 15 && token.type.getValue() < 17) return token;
        }
        return null;
    }

    public void addToken(String keyword, Token.Type type) {
        tokenList.add(new Token(keyword.trim(), type));
        text = text.replace(keyword, "");
    }

    public void cleanText() {
        this.text = text.replace(KeywordDictionary.politeness_keyword, "")
                .replaceAll("[^a-zA-Z0-9üöäë ]+","")
                .toLowerCase()
                .trim();
    }

}
