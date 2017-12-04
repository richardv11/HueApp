package a477.hueapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * Created by mrodger4 on 11/21/17.
 */

public class SavedRunsHelperTest {
    private Context appContext;
    private SQLiteDatabase db;
    private SavedRunsHelper srHelper;

    private static String testName = "testOne";
    private static String testNameTwo = "testTwo";
    private static String testPattern = "pattern:1,a.2,a";
    private static String testPatternTwo = "pattern2:2,a.3,a";

    @Test
    public void addRunWorks() throws Exception {
        tearDown();
        setUp();
        ContentValues cv = new ContentValues();
        cv.put(SavedRunContract.SavedRunEntry.RUN_NAME, testName);
        cv.put(SavedRunContract.SavedRunEntry.RUN_PATTERN, testPattern);

        boolean save = srHelper.saveSavedRun(db, cv);
        assertEquals(save,true);

        String[] projection = {
                SavedRunContract.SavedRunEntry.RUN_NAME
        };

        Cursor cursor = db.query(SavedRunContract.SavedRunEntry.TABLE_NAME, projection, null, null, null, null, null);
        if (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(SavedRunContract.SavedRunEntry.RUN_NAME));
            assertEquals(testName, name);
        }
        cursor.close();

        String qry = "Delete from " + SavedRunContract.SavedRunEntry.TABLE_NAME + " WHERE " +
                SavedRunContract.SavedRunEntry.RUN_NAME + " =" + "'" + testName + "'";
        db.execSQL(qry);

        tearDown();
    }

    @Test
    public void deleteRunWorks() throws Exception {
        tearDown();
        setUp();
        ContentValues cv = new ContentValues();
        String testName = "testOne";
        cv.put(SavedRunContract.SavedRunEntry.RUN_NAME, testName);
        cv.put(SavedRunContract.SavedRunEntry.RUN_PATTERN, "pattern:1,a.2,a");

        boolean save = srHelper.saveSavedRun(db, cv);
        assertEquals(save,true);

        boolean delete = srHelper.deleteSavedRun(db, testName);
        assertEquals(delete, true);

        String[] projection = {
                SavedRunContract.SavedRunEntry.RUN_NAME
        };

        Cursor cursor = db.query(SavedRunContract.SavedRunEntry.TABLE_NAME,projection,null,null,null,null,null);
        assertEquals(cursor.getCount(),0);
        cursor.close();
        tearDown();
    }

    @Test
    public void getRunNames() throws Exception {
        tearDown();
        setUp();
        ContentValues cv = new ContentValues();
        cv.put(SavedRunContract.SavedRunEntry.RUN_NAME, testName);
        cv.put(SavedRunContract.SavedRunEntry.RUN_PATTERN, testPattern);
        srHelper.saveSavedRun(db,cv);
        cv = new ContentValues();
        cv.put(SavedRunContract.SavedRunEntry.RUN_NAME, testNameTwo);
        cv.put(SavedRunContract.SavedRunEntry.RUN_PATTERN, testPatternTwo);
        srHelper.saveSavedRun(db,cv);

        ArrayList<String> res = srHelper.getAllSavedRunNames(db);
        assertEquals(testName, res.remove(0));
        assertEquals(testNameTwo, res.remove(0));
        tearDown();
    }

    @Test
    public void getRun() throws Exception {
        tearDown();
        setUp();
        ContentValues cv = new ContentValues();
        cv.put(SavedRunContract.SavedRunEntry.RUN_NAME, testName);
        cv.put(SavedRunContract.SavedRunEntry.RUN_PATTERN, testPattern);

        boolean save = srHelper.saveSavedRun(db, cv);
        assertEquals(save,true);

        HashMap<String, String> res = srHelper.getSavedRun(db,testName);

        assertEquals(testName,res.get(SavedRunContract.SavedRunEntry.RUN_NAME));
        assertEquals(testPattern,res.get(SavedRunContract.SavedRunEntry.RUN_PATTERN));

        tearDown();
    }

    private void setUp() {
        appContext = InstrumentationRegistry.getTargetContext();
        srHelper = new SavedRunsHelper(appContext);
        db = srHelper.getWritableDatabase();
        try {
            srHelper.onCreate(db);
        } catch(Exception e){

        }
    }

    private void tearDown() {
        try {
            if (db != null && srHelper != null && appContext != null) {
                String qry = "DROP TABLE IF EXISTS " + SavedRunContract.SavedRunEntry.TABLE_NAME;
                db.execSQL(qry);
            }
        }catch(Exception e){

        }
    }
}
