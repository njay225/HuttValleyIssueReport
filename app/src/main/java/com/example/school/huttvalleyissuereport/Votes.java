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

import static android.content.ContentValues.TAG;

public class Votes extends Fragment {
    //Initialises the reportListView
    private ListView reportListView;

    private Activity mActivity;
    public Votes() {
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
        View rootView = inflater.inflate(R.layout.fragment_votes, container, false);

        //Gets the List View and links it to the variable
        reportListView = (ListView) rootView.findViewById(R.id.reportListView);

        //Executes Background Task to add votes to list view
        addUserVotes addUserVotes = new addUserVotes();
        addUserVotes.execute();

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

    private class addUserVotes extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            //Checks if the voted reports have been retrieved from the UserViewActivity
            while(UserViewActivity.currentUserVotes == null){
                Log.d(TAG, "doInBackground: user reports are not found");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid){
            super.onPostExecute(aVoid);
            //Once the votes have been retrieved, then they are added to the reportListView
            TaskAdapter adapter = new TaskAdapter(mActivity.getBaseContext(), UserViewActivity.currentUserVotes);
            reportListView.setAdapter(adapter);
        }
    }

}
