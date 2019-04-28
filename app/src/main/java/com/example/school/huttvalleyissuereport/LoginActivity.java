package com.example.school.huttvalleyissuereport;

import android.content.DialogInterface;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
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

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    //Variable for Logging into the Console
    private static final String TAG = "LoginActivity";

    //Initialising Edit Text Variables
    private EditText emailPhoneEditText, passwordEditText;

    //Initialising String variables to hold Edit Text Values
    private String emailPhoneText, passwordText;

    //Initialising Helper Text View Variables
    private TextView emailPhoneHelperTextView, passwordHelperTextView;

    //Initialising Buttons
    private Button loginButton, signUpButton;

    //Initialising the Email Phone title text view
    private TextView emailPhoneTextView;

    //Initialising Relative Layout for Password Widgets
    private RelativeLayout passwordRelativeLayout;

    //Boolean variable to check if the user is logging in with Email or Phone
    private Boolean loggingInWithPhone = false;

    //Initialising Text View Links
    private TextView forgotPasswordTextView, contactTextView;

    //Initialising Google Login Button
    private SignInButton googleLoginButton;
    GoogleApiClient mGoogleApiClient;

    //Constant used for the request code for Google Login
    private static final int RC_SIGN_IN = 9001;

    //Initialising Facebook Login Button
    private LoginButton facebookLoginButton;
    private CallbackManager mCallBackManager;

    //Initialising Firebase Authentication
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks;

    //Initialising Views for Phone Verification Dialog
    private Button confirmButton, cancelButton;
    private EditText phoneAuthCodeEditText;

    //Initialising Views for Phone Username Input Dialog
    private Button confirmUserNameButton;
    private EditText phoneAuthUserNameEditText;

    //Initialising Firebase Database
    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef;

    //Initialising Progress Spinner
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();
        //noinspection deprecation
        FacebookSdk.sdkInitialize(getApplicationContext());

        //Connecting to Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        //Checking if user is already logged in
        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            //If a user is logged in then home page is opened
            openHomeActivity();
        }

        //Setting up Facebook Login
        //Logs out user if previous login had been through Facebook
        LoginManager.getInstance().logOut();
        mCallBackManager = new CallbackManager.Factory().create();
        facebookLoginButton = (LoginButton) findViewById(R.id.facebookLoginButton);
        facebookLoginButton.setReadPermissions("email", "public_profile");

        //Checks whether or not user has successfully logged in via Facebook
        facebookLoginButton.registerCallback(mCallBackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel(){}

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, error.toString());
            }
        });

        //Setting up Google Sign Up
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_sign_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        //When the Google Login Button is clicked
        googleLoginButton = (SignInButton) findViewById(R.id.googleLoginButton);
        googleLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleLogin();
            }
        });

        //Setting up Sign Up Button
        signUpButton = (Button) findViewById(R.id.signUpButton);
        //When Sign Up Button is clicked
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Open Sign Up Page
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        //Linking up Edit Text variables to widgets
        emailPhoneEditText = (EditText) findViewById(R.id.emailPhoneEditText);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);

        //Linking up Helper Text Views to Widgets
        emailPhoneHelperTextView = (TextView) findViewById(R.id.emailPhoneHelperTextView);
        passwordHelperTextView = (TextView) findViewById(R.id.passwordHelperTextView);

        //Linking up Email Phone Text View title to widget
        emailPhoneTextView = (TextView) findViewById(R.id.emailPhoneTextView);

        //Linking up password Relative Layout to Widget
        passwordRelativeLayout = (RelativeLayout) findViewById(R.id.passwordRelativeLayout);

        //Linking Login Button to widget
        loginButton = (Button) findViewById(R.id.loginButton);

        emailPhoneEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

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
                RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) loginButton.getLayoutParams();
                if(letterFound){
                    //If a letter has been found, then the user cannot be signing up with a phone number
                    loggingInWithPhone = false;
                    //Setting emailPhoneTextView text to display "Email"
                    emailPhoneTextView.setText(R.string.email_phone_edit_text_email_input);
                    //email users require the password widgets so they are made visible
                    passwordRelativeLayout.setVisibility(View.VISIBLE);
                    //the sign up button is moved below the password widgets
                    p.addRule(RelativeLayout.BELOW, R.id.passwordRelativeLayout);
                    loginButton.setLayoutParams(p);
                }else{
                    //is a letter has not been found then the user would be signing up with their phone number
                    loggingInWithPhone = true;
                    //Setting emailPhoneTextView text to display "Phone"
                    emailPhoneTextView.setText(R.string.email_phone_edit_text_phone_input);
                    //phone users don't need to enter a password so the password widgets are hidden
                    passwordRelativeLayout.setVisibility(View.INVISIBLE);
                    //Clears the password edit texts
                    passwordEditText.setText("");
                    //the signup button is moved below the emailPhoneHelperTextView
                    p.addRule(RelativeLayout.BELOW, R.id.emailPhoneHelperTextView);
                    loginButton.setLayoutParams(p);
                }

                if(Objects.equals(s.toString(), "")){
                    emailPhoneTextView.setText(R.string.email_phone_edit_text_default);
                }
            }
            }
        );

        //Linking Forgot Password TextView to Widget
        forgotPasswordTextView = (TextView) findViewById(R.id.forgotPasswordTextView);

        //Adding onclick listener to forgot password textview
        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Initialising Email Edit Text
                final EditText resetEmailEditText = new EditText(LoginActivity.this);
                resetEmailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

                new AlertDialog.Builder(LoginActivity.this)
                        .setView(resetEmailEditText)
                        .setTitle("Please Enter Your Email Address")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String resetEmail = resetEmailEditText.getText().toString();
                                if(resetEmail == ""){
                                    Toast.makeText(LoginActivity.this, "Please enter a reset email", Toast.LENGTH_SHORT).show();
                                }else{
                                    mAuth.sendPasswordResetEmail(resetEmail).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(LoginActivity.this, "Email Sent", Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            if(e instanceof FirebaseAuthInvalidUserException){
                                                Toast.makeText(LoginActivity.this, R.string.incorrect_email, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                            }
                        }).show();
            }
        });

        //Attempt to log user in when login button is clicked
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Gets the text from the edit texts
                emailPhoneText = emailPhoneEditText.getText().toString();
                passwordText = passwordEditText.getText().toString();

                //Error Checking
                    //Checks if user has entered an email or phone number
                if(Objects.equals(emailPhoneText, "")){
                    //Gives the user an error
                    emailPhoneHelperTextView.setText(R.string.null_emailPhone);
                    //Checks if user has entered a password given that they are signing up with an Email
                }else if(Objects.equals(passwordText, "") && !loggingInWithPhone){
                    //Gives the user an error
                    passwordHelperTextView.setText(R.string.null_password);
                }else{
                    //Calls function to sign user up
                    loginUser();
                }
            }
        });

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
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);

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
                        logUserInWithPhone(credential);
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

        //Settings up Firebase Database
        mDatabase = FirebaseDatabase.getInstance();
        userRef = mDatabase.getReference(getResources().getString(R.string.database_users_ref));

        //Linking Progress Spinner to Widget
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private void logUserInWithPhone(PhoneAuthCredential credential) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithCredential(credential).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                progressBar.setVisibility(View.GONE);
                //User has entered correct code
                final FirebaseUser user = authResult.getUser();
                if(user.getDisplayName() == null){
                    //Creating an Alert Dialog so that the user can enter a Username
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);

                    //Adds a custom layout to the alert dialog
                    View view = getLayoutInflater().inflate(R.layout.phone_auth_add_username, null);
                    builder.setView(view);
                    final AlertDialog dialog = builder.create();
                    dialog.show();

                    //Gets Widgets from the custom view
                    confirmUserNameButton = (Button) dialog.findViewById(R.id.confirmButton);
                    phoneAuthUserNameEditText = (EditText) dialog.findViewById(R.id.phoneAuthUserNameEditText);

                    confirmUserNameButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final String userName = phoneAuthUserNameEditText.getText().toString();

                            if(Objects.equals(userName, "")){
                                Toast.makeText(LoginActivity.this, "Please enter a Username", Toast.LENGTH_SHORT).show();
                            }else{
                                //Setting up the builder to add the userName
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(userName).build();

                                //Adds the username to the profile
                                user.updateProfile(profileUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //If username is added successfully, then we say that the account was created
                                        Toast.makeText(LoginActivity.this, "Account Created Successfully", Toast.LENGTH_SHORT).show();
                                        addUserToDatabase(user);
                                    }
                                });
                            }



                        }
                    });
                }else{
                    //Take them to homepage
                    openHomeActivity();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.GONE);
                if(e instanceof FirebaseAuthInvalidCredentialsException){
                    Toast.makeText(LoginActivity.this, "Code entered incorrectly", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openHomeActivity(){
        //Creates intent to go to the home activity
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        //closes the current intent so user cannot navigate back without logging out
        finish();
    }
    private void addUserToDatabase(FirebaseUser user) {
        userRef.child(user.getUid()).setValue(user.getDisplayName());
        openHomeActivity();
    }


    private void loginUser() {
        //Clear Helper Text Views
        emailPhoneHelperTextView.setText("");
        passwordHelperTextView.setText("");

        if(!loggingInWithPhone){
            progressBar.setVisibility(View.VISIBLE);
            //Log user in with their email and password
            mAuth.signInWithEmailAndPassword(emailPhoneText, passwordText)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this, "Logged in", Toast.LENGTH_SHORT).show();
                            //Add user to Database
                            openHomeActivity();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressBar.setVisibility(View.GONE);
                    if(e instanceof FirebaseAuthInvalidCredentialsException){
                        passwordHelperTextView.setText(R.string.incorrect_password);
                    }else if(e instanceof FirebaseAuthInvalidUserException){
                        emailPhoneHelperTextView.setText(R.string.incorrect_email);
                    }
                }
            });
        }else{
            //User in logging in with their phone numebr
            //Makes sure the phone number is valid by sending a validation code via text which times out after 60s
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    emailPhoneText,
                    getResources().getInteger(R.integer.phone_auth_timeout),
                    TimeUnit.SECONDS,
                    LoginActivity.this,
                    mCallBacks);
        }

    }

    private void handleFacebookAccessToken(AccessToken accessToken) {
        progressBar.setVisibility(View.VISIBLE);
        //Gets the Login Credential returned from the user Login
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        //Logs the user into Firebase and adds a listener to check when process is complete
        mAuth.signInWithCredential(credential).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                //Add user to Database
                addUserToDatabase(authResult.getUser());
                Toast.makeText(LoginActivity.this, "Logged In", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, e.toString());
                if(e instanceof FirebaseAuthUserCollisionException){
                    Toast.makeText(LoginActivity.this, "Email is already in use with another provider", Toast.LENGTH_LONG).show();
                }
                LoginManager.getInstance().logOut();
            }
        });
    }

    private void googleLogin(){
        progressBar.setVisibility(View.VISIBLE);
        //Attempts to log the user into Google
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        //Checks if user logged in through the app
        if(requestCode == RC_SIGN_IN){
            //Checks if the user has signed into Google
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if(result.isSuccess()){
                progressBar.setVisibility(View.GONE);
                //Get their account details and try to sign them into firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            }else{
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, result.getStatus().toString());
            }
        }else{
            mCallBackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount account) {
        //Gets the credential returned when the user logged in
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        //Signs the user into firebase and checks when the process is complete
        mAuth.signInWithCredential(credential).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                progressBar.setVisibility(View.GONE);
                //Add user to Database
                addUserToDatabase(authResult.getUser());

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, e.toString());
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Connection Failed");
    }


}
