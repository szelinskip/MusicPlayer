package pl.pisquared.musicplayer;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import pl.pisquared.musicplayer.utils.StringTrackUtils;
import pl.pisquared.musicplayer.utils.TimeConverterUtils;


public class MainActivity extends AppCompatActivity implements TrackListAdapter.OnTrackListItemClickListener, TrackListAdapter.MusicPlayer, View.OnClickListener
{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_READ_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    private static final String PERMISSION_DENIED_MSG = "This app requires external storage read permission to work";
    private static final int FORWARD_TRACK_BY_MILLISECONDS = 10000;  // 10000ms = 10s
    private static final int REWIND_TRACK_BY_MILLISECONDS = 10000;  // 10000ms = 10s
    private RecyclerView rvTrackList;
    private TrackListAdapter trackListAdapter;
    private ImageButton ibRewind;
    private ImageButton ibPlayPause;
    private ImageButton ibForward;
    private TextView tvTrackTitle;
    private SeekBar sbTrackProgressBar;
    private TextView tvLeftTime;
    private List<Track> trackList;
    private Track currentTrack;
    private ImageButton currentTrackIBView;
    private MusicPlayerViewModel viewModel;

    private MusicForegroundService musicService;
    private boolean musicServiceBounded = false;

    private BroadcastReceiver messageReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String msg = intent.getStringExtra(Constants.MESSAGE_KEY);
            switch(msg)
            {
                case Constants.PREPARED_MSG:
                    trackPrepared();
                    break;
                case Constants.COMPLETION_MSG:
                    int newTrackIndex = intent.getIntExtra(Constants.CURRENT_TRACK_KEY, trackList.indexOf(currentTrack));
                    trackCompletion(newTrackIndex);
                    break;
                case Constants.UPDATE_PROGRESS_MSG:
                    updateProgressBar();
                    break;
            }
        }
    };

    private ServiceConnection musicConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MusicForegroundService.MusicPlayerServiceBinder binder = (MusicForegroundService.MusicPlayerServiceBinder) service;
            musicService = binder.getService();
            musicService.setTrackList(trackList);
            musicServiceBounded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            musicServiceBounded = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvTrackList = findViewById(R.id.rv_track_list);
        ibRewind = findViewById(R.id.ib_rewind);
        ibPlayPause = findViewById(R.id.ib_play_pause);
        ibForward = findViewById(R.id.ib_forward);
        tvTrackTitle = findViewById(R.id.tv_track_title);
        tvLeftTime = findViewById(R.id.tv_left_time);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_READ_EXTERNAL_STORAGE_REQUEST_CODE);
        }
        else
        {
            performInits();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode)
        {
            case PERMISSION_READ_EXTERNAL_STORAGE_REQUEST_CODE:
            {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    performInits();
                }
                else
                {
                    Toast.makeText(this, PERMISSION_DENIED_MSG, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void performInits()
    {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        viewModel = ViewModelProviders.of(this, new MusicPlayerViewModelFactory(getApplication())).get(MusicPlayerViewModel.class);
        getTrackList();
        initTrackRecyclerView();
        initTrackProgressBar();
        initControlButtons();
        initCurrentTrack();
        setService();
        initBroadcastReceiver();
    }

    private void getTrackList()
    {
        trackList = viewModel.getTracks();
    }

    private void initTrackRecyclerView()
    {
        rvTrackList.setLayoutManager(new LinearLayoutManager(this));
        trackListAdapter = new TrackListAdapter(trackList, this, this);
        trackListAdapter.setHasStableIds(true);
        rvTrackList.setAdapter(trackListAdapter);
    }

    private void initCurrentTrack()
    {
        currentTrack = viewModel.getCurrentTrack();
    }

    private void initTrackProgressBar()
    {
        sbTrackProgressBar = findViewById(R.id.sb_track_progress);
        sbTrackProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if(fromUser)
                {
                    if(currentTrack != null && musicService != null && (musicService.isPlaying() || musicService.isPaused()))
                    {
                        musicService.getPlayer().seekTo(progress);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void initControlButtons()
    {
        ibPlayPause.setOnClickListener(this);
        ibForward.setOnClickListener(this);
        ibRewind.setOnClickListener(this);
    }

    private void initBroadcastReceiver()
    {
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(Constants.BROADCAST_INTENT_KEY));
    }

    private void setService()
    {
        if(musicService == null)
        {
            Intent startMusicService = new Intent(this, MusicForegroundService.class);
            startService(startMusicService);
            getApplicationContext().bindService(startMusicService, musicConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onTrackButtonClick(ImageButton ibView, Track track)
    {
        if(track.equals(currentTrack))
        {
            currentTrackIBView = ibView;
            playPauseButtonsClickAux();
        }
        else
        {
            if(musicService != null && (musicService.isPlaying() || musicService.isPaused()))
            {
                musicService.getPlayer().reset();
                musicService.setPlaying(false);
                musicService.setPaused(false);
            }
            if(currentTrackIBView != null)
            {
                currentTrackIBView.setBackgroundResource(R.drawable.baseline_play_arrow_white_36);
            }
            int currentTrackPosition = trackList.indexOf(currentTrack);
            int newTrackPosition = trackList.indexOf(track);
            currentTrack = track;
            viewModel.setCurrentTrack(currentTrack);
            currentTrackIBView = ibView;
            currentTrackIBView.setBackgroundResource(R.drawable.baseline_pause_white_36);
            ibPlayPause.setBackgroundResource(R.drawable.baseline_pause_white_48);
            trackListAdapter.notifyItemChanged(currentTrackPosition);
            trackListAdapter.notifyItemChanged(newTrackPosition);
            playTrack(track);
        }
    }

    @Override
    public void onClick(View v)
    {
        int viewID = v.getId();
        switch(viewID)
        {
            case R.id.ib_play_pause:
                playPauseButtonClick();
                break;
            case R.id.ib_forward:
                forwardButtonClick();
                break;
            case R.id.ib_rewind:
                rewindButtonClick();
                break;
        }
    }

    private void playPauseButtonClick()
    {
        if(currentTrack != null)
        {
            playPauseButtonsClickAux();
        }
    }

    private void playPauseButtonsClickAux()
    {
        if(musicService != null)
        {
            if(musicService.getPlayer().isPlaying())
            {
                musicService.getPlayer().pause();
                musicService.setPlaying(false);
                musicService.setPaused(true);
                currentTrackIBView.setBackgroundResource(R.drawable.baseline_play_arrow_white_36);
                ibPlayPause.setBackgroundResource(R.drawable.baseline_play_arrow_white_48);
            }
            else
            {
                musicService.getPlayer().start();
                sbTrackProgressBar.setProgress(musicService.getPlayer().getCurrentPosition());
                musicService.setPlaying(true);
                musicService.setPaused(false);
                currentTrackIBView.setBackgroundResource(R.drawable.baseline_pause_white_36);
                ibPlayPause.setBackgroundResource(R.drawable.baseline_pause_white_48);
            }
        }
    }

    private void forwardButtonClick()
    {
        if(currentTrack != null && musicService != null && (musicService.isPlaying() || musicService.isPaused()))
        {
            int duration = musicService.getPlayer().getDuration();
            int currentPosition = musicService.getPlayer().getCurrentPosition();
            int forwardedPosition = FORWARD_TRACK_BY_MILLISECONDS + currentPosition < duration ? FORWARD_TRACK_BY_MILLISECONDS + currentPosition : duration;
            musicService.getPlayer().seekTo(forwardedPosition);
        }
    }

    private void rewindButtonClick()
    {
        if(currentTrack != null && musicService != null && (musicService.isPlaying() || musicService.isPaused()))
        {
            int currentPosition = musicService.getPlayer().getCurrentPosition();
            int rewoundPosition = currentPosition - REWIND_TRACK_BY_MILLISECONDS > 0 ? currentPosition - REWIND_TRACK_BY_MILLISECONDS : 0;
            musicService.getPlayer().seekTo(rewoundPosition);
        }
    }

    public void playTrack(Track track)
    {
        musicService.playTrack(track);
    }

    private void trackPrepared()
    {
        currentTrack = musicService.getCurrentTrack();
        tvTrackTitle.setText(currentTrack.getTitle());
        sbTrackProgressBar.setMax(musicService.getPlayer().getDuration());
    }

    private void trackCompletion(int newTrackIndex)
    {
        if(newTrackIndex != musicService.getCurrentTrack().getId())
        {
            int trackIndex = trackList.indexOf(currentTrack);
            Track track = trackList.get(newTrackIndex);
            trackListAdapter.notifyItemChanged(trackIndex);
            trackListAdapter.notifyItemChanged(newTrackIndex);
            currentTrackIBView.setBackgroundResource(R.drawable.baseline_pause_white_36);
            sbTrackProgressBar.setProgress(0);
            currentTrack = track;
            viewModel.setCurrentTrack(currentTrack);
        }
    }

    private void updateProgressBar()
    {
        sbTrackProgressBar.setProgress(musicService.getPlayer().getCurrentPosition());
        Pair<Integer, Integer> minsSeconds = TimeConverterUtils.getMinutesSecondsFromMilliSeconds(musicService.getPlayer().getDuration() - musicService.getPlayer().getCurrentPosition());
        tvLeftTime.setText(StringTrackUtils.getTimeRepresentation(minsSeconds.first, minsSeconds.second));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(musicService != null && (musicService.isPlaying() || musicService.isPaused()))
        {
            sbTrackProgressBar.setProgress(musicService.getPlayer().getCurrentPosition());
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if(musicServiceBounded)
        {
            getApplicationContext().unbindService(musicConnection);
            musicServiceBounded = false;
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance()
    {
        return super.onRetainCustomNonConfigurationInstance();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        if(currentTrack != null && musicService != null)
        {
            tvTrackTitle.setText(currentTrack.getTitle());
            if(musicService.getPlayer().isPlaying())
                ibPlayPause.setBackgroundResource(R.drawable.baseline_pause_white_48);
            else
                ibPlayPause.setBackgroundResource(R.drawable.baseline_play_arrow_white_48);
        }
    }

    @Override
    public Track getCurrentTrack()
    {
        return currentTrack;
    }

    @Override
    public void setTrackButton(ImageButton ibTrackButton)
    {
        currentTrackIBView = ibTrackButton;
        if(musicService != null)
        {
            if(musicService.isPlaying())
                currentTrackIBView.setBackgroundResource(R.drawable.baseline_pause_white_36);
            else if(musicService.isPaused())
                currentTrackIBView.setBackgroundResource(R.drawable.baseline_play_arrow_white_36);
        }
    }
}
