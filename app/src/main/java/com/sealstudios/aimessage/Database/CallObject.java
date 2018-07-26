package com.sealstudios.aimessage.Database;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import com.sealstudios.aimessage.Utils.Constants;

import java.util.List;

/**
 * Created by marti on 22/07/2018.
 */

public class CallObject {
    @Embedded
    private DatabaseCalls call;

    @Relation(parentColumn = Constants.CALL_ID, entityColumn = Constants.FS_ID, entity = DatabaseContacts.class, projection = Constants.FS_IMAGE)
    public List<String> profileImages;

    public CallObject(){
    }

    public DatabaseCalls getCall() {
        return call;
    }

    public void setCall(DatabaseCalls call) {
        this.call = call;
    }

    public List<String> getProfileImage() {
        return profileImages;
    }

    public void setProfileImage(List<String> profileImages) {
        this.profileImages = profileImages;
    }


    @Override
    public boolean equals(Object otherUserObject) {
        if (!(otherUserObject instanceof CallObject)) {
            return false;
        }
        CallObject that = (CallObject) otherUserObject;
        if (this.call == null && that.call == null){
            return false;
        }
        return this.call.getCall_id().equals(that.call.getCall_id())
                && this.call.getCall_caller_id().equals(that.call.getCall_caller_id())
                && this.call.getCall_called_id().equals(that.call.getCall_called_id())
                && this.call.getCall_caller_name().equals(that.call.getCall_caller_name())
                && this.call.getCall_called_name().equals(that.call.getCall_called_name())
                && this.call.getCall_status().equals(that.call.getCall_status())
                ;

    }


    public static class callList {
        private List<CallObject> callsList;
        public List<CallObject> getCallList() {
            return callsList;
        }
    }
}
