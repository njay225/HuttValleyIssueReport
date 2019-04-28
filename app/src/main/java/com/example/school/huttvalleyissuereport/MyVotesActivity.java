package com.example.school.huttvalleyissuereport;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyVotesActivity extends AppCompatActivity {

    //Variable for Logging to Console
    private static final String TAG = "MyVotes";

    //Array list to hold user's reports
    private ArrayList<String> userVotesIds = new ArrayList<>();
    private ArrayList<Report> userVotes = new ArrayList<>();

    //Initialising Report ListView
    private ListView reportListView;

    //Initialing Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    //Initialising Firebase Databse
    private FirebaseDatabase mDatabase;
    private DatabaseReference userVotesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_votes);

        //Sets the title at the top of the activity to "My Votes"
        getSupportActionBar().setTitle("My Votes");

        //The reportListView is linked to the widget
        reportListView = (ListView) findViewById(R.id.reportListView);

        //Firebase Authentication is set up and current user is retrieved
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }

    private void checkUserVotes(){
        //A single event listener is added to the uservotes ref to find what reports the user
        //has voted for
        userVotesRef.child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: ");
                //All the reports the user has votes for are looped through
                for(DataSnapshot postSnapShot : dataSnapshot.getChildren()){
                    //the vote id is added to the userVotesIds array
                    userVotesIds.add(String.valueOf(postSnapShot.getValue()));
                }

                //All reports are then looped through
                for(Report report : MostRecent.reportsArray){
                    //If the userVotesIDs contains the report Id then it is added to the user
                    //votes array
                    if(userVotesIds.contains(report.getReportID())){
                        userVotes.add(report);
                    }
                }
                //the reports are then added to the list view
                populateList();


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void populateList(){
        TaskAdapter adapter = new TaskAdapter(this, userVotes);
        reportListView.setAdapter(adapter);
    }

    @Override
    public void onPause(){
        super.onPause();
        //Clears the userVotes and userVotesIds arrays when the user leaves activity
        userVotes.clear();
        userVotesIds.clear();
    }

    @Override
    public void onResume(){
        super.onResume();
        //Firebase Database is set up
        mDatabase = FirebaseDatabase.getInstance();
        userVotesRef = mDatabase.getReference(getResources().getString(R.string.database_user_votes_ref));
        //Function is run to check the user votes
        checkUserVotes();
    }




}
