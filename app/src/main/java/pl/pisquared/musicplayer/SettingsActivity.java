package pl.pisquared.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener
{
    private CheckBox cbShufflePlay;
    private CheckBox cbAutoPlayNext;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initViews();
        preferences = getSharedPreferences(Constants.SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        boolean shufflePlay = preferences.getBoolean(Constants.SHUFFLE_PLAY_KEY, false);
        boolean autoPlayNext = preferences.getBoolean(Constants.AUTO_PLAY_NEXT_KEY, true);
        cbShufflePlay.setChecked(shufflePlay);
        cbAutoPlayNext.setChecked(autoPlayNext);
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
        int id = buttonView.getId();
        switch(id)
        {
            case R.id.cb_shuffle_play:
                shufflePlayToggle(isChecked);
                break;
            case R.id.cb_auto_play_next:
                autoPlayNextToggle(isChecked);
                break;
        }
    }

    private void shufflePlayToggle(boolean isChecked)
    {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constants.SHUFFLE_PLAY_KEY, isChecked);
        editor.commit();
    }

    private void autoPlayNextToggle(boolean isChecked)
    {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constants.AUTO_PLAY_NEXT_KEY, isChecked);
        editor.commit();
    }
}
