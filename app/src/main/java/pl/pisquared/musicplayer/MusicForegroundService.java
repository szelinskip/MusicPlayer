package pl.pisquared.musicplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MusicForegroundService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener
{
    private static final String TAG = "MusicForegroundService";

    public static MusicForegroundService musicForegroundService = null;

    private static final int UPDATE_TRACK_PROGRESS_BAR_DELAY = 1000;  // 1000ms = 1s
    private static final int MUSIC_PLAYER_NOTIFICATION_ID = 1;
    private static final int DEFAULT_PLAY_BUTTON_RES_ID = R.drawable.baseline_play_arrow_black_36;
    private List<Track> baseList;
    private List<Track> trackList;
    private MediaPlayer player;
    private Track currentTrack;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean shufflePlay;
    private boolean autoPlayNext;
    private MusicPlayerServiceBinder binder = new MusicPlayerServiceBinder();
    private LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
    private RemoteViews notificationLayout;
    private boolean isExternalEventReceiverRegistred = false;

    private Handler progressUpdaterHandler = new Handler();
    private Runnable trackProgressBarUpdaterRunnable = () -> {
        Intent intent = new Intent(Constants.ACTIVITY_BROADCAST_INTENT_KEY);
        intent.putExtra(Constants.MESSAGE_KEY, Constants.UPDATE_PROGRESS_MSG);
        localBroadcastManager.sendBroadcast(intent);
        progressUpdaterHandler.postDelayed(this.trackProgressBarUpdaterRunnable, UPDATE_TRACK_PROGRESS_BAR_DELAY);
    };

    private BroadcastReceiver buttonClicksReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getStringExtra(Constants.BUTTON_CLICKED_MSG);
            switch(action)
            {
                case Constants.REWIND_INTENT_ACTION:
                    Log.d(TAG, "REWIND CLICK");
                    rewindTrack();
                    break;

                case Constants.PLAY_PAUSE_INTENT_ACTION:
                    Log.d(TAG, "PLAY PAUSE CLICK");
                    playOrPauseTrack();
                    break;

                case Constants.FORWARD_INTENT_ACTION:
                    Log.d(TAG, "FORWARD CLICK");
                    forwardTrack();
                    break;

                case Constants.CLOSE_INTENT_ACTION:
                    Log.d(TAG, "CLOSE CLICK");
                    close();
                    break;
            }
        }
    };

    private BroadcastReceiver externalEventReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
            {
                onHeadsetUnplugged();
            }
        }
    };

    @Override
    public void onCreate()
    {
        super.onCreate();
        musicForegroundService = this;
        createNotificationLayout();
        initPlayer();
        currentTrack = null;
        registerReceiver(buttonClicksReceiver, new IntentFilter(Constants.BUTTONS_BROADCAST_INTENT_KEY));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        initPlayer();

        Notification notification = createNotification(DEFAULT_PLAY_BUTTON_RES_ID);

        startForeground(MUSIC_PLAYER_NOTIFICATION_ID, notification);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        sendSelfDestroyMsg();
        unregisterReceiver(buttonClicksReceiver);
        unregisterExternalEventReceiver();
    }

    public void close()
    {
        musicForegroundService = null;
        sendUnbindRequest();
        progressUpdaterHandler.removeCallbacks(trackProgressBarUpdaterRunnable);
        player.release();
        stopForeground(true);
        stopSelf();
    }

    private void onHeadsetUnplugged()
    {
        if(isPlaying)
        {
            pauseTrack();
            sendPauseMsg();
        }
    }

    private void sendPauseMsg()
    {
        Intent intent = new Intent(Constants.ACTIVITY_BROADCAST_INTENT_KEY);
        intent.putExtra(Constants.MESSAGE_KEY, Constants.PAUSED_MSG);
        localBroadcastManager.sendBroadcast(intent);
    }

    public void sendUnbindRequest()
    {
        Intent intent = new Intent(Constants.ACTIVITY_BROADCAST_INTENT_KEY);
        intent.putExtra(Constants.MESSAGE_KEY, Constants.UNBIND_REQUEST_MSG);
        localBroadcastManager.sendBroadcast(intent);
    }

    public void sendSelfDestroyMsg()
    {
        Intent intent = new Intent(Constants.ACTIVITY_BROADCAST_INTENT_KEY);
        intent.putExtra(Constants.MESSAGE_KEY, Constants.SERVICE_DESTROY_MSG);
        localBroadcastManager.sendBroadcast(intent);
    }

    public void createNotificationLayout()
    {
        notificationLayout = new RemoteViews(getPackageName(), R.layout.player_notification);

        Intent rewindClickedIntent = new Intent(Constants.BUTTONS_BROADCAST_INTENT_KEY);
        rewindClickedIntent.putExtra(Constants.BUTTON_CLICKED_MSG, Constants.REWIND_INTENT_ACTION);
        PendingIntent rewindClickedPIntent = PendingIntent.getBroadcast(this, Constants.REWIND_INTENT_REQUEST_CODE, rewindClickedIntent, 0);
        notificationLayout.setOnClickPendingIntent(R.id.ib_notification_rewind, rewindClickedPIntent);

        Intent playPauseClickedIntent = new Intent(Constants.BUTTONS_BROADCAST_INTENT_KEY);
        playPauseClickedIntent.putExtra(Constants.BUTTON_CLICKED_MSG, Constants.PLAY_PAUSE_INTENT_ACTION);
        PendingIntent playPauseClickedPIntent = PendingIntent.getBroadcast(this, Constants.PLAY_PAUSE_INTENT_REQUEST_CODE, playPauseClickedIntent, 0);
        notificationLayout.setOnClickPendingIntent(R.id.ib_notification_play_pause, playPauseClickedPIntent);

        Intent forwardClickedIntent = new Intent(Constants.BUTTONS_BROADCAST_INTENT_KEY);
        forwardClickedIntent.putExtra(Constants.BUTTON_CLICKED_MSG, Constants.FORWARD_INTENT_ACTION);
        PendingIntent forwardClickedPIntent = PendingIntent.getBroadcast(this, Constants.FORWARD_INTENT_REQUEST_CODE, forwardClickedIntent, 0);
        notificationLayout.setOnClickPendingIntent(R.id.ib_notification_forward, forwardClickedPIntent);

        Intent closeClickedIntent = new Intent(Constants.BUTTONS_BROADCAST_INTENT_KEY);
        closeClickedIntent.putExtra(Constants.BUTTON_CLICKED_MSG, Constants.CLOSE_INTENT_ACTION);
        PendingIntent closeClickedPIntent = PendingIntent.getBroadcast(this, Constants.CLOSE_INTENT_REQUEST_CODE, closeClickedIntent, 0);
        notificationLayout.setOnClickPendingIntent(R.id.ib_notification_close, closeClickedPIntent);

    }

    public Notification createNotification(int playPauseResId)
    {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        if(currentTrack != null)
        {
            notificationLayout.setTextViewText(R.id.tv_notification_track_title, currentTrack.getTitle());
            notificationLayout.setTextViewText(R.id.tv_notification_track_author, currentTrack.getAuthor());
        }
        else
        {
            notificationLayout.setTextViewText(R.id.tv_notification_track_title, getText(R.string.default_title));
            notificationLayout.setTextViewText(R.id.tv_notification_track_author, getText(R.string.default_author));
        }

        notificationLayout.setImageViewResource(R.id.ib_notification_play_pause, playPauseResId);

        Notification notification =
                new NotificationCompat.Builder(this, MusicApp.NOTIFICATION_CHANNEL_ID)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentTitle(Constants.NOTIFICATION_TITLE)
                        .setSmallIcon(R.drawable.baseline_audiotrack_black_18)
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                        .setCustomContentView(notificationLayout)
                        .setContentIntent(pendingIntent)
                        .build();

        return notification;
    }

    private void updateNotification(int playPauseResId)
    {
        Notification notification = createNotification(playPauseResId);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if(notificationManager != null)
            notificationManager.notify(MUSIC_PLAYER_NOTIFICATION_ID, notification);
    }

    public void rewindTrack()
    {
        if(currentTrack != null && (isPlaying || isPaused))
        {
            int currentPosition = player.getCurrentPosition();
            int rewoundPosition = currentPosition - Constants.REWIND_TRACK_BY_MILLISECONDS > 0 ? currentPosition - Constants.REWIND_TRACK_BY_MILLISECONDS : 0;
            player.seekTo(rewoundPosition);
        }
    }

    public void playOrPauseTrack()
    {
        if(currentTrack != null)
        {
            if(player.isPlaying())
            {
                pauseTrack();
            }
            else
            {
                registerExternalEventReceiver();
                player.start();
                progressUpdaterHandler.postDelayed(trackProgressBarUpdaterRunnable, 0);
                isPlaying = true;
                isPaused = false;
                updateNotification(R.drawable.baseline_pause_black_36);
            }
        }
    }

    private void pauseTrack()
    {
        player.pause();
        unregisterExternalEventReceiver();
        progressUpdaterHandler.removeCallbacks(trackProgressBarUpdaterRunnable);
        isPlaying = false;
        isPaused = true;
        updateNotification(R.drawable.baseline_play_arrow_black_36);
    }

    public void forwardTrack()
    {
        if(currentTrack != null && (isPlaying || isPaused))
        {
            int duration = player.getDuration();
            int currentPosition = player.getCurrentPosition();
            int forwardedPosition = Constants.FORWARD_TRACK_BY_MILLISECONDS + currentPosition < duration ? Constants.FORWARD_TRACK_BY_MILLISECONDS + currentPosition : duration;
            player.seekTo(forwardedPosition);
        }
    }

    public void playTrack(Track track)
    {
        currentTrack = track;
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.getId());
        try
        {
            player.setDataSource(this, uri);
            player.prepareAsync();
            updateNotification(R.drawable.baseline_pause_black_36);
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

    public void setSettings(boolean shufflePlay, boolean autoPlayNext)
    {
        boolean isAlreadyShuffle = this.shufflePlay;
        this.shufflePlay = shufflePlay;
        this.autoPlayNext = autoPlayNext;
        if(shufflePlay && !isAlreadyShuffle)
        {
            Collections.shuffle(trackList);
        }
        else if (!shufflePlay)
        {
            trackList = baseList;
        }
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
        this.baseList = new ArrayList<Track>(trackList.size());
        baseList.addAll(trackList);
        trackList = new ArrayList<Track>(trackList.size());
        trackList.addAll(baseList);
        if(shufflePlay)
        {
            Collections.shuffle(trackList);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
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
            if(autoPlayNext)
            {
                int trackIndex = trackList.indexOf(currentTrack);
                int nextTrackIndex = (trackIndex + 1) % trackList.size();
                Track nextTrack = trackList.get(nextTrackIndex);
                currentTrack = nextTrack;
                Intent intent = new Intent(Constants.ACTIVITY_BROADCAST_INTENT_KEY);
                intent.putExtra(Constants.MESSAGE_KEY, Constants.COMPLETION_MSG);
                intent.putExtra(Constants.CURRENT_TRACK_KEY, baseList.indexOf(currentTrack));
                localBroadcastManager.sendBroadcast(intent);
                playTrack(currentTrack);
            }
            else
            {
                Intent intent = new Intent(Constants.ACTIVITY_BROADCAST_INTENT_KEY);
                intent.putExtra(Constants.MESSAGE_KEY, Constants.COMPLETION_MSG);
                intent.putExtra(Constants.CURRENT_TRACK_KEY, trackList.indexOf(currentTrack));
                localBroadcastManager.sendBroadcast(intent);
                isPlaying = false;
                isPaused = false;
                currentTrack = null;
            }

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
        Intent intent = new Intent(Constants.ACTIVITY_BROADCAST_INTENT_KEY);
        intent.putExtra(Constants.MESSAGE_KEY, Constants.PREPARED_MSG);
        localBroadcastManager.sendBroadcast(intent);
        registerExternalEventReceiver();
        mp.start();
        progressUpdaterHandler.postDelayed(trackProgressBarUpdaterRunnable, 0);
        isPlaying = true;
    }

    private void registerExternalEventReceiver()
    {
        if(!isExternalEventReceiverRegistred)
        {
            registerReceiver(externalEventReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            isExternalEventReceiverRegistred = true;
        }
    }

    private void unregisterExternalEventReceiver()
    {
        if(isExternalEventReceiverRegistred)
        {
            unregisterReceiver(externalEventReceiver);
            isExternalEventReceiverRegistred = false;
        }
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
