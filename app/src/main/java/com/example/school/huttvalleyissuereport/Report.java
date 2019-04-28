package com.example.school.huttvalleyissuereport;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by School on 30/07/2017.
 */

class Report implements Serializable{
    public String title;
    public String description;
    public String category;
    public String urgency;
    public String address;
    public Long dateAdded;
    public Double latitude;
    public Double longitude;
    public String creator;
    public String fileOne;
    public String fileTwo;
    public String fileThree;
    public Boolean resolved;
    public String reportID;
    public int votes;

    Report(String title, String description, String category, String urgency, String address,
                Long dateAdded, Double latitude, Double longitude, String creator, String fileOne,
                String fileTwo, String fileThree, Boolean resolved, String reportID, int votes) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.urgency = urgency;
        this.address = address;
        this.dateAdded = dateAdded;
        this.latitude = latitude;
        this.longitude = longitude;
        this.creator = creator;
        this.fileOne = fileOne;
        this.fileTwo = fileTwo;
        this.fileThree = fileThree;
        this.resolved = resolved;
        this.reportID = reportID;
        this.votes = votes;
    }

    public Report (){

    }

    String getCategory() {
        return category;
    }

    public String getUrgency() {
        return urgency;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public int getVotes() {
        return votes;
    }

    public Long getDateAdded() {
        return dateAdded;
    }

    public String getAddress() {
        return address;
    }

    public String getCreator() {
        return creator;
    }

    public String getDescription() {
        return description;
    }

    public String getFileOne() {
        return fileOne;
    }

    public String getFileThree() {
        return fileThree;
    }

    public String getFileTwo() {
        return fileTwo;
    }

    public String getTitle() {
        return title;
    }

    public String getReportID() {
        return reportID;
    }
}

