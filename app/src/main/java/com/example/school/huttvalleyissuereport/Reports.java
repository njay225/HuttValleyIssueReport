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
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;


public class Reports extends Fragment {

    //Initialises Report List View
    private ListView reportListView;

    private Activity mActivity;

    public Reports() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_reports, container, false);

        //Gets ListView and links it to the variable
        reportListView = (ListView) rootView.findViewById(R.id.reportListView);

        //Adds the reports retrieved from the UserViewActivity
        TaskAdapter adapter = new TaskAdapter(mActivity.getBaseContext(), UserViewActivity.currentUserReports);
        reportListView.setAdapter(adapter);


        return rootView;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }



}
