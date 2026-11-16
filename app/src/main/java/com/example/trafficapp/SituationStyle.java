package com.example.trafficapp;

public class SituationStyle {

    public static int iconFor(Situation s) {
        String record = lower(s.getRecordType());
        String sub = lower(s.getSubType());
        String all = record + " " + sub;

        if (Situation.ROADWORKS.equals(s.getCategory())
                || record.contains("maintenance") || record.contains("construction")) {
            return R.drawable.ic_roadworks;
        }
        if (all.contains("accident")) return R.drawable.ic_accident;
        if (sub.contains("roadclosed") || sub.contains("carriagewayclosure")) return R.drawable.ic_road_closed;
        if (sub.contains("lane") || sub.contains("contraflow") || sub.contains("narrow")) return R.drawable.ic_lane_closed;
        if (record.contains("abnormaltraffic") || sub.contains("queu")
                || sub.contains("slow") || sub.contains("stationary")) return R.drawable.ic_queue;
        if (record.contains("vehicleobstruction") || sub.contains("broken")) return R.drawable.ic_breakdown;
        if (record.contains("weather") || record.contains("environment") || record.contains("poorenvironment")
                || all.contains("flood") || all.contains("ice") || all.contains("snow")) return R.drawable.ic_weather;
        if (record.contains("obstruction") || record.contains("animal")) return R.drawable.ic_obstruction;
        return R.drawable.ic_info;
    }

    public static final int DELAY_NONE = 0;
    public static final int DELAY_SHORT = 1;
    public static final int DELAY_MEDIUM = 2;
    public static final int DELAY_LONG = 3;
    public static final int DELAY_UNKNOWN = 4;

    private static final long ONE_DAY_HOURS = 24;
    private static final long ONE_WEEK_HOURS = 24 * 7;

    public static int delayLevel(Situation s) {
        if (!Situation.ROADWORKS.equals(s.getCategory())) return DELAY_NONE;

        String delays = lower(s.getImpact().getDelaysType());
        long hours = s.getDurationHours();

        if (delays.equals("longdelays") || hours > ONE_WEEK_HOURS) return DELAY_LONG;
        if (hours >= ONE_DAY_HOURS) return DELAY_MEDIUM;
        if (hours >= 0) return DELAY_SHORT;
        if (delays.equals("delays")) return DELAY_MEDIUM;
        return DELAY_UNKNOWN;
    }

    public static int delayColourRes(Situation s) {
        switch (delayLevel(s)) {
            case DELAY_SHORT: return R.color.ForestGreen;
            case DELAY_MEDIUM: return R.color.Orange;
            case DELAY_LONG: return R.color.FireBrick;
            case DELAY_UNKNOWN: return R.color.Gray;
            default: return android.R.color.transparent;
        }
    }

    public static String delayLabel(Situation s) {
        switch (delayLevel(s)) {
            case DELAY_SHORT: return "Short (under 1 day)";
            case DELAY_MEDIUM: return "Medium (1 to 7 days)";
            case DELAY_LONG: return "Long (over 7 days or long delays)";
            case DELAY_UNKNOWN: return "Unknown duration";
            default: return "";
        }
    }

    private static String lower(String text) {
        return text == null ? "" : text.toLowerCase();
    }
}
