package a477.hueapp;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.philips.lighting.model.PHLight;

import java.sql.Timestamp;

import a477.hueapp.hue.HueHelper;
import a477.hueapp.hue.HueHelperException;
import a477.hueapp.savedRuns.SavedRunContract;
import a477.hueapp.savedRuns.SavedRunRunner;
import a477.hueapp.savedRuns.SavedRunStateManager;
import a477.hueapp.savedRuns.SavedRunStates;
import a477.hueapp.savedRuns.SavedRunsHelper;

public class SavedSongs extends AppCompatActivity implements View.OnClickListener {

    ResideMenu resideMenu;
    private ResideMenuItem itemHome, itemSettings;
    Toolbar toolbar;
    private ArrayAdapter<String> adapter;
    private ListView savedRunList;
    private SavedRunsHelper srHelper;
    private SavedRunStateManager stateManager;
    private SQLiteDatabase db;
    private Thread runThread;
    private String selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_songs);

        srHelper = SavedRunsHelper.getInstance(getApplicationContext());
        db = srHelper.getWritableDatabase();
        stateManager = SavedRunStateManager.getInstance();

        // Attach menu to current activity, only left side
        resideMenu = new ResideMenu(this);
        resideMenu.setBackground(R.drawable.ligh_background);
        resideMenu.attachToActivity(this);
        resideMenu.setSwipeDirectionDisable(ResideMenu.DIRECTION_RIGHT);
        resideMenu.setSwipeDirectionDisable(ResideMenu.DIRECTION_LEFT);

        // Create menu items
        itemHome = new ResideMenuItem(this, R.drawable.home48, "Home");
        itemSettings = new ResideMenuItem(this, R.drawable.settings48, "Settings");

        // Add the onClickListener for each menu option
        itemHome.setOnClickListener(this);
        itemSettings.setOnClickListener(this);

        // Now add options to the menu
        resideMenu.addMenuItem(itemHome, ResideMenu.DIRECTION_LEFT);
        resideMenu.addMenuItem(itemSettings, ResideMenu.DIRECTION_LEFT);

        // Listen on the menu click on the toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resideMenu.openMenu(ResideMenu.DIRECTION_LEFT);
            }
        });

        savedRunList = (ListView) findViewById(R.id.savedRunList);
        adapter = new ArrayAdapter<>(this, R.layout.line);

        adapter.addAll(srHelper.getAllSavedRunNames(db));

        savedRunList.setAdapter(adapter);

        savedRunList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selected = adapter.getItem(position);
//                run(adapter.getItem(position));
            }
        });

        savedRunList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                boolean toReturn = srHelper.deleteSavedRun(db, adapter.getItem(position));
                if (toReturn)
                    adapter.remove(adapter.getItem(position));
                return toReturn;
            }
        });

    }

    public void run(String name, int startingIndex) {
        String run = srHelper.getSavedRun(db, name).get(SavedRunContract.SavedRunEntry.RUN_PATTERN);

        // Turn on lights
        HueHelper hueHelper = HueHelper.getInstance();
        for (PHLight light : hueHelper.getLightsInUse()) {
            try {
                hueHelper.toggleLightOn(light);
            } catch (HueHelperException e) {
                Log.e("SavedSongs", "run: ", e);
            }
        }

        SavedRunRunner runner = new SavedRunRunner(run, startingIndex);
        runThread = new Thread(runner);
        runThread.start();
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        if (view == itemHome) {
            // Home
            intent = new Intent(this, HomeActivity.class);
            startActivity(intent);

        }
        if (view == itemSettings) {
            // Settings
            intent = new Intent(this, Settings.class);
            startActivity(intent);
        }
        resideMenu.closeMenu();
    }

    public void stop(View view) {
        if (runThread != null)
            runThread.interrupt();
        stateManager.playerStopped();
    }

    public void play(View view) {
        // If we were paused restart the run. Else start a new one.
        if (stateManager.getState().equals(SavedRunStates.PAUSED)) {
            run(selected, stateManager.getLastNoteIndex());
        } else if (selected != null && !selected.equals(""))
            run(selected, 0);

        stateManager.playerStarted();
    }

    public void pause(View view) {
        runThread.interrupt();
        stateManager.playerPaused();
    }
}
