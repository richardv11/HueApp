package a477.hueapp.savedRuns;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import a477.hueapp.hue.HueHelperException;

/**
 * Created by mrodger4 on 11/20/17.
 * <p>
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
    public static final String DATABASE_NAME = "hueHelper-runs.db";

    private final static String CREATE_CMD = "CREATE TABLE " + SavedRunContract.SavedRunEntry.TABLE_NAME + " (" + SavedRunContract.SavedRunEntry._ID +
            " INTEGER PRIMARY KEY AUTOINCREMENT, " + SavedRunContract.SavedRunEntry.RUN_NAME + " TEXT NOT NULL UNIQUE, " +
            SavedRunContract.SavedRunEntry.RUN_PATTERN + " TEXT)";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + SavedRunContract.SavedRunEntry.TABLE_NAME;

    private static SavedRunsHelper instance;
    private String run;

    public static SavedRunsHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SavedRunsHelper(context);
        }
        return instance;
    }

    public void addNote(String note) {
        if (run == null) {
            run = "";
        } else {
            run += note + ",";
        }
    }

    public void clearRun() {
        run = "";
    }

    public boolean saveSavedRun(SQLiteDatabase db, String name) throws HueHelperException {
        if (run != null && !run.equals("")) {
            try {
                ContentValues cv = new ContentValues();
                cv.put(SavedRunContract.SavedRunEntry.RUN_NAME, name);
                cv.put(SavedRunContract.SavedRunEntry.RUN_PATTERN, run.substring(0, run.length() - 1));
                db.insert(SavedRunContract.SavedRunEntry.TABLE_NAME, null, cv);
                clearRun();
                return true;
            } catch (SQLException e) {
                Log.e(TAG, " - Exception: " + e);
                // Don't want to clear here. Maybe want to give them an option to try saving again?
                return false;
            }
        } else {
            throw new HueHelperException("Empty Run");
        }
    }

    public boolean saveSavedRun(SQLiteDatabase db, String name, String pattern) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(SavedRunContract.SavedRunEntry.RUN_NAME, name);
            cv.put(SavedRunContract.SavedRunEntry.RUN_PATTERN, pattern);
            db.insert(SavedRunContract.SavedRunEntry.TABLE_NAME, null, cv);
            return true;
        } catch (SQLException e) {
            Log.e(TAG, " - Exception: " + e);
            // Don't want to clear here. Maybe want to give them an option to try saving again?
            return false;
        }
    }

    public boolean deleteSavedRun(SQLiteDatabase db, String runName) {
        try {
            String selection = SavedRunContract.SavedRunEntry.RUN_NAME + " LIKE ?";
            String[] selectionArgs = {runName};
            db.delete(SavedRunContract.SavedRunEntry.TABLE_NAME, selection, selectionArgs);
            return true;
        } catch (SQLException e) {
            Log.e(TAG, " - Exception: " + e);
            return false;
        }
    }

    public HashMap<String, String> getSavedRun(SQLiteDatabase db, String runName) {
        HashMap<String, String> toReturn = new HashMap<>();

        String[] projection = {
                SavedRunContract.SavedRunEntry.RUN_NAME,
                SavedRunContract.SavedRunEntry.RUN_PATTERN
        };

        String selection = SavedRunContract.SavedRunEntry.RUN_NAME + " = ?";
        String[] selectionArgs = {runName};

        Cursor cursor = db.query(SavedRunContract.SavedRunEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, null);

        if (cursor.getCount() == 1) {
            while (cursor.moveToNext()) {
                toReturn.put(SavedRunContract.SavedRunEntry.RUN_NAME, cursor.getString(cursor.getColumnIndex(SavedRunContract.SavedRunEntry.RUN_NAME)) + "");
                toReturn.put(SavedRunContract.SavedRunEntry.RUN_PATTERN, cursor.getString(cursor.getColumnIndex(SavedRunContract.SavedRunEntry.RUN_PATTERN)) + "");
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
                SavedRunContract.SavedRunEntry.RUN_NAME
        };
        Cursor cursor = db.query(SavedRunContract.SavedRunEntry.TABLE_NAME, projection, null, null, null, null, null);

        while (cursor.moveToNext()) {
            String tmp = cursor.getString(cursor.getColumnIndex(SavedRunContract.SavedRunEntry.RUN_NAME));
            toReturn.add(tmp);
        }

        cursor.close();
        return toReturn;
    }

    private SavedRunsHelper(Context context) {
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
