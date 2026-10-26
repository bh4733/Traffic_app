package com.example.trafficapp;

public class impact {
    private int lanesRestricted = -1;
    private int operationalLanes = -1;
    private String delaysType;

    public int getLanesRestricted() { return lanesRestricted; }
    public void setLanesRestricted(int lanesRestricted) { this.lanesRestricted = lanesRestricted; }

    public int getOperationalLanes() { return operationalLanes; }
    public void setOperationalLanes(int operationalLanes) { this.operationalLanes = operationalLanes; }

    public String getDelaysType() { return delaysType; }
    public void setDelaysType(String delaysType) { this.delaysType = delaysType; }
}
