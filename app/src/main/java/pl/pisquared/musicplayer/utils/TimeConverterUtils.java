package pl.pisquared.musicplayer.utils;

import android.support.v4.util.Pair;


public class TimeConverterUtils
{
    public static Pair<Integer, Integer> getMinutesSecondsFromMilliSeconds(long ms)
    {
        int seconds = (int) (ms / 1000);
        int minutes = seconds / 60;
        int leftSeconds = seconds % 60;
        return new Pair<>(minutes, leftSeconds);
    }
}
