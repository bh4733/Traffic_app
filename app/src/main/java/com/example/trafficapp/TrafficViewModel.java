package com.example.trafficapp;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrafficViewModel extends AndroidViewModel {

    private static final long UPDATE_INTERVAL_MINUTES = 2;
    private static final long UPDATE_INTERVAL_MS = UPDATE_INTERVAL_MINUTES * 60 * 1000;

    private final MutableLiveData<List<Situation>> situations =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> status = new MutableLiveData<>("Loading…");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final TrafficRepository repository;

    private final Runnable autoUpdate = new Runnable() {
        @Override
        public void run() {
            refreshNow();
            mainHandler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);
        mainHandler.post(autoUpdate);
    }

    private void refresh() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                status.setValue("Updating…");
            }
        });

        final List<Situation> data = repository.fetchAll();
        final String message = repository.getStatus();

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                situations.setValue(data);
                status.setValue(message);
            }
        });
    }

    public void refreshNow() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
    }

    public LiveData<List<Situation>> getSituations() { return situations; }
    public LiveData<String> getStatus() { return status; }

    @Override
    protected void onCleared() {
        mainHandler.removeCallbacks(autoUpdate);
        executor.shutdownNow();
        super.onCleared();
    }
}
