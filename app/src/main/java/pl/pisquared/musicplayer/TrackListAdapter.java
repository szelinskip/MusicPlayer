package pl.pisquared.musicplayer;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import pl.pisquared.musicplayer.utils.StringTrackUtils;
import pl.pisquared.musicplayer.utils.TimeConverterUtils;

public class TrackListAdapter extends RecyclerView.Adapter<TrackListAdapter.ViewHolder>
{
    private static final String TAG = TrackListAdapter.class.getSimpleName();
    private static final String UNKNOWN_AUTHOR = "Unknown";
    private static final int GREEN_ACCENT_LIGHTER_COLOR = 0xff80cbc4;
    private static final int GREEN_ACCENT_DARKER_COLOR = 0xff009688;
    private static final int BROWN_LIGHTER_BACKGROUND_COLOR = 0xff784F40;
    private List<Track> tracks;
    private OnTrackListItemClickListener onTrackListItemClickListener;
    private MusicPlayer musicPlayer;

    public TrackListAdapter(List<Track> tracks, OnTrackListItemClickListener onTrackListItemClickListener, MusicPlayer musicPlayer)
    {
        this.tracks = tracks;
        this.onTrackListItemClickListener = onTrackListItemClickListener;
        this.musicPlayer = musicPlayer;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tracks_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Track track = tracks.get(position);
        String title = track.getTitle();
        String author = track.getAuthor();
        holder.tvTitle.setText(title);
        if(StringTrackUtils.isValidAuthor(author))
        {
            holder.tvAuthor.setText(author);
        }
        else
        {
            holder.tvAuthor.setText(UNKNOWN_AUTHOR);
        }
        Track currentTrack = musicPlayer.getCurrentTrack();
        ImageButton ibPlayPause = holder.ibPlayPauseTrack;
        if(currentTrack != null)
            Log.d(TAG, "position: " + position + " ; current track id: " + currentTrack.getId() + " ; the track id: " + track.getId());
        else
            Log.d(TAG, "position: " + position + " ; current track id: CURRENT TRACK IS NULL ; the track id: " + track.getId());

        if(track.equals(currentTrack))
        {
            musicPlayer.setTrackButton(ibPlayPause);
            ibPlayPause.setBackgroundResource(R.drawable.baseline_pause_white_36);
            setColors(holder, Color.WHITE, GREEN_ACCENT_DARKER_COLOR);
            Log.d(TAG, "position: " + position + " ; holder button set to pause");
        }
        else
        {
            ibPlayPause.setBackgroundResource(R.drawable.baseline_play_arrow_white_36);
            setColors(holder, GREEN_ACCENT_LIGHTER_COLOR, BROWN_LIGHTER_BACKGROUND_COLOR);
            Log.d(TAG, "position: " + position + " ; holder button set to play");
        }
        Pair<Integer, Integer> minsSecs = TimeConverterUtils.getMinutesSecondsFromMilliSeconds(track.getDuration());
        holder.tvDuration.setText(StringTrackUtils.getTimeRepresentation(minsSecs.first, minsSecs.second));
    }

    private void setColors(ViewHolder holder, int textColor, int backgroundColor)
    {
        LayerDrawable layer = (LayerDrawable) holder.itemView.getBackground();
        GradientDrawable shape = (GradientDrawable) layer.findDrawableByLayerId(R.id.track_item_background);
        holder.tvAuthor.setTextColor(textColor);
        holder.tvDuration.setTextColor(textColor);
        holder.tvTitle.setTextColor(textColor);
        shape.setColor(backgroundColor);
    }

    @Override
    public int getItemCount()
    {
        return tracks.size();
    }

    @Override
    public long getItemId(int position)
    {
        return tracks.get(position).getId();
    }

    public int getTrackPosition(Track track)
    {
        if(track != null)
            return tracks.indexOf(track);
        else
            return -1;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        private TextView tvTitle;
        private TextView tvAuthor;
        private TextView tvDuration;
        private ImageButton ibPlayPauseTrack;

        private ViewHolder(View itemView)
        {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_track_title);
            tvAuthor = itemView.findViewById(R.id.tv_track_author);
            tvDuration = itemView.findViewById(R.id.tv_track_duration);
            ibPlayPauseTrack = itemView.findViewById(R.id.ib_track_play_pause);
            ibPlayPauseTrack.setOnClickListener(this);
        }

        @Override
        public void onClick(View v)
        {
            Track track = tracks.get(getAdapterPosition());
            onTrackListItemClickListener.onTrackButtonClick((ImageButton) v, track);
        }
    }

    public interface MusicPlayer
    {
        Track getCurrentTrack();
        void setTrackButton(ImageButton ibTrackButton);
    }

    public interface OnTrackListItemClickListener
    {
        void onTrackButtonClick(ImageButton ibView, Track track);
    }
}
