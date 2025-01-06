package com.example.offmap.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.offmap.services.SQLiteManager;

public class LoginViewModelFactory implements ViewModelProvider.Factory {

    private final SQLiteManager sqLiteManager;

    public LoginViewModelFactory(SQLiteManager sqLiteManager) {
        this.sqLiteManager = sqLiteManager;
    }

    /** @noinspection unchecked*/
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(sqLiteManager);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
