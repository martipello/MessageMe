package com.sealstudios.aimessage.ViewModels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import com.sealstudios.aimessage.Database.DatabaseUser;
import com.sealstudios.aimessage.Database.UserRepository;

import java.util.List;
import java.util.concurrent.ExecutionException;


public class UserViewModel extends AndroidViewModel{
    private UserRepository userRepository;
    private LiveData<List<DatabaseUser>> user;
    private String mUserId;

    public UserViewModel(Application application){
        super(application);
        userRepository = new UserRepository(application);
        user = userRepository.getMyUser();
    }

    public LiveData<List<DatabaseUser>> getMyUsers(){
        return userRepository.getMyUser();
    }

    public LiveData<DatabaseUser> getUserById(String id){
        return userRepository.getUserById(id);
    }

    public LiveData<DatabaseUser> getById(String id){
        return userRepository.getById(id);
    }

    public void removeListener(){
        userRepository.removeListener();
    }

    public List<DatabaseUser> getUserByName(String name) throws ExecutionException, InterruptedException {
        return  userRepository.getUserByName(name);
    }

    public void insertMyUser(DatabaseUser contact, String token){
        userRepository.insertUser(contact,token);
    }

    public void updateMyUser(DatabaseUser contact,String token){
        userRepository.updateUser(contact, token);
    }

    public void deleteMyUser(DatabaseUser contact){
        userRepository.deleteUser(contact);
    }
}
