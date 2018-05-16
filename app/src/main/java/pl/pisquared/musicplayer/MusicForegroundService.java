package pl.pisquared.musicplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import pl.pisquared.musicplayer.utils.StringTrackUtils;
import pl.pisquared.musicplayer.utils.TimeConverterUtils;


public class MusicForegroundService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener
{
    private static final String TAG = "MusicForegroundService";
    private static final int UPDATE_TRACK_PROGRESS_BAR_DELAY = 1000;  // 1000ms = 1s
    private static final int MUSIC_PLAYER_NOTIFICATION_ID = 1;
    private List<Track> trackList;
    private MediaPlayer player;
    private Track currentTrack;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private MusicPlayerServiceBinder binder = new MusicPlayerServiceBinder();
    private LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

    private Handler progressUpdaterHandler = new Handler();
    private Runnable trackProgressBarUpdaterRunnable = () -> {
        Intent intent = new Intent(Constants.BROADCAST_INTENT_KEY);
        intent.putExtra(Constants.MESSAGE_KEY, Constants.UPDATE_PROGRESS_MSG);
        progressUpdaterHandler.postDelayed(this.trackProgressBarUpdaterRunnable, UPDATE_TRACK_PROGRESS_BAR_DELAY);
    };

    @Override
    public void onCreate()
    {
        super.onCreate();
        initPlayer();
        currentTrack = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        initPlayer();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new NotificationCompat.Builder(this, MusicApp.NOTIFICATION_CHANNEL_ID)
                        .setContentTitle("Sample title")
                        .setContentText("Content test")
                        .setSmallIcon(R.drawable.baseline_fast_forward_white_18)
                        .setContentIntent(pendingIntent)
                        .build();

        startForeground(MUSIC_PLAYER_NOTIFICATION_ID, notification);
        stopSelf();
        return START_NOT_STICKY;
    }

    public void playTrack(Track track)
    {
        currentTrack = track;
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.getId());
        try
        {
            player.setDataSource(this, uri);
            player.prepareAsync();
        } catch (IOException e)
        {
            Toast.makeText(this, R.string.track_playback_error, Toast.LENGTH_SHORT).show();
        }
    }

    public MediaPlayer getPlayer()
    {
        return player;
    }

    public Track getCurrentTrack()
    {
        return currentTrack;
    }

    public boolean isPlaying()
    {
        return isPlaying;
    }

    public void setPlaying(boolean playing)
    {
        isPlaying = playing;
    }

    public boolean isPaused()
    {
        return isPaused;
    }

    public void setPaused(boolean paused)
    {
        isPaused = paused;
    }

    public void setTrackList(List<Track> trackList)
    {
        this.trackList = trackList;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp)
    {
        Log.d(TAG, "on Completion called");
        if(currentTrack != null)
        {
            if(isPlaying || isPaused)
            {
                player.reset();
                isPlaying = false;
                isPaused = false;
            }
            int trackIndex = trackList.indexOf(currentTrack);
            int nextTrackIndex = (trackIndex + 1) % trackList.size();
            Track nextTrack = trackList.get(nextTrackIndex);
            currentTrack = nextTrack;
            Intent intent = new Intent(KEYGUARD_SERVICE);
            intent.putExtra(Constants.MESSAGE_KEY, Constants.COMPLETION_MSG);
            intent.putExtra(Constants.CURRENT_TRACK_KEY, trackList.indexOf(currentTrack));
            localBroadcastManager.sendBroadcast(intent);
            playTrack(currentTrack);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        Log.d(TAG, "ERROR OCCURED - what: " + what + " ; extra: " + extra);
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp)
    {
        Intent intent = new Intent(Constants.BROADCAST_INTENT_KEY);
        intent.putExtra(Constants.MESSAGE_KEY, Constants.PREPARED_MSG);
        localBroadcastManager.sendBroadcast(intent);
        mp.start();
        progressUpdaterHandler.postDelayed(trackProgressBarUpdaterRunnable, 0);
        isPlaying = true;
    }

    private void initPlayer()
    {
        player = new MediaPlayer();
        player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public class MusicPlayerServiceBinder extends Binder
    {
        public MusicForegroundService getService()
        {
            return MusicForegroundService.this;
        }
    }
}
