package com.example.school.huttvalleyissuereport;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SignUpActivity extends AppCompatActivity {

    //Variable or logging into the console
    private final static String TAG = "SignUpActivity";

    //Initialising Edit Text Variables
    private EditText userNameEditText, emailPhoneEditText, passwordEditText, reenterPasswordEditText;

    //Initialising String Variables to hold EditText Values
    private String userNameText, emailPhoneText, passwordText, reenterPasswordText;

    //Initialising Helper Text View Variables
    private TextView userNameHelperTextView, emailPhoneHelperTextView, passwordHelperTextView, reenterPasswordHelperTextView;

    //Initialising the Email Phone title text view
    private TextView emailPhoneTextView;

    //Initialising Relative Layout for Password Widgets
    private RelativeLayout passwordRelativeLayout;

    //Initialising Buttons
    private Button signUpButton;

    //Initialising Views for Phone Verification Dialog
    private Button confirmButton, cancelButton;
    private EditText phoneAuthCodeEditText;

    //Initialising Text View Button
    private TextView alreadyMemberTextView;

    //Boolean Variable to check if user is signing up with Email or Phone
    private Boolean signingUpWithPhone = false;

    //Initialising Firebase Authentication
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks;

    //Initialising Firebase Database
    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef;

    //Initialising Progress Spinner
    private ProgressBar progressSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        getSupportActionBar().hide();

        //Linking Edit Text Variables to Edit Text Widgets
        userNameEditText = (EditText) findViewById(R.id.userNameEditText);
        emailPhoneEditText = (EditText) findViewById(R.id.emailPhoneEditText);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        reenterPasswordEditText = (EditText) findViewById(R.id.reenterPasswordEditText);

        //Linking Helper Text Views to Text View Widgets
        userNameHelperTextView = (TextView) findViewById(R.id.userNameHelperTextView);
        emailPhoneHelperTextView = (TextView) findViewById(R.id.emailPhoneHelperTextView);
        passwordHelperTextView = (TextView) findViewById(R.id.passwordHelperTextView);
        reenterPasswordHelperTextView = (TextView) findViewById(R.id.reenterPasswordHelperTextView);

        //Linking Title Text View to Text View Widget
        emailPhoneTextView = (TextView) findViewById(R.id.emailPhoneTextView);

        //Linking progress spinner to widget
        progressSpinner = (ProgressBar) findViewById(R.id.progressBar);

        //Setting up Already Member Button
        alreadyMemberTextView = (TextView) findViewById(R.id.alreadyMemberTextView);
        alreadyMemberTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Open Login Page
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        //Finding Password Relative Layout
        passwordRelativeLayout = (RelativeLayout) findViewById(R.id.passwordRelativeLayout);

        //Checking if user is signing up with Email or Phone
        emailPhoneEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                //Clears Helper Text View as it won't be valid for new input
                emailPhoneHelperTextView.setText("");
                //Creates Array to hold each character in emailPhoneEditText
                char[] characterArray = s.toString().toCharArray();
                Boolean letterFound = false;
                //Goes through each character to check if its a letter
                for (char character : characterArray) {
                    //Checks if character is not a number
                    if(!Character.isDigit(character)){
                        letterFound = true;
                    }
                }

                //Layout Parameter which will be applied to the sign up button to change its position depending whether or not user is signing up with email or phone
                RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) signUpButton.getLayoutParams();
                if(letterFound){
                    //If a letter has been found, then the user cannot be signing up with a phone number
                    signingUpWithPhone = false;
                    //Setting emailPhoneTextView text to display "Email"
                    emailPhoneTextView.setText(R.string.email_phone_edit_text_email_input);
                    //email users require the password widgets so they are made visible
                    passwordRelativeLayout.setVisibility(View.VISIBLE);
                    //the sign up button is moved below the password widgets
                    p.addRule(RelativeLayout.BELOW, R.id.passwordRelativeLayout);
                    signUpButton.setLayoutParams(p);
                }else{
                    //is a letter has not been found then the user would be signing up with their phone number
                    signingUpWithPhone = true;
                    //Setting emailPhoneTextView text to display "Phone"
                    emailPhoneTextView.setText(R.string.email_phone_edit_text_phone_input);
                    //phone users don't need to enter a password so the password widgets are hidden
                    passwordRelativeLayout.setVisibility(View.INVISIBLE);
                    //Clears the password edit texts
                    passwordEditText.setText("");
                    reenterPasswordEditText.setText("");
                    //the signup button is moved below the emailPhoneHelperTextView
                    p.addRule(RelativeLayout.BELOW, R.id.emailPhoneHelperTextView);
                    signUpButton.setLayoutParams(p);
                }

                if(Objects.equals(s.toString(), "")){
                    emailPhoneTextView.setText(R.string.email_phone_edit_text_default);
                }
            }
        });

        //Setting up Sign Up Button
        signUpButton = (Button) findViewById(R.id.signUpButton);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Gets the text from the edit texts
                userNameText = userNameEditText.getText().toString();
                emailPhoneText = emailPhoneEditText.getText().toString();
                passwordText = passwordEditText.getText().toString();
                reenterPasswordText = reenterPasswordEditText.getText().toString();

                //Error Checking

                //Checks is the username is null
                if(Objects.equals(userNameText, "")){
                    //Gives the user an error
                    userNameHelperTextView.setText(R.string.null_username);
                //Checks if the passwords provided are equal
                }else if(!Objects.equals(passwordText, reenterPasswordText)){
                    //Gives the user user an error
                    reenterPasswordHelperTextView.setText(R.string.passwords_not_equal);
                //Checks if user has entered an email or phone number
                }else if(Objects.equals(emailPhoneText, "")){
                    //Gives the user an error
                    emailPhoneHelperTextView.setText(R.string.null_emailPhone);
                //Checks if user has entered a password given that they are signing up with an Email
                }else if(Objects.equals(passwordText, "") && !signingUpWithPhone){
                    //Gives the user an error
                    passwordHelperTextView.setText(R.string.null_password);
                }else{
                    progressSpinner.setVisibility(View.VISIBLE);
                    signUpButton.setClickable(false);
                    //Calls function to sign user up
                    signUpUser(signingUpWithPhone);
                }
            }
        });

        //Sets up Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        //Sets up phone number validation
        mCallBacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks(){

            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signUserUpWithPhone(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                signUpButton.setClickable(true);
                if(e instanceof FirebaseTooManyRequestsException){
                    emailPhoneHelperTextView.setText(R.string.too_many_phone_requests);
                }else if(e instanceof FirebaseAuthInvalidCredentialsException){
                    emailPhoneHelperTextView.setText(R.string.invalid_phone_number);
                }else{
                    Log.d(TAG, "onVerificationFailed: " + e.getMessage());
                }
            }

            @Override
            public void onCodeSent(final String verificationId, PhoneAuthProvider.ForceResendingToken token){
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                //Creating an Alert Dialog so that the user can enter the verification code
                AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);

                //Adds a custom layout to the alert dialog
                View view = getLayoutInflater().inflate(R.layout.phone_auth_popup, null);
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
                        signUserUpWithPhone(credential);
                    }
                });

                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

            }

            @Override
            public void onCodeAutoRetrievalTimeOut(String verificationId){
                signUpButton.setClickable(true);
                cancelButton.setVisibility(View.VISIBLE);
                progressSpinner.setVisibility(View.GONE);
                Toast.makeText(SignUpActivity.this, "Code has timed out", Toast.LENGTH_SHORT).show();
                emailPhoneHelperTextView.setText(R.string.phone_auth_timed_out);
            }
        };

        //Setting up Database Connection
        mDatabase = FirebaseDatabase.getInstance();
        userRef = mDatabase.getReference(getResources().getString(R.string.database_users_ref));

    }

    private void signUserUpWithPhone(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                //User has entered correct code
                FirebaseUser user = authResult.getUser();
                addUserName(user);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                signUpButton.setClickable(true);
                progressSpinner.setVisibility(View.GONE);
                Log.d(TAG, e.getMessage());
                if(e instanceof FirebaseAuthInvalidCredentialsException){
                    Toast.makeText(SignUpActivity.this, "Code entered incorrectly", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addUserName(final FirebaseUser user) {
        //Setting up the builder to add the userName
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(userNameText).build();

        //Adds the username to the profile
        user.updateProfile(profileUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //If username is added successfully, then we say that the account was created
                Toast.makeText(SignUpActivity.this, "Account Created Successfully", Toast.LENGTH_SHORT).show();
                addUserToDatabase(user);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                signUpButton.setClickable(true);
                progressSpinner.setVisibility(View.GONE);
            }
        });
    }

    private void addUserToDatabase(FirebaseUser user){
        progressSpinner.setVisibility(View.GONE);
        //Adds username to database
        userRef.child(user.getUid()).setValue(user.getDisplayName());
        //Opens Home Activity
        Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
        startActivity(intent);
    }

    private void signUpUser(Boolean signingUpWithPhone) {
        //Clear Helper Text Views
        userNameHelperTextView.setText("");
        emailPhoneHelperTextView.setText("");
        reenterPasswordHelperTextView.setText("");
        passwordHelperTextView.setText("");

        //Checks if the user is signing up with email or phone
        if(!signingUpWithPhone){
            //Creates an account using the provided email and password
            mAuth.createUserWithEmailAndPassword(emailPhoneText, passwordText)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            //If successful the username is then added to the user account
                            FirebaseUser user = mAuth.getCurrentUser();
                            addUserName(user);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            signUpButton.setClickable(true);
                            progressSpinner.setVisibility(View.GONE);
                            //If the account creation failed, then we fill the helper text accordingly
                            Log.d(TAG, e.toString());

                            if(e instanceof FirebaseAuthWeakPasswordException){
                                passwordHelperTextView.setText(R.string.short_password);
                            }else if(e instanceof FirebaseAuthInvalidCredentialsException) {
                                emailPhoneHelperTextView.setText(R.string.poor_email_format);
                            }else if(e instanceof FirebaseAuthUserCollisionException){
                                emailPhoneHelperTextView.setText(R.string.email_in_use);
                            }
                        }
                     });
        }else{
            //User is signing up with their phone number
            //Makes sure the phone number is valid by sending a validation code via text which times out after 60s
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    emailPhoneText,
                    getResources().getInteger(R.integer.phone_auth_timeout),
                    TimeUnit.SECONDS,
                    SignUpActivity.this,
                    mCallBacks);

        }

    }
}
