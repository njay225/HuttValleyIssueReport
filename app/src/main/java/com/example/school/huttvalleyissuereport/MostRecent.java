package com.example.school.huttvalleyissuereport;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

public class MostRecent extends Fragment {

    //Variable for logging to the console
    private static final String TAG = "MostPopular";

    //Initialising Widgets
    private ListView reportListView;

    //Initialising Firebase Database
    private FirebaseDatabase mDatabase;
    private DatabaseReference reportsRef;

    //Initialising ArrayList to Hold Reports
    public static ArrayList<Report> reportsArray =  new ArrayList<>();
    private ArrayList<Report> mostRecentReportsArray = new ArrayList<>();

    //Initialising Progress Spinner
    private ProgressBar progressBar;
    private boolean listViewSet = false;

    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void getReports(){
        //All reports are found and are ordered by the date added
        reportsRef.orderByChild("dateAdded").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                //The datasnapshot is converted to a report object
                Report report = dataSnapshot.getValue(Report.class);
                //the report is then added to the first index of the mostRecentReportArray
                //it is added to the first because, the earliest reports are retrieved first
                //by the event listener
                mostRecentReportsArray.add(0, report);
                //the public report array is then set to equal the mostRecentReportsArray
                reportsArray = mostRecentReportsArray;
                Log.d(TAG, "onDataChange: " + mostRecentReportsArray.size());
                //the list view is then populated
                populateList();
            }

            //When a change occurs to a report
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                //The datasnapshot is converted to a report object
                Report report = dataSnapshot.getValue(Report.class);
                Log.d(TAG, "onChildChanged: " + report.getVotes());

                //All reports are looped through so that the report can be changed in the arrays
                for(int i = 0; i< mostRecentReportsArray.size(); i++){
                    Log.d(TAG, "onChildChanged: " + report.getReportID());
                    Log.d(TAG, "onChildChanged: " + report.getReportID() + " = " + mostRecentReportsArray.get(i).getReportID());
                    //If the report at index i in the mostRecentReportsArray is the one that has been changed
                    if(Objects.equals(report.getReportID(), mostRecentReportsArray.get(i).getReportID())){
                        Log.d(TAG, "onChildChanged: Arrays are being changed");
                        //Then it is replaced by the changed report
                        mostRecentReportsArray.set(i, report);
                        //the public report array is then set to equal the mostRecentReportsArray
                        reportsArray = mostRecentReportsArray;
                    }
                }
                //the list view is then populated
                populateList();

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //The datasnapshot is converted to a report object
                Report report = dataSnapshot.getValue(Report.class);
                Log.d(TAG, "onChildChanged: " + report.getVotes());

                //All reports are looped through so that the report can be deleted from the arrays
                for(int i = 0; i< mostRecentReportsArray.size(); i++){
                    Log.d(TAG, "onChildChanged: " + report.getReportID());
                    Log.d(TAG, "onChildChanged: " + report.getReportID() + " = " + mostRecentReportsArray.get(i).getReportID());
                    //If the report at index i in the mostRecentReportsArray is the one that has been changed
                    if(Objects.equals(report.getReportID(), mostRecentReportsArray.get(i).getReportID())){
                        Log.d(TAG, "onChildChanged: Arrays are being changed");
                        mostRecentReportsArray.remove(i);
                        //the public report array is then set to equal the mostRecentReportsArray
                        reportsArray = mostRecentReportsArray;
                    }
                }
                //the list view is then populated
                populateList();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void populateList(){
        TaskAdapter adapter = new TaskAdapter(mActivity.getBaseContext(), mostRecentReportsArray);
        reportListView.setAdapter(adapter);
        listViewSet = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_most_recent, container, false);

        //When view is created, the reportListView variable is linked to the widget
        reportListView = (ListView) rootView.findViewById(R.id.reportListView);

        //Linking Spinner to Widget
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        return rootView;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;

        //Firebase Database is set up and a reference is retrieved
        mDatabase = FirebaseDatabase.getInstance();
        reportsRef = mDatabase.getReference(getResources().getString(R.string.database_reports_ref));
        reportsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: " + dataSnapshot.toString());
                if(dataSnapshot.getValue() != null){
                    //Checks when reports are finished loading
                    checkReports checkReports = new checkReports();
                    checkReports.execute();
                }else{
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //This function gets the reports
        getReports();

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    private class checkReports extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            reportListView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            while(!listViewSet){
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            reportListView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

}
