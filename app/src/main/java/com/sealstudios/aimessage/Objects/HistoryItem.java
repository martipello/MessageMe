package com.sealstudios.aimessage.Objects;


public class HistoryItem {
    private ChatUser user;
    private Long timeStamp;

    public HistoryItem(ChatUser user, Long timeStamp){
        this.user=user;
        this.timeStamp=timeStamp;
    }

    public ChatUser getUser() {
        return user;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }
}