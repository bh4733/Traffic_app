package com.example.trafficapp;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class parser {

    public ArrayList<Situation> parse(String xml, String category) {
        ArrayList<Situation> list = new ArrayList<>();
        Situation current = null;
        boolean inPublicComment = false;
        String locationRole = "point";
        double pendingLat = Double.NaN;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xml));

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xpp.getName();

                    if (tag.equalsIgnoreCase("situationRecord")) {
                        current = new Situation();
                        current.setCategory(category);
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            String attr = xpp.getAttributeName(i);
                            if (attr.equalsIgnoreCase("type")) {
                                current.setRecordType(xpp.getAttributeValue(i));
                            } else if (attr.equalsIgnoreCase("id")) {
                                current.setId(xpp.getAttributeValue(i));
                            }
                        }

                    }
                    else if (current != null) {
                        if (tag.equalsIgnoreCase("generalPublicComment")) {
                            inPublicComment = true;
                        } else if (tag.equalsIgnoreCase("value") && inPublicComment
                                && current.getComment() == null) {
                            current.setComment(xpp.nextText().trim());
                        } else if (tag.equalsIgnoreCase("overallStartTime")) {
                            current.setStartTime(xpp.nextText().trim());
                        } else if (tag.equalsIgnoreCase("overallEndTime")) {
                            current.setEndTime(xpp.nextText().trim());
                        } else if (tag.equalsIgnoreCase("probabilityOfOccurrence")) {
                            current.setProbability(xpp.nextText().trim());
                        } else if (tag.equalsIgnoreCase("numberOfLanesRestricted")) {
                            current.getImpact().setLanesRestricted(toInt(xpp.nextText()));
                        } else if (tag.equalsIgnoreCase("numberOfOperationalLanes")) {
                            current.getImpact().setOperationalLanes(toInt(xpp.nextText()));
                        } else if (tag.equalsIgnoreCase("delaysType")) {
                            current.getImpact().setDelaysType(xpp.nextText().trim());
                        } else if (tag.equalsIgnoreCase("roadOrCarriagewayOrLaneManagementType")
                                || tag.equalsIgnoreCase("abnormalTrafficType")
                                || tag.equalsIgnoreCase("roadMaintenanceType")) {
                            current.setSubType(xpp.nextText().trim());
                        } else if (tag.equalsIgnoreCase("from") || tag.equalsIgnoreCase("to")) {
                            locationRole = tag.toLowerCase();
                        } else if (tag.equalsIgnoreCase("latitude")) {
                            pendingLat = toDouble(xpp.nextText());
                        } else if (tag.equalsIgnoreCase("longitude")) {
                            double lon = toDouble(xpp.nextText());
                            if (!Double.isNaN(pendingLat) && !Double.isNaN(lon)) {
                                current.getLocations().add(new map_location(locationRole, pendingLat, lon));
                            }
                            pendingLat = Double.NaN;
                        }

                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    String tag = xpp.getName();
                    if (tag.equalsIgnoreCase("generalPublicComment")) {
                        inPublicComment = false;
                    } else if (tag.equalsIgnoreCase("from") || tag.equalsIgnoreCase("to")) {
                        locationRole = "point";
                    } else if (tag.equalsIgnoreCase("situationRecord") && current != null) {
                        current.setRoadName(extractRoad(current.getComment()));
                        Log.d("Parsing", "New situation: " + current.toString());
                        list.add(current);
                        current = null;
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException | NumberFormatException e) {
            Log.e("Parsing", "EXCEPTION " + e);
        }
        Log.d("Parsing", category + ": " + list.size() + " situations");
        for (Situation s : list) {
            Log.d("ParsedList", s.toString());
        }
        return list;
    }

    private double toDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return Double.NaN; }
    }

    private int toInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return -1; }
    }

    private String extractRoad(String text) {
        if (text == null) return "Unknown";
        String[] words = text.split(" ");
        for (String word : words) {
            if (word.startsWith("(")) word = word.substring(1);
            if (word.length() < 2) continue;
            char first = word.charAt(0);
            if (first != 'A' && first != 'M') continue;
            int end = 1;
            while (end < word.length() && Character.isDigit(word.charAt(end))) end++;
            int digits = end - 1;
            if (digits < 1 || digits > 4) continue;
            if (end < word.length() && Character.isLetterOrDigit(word.charAt(end))) continue;
            return word.substring(0, end);
        }
        return "Unknown";
    }
}
