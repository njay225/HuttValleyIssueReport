package com.example.school.huttvalleyissuereport;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class UserInfo extends Fragment {

    //Variable for logging into the console
    private static final String TAG = "UserInfo";

    //Initialising TextViews
    private TextView userNameTextView, reportsMadeTextView, votesMadeTextView,
    mostPopularReportTextView, mostRecentReportTextView;

    //Initialising Buttons
    private Button editAccountButton;

    //Initialising Report Relative Layouts
    private RelativeLayout mostPopularReportRelativeLayout, mostRecentReportRelativeLayout;

    //Initialising Firebase Authentication
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks;

    //Initialising Views for Phone Verification Dialog
    private Button confirmButton, cancelButton;
    private EditText phoneAuthCodeEditText;

    //Initialising Firebase Database
    private FirebaseDatabase mDatabase;
    private DatabaseReference userNameRef;
    private DatabaseReference votesRef;

    //String to hold User ID
    private String currentUserID = UserViewActivity.currentUserID;

    //Initialising Array list to hold reports from Most Recent Fragment
    private ArrayList<Report> reports = new ArrayList<>();

    //Initialising Report Objects to hold most popular and most recent
    Report mostPopularReport;
    Report mostRecentReport;

    //Initialising LayoutInflater
    private LayoutInflater layoutInflater;

    //Initialising Progress Bar
    private ProgressBar progressBar;

    View rootView;

    public UserInfo() {
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
        layoutInflater = inflater;
        rootView = layoutInflater.inflate(R.layout.fragment_user_info, container, false);

        //Linking text views to widgets
        userNameTextView = (TextView) rootView.findViewById(R.id.userNameTextView);
        reportsMadeTextView = (TextView) rootView.findViewById(R.id.reportsMadeTextView);
        votesMadeTextView = (TextView) rootView.findViewById(R.id.votesMadeTextView);
        mostPopularReportTextView = (TextView) rootView.findViewById(R.id.mostPopularReportTextView);
        mostRecentReportTextView = (TextView) rootView.findViewById(R.id.mostRecentReportTextView);

        //Linking to Progress Bar
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        //Linking Buttons to widgets
        editAccountButton = (Button) rootView.findViewById(R.id.editAccountButton);

        editAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Sets the array of Options for editing account details
                final ArrayList<String> dialogOptionsArrayList = new ArrayList<>();
                //Adds option which all users can select - editing their username
                dialogOptionsArrayList.add("Change Username");

                Log.d(TAG, "onClick: " + mUser.getProviders().get(0));

                //Checks if the user has signed up via email and password
                if(Objects.equals(mUser.getProviders().get(0), "password")){
                    //If they have, then option to change email or password it given
                    dialogOptionsArrayList.add("Change Email");
                    dialogOptionsArrayList.add("Change Password");
                //If the user has signed up via their phone number
                }else if(Objects.equals(mUser.getProviders().get(0), "phone")){
                    //Then the option to change their phone number is given
                    dialogOptionsArrayList.add("Change Phone");
                }

                //The arraylist is converted to a standard array which can be passed to the dialog
                CharSequence[] dialogOptions = new CharSequence[dialogOptionsArrayList.size()];
                dialogOptions = dialogOptionsArrayList.toArray(dialogOptions);

                //Initialises a dialog
                new AlertDialog.Builder(getContext())
                        //Array is passed to dialog
                        .setItems(dialogOptions, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Switch statement checks which option was clicked
                                switch (which){
                                    //If the first option was clicked
                                    case 0:
                                        //changeUserName function is run
                                        changeUserName();
                                        break;
                                    //If second option was clicked
                                    case 1:
                                        //Checks if the user is signed in with their email and password
                                        if(Objects.equals(mUser.getProviders().get(0), "password")){
                                            //changeEmail function is run
                                            changeEmail();
                                        }else{
                                            //Otherwise changePhone function is run
                                            changePhone();
                                        }
                                        break;
                                    case 2:
                                        //The third option can only be to run the changePassword function
                                        changePassword();
                                        break;
                                }
                            }
                        }).show();
            }
        });

        //Linking Relative Layouts to Widgets
        mostPopularReportRelativeLayout = (RelativeLayout) rootView.findViewById(R.id.mostPopularReportRelativeLayout);
        mostRecentReportRelativeLayout = (RelativeLayout) rootView.findViewById(R.id.mostRecentReportRelativeLayout);

        //Setting up Firebase Authentication
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        //Setting up Firebase Database
        mDatabase = FirebaseDatabase.getInstance();
        userNameRef = mDatabase.getReference(getResources().getString(R.string.database_users_ref));
        votesRef = mDatabase.getReference(getResources().getString(R.string.database_user_votes_ref));


        //Getting reports from Most Recent fragment
        reports = MostRecent.reportsArray;

        //All reports are looped through
        for(int i = 0; i < reports.size(); i++){
            //Checks if the user created the report by comparing currentUserID to the report creator
            if(Objects.equals(reports.get(i).getCreator(), currentUserID)){
                //If the report belongs to the user
                //Checks if this is first iteration of loop
                if(mostPopularReport == null || mostRecentReport == null){
                    //If it is, then mostPopularReport and mostRecentReport are set to first report
                    //in the loop
                    mostPopularReport = reports.get(i);
                    mostRecentReport = reports.get(i);
                }

                //Checks if the mostPopularReport has fewer votes than that at index i
                if(mostPopularReport.getVotes() < reports.get(i).getVotes()){
                    //If it does, then the mostPopularReport is replaced
                    mostPopularReport = reports.get(i);
                }

                //Checks if the mostRecentReport was made earlier than the report at index i
                if(mostRecentReport.getDateAdded() <  reports.get(i).getDateAdded()){
                    //If it was, then the mostRecentReport is replaced
                    mostRecentReport = reports.get(i);
                }
            }
        }

        //Function to get the currentUser's user name is run
        getUserName();

        //Number of reports the user has made is retrieved from UserViewActivity and passed
        //To reportMadeTextView
        reportsMadeTextView.setText("Reports Made: " + UserViewActivity.reportCounter);

        //Background task is run to get user votes
        updateVotesTextView updateVotes = new updateVotesTextView();
        updateVotes.execute();

        //If the user has reports made and mostPopular and mostRecent reports are not empty
        if(mostRecentReport != null && mostPopularReport != null){
            //Then Functions are run to add the two reports to the UI
            addView(mostPopularReport, mostPopularReportRelativeLayout);
            addView(mostRecentReport, mostRecentReportRelativeLayout);
        }else{
            //If there are no reports then Most Popular and Most Recent titles are hidden
            mostPopularReportTextView.setVisibility(View.GONE);
            mostRecentReportTextView.setVisibility(View.GONE);
        }

        //Sets up phone number validation
        mCallBacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks(){

            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {

            }

            @Override
            public void onVerificationFailed(FirebaseException e) {

            }

            @Override
            public void onCodeSent(final String verificationId, PhoneAuthProvider.ForceResendingToken token){
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                //Creating an Alert Dialog so that the user can enter the verification code
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                //Adds a custom layout to the alert dialog
                View view = layoutInflater.inflate(R.layout.phone_auth_popup, null);
                builder.setView(view);
                final AlertDialog dialog = builder.create();
                dialog.show();

                //Gets Widgets from the custom view
                confirmButton = (Button) dialog.findViewById(R.id.confirmButton);
                phoneAuthCodeEditText = (EditText) dialog.findViewById(R.id.phoneAuthUserNameEditText);
                cancelButton = (Button) dialog.findViewById(R.id.cancelButton);


                //When confirm button is clicked, function is called to check verification code
                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, phoneAuthCodeEditText.getText().toString());
                        mUser.updatePhoneNumber(credential).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(getContext(), "Phone Number Updated", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                if(e instanceof FirebaseAuthInvalidCredentialsException){
                                    Toast.makeText(getContext(), "Code entered incorrectly", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });

                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }
        };

        return  rootView;

    }


    //Function gets the current user's username
    private void getUserName() {
        //If the current user is the logged in user
        if(Objects.equals(currentUserID, mUser.getUid())){
            //The user name is retrieved from the firebase user object and passed to the userNameTextView
            userNameTextView.setText(mUser.getDisplayName());
        }else{
            //Otherwise, the edit account button is hidden
            editAccountButton.setVisibility(View.GONE);

            //A single event listener is added to get username
            userNameRef.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //User name is retrieved, converted to a string and then sent to userNameTextView
                    userNameTextView.setText(dataSnapshot.getValue().toString());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }


    //This function changes the users username
    private void changeUserName(){
        final EditText userNameEditText =  new EditText(getContext());

        //New dialog is initialised
        new AlertDialog.Builder(getContext())
                //the relative layout is set as the dialog view
                .setView(userNameEditText)
                //The title is set of the dialog
                .setTitle("Enter your new Username")
                //The cancel button is set up for the dialog
                .setNegativeButton("Cancel", null)
                //The confirm button is set for the dialog
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //When the button is clicked
                        //the entered username is retrieved
                       final String newUserName = userNameEditText.getText().toString();

                        //Checks if the username is empty
                        if(Objects.equals(newUserName, "")){
                            //If so, a message is sent to the user informing them to enter a username
                            Toast.makeText(getContext(), "Please Enter a Username", Toast.LENGTH_SHORT).show();
                        }else{
                            //Other wise, progress spinner is shown
                            progressBar.setVisibility(View.VISIBLE);
                            //Users user name is updated with the new username
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(newUserName).build();

                            //Adds the username to the profile
                            mUser.updateProfile(profileUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //If username is added successfully, then we say that the username was changed
                                    Toast.makeText(getContext(), "UserName Changed Successfully", Toast.LENGTH_SHORT).show();
                                    //The database is updated
                                    userNameRef.child(mUser.getUid()).setValue(newUserName);
                                    //The progress spinner is hidden
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                        }
                    }
                }).show();
    }

    //Function to change the users password
    private void changePassword(){
        //Password Changing layout is retrieved
        View view = layoutInflater.inflate(R.layout.change_password_popup, null);

        //Edit Texts are Initialised and linked to variables
        final EditText oldPasswordEditText = (EditText) view.findViewById(R.id.oldPasswordEditText);
        final EditText newPasswordEditText = (EditText) view.findViewById(R.id.newPasswordEditText);
        final EditText newPasswordCheckEditText = (EditText) view.findViewById(R.id.newPasswordCheckEditText);

        //Alert Dialog is set up
        new AlertDialog.Builder(getContext())
                //View of dialog is set to password changing layout
                .setView(view)
                //Title is set
                .setTitle("Update Password")
                //Cancel button is set up
                .setNegativeButton("Cancel", null)
                //Confirm button is set up
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //When confirm button is clicked
                        //The text entered is retrieved
                        final String oldPassword = oldPasswordEditText.getText().toString();
                        final String newPassword = newPasswordEditText.getText().toString();
                        final String newPasswordCheck = newPasswordCheckEditText.getText().toString();

                        //If no old password was entered
                        if(Objects.equals(oldPassword, "")) {
                            //Message is sent to the user informing them to enter their old password
                            Toast.makeText(getContext(), "Please Enter Your Old Password", Toast.LENGTH_SHORT).show();
                        //If the user hasn't entered a new password
                        }else if(Objects.equals(newPassword, "")){
                            //Message is sent to the user informing them to enter their new password
                            Toast.makeText(getContext(), "Please Enter a New Password", Toast.LENGTH_SHORT).show();
                        //If the new passwords don't match
                        }else if(!Objects.equals(newPassword, newPasswordCheck)){
                            //Message is sent to the user informing them that the entered passwords aren't equal
                            Toast.makeText(getContext(), "Passwords are not the same", Toast.LENGTH_SHORT).show();
                        }else{
                            //Progress spinner is set to visible
                            progressBar.setVisibility(View.VISIBLE);
                            //Email credential is created
                            AuthCredential credential = EmailAuthProvider.getCredential(mUser.getEmail(), oldPassword);
                            //User is re logged into Firebase
                            mUser.reauthenticate(credential).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //If the user was logged in successfully
                                    //Their password is updated
                                    mUser.updatePassword(newPassword).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //If the password was updated successfully
                                            //Progress spinner is hidden
                                            progressBar.setVisibility(View.GONE);
                                            //User gets message to say that password was updated
                                            Toast.makeText(getContext(), "Password Updated", Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //Checks if the password was too weak
                                            if(e instanceof FirebaseAuthWeakPasswordException) {
                                                //Progress spinner is hidden
                                                progressBar.setVisibility(View.GONE);
                                                //User is informed about weak password
                                                Toast.makeText(getContext(), R.string.short_password, Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //If user had an incorrect password and couldn't be logged in
                                    if(e instanceof FirebaseAuthInvalidCredentialsException){
                                        //Progress spinner is hidden
                                        progressBar.setVisibility(View.GONE);
                                        //User is informed about incorrect password
                                        Toast.makeText(getContext(), R.string.incorrect_password, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }


                    }
                }).show();
    }

    //Function is used to change the users phone number
    private void changePhone(){
        final EditText phoneEditText =  new EditText(getContext());
        phoneEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

        //Alert Dialog is initialised
        new AlertDialog.Builder(getContext())
                //Title is set for dialog
                .setTitle("Enter New Phone Number")
                //View for dialog is set to relative layout
                .setView(phoneEditText)
                //Cancel button is set up
                .setNegativeButton("Cancel", null)
                //Enter button is set up
                .setPositiveButton("Enter", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //When its clicked
                        //The users phone number is retrieved
                        String phoneNumber = phoneEditText.getText().toString();
                        //If the phone number is empty
                        if(Objects.equals(phoneNumber, "")){
                            //Message is sent to user informing them to enter a phone number
                            Toast.makeText(getContext(), "Please Enter a Phone Number", Toast.LENGTH_SHORT).show();
                        }else{
                            //If phone is valid, then validation message is sent to the user
                            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                                    phoneNumber,
                                    getResources().getInteger(R.integer.phone_auth_timeout),
                                    TimeUnit.SECONDS,
                                    (Activity) getContext(),
                                    mCallBacks);
                        }
                    }
                }).show();
    }

    //Function changes the user's email address
    private void changeEmail(){
        final EditText emailEditText =  new EditText(getContext());
        emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        //Alert dialog is initialised
        new AlertDialog.Builder(getContext())
                //Relative layout is set as dialog view
                .setView(emailEditText)
                //Title is set
                .setTitle("Enter your new Email")
                //Cancel button is set up
                .setNegativeButton("Cancel", null)
                //Confirm Button is set up
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //When confirm button is clicked
                        //New Email is retrieved
                        final String newEmail = emailEditText.getText().toString();
                        //Checks if the email address is empty
                        if(Objects.equals(newEmail, "")) {
                            //If so, message is sent to user informing them to enter an email
                            Toast.makeText(getContext(), "Please Enter an Email", Toast.LENGTH_SHORT).show();
                        }else{
                            //Progress spinner is set to visible
                            progressBar.setVisibility(View.VISIBLE);
                            //Another dialog is initialised for user to confirm their email
                            new AlertDialog.Builder(getContext())
                                    //Title is set
                                    .setTitle("Is this Email Correct?")
                                    //Message is set as entered email
                                    .setMessage(newEmail)
                                    //No button is set up
                                    .setNegativeButton("No", null)
                                    //Yes button is set up
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            //Email update it then applied
                                            mUser.updateEmail(newEmail).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //If successful
                                                    //Progress spinner is hidden
                                                    progressBar.setVisibility(View.GONE);
                                                    //Message is sent to user informing them that email was changed
                                                    Toast.makeText(getContext(), "Email Changed Successfully", Toast.LENGTH_SHORT).show();
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    //If it failed
                                                    //Checks if it was because invalid email format
                                                    if(e instanceof FirebaseAuthInvalidCredentialsException) {
                                                        //Message is sent to user informing them that email isn't valid
                                                        Toast.makeText(getContext(), R.string.poor_email_format, Toast.LENGTH_LONG).show();
                                                    //Checks if the email is already in use
                                                    }else if(e instanceof FirebaseAuthUserCollisionException){
                                                        //Message is sent to user informing them that the email is already in use
                                                        Toast.makeText(getContext(), R.string.email_in_use, Toast.LENGTH_LONG).show();
                                                    }
                                                    //Progress Spinner is hidden
                                                    progressBar.setVisibility(View.GONE);
                                                }
                                            });

                                        }
                                    }).show();
                        }


                    }
                }).show();
    }

    //This function is for adding most popular and most recent report card
    private void addView(final Report reportCard, RelativeLayout relativeLayout){
        //Initialises a layout inflater
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //Gets the parent of the main relative layout
        ViewGroup parent = (ViewGroup) relativeLayout.getParent();
        //Gets the report card view
        View reportCardView = layoutInflater.inflate(R.layout.report_card, parent, false);

        //If the view for the report card is empty
        if(reportCardView == null) {
            //the view is set to the report card
            reportCardView = layoutInflater.inflate(R.layout.report_card, parent, false);
        }

        //A data format is initialised
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");

        //The date from the report class is then converted to the date format
        String reportDate = dateFormat.format(reportCard.getDateAdded());

        //Views within the report card layout are initialised
        TextView reportTitle = (TextView) reportCardView.findViewById(R.id.titleTextView);
        TextView address = (TextView) reportCardView.findViewById(R.id.locationTextView);
        TextView category = (TextView) reportCardView.findViewById(R.id.categoryTextView);
        //TextView description = (TextView) reportCardView.findViewById(R.id.descriptionTextView);
        TextView date = (TextView) reportCardView.findViewById(R.id.dateTextView);
        TextView votes = (TextView) reportCardView.findViewById(R.id.votesTextView);
        RelativeLayout card = (RelativeLayout) reportCardView.findViewById(R.id.reportCardRelativeLayout);
        ImageView voteIcon = (ImageView) reportCardView.findViewById(R.id.voteIconImageView);

        //Data from the report card class is added to the views within the report card
        reportTitle.setText(reportCard.title);
        address.setText(reportCard.address);
        category.setText(reportCard.category);
        //description.setText(reportCard.description);
        votes.setText(Integer.toString(reportCard.votes) + " Votes");
        date.setText(reportDate);

        //Checks if the number of votes is greater than 1000
        if(reportCard.votes > getContext().getResources().getInteger(R.integer.report_trending_limit)){
            //If it is then add a trending icon
            voteIcon.setImageResource(R.drawable.report_trending_up);
        }else{
            //If not, then add a flat line icon
            voteIcon.setImageResource(R.drawable.report_trending_flat);
        }

        Log.d(TAG, "getView: " + reportCard.getUrgency());

        //Checks if the urgency of the report is "Very Urgent"
        if(Objects.equals(reportCard.getUrgency(), getContext().getResources().getString(R.string.very_urgent_urgency))){
            //The background colour of the report card is changed
            card.setBackgroundResource(R.drawable.report);
            reportTitle.setTextColor(getContext().getResources().getColor(R.color.veryUrgent));
            address.setTextColor(getContext().getResources().getColor(R.color.veryUrgent));
            category.setTextColor(getContext().getResources().getColor(R.color.veryUrgent));
            date.setTextColor(getContext().getResources().getColor(R.color.veryUrgent));
            date.setTextColor(getContext().getResources().getColor(R.color.veryUrgent));
            votes.setTextColor(getContext().getResources().getColor(R.color.veryUrgent));
            voteIcon.setColorFilter(getContext().getResources().getColor(R.color.veryUrgent));
        }else{
            //Other wise the standard colour is used
            Log.d(TAG, "getView: Report is not very urgent" + reportCard.getUrgency());
            card.setBackgroundResource(R.drawable.report);
            reportTitle.setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
            address.setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
            category.setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
            date.setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
            date.setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
            votes.setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
            voteIcon.setColorFilter(getContext().getResources().getColor(R.color.colorPrimary));
        }

        //An onclick listener is added to the report card
        reportCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //The public clicked report is set to the clicked report
                TaskAdapter.clickedReport = reportCard;
                //The reportview activity is then opened
                Intent intent = new Intent(getContext(), ReportViewActivity.class);
                getContext().startActivity(intent);
            }
        });

        //The report card is then added to the passed layout
        relativeLayout.addView(reportCardView);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause(){
        super.onPause();
    }


    //Background task used to update the votes textview
    private class updateVotesTextView extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... params) {
            //Keeps checking if the votes have been retrieved in the UserViewActivity
            while(!UserViewActivity.votesRetrieved){
                Log.d(TAG, "doInBackground: " + UserViewActivity.votesRetrieved);
                Log.d(TAG, "doInBackground: votes not retrieved");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid){
            super.onPostExecute(aVoid);
            //Once the votes have been retrieved, they are passed to the votesMadeTextView
            votesMadeTextView.setText("Votes Made: " + UserViewActivity.voteCounter);
        }
    }

}
