package com.example.trafficapp;

public class map_location {
    private final String role;
    private final double lat;
    private final double lon;

    public map_location(String role, double lat, double lon){
        this.role = role; this.lat = lat; this.lon = lon;
    }
    public String getRole() { return role; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }

    @Override
    public String toString() {
        return role + "(" + lat + ", " + lon + ")";
    }
}
