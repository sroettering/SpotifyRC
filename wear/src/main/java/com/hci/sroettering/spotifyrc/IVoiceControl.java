package com.hci.sroettering.spotifyrc;

/**
 * Created by sroettering on 23.05.16.
 */
public interface IVoiceControl {

    public void processVoiceCommand(String voiceCommand);

    public void restartListeningService();

    public void onListeningError();

    public void stopListening();

}
