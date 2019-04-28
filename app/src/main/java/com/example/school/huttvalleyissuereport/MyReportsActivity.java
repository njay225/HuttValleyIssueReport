package com.example.school.huttvalleyissuereport;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Objects;

public class MyReportsActivity extends AppCompatActivity {

    //Variable for Logging to Console
    private static final String TAG = "MyReports";

    //Array list to hold user's reports
    private ArrayList<Report> userReports = new ArrayList<>();

    //Initialising Report ListView
    private ListView reportListView;

    //Initialing Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        //Sets the title at the top of the screen
        getSupportActionBar().setTitle("My Reports");

        //Links the listview variable to the widget
        reportListView = (ListView) findViewById(R.id.reportListView);

        //Gets the current user
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        //Calls the function to check which reports belong to the user
        checkReports();
    }

    //Loops through all reports and checks if the user created them
    private void checkReports(){
        //For loop which goes through each report
        for(int i = 0; i < MostRecent.reportsArray.size(); i++){
            //reportIsUsers Boolean is set to true
            Boolean reportIsUsers = true;
            //The report at the index i is retrieved from the public reports array
            //From the Most Recent Class
            Report report = MostRecent.reportsArray.get(i);

            //Checks if the reportCreator for the report is not equal to the
            //current users ID
            if(!Objects.equals(report.getCreator(), mUser.getUid())){
                //If they are not equal, then the report is not the users and
                //reportIsUsers is set to false
                reportIsUsers = false;
            }

            //If the report was made my the user, then it is added to the userReports Array
            if(reportIsUsers){
                userReports.add(report);
            }
        }
        //If the user hasn't made any reports, then a message is displayed to the user
        if(userReports.size() == 0){
            Toast.makeText(this, "No Reports Made", Toast.LENGTH_SHORT).show();
        }else{
            //If there are reports the user has made, then the listview is populated
            populateList();
        }
    }

    private void populateList(){
        TaskAdapter adapter = new TaskAdapter(this, userReports);
        reportListView.setAdapter(adapter);
    }
}
