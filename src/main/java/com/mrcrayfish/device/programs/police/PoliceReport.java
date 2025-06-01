package com.mrcrayfish.device.programs.police;

public class PoliceReport {
    private String suspectName;
    private String location;
    private String date;
    private String details;
    
    public PoliceReport(String suspectName, String location, String date, String details) {
        this.suspectName = suspectName;
        this.location = location;
        this.date = date;
        this.details = details;
    }
    
    public String getSuspectName() {
        return suspectName;
    }
    
    public String getLocation() {
        return location;
    }
    
    public String getDate() {
        return date;
    }
    
    public String getDetails() {
        return details;
    }
    
    @Override
    public String toString() {
        return suspectName + " - " + date;
    }
} 