package com.hci.sroettering.spotifyrc;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import kaaes.spotify.webapi.android.models.AudioFeaturesTrack;
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks;
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.TrackSimple;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by sroettering on 30.05.16.
 */
public class AudioFeatureDatabase {

    private final String directoryName = "/SpotifyRC";
    private final String fileName = "AudioFeatures.csv";

    private SpotifyManager mSpotifyManager = SpotifyManager.getInstance();

    private String[] featureNames = {"tempo", "stimmung", "energie", "tanzbarkeit", "lautstärke"}; // loudness at the end
    private HashMap<String, Float> maxValues;
    private HashMap<String, Float> minValues;
    private HashMap<String, AudioFeaturesTrack> audioFeatures; // maybe a hashmap with key = id is better here
    private HashMap<String, TrackSimple> trackCollection;
    private ArrayList<String> trackQueue;
    private boolean isReady;
    private boolean newFeaturesAdded;

    public AudioFeatureDatabase() {
        maxValues = new HashMap<>();
        minValues = new HashMap<>();
        initValues();
        audioFeatures = new HashMap<>();
        trackCollection = new HashMap<>();
        trackQueue = new ArrayList<>();
        isReady = false;
        newFeaturesAdded = false;
        initFromCache();
    }

    private void initValues() {
        for(int i = 0; i < featureNames.length - 1; i++) {
            minValues.put(featureNames[i], 2f);
            maxValues.put(featureNames[i], -1f);
        }
        minValues.put(featureNames[featureNames.length-1], -100f); // loudness is measured in db
        maxValues.put(featureNames[featureNames.length-1], 0f);
    }

    public float getFeatureForID(String type, String id) {
        AudioFeaturesTrack aft = audioFeatures.get(id);
        float value = 0f;
        if(type.equals("tempo")) {
            value = aft.tempo;
        } else if(type.equals("stimmung")) {
            value = aft.valence;
        } else if(type.equals("lautstärke")) {
            value = aft.loudness;
        } else if(type.equals("energie")) {
            value = aft.energy;
        } else if(type.equals("tanzbarkeit")) {
            value = aft.danceability;
        }
        return value;
    }

    public List<TrackSimple> getTracksWithFeature(String type, float multiplier) {
        float maxValue = maxValues.get(type);
        float minValue = minValues.get(type);
        float currentValue = mSpotifyManager.getAudioFeatureForCurrentTrack(type);
        float newValue = 0f;
        float tolerance = 0.075f;

        // scale difference between either top or bottom limit with multiplier and add to current value
        if(multiplier < 0) {
            newValue = (currentValue - minValue) * multiplier + currentValue;
        } else {
            newValue = (maxValue - currentValue) * multiplier + currentValue;
        }

        float value = 0f;
        List<TrackSimple> tracksInRange = new ArrayList<>();
        for(String id: audioFeatures.keySet()) {
            AudioFeaturesTrack aft = audioFeatures.get(id);
            if(type.equals("tempo")) {
                value = aft.tempo;
            } else if(type.equals("stimmung")) {
                value = aft.valence;
            } else if(type.equals("lautstärke")) {
                value = aft.loudness;
            } else if(type.equals("energie")) {
                value = aft.energy;
            } else if(type.equals("tanzbarkeit")) {
                value = aft.danceability;
            }

            // find songs with desired feature with value=newValue +/- tolerance [%]
            if(value > newValue - newValue*tolerance && value < newValue + newValue*tolerance) {
                tracksInRange.add(trackCollection.get(id));
            }
        }
        return tracksInRange;
    }

    public void setReady() {
        isReady = true;
    }

