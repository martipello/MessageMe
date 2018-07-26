package com.sealstudios.aimessage;


import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.myhexaville.smartimagepicker.ImagePicker;
import com.rilixtech.materialfancybutton.MaterialFancyButton;
import com.sealstudios.aimessage.Database.DatabaseUser;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.ViewModels.UserViewModel;
import com.vanniktech.emoji.EmojiEditText;

import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class IntroFrag extends Fragment {
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int LOADER = 100;
    private CircleImageView icon;
    private EmojiEditText profileName, profileStatus;
    private MaterialFancyButton getStarted;
    private ImageView imageBackground;
    private DatabaseUser user = new DatabaseUser();
    private ImageView camIcon;
    private ImagePicker imagePicker;
    private FirebaseFirestore db;
    private String stringRef = Constants.STORAGE_REF;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private String phoneNumber;
    private boolean existingUser = false;
    private MaterialDialog materialDialogBuilder;
    private MaterialDialog materialDialog2Builder;
    private String materialDialogBuilderMessage2;
    private String materialDialogBuilderMessage;
    private String userId;
    private String token;
    private Context mContext;
    private boolean hasUploaded = false;
    private boolean hasChecked = false;
    private UserViewModel userViewModel;

    public IntroFrag() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            //Restore the fragment's state here
            user = savedInstanceState.getParcelable(Constants.FS_ID);
            populateViews();
        }
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
        View rootView = inflater.inflate(R.layout.intro_frag, container, false);
        db = FirebaseFirestore.getInstance();
        mContext = getActivity();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReferenceFromUrl(stringRef);
        Bundle b;
        if (getArguments() != null) {
            b = getArguments();
            phoneNumber = b.getString(Constants.NUMBER);
            userId = b.getString(Constants.USER_ID);
            user.setUser_id(userId);
            user.setUser_number(phoneNumber);
        }
        profileName = rootView.findViewById(R.id.field_phone_number);
        profileStatus = rootView.findViewById(R.id.status_text);
        icon = rootView.findViewById(R.id.icon);
        camIcon = rootView.findViewById(R.id.camera_icon);
        imageBackground = rootView.findViewById(R.id.backgroundImageView);
        Glide.with(getActivity().getApplicationContext())
                .load(R.drawable.message_background)
                .apply(new RequestOptions()
                        .centerCrop())
                .into(imageBackground);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshImagePicker(icon);
                imagePicker.choosePicture(true /*show camera intents*/);
            }
        });
        getStarted = rootView.findViewById(R.id.button_start_verification);
        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        if (!
                hasChecked) {
            checkProfileExists(userId);
        } else {
            populateViews();
        }
        return rootView;
    }

    //ORDER OF CALLS
    //check profile exists ? get profile : inform that profile isnt there
    //get started onclick
    //validate data
    //uploadimage
    //save user
    //move to next activity
    public void clearViews() {
        profileName.getText().clear();
        profileStatus.getText().clear();
        icon.setImageResource(R.drawable.contact_placeholder);
        camIcon.setTag(Constants.CAM_TAG_DEFAULT);
    }

    private void checkProfileExists(String userId) {
        materialDialogBuilderMessage = mContext.getResources().getString(R.string.profile_check);
        MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext)
                .title(R.string.Please_wait)
                .content(materialDialogBuilderMessage)
                .progress(true, 0)
                .cancelable(false);
        materialDialogBuilder = builder.build();
        materialDialogBuilder.show();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference userRef = db.collection(Constants.USERS).document(userId);
        token = FirebaseInstanceId.getInstance().getToken();
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        StorageReference storageRef = storage.getReferenceFromUrl(stringRef)
                                .child(document.get(Constants.FS_ID).toString())
                                .child(Constants.PROFILE_PICS)
                                .child(Constants.PROFILE_IMAGE);

                        storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                user = document.toObject(DatabaseUser.class);
                                existingUser = true;
                                camIcon.setTag(Constants.CAM_TAG_EXISTING);
                                if (getActivity() != null) {
                                    if (!getActivity().isFinishing()) {
                                        dismissProgressDialog();
                                    }
                                }
                                Snackbar.make(profileName, "Profile found", Snackbar.LENGTH_SHORT).show();
                                populateViews();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                            }
                        });
                    } else {
                        existingUser = false;
                        Snackbar.make(profileName, mContext.getString(R.string.no_profile_found), Snackbar.LENGTH_SHORT).show();
                        //change this for a dialog explaining we are checking f they are a user
                        if (getActivity().isFinishing()) {
                            return;
                        }
                        dismissProgressDialog();
                    }
                }
            }
        });
        hasChecked = true;
    }

    private DatabaseUser getUser() {
        return this.user;
    }

    private void populateViews() {
        DatabaseUser dbUser = getUser();
        if (getActivity() != null && dbUser != null) {
            profileName.setText(dbUser.getUser_name());
            profileStatus.setText(dbUser.getUser_status());
            if (dbUser.getUser_image() != null) {
                Uri uri = Uri.parse(dbUser.getUser_image());
                Glide.with(getActivity())
                        .load(uri)
                        .apply(new RequestOptions()
                                .dontAnimate().placeholder(R.drawable.placeholder))
                        .into(icon);
            }
        }
    }

    private void validateData() {
        //check all data is ok
        DatabaseUser databaseUser = getUser();
        if (profileName.getText().toString().isEmpty()) {
            Snackbar.make(profileName, mContext.getString(R.string.validate_name_failed), Snackbar.LENGTH_SHORT).show();
        } else if (profileStatus.getText().toString().isEmpty()) {
            Snackbar.make(profileName, mContext.getString(R.string.validate_status_failed), Snackbar.LENGTH_SHORT).show();
        } else if (phoneNumber.isEmpty()) {
            Snackbar.make(profileName, mContext.getString(R.string.validate_number_failed), Snackbar.LENGTH_SHORT).show();
        } else if (userId.isEmpty()) {
            Snackbar.make(profileName, mContext.getString(R.string.validate_id_failed), Snackbar.LENGTH_SHORT).show();
        } else if (camIcon.getTag().equals(Constants.CAM_TAG_DEFAULT)) {
            Snackbar.make(profileName, mContext.getString(R.string.validate_profile_image_failed), Snackbar.LENGTH_SHORT).show();
        } else {
            materialDialogBuilderMessage2 = mContext.getString(R.string.update_profile);
            MaterialDialog.Builder builder2 = new MaterialDialog.Builder(mContext)
                    .title(R.string.Please_wait)
                    .content(materialDialogBuilderMessage2)
                    .progress(true, 0)
                    .cancelable(false);
            materialDialog2Builder = builder2.build();
            materialDialog2Builder.show();
            databaseUser.setUser_id(userId);
            databaseUser.setUser_name(profileName.getText().toString());
            databaseUser.setUser_status(profileStatus.getText().toString());
            databaseUser.setUser_number(phoneNumber);
            Uri uri;
            if (databaseUser.getUser_image() != null){
                if (databaseUser.getUser_image().startsWith("file")) {
                    uri = Uri.parse(databaseUser.getUser_image());
                    uploadImage(databaseUser,uri);
                }
                else if (databaseUser.getUser_image().startsWith("https")){
                    saveUser(databaseUser);
                }
            }else{
                uri = Uri.parse("android.resource://com.sealstudios.aimessage/drawable/contact_placeholder");
                uploadImage(databaseUser,uri);
            }

        }
    }

    private void uploadImage(DatabaseUser databaseUser, Uri uri) {
        final StorageReference uploadRef = storageReference.child(userId).child(Constants.PROFILE_PICS).child(Constants.PROFILE_IMAGE);
        UploadTask uploadTask = uploadRef.putFile(uri);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Snackbar.make(profileName, mContext.getString(R.string.failed_creating_profile), Snackbar.LENGTH_SHORT);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                uploadRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        databaseUser.setUser_image(uri.toString());
                        //TODO create a smaller image
                        databaseUser.setUser_small_image(uri.toString());
                        saveUser(databaseUser);
                    }
                });
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
            }
        });
    }

    private void saveUser(DatabaseUser databaseUser) {
        token  = FirebaseInstanceId.getInstance().getToken();
        SharedPreferences pref = getActivity().getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(Constants.SIGNED_IN, true);
        editor.putString(Constants.FS_ID, databaseUser.getUser_id());
        editor.putString(Constants.FS_NUMBER, databaseUser.getUser_number());
        editor.apply();
        if (existingUser) {
            userViewModel.updateMyUser(databaseUser, token);
        } else {
            databaseUser.setBlocked(false);
            databaseUser.setUnread(0);
            databaseUser.setUser_time_stamp(new Date());
            userViewModel.insertMyUser(databaseUser, token);
        }
        dismissProgressDialog();
        Snackbar.make(profileName, R.string.update_complete, Snackbar.LENGTH_SHORT).show();
        hasUploaded = true;
        Intent i = new Intent(mContext, MainActivity.class);
        i.putExtra(Constants.FS_ID, databaseUser.getUser_id());
        i.putExtra(Constants.FS_NUMBER, databaseUser.getUser_number());
        i.putExtra(Constants.SIGNED_IN, true);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        if (getActivity() != null) {
            if (getActivity().isFinishing()) {
                return;
            }
        }

    }

    private void refreshImagePicker(ImageView imageView) {
        imageView.setImageURI(null);
        imagePicker = new ImagePicker(getActivity(),
                this,
                imageUri -> {/*on image picked */
                    imageView.setImageURI(imageUri);
                })
                .setWithImageCrop(1, 1);
    }

    private void dismissProgressDialog() {
        if (materialDialogBuilder != null && materialDialogBuilder.isShowing()) {
            materialDialogBuilder.dismiss();
        }
        if (materialDialog2Builder != null && materialDialog2Builder.isShowing()) {
            materialDialog2Builder.dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(Constants.FS_ID, user);
    }

    @Override
    public void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imagePicker.handleActivityResult(resultCode, requestCode, data);
        camIcon.setTag(Constants.CAM_TAG_CHANGED);
        if (imagePicker.getImageFile() != null) {
            Uri uri = Uri.fromFile(imagePicker.getImageFile());
            user.setUser_image(uri.toString());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        imagePicker.handlePermission(requestCode, grantResults);
    }

}
