package com.example.school.huttvalleyissuereport;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Objects;

public class ResolvedReportsActivity extends AppCompatActivity {

    //Variable for Logging to Console
    private static final String TAG = "MyReports";

    //Array list to hold user's reports
    private ArrayList<Report> resolvedReports = new ArrayList<>();

    //Initialising Report ListView
    private ListView reportListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resolved_reports);

        getSupportActionBar().setTitle("Resolved Reports");

        //Links the List View Widget to Variable
        reportListView = (ListView) findViewById(R.id.reportListView);

        //Calls function to check reports
        checkReports();
    }

    //Function checks if reports are resolved
    private void checkReports(){
        //Loops through each report
        for(int i = 0; i < MostRecent.reportsArray.size(); i++){
            //Gets the current report
            Report report = MostRecent.reportsArray.get(i);
            //Checks if the report has been resolved
            if(report.getResolved()){
                //Adds the report to the resolved reports array
                resolvedReports.add(report);
            }
        }
        //If there are no resolved reports, message is shown to user
        if(resolvedReports.size() == 0){
            Toast.makeText(this, "No Reports Resolved Yet", Toast.LENGTH_SHORT).show();
        }else{
            //If there are resolved reports, populate the list view
            populateList();
        }
    }

    private void populateList(){
        //Call task adapter function with resolved reports array
        TaskAdapter adapter = new TaskAdapter(this, resolvedReports);
        //apply adapter to the report list view
        reportListView.setAdapter(adapter);
    }
}
