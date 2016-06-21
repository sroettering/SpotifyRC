package com.hci.sroettering.spotifyrc;

import java.util.Date;

/**
 * Created by sroettering on 21.06.16.
 */
public class InputEvent {

    private Date date;
    private String message;

    public InputEvent(String msg) {
        date = new Date();
        message = msg;
    }

    @Override
    public String toString() {
        return date + ": " + message;
    }

}
