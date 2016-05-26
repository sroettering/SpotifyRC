package com.hci.sroettering.spotifyrc;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sroettering on 15.05.16.
 *
 * Message types:
 * command msg = <cmdName>[;<additionalData>] // e.g. play;artist;artistID / next / prev / pause
 * update msg = <type>;<additionalData> // e.g. play;artistsong / shuffle;0
 * data msg = <dataType>;<data> // e.g. playlist;Vatertag--id;EDM--id;Chillen--id / song;artist--track--duration--id
 * sensor msg =
 */
public class CommunicationManager implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static CommunicationManager instance;

    private final String cmdPath = "/command";
    private final String textCmdPath = "/textCommand";
    private final String updatePath = "/playerUpdate";
    private final String dataPath = "/listData";
    private final String sensorPath = "/sensorData";
    private GoogleApiClient mApiClient;
    private Context mContext;
    private List<MessageListener> listeners;

    private CommunicationManager() {
        listeners = new ArrayList<>();
    }

    public static CommunicationManager getInstance() {
        if(instance == null) {
            instance = new CommunicationManager();
        }
        return instance;
    }

    public void setContext(Context context) {
        mContext = context;
        init();
    }

    public void addListener(MessageListener msgListener) {
        if(!listeners.contains(msgListener)) {
            listeners.add(msgListener);
        }
    }

    private void init() {
        mApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mApiClient, this);
        Log.d("CommunicationManager", "Connected to Watch");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("CommunicationManager", "Connection suspended");
    }

    @Override
    public void onMessageReceived(MessageEvent msgEvent) {
        Log.d("CommunicationManager", "Received Watch Message " + msgEvent.getPath());
        String msgPath = msgEvent.getPath();
        String msg = new String(msgEvent.getData());
        Log.d("CommunicationManager", "Message: " + msg);
        if (msgPath.equals(sensorPath)) {
            for (MessageListener msgListener : listeners) {
                msgListener.onSensorMessage(msg);
            }
        } else if (msgPath.equals(cmdPath)) {
            for (MessageListener msgListener : listeners) {
                msgListener.onCommandMessage(msg);
            }
        } else if(msgPath.equals(textCmdPath)) {
            for (MessageListener msgListener : listeners) {
                msgListener.onTextCommandMessage(msg);
            }
        } else if(msgPath.equals(updatePath)) {
            for(MessageListener msgListener: listeners) {
                msgListener.onUpdateMessage(msg);
            }
        } else if(msgPath.equals(dataPath)) {
            // should never happen on handheld
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("CommunicationManager", "Connection failed: " + connectionResult.getErrorMessage());
    }

    public void onStop() {
        if(mApiClient != null && mApiClient.isConnected()) {
            Wearable.MessageApi.removeListener(mApiClient, this);
            mApiClient.disconnect();
        }
    }


    // Sending messages

    public void sendTrackUpdate(String artistName, String songName) {
        String update = "play;" + artistName + " - \n" + songName;
        sendMessage(updatePath, update);
    }

    public void sendResume() {
        String update = "resume";
        sendMessage(updatePath, update);
    }

    public void sendPause() {
        String update = "pause";
        sendMessage(updatePath, update);
    }

    public void sendShuffle(boolean isEnabled) {
        String update = "shuffle;" + (isEnabled ? "1" : "0");
        sendMessage(updatePath, update);
    }

    public void sendData(String type, String content) {
        String data = type + "" + content;
        sendMessage(dataPath, data);
    }

    // Sends the state of the toggle buttons to the watch
    public void sendGUIState(boolean isShuffleEnabled, boolean isPlaying) {
        String msg = "";
        msg = "shuffle;" + (isShuffleEnabled? "1" : "0");
        sendMessage(updatePath, msg);

        msg = isPlaying? "resume" : "pause";
        sendMessage(updatePath, msg);
    }


    private void sendMessage(final String path, final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
                for(Node n: nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, n.getId(), path, text.getBytes()).await();
                    Log.d("CommunicationManager", result.getStatus().toString());
                }
            }
        }).start();
    }

    // Must be implemented to receive messages
    public interface MessageListener {
        void onCommandMessage(String msg);
        void onTextCommandMessage(String msg);
        void onSensorMessage(String msg);
        void onUpdateMessage(String msg);
    }

}
