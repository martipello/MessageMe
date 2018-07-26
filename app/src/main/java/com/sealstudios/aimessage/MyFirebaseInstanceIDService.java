package com.sealstudios.aimessage;

import android.support.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.sealstudios.aimessage.Utils.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens for changes in the InstanceID
 */
public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private FirebaseFirestore db;
    //DaoSession daoSession;

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        if (FirebaseAuth.getInstance().getCurrentUser() != null){
            sendRegistrationToServer(refreshedToken);
        }
    }

    private void sendRegistrationToServer(String token) {
        if (token == null)
            throw new NullPointerException("token is null");
        else{
            FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            String userId = currentFirebaseUser.getUid();
            CollectionReference customers = db.collection(Constants.USERS);
            Map<String, Object> user = new HashMap<>();
            user.put(Constants.FS_TOKEN,token);
            customers.document(userId).update(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                }
            });
        }
    }
}
