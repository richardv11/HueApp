package a477.hueapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import a477.hueapp.SavedRunContract.SavedRunEntry;

/**
 * Created by mrodger4 on 11/20/17.
 *
 * This class acts as a helper between the Main Activity and the
 * database that stores the saved runs.
 * <p>
 * Methods:
 * - saveSavedRun
 * - deleteSavedRun
 * - getSavedRun
 * - getAllSavedRuns
 */
public class SavedRunsHelper extends SQLiteOpenHelper {

    private final static int VERSION = 1;
    private final static String TAG = "HUE SQL";
    public static final String DATABASE_NAME = "hue-runs.db";

    private final static String CREATE_CMD = "CREATE TABLE " + SavedRunEntry.TABLE_NAME + " (" + SavedRunEntry._ID +
            " INTEGER PRIMARY KEY AUTOINCREMENT, " + SavedRunEntry.RUN_NAME + " TEXT NOT NULL UNIQUE, " +
            SavedRunEntry.RUN_PATTERN + " TEXT)";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + SavedRunEntry.TABLE_NAME;

    public boolean saveSavedRun(SQLiteDatabase db, ContentValues cv) {
        try {
            db.insert(SavedRunEntry.TABLE_NAME, null, cv);
            return true;
        } catch (SQLException e) {
            Log.e(TAG, " - Exception: " + e);
            return false;
        }
    }

    public boolean deleteSavedRun(SQLiteDatabase db, String runName) {
        try {
            String selection = SavedRunEntry.RUN_NAME + " LIKE ?";
            String[] selectionArgs = {runName};
            db.delete(SavedRunEntry.TABLE_NAME, selection, selectionArgs);
            return true;
        } catch (SQLException e) {
            Log.e(TAG, " - Exception: " + e);
            return false;
        }
    }

    public HashMap<String, String> getSavedRun(SQLiteDatabase db, String runName) {
        HashMap<String, String> toReturn = new HashMap<>();

        String[] projection = {
                SavedRunEntry.RUN_NAME,
                SavedRunEntry.RUN_PATTERN
        };

        String selection = SavedRunEntry.RUN_NAME + " = ?";
        String[] selectionArgs = {runName};

        Cursor cursor = db.query(SavedRunEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, null);

        if (cursor.getCount() == 1) {
            while (cursor.moveToNext()) {
                toReturn.put(SavedRunEntry.RUN_NAME, cursor.getString(cursor.getColumnIndex(SavedRunEntry.RUN_NAME)) + "");
                toReturn.put(SavedRunEntry.RUN_PATTERN, cursor.getString(cursor.getColumnIndex(SavedRunEntry.RUN_PATTERN)) + "");
            }
            cursor.close();
            return toReturn;
        } else
            cursor.close();
        return null;
    }

    public ArrayList<String> getAllSavedRunNames(SQLiteDatabase db) {
        ArrayList<String> toReturn = new ArrayList<>();

        String[] projection = {
                SavedRunEntry.RUN_NAME
        };
        Cursor cursor = db.query(SavedRunEntry.TABLE_NAME, projection, null, null, null, null, null);

        while (cursor.moveToNext()) {
            String tmp = cursor.getString(cursor.getColumnIndex(SavedRunEntry.RUN_NAME));
            toReturn.add(tmp);
        }

        cursor.close();
        return toReturn;
    }

    public SavedRunsHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CMD);
        // TODO: Pre-load a run?
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
