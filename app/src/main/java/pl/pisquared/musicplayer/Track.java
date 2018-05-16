package pl.pisquared.musicplayer;

public class Track
{
    private long id;
    private String title;
    private String author;
    private long duration;
    private String fileName;

    public Track(long id, String title, String author, long duration, String fileName)
    {
        this.id = id;
        this.title = title;
        this.author = author;
        this.duration = duration;
        this.fileName = fileName;
    }

    public long getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public String getAuthor()
    {
        return author;
    }

    public long getDuration()
    {
        return duration;
    }

    public String getFileName()
    {
        return fileName;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj != null && obj instanceof Track)
        {
            return this.id == ((Track) obj).id;
        }
        else
            return false;
    }
}
