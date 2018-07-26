package com.sealstudios.aimessage.Utils;

public class Constants {
    //constants for shared prefs
    public static final String SHARED_PREFS = "aimessage.SHARED_PREFS";
    public static final String USER_NAME    = "aimessage.SHARED_PREFS.USER_NAME";
    public static final String USER_NUMBER    = "aimessage.SHARED_PREFS.USER_NUMBER";
    public static final String CALL_USER_ID    = "aimessage.SHARED_PREFS.CALL_USER_ID";
    public static final String CALL_USER_NAME    = "aimessage.SHARED_PREFS.CALL_USER_NAME";
    public static final String STDBY_SUFFIX = "-stdby";
//constnats for video calls
    public static final String PUB_KEY = "pub-c-3d403f21-b5c4-4645-b9a8-7e1276815987"; // Your Pub Key
    public static final String SUB_KEY = "sub-c-eb49b30a-5519-11e8-aac2-e2d70733f5dd"; // Your Sub Key

    public static final String JSON_CALL_USER_ID = "call_user_id";
    public static final String JSON_CALL_USER_NAME = "call_user_name";
    public static final String JSON_CALL_TIME = "call_time";
    public static final String JSON_OCCUPANCY = "occupancy";
    public static final String JSON_STATUS    = "status";

    // JSON for user messages
    public static final String JSON_USER_MSG  = "user_message";
    public static final String JSON_MSG_UUID  = "msg_uuid";
    public static final String JSON_MSG       = "msg_message";
    public static final String JSON_TIME      = "msg_timestamp";

    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_OFFLINE   = "Offline";
    public static final String STATUS_BUSY      = "Busy";

    //DATABASE constants
    public static final String DB_USER_NAME = "user_db";
    //PREFERENCE constants
    public static final String FIRST_TIME = "first_time";
    public static final String NUM_OF_CONTACTS = "Available";

    public static final String MUTED_CONVOS = "muted_conversations";
    //constants for fragments
    public static final String STATUS_FRAG = "status_fragment";
    public static final String CONVO_FRAG = "conversations_fragment";
    //firebase user object constants
    public static final String USERS = "users";
    public static final String CALLS = "calls";
    public static final String CONTACTS = "contacts";
    public static final String DATA_TYPE_TEXT = "TEXT";
    public static final String DATA_TYPE_IMAGE = "IMAGE";
    public static final String DATA_TYPE_CALL = "CALL";
    //firebase contacts constants
    public static final String FS_NAME = "user_name";
    public static final String FS_NUMBER = "user_number";
    public static final String FS_ID = "user_id";
    public static final String FS_STATUS = "user_status";
    public static final String FS_IMAGE = "user_image";
    public static final String FS_SMALL_IMAGE = "user_small_image";
    public static final String FS_TIME_STAMP = "user_time_stamp";
    public static final String FS_RECENT_MSG = "user_recent_message";
    public static final String FS_MSG_TIME_STAMP = "msg_time_stamp";
    public static final String FS_TOKEN = "registeredToken";
    public static final String FS_BLOCKED = "blocked";
    public static final String FS_UNREAD = "unread";
    //firebase messages constants
    public static final String MESSAGES = "messages";
    public static final String MSG_TIME_STAMP = "time_stamp";
    public static final String MSG_SENT_RECEIVED = "sent_received";
    public static final String MSG_ID = "messageId";
    public static final String MSG_DATA_TYPE = "data_type";
    public static final String MSG_DATA_URL = "data_url";
    public static final String MSG_SENDER = "senderId";
    public static final String MSG_TEXT = "message";
    public static final String MSG_SENDER_NAME = "senderName";
    public static final String MSG_RECIPIENT = "recipientId";
    public static final String MSG_RECIPIENT_NAME = "recipientName";
    //firebase calls constants
    public static final String CALL_ID = "call_id";
    public static final String CALL_CALLER_NAME = "call_caller_name";
    public static final String CALL_CALLER_NUMBER = "call_caller_number";
    public static final String CALL_CALLER_ID = "call_caller_id";
    public static final String CALL_TIME_STAMP = "call_time_stamp";
    public static final String CALL_CALLED_NAME = "call_called_name";
    public static final String CALL_CALLED_ID = "call_called_id";
    public static final String CALL_CALLED_NUMBER = "call_called_number";
    public static final String CALL_STATUS = "call_status";

    public static final String CALL_RECEIVED = "call_received";
    public static final String CALL_MISSED = "call_missed";
    public static final String CALL_MADE = "call_made";
    public static final String CALL_REJECTED = "call_rejected";
    public static final String CALL_INCOMING = "incoming";
    public static final String CALL_OUTGOING = "outgoing";

    public static final String CLICK_ACTION = "messagelist_activity";

    //storage constants
    public static final String STORAGE_REF = "gs://aimessage-b2502.appspot.com";
    public static final String PROFILE_PICS = "profile_pics";
    public static final String PROFILE_IMAGE = "profile_image";
    public static final String IMAGE = "image";


    public static final String SENDER_ID = "sender";
    public static final String RECIPIENT_ID = "recipient";

    public static final String USER_ID = "_id";
    public static final String TOKEN = "token";
    public static final String NUMBER = "number";
    public static final String VERIFICATION = "verificationId";

    public static final String CAM_TAG_DEFAULT = "DEFAULT";
    public static final String CAM_TAG_EXISTING = "EXISTING";
    public static final String CAM_TAG_CHANGED = "CHANGED";
    public static final String ACTIVE = "active";
    public static final String ACTIVE_USER = "active_user";
    public static final String SIGNED_IN = "signed_in";
    public static final String USER_NOTIFICATION_ID = "notification_id";

    public static final String ACTIVITY_FLAG = "activity_flag";


    public static final String APP_PROVIDER = "com.sealstudios.aimessage.provider";
}
