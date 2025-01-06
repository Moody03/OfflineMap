package com.example.offmap.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.offmap.services.SQLiteManager;

public class SignupViewModelFactory implements ViewModelProvider.Factory {

    private final SQLiteManager sqLiteManager;

    public SignupViewModelFactory(SQLiteManager sqLiteManager) {
        this.sqLiteManager = sqLiteManager;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked") // Suppress the unchecked cast warning
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SignupViewModel.class)) {
            return (T) new SignupViewModel(sqLiteManager);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
