package com.sealstudios.aimessage;

import android.Manifest;
import android.annotation.TargetApi;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;
import com.sealstudios.aimessage.Database.DatabaseContacts;
import com.sealstudios.aimessage.Database.DatabaseUser;
import com.sealstudios.aimessage.Database.LiveDatabaseBuilder;
import com.sealstudios.aimessage.Database.LiveDbOpenHelper;
import com.sealstudios.aimessage.Database.UserRepository;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.ViewModels.UserViewModel;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int SIGN_IN_REQUEST_CODE = 100;
    private Fragment statusFragment;
    private Fragment callFragment;
    private Fragment convoFragment;
    private FirebaseFirestore db;
    private static final String MAIN_SELECTION = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
    private final String CALL_FRAG = "CALLS", CONVO_FRAG = "CONVO", STATUS_FRAG = "STATUS", TAG = "TAG", KEY = "KEY";
    private String CURRENT_FRAG = "";
    private String mSearchString;
    private String stringRef = Constants.STORAGE_REF;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    public static String userId;
    private String usersNumber;
    public Pubnub mPubNub;
    private String stdByChannel;
    private final int PERMISSION_REQUEST_CONTACT = 120;
    private static final int LOADER_MAIN = 440;
    private static final int FIRESTORE_LOADER = 54;
    private String[] mSelectionArgs = {mSearchString};
    private MaterialDialog materialDialogBuilder1;
    private MaterialDialog materialDialogBuilder2;
    private MaterialDialog materialDialogBuilder3;
    private String materialDialogBuilder2Message;
    private String materialDialogBuilder3Message;
    private final int[] usersBackedUp = {0};
    private ContentResolver cr;
    private FloatingActionButton fab;
    private boolean userSignedIn = false;
    private boolean nav = false;
    private Bundle mySavedInstanceState;
    private UserViewModel userViewModel;
    private DatabaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mySavedInstanceState = savedInstanceState;
        final Bundle data = getIntent().getExtras();
        SharedPreferences pref = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        /*
        if (data != null) {
            //typically just signed in
            userId = data.getString(Constants.FS_ID);
            usersNumber = data.getString(Constants.FS_NUMBER);
            userSignedIn = data.getBoolean(Constants.SIGNED_IN, false);
        }
        */
        userSignedIn = pref.getBoolean(Constants.SIGNED_IN, false);
        userId = pref.getString(Constants.FS_ID, "id");
        usersNumber = pref.getString(Constants.FS_NUMBER, "");
        mSearchString = "";
        cr = getContentResolver();
        EmojiManager.install(new GoogleEmojiProvider());
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fab = findViewById(R.id.new_message);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nav = true;
                askForContactPermission(nav);
            }
        });
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Start sign in/sign up activity
            Intent i = new Intent(this, LoginActivity.class);
            startActivityForResult(
                    i, SIGN_IN_REQUEST_CODE);
        } else {
            if (!userSignedIn) {
                FirebaseAuth.getInstance().signOut();
                Intent i = new Intent(this, LoginActivity.class);
                startActivityForResult(
                        i, SIGN_IN_REQUEST_CODE);
            } else {
                // User is already signed in. Therefore, display
                // a welcome Toast
                readyFragment();
                FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                db = FirebaseFirestore.getInstance();
                storage = FirebaseStorage.getInstance();
                storageReference = storage.getReferenceFromUrl(stringRef);
                stdByChannel = userId + Constants.STDBY_SUFFIX;
                //initPubNub();
                askForContactPermission(nav);
                userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
                userViewModel.getById(userId).observe(this, new Observer<DatabaseUser>() {
                    @Override
                    public void onChanged(@Nullable DatabaseUser databaseUser) {
                        user = databaseUser;
                    }
                });
            }
        }
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_convo:
                                CURRENT_FRAG = CONVO_FRAG;
                                switchFragment(convoFragment, CONVO_FRAG);
                                fab.show();
                                break;
                            case R.id.action_calls:
                                CURRENT_FRAG = CALL_FRAG;
                                switchFragment(callFragment, CALL_FRAG);
                                fab.hide();
                                break;
                            case R.id.action_status:
                                CURRENT_FRAG = STATUS_FRAG;
                                switchFragment(statusFragment, STATUS_FRAG);
                                fab.hide();
                                break;
                        }
                        return true;
                    }
                });
    }

    private void readyFragment() {
        statusFragment = new StatusFragment();
        convoFragment = new ConversationsFragment();
        callFragment = new CallsFragment();
        if (mySavedInstanceState == null) {
            CURRENT_FRAG = CONVO_FRAG;
            switchFragment(convoFragment, CONVO_FRAG);
        } else {
            Fragment fragment = getSupportFragmentManager().getFragment(mySavedInstanceState, KEY);
            String Tag = mySavedInstanceState.getString(TAG);
            CURRENT_FRAG = Tag;
            if (fragment != null) {
                switchFragment(fragment, Tag);
            } else {
                switchFragment(convoFragment, CONVO_FRAG);
            }
        }
    }

    private void switchFragment(Fragment fragment, String tag) {
        if (tag.equals(CONVO_FRAG)) {
            fab.show();
        } else {
            fab.hide();
        }
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.container, fragment, tag);
        fragmentTransaction.commitAllowingStateLoss()
        ;
    }

    private void saveToDatabase(DatabaseContacts contact) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                LiveDbOpenHelper dbContactOpenHelper = LiveDatabaseBuilder.getUserDatabase(MainActivity.this);
                dbContactOpenHelper.contactDaoLive().insertContact(contact);
            }
        }).start();
    }

    private void saveToFireStore(ArrayList<DatabaseContacts> userObjects) {
        ArrayList<DatabaseContacts> users = new ArrayList<>();
        usersBackedUp[0] = 0;
        users.addAll(userObjects);
        materialDialogBuilder2Message = MainActivity.this.getString(R.string.backup_progress, usersBackedUp[0], userObjects.size());
        MaterialDialog.Builder builder2 = new MaterialDialog.Builder(MainActivity.this)
                .title(R.string.Please_wait)
                .content(materialDialogBuilder2Message)
                .progress(true, 0)
                .cancelable(false);
        materialDialogBuilder2 = builder2.build();
        materialDialogBuilder2.show();

        for (DatabaseContacts userObject : userObjects) {
            saveToDatabase(userObject);
            Map<String, Object> user = new HashMap<>();
            user.put(Constants.FS_NAME, userObject.getUser_name());
            user.put(Constants.FS_NUMBER, userObject.getUser_number());
            user.put(Constants.FS_ID, userObject.getUser_id());
            user.put(Constants.FS_STATUS, userObject.getUser_status());
            user.put(Constants.FS_IMAGE, userObject.getUser_image());
            user.put(Constants.FS_BLOCKED, userObject.getBlocked());
            user.put(Constants.FS_SMALL_IMAGE, userObject.getUser_image());
            user.put(Constants.FS_RECENT_MSG, userObject.getUser_recent_message());
            user.put(Constants.FS_MSG_TIME_STAMP, userObject.getMsg_time_stamp());
            user.put(Constants.FS_UNREAD, 0);
            user.put(Constants.FS_TIME_STAMP, userObject.getUser_time_stamp());
            db.collection(Constants.USERS)
                    .document(userId)
                    .collection(Constants.CONTACTS)
                    .document(userObject.getUser_id())
                    .set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    usersBackedUp[0]++;
                    materialDialogBuilder2Message = MainActivity.this.getString(R.string.backup_progress, usersBackedUp[0], userObjects.size());
                    runOnUiThread(changeMessage2);
                    if (usersBackedUp[0] == userObjects.size()) {
                        //setContacts(users);
                        if (MainActivity.this.isFinishing()) {
                            return;
                        }
                    }
                }
            });
        }
    }

    private void getContactsFrom() {
        //TODO this only check if this is the first time the user has used the app on this device
        //TODO check if user has a profile and get all contcats from firestore after going through database and contacts
        SharedPreferences pref = getApplicationContext().getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        boolean first_time = pref.getBoolean(Constants.FIRST_TIME, true);
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (first_time) {
            //first time
            if (!isConnected) {
                Snackbar.make(fab, R.string.no_connection, Snackbar.LENGTH_LONG).show();
                //System.out.println("first time connection is false");

            } else {
                //System.out.println("first time connection is true");
                getSupportLoaderManager().initLoader(LOADER_MAIN, null, MainActivity.this);
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean(Constants.FIRST_TIME, false);
                editor.apply();
            }
        } else {
        }
    }

    public void restartMyLoader() {
        getSupportLoaderManager().restartLoader(LOADER_MAIN, null, this);
    }

    private void checkWithFirestore(ArrayList<DatabaseContacts> phoneContacts) {
        ///this method takes a list of all the users contacts and checks in the database if
        //the contacts number is also a user of this app
        final CollectionReference userRef = db.collection(Constants.USERS);
        ArrayList<DatabaseContacts> appUsers = new ArrayList<>();
        final int[] contactsWithApp = {0};
        final int[] allContacts = {phoneContacts.size()};
        final int[] checkedContacts = {0};
        materialDialogBuilder3Message = MainActivity.this.getString(R.string.check_contacts_progress, contactsWithApp[0]);
        MaterialDialog.Builder builder3 = new MaterialDialog.Builder(this)
                .title(R.string.Please_wait)
                .content(materialDialogBuilder3Message)
                .progress(true, 0)
                .cancelable(false);
        materialDialogBuilder3 = builder3.build();
        materialDialogBuilder3.show();

        for (DatabaseContacts contact : phoneContacts) {
            //users = all contacts
            String number = contact.getUser_number();
            Query query = userRef.whereEqualTo(Constants.FS_NUMBER, number);
            query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        checkedContacts[0]++;//checked_contacts_progress
                        materialDialogBuilder3Message = MainActivity.this.getString(R.string.checked_contacts_progress, checkedContacts[0], allContacts[0], contactsWithApp[0]);
                        runOnUiThread(changeMessage);
                        for (DocumentSnapshot doc : task.getResult()) {
                            contactsWithApp[0]++;
                            materialDialogBuilder3Message = MainActivity.this.getString(R.string.checked_contacts_progress, checkedContacts[0], allContacts[0], contactsWithApp[0]);
                            runOnUiThread(changeMessage);
                            DatabaseContacts contact = doc.toObject(DatabaseContacts.class);
                            appUsers.add(contact);
                        }
                        if (checkedContacts[0] == allContacts[0]) {
                            if (appUsers.isEmpty()) {
                                materialDialogBuilder3.dismiss();
                                new MaterialDialog.Builder(MainActivity.this)
                                        .title(R.string.sorry)
                                        .content(R.string.no_friends_swipe_down)
                                        .cancelable(true)
                                        .positiveText(R.string.got_it)
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .show();
                            } else {
                                saveToFireStore(appUsers);
                                if (MainActivity.this.isFinishing()) {
                                    return;
                                }
                                dismissProgressDialog();
                            }
                            //System.out.println("all complete ");
                        }
                    }
                }
            });
        }
        //end of the for loop
    }

    @NonNull
    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        //search string is mSearchString
        mSelectionArgs[0] = "%" + mSearchString + "%";
        MaterialDialog.Builder builder = new MaterialDialog.Builder(MainActivity.this)
                .title(R.string.Please_wait)
                .content(R.string.build_contact_list)
                .progress(true, 0)
                .cancelable(false);
        materialDialogBuilder1 = builder.build();
        materialDialogBuilder1.show();
        // Starts the query
        return new CursorLoader(
                this,
                //ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                ContactsContract.Contacts.CONTENT_URI,
                null,
                MAIN_SELECTION,
                mSelectionArgs,
                //ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        );
    }

    @Override
    public void onLoadFinished(@NonNull android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        ArrayList<DatabaseContacts> contacts = new ArrayList<>();
        if (data != null) {
            (new sortContacts(data, contacts)).execute("main");
        } else {
            Toast.makeText(MainActivity.this, R.string.contacts_problem, Toast.LENGTH_SHORT).show();
        }
    }

    private void dismissProgressDialog() {
        System.out.println("dismiss progress called");
        if (materialDialogBuilder1 != null && materialDialogBuilder1.isShowing()) {
            materialDialogBuilder1.dismiss();
        }
        if (materialDialogBuilder2 != null && materialDialogBuilder2.isShowing()) {
            materialDialogBuilder2.dismiss();
        }
        if (materialDialogBuilder3 != null && materialDialogBuilder3.isShowing()) {
            materialDialogBuilder3.dismiss();
        }
    }

    private class sortContacts extends AsyncTask<String, Void, String> {
        Cursor data;
        ArrayList<DatabaseContacts> users;

        sortContacts(Cursor data, ArrayList<DatabaseContacts> users) {
            this.data = data;
            this.users = users;
        }

        @Override
        protected String doInBackground(String... strings) {
            if (data != null && data.getCount() > 0) {
                Cursor cursor = data;
                cursor.moveToFirst();
                do {
                    String phoneNumber = "";
                    if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(
                            ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                        String id = cursor.getString(cursor.getColumnIndex(
                                ContactsContract.Contacts._ID));
                        String name = cursor.getString(cursor.getColumnIndex(
                                ContactsContract.Contacts.DISPLAY_NAME));
                        Cursor pCur = cr.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                        + " = ?", new String[]{id}, null);
                        int i = 0;
                        int pCount = pCur.getCount();
                        String[] phoneNum = new String[pCount];
                        String[] phoneType = new String[pCount];
                        while (pCur != null && pCur.moveToNext()) {
                            phoneNum[i] = pCur.getString(pCur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));
                            phoneType[i] = pCur.getString(pCur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.TYPE));
                            i++;
                        }
                        pCur.close();
                        //change this to check if its a mobile number or not
                        phoneNumber = phoneNum[0];
                    }
                    DatabaseContacts contacts = new DatabaseContacts();
                    contacts.setUser_name(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
                    contacts.setUser_status("");
                    contacts.setUser_id(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID)));
                    contacts.setUser_number(phoneNumber.replaceAll("\\s", "").trim());
                    contacts.setUser_image(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)));
                    contacts.setUser_time_stamp(new Date());
                    contacts.setUser_recent_message("");
                    contacts.setMsg_time_stamp(new Date());
                    contacts.setBlocked(false);
                    contacts.setUnread(0);

                    //CHECK IF CONTACT IS ALREADY IN THE DATABASE
                    //CHECK IF CONTACT IS THE USER
                    if (!contacts.getUser_number().isEmpty()) {
                        if (contacts.getUser_number().charAt(0) == '0') {
                            contacts.setUser_number(contacts.getUser_number().replaceFirst("0", "+44"));
                        }
                        if (!contacts.getUser_number().equals(usersNumber)) {
                            users.add(contacts);
                        } else {
                        }
                    }
                } while (cursor.moveToNext());
            }
            return strings[0];
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //cursor.close();
            SharedPreferences pref = getApplicationContext().getSharedPreferences(Constants.SHARED_PREFS, 0);
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(Constants.NUM_OF_CONTACTS, users.size());
            editor.apply();
            if (MainActivity.this.isFinishing()) {
                return;
            }
            dismissProgressDialog();
            checkWithFirestore(users);
        }
    }

    @Override
    public void onLoaderReset(@NonNull android.support.v4.content.Loader<Cursor> loader) {

    }


    /**
     * Subscribe to standby channel so that it doesn't interfere with the WebRTC Signaling.
     */
    /*
    private void initPubNub() {
        mPubNub = new Pubnub(Constants.PUB_KEY, Constants.SUB_KEY);
        mPubNub.setUUID(userId);
        subscribeStdBy();
    }
    */
    /**
     * Subscribe to standby channel
     */
    /*
    private void subscribeStdBy() {
        try {
            mPubNub.subscribe(stdByChannel, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.d("MA-iPN", "MESSAGE: " + message.toString());
                    if (!(message instanceof JSONObject)) return; // Ignore if not JSONObject
                    JSONObject jsonMsg = (JSONObject) message;
                    try {
                        if (!jsonMsg.has(Constants.JSON_CALL_USER))
                            return;     //Ignore Signaling messages.
                        String user = jsonMsg.getString(Constants.JSON_CALL_USER);
                        dispatchIncomingCall(user);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void connectCallback(String channel, Object message) {
                    Log.d("MA-iPN", "CONNECTED: " + message.toString());
                    setUserStatus(Constants.STATUS_AVAILABLE);
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    Log.d("MA-iPN", "ERROR: " + error.toString());
                }
            });
        } catch (PubnubException e) {
            Log.d("HERE", "HERE");
            e.printStackTrace();
        }
    }
    */
    public void makeCall(View view,String callNum){
        if (callNum.isEmpty() || callNum.equals(userId)){
            showToast("Enter a valid user ID to call.");
            return;
        }
        //dispatchCall(callNum);
    }


    /**
     * TODO: Debate who calls who. Should one be on standby? Or use State API for busy/available
     * Check that user is online. If they are, dispatch the call by publishing to their standby
     * channel. If the publish was successful, then change activities over to the video chat.
     * The called user will then have the option to accept of decline the call. If they accept,
     * they will be brought to the video chat activity as well, to connect video/audio. If
     * they decline, a hangup will be issued, and the VideoChat adapter's onHangup callback will
     * be invoked.
     *
     * @param callNum Number to publish a call to.
     */
    /*
    public void dispatchCall(final String callNum) {
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
            final String callNumStdBy = callNum + Constants.STDBY_SUFFIX;
            LiveDbOpenHelper dbUserOpenHelper = LiveDatabaseBuilder.getUserDatabase(MainActivity.this);
            DatabaseUser databaseUsers = dbUserOpenHelper.userDaoLive().returnUserById(userId);
            String name = databaseUsers.getUser_name();
            mPubNub.hereNow(callNumStdBy, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.d("MA-dC", "HERE_NOW: " + " CH - " + callNumStdBy + " " + message.toString());
                    try {
                        int occupancy = ((JSONObject) message).getInt(Constants.JSON_OCCUPANCY);
                        if (occupancy == 0) {
                            showToast("User is not online!");
                            return;
                        }
                        JSONObject jsonCall = new JSONObject();
                        jsonCall.put(Constants.JSON_CALL_USER_ID, userId);
                        jsonCall.put(Constants.JSON_CALL_TIME, System.currentTimeMillis());
                        mPubNub.publish(callNumStdBy, jsonCall, new Callback() {
                            @Override
                            public void successCallback(String channel, Object message) {
                                Log.d("MA-dC", "SUCCESS: " + message.toString());
                                Intent intent = new Intent(MainActivity.this, VideoChatActivity.class);
                                intent.putExtra(Constants.USER_NAME, name);
                                intent.putExtra(Constants.CALL_USER_ID, callNum);  // Only accept from this number?
                                intent.putExtra(Constants.USER_NUMBER, userId);
                                intent.putExtra("dialed", true);
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
    */
    /**
     * Handle incoming calls.
     *
     * @param userId
     */
    /*
    private void dispatchIncomingCall(String userId) {
        //showToast("Call from: " + userId);
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

            Intent intent = new Intent(MainActivity.this, IncomingCallActivity.class);
            intent.putExtra(Constants.USER_NAME, user.getUser_name());
            intent.putExtra(Constants.CALL_USER_ID, user.getUser_id());
            intent.putExtra(Constants.USER_NUMBER, user.getUser_number());
            startActivity(intent);
        }
    }
    */
    /*
    private void setUserStatus(String status) {
        try {
            JSONObject state = new JSONObject();
            state.put(Constants.JSON_STATUS, status);
            mPubNub.setState(stdByChannel, userId, state, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.d("MA-sUS", "State Set: " + message.toString());
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getUserStatus(String userId) {
        String stdByUser = userId + Constants.STDBY_SUFFIX;
        mPubNub.getState(stdByUser, userId, new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                Log.d("MA-gUS", "User Status: " + message.toString());
            }
        });
    }
    */
    /**
     * Ensures that toast is run on the UI thread.
     *
     * @param message
     */
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Log out, remove username from SharedPreferences, unsubscribe from PubNub, and send user back
     * to the LoginActivity
     */
    public void signOut() {
        /*
        this.mPubNub.unsubscribeAll();
        SharedPreferences.Editor edit = this.mSharedPreferences.edit();
        edit.remove(Constants.USER_NAME);
        edit.apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("oldUsername", this.username);
        startActivity(intent);
        */
    }

    public String getSmsTodayYestFromMilli(long msgTimeMillis) {

        Calendar messageTime = Calendar.getInstance();
        messageTime.setTimeInMillis(msgTimeMillis);
        // get Currunt time
        Calendar now = Calendar.getInstance();

        final String strTimeFormate = "h:mm aa";
        final String strDateFormate = "dd/MM/yyyy h:mm aa";

        if (now.get(Calendar.DATE) == messageTime.get(Calendar.DATE)
                &&
                ((now.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH)))
                &&
                ((now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)))
                ) {

            return "today at " + DateFormat.format(strTimeFormate, messageTime);

        } else if (
                ((now.get(Calendar.DATE) - messageTime.get(Calendar.DATE)) == 1)
                        &&
                        ((now.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH)))
                        &&
                        ((now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)))
                ) {
            return "yesterday at " + DateFormat.format(strTimeFormate, messageTime);
        } else {
            return "date : " + DateFormat.format(strDateFormate, messageTime);
        }
    }

    private void askForContactPermission(boolean navigate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.READ_CONTACTS)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.contacts_access);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setMessage(R.string.confirm_contact_access);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @TargetApi(Build.VERSION_CODES.M)
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            requestPermissions(new String[]
                                    {android.Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_CONTACT);
                        }
                    });
                    builder.show();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.READ_CONTACTS},
                            PERMISSION_REQUEST_CONTACT);
                }
            } else {
                if (navigate) {
                    startActivity(new Intent(MainActivity.this, ContactsActivity.class));
                    nav = false;
                } else {
                    getContactsFrom();
                }
            }
        } else {
            //getContact();
            if (navigate) {
                startActivity(new Intent(MainActivity.this, ContactsActivity.class));
                nav = false;
            } else {
                getContactsFrom();
            }

        }
    }

    private Runnable changeMessage = new Runnable() {
        @Override
        public void run() {
            materialDialogBuilder3.setContent(materialDialogBuilder3Message);
        }
    };

    private Runnable changeMessage2 = new Runnable() {
        @Override
        public void run() {
            materialDialogBuilder2.setContent(materialDialogBuilder2Message);
        }
    };

    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CONTACT: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (nav) {
                        startActivity(new Intent(MainActivity.this, ContactsActivity.class));
                        nav = false;
                    } else {
                        getContactsFrom();
                    }
                } else {
                    Toast.makeText(this, R.string.no_permissions, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case 0: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, R.string.no_permissions, Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment f : fragments) {
                if (f instanceof IntroFrag) {
                    f.onActivityResult(requestCode, resultCode, data);
                } else if (f instanceof StatusFragment) {
                    f.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
        if (requestCode == SIGN_IN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this,
                        R.string.sign_in_success,
                        Toast.LENGTH_LONG)
                        .show();
                //displayChatMessages();
            } else {
                Toast.makeText(this,
                        R.string.sign_in_failed,
                        Toast.LENGTH_LONG)
                        .show();
                // Close the app
                finish();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String current = CURRENT_FRAG;
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(current);
        if (!current.isEmpty()) {
            outState.putString(TAG, current);
        }
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, KEY, fragment);
        }
    }
}
