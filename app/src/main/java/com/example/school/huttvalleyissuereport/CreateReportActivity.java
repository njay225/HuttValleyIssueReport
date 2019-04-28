package com.example.school.huttvalleyissuereport;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;


public class CreateReportActivity extends AppCompatActivity {

    //Variable for logging to console
    private final String TAG = "Create Report Activity";

    //Permission Constants
    private final static int EXTERNAL_STORAGE_PERMISSION = 1;
    private final static int GET_FINE_LOCATION_PERMISSION = 2;

    //Face Detection Constants
    private final Size MAX_SIZE = new Size();
    private final Size MIN_SIZE = new Size(60, 60);
    private final int FLAGS = 2;
    private final int FRONTAL_MIN_NEIGHBORS = 1;
    private final int PROFILE_MIN_NEIGHBORS = 1;
    private final double FRONTAL_SCALE_FACTOR = 1.05;
    private final double PROFILE_SCALE_FACTOR = 1.04;

    private final Size FACE_BLUR = new Size(71, 71);
    private final int IMAGE_COMPRESSION_SCALE = 75;

    //Permission Booleans
    private Boolean canReadExternalStorage = false;
    private Boolean canReadFineLocation = false;

    //Intent Constants
    private static final int REQUEST_CAMERA_IMAGE = 1;
    private static final int SELECT_IMAGE = 2;
    private static final int SELECT_VIDEO = 3;
    private static final int REQUEST_CAMERA_VIDEO = 4;

    //File Size Constants
    private static final long VIDEO_SIZE_LIMIT = 100 * 1024 * 1024;

    //Initialising Edit Text Variables
    private EditText titleEditText, locationEditText, descriptionEditText;

    //Initialising Progress Spinner
    private ProgressBar progressBar;

    //String Variables to hold Edit Text Values and Spinner Values
    private String titleString, locationString, descriptionString, categoryString, urgencyString;

    private LatLng issueLocation;
    private Address currentAddress;

    //Initialising Buttons
    private Button addLocationButton, uploadFileButton, finishReportButton, saveReportButton;

    //Initialising Spinners (Drop-downs)
    private Spinner urgencySpinner, categorySpinner;

    //Initialising Helper Text Views
    private TextView titleHelperTextView, descriptionHelperTextView, fileUploadHelperTextView, locationHelperTextView;

    //Initialising File Preview Area
    private HorizontalScrollView fileScrollView;
    private LinearLayout fileLinearLayout;

    //Initialising Relative Layout
    private RelativeLayout relativeLayout;

    //Initialising OpenCV
    private File mCascadeFile;
    private CascadeClassifier mJavaDetectorFrontalFace, mJavaDetectorProfileFace;
    private BaseLoaderCallback mOpenCVCallBack = new
            BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(int status) {
                    super.onManagerConnected(status);
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
                            try {
                                //Loading Frontal Face Detector
                                InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                                File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                                mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                                FileOutputStream os = new FileOutputStream(mCascadeFile);

                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    os.write(buffer, 0, bytesRead);
                                }

                                mJavaDetectorFrontalFace = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                                mJavaDetectorFrontalFace.load(mCascadeFile.getAbsolutePath());
                                if (mJavaDetectorFrontalFace.empty()) {
                                    Log.d(TAG, "Failed to load Cascade");
                                } else {
                                    Log.d(TAG, "Cascade Loaded");
                                    cascadeDir.delete();
                                }

                                //Loading Profile Face Detector
                                is = getResources().openRawResource(R.raw.haarcascade_profileface);
                                cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                                mCascadeFile = new File(cascadeDir, "haarcascade_profileface.xml");
                                os = new FileOutputStream(mCascadeFile);

                                buffer = new byte[4096];
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    os.write(buffer, 0, bytesRead);
                                }
                                is.close();
                                os.close();

