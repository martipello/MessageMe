package com.sealstudios.aimessage.ViewModels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.content.SharedPreferences;

import com.sealstudios.aimessage.Database.DatabaseMessage;
import com.sealstudios.aimessage.Database.MessageRepository;
import com.sealstudios.aimessage.Utils.Constants;

import java.util.List;


public class MessagesViewModel extends ViewModel {
    private MessageRepository messageRepository;
    private LiveData<List<DatabaseMessage>> messages;
    private MutableLiveData<String> messageText;
    private String mRecipientId;
    private String userId;


    public MessagesViewModel(Application application,String recipientId){
        //super(application);
        SharedPreferences pref = application.getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
        //recipientId = pref.getString(Constants.ACTIVE_USER, "recipientId");
        userId = pref.getString(Constants.FS_ID, "userId");
        messageRepository = new MessageRepository(application,recipientId);
        mRecipientId = recipientId;
        messageText = new MutableLiveData<>();
        messages = messageRepository.getAllMessages(userId , recipientId);
        /*
        messages = Transformations.switchMap(messageText, id ->
                messageRepository.getAllByMessageText(id));
                */
    }

    public LiveData<List<DatabaseMessage>> autoAllMessages(){
        return messageRepository.getAllMessages(userId, mRecipientId);
    }

    public LiveData<List<DatabaseMessage>> getAllMessages(String sId, String rId){
        return messageRepository.getAllMessages(sId, rId);
    }

    public LiveData<List<DatabaseMessage>> getMessagesById(String id){
        return messageRepository.getAllMessagesById(userId,id);
    }

    public List<DatabaseMessage> getUnreadMessagesById(String recipientId, String senderId){
        return messageRepository.getAllUnreadById(recipientId,senderId);
    }

    public void setSearchText(String text) {
        if (text != null)
            this.messageText.setValue(text);
    }

    public void markAllAsRead(String senderId, String recipientId){
        messageRepository.markAllAsRead(recipientId,senderId);
    }

    public void removeListener(){
        messageRepository.removeListener();
    }

    public LiveData<List<DatabaseMessage>> getMessagesByName(String name){
        return messageRepository.getAllMessagesByName(name);
    }

    public void insertMessage(DatabaseMessage message){
        messageRepository.insertMessage(message);
    }

    public void insertMessageOnline(DatabaseMessage message){
        messageRepository.insertMessageOnline(message);
    }

    public void insertDataMessageOnline(DatabaseMessage databaseMessage){
        messageRepository.insertDataMessageOnline(databaseMessage);
    }

    public void updateMessage(DatabaseMessage msg){
        messageRepository.updateMessage(msg);
    }

    public void deleteMessage(DatabaseMessage msg){
        messageRepository.deleteMessage(msg);
    }
}
