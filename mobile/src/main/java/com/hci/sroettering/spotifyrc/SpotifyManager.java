package com.hci.sroettering.spotifyrc;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.ArtistsCursorPager;
import kaaes.spotify.webapi.android.models.CategoriesPager;
import kaaes.spotify.webapi.android.models.Category;
import kaaes.spotify.webapi.android.models.Pager;
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
 * Created by sroettering on 12.04.16.
 */
public class SpotifyManager implements PlayerNotificationCallback, ConnectionStateCallback, PlayerStateCallback {

    // Spotify Request Codes
    private static final int LOGIN_RC = 1337;

    // Spotify Application Client ID
    private static final String CLIENT_ID = "5092c0cbcb7140b4a39622edfe8b6305";

    // Spotify Application Redirect URI
    private static final String REDIRECT_URI = "SpotifyRCLogin://login";

    // Singleton Instance
    private static SpotifyManager instance;

    // current Activity
    private Activity mContext;

    private CommunicationManager commManager;

    private Timer progressUpdateTimer = null;

    // Spotify
    private String accessToken = "";
    private SpotifyApi spotifyApi;
    private SpotifyService spotify = null;
    private Player mPlayer;
    private PlayerState playerState;
    private boolean isShuffle;
    private Map<String, Object> options;
    private final int pagerLimit = 50; // maximum is 50

    // List Info
    private ListDataContainer ldc;

    // Connecting current player TrackURI list to track objects
    private HashMap<String, TrackSimple> currentQueue;


    // Initialization

    private SpotifyManager() {
        spotifyApi = new SpotifyApi();
        isShuffle = false;
        ldc = new ListDataContainer();
        currentQueue = new HashMap<String, TrackSimple>();
        options = new HashMap<>();
        options.put(SpotifyService.LIMIT, pagerLimit);
    }

    public static SpotifyManager getInstance() {
        if(instance == null) {
            instance = new SpotifyManager();
        }
        return instance;
    }

    public void setContext(Activity context) {
        this.mContext = context;
    }

    public void setCommunicationManager(CommunicationManager manager) {
        commManager = manager;
    }

    public ListDataContainer getLdc() {
        return this.ldc;
    }

