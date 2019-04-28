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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class MostPopular extends Fragment {

    //Variable for logging to the console
    private static final String TAG = "MostPopular";

    //Initialising Widgets
    private ListView reportListView;

    //Initialising Firebase Database
    private FirebaseDatabase mDatabase;
    private DatabaseReference reportsRef;

    //Initialising ArrayList to Hold Reports
    private ArrayList<Report> mostPopularReportsArray =  new ArrayList<Report>();

    //Initialising Progress Spinner
    private ProgressBar progressBar;
    private boolean listViewSet = false;

    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private void getReports(){
        //The 100 most popular reports are retrieved
        Log.d(TAG, "getReports: Most Popular Reports");
        reportsRef.orderByChild("votes").limitToLast(getResources().getInteger(R.integer.most_popular_length)).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                //The datasnapshot is converted to a report object
                Report report = dataSnapshot.getValue(Report.class);
                //the report is then added to the first index of the mostPopularReportsArray
                //it is added to the first because, the least popular of the 100 are retrieved first
                //by the event listener

                //If the report has not been resolved then it is added to the arraylist
                if(!report.getResolved()){
                    mostPopularReportsArray.add(0, report);
                }

                //The List View is populated
                populateList();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildChanged: ");
                Log.d(TAG, "onChildChanged: " + s);
                //The datasnapshot is converted to a report object
                Report report = dataSnapshot.getValue(Report.class);

                //All reports are looped through to find which one has been changed
                for(int i = 0; i<mostPopularReportsArray.size(); i++){
                    //If the report at index i has been changed
                    if(Objects.equals(report.getReportID(), mostPopularReportsArray.get(i).getReportID())){
                        //Then the report is updated
                        mostPopularReportsArray.set(i, report);

                        //The mostPopularReportArray is then reordered to make sure the reports are shown in the
                        //correct order
                        Collections.sort(mostPopularReportsArray, new Comparator<Report>() {
                            @Override
                            public int compare(Report o1, Report o2) {
                                return Integer.valueOf(o2.getVotes()).compareTo(o1.getVotes());
                            }
                        });

                        //Hides the report if it has been resolved
                        if(report.getResolved()){
                            mostPopularReportsArray.remove(i);
                        }

                    }
                }
                //The list view is then populated
                populateList();

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //The datasnapshot is converted to a report object
                Report report = dataSnapshot.getValue(Report.class);

                //All reports are looped through to find which one has been deleted
                for(int i = 0; i<mostPopularReportsArray.size(); i++){
                    //If the report at index i has been deleted
                    if(Objects.equals(report.getReportID(), mostPopularReportsArray.get(i).getReportID())){
                        //Then the report is updated
                        mostPopularReportsArray.remove(i);
                    }
                }
                //The list view is then populated
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
        Log.d(TAG, "populateList: Task Adapter");
        TaskAdapter adapter = new TaskAdapter(mActivity.getBaseContext(), mostPopularReportsArray);
        reportListView.setAdapter(adapter);
        listViewSet = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_most_popular, container, false);

        //The list view variable is linked to the widget
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

        //Firebase Database is set up and a reports reference is retrieved
        mDatabase = FirebaseDatabase.getInstance();
        reportsRef = mDatabase.getReference(getResources().getString(R.string.database_reports_ref));

        reportsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
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
