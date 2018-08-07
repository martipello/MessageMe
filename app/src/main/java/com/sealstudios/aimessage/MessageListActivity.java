package com.sealstudios.aimessage;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;
import com.sealstudios.aimessage.Database.DatabaseContacts;
import com.sealstudios.aimessage.Database.DatabaseMessage;
import com.sealstudios.aimessage.Database.DatabaseUser;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.ViewModels.ContactsViewModel;
import com.sealstudios.aimessage.ViewModels.MessagesViewModel;
import com.sealstudios.aimessage.ViewModels.MessagesViewModelFactory;
import com.sealstudios.aimessage.ViewModels.UserViewModel;
import com.sealstudios.aimessage.adapters.LiveMessageAdapter;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.google.GoogleEmojiProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageListActivity extends AppCompatActivity {

    private static final int TAKE_PICTURE_REQUEST = 100;
    private static final int PICK_IMAGE_REQUEST = 20;
    public static final String LAYOUT_MANAGER_STATE = "STATE";
    public static final String IMAGE = "IMAGE";
    private static final int LOADER_ID = 200;
    private RecyclerView mMessageRecycler;
    private LiveMessageAdapter mMessageAdapter;
    private ArrayList<DatabaseMessage> messageList, selectedMessageList;
    private EmojiEditText messageText;
    private ImageButton addCamera, addFile, addEmoji;
    private CircleImageView fromCam, fromFile, fromGallery;
    private DatabaseContacts recipientUser;
    private DatabaseUser senderUser;
    private FirebaseFirestore db;
    private String TAG = "emoji keyboard";
    private String prog3Message, stdByChannel;
    private LinearLayoutManager linearLayoutManager;
    private Pubnub mPubNub;
    private Menu myMenu;
    private boolean showMenuItems = false;
    private TextView results;
    private ProgressDialog prog3;
    private CardView mRevealView;
    File galleryFile;
    private EmojiPopup emojiPopup;
    private SparseBooleanArray selectedItems;
    private boolean hidden = true;
    public static boolean isActive;
    private CoordinatorLayout rootLayout;
    private String pictureImagePath = "";
    private Uri cameraUri;
    private String userId, recipientId, recipientName;
    private ContactsViewModel contactViewModel;
    private UserViewModel userViewModel;
    private MessagesViewModel messagesViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EmojiManager.install(new GoogleEmojiProvider());
        setContentView(R.layout.activity_message_list);
        final Bundle data = getIntent().getExtras();
        isActive = true;
        if (data != null) {
            SharedPreferences pref = getApplicationContext().getSharedPreferences(Constants.SHARED_PREFS, 0);
            userId = pref.getString(Constants.FS_ID, "id");
            recipientName = (data.getString(Constants.FS_NAME));
            recipientId = (data.getString(Constants.FS_ID));
            getSupportActionBar().setTitle(recipientName);
            SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
            SharedPreferences.Editor ed = sp.edit();
            ed.putString(Constants.ACTIVE_USER, recipientId);
            ed.apply();
            Bundle bundle = new Bundle();
            bundle.putString(Constants.FS_ID, userId);
            bundle.putString(Constants.DB_USER_NAME, recipientId);
            //getSupportLoaderManager().initLoader(LOADER_ID, bundle, dataLoaderListener);
        }
        CoordinatorLayout llBottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        ViewCompat.postOnAnimation(llBottomSheet, new Runnable() {
            @Override
            public void run() {
                ViewCompat.postInvalidateOnAnimation(llBottomSheet);
            }
        });
        results = findViewById(R.id.results);
        rootLayout = findViewById(R.id.root_layout);
        mRevealView = findViewById(R.id.reveal_items);
        messageText = findViewById(R.id.message_text);
        fromCam = findViewById(R.id.from_camera);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            fromCam.setEnabled(false);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
        fromCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                takePicture();
            }
        });
        fromGallery = findViewById(R.id.from_gallery);
        fromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                pickImage();
            }
        });
        fromFile = findViewById(R.id.from_files);
        fromFile.setEnabled(false);
        fromFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(v, R.string.coming_soon, Snackbar.LENGTH_SHORT).show();
            }
        });
        addFile = findViewById(R.id.add_attachment);
        addFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //revealAnimation(isKeyboardOpen(rootLayout));
                bottomSheetBehavior.setState(bottomSheetBehavior.getState() ==
                        BottomSheetBehavior.STATE_HIDDEN ? BottomSheetBehavior.STATE_COLLAPSED : BottomSheetBehavior.STATE_HIDDEN);
            }
        });
        addEmoji = findViewById(R.id.add_emoji);
        addEmoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emojiPopup.toggle();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setUpEmojiPopup();
        db = FirebaseFirestore.getInstance();
        selectedItems = new SparseBooleanArray();
        selectedMessageList = new ArrayList<>();
        ImageView imageView = findViewById(R.id.background);
        Glide.with(this).load(R.drawable.message_background).apply(new RequestOptions().centerCrop()).into(imageView);
        mMessageRecycler = findViewById(R.id.reyclerview_message_list);
        MessageListActivity.OnItemTouchListener itemTouchListener = new MessageListActivity.OnItemTouchListener() {
            @Override
            public void onCardClick(View view, int position) {
                if (mMessageAdapter.getSelectedItemCount() > 0) {
                    DatabaseMessage userMessage = mMessageAdapter.getList().get(position);
                    if (!selectedItems.get(position)) {
                        selectedMessageList.add(userMessage);
                    } else {
                        selectedMessageList.remove(userMessage);
                    }
                    mMessageAdapter.toggleSelection(position);
                    if (mMessageAdapter.getSelectedItemCount() < 1) {
                        showMenuItems = false;
                        hideShowMenu(showMenuItems);
                    }
                } else {
                    if (mMessageAdapter.getList().get(position).getData_type().equals(Constants.DATA_TYPE_IMAGE)) {
                        //open preview activity
                        Intent i = new Intent(MessageListActivity.this, PreviewImageActivity.class);
                        i.putExtra(Constants.USER_NAME, senderUser.getUser_id());
                        i.putExtra(Constants.DB_USER_NAME, recipientUser.getUser_id());
                        i.putExtra(Constants.JSON_MSG_UUID, mMessageAdapter.getList().get(position).getMessageId());
                        startActivity(i);
                    }
                }
            }
            @Override
            public void onCardLongClick(View view, int position) {
                //clearGrid();
                if (mMessageAdapter.getSelectedItemCount() < 1) {
                    mMessageAdapter.toggleSelection(position);
                    showMenuItems = true;
                    hideShowMenu(showMenuItems);
                } else {
                }
            }
        };
        messageList = new ArrayList<>();
        mMessageAdapter = new LiveMessageAdapter(messageList, getApplicationContext(), itemTouchListener, selectedItems, userId);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mMessageRecycler.setLayoutManager(linearLayoutManager);
        mMessageRecycler.setAdapter(mMessageAdapter);
        CircleImageView fab = findViewById(R.id.send);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateMessage(createMessage(messageText.getText().toString(), Constants.DATA_TYPE_TEXT), null);
            }
        });
        contactViewModel = ViewModelProviders.of(this).get(ContactsViewModel.class);
        contactViewModel.getContactById(recipientId).observe(this, new Observer<DatabaseContacts>() {
            @Override
            public void onChanged(@Nullable DatabaseContacts databaseContacts) {
                recipientUser = databaseContacts;
            }
        });
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        userViewModel.getById(userId).observe(this, new Observer<DatabaseUser>() {
            @Override
            public void onChanged(@Nullable DatabaseUser databaseUser) {
                senderUser = databaseUser;
            }
        });
        messagesViewModel = ViewModelProviders.of(this,
                new MessagesViewModelFactory(this.getApplication(),recipientId)).get(MessagesViewModel.class);
        messagesViewModel.getAllMessages(userId, recipientId).observe(this, new Observer<List<DatabaseMessage>>() {
            @Override
            public void onChanged(@Nullable List<DatabaseMessage> databaseMessageList) {
                ArrayList<DatabaseMessage> tempList = new ArrayList<>();
                tempList.addAll(databaseMessageList);
                for (DatabaseMessage message : tempList){
                    Log.d("VC-MsgLstAct" , "sr " + message.getSent_received() + " UN " + message.getSenderName() + " text " + message.getMessage());
                }
                mMessageAdapter.refreshMyList(tempList);
                linearLayoutManager.smoothScrollToPosition(mMessageRecycler, null, tempList.size());
                setText(tempList.size());
            }
        });
        stdByChannel = userId + Constants.STDBY_SUFFIX;
        initPubNub();
        //TODO implement paging
    }

    private void setUpEmojiPopup() {
        emojiPopup = EmojiPopup.Builder.fromRootView(rootLayout)
                .setOnEmojiBackspaceClickListener(ignore -> Log.d(TAG, "Clicked on Backspace"))
                .setOnEmojiClickListener((ignore, ignore2) -> Log.d(TAG, "Clicked on emoji"))
                .setOnEmojiPopupShownListener(() -> addEmoji.setImageResource(R.drawable.baseline_keyboard_black_24))
                .setOnSoftKeyboardOpenListener(ignore -> Log.d(TAG, "Opened soft keyboard"))
                .setOnEmojiPopupDismissListener(() -> addEmoji.setImageResource(R.drawable.baseline_mood_black_24))
                .setOnSoftKeyboardCloseListener(() -> Log.d(TAG, "Closed soft keyboard"))
                .build(messageText);
    }

    private void showImagePreviewMessageDialog(String messageText, Uri imageUri) {
        FragmentManager fm = getSupportFragmentManager();
        ImagePreviewMessage imagePreviewMessage = ImagePreviewMessage.newInstance(messageText, imageUri);
        imagePreviewMessage.show(fm, "fragment_picture_message");
    }

    public DatabaseMessage createMessage(String message_Text, String dataType) {
        Calendar cal = Calendar.getInstance();
        DatabaseMessage databaseMessage = new DatabaseMessage();
        databaseMessage.setMessageId(cal.getTime().toString());
        databaseMessage.setSenderId(senderUser.getUser_id());
        databaseMessage.setMessage(message_Text);
        databaseMessage.setTime_stamp(cal.getTime());
        databaseMessage.setData_type(dataType);
        databaseMessage.setData_url("");
        databaseMessage.setRecipientId(recipientUser.getUser_id());
        databaseMessage.setSenderName(senderUser.getUser_name());
        databaseMessage.setSent_received(0);
        databaseMessage.setRecipientName(recipientUser.getUser_name());
        return databaseMessage;
    }

    private void takePicture() {
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (pictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ignored) {
            }
            if (photoFile != null) {
                cameraUri = FileProvider.getUriForFile(this, Constants.APP_PROVIDER, photoFile);
                pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                startActivityForResult(pictureIntent, TAKE_PICTURE_REQUEST);
            }
        }
    }

    private void pickImage() {
        //TODO use this method in place of the image picker in the status and login activity
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, MessageListActivity.this.getString(R.string.choose_picture)), PICK_IMAGE_REQUEST);
        /*
        //may use this for build version < 19
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra("crop", "false");
        intent.putExtra("scale", true);
        intent.putExtra("outputX", 256);
        intent.putExtra("outputY", 256);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, 5);
        */
    }

    public void validateMessage(DatabaseMessage userMessage, Uri imageUri) {
        //check if text is 0
        //check if there is an image or text etc
        //check if user is allowed to send this message to the recipient
        //check if user is in the recipient contact and if not add them
        Log.d("VldMsg","data type " + userMessage.getData_type() + " senderid " + userMessage.getSenderId());
        if (recipientUser.getBlocked()) {
            Snackbar.make(mMessageRecycler, R.string.blocked_user, Snackbar.LENGTH_LONG).show();
        } else {
            switch (userMessage.getData_type()) {
                case Constants.DATA_TYPE_TEXT:
                    if (messageText.getText().length() < 1) {
                        new MaterialDialog.Builder(MessageListActivity.this)
                                .title(R.string.confirm)
                                .content(R.string.blank_message)
                                .positiveText(R.string.yes)
                                .negativeText(R.string.cancel)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(MaterialDialog dialog, DialogAction which) {
                                        //messagesViewModel.insertMessage(userMessage);
                                        messagesViewModel.insertMessageOnline(userMessage);
                                        messageText.getText().clear();
                                    }
                                }).onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                dialog.dismiss();
                            }
                        });

                    } else {
                        ///messagesViewModel.insertMessage(userMessage);
                        messagesViewModel.insertMessageOnline(userMessage);
                        messageText.getText().clear();
                    }
                    break;
                case Constants.DATA_TYPE_IMAGE:
                    userMessage.setData_url(imageUri.toString());
                    messagesViewModel.insertDataMessageOnline(userMessage);
                    messageText.getText().clear();
                    break;
            }
        }
    }

    /**
     * Subscribe to standby channel so that it doesn't interfere with the WebRTC Signaling.
     */
    private void hideShowMenu(boolean show) {
        myMenu.findItem(R.id.action_vid_call).setVisible(!show);
        myMenu.findItem(R.id.action_vid_call).setEnabled(!show);
        myMenu.findItem(R.id.delete).setVisible(show);
        myMenu.findItem(R.id.delete).setEnabled(show);
        myMenu.findItem(R.id.copy).setVisible(show);
        myMenu.findItem(R.id.copy).setEnabled(show);
    }

    private void initPubNub() {
        mPubNub = new Pubnub(Constants.PUB_KEY, Constants.SUB_KEY);
        mPubNub.setUUID(userId);
        subscribeStdBy();
    }

    /**
     * Subscribe to standby channel
     */
    private void subscribeStdBy() {
        Log.d("VC-MsgLstAct", "Subscribe stndby " + stdByChannel);
        try {
            mPubNub.subscribe(stdByChannel, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.d("VC-MsgLstAct", "MESSAGE: " + message.toString());
                    if (!(message instanceof JSONObject)) return; // Ignore if not JSONObject
                    JSONObject jsonMsg = (JSONObject) message;
                    try {
                        /*
                        JSONObject jsonCall = new JSONObject();
                            jsonCall.put(Constants.JSON_CALL_USER_ID, userId);//this is correct
                            jsonCall.put(Constants.JSON_CALL_USER_NAME, name);//this is correct
                            jsonCall.put(Constants.JSON_CALL_TIME, System.currentTimeMillis());
                         */
                        if (!jsonMsg.has(Constants.JSON_CALL_USER_ID))
                            return;     //Ignore Signaling messages.
                        String user = jsonMsg.getString(Constants.JSON_CALL_USER_ID);
                        String userName = jsonMsg.getString(Constants.JSON_CALL_USER_NAME);
                        Log.d("VC-MsgLstAct" , "subscribeStdBy user " + user);
                        dispatchIncomingCall(user,userName);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void connectCallback(String channel, Object message) {
                    Log.d("MsgListAct", "CONNECTED: " + message.toString());
                    setUserStatus(Constants.STATUS_AVAILABLE);
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    Log.d("MsgListAct", "ERROR: " + error.toString());
                }
            });
        } catch (PubnubException e) {
            Log.d("MsgListAct", "HEREEEE");
            e.printStackTrace();
        }
    }


    private void setText(int size) {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        //TODO make this a better pop up rather than text in the middle of the screen
        if (!isConnected) {
            results.setVisibility(View.VISIBLE);
            results.setText(this.getString(R.string.no_connection));
        } else {
            results.setVisibility(View.GONE);
        }
        if (size == 0) {
            results.setVisibility(View.VISIBLE);
            results.setText(this.getString(R.string.no_messages));
        } else {
            results.setVisibility(View.GONE);
        }
    }

    private void makeCall(View view) {
        Log.d("VC-MsgLstAct", "make call");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO}, 0);
        } else {
            //
            if (recipientUser == null){
                Snackbar.make(mMessageRecycler,R.string.oops,Snackbar.LENGTH_SHORT).show();
            }else{
                String callId = recipientUser.getUser_id();
                //String callId = recipientUser.getUser_id();
                if (callId.isEmpty() || callId.equals(userId)) {
                    //showToast("Enter a valid user ID to call.");
                    Snackbar.make(mMessageRecycler, "Sorry we can't call this contact", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (!recipientUser.getBlocked()){
                    dispatchCall(callId);
                    Log.d("VC-MsgLstAct", "dispatchCall(callNum) " + callId);
                }else{
                    Snackbar.make(messageText, getString(R.string.blocked_user),Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void dispatchCall(final String callId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO}, 0);
        } else {
                final String callNumStdBy = callId + Constants.STDBY_SUFFIX;
                final String name = senderUser.getUser_name();
                mPubNub.hereNow(callNumStdBy, new Callback() {
                    @Override
                    public void successCallback(String channel, Object message) {
                        Log.d("MsgList", "dispatch call HERE_NOW: " + " CH - " + callNumStdBy + " " + message.toString());
                        try {
                            int occupancy = ((JSONObject) message).getInt(Constants.JSON_OCCUPANCY);
                            Log.d("MsgList", "Json occupancy " + occupancy);
                    /*
                    if (occupancy == 0) {
                        System.out.println("User is not online");
                        return;
                    }                    */

                            Log.d("VC-MsgLstAct", "JSON_CALL_USER_NAME " + name + " JSON_CALL_USER_ID " + userId);
                            JSONObject jsonCall = new JSONObject();
                            jsonCall.put(Constants.JSON_CALL_USER_ID, userId);//this is correct
                            jsonCall.put(Constants.JSON_CALL_USER_NAME, name);//this is correct
                            jsonCall.put(Constants.JSON_CALL_TIME, System.currentTimeMillis());
                            mPubNub.publish(callNumStdBy, jsonCall, new Callback() {
                                @Override
                                public void successCallback(String channel, Object message) {
                                    Log.d("VC-MsgLstAct", "SUCCESS: " + message.toString());//message object retrieved from target device
                                    Intent intent = new Intent(MessageListActivity.this, VideoChatActivity.class);
                                    Bundle b = new Bundle();
                                    b.putString(Constants.USER_NAME, name);
                                    b.putString(Constants.USER_ID, userId);
                                    b.putString(Constants.CALL_USER_ID, callId);
                                    b.putString(Constants.CALL_USER_NAME,recipientName);
                                    b.putBoolean("dialed", true);
                                    intent.putExtras(b);
                                    startActivity(intent);
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        }

    }


    private void dispatchIncomingCall(String mUserId, String mUserName) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO}, 0);
        } else {
            Log.d("VC-MsgLstAct","dispatchIncomingCall USER_NAME " + senderUser.getUser_name() + " USER_ID "
                    + userId + " CALL_USER_ID " + mUserId + " CALL_USER_NAME " + mUserName);
            Intent intent = new Intent(MessageListActivity.this, IncomingCallActivity.class);
            Bundle b = new Bundle();
            b.putString(Constants.USER_NAME, senderUser.getUser_name());
            b.putString(Constants.USER_ID, userId);
            b.putString(Constants.CALL_USER_ID, mUserId);
            b.putString(Constants.CALL_USER_NAME,mUserName);
            b.putBoolean("dialed", false);
            intent.putExtras(b);
            startActivity(intent);
        }
    }

    private void setUserStatus(String status) {
        try {
            JSONObject state = new JSONObject();
            state.put(Constants.JSON_STATUS, status);
            mPubNub.setState(stdByChannel, userId, state, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.d("MLA-sUS", "State Set: " + message.toString());
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getUserStatus() {
        String stdByUser = senderUser.getUser_id() + Constants.STDBY_SUFFIX;
        mPubNub.getState(stdByUser, senderUser.getUser_id(), new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                Log.d("MLA-gUS", "User Status: " + message.toString());
            }
        });
    }

    private void deleteMessages() {
        new MaterialDialog.Builder(this)
                .title(R.string.confirm)
                .content(R.string.confirm_delete)
                .positiveText(R.string.yes).onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                ArrayList<DatabaseMessage> messages = new ArrayList<>();
                messages.addAll(mMessageAdapter.getSelectedItems());
                final int[] allMessages = {mMessageAdapter.getSelectedItemCount()};
                final int[] messagesDeleted = {0};
                final int[] messagesDeletedErrors = {0};
                dialog.dismiss();
                prog3Message = MessageListActivity.this.getString(R.string.deleting_update, allMessages[0]);
                prog3 = new ProgressDialog(MessageListActivity.this);
                prog3.setTitle(R.string.Please_wait);
                prog3.setMessage(prog3Message);
                prog3.setCancelable(false);
                prog3.setIndeterminate(true);
                prog3.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                prog3.show();
                for (DatabaseMessage message : messages) {
                    messagesViewModel.deleteMessage(message);
                    db.collection(Constants.USERS).document(senderUser.getUser_id()).collection(Constants.CONTACTS)
                            .document(recipientUser.getUser_id()).collection(Constants.MESSAGES).document(message.getMessageId())
                            .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    messagesDeleted[0]++;
                                    prog3Message = MessageListActivity.this.getString(R.string.deleted_update, messagesDeleted[0], allMessages[0], messagesDeletedErrors[0]);

                                    runOnUiThread(changeMessage);
                                    if (messagesDeleted[0] == allMessages[0]) {
                                        prog3.dismiss();
                                        showMenuItems = false;
                                        ///selectedMessageList.clear();
                                        hideShowMenu(showMenuItems);
                                        mMessageAdapter.clearSelections();
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "Error deleting document", e);
                                    messagesDeleted[0]++;
                                    messagesDeletedErrors[0]++;
                                    prog3Message = MessageListActivity.this.getString(R.string.deleted_update, messagesDeleted[0], allMessages[0], messagesDeletedErrors[0]);
                                    runOnUiThread(changeMessage);
                                    if (messagesDeleted[0] == allMessages[0]) {
                                        prog3.dismiss();
                                        showMenuItems = false;
                                        ///selectedMessageList.clear();
                                        hideShowMenu(showMenuItems);
                                        mMessageAdapter.clearSelections();
                                    }
                                }
                            });

                }
            }
        })
                .negativeText(R.string.cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private void muteConversation() {
        //check if conversation is muted assign this to a boolean
        boolean muted = false;
        if (muted) {
            new MaterialDialog.Builder(this)
                    .title(R.string.mute)
                    .content(R.string.confirm_mute)
                    .positiveText(R.string.yes)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            //TODO mute conversation
                        }
                    }).negativeText(R.string.cancel).onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    dialog.dismiss();
                }
            }).show();
        } else {
            new MaterialDialog.Builder(this)
                    .title(R.string.unmute)
                    .content(R.string.confirm_unmute)
                    .positiveText(R.string.yes)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            //TODO mute conversation
                        }
                    }).negativeText(R.string.cancel).onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    dialog.dismiss();
                }
            }).show();
        }
    }

    private void blockUser() {
        //check if user is blocked assign this to a boolean
        boolean blocked = recipientUser.getBlocked();
        String title = !blocked ? getString(R.string.block) : getString(R.string.unblock);
        String content = !blocked ? getString(R.string.confirm_block) : getString(R.string.confirm_unblock);
        new MaterialDialog.Builder(MessageListActivity.this)
                .title(title)
                .content(content)
                .positiveText(R.string.yes)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Map<String, Object> blockMap = new HashMap<>();
                        blockMap.put(Constants.FS_BLOCKED, !blocked);
                        DocumentReference messageSenderRef = db.collection(Constants.USERS)
                                .document(senderUser.getUser_id()).collection(Constants.CONTACTS).document(recipientUser.getUser_id());
                        messageSenderRef.update(blockMap);
                        DocumentReference messageRecipientRef = db.collection(Constants.USERS)
                                .document(recipientUser.getUser_id()).collection(Constants.CONTACTS).document(senderUser.getUser_id());
                        messageRecipientRef.update(blockMap);
                    }
                }).negativeText(R.string.cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private Runnable changeMessage = new Runnable() {
        @Override
        public void run() {
            prog3.setMessage(prog3Message);
        }
    };

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir =
                getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        pictureImagePath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, R.string.no_permissions, Toast.LENGTH_SHORT).show();
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == Activity.RESULT_OK) {
            //validateMessage(createMessage(messageText.getText().toString(), Constants.DATA_TYPE_IMAGE), cameraUri);
            showImagePreviewMessageDialog(messageText.getText().toString(),cameraUri);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // User Cancelled the action
        }
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            //validateMessage(createMessage(messageText.getText().toString(), Constants.DATA_TYPE_IMAGE), uri);
            showImagePreviewMessageDialog(messageText.getText().toString(),uri);
        }

    }

    @Override
    public void onBackPressed() {
        if (emojiPopup != null && emojiPopup.isShowing()) {
            emojiPopup.dismiss();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        userViewModel.removeListener();
        contactViewModel.removeListener();
        messagesViewModel.removeListener();
        isActive = false;
        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString(Constants.ACTIVE_USER, "");
        ed.apply();
    }

    @Override
    protected void onStop() {
        if (emojiPopup != null) {
            emojiPopup.dismiss();
        }

        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActive = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(LAYOUT_MANAGER_STATE, linearLayoutManager.onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable(LAYOUT_MANAGER_STATE);
            linearLayoutManager.onRestoreInstanceState(savedRecyclerLayoutState);
        }
    }

    public interface OnItemTouchListener {
        void onCardClick(View view, int position);

        void onCardLongClick(View view, int position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        myMenu = menu;
        //TODO check if conversation is muted
        //TODO check if user is blocked
        ///myMenu.findItem(R.id.mute).setTitle();
        //myMenu.findItem(R.id.block).setTitle();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_conversation, menu);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.preferences:
                return true;
            case R.id.mute:
                muteConversation();
                return true;
            case R.id.block:
                blockUser();
                return true;
            case R.id.action_vid_call:
                makeCall(mMessageRecycler);
                return true;
            case R.id.delete:
                //TODO show confirmation message
                deleteMessages();
                return true;
            case R.id.copy:
                StringBuilder builder = new StringBuilder();
                ArrayList<DatabaseMessage> messages = new ArrayList<>();
                messages.addAll(mMessageAdapter.getSelectedItems());
                for (DatabaseMessage message : messages) {
                    builder.append(message.getMessage());
                    builder.append("\n");
                }
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Message text", builder.toString());
                clipboard.setPrimaryClip(clip);
                Snackbar.make(mMessageRecycler, R.string.copy_text, Snackbar.LENGTH_SHORT).show();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
