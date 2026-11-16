package com.example.trafficapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class TrafficViewModel extends AndroidViewModel {


    private static final long UPDATE_INTERVAL_MINUTES = 2;

    private final MutableLiveData<List<Situation>> situations =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> status = new MutableLiveData<>("Loading…");

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final TrafficRepository repository;

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);
        scheduler.scheduleWithFixedDelay(this::refresh, 0,
                UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    // Runs on the background thread
    private void refresh() {
        status.postValue("Updating…");
        List<Situation> data = repository.fetchAll();
        situations.postValue(data);
        status.postValue(repository.getStatus());
    }

    public void refreshNow() {
        scheduler.execute(this::refresh);
    }

    public LiveData<List<Situation>> getSituations() { return situations; }
    public LiveData<String> getStatus() { return status; }

    @Override
    protected void onCleared() {
        scheduler.shutdownNow();
        super.onCleared();
    }
}
