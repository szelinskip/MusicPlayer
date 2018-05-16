package pl.pisquared.musicplayer.utils;

import java.util.Locale;

public class StringTrackUtils
{
    private static final String INVALID_TITLE_PATTERN = "<unknown>";
    private static final String INVALID_AUTHOR_PATTERN = "<unknown>";

    public static boolean isValidTitle(String title)
    {
        return !title.equals(INVALID_TITLE_PATTERN);
    }

    public static boolean isValidAuthor(String author)
    {
        return !author.equals(INVALID_AUTHOR_PATTERN);
    }

    public static String getTimeRepresentation(int minutes, int seconds)
    {
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }
}
