package com.sealstudios.aimessage.ViewModels;

import android.app.Application;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

/**
 * Created by marti on 23/07/2018.
 */

    public class MessagesViewModelFactory extends ViewModelProvider.NewInstanceFactory {
        private Application mApplication;
        private String mParam;


        public MessagesViewModelFactory(Application application, String param) {
            mApplication = application;
            mParam = param;
        }


        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new MessagesViewModel(mApplication, mParam);
        }
    }