package com.example.school.huttvalleyissuereport;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class ReportViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    //Variable for logging to console
    private static final String TAG = "ReportViewActivity";

    //Initialising Map Fragment
    private GoogleMap gMaps;

    //Constant for Display Width
    private static double DISPLAY_WIDTH;

    //Initialising Report Variable
    private Report report;

    //Initialising Text Views
    private TextView titleTextView, urgencyTextView, categoryTextView, creatorTextView, descriptionTextView,
                    votesTextView, addressTextView;

    //Initialising Horizontal Scroll View for files
    private HorizontalScrollView fileScrollView;
    private LinearLayout fileLinearLayout;

    //Initialising Button
    private Button voteButton;

    //Initialising Firebase Database
    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef, userVotesRef, reportRef;

    //Initialing Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    //Initialising Firebase Storage
    private StorageReference storageRef;
    private FirebaseStorage mStorage;

    //Boolean to check if user has voted
    private Boolean userHasVoted = false;

    //Holds User Vote Key
    private String userVoteKey;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_view);

        getSupportActionBar().hide();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        report = TaskAdapter.clickedReport;

        if(report == null){
            Intent intent = new Intent(ReportViewActivity.this, HomeActivity.class);
            startActivity(intent);
        }

        //Linking Text Views
        titleTextView = (TextView) findViewById(R.id.titleTextView);
        urgencyTextView = (TextView) findViewById(R.id.urgencyTextView);
        categoryTextView = (TextView) findViewById(R.id.categoryTextView);
        creatorTextView = (TextView) findViewById(R.id.creatorTextView);
        descriptionTextView = (TextView) findViewById(R.id.descriptionTextView);
        votesTextView = (TextView) findViewById(R.id.voteTextView);
        addressTextView = (TextView) findViewById(R.id.addressTextView);

        //Adding onclick listener to report creator user name
        creatorTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ReportViewActivity.this, UserViewActivity.class);
                intent.putExtra("userID", report.getCreator());
                startActivity(intent);
            }
        });

        //Linking Scroll View
        fileScrollView = (HorizontalScrollView) findViewById(R.id.fileScrollView);
        fileLinearLayout = (LinearLayout) findViewById(R.id.fileLinearLayout);

        //Linking Button
        voteButton = (Button) findViewById(R.id.voteButton);

        //Setting the textview text
        titleTextView.setText(report.getTitle());
        urgencyTextView.setText(report.getUrgency());
        categoryTextView.setText(report.getCategory());
        descriptionTextView.setText(report.getDescription());
        addressTextView.setText(report.getAddress());

        //Linking to Firebase Database and getting references
        mDatabase = FirebaseDatabase.getInstance();
        userRef = mDatabase.getReference(getResources().getString(R.string.database_users_ref));
        userVotesRef = mDatabase.getReference(getResources().getString(R.string.database_user_votes_ref));
        reportRef = mDatabase.getReference(getResources().getString(R.string.database_reports_ref));

        //Linking to Firebase Authentication and getting the current user
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        //Getting the current display dimensions
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);

        //Setting the display width variable
        DISPLAY_WIDTH = point.x;

        //Checks if the report has any files
        if(Objects.equals(report.getFileOne(), "null") && Objects.equals(report.getFileTwo(), "null") && Objects.equals(report.getFileThree(), "null")){
            //If there are no files then the filescrollview is hidden
            fileScrollView.setVisibility(View.GONE);
            //Top padding is added to the titleTextView
            titleTextView.setPadding(0,45,0,0);
        }else{
            //If the report has files
            //Linking to Firebase Storage and getting a reference
            mStorage = FirebaseStorage.getInstance();
            storageRef = mStorage.getReference(getResources().getString(R.string.storage_files_ref));
            getFiles();
        }

        if(report.getResolved()){
            voteButton.setVisibility(View.GONE);
        }

        //Gets the number of votes the report has (live counter)
        setVoteText();

        //Gets the creator user name using reportCreator
        getCreatorName();

        //Checks if the current user has voted for the report
        checkIfVoted();

        //Adds an onclick listener to vote button
        voteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //After the user has clicked the button, it is temporarily disabled to prevent double clicks
                voteButton.setClickable(false);
                //If the user has votes before
                if(userHasVoted){
                            //The report is removed from the user votes section of the Firebase Database
                            userVotesRef.child(mUser.getUid()).child(userVoteKey).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    //If that was done successfully
                                    if(task.isSuccessful()){
                                        //Change the style of the vote button
                                        voteButton.setBackgroundResource(R.drawable.primary_border);
                                        //Change the text of the vote button
                                        voteButton.setText("Vote");
                                        //Sets the colour of the vote text to white
                                        voteButton.setTextColor(getResources().getColor(R.color.colorPrimary));
                                        //Run a transaction to the votes of the report in the Database
                                        //A transaction is run because the number of votes is something
                                        //Which will change often and therefore, to get the accurate number
                                        //A transaction is run which takes into account simultaneous changes
                                        //to the database and then applies the change so that the final vote
                                        //number is correct
                                        reportRef.child(report.getReportID()).runTransaction(new Transaction.Handler() {
                                            @Override
                                            public Transaction.Result doTransaction(MutableData mutableData) {
                                                //Gets the report which needs to be changed
                                                Report report = mutableData.getValue(Report.class);

                                                //The number of votes is reduced by 1
                                                report.votes--;

                                                //The report is then updated in the database
                                                mutableData.setValue(report);
                                                return Transaction.success(mutableData);
                                            }

                                            @Override
                                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                                //When the vote is successfully registered, the Boolean userHasVoted is updated
                                                userHasVoted = false;
                                                //The vote button is reactivated
                                                voteButton.setClickable(true);
                                            }
                                        });
                                    }
                                }
                            });
                }else{
                    //If the user hasn't voted
                    //The report ID is added to the userVotes section of the database under the current users ID
                    userVoteKey = userVotesRef.child(mUser.getUid()).push().getKey();
                    userVotesRef.child(mUser.getUid()).child(userVoteKey).setValue(report.getReportID()).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            //If the report ID was added successfully
                            if(task.isSuccessful()){
                                //Vote button style is updated
                                voteButton.setBackgroundResource(R.drawable.secondary_button_border);
                                //Vote button text is updated
                                voteButton.setText("Voted");
                                //Sets the colour of the vote text to white
                                voteButton.setTextColor(getResources().getColor(R.color.white));
                                //Run a transaction to the votes of the report in the Database
                                //A transaction is run because the number of votes is something
                                //Which will change often and therefore, to get the accurate number
                                //A transaction is run which takes into account simultaneous changes
                                //to the database and then applies the change so that the final vote
                                //number is correct
                                reportRef.child(report.getReportID()).runTransaction(new Transaction.Handler() {
                                    @Override
                                    public Transaction.Result doTransaction(MutableData mutableData) {
                                        //The report which needs to be changed is retrieved
                                        Report report = mutableData.getValue(Report.class);

                                        //The number of votes for the report is increased by 1
                                        report.votes++;

                                        //The report is then updated in the database
                                        mutableData.setValue(report);
                                        return Transaction.success(mutableData);
                                    }

                                    @Override
                                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                        //When the vote has been registered, the vote button is reactiivated
                                        voteButton.setClickable(true);
                                        //The boolean userHasVoted is set to true
                                        userHasVoted = true;

                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

    }

    //This function gets the files associated with the report is any exist
    private void getFiles() {
        //Initialises an array of file names
        final ArrayList<String> files = new ArrayList<>();

        //Loops through each of the file names in the report class
        for(int i = 0; i < getResources().getInteger(R.integer.file_upload_limit); i++){
            //Checks the number the loop is on
            switch (i){
                case 0:
                    //If the count is 0 and the first file in the report class is not empty
                    if(!Objects.equals(report.getFileOne(), "null")){
                        //the file name is added to the files array list
                        files.add(report.getFileOne());
                    }
                    break;
                case 1:
                    //If the count is 1 and second file in the report class is not empty
                    if(!Objects.equals(report.getFileTwo(), "null")){
                        //the file name is added to the files array list
                        files.add(report.getFileTwo());
                    }
                    break;
                case 2:
                    //If the count is 2 and the third file in the report class is not empty
                    if(!Objects.equals(report.getFileThree(), "null")){
                        //the file name is added to the files array list
                        files.add(report.getFileThree());
                    }
                    break;
            }
        }

        //All the file names are looped through
        for(int i = 0; i < files.size(); i++){
            //The file type is retrieved via the first 5 characters of the file name eg image or video
            String fileType = files.get(i).substring(0,5);
            Log.d(TAG, "getFiles: " + fileType);

            //If the file type is a video
            if(Objects.equals(fileType, "video")){
                //A relative layout is initialised
                final RelativeLayout relativeLayout = new RelativeLayout(ReportViewActivity.this);
                //The background colour of the relative layout is set
                relativeLayout.setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                //Layout params for the relative layout is set with both the height and width of the layout set to match the parent
                RelativeLayout.LayoutParams relativeLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                //The params are then applied to the relative layout
                relativeLayout.setLayoutParams(relativeLayoutParams);

                //An imageview is initialised for a video play button
                ImageView videoIcon = new ImageView(ReportViewActivity.this);
                //The icon is then added to the imageview
                videoIcon.setImageResource(R.drawable.play_icon);
                //Layout params for the imageview is initialised with the height and width set to wrap the content of the icon
                RelativeLayout.LayoutParams imageViewLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                //The params are then set to center the icon within the relative layout
                imageViewLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                //The params are applied to the imageview
                videoIcon.setLayoutParams(imageViewLayoutParams);
                //the image view is then added to the relative layout
                relativeLayout.addView(videoIcon);

                //Gets the final variable of the for loop counter so it can be accessed from in onClickListener
                final int finalI = i;
                //An onclick listener is added to the videoIcon
                videoIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //The video view activity is then run so the user can view the video
                        //Note, the video is not loaded in this activity so that the user
                        //only downloads the video when they want to watch it so there are no
                        //unexpected data usages
                        Intent intent = new Intent(ReportViewActivity.this, VideoViewActivity.class);
                        //The file name is then passed to the intent
                        intent.putExtra("firebaseLocation", files.get(finalI));
                        //the activity is opened
                        startActivity(intent);
                    }
                });
                //The relative layout is then added to the scrollview
                fileLinearLayout.addView(relativeLayout);

            }else{
                //If the file is an image
                //A max file size is then specified
                final long TEN_MEGABYTES = 10 * 1024 * 1024;
                //The image is then loaded from Firebase
                storageRef.child("/"+report.getReportID() + "/" + files.get(i) + getResources().getString(R.string.image_file_extension)).getBytes(TEN_MEGABYTES).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        //If the download was successful, the bytes downloaded are put into a bitmap
                        Bitmap tempBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                        //A new relative layout is initialised
                        RelativeLayout relativeLayout = new RelativeLayout(ReportViewActivity.this);

                        //An image view is then initialised
                        ImageView tempImageView = new ImageView(ReportViewActivity.this);
                        //Layout params are then initialised for the imageview so that the width matches it's parent and the height wraps the images content
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        tempImageView.setLayoutParams(params);

                        Log.d(TAG, "onSuccess: tempBitmap.getWidth: " + tempBitmap.getWidth());
                        Log.d(TAG, "onSuccess: tempBitmap.getHeight" + tempBitmap.getHeight());

                        //In order to display the image, the width of the image should be equal to the width
                        //of the scroll view

                        //Therefore A scale factor is found in order to find out how much the image should
                        //be scaled in order to resize the image
                        double scaleFactor =  (double) fileLinearLayout.getWidth() / (double) tempBitmap.getWidth();

                        Log.d(TAG, "onSuccess: DISPLAY_WIDTH: " + DISPLAY_WIDTH);
                        Log.d(TAG, "onSuccess: " + tempBitmap.getHeight() * scaleFactor);
                        Log.d(TAG, "onSuccess: " + scaleFactor);

                        //The image is then scaled to the width of the scroll view and the height based on the scale factor
                        tempBitmap = Bitmap.createScaledBitmap(tempBitmap, fileLinearLayout.getWidth(), (int) Math.ceil(tempBitmap.getHeight() * scaleFactor), false);

                        //A final copy of this bitmap is then saved to be displayed later
                        final Bitmap finalBitmap = tempBitmap;

                        //A cropped version of the bitmap is added to the imageview so that it fits into the scroll view
                        tempImageView.setImageBitmap(Bitmap.createBitmap(tempBitmap, 0, 0, fileLinearLayout.getWidth(),  fileLinearLayout.getHeight()));

                        //Padding is added to the sides
                        tempImageView.setPadding(5, 0, 5, 0);
                        //the image view is added to the relative layout
                        relativeLayout.addView(tempImageView);
                        //The relative layout is added to the scroll view
                        fileLinearLayout.addView(relativeLayout);

                        //An onclick listener is then added to the image view
                        tempImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //A relative layout is initialised
                                final RelativeLayout layout = new RelativeLayout(ReportViewActivity.this);
                                //Layout params are added so that its height and width match its parent
                                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                                //The params are then applied to the layout
                                layout.setLayoutParams(params);

                                //An image view is initialised
                                ImageView imageView = new ImageView(ReportViewActivity.this);
                                //the final bitmap is then added to the imageview
                                imageView.setImageBitmap(finalBitmap);
                                //the imageview is added to the relative layout
                                layout.addView(imageView);

                                //A dialog builder is initialised
                                AlertDialog.Builder builder = new AlertDialog.Builder(ReportViewActivity.this);

                                //It's view is set to the relative layout
                                builder.setView(layout)
                                        //A close button is initialised and the dialog is then shown
                                        .setNegativeButton("Close", null)
                                        .show();

                            }
                        });
                    }
                });
            }
        }



    }

    private void getCreatorName(){
        //A single event listener is then added to find the report creator's username
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            //Function returns all users
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: " + dataSnapshot.toString());
                //Loops through all users
                for(DataSnapshot postSnapShot : dataSnapshot.getChildren()){
                    Log.d(TAG, "onDataChange: " + postSnapShot.toString());
                    //Checks if the creator userID=the userID in the database
                    if(Objects.equals(postSnapShot.getKey(), report.getCreator())){
                        //the creator text view is then set to the username
                        creatorTextView.setText((CharSequence) postSnapShot.getValue());
                        //Underlines the text within the textview
                        creatorTextView.setPaintFlags(creatorTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Initialises the map
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Sets up the map
        gMaps = googleMap;

        //Makes sure that the coordinates aren't empty
        if(report.getLatitude() != null && report.getLatitude() != null) {
            //Report coordinates are then linked together
            LatLng currentCoordinates = new LatLng(report.getLatitude(), report.getLongitude());
            //A marker is added to the report location
            gMaps.addMarker(new MarkerOptions().position(currentCoordinates));
            //The camera is moved to the location
            gMaps.moveCamera(CameraUpdateFactory.newLatLng(currentCoordinates));
            //Zooms into the report location
            gMaps.moveCamera(CameraUpdateFactory.newLatLngZoom(currentCoordinates, 14.0f));
        }
    }

    //Function checks if the user has voted
    private void checkIfVoted(){
        //Goes through the userVotes section of the database, then into the current users ID, and checks if
        //the current report ID is there
        userVotesRef.child(mUser.getUid()).orderByValue().equalTo(report.getReportID()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot postSnapShot: dataSnapshot.getChildren()){
                    //If a value is found then the for each loop will run
                    Log.d(TAG, "onDataChange: dataSnapShot Found");
                    //user vote key is retrieved (so that if the user takes away vote, then vote can be removed from userVotes)
                    userVoteKey = postSnapShot.getKey();
                    Log.d(TAG, "onDataChange: " + userVoteKey);
                    //The vote button style is updated
                    voteButton.setBackgroundResource(R.drawable.secondary_button_border);
                    //Vote button text is updated
                    voteButton.setText("Voted");
                    //Sets the colour of the vote text to white
                    voteButton.setTextColor(getResources().getColor(R.color.white));
                    //Finally userHasVoted is set to true
                    userHasVoted = true;
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Sets a live counter of the votes
    private void setVoteText(){
        //Adds a value listener to the votes of the current report
        reportRef.child(report.getReportID()).child("votes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Every time the votes changes, the votesTextView is updated to show change in votes
                votesTextView.setText("Votes: " + dataSnapshot.getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
