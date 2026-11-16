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
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import androidx.appcompat.app.AlertDialog;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnClickListener, AdapterView.OnItemSelectedListener {
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
        adapter = new SituationAdapter(this::showDetails);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(llm);
        rv.setAdapter(adapter);

        if (savedInstanceState != null) {
            selectedRoad = savedInstanceState.getString(KEY_ROADS, ALL_ROADS);
        }

        roadSpinner = findViewById(R.id.roadSpinner);
        roadAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>());
        roadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roadSpinner.setAdapter(roadAdapter);
        roadSpinner.setOnItemSelectedListener(this);

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);
        viewModel.getStatus().observe(this, msg -> statusText.setText(msg));
        viewModel.getSituations().observe(this, list -> {
            allSituations = list;
            updateRoadSpinner();
            applyFilter();
            logSituationTypes(list);
        });
    }

    @Override
    public void onClick(View v) {
        if (v == startButton) {
            viewModel.refreshNow();
        } else if (v == allRadio || v == incidentRadio || v == roadworksRadio) {
            applyFilter();
        }
    }

    private void applyFilter() {
        List<Situation> shown = new ArrayList<>();
        for (Situation s : allSituations) {
            if (allRadio.isChecked()
                    || (incidentRadio.isChecked() && Situation.INCIDENT.equals(s.getCategory()))
                    || (roadworksRadio.isChecked() && Situation.ROADWORKS.equals(s.getCategory()))) {
                shown.add(s);
            }
        }
        delayLegend.setVisibility(incidentRadio.isChecked() ? View.GONE : View.VISIBLE);
        adapter.setData(shown);
        countText.setText(shown.size() + " situation(s)");
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
            durationView.setTextColor(ContextCompat.getColor(this, SituationStyle.delayColourRes(s)));
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
        builder.setPositiveButton("Close", (dialog, id) -> dialog.dismiss());
        builder.setNeutralButton("Show on map", (dialog, id) ->
                Toast.makeText(this, "Map coming soon", Toast.LENGTH_SHORT).show());
        builder.create().show();
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
        TreeMap<String, Integer> counts = new TreeMap<>(MainActivity::compareRoads);
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
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ROADS, selectedRoad);
    }
}
