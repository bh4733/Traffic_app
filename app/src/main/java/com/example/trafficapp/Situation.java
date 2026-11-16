package com.example.trafficapp;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Situation {
    public static final String INCIDENT = "Incident";
    public static final String ROADWORKS = "Roadworks";

    private String id;
    private String category;     // which feed it came from: INCIDENT or ROADWORKS
    private String recordType;   // xsi:type attribute, e.g. MaintenanceWorks
    private String subType;      // e.g. roadClosed, queuingTraffic, roadworks
    private String comment;      // generalPublicComment text
    private String roadName;     // extracted from the comment, e.g. "A78"
    private String startTime;
    private String endTime;
    private String probability;
    private impact impact = new impact();
    private ArrayList<map_location> locations = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getProbability() { return probability; }
    public void setProbability(String probability) { this.probability = probability; }

    public impact getImpact() { return impact; }
    public void setImpact(impact impact) { this.impact = impact; }

    public ArrayList<map_location> getLocations() { return locations; }
    public void setLocations(ArrayList<map_location> locations) { this.locations = locations; }



    public String getDisplayType() {
        String t = (subType != null) ? subType : recordType;
        if (t == null) return category;
        String spaced = t.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    public long getDurationHours() {
        try {
            return Duration.between(OffsetDateTime.parse(startTime),
                    OffsetDateTime.parse(endTime)).toHours();
        } catch (Exception e) { return -1; }   // missing or bad dates
    }

    public static String formatTime(String iso) {
        if (iso == null) return "Not given";
        try {
            return OffsetDateTime.parse(iso)
                    .format(DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm"));
        } catch (Exception e) { return iso; }
    }
    public static String readable(String raw) {
        if (raw == null || raw.isEmpty()) return "Not stated";
        String spaced = raw.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }
}