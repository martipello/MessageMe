package com.sealstudios.aimessage;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.myhexaville.smartimagepicker.ImagePicker;
import com.rilixtech.materialfancybutton.MaterialFancyButton;
import com.sealstudios.aimessage.Database.DatabaseContacts;
import com.sealstudios.aimessage.Database.DatabaseUser;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.ViewModels.ContactsViewModel;
import com.sealstudios.aimessage.ViewModels.UserViewModel;
import com.sealstudios.aimessage.adapters.LiveContactsStatusAdapter;
import com.vanniktech.emoji.EmojiEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class StatusFragment extends Fragment {
    private CircleImageView icon;
    private EmojiEditText profileName, profileStatus;
    private static String FRAG_TAG = "fragment_contact";
    private final static int loader = 101;
    private final static int checker = 201;
    private final int databaseChecker = 301;
    private TextView numberText;
    private MaterialFancyButton getStarted;
    private ImageView imageBackground;
    private ImageView camIcon;
    private ImagePicker imagePicker;
    private FirebaseFirestore db;
    private String stringRef = Constants.STORAGE_REF;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private RecyclerView recyclerView;
    private LiveContactsStatusAdapter contactsAdapter;
    private LinearLayoutManager linearLayoutManager;
    private ArrayList<DatabaseContacts> userObjectArrayList;
    final int[] usersBackedUp = {0};
    private MaterialDialog materialDialog2Builder;
    private String materialDialogBuilderMessage2;
    private String userId;
    private ContactsViewModel contactViewModel;
    private UserViewModel userViewModel;
    DatabaseUser dbUser;

    public StatusFragment() {
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
        View rootView = inflater.inflate(R.layout.fragment_status, container, false);
        setHasOptionsMenu(true);
        userObjectArrayList = new ArrayList<>();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReferenceFromUrl(stringRef);
        //getActivity().getActionBar().setTitle("Status");
        imageBackground = rootView.findViewById(R.id.backgroundImageView);
        Glide.with(getActivity())
                .load(R.drawable.message_background)
                .apply(new RequestOptions()
                        .centerCrop())
                .into(imageBackground);
        userId = MainActivity.userId;
        recyclerView = rootView.findViewById(R.id.contacts_list_view);
        numberText = rootView.findViewById(R.id.phone_number);
        icon = rootView.findViewById(R.id.icon);
        camIcon = rootView.findViewById(R.id.camera_icon);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshImagePicker(icon);
                imagePicker.choosePicture(true /*show camera intents*/);
            }
        });
        profileName = rootView.findViewById(R.id.field_phone_number);
        profileStatus = rootView.findViewById(R.id.status_text);
        linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        ContactsActivity.OnItemTouchListener itemTouchListener = new ContactsActivity.OnItemTouchListener() {
            @Override
            public void onCardClick(View view, int position) {
                DatabaseContacts user = contactsAdapter.getList().get(position);
                Bundle b = new Bundle();
                b.putString(Constants.FS_NAME, user.getUser_name());
                b.putString(Constants.FS_ID, user.getUser_id());
                Intent i = new Intent(getActivity(), MessageListActivity.class);
                i.putExtras(b);
                startActivity(i);
            }
            @Override
            public void onCardLongClick(View view, int position) {
                //clearGrid();
            }
        };
        ContactsActivity.OnImageTouchListener imageTouchListener = new ContactsActivity.OnImageTouchListener() {
            @Override
            public void onImageClick(View view, int position) {
                //open contacts preview fragment
                //set argument (Constants.USER_NAME,user.get_id())
                DatabaseContacts user = contactsAdapter.getList().get(position);
                Bundle b = new Bundle();
                b.putString(Constants.USER_NAME,user.getUser_id());
                b.putString(Constants.DB_USER_NAME,user.getUser_id());
                showEditDialog(userId,user.getUser_id());
            }
        };
        contactsAdapter = new LiveContactsStatusAdapter(userObjectArrayList, getActivity(), itemTouchListener,imageTouchListener);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(contactsAdapter);
        contactViewModel = ViewModelProviders.of(this).get(ContactsViewModel.class);
        contactViewModel.getAllContacts().observe(this, new Observer<List<DatabaseContacts>>() {
            @Override
            public void onChanged(@Nullable List<DatabaseContacts> databaseContacts) {
                ArrayList<DatabaseContacts> tempList = new ArrayList<>();
                tempList.addAll(databaseContacts);
                contactsAdapter.refreshMyList(tempList);
            }
        });
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        userViewModel.getUserById(userId).observe(this, new Observer<DatabaseUser>() {
            @Override
            public void onChanged(@Nullable DatabaseUser databaseUser) {
                dbUser = databaseUser;
                populateViews(databaseUser);
            }
        });
        getStarted = rootView.findViewById(R.id.button_start_verification);
        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData(dbUser);
            }
        });
        getStarted.setVisibility(View.GONE);
        return rootView;
    }

    private void populateViews(DatabaseUser user) {
        profileName.setText(user.getUser_name());
        profileStatus.setText(user.getUser_status());
        numberText.setText(user.getUser_number());
        Glide.with(getActivity().getApplicationContext())
                .load(user.getUser_image())
                .apply(new RequestOptions()
                        .dontAnimate().placeholder(R.drawable.contact_placeholder))
                .into(icon);
        profileStatus.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getStarted.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        profileName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getStarted.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        if (camIcon.getTag().equals(Constants.CAM_TAG_DEFAULT)){
            getStarted.setVisibility(View.GONE);
        }else{
            getStarted.setVisibility(View.VISIBLE);
        }
    }

    private void validateData(DatabaseUser databaseUser) {
        if (dbUser != null){
            if (profileName.getText().toString().isEmpty()){
                Snackbar.make(profileName,R.string.validate_name_failed,Snackbar.LENGTH_SHORT).show();
            }
            else if (profileStatus.getText().toString().isEmpty()){
                Snackbar.make(profileName,R.string.validate_status_failed,Snackbar.LENGTH_SHORT).show();
            }else{
                materialDialogBuilderMessage2 = getActivity().getString(R.string.update_profile);
                MaterialDialog.Builder builder2 = new MaterialDialog.Builder(getActivity())
                        .title(R.string.Please_wait)
                        .content(materialDialogBuilderMessage2)
                        .progress(true, 0)
                        .cancelable(false);
                materialDialog2Builder = builder2.build();
                materialDialog2Builder.show();
                databaseUser.setUser_name(profileName.getText().toString());
                databaseUser.setUser_status(profileStatus.getText().toString());
                uploadImage(databaseUser);
            }
        }
        else {
            Snackbar.make(profileName,R.string.oops,Snackbar.LENGTH_SHORT).show();
        }
    }

    private void updateUser(DatabaseUser mDbUser) {
        userViewModel.updateMyUser(mDbUser,"");
        if (!getActivity().isFinishing()){
            materialDialog2Builder.dismiss();
        }
        getStarted.setVisibility(View.GONE);
        Snackbar.make(recyclerView,R.string.update_complete,Snackbar.LENGTH_SHORT).show();
    }

    private void uploadImage(DatabaseUser mDbUser) {
        if (camIcon.getTag().equals(Constants.CAM_TAG_CHANGED) && imagePicker.getImageFile() != null) {
            //TODO Image tag may say changed even when not as a result of user starting to pick an image and then not selecting one
            Uri uri = Uri.fromFile(imagePicker.getImageFile());
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            final StorageReference uploadRef = storageReference.child(userId).child(Constants.PROFILE_PICS).child(timeStamp);
            UploadTask uploadTask = uploadRef.putFile(uri);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Snackbar.make(profileName, R.string.failed_creating_profile, Snackbar.LENGTH_SHORT);
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    uploadRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            mDbUser.setUser_time_stamp(new Date());
                            mDbUser.setUser_image(uri.toString());
                            updateUser(mDbUser);
                        }
                    });
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                }
            });
        }else{
            mDbUser.setUser_time_stamp(new Date());
            updateUser(mDbUser);
        }
    }

    private void refreshImagePicker(CircleImageView imageView) {
        imageView.setImageURI(null);
        imagePicker = new ImagePicker(getActivity(),
                this,
                imageUri -> {/*on image picked */
                    imageView.setImageURI(imageUri);
                })
                .setWithImageCrop(1, 1);
    }

    private void showEditDialog(String userId,String contactId) {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        ContactPreviewFragment contactPreviewFragment = ContactPreviewFragment.newInstance(userId,contactId);
        contactPreviewFragment.show(fm, FRAG_TAG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imagePicker.handleActivityResult(resultCode, requestCode, data);
        getStarted.setVisibility(View.VISIBLE);
        camIcon.setTag(Constants.CAM_TAG_CHANGED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        imagePicker.handlePermission(requestCode, grantResults);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.menu_main_blank, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.preferences) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
