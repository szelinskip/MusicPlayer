package pl.pisquared.musicplayer;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import java.io.IOException;
import java.util.List;

import pl.pisquared.musicplayer.utils.StringTrackUtils;
import pl.pisquared.musicplayer.utils.TimeConverterUtils;


public class MainActivity extends AppCompatActivity implements TrackListAdapter.OnTrackListItemClickListener, TrackListAdapter.MusicPlayer,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, View.OnClickListener
{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_READ_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    private static final String PERMISSION_DENIED_MSG = "This app requires external storage read permission to work";
    private static final String BUNDLE_KEY_IS_PLAYING = "is_playing";
    private static final String BUNDLE_KEY_IS_PAUSED = "is_paused";
    private static final int UPDATE_TRACK_PROGRESS_BAR_DELAY = 1000;  // 1000ms = 1s
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
    private MediaPlayer player;
    private Track currentTrack;
    private ImageButton currentTrackIBView;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private MusicPlayerViewModel viewModel;
    private Handler progressUpdaterHandler = new Handler();
    private Runnable trackProgressBarUpdaterRunnable = () -> {
        Log.d(TAG, "inside handler, position: " + player.getCurrentPosition());
        Log.d(TAG, "bar max: " + sbTrackProgressBar.getMax() + " ; duration track: " + player.getDuration() + " bar curr pos: "+ sbTrackProgressBar.getProgress());
        sbTrackProgressBar.setProgress(player.getCurrentPosition());
        Pair<Integer, Integer> minsSeconds = TimeConverterUtils.getMinutesSecondsFromMilliSeconds(player.getDuration() - player.getCurrentPosition());
        tvLeftTime.setText(StringTrackUtils.getTimeRepresentation(minsSeconds.first, minsSeconds.second));
        progressUpdaterHandler.postDelayed(this.trackProgressBarUpdaterRunnable, UPDATE_TRACK_PROGRESS_BAR_DELAY);
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
        initPlayer();
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

    private void initPlayer()
    {
        player = viewModel.getPlayer();
        if(player != null)
        {
            player.setOnPreparedListener(this);
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
            if(currentTrack != null)
            {
                Log.d(TAG, "Player current position: " + player.getCurrentPosition());
                int playerDuration = player.getDuration();
                sbTrackProgressBar.setMax(playerDuration);
                int playerCurrPos = player.getCurrentPosition();
                sbTrackProgressBar.setProgress(playerCurrPos);
                Log.d(TAG, " init player bar max: " + sbTrackProgressBar.getMax() + " ; duration track: " + player.getDuration() + " bar curr pos: "+ sbTrackProgressBar.getProgress());
            }
            else
                sbTrackProgressBar.setProgress(0);
        }
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
                    if(currentTrack != null && (isPlaying || isPaused))
                    {
                        progressUpdaterHandler.removeCallbacks(trackProgressBarUpdaterRunnable);
                        player.seekTo(progress);
                        progressUpdaterHandler.postDelayed(trackProgressBarUpdaterRunnable, 0);
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
            if(isPlaying || isPaused)
            {
                player.reset();
                isPlaying = false;
                isPaused = false;
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
        if(player.isPlaying())
        {
            player.pause();
            progressUpdaterHandler.removeCallbacks(trackProgressBarUpdaterRunnable);
            isPlaying = false;
            isPaused = true;
            currentTrackIBView.setBackgroundResource(R.drawable.baseline_play_arrow_white_36);
            ibPlayPause.setBackgroundResource(R.drawable.baseline_play_arrow_white_48);
        }
        else
        {
            player.start();
            sbTrackProgressBar.setProgress(player.getCurrentPosition());
            progressUpdaterHandler.postDelayed(trackProgressBarUpdaterRunnable, 0);
            isPaused = false;
            isPlaying = true;
            currentTrackIBView.setBackgroundResource(R.drawable.baseline_pause_white_36);
            ibPlayPause.setBackgroundResource(R.drawable.baseline_pause_white_48);
        }
    }

    private void forwardButtonClick()
    {
        if(currentTrack != null && (isPlaying | isPaused))
        {
            progressUpdaterHandler.removeCallbacks(trackProgressBarUpdaterRunnable);
            int duration = player.getDuration();
            int currentPosition = player.getCurrentPosition();
            int forwardedPosition = FORWARD_TRACK_BY_MILLISECONDS + currentPosition < duration ? FORWARD_TRACK_BY_MILLISECONDS + currentPosition : duration;
            player.seekTo(forwardedPosition);
            progressUpdaterHandler.postDelayed(trackProgressBarUpdaterRunnable, 0);
        }
    }

    private void rewindButtonClick()
    {
        if(currentTrack != null && (isPlaying | isPaused))
        {
            progressUpdaterHandler.removeCallbacks(trackProgressBarUpdaterRunnable);
            int currentPosition = player.getCurrentPosition();
            int rewoundPosition = currentPosition - REWIND_TRACK_BY_MILLISECONDS > 0 ? currentPosition - REWIND_TRACK_BY_MILLISECONDS : 0;
            player.seekTo(rewoundPosition);
            progressUpdaterHandler.postDelayed(trackProgressBarUpdaterRunnable, 0);
        }
    }

    public void playTrack(Track track)
    {
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

    @Override
    public void onPrepared(MediaPlayer mp)
    {
        tvTrackTitle.setText(currentTrack.getTitle());
        sbTrackProgressBar.setMax(mp.getDuration());
        mp.start();
        progressUpdaterHandler.postDelayed(trackProgressBarUpdaterRunnable, 0);
        isPlaying = true;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        progressUpdaterHandler.removeCallbacks(trackProgressBarUpdaterRunnable);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(player != null && (isPlaying | isPaused))
        {
            progressUpdaterHandler.postDelayed(trackProgressBarUpdaterRunnable, 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_KEY_IS_PLAYING, isPlaying);
        outState.putBoolean(BUNDLE_KEY_IS_PAUSED, isPaused);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        isPlaying = savedInstanceState.getBoolean(BUNDLE_KEY_IS_PLAYING);
        isPaused = savedInstanceState.getBoolean(BUNDLE_KEY_IS_PAUSED);
        if(currentTrack != null)
        {
            tvTrackTitle.setText(currentTrack.getTitle());
            if(player.isPlaying())
                ibPlayPause.setBackgroundResource(R.drawable.baseline_pause_white_48);
            else
                ibPlayPause.setBackgroundResource(R.drawable.baseline_play_arrow_white_48);
        }
    }

    @Override
    public boolean isPlaying()
    {
        return isPlaying;
    }

    @Override
    public boolean isPaused()
    {
        return isPaused;
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
        if(isPlaying)
            currentTrackIBView.setBackgroundResource(R.drawable.baseline_pause_white_36);
        else if(isPaused)
            currentTrackIBView.setBackgroundResource(R.drawable.baseline_play_arrow_white_36);
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
            viewModel.setCurrentTrack(currentTrack);
            trackListAdapter.notifyItemChanged(trackIndex);
            trackListAdapter.notifyItemChanged(nextTrackIndex);
            currentTrackIBView.setBackgroundResource(R.drawable.baseline_pause_white_36);
            sbTrackProgressBar.setProgress(0);
            playTrack(currentTrack);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        Log.d(TAG, "ERROR OCCURED - what: " + what + " ; extra: " + extra);
        return true;
    }
}
