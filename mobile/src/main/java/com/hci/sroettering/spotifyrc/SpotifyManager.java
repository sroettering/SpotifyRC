package com.hci.sroettering.spotifyrc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
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
import kaaes.spotify.webapi.android.models.Tracks;
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
    private final int pagerLimit = 50; // maximum is 50

    private int playlistTotal = 0;
    private int albumTotal = 0;
    private int songTotal = 0;
    private int artistTotal = 0;
    private int categoryTotal = 0;

    // List Info
    private ListDataContainer ldc;

    // Connecting current player TrackURI list to track objects
    private HashMap<String, TrackSimple> currentQueue;


    // Initialization

    private SpotifyManager() {
        spotifyApi = new SpotifyApi();
        isShuffle = false;
        currentQueue = new HashMap<String, TrackSimple>();
    }

    public static SpotifyManager getInstance() {
        if(instance == null) {
            Log.d("SpotifyManager", "creating new instance");
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
        ldc = new ListDataContainer(); // do not place in constructor
        Log.d("SpotifyManager", "logging into spotify");
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
        Log.d("SpotifyManager", "initializing SpotifyManager");
        initPlayer();
        initPlaylists();
        // parallel requests might be bad in terms of rate limiting from spotify
//        initSongs();
//        initAlbums();
//        initArtists();
//        initCategories();
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
                    Log.d("SpotifyManager", "Initialized Player");
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("SpotifyManager", "Could not initialize player: " + throwable.getMessage());
                }
            });
        }
    }

    private void initPlaylists() {
        retrievePlaylists(0);
    }

    private void retrievePlaylists(final int offset) {
        HashMap<String, Object> options = new HashMap<>();
        options.put(SpotifyService.LIMIT, pagerLimit);
        options.put(SpotifyService.OFFSET, offset);

        spotify.getMyPlaylists(options, new Callback<Pager<PlaylistSimple>>() {
            @Override
            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                ldc.setPlaylists(playlistSimplePager.items);
                ((MainActivity) mContext).updateData(ldc.getPlaylists(), 0);
                //updateWatchData();
                playlistTotal = playlistSimplePager.total;
                if (ldc.getPlaylists().size() < playlistTotal) {
                    retrievePlaylists(offset + pagerLimit);
                } else {
                    Log.d("SpotifyManager", "Initialized Playlists");
                    initSongs();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SpotifyManager", "Error retrieving playlists: " + error.getMessage());
            }
        });
    }

    private void initSongs() {
        retrieveSongs(0);
    }

    private void retrieveSongs(final int offset) {
        HashMap<String, Object> options = new HashMap<>();
        options.put(SpotifyService.LIMIT, pagerLimit);
        options.put(SpotifyService.OFFSET, offset);

        spotify.getMySavedTracks(options, new Callback<Pager<SavedTrack>>() {
            @Override
            public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                ldc.setSongs(savedTrackPager.items);
                ((MainActivity) mContext).updateData(ldc.getSongs(), 1);
                //updateWatchData();
                songTotal = savedTrackPager.total;
                if (ldc.getSongs().size() < songTotal) {
                    retrieveSongs(offset + pagerLimit);
                } else {
                    Log.d("SpotifyManager", "Initialized Saved Tracks");
                    initAlbums();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SpotifyManager", "Error retrieving songs: " + error.getMessage());
            }
        });
    }

    private void initAlbums() {
        retrieveAlbums(0);
    }

    private void retrieveAlbums(final int offset) {
        HashMap<String, Object> options = new HashMap<>();
        options.put(SpotifyService.LIMIT, pagerLimit);
        options.put(SpotifyService.OFFSET, offset);

        spotify.getMySavedAlbums(options, new Callback<Pager<SavedAlbum>>() {
            @Override
            public void success(Pager<SavedAlbum> savedAlbumPager, Response response) {
                ldc.setAlbums(savedAlbumPager.items);
                ((MainActivity) mContext).updateData(ldc.getAlbums(), 2);
                //updateWatchData();
                albumTotal = savedAlbumPager.total;
                if (ldc.getAlbums().size() < albumTotal) {
                    retrieveAlbums(offset + pagerLimit);
                } else {
                    Log.d("SpotifyManager", "Initialized Saved Albums");
                    initArtists();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SpotifyManager", "Error retrieving albums: " + error.getMessage());
            }
        });
    }

    private void initArtists() {
        retrieveArtists(0);
    }

    private void retrieveArtists(final int offset) {
        HashMap<String, Object> options = new HashMap<>();
        options.put(SpotifyService.LIMIT, pagerLimit);
        options.put(SpotifyService.OFFSET, offset);

        // ArtistsCursorPager seems to contain duplicates
        // Since we are filtering out dupes, we need to adjust
        // the exit condition of the recursion
        final int curNumArtists = ldc.getArtists().size();

        spotify.getFollowedArtists(options, new Callback<ArtistsCursorPager>() {
            @Override
            public void success(ArtistsCursorPager artistCursorPager, Response response) {
                ldc.setArtists(artistCursorPager.artists.items);
                ((MainActivity) mContext).updateData(ldc.getArtists(), 3);
                //updateWatchData();
                artistTotal = artistCursorPager.artists.total;
                Log.d("SpotifyManager", "total: " + artistTotal + "; offset: " + offset + "; ldc: " + ldc.getArtists().size());
                if (ldc.getArtists().size() != curNumArtists) {
                    retrieveArtists(offset + pagerLimit);
                } else {
                    Log.d("SpotifyManager", "Initialized Saved Artists");
                    initCategories();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("SpotifyManager", "Error retrieving artists: " + error.getMessage());
            }
        });
    }

    private void initCategories() {
        HashMap<String, Object> options = new HashMap<>();
        options.put(SpotifyService.LIMIT, pagerLimit);
        options.put(SpotifyService.OFFSET, 0);

        spotify.getCategories(options, new SpotifyCallback<CategoriesPager>() {
            @Override
            public void success(CategoriesPager categoriesPager, Response response) {
                ldc.setCategories(categoriesPager.categories.items);
                ((MainActivity) mContext).updateData(ldc.getCategories(), 4);
                Log.d("SpotifyManager", "Initialized Categories");
                loadLastPlayedQueue();
                updateWatchData();
                ldc.featureDatabase.setReady();
                ldc.featureDatabase.retrieveAudioFeaturesForQueuedTracks();
            }

            @Override
            public void failure(SpotifyError error) {
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
                    + "--" + MainActivity.formatMilliseconds((int) (track.track.duration_ms))
                    + "--" + track.track.id;
        }
        if(!msg.equals(""))commManager.sendData("song", msg);
    }


    // Playback

//    public void play(Track track) {
//        if(mPlayer.isInitialized()) {
//            currentQueue.clear();
//            currentQueue.put(track.uri, track);
//            mPlayer.play(track.uri);
//            mPlayer.setRepeat(true);
//            startTimer();
//            saveLastPlayedQueue();
//        }
//    }

    public void play(List<TrackSimple> tracks) {
        if(mPlayer.isInitialized()) {
            currentQueue.clear();
            List<String> trackUris = new ArrayList<>();
            for (TrackSimple track : tracks) {
                currentQueue.put(track.uri, track);
                trackUris.add(track.uri);
            }
            mPlayer.play(trackUris);
            mPlayer.setShuffle(isShuffle);
            startTimer();
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

    // loading spotify items into the player

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

    public void loadPlaylist(int pos) {
        String ownerID = ldc.getPlaylists().get(pos).owner.id;
        String playlistID = ldc.getPlaylists().get(pos).id;
        loadPlaylistTracks(ownerID, playlistID);
    }

    public void loadSong(int pos) {
        List<SavedTrack> songs = ldc.getSongs();
        List<TrackSimple> tracks = new ArrayList<>();
        //play(songs.get(pos).track);
        currentQueue.clear();
        //mPlayer.queue(songs.get(pos).track.uri);
        // add all other saved songs to queue, too, as spotify does
        for(int i = 0; i < songs.size(); i++) {
            if(i != pos) {
                Track track = songs.get(i).track;
                tracks.add(track);
                //mPlayer.queue(track.uri);
                currentQueue.put(track.uri, track);
            }
        }
        currentQueue.put(songs.get(pos).track.uri, songs.get(pos).track);
        tracks.add(0, songs.get(pos).track); // add the clicked song last and to the front
        //mPlayer.setShuffle(isShuffle);
        //mPlayer.setRepeat(true);
        //startTimer();
        play(tracks);
    }

    public void loadAlbum(int pos) {
        List<TrackSimple> tracks = ldc.getAlbums().get(pos).album.tracks.items;
        play(tracks);
    }

    public void loadArtist(int pos) {
        Artist artist = ldc.getArtists().get(pos);
        List<SavedTrack> allTracks = ldc.getSongs();
        final List<TrackSimple> artistTracks = new ArrayList<>();

        // filter your saved songs by artist
        for(SavedTrack track: allTracks) {
            for(ArtistSimple a: track.track.artists) {
                if (a.id.equals(artist.id)) {
                    //Log.d("loadArtist", "found track for artist: " + artist.name);
                    artistTracks.add(track.track);
                }
            }
        }

        // you dont always have songs for a saved artist, so the artists top tracks get added to queue
        artistTracks.addAll(ldc.getArtistTopTracks(artist.id));

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

    public void loadSongsByFeature(String type, float mult) {
        List<TrackSimple> tracks = ldc.getAudioFeatureDatabase().getTracksWithFeature(type, mult);
        if(!tracks.isEmpty()) {
            Collections.shuffle(tracks);
            play(tracks);
        }
    }

    // retrieve playlist tracks from spotify
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

    public void turnVolumeUp() {
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume + 2, 0);
        Log.d("SpotifyManager", "turning volume up to: " + Math.max(15, (currentVolume + 2)));
    }

    public void turnVolumeDown() {
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume - 2, 0);
        Log.d("SpotifyManager", "lowering volume down to: " + Math.max(0, (currentVolume - 2)));
    }

    public SpotifyService getSpotifyService() {
        return spotify;
    }

    public float getAudioFeatureForCurrentTrack(String featureName) {
        float value = 0f;
        TrackSimple track = currentQueue.get(playerState.trackUri);
        if(track != null) {
            value = ldc.getAudioFeatureDatabase().getFeatureForID(featureName, track.id);
        }
        return value;
    }


    /*
     * Track encoding:
     *
     * id;uri;name;artist[0].name;duration;
     *
     */
    private void loadLastPlayedQueue() {
        if(SpotifyCache.hasLastPlayedQueue()) {
            List<String> queueInfo = SpotifyCache.getLastPlayedQueue();
            List<TrackSimple> tracks = new ArrayList<>();
            ArtistSimple artist = null;
            String[] split;
            for(String s: queueInfo) {
                split = s.split(";");
                if(split.length == 5) {
                    TrackSimple track = new TrackSimple();
                    track.id = split[0];
                    track.uri = split[1];
                    track.name = split[2];
                    artist = new ArtistSimple();
                    artist.name = split[3];
                    track.artists = new ArrayList<>();
                    track.artists.add(artist);
                    track.duration_ms = Long.parseLong(split[4]);
                    tracks.add(track);
                }
            }
            play(tracks);
            mPlayer.pause();
        }
    }

    /*
     * Track encoding:
     *
     * id;uri;name;artist[0].name;duration;
     *
     */
    private void saveLastPlayedQueue() {
        String s = "";
        if(!currentQueue.isEmpty()) {
            TrackSimple currentTrack = currentQueue.get(playerState.trackUri);
            s += currentTrack.id + ";" + currentTrack.uri + ";" + currentTrack.name + ";"
                    + currentTrack.artists.get(0).name + ";" + currentTrack.duration_ms + ";\n";
            String currentUri = currentTrack.uri;

            for(String uri: currentQueue.keySet()) {
                if(!uri.equals(currentUri)) {
                    currentTrack = currentQueue.get(uri);
                    s += currentTrack.id + ";" + uri + ";" + currentTrack.name + ";"
                            + currentTrack.artists.get(0).name + ";" + currentTrack.duration_ms + ";\n";
                }
            }

            SpotifyCache.saveLastPlayedQueue(s);
        }
    }


    // Callbacks

    public void onDestroy() {
        saveLastPlayedQueue();
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
        playerState = state;
        if(eventType.name().equals("TRACK_CHANGED")) {
            TrackSimple track = currentQueue.get(state.trackUri);
            if(track != null) {
                ((MainActivity) mContext).updateCurrentTrackInfo(track.artists.get(0).name, track.name, (int) track.duration_ms);
            }
            saveLastPlayedQueue();
        }
        if(eventType.name().equals("PLAY")) {
            ((MainActivity) mContext).updatePlayButton(true);
        }
        if(eventType.name().equals("PAUSE")) {
            ((MainActivity) mContext).updatePlayButton(false);
        }
        if(eventType.name().equals("END_OF_CONTEXT")) {
            mPlayer.resume();
        }
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

        private HashMap<String, List<Track>> artistsTopTracks;

        // Lists with spotifyItems
        private List<SpotifyItem>[] spotifyData;

        private AudioFeatureDatabase featureDatabase;

        public ListDataContainer() {
            playlists = new ArrayList<>();
            songs = new ArrayList<>();
            albums = new ArrayList<>();
            artists = new ArrayList<>();
            categories = new ArrayList<>();
            artistsTopTracks = new HashMap<>();
            spotifyData = new ArrayList[5];
            featureDatabase = new AudioFeatureDatabase();
        }

        public List<PlaylistSimple> getPlaylists() { return playlists; }
        public List<SavedTrack> getSongs() { return songs; }
        public List<SavedAlbum> getAlbums() { return albums; }
        public List<Artist> getArtists() { return artists; }
        public List<Category> getCategories() { return categories; }
        public List<Track> getArtistTopTracks(String artistID) { return artistsTopTracks.get(artistID); }

        public void setPlaylists(List<PlaylistSimple> list) {
            playlists = list;
            ArrayList<SpotifyItem> tempList = new ArrayList<>();
            for(PlaylistSimple playlist: playlists) {
                tempList.add(new SpotifyItem(playlist));
                retrievePlaylistTracks(playlist.owner.id, playlist.id);
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
            featureDatabase.addTracks(songs);
        }

        public void setAlbums(List<SavedAlbum> list) {
            albums = list;
            ArrayList<SpotifyItem> tempList = new ArrayList<>();
            for(SavedAlbum album: albums) {
                tempList.add(new SpotifyItem(album));
                featureDatabase.addTracks(album.album.tracks.items);
            }
            spotifyData[2] = tempList;
        }

        /*
         * This method actually adds the artists to the current list
         * as opposed to the others which set the list completely new.
         */
        public void setArtists(List<Artist> list) {
            ArrayList<SpotifyItem> tempList = new ArrayList<>();
            for(Artist artist: list) {
                if(!isDuplicateArtist(artist)) {
                    artists.add(artist);
                    tempList.add(new SpotifyItem(artist));
                    retrieveArtistTopTracks(artist);
                }
            }
            spotifyData[3] = tempList;
        }

        public void setCategories(List<Category> list) {
            categories = list;
            ArrayList<SpotifyItem> tempList = new ArrayList<>();
            for(Category category: categories) {
                tempList.add(new SpotifyItem(category));
                retrieveCategoryTracks(category);
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

        public AudioFeatureDatabase getAudioFeatureDatabase() { return featureDatabase; }

        private void retrievePlaylistTracks(String ownerID, String playlistID) {
            SpotifyManager.getInstance().getSpotifyService().getPlaylist(ownerID, playlistID, new Callback<Playlist>() {
                @Override
                public void success(Playlist playlist, Response response) {
                    ArrayList<Track> tracks = new ArrayList<>();
                    for (PlaylistTrack track : playlist.tracks.items) {
                        tracks.add(track.track);
                    }
                    featureDatabase.addTracks(tracks);
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.d("SpotifyManager", "Error retrieving tracks for playlist: " + error.getMessage());
                }
            });
        }

        private void retrieveArtistTopTracks(final Artist artist) {
            if(artistsTopTracks.get(artist.id) != null) {
                return;
            }
            SpotifyManager.getInstance().getSpotifyService().getArtistTopTrack(artist.id, "DE", new Callback<Tracks>() {
                @Override
                public void success(Tracks tracks, Response response) {
                    featureDatabase.addTracks(tracks.tracks);
                    artistsTopTracks.put(artist.id, tracks.tracks);
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.d("SpotifyManager", "Error retrieving tracks for Artist: " + error.getMessage());
                }
            });
        }

        private void retrieveCategoryTracks(final Category category) {
            SpotifyManager.getInstance().getSpotifyService().getPlaylistsForCategory(category.id, null, new Callback<PlaylistsPager>() {
                @Override
                public void success(PlaylistsPager playlistsPager, Response response) {
                    PlaylistSimple playlist = playlistsPager.playlists.items.get(0);
                    retrievePlaylistTracks(playlist.owner.id, playlist.id);
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.d("Spotify", "Error while retrieving playlists for category: " + category.name + "; " + error.getMessage());
                }
            });
        }

        private boolean isDuplicateArtist(Artist artist) {
            for(Artist a: artists) {
                if(a.id.equals(artist.id)) return true;
            }
            return false;
        }

    }

}
