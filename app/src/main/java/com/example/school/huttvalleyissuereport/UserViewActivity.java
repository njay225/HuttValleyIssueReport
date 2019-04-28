package com.example.school.huttvalleyissuereport;

import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

public class UserViewActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    //Public variable to hold userID
    public static String currentUserID;

    //Public Arraylists to hold user reports and votes
    public static ArrayList<Report> currentUserReports = new ArrayList<>();
    public static ArrayList<Report> currentUserVotes = new ArrayList<>();

    //Counter to count user votes and reports
    public static int reportCounter, voteCounter;

    //Boolean Variables to check if firebase function have run
    public static Boolean votesRetrieved  = false;

    //Initialising Firebase Database
    private FirebaseDatabase mDatabase;
    private DatabaseReference voteRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("User Details");
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        //Gets the User ID passed onto the activity
        currentUserID = getIntent().getExtras().getString("userID");

        //Sets up Firebase Database and gets userVotes Reference
        mDatabase = FirebaseDatabase.getInstance();
        voteRef = mDatabase.getReference(getResources().getString(R.string.database_user_votes_ref));

        //Gets the User's Reports
        getUserReports();
        //Gets the User's Votes
        getUserVotes();

    }


    //Function retrieves the users votes
    private void getUserVotes(){
        //Adds a single event listener to voteRef under currents users id
        voteRef.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Initialises an array list to hold voted report ids
                ArrayList<String> votedReportIds = new ArrayList<>();
                //Loops through all of the children under currentUserID
                for(DataSnapshot postSnapShot : dataSnapshot.getChildren()){
                    //Each child is a vote, so one is added to the vote counter
                    voteCounter++;
                    //the vote id is added to the userVotesIds array
                    votedReportIds.add(String.valueOf(postSnapShot.getValue()));
                }

                //All Saved reports are then looped through
                for(Report report : MostRecent.reportsArray){
                    //If the userVotesIDs contains the report Id then it is added to the user
                    //votes array
                    if(votedReportIds.contains(report.getReportID())){
                        currentUserVotes.add(report);
                    }
                }
                //Sets a Boolean votesRetrieved to true to indicate that votes have been retrieved
                votesRetrieved = true;
                }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //This function gets all reports which the user has made
    private void getUserReports(){
        //Loops through all reports on the database
        for(int i = 0; i < MostRecent.reportsArray.size(); i++){
            //Saves the report at index i to report object
            Report report = MostRecent.reportsArray.get(i);
            //If the report creator id is equal to the current users
            if(Objects.equals(report.getCreator(), currentUserID)){
                //The report counter is increased by 1
                reportCounter++;
                //The report is added to array which holds users reports
                currentUserReports.add(report);
            }
        }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        //All Counters and ArrayLists are cleared
        voteCounter = 0;
        reportCounter = 0;
        currentUserVotes.clear();
        currentUserReports.clear();
        this.finish();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).

            switch (position) {
                case 0:
                    return new UserInfo();
                case 1:
                    return new Reports();
                case 2:
                    return new Votes();
                default:
                    return null;
            }


        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return getResources().getInteger(R.integer.user_view_fragments_total);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "User Info";
                case 1:
                    return "Reports";
                case 2:
                    return "Votes";
            }
            return null;
        }
    }

}
