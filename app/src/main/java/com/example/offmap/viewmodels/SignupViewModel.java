package com.example.offmap.viewmodels;

import android.util.Base64;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.offmap.models.User;
import com.example.offmap.services.SQLiteManager;
import java.security.SecureRandom;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class SignupViewModel extends ViewModel {

    private static final String TAG = "SignupViewModel";

    private final SQLiteManager sqLiteManager;

    public SignupViewModel(SQLiteManager sqLiteManager) {
        this.sqLiteManager = sqLiteManager;
    }

    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> userCreationSuccess = new MutableLiveData<>();

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<Boolean> getUserCreationSuccess() {
        return userCreationSuccess;
    }

    public void signUp(String fullName, String email, String password) {
        try {
            String hashedPassword = hashPassword(password);

            // Create user object
            User user = new User(generateUid(), fullName, email);

            // Save user to SQLite
            boolean isSaved = sqLiteManager.saveUser(user, hashedPassword);
            if (isSaved) {
                Log.d(TAG, "User saved successfully");
                toastMessage.postValue("Account created successfully.");
                userCreationSuccess.postValue(true);
            } else {
                toastMessage.postValue("Error saving user data.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Sign-up error: ", e);
            toastMessage.postValue("Unexpected error occurred. Try again.");
        }
    }

    private String hashPassword(String password) throws Exception {
        byte[] salt = generateSalt();

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();

        // Combine salt and hash, using a delimiter
        return Base64.encodeToString(salt, Base64.DEFAULT) + ":" + Base64.encodeToString(hash, Base64.DEFAULT);
    }

    private byte[] generateSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return salt;
    }

    public String generateUid() {
        return UUID.randomUUID().toString();
    }



}
