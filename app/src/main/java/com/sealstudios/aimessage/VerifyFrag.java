package com.sealstudios.aimessage;



import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.rilixtech.materialfancybutton.MaterialFancyButton;
import com.sealstudios.aimessage.Utils.Constants;

public class VerifyFrag extends Fragment {

    private EditText Code;// two edit text one for enter phone number other for enter OTP code
    private Button resend;// sent button to request for verification and verify is for to verify code
    private MaterialFancyButton Verify;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private PhoneAuthProvider.ForceResendingToken token;
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private String number;

    public VerifyFrag() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_verify, container, false);
        Code = rootView.findViewById(R.id.field_verification_code);
        ImageView imageView = rootView.findViewById(R.id.background);
        Glide.with(this)
                .load(R.drawable.message_background)
                .apply(new RequestOptions()
                        .centerCrop())
                .into(imageView);
        Verify = rootView.findViewById(R.id.button_verify_phone);
        Bundle b = new Bundle();
        if (getArguments() != null){
            b = getArguments();
            mVerificationId = b.getString(Constants.VERIFICATION);
            number = b.getString(Constants.NUMBER);
            token = b.getParcelable(Constants.TOKEN);
        }
        mAuth = FirebaseAuth.getInstance();
        Verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code=Code.getText().toString();
                verifyPhoneNumberWithCode(mVerificationId ,code);            //call function for verify code
            }
        });
        resend = rootView.findViewById(R.id.btn_reset_password);
        resend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((LoginActivity)getActivity()).resendVerificationCode(number,token);
            }
        });
        return rootView;

    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information

                            FirebaseUser user = task.getResult().getUser();
                            //Toast.makeText(getApplicationContext(), "sign in successfull", Toast.LENGTH_SHORT).show();
                            Snackbar.make(Code,R.string.sign_in_success,Snackbar.LENGTH_LONG).show();
                            Fragment fragment = new IntroFrag();
                            Bundle b = new Bundle();
                            b.putString(Constants.NUMBER,number);
                            b.putString(Constants.VERIFICATION , mVerificationId);
                            b.putString(Constants.USER_ID , user.getUid());
                            ((LoginActivity)getActivity()).switchFragment(fragment,b);
                        } else {
                            // Sign in failed, display a message and update the UI

                            Snackbar.make(Code,R.string.sign_in_failed,Snackbar.LENGTH_LONG).show();

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Snackbar.make(Code,R.string.invalid_code,Snackbar.LENGTH_LONG).show();
                            }
                        }
                        LoginActivity.mVerificationInProgress = false;
                    }
                });
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }



}
