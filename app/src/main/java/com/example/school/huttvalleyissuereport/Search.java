package com.example.school.huttvalleyissuereport;

/**
 * Created by School on 8/08/2017.
 */

class Search {
    private String keyword;
    private String category;
    private String urgency;
    private String location;
    private Long dateFrom;
    private Long dateTo;

    Search(String keyword, String category, String urgency, String location, Long dateFrom, Long dateTo){
        this.keyword = keyword;
        this.category = category;
        this.urgency = urgency;
        this.location = location;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    String getUrgency() {
        return urgency;
    }

    Long getDateFrom() {
        return dateFrom;
    }

    Long getDateTo() {
        return dateTo;
    }

    String getCategory() {
        return category;
    }

    String getKeyword() {
        return keyword;
    }

    public String getLocation() {
        return location;
    }
}