                                mJavaDetectorProfileFace = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                                mJavaDetectorProfileFace.load(mCascadeFile.getAbsolutePath());
                                if (mJavaDetectorProfileFace.empty()) {
                                    Log.d(TAG, "Failed to load Cascade");
                                } else {
                                    Log.d(TAG, "Cascade Loaded");
                                    cascadeDir.delete();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                    }
                }
            };

    //Initialising Firebase Database
    private FirebaseDatabase mDatabase;
    private DatabaseReference categoryRef, urgencyRef;

    //Initialising Firebase Storage
    private FirebaseStorage mStorage;
    private StorageReference mStorageReference;

    //Initialising Firebase Authentication
    private FirebaseAuth mAuth;

    //Initialising Array to hold FileNames
    ArrayList<String> fileNames = new ArrayList<String>();

    //Initialises the String variables of the file paths
    private String fileOne = "null", fileTwo = "null", fileThree = "null";

    //Initialising Array to hold Files
    private ArrayList<byte[]> imageFiles = new ArrayList<byte[]>();
    private ArrayList<Uri> videoFiles = new ArrayList<Uri>();
    private int videoFileCount = 0;
    private String currentPicturePath;
    private int imagesDetecting = 0;
    private int filesUploaded = 0;

    //Initialising Report Object to hold saved report
    private Report savedReport;

    //Initialising Arraylist which holds all saved reports
    private ArrayList<Report> savedReportsArrayList = new ArrayList<>();

    //Boolean to check if add location button has been clicked
    private boolean addLocationClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_report);

        //Sets the title at the top of the screen to "Create Report"
        getSupportActionBar().setTitle("Create Report");

        //Linking Buttons to Widgets
        uploadFileButton = (Button) findViewById(R.id.uploadFileButton);
        addLocationButton = (Button) findViewById(R.id.addLocationButton);
        finishReportButton = (Button) findViewById(R.id.createReportButton);
        saveReportButton = (Button) findViewById(R.id.saveReportButton);

        //Linking Edit Text Variables to Widgets
        titleEditText = (EditText) findViewById(R.id.titleEditText);
        locationEditText = (EditText) findViewById(R.id.locationTextView);
        descriptionEditText = (EditText) findViewById(R.id.descriptionEditText);

        //Linking Spinners to Widgets
        urgencySpinner = (Spinner) findViewById(R.id.urgencySpinner);
        categorySpinner = (Spinner) findViewById(R.id.categorySpinner);

        //Linking Helper Text Views to Widgets
        titleHelperTextView = (TextView) findViewById(R.id.titleHelperTextView);
        descriptionHelperTextView = (TextView) findViewById(R.id.descriptionHelperTextView);
        fileUploadHelperTextView = (TextView) findViewById(R.id.fileUploadHelperTextView);
        locationHelperTextView = (TextView) findViewById(R.id.locationHelperTextView);

        //Linking File Preview Variables to Widgets
        fileScrollView = (HorizontalScrollView) findViewById(R.id.fileScrollView);
        fileLinearLayout = (LinearLayout) findViewById(R.id.fileLinearLayout);

        //Linking Relative Layout Variable to Widget
        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);

        //Linking Progress Spinner to Widget
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        //Setting On click listener for fileUploadButton
        uploadFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Requests Storage Permissions from user if their android version is >M
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !canReadExternalStorage) {
                    //Request Storage Permissions
                    requestStoragePermissions();
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || canReadExternalStorage) {
                    //Else, clear any previous errors
                    fileUploadHelperTextView.setText("");
                    //Run get File Function
                    getFile();
                }

            }
        });

        //Setting On Click Listener for addLocationButton
        addLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Checks if user is running android marshmallow or higher and canReadFineLocation is false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !canReadFineLocation) {
                    //If they are, check their location permissions
                    requestLocationPermissions();
                }else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || canReadFineLocation) {
                    addLocationClicked = true;
                    openLocationDialog();
                }
            }
        });

        //Setting On Click Listener for Finish Report Button
        finishReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creates a new Alert Dialog
                new AlertDialog.Builder(CreateReportActivity.this)
                        //Set the title
                        .setTitle("Are you sure?")
                        //Sets the "Yes" Button
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //If clicked, finishReport function is called
                                checkReportValidity();
                            }
                        })
                        //Sets "No" Button
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        saveReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(CreateReportActivity.this)
                        .setTitle("Are you sure you want to save the report?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saveReport();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        savedReportsArrayList = SavedReportActivity.savedReportsArrayList;
        Log.d(TAG, "onCreate: " + getIntent().getExtras().get("savedReport"));
        //Checking if activity was opened from savedReportActivity
        if(Objects.equals(getIntent().getExtras().get("savedReport"), "true")){
            savedReport = SavedReportActivity.clickedSavedReport;
            populateFields();
        }

        //Linking to FireBase Database
        mDatabase = FirebaseDatabase.getInstance();
        categoryRef = mDatabase.getReference(String.valueOf(R.string.database_category_ref));
        urgencyRef = mDatabase.getReference(String.valueOf(R.string.database_reports_ref));

        //Linking to Firebase Storage
        mStorage = FirebaseStorage.getInstance();
        mStorageReference = mStorage.getReference();

        //Linking to Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        //Populate Spinners
        populateSpinners populateSpinners = new populateSpinners();
        populateSpinners.execute();

    }

    private boolean checkConnection(){
        //Variables for checking network status
        ConnectivityManager cm;
        NetworkInfo activeNetwork;

        //Checks if device is connected to a network
        cm = (ConnectivityManager) getBaseContext().getSystemService(CONNECTIVITY_SERVICE);
        activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void checkLocationIsLowerHutt(){
        try {
            //Gets the address based on the coordinates (array is returned)
            List<Address> addresses = new Geocoder(CreateReportActivity.this).getFromLocation(issueLocation.latitude, issueLocation.longitude, 1);
            //Sets the current address to the first and only object in the addresses array
            currentAddress = addresses.get(0);

            Log.d(TAG, "checkLocationIsHuttValley: " + currentAddress);

            //Checks if the locality of the address is in Lower Hutt
            if(Objects.equals(currentAddress.getLocality(), "Lower Hutt")){
                Log.d(TAG, "populateFields: Location is in lower hutt");
                //Initialising string to hold address
                String address = "";
                //Initialising counter to go through each line of the address
                int counter = 0;
                //While there is an address line at the index "counter"
                while(currentAddress.getAddressLine(counter) != null){
                    //Add it to the address string
                    address += currentAddress.getAddressLine(counter) + ", ";
                    //increase the counter by 1
                    counter++;
                }
                //Removes the last "," from the address string
                address = address.substring(0, address.length() - 2);
                //Sets the location edit text to the address
                locationEditText.setText(address);
                MapsActivity.issueLocation = issueLocation;
            }else{
                issueLocation = null;
                Toast.makeText(this, "Issue Location is not in Lower Hutt", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void populateFields(){
        titleEditText.setText(savedReport.getTitle());
        descriptionEditText.setText(savedReport.getDescription());
        locationEditText.setText(savedReport.getAddress());
        issueLocation = new LatLng(savedReport.getLatitude(), savedReport.getLongitude());

        ArrayList<String> videoFilesUriString = new ArrayList<>();
        try {
            FileInputStream fileInputStream = openFileInput(savedReport.getReportID()+"imagesArray");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            imageFiles = (ArrayList<byte[]>) objectInputStream.readObject();
            objectInputStream.close();
            Log.d(TAG, "populateFields: " + imageFiles.size());

            fileInputStream = openFileInput(savedReport.getReportID()+"videoArray");
            objectInputStream = new ObjectInputStream(fileInputStream);
            videoFilesUriString = (ArrayList<String>) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        for(int i = 0; i<videoFilesUriString.size(); i++){
            if(videoFilesUriString.get(i)!=null){
                videoFiles.add(Uri.parse(videoFilesUriString.get(i)));
            }else{
                videoFiles.add(null);
            }
        }

        for(int i = 0; i < videoFiles.size(); i++){
            if(videoFiles.get(i) != null){
                String videoPath = videoFiles.get(i).getPath();

                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
                Log.d(TAG, "Thumbnail Created");

                //Increases the count of video files by 1
                videoFileCount++;

                //The thumbnail is added to the scroll view
                addImageToPreview(thumbnail, true, videoPath);
            }
        }

        for(int i = 0; i <imageFiles.size(); i++){
            if(imageFiles.get(i) != null){
                byte[] imageByteArray = imageFiles.get(i);
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
                addImageToPreview(bitmap, false, "");
            }
        }

        Log.d(TAG, "populateFields: saved report address " +savedReport.getAddress());
        Log.d(TAG, "populateFields: connected? " + checkConnection());
        Log.d(TAG, "populateFields: issue location " + issueLocation);
        if(issueLocation != null && checkConnection() && savedReport.getAddress() == null){
            checkLocationIsLowerHutt();
        }else if(savedReport.getAddress() != null){
            locationEditText.setText(savedReport.getAddress());
        }

    }

    private void openLocationDialog() {
        //Opens the Maps Activity
        Intent intent = new Intent(CreateReportActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    private void getFile() {
        //Sets the array of Options for getting a file
        final CharSequence[] dialogOptions = {"Video", "Image"};
        Log.d(TAG, "getFile: " + (fileNames.size() + imagesDetecting));
        //Checks if the user is under the upload limit
        if((fileNames.size() + imagesDetecting) < getResources().getInteger(R.integer.file_upload_limit)) {
            //Initialises a dialog builder
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateReportActivity.this);
            //Sets the title of the dialog
            builder.setTitle("Upload File")
                    //Sets the options for the dialog
                    .setItems(dialogOptions, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int option) {
                            Log.d(TAG, String.valueOf(option));
                            if (option == 0) {
                                //If the user wants to select a video, uploadVideoFile function is called
                                uploadVideoFile();
                            } else if (option == 1) {
                                //If the user wants to select an image, uploadImageFile function is called
                                uploadImageFile();
                            }
                        }
                    })
                    .show();
        }else{
            //If user is at file upload limit, show them an error
            fileUploadHelperTextView.setText(R.string.create_report_file_limit_reached);
        }
    }

    private void uploadVideoFile() {
        //Sets the Array of options to choose where to get the video file from
        final CharSequence[] dialogOptions = {"Camera", "Gallery"};

        //Checks if a video has already been uploaded
        if(videoFileCount >= getResources().getInteger(R.integer.video_upload_limit)){
            fileUploadHelperTextView.setText(R.string.create_report_video_limit_reached);
        }else{
            //If no video has been uploaded
            //Initialise a new dialog builder
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateReportActivity.this);
            //Set the dialog options
            builder.setTitle("How do you want to get the Video?").
                    setItems(dialogOptions, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int option) {
                            if (option == 0) {
                                //If the user want to record a video
                                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                                //Sets the video quality to a minimum
                                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
                                //Opens the camera
                                startActivityForResult(intent, REQUEST_CAMERA_VIDEO);
                            } else if (option == 1) {
                                //If the user wants to open the gallery
                                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                                //This makes sure that the videos shown in the gallery are saved to the device
                                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                                //The Gallery is opened
                                startActivityForResult(intent, SELECT_VIDEO);
                            }
                        }
                    }).show();
        }
    }

    private void uploadImageFile() {
        //Initialises options to go into dialog menu
        final CharSequence[] dialogOptions = {"Camera", "Gallery"};
        //Initialise a dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(CreateReportActivity.this);
        //Set the dialog title
        builder.setTitle("How do you want to get the Image?")
                //Add the dialog options
                .setItems(dialogOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int option) {
                        if (option == 0) {
                            //If the user wants to take image from camera
                            //Open Camera
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, REQUEST_CAMERA_IMAGE);
                        } else if (option == 1) {
                            //If user wants to choose image from gallery
                            //Open gallery
                            Intent intent = new Intent();
                            //This makes sure that the images shown in the gallery are saved to the device
                            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                            intent.setType("image/*");
                            intent.setAction(Intent.ACTION_PICK);
                            startActivityForResult(intent, SELECT_IMAGE);
                        }
                    }
                }).show();

    }

    private void requestLocationPermissions() {
        //Checks if the app has permission to access location, if not, permission is requested
        if (ContextCompat.checkSelfPermission(CreateReportActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GET_FINE_LOCATION_PERMISSION);
        } else {
            addLocationClicked = true;
            canReadFineLocation = true;
            openLocationDialog();
        }
    }

    private void requestStoragePermissions() {
        //Checks if the app has permission to access external storage, if not, permission is requested
        if (ContextCompat.checkSelfPermission(CreateReportActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(CreateReportActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION);
        } else {
            canReadExternalStorage = true;
            getFile();
        }
    }

    //Runs After the user has granted or denied permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case EXTERNAL_STORAGE_PERMISSION:
                //If the permission was for storage
                //If they were granted
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    //set canReadExternalStorage to true
                    canReadExternalStorage = true;
                } else {
                    //Else inform user that they cannot upload a file
                    Toast.makeText(this, "Permission is needed to upload file", Toast.LENGTH_SHORT).show();
                }
                break;
            case GET_FINE_LOCATION_PERMISSION:
                //If the permission was for location
                //If permission was granted
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    //set canReadFineLocation to true
                    canReadFineLocation = true;
                } else {
                    //Else inform user that they cannot add a location
                    Toast.makeText(this, "Permission is needed to access location", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    //Runs when an image or video has been selected or captured
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Checking if OpenCV has loaded
        //If openCV has loaded
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV loaded successfully");
            //Run the call back as a success
            mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d(TAG, "OpenCV Not Loaded");
            //Load OpenCV
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mOpenCVCallBack);
        }

        //If the intent if okay and the data captured is not empty
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case SELECT_IMAGE:
                    //If image has been selected from the gallery getImageDataFromGallery
                    Toast.makeText(this, "Image Selected", Toast.LENGTH_SHORT).show();
                    getImageDataFromGallery(data);
                    break;
                case SELECT_VIDEO:
                    //If video has been selected from the gallery getVideo
                    Toast.makeText(this, "Video Selected", Toast.LENGTH_SHORT).show();
                    getVideo(data);
                    break;
                case REQUEST_CAMERA_IMAGE:
                    //If image has been captured from the camera getImageDataFromCamera
                    Toast.makeText(this, "Image Taken", Toast.LENGTH_SHORT).show();
                    getImageDataFromCamera(data);
                    break;
                case REQUEST_CAMERA_VIDEO:
                    //If video has been captured from the camera getVideo
                    Toast.makeText(this, "Video Recorded", Toast.LENGTH_SHORT).show();
                    getVideo(data);
                    break;
            }

        }

    }

    private void getImageDataFromCamera(Intent data) {
        Bitmap bitmap = (Bitmap) data.getExtras().get("data");
        detectFace(bitmap, (String) data.getExtras().get("path"));

    }

    @SuppressWarnings("deprecation")
    private void getVideo(Intent data) {
        //Get the String path of the video
        Uri selectedVideo = data.getData();
        String[] filePathColumn = {MediaStore.Images.Media.DATA};

        Cursor cursor = getContentResolver().query(selectedVideo, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String videoPath = cursor.getString(columnIndex);
        try{
            File file = new File(videoPath);
            //Checks if the video is too large
            if(file.length() > VIDEO_SIZE_LIMIT){
                //Gives the user a video size error
                fileUploadHelperTextView.setText("Video needs to be under " + VIDEO_SIZE_LIMIT / (1024*1024) + "MB");
            }else{
                //If video is valid
                //Create a thumbnail for the video
                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
                Log.d(TAG, "Thumbnail Created");

                //Adds the location of the video to the video files array
                videoFiles.add(Uri.fromFile(file));
                //Adds a place holder to the imageFiles array
                imageFiles.add(null);
                //Increases the count of video files by 1
                videoFileCount++;

                //The thumbnail is added to the scroll view
                addImageToPreview(thumbnail, true, videoPath);

            }
        }catch (Exception e){
            if(e instanceof NullPointerException){
                fileUploadHelperTextView.setText(R.string.videoError);
            }
        }




    }

    private void getImageDataFromGallery(Intent data) {
        //Gets the String path of the selected image
        Uri selectedImage = data.getData();
        String[] filePathColumn = {MediaStore.Images.Media.DATA};

        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        Log.d(TAG, selectedImage.toString());
        cursor.close();

        //Decrease the size of image to load it faster
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;

        //Gets image at picturePath
        Bitmap bitmap = BitmapFactory.decodeFile(picturePath, options);

        //Changes orientation of image so that it is upright when displayed
        float degree = 0;
        try {
            //Get the meta data of the image (ExifInterface holds the meta data)
            ExifInterface imgParams = new ExifInterface(picturePath);

                //If Image has meta data, orientation information is located
                int orientation = imgParams.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
                //If the orientation of the image exists (-1 is the default value)
                if (orientation != -1) {
                    switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            //Image is rotated by 90 degrees
                            degree = 90;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            //Image is rotated by 180 degrees
                            degree = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            //Image is rotated by 270 degrees
                            degree = 270;
                            break;
                    }

                }
          //Catch runs if ExifInterface(picturePath) has an exception
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Rotate the bitmap based on orientation information
        bitmap = rotateBitmap(bitmap, degree);
        //Call the detect face function
        detectFace(bitmap, picturePath);
    }

    private void detectFace(Bitmap bitmap, String picturePath) {
        //Sets the currentPicturePath to the passed picturePath,
        //so that it can be accessed by the background task
        currentPicturePath = picturePath;
        //Run the background task to detect faces in the passed bitmap
        detectFace detectFace = new detectFace();
        detectFace.execute(bitmap);
    }

    private void addImageToPreview(final Bitmap bitmap, final Boolean isVideo, final String path) {
        Log.d(TAG, "addImageToPreview: Is Running");
        //Initialise new Image View Widget
        final ImageView fileScrollViewImageView = new ImageView(CreateReportActivity.this);

        //Sets the Image View image to the passed bitmap
        fileScrollViewImageView.setImageBitmap(bitmap);

        //Gets rid of any borders for the image
        fileScrollViewImageView.setAdjustViewBounds(true);
        //Adds padding the left and right of the Image View
        fileScrollViewImageView.setPadding(5, 0, 5, 0);

        //Generates a file name for Firebase Storage
        //Initialises the String variable for the fileName
        final String fileName;
        //If the file is a video
        if(isVideo){
            //Set the file name to "video" + its place in the fileNames Arraylist
            fileName = getResources().getString(R.string.video_file_prefix) + String.valueOf(fileNames.size() + 1);
        }else{
            //Set the file name to "image" + its place in the fileNames Arraylist
            fileName = getResources().getString(R.string.image_file_prefix) + String.valueOf(fileNames.size() + 1);
        }
        //Adds the fileName to the array of file names
        fileNames.add(fileName);

        Log.d(TAG, "addImageToPreview: image is being added to view");
        //Adds the Image View to the scroll view
        fileLinearLayout.addView(fileScrollViewImageView);

        Log.d(TAG, "addImageToPreview: " + fileLinearLayout.getChildCount());

        //Adds an onClickListener to the Image View
        fileScrollViewImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creates an Alert Dialog to preview the image or video
                //Initialises a Relative layout for the dialog
                final RelativeLayout layout = new RelativeLayout(CreateReportActivity.this);
                //Initialises layout params for the layout to match the height and width of the dialog
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                layout.setLayoutParams(params);

                //Sets up a Video View and Image View variable
                VideoView videoView;
                ImageView imageView;

                //Initialise Layout Params for the image or video
                RelativeLayout.LayoutParams fileParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                fileParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

                Log.d(TAG, "onClick: " + isVideo);
                //If the file is a video
                if (isVideo) {
                    //Initialise a Video View
                    videoView = new VideoView(CreateReportActivity.this);
                    videoView.setLayoutParams(fileParams);
                    //Sets the video view video to the file specified at the path
                    videoView.setVideoPath(path);
                    //Starts the video
                    videoView.start();
                    //Brings the Video View in front of everything
                    videoView.setZOrderOnTop(true);
                    //Adds the Video to the dialog layout
                    layout.addView(videoView);
                } else {
                    Log.d(TAG, "onClick: File is image");
                    //If the file is an image
                    //Initialise the Image View
                    imageView = new ImageView(CreateReportActivity.this);
                    imageView.setLayoutParams(fileParams);
                    //Add the image Bitmap to the Image View
                    imageView.setImageBitmap(bitmap);
                    //Add the Image to the dialog layout
                    layout.addView(imageView);
                }

                //Initialise an Alert Dialog Builder
                AlertDialog.Builder builder = new AlertDialog.Builder(CreateReportActivity.this);

                //Set the builder view to the relative layout
                builder.setView(layout)
                        //Adds the close button
                        .setPositiveButton("Close", null)
                        //Adds a Delete Button
                        .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Creates an alert dialog to confirm deleting the file
                                new AlertDialog.Builder(CreateReportActivity.this)
                                        //Sets dialog title to "Are you sure"
                                        .setTitle("Are you Sure?")
                                        //Sets "No" Button
                                        .setNegativeButton("No", null)
                                        //Sets the "Yes" Button
                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                //Removes the file preivew from the scroll view
                                                fileLinearLayout.removeView(fileScrollViewImageView);
                                                //Goes through the files names array to find the current file name
                                                for(int i = 0; i < fileNames.size(); i++){
                                                    //If the file names are equal
                                                    if(Objects.equals(fileNames.get(i), fileName)){
                                                        //Remove that index from all file arrays
                                                        fileNames.remove(i);
                                                        videoFiles.remove(i);
                                                        imageFiles.remove(i);
                                                        //Checks if the file was a video
                                                        if(isVideo){
                                                            //decrease the video file counter so
                                                            //user can upload another video
                                                            videoFileCount--;
                                                        }
                                                    }
                                                }
                                                Log.d(TAG, "onClick: " + fileNames.toString());
                                        //Shows the confirmation dialog
                                            }
                                        }).show();
                        //Shows the file preview dialog
                            }
                        })
                        .show();
            }
        });
        Log.d(TAG, "addImageToPreview: " + fileNames.toString());

    }

    private Bitmap rotateBitmap(Bitmap bitmap, float degree) {
        //Matrix is created which is then rotated by angle passed to method
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);

        //New bitmap is returned based on the rotated matrix
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void checkExistingReports(){

        ArrayList<Report> existingReports = new ArrayList<>();
        ListView reportListView = new ListView(CreateReportActivity.this);
        reportListView.setDividerHeight(20);
        reportListView.setPadding(8, 8, 8, 8);
        reportListView.setElevation(2.0f);

        RelativeLayout relativeLayout = new RelativeLayout(CreateReportActivity.this);
        relativeLayout.addView(reportListView);

        for(int i = 0; i < MostRecent.reportsArray.size(); i++){
            Report report = MostRecent.reportsArray.get(i);
            Boolean isExisting = true;
            if(!Objects.equals(categoryString, report.getCategory())){
                Log.d(TAG, "checkExistingReports: " + categoryString + " != " + report.getCategory());
                isExisting = false;
            }else if(currentAddress != null){
                Log.d(TAG, "checkExistingReports: current address street " + currentAddress.getThoroughfare());
                String thoroughFareInitial = currentAddress.getThoroughfare();
                String thoroughFareFinal = currentAddress.getThoroughfare();

                Log.d(TAG, "checkExistingReports: " + thoroughFareInitial.substring((thoroughFareInitial.length() - 4), thoroughFareInitial.length()));
                //Checks if Street Name ends in Road
                if(Objects.equals(thoroughFareInitial.substring((thoroughFareInitial.length() - 4), thoroughFareInitial.length()), "Road")){
                    thoroughFareFinal = thoroughFareInitial.substring(0, thoroughFareInitial.length() -5);
                    thoroughFareFinal += " Rd";
                }

                if(!report.getAddress().contains(currentAddress.getThoroughfare()) && !report.getAddress().contains(thoroughFareFinal)){
                    Log.d(TAG, "checkExistingReports: " + currentAddress.getThoroughfare() + " != " + report.getAddress() + " != " + thoroughFareFinal);
                    isExisting = false;
                }
            }else if(report.getResolved()){
                Log.d(TAG, "checkExistingReports: " + report.getResolved());
                isExisting = false;
            }

            if(isExisting){
                existingReports.add(report);
            }
        }

        TaskAdapter adapter = new TaskAdapter(this, existingReports);
        reportListView.setAdapter(adapter);

        if(existingReports.size()>0){
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateReportActivity.this);

            builder.setView(relativeLayout)
                    .setTitle("Does your report already exist?")
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            uploadReport();
                        }
                    })
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(CreateReportActivity.this, HomeActivity.class);
                            startActivity(intent);
                        }
                    }).show();
        }else{
            uploadReport();
        }
    }

    private void uploadReport(){
        //Starts Progress Spinner
        progressBar.setVisibility(View.VISIBLE);
        //generates a reportID in Firebase
        final String reportID = mDatabase.getReference().child(getResources().getString(R.string.database_reports_ref)).push().getKey();

        //Creates a report object using the information above
        final Report report = new Report(titleString, descriptionString, categoryString, urgencyString, locationString, new Date().getTime(),
                issueLocation.latitude, issueLocation.longitude, mAuth.getCurrentUser().getUid(), fileOne, fileTwo, fileThree, false, reportID, 0);

        //Loops through all of the fileNames and adds them to FirebaseStorage
        for (int i = 0; i < fileNames.size(); i++) {
            //Initialises temp String variable currentFileName
            String currentFileName;
            //Initialises temp Metadata variable to hold Firebase Storage file Metadata
            StorageMetadata metaData;
            //Gets the file type of the file which is the first 5 characters of the file name
            String fileType = fileNames.get(i).substring(0,5);
            Log.d(TAG, "finishReport: " + fileNames.get(i).substring(0,5));
            //Initialises a upload task which is used to upload the file to Firebase
            UploadTask uploadTask;

            //Checks if the file type was a video
            if (Objects.equals(fileType, getResources().getString(R.string.video_file_prefix))) {
                //Sets the current file name
                currentFileName = fileNames.get(i) + getResources().getString(R.string.video_file_extension);
                //Sets the file type to "video/mpeg"
                metaData = new StorageMetadata.Builder()
                        .setContentType(getResources().getString(R.string.video_filetype)).build();

                //Creates a reference for the file location
                mStorageReference = mStorage.getReference().child(getResources().getString(R.string.storage_files_ref)).child(reportID).child(currentFileName);

                Log.d(TAG, "finishReport: " + videoFiles.get(i).toString());

                //Puts the video file into the file location from the reference above
                uploadTask = mStorageReference.putFile(videoFiles.get(i), metaData);
            } else {
                //If the file was an image
                currentFileName = fileNames.get(i) + getResources().getString(R.string.image_file_extension);
                //Sets the file type to "image/jpeg"
                metaData = new StorageMetadata.Builder()
                        .setContentType(getResources().getString(R.string.image_file_extension)).build();

                //Creates a reference for the file location
                mStorageReference = mStorage.getReference().child(getResources().getString(R.string.storage_files_ref)).child(reportID).child(currentFileName);

                //Uploads the image bytes to the file location from the reference above
                uploadTask = mStorageReference.putBytes(imageFiles.get(i), metaData);
            }

            //Initialises a final version of the loop counter which can be accessed in the onSuccessListener
            final int finalI = i;

            //Adds a success listener to the file upload
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //Sends a message to the user that the file has been uploaded
                    Toast.makeText(CreateReportActivity.this, "File " + (finalI + 1) + " Uploaded", Toast.LENGTH_SHORT).show();
                    filesUploaded++;
                    //Checks if the final file has been uploaded
                    if(filesUploaded == fileNames.size()){
                        //Adds the report to Firebase database
                        mDatabase.getReference().child(getResources().getString(R.string.database_reports_ref)).child(reportID).setValue(report);

                        if(Objects.equals(getIntent().getExtras().get("savedReport"), "true")){
                            savedReportsArrayList.set(Integer.parseInt(savedReport.getReportID()), null);
                            try {
                                FileOutputStream fileOutputStream = openFileOutput(getResources().getString(R.string.saved_reports_array_list_location), MODE_PRIVATE);
                                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                                objectOutputStream.writeObject(savedReportsArrayList);
                                objectOutputStream.flush();
                                objectOutputStream.close();
                                fileOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                        //Takes the user back to the homepage
                        Intent intent = new Intent(CreateReportActivity.this, HomeActivity.class);
                        startActivity(intent);
                    }
                }
            });
        }

        //If the user has uploaded no images
        if(fileNames.size() == 0){
            //Hides Progress Spinner
            progressBar.setVisibility(View.GONE);
            //Adds the report to Firebase Database
            mDatabase.getReference().child(getResources().getString(R.string.database_reports_ref)).child(reportID).setValue(report);

            if(Objects.equals(getIntent().getExtras().get("savedReport"), "true")){
                savedReportsArrayList.set(Integer.parseInt(savedReport.getReportID()), null);
                try {
                    FileOutputStream fileOutputStream = openFileOutput(getResources().getString(R.string.saved_reports_array_list_location), MODE_PRIVATE);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(savedReportsArrayList);
                    objectOutputStream.flush();
                    objectOutputStream.close();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //Takes the user back to the homepage
            Intent intent = new Intent(CreateReportActivity.this, HomeActivity.class);
            startActivity(intent);
        }
    }

    private void getReportData(){
        //Gets values from user inputs and saves them to String variables
        titleString = titleEditText.getText().toString();
        descriptionString = descriptionEditText.getText().toString();
        urgencyString = urgencySpinner.getSelectedItem().toString();
        categoryString = categorySpinner.getSelectedItem().toString();
        if(!Objects.equals(locationEditText.getText().toString(), "")){
            if(!Objects.equals(locationEditText.getText().toString().substring(0,14), "Report Location")){
                checkLocationIsLowerHutt();
                locationString = locationEditText.getText().toString();
                Log.d(TAG, "getReportData: Location " + locationString);
            }
        }

        //Loops through each of the fileNames and adds them to the respective String Variable
        for(int i = 0; i < fileNames.size(); i++){
            switch (i) {
                case 0:
                    fileOne = fileNames.get(i);
                    break;
                case 1:
                    fileTwo = fileNames.get(i);
                    break;
                case 2:
                    fileThree = fileNames.get(i);
                    break;
            }
        }
    }

    private void checkReportValidity(){
        //Initialising Boolean to check if report is valid or not
        Boolean reportIsValid = false;

        getReportData();

        //Error Checking
        //Checks if the title is empty
        if(Objects.equals(titleString, "")){
            titleHelperTextView.setText(R.string.create_report_null_title);
        //Checks if the description is empty
        }else if(Objects.equals(descriptionString, "")){
            descriptionHelperTextView.setText(R.string.create_report_null_description);
        //Checks if the location is empty
        }else if(Objects.equals(locationString, "")){
            locationHelperTextView.setText(R.string.create_report_null_location);
        //Checks if the urgency is the default value
        }else if(Objects.equals(urgencyString, getResources().getString(R.string.default_urgency))){
            Toast.makeText(this, "Please specify an urgency", Toast.LENGTH_LONG).show();
        //Checks if the category is the default value
        }else if(Objects.equals(categoryString, getResources().getString(R.string.default_category))){
            Toast.makeText(this, "Please specify a category", Toast.LENGTH_LONG).show();
        }else if(!checkConnection()){
            Toast.makeText(this, "Please Connect to the Internet", Toast.LENGTH_SHORT).show();
        }else{
            //If all requirements are met, the report is valid
            reportIsValid = true;
        }

        //This only runs if the report is Valid (reportIsValid = true)
        if(reportIsValid) {
            checkExistingReports();
        }
    }

    private void saveReport(){
        getReportData();

        Report report;
        String imagesArrayFileName, videoArrayFileName;

        if(issueLocation == null){
            Toast.makeText(this, "Please add a report location", Toast.LENGTH_SHORT).show();
            return;
        }

        if(savedReport != null){
            report = new Report(titleString, descriptionString, categoryString, urgencyString, locationString, new Date().getTime(),
                    issueLocation.latitude, issueLocation.longitude, mAuth.getCurrentUser().getUid(), fileOne, fileTwo, fileThree, false, savedReport.getReportID(), 0);
            savedReportsArrayList.set(Integer.parseInt(savedReport.getReportID()), report);
        }else{
            report = new Report(titleString, descriptionString, categoryString, urgencyString, locationString, new Date().getTime(),
                    issueLocation.latitude, issueLocation.longitude, mAuth.getCurrentUser().getUid(), fileOne, fileTwo, fileThree, false, String.valueOf(savedReportsArrayList.size()), 0);
            savedReportsArrayList.add(report);
        }

        imagesArrayFileName = report.getReportID()+"imagesArray";
        videoArrayFileName = report.getReportID()+"videoArray";

        ArrayList<String> videoFilesStringUri = new ArrayList<>();
        for(int i = 0; i < videoFiles.size(); i++){
            if(videoFiles.get(i) != null){
                videoFilesStringUri.add(videoFiles.get(i).toString());
            }else{
                videoFilesStringUri.add(null);
            }
        }

        try {
            //Saving Report Data
            FileOutputStream fileOutputStream = openFileOutput(getResources().getString(R.string.saved_reports_array_list_location), MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(savedReportsArrayList);
            objectOutputStream.flush();
            objectOutputStream.close();
            fileOutputStream.close();

            fileOutputStream = openFileOutput(imagesArrayFileName, MODE_PRIVATE);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(imageFiles);
            objectOutputStream.flush();
            objectOutputStream.close();
            objectOutputStream.close();

            fileOutputStream = openFileOutput(videoArrayFileName, MODE_PRIVATE);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(videoFilesStringUri);
            objectOutputStream.flush();
            objectOutputStream.close();
            objectOutputStream.close();

            Toast.makeText(this, "Report Saved", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(CreateReportActivity.this, HomeActivity.class);
            startActivity(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        //Gets the last address from the MapsActivity
        currentAddress = MapsActivity.currentAddress;
        //Checks if an address exists
        if(currentAddress != null && addLocationClicked){
            issueLocation = MapsActivity.issueLocation;
            //Initialising string to hold address
            String address = "";
            //Initialising counter to go through each line of the address
            int counter = 0;
            //While there is an address line at the index "counter"
            while(currentAddress.getAddressLine(counter) != null){
                //Add it to the address string
                address += currentAddress.getAddressLine(counter) + ", ";
                //increase the counter by 1
                counter++;
            }
            //Removes the last "," from the address string
            address = address.substring(0, address.length() - 2);
            //Sets the location edit text to the address
            locationEditText.setText(address);
            addLocationClicked = false;
        }else if(issueLocation != null && addLocationClicked){
            locationEditText.setText("Report Location: " + issueLocation.latitude + ", " + issueLocation.longitude);
            addLocationClicked = false;
        }

    }

    //Background Task to detect faces, it runs in the background because face detection can take a long time.
    private class detectFace extends AsyncTask<Bitmap, Void, Bitmap>{

        private ProgressBar progressBar = new ProgressBar(CreateReportActivity.this);

        //Before the background task is executed
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            //Increases the current number of images being detected by 1
            imagesDetecting++;
            //Adds a progress spinner to the file preview window
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
            fileLinearLayout.addView(progressBar);
            //Sets the finish report and save report to unclickable
            finishReportButton.setClickable(false);
            saveReportButton.setClickable(false);
        }

        //The Actual Background Task - Face Detection
        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            Log.d(TAG, "doInBackground: Detecting Face");
            //Gets the first parameter passed - the bitmap which is to be scanned
            Bitmap bitmap = params[0];
            //Converting image to Matrix for OpenCV
            Bitmap tempBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Mat matrix = new Mat(tempBitmap.getHeight(), tempBitmap.getWidth(), CvType.CV_8UC4);
            Utils.bitmapToMat(tempBitmap, matrix);

            //Detect Face
            //Converts matrix to greyscale
            Mat greyMat = new Mat();
            Mat gaussianBlur = new Mat();
            Imgproc.cvtColor(matrix, greyMat, Imgproc.COLOR_RGB2GRAY);

            //Blurs greyscale matrix to assist face detection
            Imgproc.GaussianBlur(greyMat, gaussianBlur, new Size(5, 5), 5);

            //Sets up the rectangle matrices used to outline faces
            MatOfRect faces = new MatOfRect();
            MatOfRect profile = new MatOfRect();

            if (mJavaDetectorFrontalFace != null) {
                Log.d(TAG, "doInBackground: mJavaDetectorFrontalFace is not null");
                //Detect faces using Frontal Face Cascade
                mJavaDetectorFrontalFace.detectMultiScale(gaussianBlur, faces, FRONTAL_SCALE_FACTOR, FRONTAL_MIN_NEIGHBORS, FLAGS,  MIN_SIZE, MAX_SIZE);
            }

            if (mJavaDetectorProfileFace != null) {
                Log.d(TAG, "doInBackground: mJavaDetectorProfileFace is not null");
                //Detect Faces using Profile Face Cascade
                mJavaDetectorProfileFace.detectMultiScale(gaussianBlur, faces, PROFILE_SCALE_FACTOR, PROFILE_MIN_NEIGHBORS, FLAGS, MIN_SIZE, MAX_SIZE);
            }

            Rect[] facesArray = faces.toArray();
            for (Rect aFacesArray : facesArray) {
                //Gets the section of the matrix which has a face and blurs it
                Mat mask = matrix.submat(aFacesArray);
                Imgproc.GaussianBlur(mask, mask, FACE_BLUR, 71);
            }

            Rect[] profileFacesArray = profile.toArray();
            for (Rect aProfileFacesArray : profileFacesArray) {
                //Gets the section of the matrix which has a face and blurs it
                Mat mask = matrix.submat(aProfileFacesArray);
                Imgproc.GaussianBlur(mask, mask, FACE_BLUR, 71);
            }


            //Converts matrix with blurring to bitmap
            Utils.matToBitmap(matrix, bitmap);

            //Compresses bitmap to jpeg and then to a byte array
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_SCALE, byteArrayOutputStream);
            byte [] tempByteArray;
            tempByteArray = byteArrayOutputStream.toByteArray();
            Log.d(TAG, "detectFace: " + tempByteArray.toString());

            //Adds the byte array to another array which is holding all the images
            imageFiles.add(tempByteArray);

            //Adds a place holder to the video array so that the index in the fileNames array matches with
            //the index in the imageFiles and videoFiles arrays
            videoFiles.add(null);

            //The bitmap is then returned to onPostExecute
            return bitmap;
        }


        //After the task is completed
        @Override
        protected void onPostExecute(Bitmap bitmap){
            super.onPostExecute(bitmap);
            //Decreases the number of images being detected by1
            imagesDetecting--;
            //Progress spinner is removed
            fileLinearLayout.removeView(progressBar);
            //bitmap received is then shown in the preview
            addImageToPreview(bitmap, false, currentPicturePath);
            //Sets the finish report and save report to clickable
            finishReportButton.setClickable(true);
            saveReportButton.setClickable(true);
        }
    }

    private class populateSpinners extends  AsyncTask<Void, Void, Void>{

        ArrayAdapter<String> urgencySpinnerArrayAdapter, categorySpinnerArrayAdapter;

        @Override
        protected Void doInBackground(Void... params) {
           do{
               categorySpinnerArrayAdapter = new ArrayAdapter<String>(CreateReportActivity.this, android.R.layout.simple_spinner_item, HomeActivity.categoryArrayList);
               categorySpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            }while(HomeActivity.categoryArrayList.size() == 0);

            do{
                urgencySpinnerArrayAdapter = new ArrayAdapter<String>(CreateReportActivity.this, android.R.layout.simple_spinner_item, HomeActivity.urgencyArrayList);
                urgencySpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            }while(HomeActivity.urgencyArrayList.size() == 0);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            categorySpinner.setAdapter(categorySpinnerArrayAdapter);
            urgencySpinner.setAdapter(urgencySpinnerArrayAdapter);
            if(savedReport != null){
                for(int i = 0; i< HomeActivity.categoryArrayList.size(); i++){
                    Log.d(TAG, "populateFields: " + HomeActivity.categoryArrayList.get(i) + "==" + savedReport.getCategory());
                    if(Objects.equals(HomeActivity.categoryArrayList.get(i), savedReport.getCategory())){
                        categorySpinner.setSelection(i);
                    }
                }

                for(int i = 0; i< HomeActivity.urgencyArrayList.size(); i++){
                    Log.d(TAG, "populateFields: " + HomeActivity.urgencyArrayList.get(i) + "==" + savedReport.getUrgency());
                    if(Objects.equals(HomeActivity.urgencyArrayList.get(i), savedReport.getUrgency())){
                        urgencySpinner.setSelection(i);
                    }
                }
            }
        }
    }
}
