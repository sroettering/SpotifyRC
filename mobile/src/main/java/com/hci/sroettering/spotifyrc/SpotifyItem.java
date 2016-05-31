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
import kaaes.spotify.webapi.android.models.PlaylistsPager;
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
 * This class converts every possible type of spotify data (playlist, song, album, artist and category)
 * into one single type.
 * It saves the original name, the id, the type of item and the audio features of the song(s)
 * Would be nice to have gui operate based on this class...
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
     "liveness": 0.159, // most likely not useful
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
    public int type; // 0 = playlist; 1 = song; 2 = album; 3 = artist; 4 = category

    private SpotifyManager spotifyManager = SpotifyManager.getInstance();

    private SpotifyItem(int type, String id, String text) {
        this.type = type;
        this.spotifyID = id;
        this.text = text.toLowerCase();
    }

    public SpotifyItem(PlaylistSimple playlist) {
        this(0, playlist.id, playlist.name);
    }

    // saving the artist name in the text, makes it possible to find non saved artists
    public SpotifyItem(SavedTrack track) {
        this(1, track.track.id, track.track.artists.get(0).name + " - " + track.track.name);
    }

    public SpotifyItem(SavedAlbum album) {
        this(2, album.album.id, album.album.name);
    }

    public SpotifyItem(Artist artist) {
        this(3, artist.id, artist.name);
    }

    public SpotifyItem(final Category category) {
        this(4, category.id, category.name);
    }

}
