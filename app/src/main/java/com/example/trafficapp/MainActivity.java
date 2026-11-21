package com.example.trafficapp;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.ViewSwitcher;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import androidx.lifecycle.Observer;
import java.util.Comparator;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnClickListener, AdapterView.OnItemSelectedListener, OnMapReadyCallback {
    private TextView statusText;
    private TextView countText;
    private View delayLegend;
    private Button startButton;
    private RadioButton allRadio, incidentRadio, roadworksRadio;
    private SituationAdapter adapter;

    private TrafficViewModel viewModel;
    private List<Situation> allSituations = new ArrayList<>();

    private static final String ALL_ROADS = "ALL";
    private static final String KEY_ROADS = "selectedRoads";
    private Spinner roadSpinner;
    private ArrayAdapter<String> roadAdapter;
    private final List<String> roadKeys = new ArrayList<>();
    private String selectedRoad = ALL_ROADS;

    private static final String KEY_DISTANCE = "selectedDistance";
    private static final int[] DISTANCES_KM = {0, 5, 10, 25, 50, 100};
    private static final double ORIGIN_LAT = 55.8666;
    private static final double ORIGIN_LON = -4.2504;
    private static final String ORIGIN_NAME = "Glasgow Caledonian University";
    private Spinner distanceSpinner;
    private TextView originText;
    private int selectedDistanceKm = 0;
    private static final String KEY_SHOWING_MAP = "showingMap";
    private ViewSwitcher viewSwitch;
    private Button mapButton;
    private GoogleMap googleMap;
    private boolean showingMap = false;
    private List<Situation> shownSituations = new ArrayList<>();
    private LatLngBounds pendingBounds;
    private LatLng pendingSinglePoint;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusText = findViewById(R.id.statusText);
        countText = findViewById(R.id.countText);
        delayLegend = findViewById(R.id.delayLegend);
        startButton = findViewById(R.id.startButton);
        allRadio = findViewById(R.id.allRadio);
        incidentRadio = findViewById(R.id.incidentRadio);
        roadworksRadio = findViewById(R.id.roadworksRadio);

        startButton.setOnClickListener(this);
        allRadio.setOnClickListener(this);
        incidentRadio.setOnClickListener(this);
        roadworksRadio.setOnClickListener(this);

        RecyclerView rv = findViewById(R.id.recView);
        adapter = new SituationAdapter(new SituationAdapter.OnSituationClickListener() {
            @Override
            public void onSituationClick(Situation s) {
                showDetails(s);
            }
        });
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(llm);
        rv.setAdapter(adapter);

        if (savedInstanceState != null) {
            selectedRoad = savedInstanceState.getString(KEY_ROADS, ALL_ROADS);
            selectedDistanceKm = savedInstanceState.getInt(KEY_DISTANCE, 0);
            showingMap = savedInstanceState.getBoolean(KEY_SHOWING_MAP, false);
        }

        roadSpinner = findViewById(R.id.roadSpinner);
        roadAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>());
        roadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roadSpinner.setAdapter(roadAdapter);
        roadSpinner.setOnItemSelectedListener(this);

        originText = findViewById(R.id.originText);
        distanceSpinner = findViewById(R.id.distanceSpinner);
        ArrayAdapter<CharSequence> distanceAdapter = ArrayAdapter.createFromResource(this,
                R.array.distance_options, android.R.layout.simple_spinner_item);
        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        distanceSpinner.setAdapter(distanceAdapter);
        distanceSpinner.setSelection(distanceIndexOf(selectedDistanceKm), false);
        distanceSpinner.setOnItemSelectedListener(this);

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);
        viewModel.getStatus().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String msg) {
                statusText.setText(msg);
            }
        });
        viewModel.getSituations().observe(this, new Observer<List<Situation>>() {
            @Override
            public void onChanged(List<Situation> list) {
                allSituations = list;
                updateRoadSpinner();
                applyFilter();
                logSituationTypes(list);
            }
        });
        viewSwitch = findViewById(R.id.viewSwitch);
        mapButton = findViewById(R.id.mapButton);
        mapButton.setOnClickListener(this);
        if (showingMap) {
            viewSwitch.showNext();
            mapButton.setText("List");
        }
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null){
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == startButton) {
            viewModel.refreshNow();
        } else if (v == allRadio || v == incidentRadio || v == roadworksRadio) {
            applyFilter();
        } else if (v == mapButton){
            setMapVisible(!showingMap);
        }
    }

    private void applyFilter() {
        boolean byDistance = selectedDistanceKm > 0;
        List<Situation> shown = new ArrayList<>();
        for (Situation s : allSituations) {
            boolean typeOk = allRadio.isChecked()
                    || (incidentRadio.isChecked() && Situation.INCIDENT.equals(s.getCategory()))
                    || (roadworksRadio.isChecked() && Situation.ROADWORKS.equals(s.getCategory()));
            boolean roadOk = ALL_ROADS.equals(selectedRoad)
                    || selectedRoad.equals(s.getRoadName());
            boolean distanceOk = !byDistance || (s.hasLocation()
                    && s.distanceKmFrom(ORIGIN_LAT, ORIGIN_LON) <= selectedDistanceKm);
            if (typeOk && roadOk && distanceOk) {
                shown.add(s);
            }
        }

        if (byDistance) {
            shown.sort(new Comparator<Situation>() {
                @Override
                public int compare(Situation a, Situation b) {
                    return Float.compare(a.distanceKmFrom(ORIGIN_LAT, ORIGIN_LON),
                            b.distanceKmFrom(ORIGIN_LAT, ORIGIN_LON));
                }
            });
            originText.setText("Distances from " + ORIGIN_NAME + ", nearest first");
            originText.setVisibility(View.VISIBLE);
        } else {
            originText.setVisibility(View.GONE);
        }

        delayLegend.setVisibility(incidentRadio.isChecked() ? View.GONE : View.VISIBLE);
        adapter.setOrigin(byDistance, ORIGIN_LAT, ORIGIN_LON);
        adapter.setData(shown);

        StringBuilder count = new StringBuilder(shown.size() + " situation(s)");
        if (!ALL_ROADS.equals(selectedRoad)) count.append(" on ").append(selectedRoad);
        if (byDistance) count.append(" within ").append(selectedDistanceKm).append(" km");
        countText.setText(count.toString());

        shownSituations = shown;
        drawOnMap(shownSituations);
    }

    private int distanceIndexOf(int km) {
        for (int i = 0; i < DISTANCES_KM.length; i++) {
            if (DISTANCES_KM[i] == km) return i;
        }
        return 0;
    }

    private void showDetails(Situation s) {
        View view = getLayoutInflater().inflate(R.layout.dialog_situation, null);

        ImageView icon = view.findViewById(R.id.dIcon);
        icon.setImageResource(SituationStyle.iconFor(s));

        ((TextView) view.findViewById(R.id.dType)).setText(s.getDisplayType());
        ((TextView) view.findViewById(R.id.dCategory)).setText(s.getCategory());
        ((TextView) view.findViewById(R.id.dRoad)).setText("Road: " + s.getRoadName());
        ((TextView) view.findViewById(R.id.dComment)).setText(
                s.getComment() != null ? s.getComment() : "No description");

        ((TextView) view.findViewById(R.id.dStart)).setText(
                "Start: " + Situation.formatTime(s.getStartTime()));
        ((TextView) view.findViewById(R.id.dEnd)).setText(
                "End: " + Situation.formatTime(s.getEndTime()));
        TextView durationView = view.findViewById(R.id.dDuration);
        String durationText = "Duration: " + formatDuration(s.getDurationHours());
        if (SituationStyle.delayLevel(s) != SituationStyle.DELAY_NONE) {
            durationText += "\nDelay rating: " + SituationStyle.delayLabel(s);
            durationView.setTextColor(getResources().getColor(SituationStyle.delayColourRes(s), null));
        }
        durationView.setText(durationText);

        impact imp = s.getImpact();
        String lanes;
        if (imp.getLanesRestricted() >= 0) {
            lanes = imp.getLanesRestricted() + " lane(s) restricted";
        } else if (imp.getOperationalLanes() == 0) {
            lanes = "Road closed - no lanes open";
        } else if (imp.getOperationalLanes() > 0) {
            lanes = imp.getOperationalLanes() + " lane(s) open";
        } else {
            lanes = "Not stated";
        }
        ((TextView) view.findViewById(R.id.dLanes)).setText("Lanes: " + lanes);
        ((TextView) view.findViewById(R.id.dDelays)).setText(
                "Delays: " + Situation.readable(imp.getDelaysType()));
        ((TextView) view.findViewById(R.id.dProbability)).setText(
                "Probability: " + Situation.readable(s.getProbability()));

        StringBuilder locs = new StringBuilder();
        for (map_location loc : s.getLocations()) {
            if (locs.length() > 0) locs.append("\n");
            locs.append(Situation.readable(loc.getRole())).append(": ")
                    .append(String.format(java.util.Locale.UK, "%.5f, %.5f", loc.getLat(), loc.getLon()));
        }
        ((TextView) view.findViewById(R.id.dLocations)).setText(
                locs.length() > 0 ? locs.toString() : "No coordinates given");

        ((TextView) view.findViewById(R.id.dId)).setText("Reference: " + s.getId());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.setNeutralButton("Show on map", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                List<Situation> single = new ArrayList<>();
                single.add(s);
                drawOnMap(single);
                setMapVisible(true);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private String formatDuration(long hours) {
        if (hours < 0) return "Not stated";
        if (hours < 24) return hours + " hour(s)";
        long days = hours / 24;
        return days + " day(s)" + (hours % 24 > 0 ? ", " + (hours % 24) + " hour(s)" : "");
    }

    private void logSituationTypes(List<Situation> list) {
        TreeSet<String> types = new TreeSet<>();
        for (Situation s : list) {
            types.add(s.getCategory() + " | " + s.getRecordType() + " | " + s.getSubType());
        }
        Log.d("Types", types.size() + " distinct types from " + list.size() + " situations");
        for (String t : types) {
            Log.d("Types", t);
        }
    }

    private void updateRoadSpinner() {
        TreeMap<String, Integer> counts = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return compareRoads(a, b);
            }
        });
        for (Situation s : allSituations) {
            String road = s.getRoadName() != null ? s.getRoadName() : "Unknown";
            counts.put(road, counts.getOrDefault(road, 0) + 1);
        }

        roadKeys.clear();
        List<String> labels = new ArrayList<>();
        roadKeys.add(ALL_ROADS);
        labels.add("All roads (" + allSituations.size() + ")");
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            roadKeys.add(e.getKey());
            labels.add(e.getKey() + " (" + e.getValue() + ")");
        }

        roadAdapter.clear();
        roadAdapter.addAll(labels);
        roadAdapter.notifyDataSetChanged();

        int index = roadKeys.indexOf(selectedRoad);
        if (index < 0) {
            selectedRoad = ALL_ROADS;
            index = 0;
        }
        roadSpinner.setSelection(index, false);
    }

    private static int compareRoads(String a, String b) {
        if (a.equals(b)) return 0;
        if (a.equals("Unknown")) return 1;
        if (b.equals("Unknown")) return -1;
        int byLetter = Character.compare(a.charAt(0), b.charAt(0));
        if (byLetter != 0) return byLetter;
        return Integer.compare(roadNumber(a), roadNumber(b));
    }

    private static int roadNumber(String road) {
        try {
            return Integer.parseInt(road.substring(1));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == roadSpinner && position < roadKeys.size()) {
            selectedRoad = roadKeys.get(position);
            applyFilter();
        } else if (parent == distanceSpinner && position < DISTANCES_KM.length) {
            selectedDistanceKm = DISTANCES_KM[position];
            applyFilter();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ROADS, selectedRoad);
        outState.putInt(KEY_DISTANCE, selectedDistanceKm);
        outState.putBoolean(KEY_SHOWING_MAP, showingMap);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Object tag = marker.getTag();
                if (tag instanceof Situation) {
                    showDetails((Situation) tag);
                }
            }
        });
        drawOnMap(shownSituations);
    }

    private void setMapVisible(boolean visible) {
        if (visible && viewSwitch.getDisplayedChild() == 0) {
            viewSwitch.showNext();
        } else if (!visible && viewSwitch.getDisplayedChild() == 1) {
            viewSwitch.showPrevious();
        }
        showingMap = visible;
        mapButton.setText(visible ? "List" : "Map");
        if (visible) {
            postFitCamera();
        }
    }

    private void postFitCamera() {
        viewSwitch.post(new Runnable() {
            @Override
            public void run() {
                fitCamera();
            }
        });
    }

    private void drawOnMap(List<Situation> list) {
        if (googleMap == null) return;
        googleMap.clear();

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int pointCount = 0;
        LatLng lastPoint = null;

        for (Situation s : list) {
            if (!s.hasLocation()) continue;

            boolean roadworks = Situation.ROADWORKS.equals(s.getCategory());

            for (map_location loc : s.getLocations()) {
                LatLng point = new LatLng(loc.getLat(), loc.getLon());
                bounds.include(point);
                pointCount++;
                lastPoint = point;
            }

            map_location first = s.getLocations().get(0);
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(first.getLat(), first.getLon()))
                    .title(s.getDisplayType() + " · " + s.getRoadName())
                    .snippet("Tap for details")
                    .icon(BitmapDescriptorFactory.defaultMarker(roadworks
                            ? BitmapDescriptorFactory.HUE_ORANGE
                            : BitmapDescriptorFactory.HUE_RED)));
            if (marker != null) {
                marker.setTag(s);
            }
        }

        if (selectedDistanceKm > 0) {
            LatLng origin = new LatLng(ORIGIN_LAT, ORIGIN_LON);
            googleMap.addMarker(new MarkerOptions()
                    .position(origin)
                    .title(ORIGIN_NAME)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            googleMap.addCircle(new CircleOptions()
                    .center(origin)
                    .radius(selectedDistanceKm * 1000.0)
                    .strokeWidth(3)
                    .strokeColor(getResources().getColor(R.color.Blue, null)));
            bounds.include(origin);
            pointCount++;
            lastPoint = origin;
        }

        pendingBounds = pointCount > 1 ? bounds.build() : null;
        pendingSinglePoint = pointCount == 1 ? lastPoint : null;
        if (showingMap) {
            postFitCamera();
        }
    }

    private void fitCamera() {
        if (googleMap == null) return;
        if (pendingBounds != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(pendingBounds, 100));
        } else if (pendingSinglePoint != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pendingSinglePoint, 13));
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(56.8, -4.2), 6));
        }
    }
}
