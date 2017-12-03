package a477.hueapp;

import android.provider.BaseColumns;

/**
 * Created by mrodger4 on 11/20/17.
 */

public class SavedRunContract {

    private SavedRunContract() {
    }

    /* Inner class that defines the table contents */
    static class SavedRunEntry implements BaseColumns {
        final static String TABLE_NAME = "saved-runs";
        final static String RUN_ID = "id";
        final static String RUN_NAME = "name";
        final static String RUN_PATTERN = "patter";
    }
}
