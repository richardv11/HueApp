package a477.hueapp;

import android.provider.BaseColumns;

public class SavedRunContract {

    private SavedRunContract() {
    }

    /* Inner class that defines the table contents */
    static class SavedRunEntry implements BaseColumns {
        final static String TABLE_NAME = "savedRuns";
        final static String RUN_NAME = "name";
        final static String RUN_PATTERN = "pattern";
    }
}
