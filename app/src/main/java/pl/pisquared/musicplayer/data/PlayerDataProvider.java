package pl.pisquared.musicplayer.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

import pl.pisquared.musicplayer.Track;

public class PlayerDataProvider
{
    private List<Track> tracks;
    private MediaPlayer player;
    private Track currentTrack;

    private static PlayerDataProvider provider = null;

    public static PlayerDataProvider getInstance(Context context)
    {
        if(provider == null)
            provider = new PlayerDataProvider(context);
        return provider;
    }

    public List<Track> getTracks()
    {
        return tracks;
    }

    public MediaPlayer getPlayer()
    {
        return player;
    }

    public Track getCurrentTrack()
    {
        return currentTrack;
    }

    public void setCurrentTrack(Track track)
    {
        this.currentTrack = track;
    }

    private PlayerDataProvider(Context context)
    {
        loadTracks(context);
        initPlayer(context);
    }

    private void loadTracks(Context context)
    {
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";
        String [] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA};

        Cursor cursor = cr.query(uri, projection, selection, null, sortOrder);
        List<Track> trackList = null;
        if(cursor != null)
        {
            long id = 0;
            String title = "";
            String author = "";
            long duration = 0;
            String fileName = "";
            trackList = new ArrayList<>(cursor.getCount() + 1);
            while(cursor.moveToNext())
            {
                id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                author = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                trackList.add(new Track(id, title, author, duration, fileName));
            }
            cursor.close();
        }
        tracks = trackList;
    }

    private void initPlayer(Context context)
    {
        player = new MediaPlayer();
        player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }


}
