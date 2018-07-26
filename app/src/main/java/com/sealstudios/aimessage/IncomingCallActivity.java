package com.sealstudios.aimessage;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.sealstudios.aimessage.Database.DatabaseCalls;
import com.sealstudios.aimessage.Database.DatabaseContacts;
import com.sealstudios.aimessage.Database.DatabaseMessage;
import com.sealstudios.aimessage.Database.DatabaseUser;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.ViewModels.CallsViewModel;
import com.sealstudios.aimessage.ViewModels.ContactsViewModel;
import com.sealstudios.aimessage.ViewModels.MessagesViewModel;
import com.sealstudios.aimessage.ViewModels.MessagesViewModelFactory;
import com.sealstudios.aimessage.ViewModels.UserViewModel;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;
import me.kevingleason.pnwebrtc.PnPeerConnectionClient;

public class IncomingCallActivity extends AppCompatActivity {
    private String callUserName;
    private String userName;
    private String userId;
    private String callUserId;
    private Pubnub mPubNub;
    private TextView mCallerID;
    private String callId;
    private String stringRef = Constants.STORAGE_REF;
    private FirebaseStorage storage;
    private DatabaseContacts caller;
    private DatabaseCalls calls;
    private DatabaseMessage message;
    CircleImageView icon;
    ContactsViewModel contactsViewModel;
    CallsViewModel callsViewModel;
    MessagesViewModel messagesViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        Bundle extras = getIntent().getExtras();
        Log.d("VC-IncClAct", "incoming call called");
        if (extras == null || !extras.containsKey(Constants.CALL_USER_ID)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            Toast.makeText(this, this.getResources().getString(R.string.oops), Toast.LENGTH_SHORT).show();
            /*
            Toast.makeText(this, "Need to pass callUserName to IncomingCallActivity in intent extras (Constants.CALL_USER).",
                    Toast.LENGTH_SHORT).show();
                   */
            finish();
            return;
        }
        callsViewModel = ViewModelProviders.of(this).get(CallsViewModel.class);
        storage = FirebaseStorage.getInstance();

        Calendar cal = Calendar.getInstance();
        callId = cal.getTime().toString();
        userId = extras.getString(Constants.USER_ID);
        userName = extras.getString(Constants.USER_NAME);
        callUserId = extras.getString(Constants.CALL_USER_ID);
        callUserName = extras.getString(Constants.CALL_USER_NAME);


        Log.d("VC-IncmngCllAct","USER_NAME " + userName + " USER_ID " + userId
                + " CALL_USER_ID " + callUserId + " CALL_USER_NAME " + callUserName);

        contactsViewModel = ViewModelProviders.of(this).get(ContactsViewModel.class);
        messagesViewModel = ViewModelProviders.of(this,
                new MessagesViewModelFactory(this.getApplication(), callUserId)).get(MessagesViewModel.class);
        mCallerID = findViewById(R.id.caller_id);
        calls = new DatabaseCalls();
        message = new DatabaseMessage();
        icon = findViewById(R.id.icon);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference userRef = db.collection(Constants.USERS).document(callUserId);
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        caller = document.toObject(DatabaseContacts.class);
                        contactsViewModel.insertMyContact(caller);
                        populateViews(caller);
                    } else {
                    }
                }
            }
        });
        ///populateViews(caller);

        this.mPubNub = new Pubnub(Constants.PUB_KEY, Constants.SUB_KEY);
        this.mPubNub.setUUID(this.userId);
    }

    private void populateViews(DatabaseContacts databaseContacts) {
        mCallerID.setText(databaseContacts.getUser_name());
        //TODO change this to small image
        Glide.with(IncomingCallActivity.this)
                .load(databaseContacts.getUser_image())
                .apply(new RequestOptions()
                        .dontAnimate().placeholder(R.drawable.contact_placeholder))
                .into(icon);
    }

    private DatabaseMessage createMessage(String callerId, String callerName, String calledId, String calledName, String message_Text, String id) {
        /*
        message text is going to be the call state
         */
        Calendar cal = Calendar.getInstance();

        DatabaseMessage databaseMessage = new DatabaseMessage();
        databaseMessage.setMessageId(id);
        databaseMessage.setSenderId(callerId);
        databaseMessage.setMessage(message_Text);
        databaseMessage.setTime_stamp(cal.getTime());
        databaseMessage.setData_type(Constants.DATA_TYPE_CALL);
        databaseMessage.setData_url("");
        databaseMessage.setRecipientId(calledId);
        databaseMessage.setSenderName(callerName);
        databaseMessage.setSent_received(0);
        databaseMessage.setRecipientName(calledName);
        return databaseMessage;
    }

    private DatabaseCalls createCall(String callerId, String callerName, String calledId, String calledName, String status, String id) {
        calls.setCall_id(id);
        calls.setCall_time_stamp(new Date());
        calls.setCall_caller_id(callerId);
        calls.setCall_caller_name(callerName);
        calls.setCall_called_id(calledId);
        calls.setCall_called_name(calledName);
        calls.setCall_status(status);
        return calls;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_blank, menu);
        return true;
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

    public void acceptCall(View view) {
        /*
        b.putString(Constants.USER_NAME, senderUser.getUser_name());
            b.putString(Constants.USER_ID, userId);
            b.putString(Constants.CALL_USER_ID, mUserId);
            b.putString(Constants.CALL_USER_NAME,recipientName);
         */
        Intent intent = new Intent(IncomingCallActivity.this, VideoChatActivity.class);
        Bundle b = new Bundle();
        b.putString(Constants.USER_NAME, userName);
        b.putString(Constants.USER_ID, userId);
        b.putString(Constants.CALL_USER_ID, callUserId);
        b.putString(Constants.CALL_USER_NAME, callUserName);
        b.putBoolean("dialed", false);
        intent.putExtras(b);
        DatabaseCalls accepted = createCall(callUserId, callUserName, userId, userName, Constants.CALL_RECEIVED, callId);
        callsViewModel.insertMyCall(accepted);
        startActivity(intent);
    }

    /**
     * Publish a hangup command if rejecting call.
     *
     * @param view
     */
    public void rejectCall(View view) {
        JSONObject hangupMsg = PnPeerConnectionClient.generateHangupPacket(this.callUserId);
        this.mPubNub.publish(this.callUserId, hangupMsg, new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                DatabaseCalls rejectedCall = createCall(callId, callUserName, userId, userName, Constants.CALL_REJECTED, callId);
                DatabaseMessage rejectMessage = createMessage(callId, callUserName, userId, userName, Constants.CALL_REJECTED, callId);
                callsViewModel.insertMyCall(rejectedCall);
                //TODO send a message
                messagesViewModel.insertMessageOnline(rejectMessage);
                Log.d("VC-IncmngCllAct", "rejectCall");
                Intent i = new Intent(IncomingCallActivity.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mPubNub != null) {
            this.mPubNub.unsubscribeAll();
        }
    }
}
