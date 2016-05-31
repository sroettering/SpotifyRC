package com.hci.sroettering.spotifyrc;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import kaaes.spotify.webapi.android.models.AudioFeaturesTrack;
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks;
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TrackSimple;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by sroettering on 30.05.16.
 */
public class AudioFeatureDatabase {

    // would be nice to have artist and track name for every audio feature
    // to have a nice output

    private SpotifyManager mSpotifyManager = SpotifyManager.getInstance();

    private HashMap<String, AudioFeaturesTrack> audioFeatures; // maybe a hashmap with key = id is better here
    private HashMap<String, TrackSimple> trackCollection;
    private ArrayList<String> trackQueue;
    private boolean isReady;
    private boolean isFinished;

    public AudioFeatureDatabase() {
        audioFeatures = new HashMap<>();
        trackCollection = new HashMap<>();
        trackQueue = new ArrayList<>();
        isReady = false;
        isFinished = false;
    }

    public void setReady() {
        isReady = true;
    }

    private void addTrackIDToQueue(String id) {
        trackQueue.add(id);
    }

    private void addTrackIDsToQueue(List<String> ids) {
        for(String s: ids) {
            if(!trackQueue.contains(s)) {
                trackQueue.add(s);
            }
        }
        //trackQueue.addAll(ids);
    }

    public void addTracks(List tracks) {
        List<String> ids = new ArrayList<>();
        for(Object track: tracks) {
            if(track instanceof TrackSimple) {
                ids.add(((TrackSimple) track).id);
                trackCollection.put(((TrackSimple) track).id, (TrackSimple) track);
            } else if(track instanceof SavedTrack) {
                ids.add(((SavedTrack) track).track.id);
                trackCollection.put(((SavedTrack) track).track.id, ((SavedTrack) track).track);
            }
        }
        addTrackIDsToQueue(ids);
    }

    public void retrieveAudioFeaturesForQueuedTracks() {
        if (!isReady) return;

        // spotify can only handle 100 track ids per request
        // could use trackQueue.sublist() instead
        String ids = "";
        int requestCounter = 1;
        Log.d("AudioFeatureDatabase", "trackQueue.size: " + trackQueue.size());
        for (int i = 0; i < trackQueue.size(); i++) {
            if(i < requestCounter*100) {
                ids += trackQueue.get(i) + ",";
            } else {
                retrieveAudioFeatures(ids);
                ids = trackQueue.get(i) + ",";
                requestCounter++;
            }
        }

        if(!ids.equals("")) {
            retrieveAudioFeatures(ids);
        }

        isFinished = true;
    }

    private void log() {
        if(isFinished) {
            Log.d("AudioFeatureDatabase", "finished loading");
            Log.d("AudioFeatureDatabase", "trackCollection.size: " + trackCollection.size());
            Log.d("AudioFeatureDatabase", "audioFeatures.size: " + audioFeatures.size());
        }
    }

    private void retrieveAudioFeatures(String ids) {
        //Log.d("AudioFeatureDatabase", "sending audioFeature request");
        mSpotifyManager.getSpotifyService().getTracksAudioFeatures(ids, new Callback<AudioFeaturesTracks>() {
            @Override
            public void success(AudioFeaturesTracks audioFeaturesTracks, Response response) {
                addAudioFeatures(audioFeaturesTracks);
                log();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("Spotify", "Error while retrieving audio features: " + error.getMessage());
            }
        });
    }

    private void addAudioFeatures(AudioFeaturesTracks features) {
        for(AudioFeaturesTrack aft: features.audio_features) {
            addAudioFeature(aft);
        }
    }

    private void addAudioFeature(AudioFeaturesTrack feature) {
        audioFeatures.put(feature.id, feature);
    }

    private String getSongNameAndArtistForID(String id) {
        TrackSimple track = trackCollection.get(id);
        return track.artists.get(0).name + " - " + track.name;
    }

    // create audio feature spreadsheet
    public void createAudioFeatureSpreadsheet() {
        String s = "";
        s += "Song;id;tempo;valence;loudness;energy;danceability;\n";
        for(String id: audioFeatures.keySet()) {
            AudioFeaturesTrack audioFeature = audioFeatures.get(id);
            if(audioFeature == null) continue;
            s += getSongNameAndArtistForID(id) + ";";
            s += id + ";";
            s += audioFeature.tempo + ";";
            s += audioFeature.valence + ";";
            s += audioFeature.loudness + ";";
            s += audioFeature.energy + ";";
            s += audioFeature.danceability + ";";
            s += "\n";
        }
        generateNoteOnSD(s.replaceAll("[.]", ","));
    }

    public void generateNoteOnSD(String sBody) {
        try {
            File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/SpotifyRC");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, "AudioFeatures.csv");
            FileOutputStream f = new FileOutputStream(gpxfile);
            PrintWriter pw = new PrintWriter(f);
            pw.println(sBody);
            pw.flush();
            pw.close();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
