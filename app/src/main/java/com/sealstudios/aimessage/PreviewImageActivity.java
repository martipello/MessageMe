package com.sealstudios.aimessage;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sealstudios.aimessage.Database.DatabaseMessage;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.adapters.CustomPagerAdapter;

import java.util.ArrayList;


public class PreviewImageActivity extends AppCompatActivity {

    private String userId;
    private String recipientId;
    private String msgId;
    private FirebaseFirestore db;
    private String stringRef = Constants.STORAGE_REF;
    private FirebaseStorage storage;
    private int position = 0;
    private StorageReference storageReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview_image_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReferenceFromUrl(stringRef);
        final Bundle data = getIntent().getExtras();
        ViewPager viewPager = findViewById(R.id.viewpager);
        if (data != null){
            userId = data.getString(Constants.USER_NAME);
            recipientId = data.getString(Constants.DB_USER_NAME);
            msgId = data.getString(Constants.JSON_MSG_UUID);
        }

        ArrayList<DatabaseMessage> userMessagesWithImages = new ArrayList<>();
        final CollectionReference userRef = db.collection(Constants.USERS)
                .document(userId)
                .collection(Constants.CONTACTS).document(recipientId).collection(Constants.MESSAGES);
        Query query = userRef.orderBy(Constants.MSG_TIME_STAMP).limit(50L);
        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    // Handle error
                    //...
                    //return;
                }
                // Convert query snapshot to a list of chats
                //messageList = snapshot.toObjects(UserMessage.class);
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    DatabaseMessage userMessage = document.toObject(DatabaseMessage.class);
                    if (userMessage.getData_type().equals(Constants.DATA_TYPE_IMAGE)){
                        userMessagesWithImages.add(userMessage);
                    }
                }
                for (int i = 0; i < userMessagesWithImages.size(); i++){
                    if (userMessagesWithImages.get(i).getMessageId().equals(msgId)){
                        position = i;
                    }
                }
                viewPager.setAdapter(new CustomPagerAdapter(PreviewImageActivity.this,userMessagesWithImages));
                viewPager.setCurrentItem(position);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
