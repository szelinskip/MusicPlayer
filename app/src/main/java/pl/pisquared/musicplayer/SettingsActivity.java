package pl.pisquared.musicplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener
{
    private CheckBox cbShufflePlay;
    private CheckBox cbAutoPlayNext;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initViews();
    }

    private void initViews()
    {
        cbShufflePlay = findViewById(R.id.cb_shuffle_play);
        cbAutoPlayNext = findViewById(R.id.cb_auto_play_next);
        cbShufflePlay.setOnCheckedChangeListener(this);
        cbAutoPlayNext.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        
    }
}