    private void addTrackIDsToQueue(List<String> ids) {
        for(String s: ids) {
            // dont add duplicate ids and dont add ids for which an audio feature entry already exists
            if(!trackQueue.contains(s) && !audioFeatures.containsKey(s)) {
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

        // throw out audio features when there is no track in current library
        Log.d("AudioFeatureDatabase", "Sync - Number of Tracks: " + trackCollection.size());
        Log.d("AudioFeatureDatabase", "Sync - Number of Features: " + audioFeatures.size());
        HashMap<String, AudioFeaturesTrack> tempMap = new HashMap<>();
        for(String key: trackCollection.keySet()) {
            if(audioFeatures.containsKey(key)) tempMap.put(key, audioFeatures.get(key));
        }
        Log.d("AudioFeatureDatabase", "Sync - Number of common features: " + tempMap.size());
        audioFeatures = tempMap;

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
        } else {
            finishedRetrievingAudioFeatures();
        }

    }

    private void finishedRetrievingAudioFeatures() {
        // only if the trackCollection and the audioFeatures maps have the same size, the retrieval is complete
        if(trackCollection.size() == audioFeatures.size()) {
            Log.d("AudioFeatureDatabase", "finished retrieving audio features");
            Log.d("AudioFeatureDatabase", "Number of Tracks: " + trackCollection.size());
//            for(int i = 0; i < featureNames.length; i++) {
//                String featureName = featureNames[i];
//                Log.d("AudioFeatureDatabase", "values for " + featureName + ": " + minValues.get(featureName) + "; " + maxValues.get(featureName));
//            }

            // if audioFeatures contains new items not in the "cache-file", write new "cache-file"
            if(newFeaturesAdded) {
                saveAudioFeaturesToCache();
                newFeaturesAdded = false;
            }
        }
//        else if(trackCollection.size() < audioFeatures.size()) {
//            // tracks must have been deleted from spotify, so they dont need to reside in the cache
//            // assumptions:
//            // trackCollection is filled before audioFeatures
//            // audioFeatures have to be completely loaded from cache and/or from spotify,
//            // so trackCollection does not contain keys not in audioFeatures
//
//            HashMap<String, AudioFeaturesTrack> tempMap = new HashMap<>();
//            for(String key: trackCollection.keySet()) {
//                // put only needed features into the new map
//                tempMap.put(key, audioFeatures.get(key));
//            }
//            audioFeatures = tempMap;
//            saveAudioFeaturesToCache();
//            newFeaturesAdded = false;
//        }
    }

    private void retrieveAudioFeatures(String ids) {
        Log.d("AudioFeatureDatabase", "sending audioFeature request");
        mSpotifyManager.getSpotifyService().getTracksAudioFeatures(ids, new Callback<AudioFeaturesTracks>() {
            @Override
            public void success(AudioFeaturesTracks audioFeaturesTracks, Response response) {
                newFeaturesAdded = true; // obviously new features are added
                addAudioFeatures(audioFeaturesTracks);
                finishedRetrievingAudioFeatures();
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
        if(feature.tempo < minValues.get("tempo")) minValues.put("tempo", feature.tempo);
        if(feature.tempo > maxValues.get("tempo")) maxValues.put("tempo", feature.tempo);

        if(feature.valence < minValues.get("stimmung")) minValues.put("stimmung", feature.valence);
        if(feature.valence > maxValues.get("stimmung")) maxValues.put("stimmung", feature.valence);

        if(feature.energy < minValues.get("energie")) minValues.put("energie", feature.energy);
        if(feature.energy > maxValues.get("energie")) maxValues.put("energie", feature.energy);

        if(feature.danceability < minValues.get("tanzbarkeit")) minValues.put("tanzbarkeit", feature.danceability);
        if(feature.danceability > maxValues.get("tanzbarkeit")) maxValues.put("tanzbarkeit", feature.danceability);

        if(feature.loudness > minValues.get("lautstärke")) minValues.put("lautstärke", feature.loudness);
        if(feature.loudness < maxValues.get("lautstärke")) maxValues.put("lautstärke", feature.loudness);
    }

    private String getSongNameAndArtistForID(String id) {
        TrackSimple track = trackCollection.get(id);
        String artistName = track.artists.get(0).name.replace(";", ""); // replace ';' to not mess up the csv file
        String trackName = track.name.replace(";", "");
        return artistName + " - " + trackName;
    }

    public void saveAudioFeaturesToCache() {
        Log.d("AudioFeatureDatabase", "Saving audio features to file");
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
        SpotifyCache.saveAudioFeatures(s.replaceAll("[.]", ","));
    }

    public void initFromCache() {
        Log.d("AudioFeatureDatabase", "loading audio features from file");

        if(!SpotifyCache.hasAudioFeatures()) {
            return;
        }

        List<String> fileContent = SpotifyCache.getAudioFeatures();

        // creating not completed AudioFeaturesTrack objects from file data
        AudioFeaturesTrack aft;
        String[] cells;
        int itemCount = 0;
        for (String line : fileContent) {
            if ("".equals(line) || line.contains("Song;id;tempo;valence;loudness;energy;danceability;")) {
                continue;
            }
            line = line.replace(",", ".");
            aft = new AudioFeaturesTrack();
            cells = line.split(";");
            //Log.d("AudioFeatureDatabase", line + " split length: " + cells.length);
            aft.id = cells[1];
            aft.tempo = Float.parseFloat(cells[2]);
            aft.valence = Float.parseFloat(cells[3]);
            aft.loudness = Float.parseFloat(cells[4]);
            aft.energy = Float.parseFloat(cells[5]);
            aft.danceability = Float.parseFloat(cells[6]);
            itemCount++;

            audioFeatures.put(aft.id, aft);
        }

        Log.d("AudioFeatureDatabase", "finished. loaded " + itemCount + " features from file");

    }

}
