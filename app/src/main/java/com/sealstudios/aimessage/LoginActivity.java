package com.sealstudios.aimessage;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.rilixtech.materialfancybutton.MaterialFancyButton;
import com.sealstudios.aimessage.Utils.Constants;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private EditText phoneNum, Code;// two edit text one for enter phone number other for enter OTP code
    private MaterialFancyButton sent_;// sent button to request for verification and verify is for to verify code
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth mAuth;
    //TODO change country code to a value from a spinner
    private String countryCode = "+44";
    private String mVerificationId;
    private String MY_TAG = "LOGIN ACTIVITY";
    public static boolean mVerificationInProgress;
    private FrameLayout container;
    private android.support.v4.app.Fragment verifyfragment;
    private android.support.v4.app.Fragment introfragment;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (mVerificationInProgress && validatePhoneNumber()) {
            //maybe get this from the saved instance state rather than the text box
            String num = phoneNum.getText().toString();
            String qualifiedNumber = countryCode + num.replaceAll("\\s", "").trim();
            startPhoneNumberVerification(qualifiedNumber);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EmojiManager.install(new GoogleEmojiProvider());
        setContentView(R.layout.activity_login);
        ImageView imageView = findViewById(R.id.background);
        Glide.with(this)
                .load(R.drawable.message_background)
                .apply(new RequestOptions()
                        .centerCrop())
                .into(imageView);

        phoneNum = findViewById(R.id.field_phone_number);
        sent_ = findViewById(R.id.button_start_verification);
        container = findViewById(R.id.container);
        verifyfragment = new VerifyFrag();
        introfragment = new IntroFrag();
        mAuth = FirebaseAuth.getInstance();
        callback_verification();               ///function initialization
        sent_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String num = phoneNum.getText().toString();
                if (validatePhoneNumber()){
                    //get country code from spinner eventually
                    countryCode = "+44";
                    String qualifiedNumber = countryCode + num.replaceAll("\\s", "").trim();
                    startPhoneNumberVerification(qualifiedNumber);
                }
                // call function for receive OTP 6 digit code
            }
        });
    }



    private void startPhoneNumberVerification(String phoneNumber) {

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
        mVerificationInProgress = true;
    }

    public void resendVerificationCode(String phoneNumber, PhoneAuthProvider.ForceResendingToken token){
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks,
                token);
        mVerificationInProgress = true;
    }

    private void callback_verification() {

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verificaiton without
                //     user action.
                mVerificationInProgress = false;
                signInWithPhoneAuthCredential(credential);
            }
            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }
                mVerificationInProgress = false;
                // Show a message and update the UI
            }
            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                // Save verification ID and resending token so we can use them later

                //maybe store a boolean that tells a progress bar in
                //verify frag that it should be loading in case we go past this and straight to intro frag
                mVerificationId = verificationId;
                mResendToken = token;
                Bundle b = new Bundle();
                b.putString(Constants.VERIFICATION,verificationId);
                b.putString(Constants.NUMBER, countryCode + phoneNum.getText().toString());
                b.putParcelable(Constants.TOKEN,mResendToken);
                switchFragment(verifyfragment,b);
                //maybe add some transition animation here
            }
        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information

                            FirebaseUser user = task.getResult().getUser();
                            //Toast.makeText(getApplicationContext(), "sign in successfull", Toast.LENGTH_SHORT).show();
                            Fragment fragment = new IntroFrag();
                            Bundle b = new Bundle();
                            b.putString(Constants.NUMBER,countryCode + phoneNum.getText().toString());
                            b.putString(Constants.USER_ID,user.getUid());
                            switchFragment(fragment,b);
                        } else {
                            // Sign in failed, display a message and update the UI

                            Snackbar.make(container,LoginActivity.this.getString(R.string.failed_creating_profile),Snackbar.LENGTH_LONG).show();

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Snackbar.make(container,LoginActivity.this.getString(R.string.invalid_code),Snackbar.LENGTH_LONG).show();
                            }
                        }
                        LoginActivity.mVerificationInProgress = false;
                    }
                });
    }

    private boolean validatePhoneNumber() {
        String phoneNumber = phoneNum.getText().toString();
        //check country code
        if (TextUtils.isEmpty(phoneNumber)) {
            phoneNum.setError(
                    this.getResources().getString(R.string.invalid_number));
            return false;
        }else if (phoneNumber.contains("+44")) {
            phoneNum.setError(this.getResources().getString(R.string.invalid_number_44));
            return false;
        }else if (phoneNumber.charAt(0) == '0') {
            phoneNum.setError(this.getResources().getString(R.string.invalid_number_0));
            return false;
        }

        return true;
    }

    public void switchFragment(Fragment fragment, Bundle bundle) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        fragment.setArguments(bundle);
        //fragmentManager.addOnBackStackChangedListener((FragmentManager.OnBackStackChangedListener) this);
        fragmentTransaction.replace(R.id.container, fragment /*,"h"*/);
        //fragmentTransaction.addToBackStack("h");
        fragmentTransaction.commit();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment f : fragments) {
                if (f instanceof IntroFrag) {
                    f.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
            outState.putBoolean("PROGRESS",mVerificationInProgress);
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mVerificationInProgress = savedInstanceState.getBoolean("PROGRESS");
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment f : fragments) {
                if (f instanceof IntroFrag) {
                    ((IntroFrag) f).clearViews();
                }
            }
        }
    }
}