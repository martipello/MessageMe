package com.sealstudios.aimessage.ViewModels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;

import com.sealstudios.aimessage.Database.ContactRepository;
import com.sealstudios.aimessage.Database.DatabaseContacts;

import java.util.List;

/**
 * Created by marti on 29/06/2018.
 */

public class ContactsViewModel extends AndroidViewModel{
    private ContactRepository contactRepository;
    private LiveData<List<DatabaseContacts>> contacts;
    private MutableLiveData<String> name;
    private String mUserId;

    public ContactsViewModel(Application application){
        super(application);
        contactRepository = new ContactRepository(application);
        name = new MutableLiveData<>();
        contacts = contactRepository.getAllContacts();
        contacts = Transformations.switchMap(name, id ->
                contactRepository.getContactByName(id));
    }

    public LiveData<List<DatabaseContacts>> getAllContacts(){
        return contactRepository.getAllContacts();
    }

    public LiveData<DatabaseContacts> getContactById(String id){
        return contactRepository.getContactById(id);
    }

    public void setUserName(String userName) {
        if (userName != null)
            this.name.setValue(userName);
    }

    public void removeListener(){
        contactRepository.removeListener();
    }

    public LiveData<List<DatabaseContacts>> getContactByName(String name){
        return  contactRepository.getContactByName(name);
    }

    public void insertMyContact(DatabaseContacts contact){
        contactRepository.insertContact(contact);
    }

    public void updateMyContact(DatabaseContacts contact){
        contactRepository.updateContact(contact);
    }

    public void deleteMyContact(DatabaseContacts contact){
        contactRepository.deleteContact(contact);
    }
}
