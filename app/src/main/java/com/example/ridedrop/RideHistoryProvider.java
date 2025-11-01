package com.example.ridedrop;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RideHistoryProvider extends ContentProvider {

    public static final String AUTHORITY = "com.example.ridedrop.provider";
    public static final String PATH_RIDES = "rides";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_RIDES);

    private static final int RIDES = 1;
    private static final int RIDE_ID = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, PATH_RIDES, RIDES);
        uriMatcher.addURI(AUTHORITY, PATH_RIDES + "/#", RIDE_ID);
    }

    private SQLiteDatabase db;

    private static final String DB_NAME = "RideHistory.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "rides";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "status TEXT, " +
            "userName TEXT, " +
            "userPhone TEXT, " +
            "timestamp LONG, " +
            "rideId TEXT, " +
            "driverId TEXT);";

    private static class RideDbHelper extends SQLiteOpenHelper {
        RideDbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        RideDbHelper dbHelper = new RideDbHelper(getContext());
        db = dbHelper.getWritableDatabase();
        return db != null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        switch (uriMatcher.match(uri)) {
            case RIDES:
                return db.query(TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            case RIDE_ID:
                selection = "_id = ?";
                selectionArgs = new String[]{uri.getLastPathSegment()};
                return db.query(TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case RIDES:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + ".ride";
            case RIDE_ID:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + ".ride";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (uriMatcher.match(uri) != RIDES) {
            throw new IllegalArgumentException("Invalid URI for insert: " + uri);
        }
        long rowID = db.insert(TABLE_NAME, null, values);
        if (rowID > 0) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return db.delete(TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return db.update(TABLE_NAME, values, selection, selectionArgs);
    }
}
