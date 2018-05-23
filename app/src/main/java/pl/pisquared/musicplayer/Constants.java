package pl.pisquared.musicplayer;

public class Constants
{
    public static final int FORWARD_TRACK_BY_MILLISECONDS = 10000;  // 10000ms = 10s
    public static final int REWIND_TRACK_BY_MILLISECONDS = 10000;  // 10000ms = 10s
    public static final String NOTIFICATION_TITLE = "Music Player";
    public static final String ACTIVITY_BROADCAST_INTENT_KEY = "MusicForegroundServiceToActivity";
    public static final String BUTTONS_BROADCAST_INTENT_KEY = "BUTTONS_CLICK_INTENT_KEY";
    public static final String MESSAGE_KEY = "MESSAGE_KEY";
    public static final String CURRENT_TRACK_KEY = "CURRENT_TRACK_KEY";
    public static final String PREPARED_MSG = "PREPARED_MSG";
    public static final String COMPLETION_MSG = "COMPLETION_MSG";
    public static final String UPDATE_PROGRESS_MSG = "UPDATE_PROGRESS_MSG";

    public static final String REWIND_INTENT_ACTION = "rewind";
    public static final int REWIND_INTENT_REQUEST_CODE = 1;
    public static final int PLAY_PAUSE_INTENT_REQUEST_CODE = 2;
    public static final int FORWARD_INTENT_REQUEST_CODE = 3;
    public static final int CLOSE_INTENT_REQUEST_CODE = 4;
    public static final String PLAY_PAUSE_INTENT_ACTION = "play_pause";
    public static final String FORWARD_INTENT_ACTION = "forward";
    public static final String CLOSE_INTENT_ACTION = "close";
    public static final String BUTTON_CLICKED_MSG = "notification button";
}
