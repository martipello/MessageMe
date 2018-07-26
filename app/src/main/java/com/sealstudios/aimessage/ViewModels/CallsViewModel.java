package com.sealstudios.aimessage.ViewModels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;

import com.sealstudios.aimessage.Database.CallObject;
import com.sealstudios.aimessage.Database.CallsRepository;
import com.sealstudios.aimessage.Database.DatabaseCalls;

import java.util.List;



public class CallsViewModel extends AndroidViewModel{
    private CallsRepository callsRepository;
    private LiveData<List<DatabaseCalls>> call;
    private MutableLiveData<String> name;
    private String mUserId;

    public CallsViewModel(Application application){
        super(application);
        callsRepository = new CallsRepository(application);
        name = new MutableLiveData<>();
        call = callsRepository.getAllCalls();
        call = Transformations.switchMap(name, id ->
                callsRepository.getCallsByName(id));
    }

    public LiveData<List<DatabaseCalls>> getAllCalls(){
        return callsRepository.getAllCalls();
    }

    public LiveData<DatabaseCalls> getCallsById(String id){
        return callsRepository.getCallById(id);
    }

    public LiveData<List<CallObject>> getCallsWithImage(){
        return callsRepository.getCallsWithImage();
    }

    public void setUserName(String userName) {
        if (userName != null)
            this.name.setValue(userName);
    }

    public void removeListener(){
        callsRepository.removeListener();
    }

    public LiveData<List<DatabaseCalls>> getCallsByName(String name){
        return callsRepository.getCallsByName(name);
    }

    public void insertMyCall(DatabaseCalls call){
        callsRepository.insertCall(call);
    }

    public void updateMyCall(DatabaseCalls call){
        callsRepository.updateCall(call);
    }

    public void deleteMyCall(DatabaseCalls call){
        callsRepository.deleteCall(call);
    }
}