    public void login() {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "user-follow-read", "user-library-read", "playlist-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(mContext, LOGIN_RC, request);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Check if result comes from the correct activity
        if (requestCode == LOGIN_RC) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                accessToken = response.getAccessToken();
                spotifyApi.setAccessToken(accessToken);
                spotify = spotifyApi.getService();
                init();
            } else {
                Log.d("SpotifyManager", "Error: " + response.getError() );
            }
        }
    }

    private void init() {
        initPlayer();
        initPlaylists();
        initSongs();
        initAlbums();
        initArtists();
        initCategories();
    }

    private void initPlayer() {
        if(accessToken != "") {
            Config playerConfig = new Config(mContext, accessToken, CLIENT_ID);
            Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                @Override
                public void onInitialized(Player player) {
                    mPlayer = player;
                    mPlayer.addConnectionStateCallback(SpotifyManager.this);
                    mPlayer.addPlayerNotificationCallback(SpotifyManager.this);
                    mPlayer.setShuffle(isShuffle);
                    mPlayer.setRepeat(true);
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("SpotifyManager", "Could not initialize player: " + throwable.getMessage());
                }
            });
        }
    }

    private void initPlaylists() {
        spotify.getMyPlaylists(options, new Callback<Pager<PlaylistSimple>>() {
            @Override
            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                ldc.setPlaylists(playlistSimplePager.items);
                ((MainActivity) mContext).updateData(ldc.getPlaylists(), 0);
                updateWatchData();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SpotifyManager", "Error retrieving playlists: " + error.getMessage());
            }
        });
    }

    private void initSongs() {
        spotify.getMySavedTracks(options, new Callback<Pager<SavedTrack>>() {
            @Override
            public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                ldc.setSongs(savedTrackPager.items);
                ((MainActivity) mContext).updateData(ldc.getSongs(), 1);
                updateWatchData();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SpotifyManager", "Error retrieving songs: " + error.getMessage());
            }
        });
    }

    private void initAlbums() {
        spotify.getMySavedAlbums(options, new Callback<Pager<SavedAlbum>>() {
            @Override
            public void success(Pager<SavedAlbum> savedAlbumPager, Response response) {
                ldc.setAlbums(savedAlbumPager.items);
                ((MainActivity) mContext).updateData(ldc.getAlbums(), 2);
                updateWatchData();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SpotifyManager", "Error retrieving albums: " + error.getMessage());
            }
        });
    }

    private void initArtists() {
        spotify.getFollowedArtists(options, new Callback<ArtistsCursorPager>() {
            @Override
            public void success(ArtistsCursorPager artistCursorPager, Response response) {
                ldc.setArtists(artistCursorPager.artists.items);
                ((MainActivity) mContext).updateData(ldc.getArtists(), 3);
                updateWatchData();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SpotifyManager", "Error retrieving artists: " + error.getMessage());
            }
        });
    }

    private void initCategories() {
        spotify.getCategories(options, new Callback<CategoriesPager>() {
            @Override
            public void success(CategoriesPager categoriesPager, Response response) {
                ldc.setCategories(categoriesPager.categories.items);
                ((MainActivity) mContext).updateData(ldc.getCategories(), 4);
                updateWatchData();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SpotifyManager", "Error retrieving categories: " + error.getMessage());
            }
        });
    }

    public void updateWatchData() {
        Log.d("SpotifyManager", "updating watch data");
        String msg = "";
        for(Category category: ldc.getCategories()) {
            msg += ";" + category.name + "--" + category.id;
        }
        if(!msg.equals(""))commManager.sendData("category", msg);

        msg = "";
        for (Artist artist : ldc.getArtists()) {
            msg += ";" + artist.name + "--" + artist.id;
        }
        if(!msg.equals(""))commManager.sendData("artist", msg);

        msg = "";
        for (SavedAlbum album : ldc.getAlbums()) {
            msg += ";" + album.album.name + "--" + album.album.id;
        }
        if(!msg.equals(""))commManager.sendData("album", msg);

        msg = "";
        for(PlaylistSimple playlist: ldc.getPlaylists()) {
            msg += ";" + playlist.name + "--" + playlist.id;
        }
        if(!msg.equals(""))commManager.sendData("playlist", msg);

        msg = "";
        for (SavedTrack track : ldc.getSongs()) {
            msg += ";" + track.track.artists.get(0).name + " - " + track.track.name
                    + "--" + MainActivity.formatMilliseconds((int) (track.track.duration_ms / 1000))
                    + "--" + track.track.id;
        }
        if(!msg.equals(""))commManager.sendData("song", msg);
    }


    // Playback

    public void play(Track track) {
        if(mPlayer.isInitialized()) {
            currentQueue.clear();
            currentQueue.put(track.uri, track);
            mPlayer.play(track.uri);
            startTimer();
        }
    }

    public void play(List<TrackSimple> tracks) {
        if(mPlayer.isInitialized()) {
            currentQueue.clear();
            for (TrackSimple track : tracks) {
                currentQueue.put(track.uri, track);
            }
            mPlayer.play(keySetToList());
            mPlayer.setShuffle(isShuffle);
            startTimer();
        }
    }

    public void play(String type, String id) {
        int pos = -1;
        if(type.equals("playlist")) {
            pos = ldc.getPosForPlaylistID(id);
            if(pos != -1) loadPlaylist(pos);
        } else if(type.equals("song")) {
            pos = ldc.getPosForSongID(id);
            if(pos != -1) loadSong(pos);
        } else if(type.equals("album")) {
            pos = ldc.getPosForAlbumID(id);
            if(pos != -1) loadAlbum(pos);
        } else if(type.equals("artist")) {
            pos = ldc.getPosForArtistID(id);
            if(pos != -1) loadArtist(pos);
        } else if(type.equals("category")) {
            pos = ldc.getPosForCategoryID(id);
            if(pos != -1) loadCategory(pos);
        }
    }

    public void resume() {
        if(mPlayer.isInitialized()) {
            mPlayer.getPlayerState(SpotifyManager.this);
            if(playerState != null && playerState.trackUri != null && playerState.trackUri != ""
                    && !playerState.playing) {
                // resume called on playing device apparently skips to next track (headphone support)
                mPlayer.resume();
                startTimer();
            }
        }
    }

    public void pause() {
        if(mPlayer.isInitialized() && playerState != null && playerState.activeDevice) {
            mPlayer.pause();
            stopTimer();
        }
    }

    public void nextTrack() {
        if(mPlayer.isInitialized() && playerState != null && playerState.activeDevice) {
            mPlayer.skipToNext();
            startTimer();
        }
    }

    public void prevTrack() {
        if(mPlayer.isInitialized() && playerState != null && playerState.activeDevice) {
            mPlayer.skipToPrevious();
            startTimer();
        }
    }

    public void shuffle(boolean isEnabled) {
        isShuffle = isEnabled;
        if(mPlayer.isInitialized() && playerState != null && playerState.activeDevice) {
            mPlayer.setShuffle(isShuffle);
        }
    }

    public void loadPlaylist(int pos) {
        String ownerID = ldc.getPlaylists().get(pos).owner.id;
        String playlistID = ldc.getPlaylists().get(pos).id;
        loadPlaylistTracks(ownerID, playlistID);
    }

    public void loadSong(int pos) {
        play(ldc.getSongs().get(pos).track);
    }

    public void loadAlbum(int pos) {
        List<TrackSimple> tracks = ldc.getAlbums().get(pos).album.tracks.items;
        play(tracks);
    }

    public void loadArtist(int pos) {
        Artist artist = ldc.getArtists().get(pos);
        List<SavedTrack> allTracks = ldc.getSongs();
        List<TrackSimple> artistTracks = new ArrayList<>();

        //filter songs by artist
        for(SavedTrack track: allTracks) {
            for(ArtistSimple a: track.track.artists) {
                if (a.id.equals(artist.id)) {
                    //Log.d("loadArtist", "found track for artist: " + artist.name);
                    artistTracks.add(track.track);
                }
            }
        }

        if(!artistTracks.isEmpty())
            play(artistTracks);
    }

    public void loadCategory(int pos) {
        String categoryID = ldc.getCategories().get(pos).id;
        spotify.getPlaylistsForCategory(categoryID, null, new Callback<PlaylistsPager>() {

            @Override
            public void success(PlaylistsPager playlistsPager, Response response) {
                PlaylistSimple playlist = playlistsPager.playlists.items.get(0); // play first playlist for simplicity
                Log.d("SpotifyManager", "Loading playlist: " + playlist.name);
                loadPlaylistTracks(playlist.owner.id, playlist.id);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SpotifyManager", "Failed to retrieve Playlists for selected Category");
            }
        });

    }

    //retrieve playlist tracks from spotify
    private void loadPlaylistTracks(String ownerID, String playlistID) {
        spotify.getPlaylist(ownerID, playlistID, new Callback<Playlist>() {
            @Override
            public void success(Playlist playlist, Response response) {
                ArrayList<TrackSimple> tracks = new ArrayList<TrackSimple>();
                for (PlaylistTrack ptrack : playlist.tracks.items) {
                    tracks.add(ptrack.track);
                }
                play(tracks);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SpotifyManager", "Error retrieving tracks for playlist: " + error.getMessage());
            }
        });
    }

    public SpotifyService getSpotifyService() {
        return spotify;
    }


    // Callbacks

    public void onDestroy() {
        Spotify.destroyPlayer(this);
    }

    @Override
    public void onLoggedIn() {
        Log.d("SpotifyManager", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("SpotifyManager", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable throwable) {
        Log.d("SpotifyManager", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("SpotifyManager", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String s) {
        Log.d("SpotifyManager", "Received connection message: " + s);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState state) {
        Log.d("SpotifyManager", "Playback event received: " + eventType.name());
        if(eventType.name().equals("TRACK_CHANGED")) {
            TrackSimple track = currentQueue.get(state.trackUri);
            ((MainActivity) mContext).updateCurrentTrackInfo(track.artists.get(0).name, track.name, (int)track.duration_ms);
        }
        if(eventType.name().equals("PLAY")) {
            ((MainActivity) mContext).updatePlayButton(true);
        }
        if(eventType.name().equals("END_OF_CONTEXT")) {
            mPlayer.resume();
        }
        playerState = state;
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {
        Log.d("SpotifyManager", "Playback error received: " + errorType.name());
    }

    @Override
    public void onPlayerState(PlayerState state) {
        playerState = state;
        //Log.d("PlayerState", "position: " + playerState.positionInMs);
    }

    // Util
    private List<String> keySetToList() {
        return new ArrayList<String>(currentQueue.keySet());
    }

    private void startTimer() {
        // stop old timer before starting a new one
        if(progressUpdateTimer != null) {
            stopTimer();
        }

        // starting a new timer
        Log.d("Timer", "Starting Timer");
        progressUpdateTimer = new Timer();
        progressUpdateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateProgress();
            }
        }, 250, 400);
    }

    private void stopTimer() {
        if(progressUpdateTimer != null) {
            Log.d("Timer", "Stopping Timer");
            progressUpdateTimer.cancel();
            progressUpdateTimer.purge();
            progressUpdateTimer = null;
        }
    }

    private void updateProgress() {
        mPlayer.getPlayerState(new PlayerStateCallback() {
            @Override
            public void onPlayerState(PlayerState state) {
                playerState = state;
                int duration = playerState.durationInMs;
                int position = playerState.positionInMs;
                //Log.d("updateProgress", "Track Länge: " + duration + "; position: " + position);
                ((MainActivity)mContext).updateProgress(position, duration);
            }
        });
    }


    // Container which holds all the spotify data
    public class ListDataContainer {

        // Lists
        private List<PlaylistSimple> playlists;
        private List<SavedTrack> songs;
        private List<SavedAlbum> albums;
        private List<Artist> artists;
        private List<Category> categories;

        // Lists with spotifyItems
        private List<SpotifyItem>[] spotifyData;

        public ListDataContainer() {
            playlists = new ArrayList<>();
            songs = new ArrayList<>();
            albums = new ArrayList<>();
            artists = new ArrayList<>();
            categories = new ArrayList<>();
            spotifyData = new ArrayList[5];
        }

        public List<PlaylistSimple> getPlaylists() { return playlists; }
        public List<SavedTrack> getSongs() { return songs; }
        public List<SavedAlbum> getAlbums() { return albums; }
        public List<Artist> getArtists() { return artists; }
        public List<Category> getCategories() { return categories; }

        public void setPlaylists(List<PlaylistSimple> list) {
            playlists = list;
            ArrayList<SpotifyItem> tempList = new ArrayList<>();
            for(PlaylistSimple playlist: playlists) {
                tempList.add(new SpotifyItem(playlist));
            }
            spotifyData[0] = tempList;
        }

        public void setSongs(List<SavedTrack> list) {
            songs = list;
            ArrayList<SpotifyItem> tempList = new ArrayList<>();
            for(SavedTrack track: songs) {
                tempList.add(new SpotifyItem(track));
            }
            spotifyData[1] = tempList;
        }

        public void setAlbums(List<SavedAlbum> list) {
            albums = list;
            ArrayList<SpotifyItem> tempList = new ArrayList<>();
            for(SavedAlbum album: albums) {
                tempList.add(new SpotifyItem(album));
            }
            spotifyData[2] = tempList;
        }

        public void setArtists(List<Artist> list) {
            artists = list;
            ArrayList<SpotifyItem> tempList = new ArrayList<>();
            for(Artist artist: artists) {
                tempList.add(new SpotifyItem(artist));
            }
            spotifyData[3] = tempList;
        }

        public void setCategories(List<Category> list) {
            categories = list;
            ArrayList<SpotifyItem> tempList = new ArrayList<>();
            for(Category category: categories) {
                tempList.add(new SpotifyItem(category));
            }
            spotifyData[4] = tempList;
        }

        public List<SpotifyItem>[] getSpotifyData() {
            return this.spotifyData;
        }

        public int getPosForPlaylistID(String id) {
            for(int i = 0; i < playlists.size(); i++) {
                if(playlists.get(i).id.equals(id)) {
                    return i;
                }
            }
            return -1;
        }

        public int getPosForSongID(String id) {
            for(int i = 0; i < songs.size(); i++) {
                if(songs.get(i).track.id.equals(id)) {
                    return i;
                }
            }
            return -1;
        }

        public int getPosForAlbumID(String id) {
            for(int i = 0; i < albums.size(); i++) {
                if(albums.get(i).album.id.equals(id)) {
                    return i;
                }
            }
            return -1;
        }

        public int getPosForArtistID(String id) {
            for(int i = 0; i < artists.size(); i++) {
                if(artists.get(i).id.equals(id)) {
                    return i;
                }
            }
            return -1;
        }

        public int getPosForCategoryID(String id) {
            for(int i = 0; i < categories.size(); i++) {
                if(categories.get(i).id.equals(id)) {
                    return i;
                }
            }
            return -1;
        }

    }

}
