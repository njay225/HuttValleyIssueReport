package com.example.school.huttvalleyissuereport;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Objects;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //Variable for logging into the console
    private static final String TAG = "HomeActivity" ;

    //Initialising Tab Layout Widgets
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    //Initialising Text Views in Navigation Menu
    private TextView userNameTextView, emailPhoneTextView;

    //Initialising Search Popup Widgets
    private EditText keywordEditText, locationEditText, dateFromEditText, dateToEditText;
    private Spinner categorySpinner, urgencySpinner;

    //Initialising Search Button
    private Button searchButton;

    //Arrays to hold values for search spinners
    public static ArrayList<String> categoryArrayList = new ArrayList<String>();
    public static ArrayList<String> urgencyArrayList = new ArrayList<String>();

    //Initialising Calender for Date Picker
    private Calendar dateFromCalendar = Calendar.getInstance();
    private Calendar dateToCalendar = Calendar.getInstance();

    //Initialising Search Object
    public static Search search;

    //Initialising Firebase Database
    private FirebaseDatabase mDatabase;
    private DatabaseReference categoryRef;
    private DatabaseReference urgencyRef;

    //Initialising Firebase Authentication
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    //Variables for checking network status
    private ConnectivityManager cm;
    private NetworkInfo activeNetwork;
    public static boolean isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Hides Title from App Bar
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //Initialises the create report button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.createReportButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, CreateReportActivity.class);
                intent.putExtra("savedReport", "false");
                startActivity(intent);
            }
        });

        //Initialises the navigation drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        //Checks if device is connected to a network
        cm = (ConnectivityManager) getBaseContext().getSystemService(CONNECTIVITY_SERVICE);
        activeNetwork = cm.getActiveNetworkInfo();

        isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        //Setting up Firebase Authentication
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        //Returns the top section of the navigation menu
        View header = navigationView.getHeaderView(0);

        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, UserViewActivity.class);
                intent.putExtra("userID", user.getUid());
                startActivity(intent);
            }
        });

        //Setting up TabLayout to show Most Popular and Most Recent Fragment
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        //Set up the ViewPager with the sections adapter
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(mViewPager);

        //Linking Nav Text Views to Widgets
        userNameTextView = (TextView) header.findViewById(R.id.userNameTextView);
        emailPhoneTextView = (TextView) header.findViewById(R.id.userEmailPhoneTextView);

        //Underlines the text within the textviews
        userNameTextView.setPaintFlags(userNameTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        emailPhoneTextView.setPaintFlags(emailPhoneTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        //Setting Text in Navigation Menu
        userNameTextView.setText(user.getDisplayName());

        //Checking if user is signed in with phone
        if(Objects.equals(user.getProviders().get(0), "phone")){
            //If they are, set the navigation header text to their phone number
            emailPhoneTextView.setText(user.getPhoneNumber());
        }else{
            //Otherwise set the navigation header text to their email
            emailPhoneTextView.setText(user.getEmail());
        }

        //Sets up Firebase Database
        mDatabase = FirebaseDatabase.getInstance();

        //Gets references for categories and urgency sections of Database
        categoryRef = mDatabase.getReference(getResources().getString(R.string.database_category_ref));
        urgencyRef = mDatabase.getReference(getResources().getString(R.string.database_urgency_ref));

        if(!isConnected){
            try {
                FileInputStream fileInputStream = openFileInput(getResources().getString(R.string.urgency_array_list_location));
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                urgencyArrayList = (ArrayList<String>) objectInputStream.readObject();
                objectInputStream.close();

                fileInputStream = openFileInput(getResources().getString(R.string.category_array_list_location));
                objectInputStream = new ObjectInputStream(fileInputStream);
                categoryArrayList = (ArrayList<String>) objectInputStream.readObject();
                objectInputStream.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                Log.d(TAG, "onCreate: Retrieving Objects Failed");
            }
        }

        //Initialises the search button
        searchButton = (Button) findViewById(R.id.searchButton);

        //Adds an onclick listener for the search button
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Initialises a dialog builder
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

                //Sets the view of the dialog to the search popup layout
                View view = getLayoutInflater().inflate(R.layout.search_popup, null);
                builder.setView(view)
                        //Sets up the "Close" Button
                        .setNegativeButton("Close", null);

                //Sets up the Edit Texts for Search popup
                keywordEditText = (EditText) view.findViewById(R.id.keywordEditText);
                locationEditText = (EditText) view.findViewById(R.id.locationEditText);

                //Sets up the Date From Edit Text
                dateFromEditText = (EditText) view.findViewById(R.id.dateFromEditText);
                //Adds on an onclicklistener to date from edit text
                dateFromEditText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Initialises a date picker to select date from
                        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener(){

                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                //Gets the selected year, month and day
                                dateFromCalendar.set(Calendar.YEAR, year);
                                dateFromCalendar.set(Calendar.MONTH, month);
                                dateFromCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                                //Checks if the date selected is after the dateTo date
                                if(dateFromCalendar.getTimeInMillis() > dateToCalendar.getTimeInMillis()){
                                    //If it is then a message is sent to the user
                                    Toast.makeText(HomeActivity.this, "Please Adjust Date Boundary", Toast.LENGTH_SHORT).show();
                                }else{
                                    //If the dateFrom is valid then add the text to the EditText on the search popup
                                    String dateFormat = "dd/MM/yy";
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
                                    dateFromEditText.setText(simpleDateFormat.format(dateFromCalendar.getTime()));
                                }
                            }
                        };
                        //Opens the date pickers Initialised above
                        new DatePickerDialog(HomeActivity.this, date, dateFromCalendar.get(Calendar.YEAR),
                                dateFromCalendar.get(Calendar.MONTH), dateFromCalendar.get(Calendar.DAY_OF_MONTH)).show();
                    }
                });

                //Initialises DateTo Edit Text
                dateToEditText = (EditText) view.findViewById(R.id.dateToEditText);

                //Adds onClickListener for DateTo Edit Text
                dateToEditText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Initialises Date Picker to select DateTo
                        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener(){

                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                //Gets the selected year, month and day
                                dateToCalendar.set(Calendar.YEAR, year);
                                dateToCalendar.set(Calendar.MONTH, month);
                                dateToCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                                //Checks if the date selected is before the DateFrom
                                if(dateFromCalendar.getTimeInMillis() > dateToCalendar.getTimeInMillis()){
                                    //If it is then message is sent to user
                                    Toast.makeText(HomeActivity.this, "Please Adjust Date Boundary", Toast.LENGTH_SHORT).show();
                                }else{
                                    //If the dateTo is valid then add the text to the EditText on the search popup
                                    String dateFormat = "dd/MM/yy";
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
                                    dateToEditText.setText(simpleDateFormat.format(dateToCalendar.getTime()));
                                }
                            }
                        };

                        //Opens the Date picker initialised
                        new DatePickerDialog(HomeActivity.this, date, dateToCalendar.get(Calendar.YEAR),
                                dateToCalendar.get(Calendar.MONTH), dateToCalendar.get(Calendar.DAY_OF_MONTH)).show();
                    }
                });

                //Initialises the spinners for the category and urgency
                categorySpinner = (Spinner) view.findViewById(R.id.categorySpinner);
                urgencySpinner = (Spinner) view.findViewById(R.id.urgencySpinner);

                Log.d(TAG, "onClick: Populating Spinners");
                //Runs Populate Spinner background task
                populateSpinners populateSpinners = new populateSpinners();
                populateSpinners.execute();

                //Initialises "Search" Button
                builder.setPositiveButton("Search", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Gets Text Values for Edit Texts
                        String keyword = keywordEditText.getText().toString();
                        String category = categorySpinner.getSelectedItem().toString();
                        String urgency = urgencySpinner.getSelectedItem().toString();
                        String location = locationEditText.getText().toString();

                        //Gets the dates from the DateTo and DateFrom Edit Texts
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yy");

                        String dateFrom = dateFromEditText.getText().toString();
                        String dateTo = dateToEditText.getText().toString();

                        //Initialises the millisecond dateFrom and DateTo
                        Long dateFromMilli = null, dateToMilli = null;
                        try {
                            //Convert the date strings to milliseconds
                            dateFromMilli = simpleDateFormat.parse(dateFrom).getTime();
                            dateToMilli = simpleDateFormat.parse(dateTo).getTime();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        //If either date is null
                        if(dateFromMilli == null){
                            //Set the date from milliseconds to 0
                            dateFromMilli = 0L;
                        }

                        if(dateToMilli == null){
                            //Sets the date to milliseconds to the current time
                            dateToMilli = dateToCalendar.getTimeInMillis();
                        }


                        //If all criteria are empty, the user is prompted to enter a search
                        if(Objects.equals(keyword, "") && Objects.equals(category, getResources().getString(R.string.default_category)) && Objects.equals(urgency, getResources().getString(R.string.default_urgency))
                                && Objects.equals(location, "") && Objects.equals(dateFrom, "") && Objects.equals(dateTo, "")){
                            Toast.makeText(HomeActivity.this, "Please Enter Some Search Criteria", Toast.LENGTH_LONG).show();
                        }else{
                            //If search has been made, let the user know
                            Toast.makeText(HomeActivity.this, "Search Made", Toast.LENGTH_SHORT).show();
                            //Create a search object which is public
                            search = new Search(keyword, category, urgency, location, dateFromMilli, dateToMilli);
                            //Open Search result activity
                            Intent intent = new Intent(HomeActivity.this, SearchResult.class);
                            startActivity(intent);
                        }
                    }
                }).show();

            }
        });

        Log.d(TAG, "onCreate: Getting Urgency and Category");
        //Get category options from Firebase if they don't exist
        if(categoryArrayList.isEmpty()){
            Log.d(TAG, "onCreate: Getting Categorys");
            getCategoryValues();
        }else{
            Log.d(TAG, "onCreate: " + categoryArrayList.toString());
        }

        //Get Urgency options from Firebase if they don't exist
        if(urgencyArrayList.isEmpty()){
            Log.d(TAG, "onCreate: Getting Urgencys");
            getUrgencyValues();
        }else{
            Log.d(TAG, "onCreate: " + urgencyArrayList.toString());
        }


    }

    private void getCategoryValues(){
        //Add a Firebase Listener to the category reference
        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Loop through each child of the categories
                for(DataSnapshot postSnapShot : dataSnapshot.getChildren()){
                    Log.d(TAG, "onDataChange: " + postSnapShot.getValue());
                    //Add the category to the categoryArrayList
                    categoryArrayList.add(String.valueOf(postSnapShot.getValue()));
                }

                //Write the Arraylist to the device for offline access
                try {
                    FileOutputStream fileOutputStream = openFileOutput(getResources().getString(R.string.category_array_list_location), MODE_PRIVATE);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(categoryArrayList);
                    objectOutputStream.flush();
                    objectOutputStream.close();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void getUrgencyValues(){
        //Add a single event listener to the urgency reference
        urgencyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Loops through all of the children in the urgency node
                for(DataSnapshot postSnapShot : dataSnapshot.getChildren()){
                    Log.d(TAG, "onDataChange: " + postSnapShot.getValue());
                    //Add the urgency value to the urgency array list
                    urgencyArrayList.add(String.valueOf(postSnapShot.getValue()));
                }

                //Write the Arraylist to the device for offline access
                try {
                    FileOutputStream fileOutputStream = openFileOutput(getResources().getString(R.string.urgency_array_list_location), MODE_PRIVATE);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(urgencyArrayList);
                    objectOutputStream.flush();
                    objectOutputStream.close();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Intent intent;
        //Checks the ID of the navigation item clicked
        switch (id){
            case R.id.nav_home:
                break;
            case R.id.nav_my_reports:
                intent = new Intent(HomeActivity.this, MyReportsActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_my_votes:
                intent = new Intent(HomeActivity.this, MyVotesActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_resolved_issues:
                intent = new Intent(HomeActivity.this, ResolvedReportsActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_saved_reports:
                intent = new Intent(HomeActivity.this, SavedReportActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_contact:
                break;
            case R.id.nav_feedback:
                break;
            case R.id.nav_settings:
                break;
            case R.id.nav_logout:
                //Prompts the user if they want to logout
                new AlertDialog.Builder(HomeActivity.this)
                    .setTitle("Are you sure?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Log the User Out
                            mAuth.signOut();
                            //Clear saved files from device
                            File cache = getCacheDir();
                            File appDir = new File(cache.getParent());

                            //Checks if the app contains a file directory
                            if(appDir.exists()){
                                //Gets all surface level directories
                                String[] children = appDir.list();
                                //For all surface level directories not named
                                //"lib" are looped through to check if they contain files or directories
                                for(String string : children){
                                    if(!string.equals("lib")){
                                        deleteDir(new File(appDir, string));

                                    }
                                }
                            }
                            //Open the Login Page
                            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                            startActivity(intent);
                            //Close the Home Activity
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private static boolean deleteDir(File dir){
        //Checks if the file directory exists and is a file directory, if its a file
        //then the file is deleted
        if(dir != null && dir.isDirectory()){
            //Gets all of files under the directory
            String[] children = dir.list();
            //For every child in the directory
            for(int i = 0; i < children.length; i++){
                //It is checked to see if the file is directory by recalling this function
                boolean success = deleteDir(new File(dir, children[i]));

                //If it was a directory, then false is returned
                if(!success){
                    return false;
                }
            }
        }

        //If it was a file then it is deleted
        return dir.delete();
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
                    return new MostPopular();
                case 1:
                    return new MostRecent();
                default:
                    return null;
            }


        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return getResources().getInteger(R.integer.home_fragments_total);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Most Popular";
                case 1:
                    return "Most Recent";
            }
            return null;
        }
    }

    @Override
    public void onRestart(){
        super.onRestart();
    }

    @Override
    public void onResume(){
        super.onResume();
        userNameTextView.setText(user.getDisplayName());
        //Checking if user is signed in with phone
        if(Objects.equals(user.getProviders().get(0), "phone")){
            //If they are, set the navigation header text to their phone number
            emailPhoneTextView.setText(user.getPhoneNumber());
        }else{
            //Otherwise set the navigation header text to their email
            emailPhoneTextView.setText(user.getEmail());
        }

    }

    private class populateSpinners extends AsyncTask<Void, Void, Void> {

        ArrayAdapter<String> urgencySpinnerArrayAdapter, categorySpinnerArrayAdapter;

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "doInBackground: Populating Spinners");
            do{
                categorySpinnerArrayAdapter = new ArrayAdapter<String>(HomeActivity.this, android.R.layout.simple_spinner_item, HomeActivity.categoryArrayList);
                categorySpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            }while(HomeActivity.categoryArrayList.size() == 0);

            do{
                urgencySpinnerArrayAdapter = new ArrayAdapter<String>(HomeActivity.this, android.R.layout.simple_spinner_item, HomeActivity.urgencyArrayList);
                urgencySpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            }while(HomeActivity.urgencyArrayList.size() == 0);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            categorySpinner.setAdapter(categorySpinnerArrayAdapter);
            urgencySpinner.setAdapter(urgencySpinnerArrayAdapter);
        }
    }


}
