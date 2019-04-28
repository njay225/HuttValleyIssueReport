package com.example.school.huttvalleyissuereport;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Objects;

public class SearchResult extends AppCompatActivity {
    
    //Variable for logging int console
    private final static String TAG = "SearchResult";

    //Initialing List View Widget
    private ListView reportListView;

    //Initialising ArrayList to Hold Reports
    private ArrayList<Report> searchReportArray =  new ArrayList<Report>();

    //Layout Inflater to inflate report list
    private LayoutInflater layoutInflater;

    //Getting Search from Home Activity
    private Search search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        //The title of the activity is changed
        getSupportActionBar().setTitle("Search Results");

        //The search class is set to the public class set in the homepage
        search = HomeActivity.search;

        //The reportListView is linked to the widget
        reportListView = (ListView) findViewById(R.id.reportListView);

        //the reports are then checked to see if they meet the search criteria
        checkReports();

    }

    private void checkReports(){
        //All reports are looped through
        for(int i = 0; i < MostRecent.reportsArray.size(); i++){
            //A boolean reportMatchesSearch is initialised to true
            Boolean reportMatchesSearch = true;
            //the current report in the loop is retrieved
            Report report = MostRecent.reportsArray.get(i);

            //A data format is initialised
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");

            //The date from the report class is then converted to the date format
            String reportDate = dateFormat.format(report.getDateAdded());

            //Initialises the millisecond dateFrom and DateTo
            Long reportDateMilli = null;
            try {
                //Convert the date string to milliseconds
                reportDateMilli = dateFormat.parse(reportDate).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "checkReports: report date = " + reportDateMilli + " dateFrom = " + search.getDateFrom() + " dateTo = " + search.getDateTo());

            //the report is checked to see if it doesn't fall within the date range specified
            if(reportDateMilli < search.getDateFrom() || reportDateMilli > search.getDateTo()) {
                reportMatchesSearch = false;
                Log.d(TAG, "checkReports: Date out of Range");

            //The report is checked to see if the urgency specified is equal to the report urgency given that it isn't the default urgency
            }else if(!Objects.equals(report.getUrgency(), search.getUrgency()) && !Objects.equals(search.getUrgency(), getResources().getString(R.string.default_urgency))){
                reportMatchesSearch = false;
                Log.d(TAG, "checkReports: Urgency not equal");

                //The report is checked to see if the category specified is equal to the report category given that it isn't the default category
            }else if(!Objects.equals(report.getCategory(), search.getCategory()) && !Objects.equals(search.getCategory(), getResources().getString(R.string.default_category))){
                reportMatchesSearch = false;
                Log.d(TAG, "checkReports: Category not equal");

            //The report title and description is checked to see if contains the keyword
            }else if(!report.getTitle().toLowerCase().contains(search.getKeyword().toLowerCase()) && !report.getDescription().toLowerCase().contains(search.getKeyword().toLowerCase())  && search.getKeyword() != null){
                reportMatchesSearch = false;
                Log.d(TAG, "checkReports: No keyword match");

            //The report address is then checked to see if it contains the search location
            }else if(!report.getAddress().toLowerCase().contains(search.getLocation().toLowerCase()) && search.getLocation() != null){
                reportMatchesSearch = false;
                Log.d(TAG, "checkReports: No Location match");
            }

            //If any of the search criteria above fails, then reportMatchesSearch is set to false

            //if reportMatchesSearch is true
            if(reportMatchesSearch){
                //then the report is added to the searchReportArray
                searchReportArray.add(report);
                //Then the listview is populated
                populateList();
            }
        }
    }

    private void populateList(){
        TaskAdapter adapter = new TaskAdapter(this, searchReportArray);
        reportListView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        finish();
    }

}
