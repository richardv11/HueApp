package a477.hueapp.savedRuns;

import android.provider.BaseColumns;

public class SavedRunContract {

    private SavedRunContract() {
    }

    /* Inner class that defines the table contents */
    public static class SavedRunEntry implements BaseColumns {
        public final static String TABLE_NAME = "savedRuns";
        public final static String RUN_NAME = "name";
        public final static String RUN_PATTERN = "pattern";
    }
}
