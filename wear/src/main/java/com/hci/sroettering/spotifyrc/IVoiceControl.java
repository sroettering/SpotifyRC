package com.hci.sroettering.spotifyrc;

/**
 * Created by sroettering on 23.05.16.
 */
public interface IVoiceControl {

    public void processVoiceCommands(String... voiceCommands);

    public void restartListeningService();

}
