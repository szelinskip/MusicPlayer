package pl.pisquared.musicplayer.viewmodel;

import android.app.Application;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

public class MusicPlayerViewModelFactory extends ViewModelProvider.NewInstanceFactory
{
    private Application appContext;

    public MusicPlayerViewModelFactory(Application appContext)
    {
        this.appContext = appContext;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass)
    {
        return (T) new MusicPlayerViewModel(appContext);
    }
}
