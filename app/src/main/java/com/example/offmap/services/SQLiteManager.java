package com.example.offmap.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;

import com.example.offmap.models.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


public class SQLiteManager extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "offmap.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USERS = "users";
    private static final String COLUMN_UID = "UID";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD_HASH = "password_hash";

    // Favourite table
    private static final String TABLE_FAVORITES = "favorites";
    private static final String COLUMN_FAVORITE_ID = "favorite_id";
    private static final String COLUMN_PLACE_NAME = "place_name";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";



    public SQLiteManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create user tables
        String createTableQuery = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_UID + " TEXT PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_EMAIL + " TEXT, " +
                COLUMN_PASSWORD_HASH + " TEXT)";
        db.execSQL(createTableQuery);
        // Create favorites table
        String createFavoritesTable = "CREATE TABLE " + TABLE_FAVORITES + " (" +
                COLUMN_FAVORITE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_UID + " TEXT, " +
                COLUMN_PLACE_NAME + " TEXT, " +
                COLUMN_LATITUDE + " REAL, " +
                COLUMN_LONGITUDE + " REAL, " +
                "FOREIGN KEY(" + COLUMN_UID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_UID + "))";
        db.execSQL(createFavoritesTable);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }


    // Save a user to the database
    public boolean saveUser(User user, String hashedPassword) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_UID, user.getUid());
            values.put(COLUMN_NAME, user.getFullName());
            values.put(COLUMN_EMAIL, user.getEmail());
            values.put(COLUMN_PASSWORD_HASH, hashedPassword);

            long result = db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            return result != -1;
        }
    }

    public User getUser(String uid) {
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.query(TABLE_USERS, null, COLUMN_UID + "=?", new String[]{uid}, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL));
                return new User(uid, name, email);
            }
        }
        return null;
    }

    // Validate user login
    public User loginUser(String email, String password) {
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.query(TABLE_USERS, null, COLUMN_EMAIL + "=?", new String[]{email}, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH));
                String uid = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));

                // Verify the password
                if (verifyPassword(password, storedHash)) {
                    return new User(uid, name, email); // Return user on successful login
                }
            }
        }
        return null; // Return null if login fails
    }

    // Verify hashed password
    private boolean verifyPassword(String password, String storedHash) {
        try {
            // Split the stored hash into salt and hash parts
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                return false; // Invalid stored format
            }

            byte[] salt = Base64.decode(parts[0], Base64.DEFAULT);
            byte[] hash = Base64.decode(parts[1], Base64.DEFAULT);

            // Hash the input password using the extracted salt
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] inputHash = factory.generateSecret(spec).getEncoded();

            // Compare the hashes
            return java.util.Arrays.equals(hash, inputHash);
        } catch (Exception e) {
            Log.e("PasswordVerification", "Error during password verification", e);
            return false; // Return false if an error occurs
        }
    }

    // Delete a user
    public void deleteUser(String uid) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USERS, COLUMN_UID + "=?", new String[] {uid});
        db.close();
    }


    // Add favorite
    public boolean addFavorite(String uid, String placeName, double latitude, double longitude) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_UID, uid);
            values.put(COLUMN_PLACE_NAME, placeName);
            values.put(COLUMN_LATITUDE, latitude);
            values.put(COLUMN_LONGITUDE, longitude);

            long result = db.insert(TABLE_FAVORITES, null, values);
            return result != -1;
        }
    }

    // Retrieve all favorites for a user
    public Cursor getFavoritesForUser(String uid) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_FAVORITES, null, COLUMN_UID + "=?", new String[] {uid}, null, null, null);
    }

    // Delete a favorite place
    public boolean deleteFavorite(int favoriteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_FAVORITES, COLUMN_FAVORITE_ID + "=?", new String[] {String.valueOf(favoriteId)});

        return rowsDeleted > 0;
    }
 }
