package com.hci.sroettering.spotifyrc;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks;
import kaaes.spotify.webapi.android.models.Category;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.SavedAlbum;
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TrackSimple;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by sroettering on 26.05.16.
 *
 *
 * Example Audio Feature:
     "danceability": 0.735,
     "energy": 0.578,
     "key": 5,
     "loudness": -11.84,
     "mode": 0,
     "speechiness": 0.0461,
     "acousticness": 0.514,
     "instrumentalness": 0.0902,
     "liveness": 0.159,
     "valence": 0.624,
     "tempo": 98.002,
     "type": "audio_features",
     "id": "06AKEBrKUckW0KREUWRnvT",
     "uri": "spotify:track:06AKEBrKUckW0KREUWRnvT",
     "track_href": "https://api.spotify.com/v1/tracks/06AKEBrKUckW0KREUWRnvT",
     "analysis_url": "http://echonest-analysis.s3.amazonaws.com/TR/xZIVRgimIx9_iJFqTriVhCm_4unjh7tZAglpO5D-xS4xNkvxq70uCFAtuoVYTaIeHbWoLKvCB6W-kvd9E=/3/full.json?AWSAccessKeyId=AKIAJRDFEY23UEVW42BQ&Expires=1455893394&Signature=rmceqCXLMbPrXt9RTIJwk%2BQzxoY%3D",
     "duration_ms": 255349,
     "time_signature": 4

    see https://developer.spotify.com/web-api/get-audio-features/ for explanations
 *
 */
public class SpotifyItem {

    public String text;
    public String spotifyID;
    public int type; // 0 = playlist; 1 = album; 2 = song; 3 = artist; 4 = category
    public AudioFeaturesTracks features;

    private SpotifyManager spotifyManager = SpotifyManager.getInstance();

    public SpotifyItem(PlaylistSimple playlist) {
        this.type = 0;
        this.spotifyID = playlist.id;
        this.text = playlist.name;
        ArrayList<TrackSimple> tracks = new ArrayList<>();
        getTracksForPlaylist(tracks, playlist.owner.id, spotifyID);
        String ids = "";
        for(TrackSimple track: tracks) {
            ids += track.id + ",";
        }
        retrieveAudioFeaturesForTracks(ids);
    }

    public SpotifyItem(SavedAlbum album) {
        this.type = 1;
        this.spotifyID = album.album.id;
        this.text = album.album.name;
        String ids = "";
        for(TrackSimple track: album.album.tracks.items) {
            ids += track.id + ",";
        }
        retrieveAudioFeaturesForTracks(ids);
    }

    public SpotifyItem(SavedTrack track) {
        this.type = 2;
        this.spotifyID = track.track.id;
        this.text = track.track.name;
        retrieveAudioFeaturesForTracks(spotifyID);
    }

    public SpotifyItem(Artist artist) {
        this.type = 3;
        this.spotifyID = artist.id;
        this.text = artist.name;
        ArrayList<TrackSimple> tracks = getTracksForArtist(spotifyID);
        String ids = "";
        for(TrackSimple track: tracks) {
            ids += track.id + ",";
        }
    }

    public SpotifyItem(Category category) {
        this.type = 4;
        this.spotifyID = category.id;
        this.text = category.name;

    }

    private void retrieveAudioFeaturesForTracks(String ids) {
        spotifyManager.getSpotifyService().getTracksAudioFeatures(ids, new Callback<AudioFeaturesTracks>() {
            @Override
            public void success(AudioFeaturesTracks audioFeaturesTracks, Response response) {
                features = audioFeaturesTracks;
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("Spotify", "Error while retrieving audio features: " + error.getMessage());
            }
        });
    }

    private void getTracksForPlaylist(final ArrayList<TrackSimple> tracks, String ownerID, String playlistID) {
        spotifyManager.getSpotifyService().getPlaylist(ownerID, playlistID, new Callback<Playlist>() {
            @Override
            public void success(Playlist playlist, Response response) {
                for (PlaylistTrack ptrack : playlist.tracks.items) {
                    tracks.add(ptrack.track);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SpotifyManager", "Error retrieving tracks for playlist: " + error.getMessage());
            }
        });
    }

    private ArrayList<TrackSimple> getTracksForArtist(String id) {
        SpotifyManager.ListDataContainer ldc = spotifyManager.getLdc();
        List<SavedTrack> allTracks = ldc.getSongs();
        ArrayList<TrackSimple> artistTracks = new ArrayList<>();

        //filter songs by artist
        for(SavedTrack track: allTracks) {
            for(ArtistSimple a: track.track.artists) {
                if (a.id.equals(id)) {
                    artistTracks.add(track.track);
                }
            }
        }
        return artistTracks;
    }

}
