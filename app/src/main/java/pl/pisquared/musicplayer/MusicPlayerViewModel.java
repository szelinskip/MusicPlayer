package pl.pisquared.musicplayer;

import android.app.Application;
import android.arch.lifecycle.ViewModel;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;


public class MusicPlayerViewModel extends ViewModel
{
    private List<Track> tracks;
    private Application appContext;

    public MusicPlayerViewModel(Application appContext)
    {
        this.appContext = appContext;
    }

    public List<Track> getTracks()
    {
        if (tracks == null)
        {
            loadTracks();
        }
        return tracks;
    }

    private void loadTracks()
    {
        ContentResolver cr = appContext.getContentResolver();
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
}
