package com.example.trafficapp;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class parser {
    private static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";

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
                        current.setId(xpp.getAttributeValue(null, "id"));
                        // xsi:type attribute, e.g. "MaintenanceWorks" (needed for icons)
                        current.setRecordType(xpp.getAttributeValue(XSI, "type"));

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
        Matcher m = Pattern.compile("\\b([AM]\\d{1,4})\\b").matcher(text);
        return m.find() ? m.group(1) : "Unknown";
    }
}
