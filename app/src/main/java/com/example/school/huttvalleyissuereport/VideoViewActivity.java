package com.example.school.huttvalleyissuereport;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class VideoViewActivity extends AppCompatActivity {

    //Variable for logging to console
    private final static String TAG = "VideoViewActivity";

    //Initialising Firebase Storage
    private FirebaseStorage mStorage;
    private StorageReference storageRef;

    //Initialising Widgets
    private VideoView videoView;
    private Button closeButton;

    //Initialising Variables for VideoView
    private MediaController mediaController;
    private String firebaseLocation;
    private Report report;

    //Initialising Progress Bar
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_view);

        //Gets the file name passed from the report view activity
        firebaseLocation = getIntent().getExtras().getString("firebaseLocation");

        //Initialising Widgets
        videoView = (VideoView) findViewById(R.id.videoView);
        closeButton = (Button) findViewById(R.id.closeButton);

        //Onclick listener is added to close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Runs same function as onBackPressed
                onBackPressed();
            }
        });

        //Gets the clicked report from the task adapter class
        report = TaskAdapter.clickedReport;

        //Sets up the progress bar and shows it
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);

        //Sets up Firebase Storage
        mStorage = FirebaseStorage.getInstance();
        storageRef = mStorage.getReference(getResources().getString(R.string.storage_files_ref));

        //Initialises a temp file to hold the vide
            File tempFile = null;
            try {
                //Creates a reference to hold the file
                File storageRef = new File(Environment.getExternalStorageDirectory(), getResources().getString(R.string.video_file_prefix));
                //Checks if the reference exists
                if(!storageRef.exists()){
                    //reference is created
                    storageRef.mkdir();
                }
                //A temp file is then created with the prefix - "video" and suffix - "mp4"
                tempFile = File.createTempFile(getResources().getString(R.string.video_file_prefix), getResources().getString(R.string.video_file_extension));

            } catch (IOException e) {
                e.printStackTrace();
            }

            //If a temp file has been created
            if(tempFile != null){
                //creates final tempFile which can be accessed by the download function
                final File finalTempFile = tempFile;
                Log.d(TAG, "onCreate: " + finalTempFile.getAbsolutePath());
                //Video is downloaded from Firebase Storage
                storageRef.child("/"+report.getReportID()+"/"+firebaseLocation+getResources().getString(R.string.video_file_extension)).getFile(tempFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        //If the task is successful
                        //The progress bar is removed
                        progressBar.setVisibility(View.GONE);
                        //the path of the video view is set to the path of the temp file
                        videoView.setVideoPath(finalTempFile.getAbsolutePath());
                        //The video is started
                        videoView.start();
                        //A media controller is initialised (video controls)
                        mediaController = new MediaController(VideoViewActivity.this);
                        //The media controller is then added to the video view
                        mediaController.setAnchorView(videoView);
                        videoView.setMediaController(mediaController);
                    }
                });
            }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        onPause();
    }
}
