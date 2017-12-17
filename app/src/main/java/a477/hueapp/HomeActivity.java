package a477.hueapp;

import android.annotation.TargetApi;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

import com.philips.lighting.model.PHLight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import a477.hueapp.MainPlayer.MainPlayerHelper;
import a477.hueapp.hue.HueHelper;
import a477.hueapp.savedRuns.SavedRunStateManager;
import a477.hueapp.savedRuns.SavedRunStates;
import a477.hueapp.savedRuns.SavedRunsHelper;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private boolean DEBUG_MODE;

    ResideMenu resideMenu;
    private ResideMenuItem itemSavedSongs;
    private ResideMenuItem itemSettings;
    Toolbar toolbar;

    HueHelper hueHelper;
    SavedRunsHelper srHelper;
    MainPlayerHelper mpHelper;
    SQLiteDatabase db;
    SavedRunStateManager savedRunStateManager;
    PlayerStateManager playerStateManager;

    // Map of lights, key is the ID
    Map<String, PHLight> lightsMap;
    // Custom map of lights, key is the light name
    Map<String, PHLight> customLightsMap = new HashMap<>();
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DEBUG_MODE = getIntent().getBooleanExtra("DEBUG_MODE", false);

        // Attach menu to current activity, only left side
        resideMenu = new ResideMenu(this);
        resideMenu.setBackground(R.drawable.ligh_background);
        resideMenu.attachToActivity(this);
        resideMenu.setSwipeDirectionDisable(ResideMenu.DIRECTION_RIGHT);
        resideMenu.setSwipeDirectionDisable(ResideMenu.DIRECTION_LEFT);

        // Create menu items
        ResideMenuItem itemHome = new ResideMenuItem(this, R.drawable.home48, "Home");
        itemSettings = new ResideMenuItem(this, R.drawable.settings48, "Settings");
        itemSavedSongs = new ResideMenuItem(this, R.drawable.play48, "Saved Songs");

        // Add the onClickListener for each menu option
        itemHome.setOnClickListener(this);
        itemSettings.setOnClickListener(this);
        itemSavedSongs.setOnClickListener(this);

        // Now add options to the menu
        resideMenu.addMenuItem(itemHome, ResideMenu.DIRECTION_LEFT);
        resideMenu.addMenuItem(itemSettings, ResideMenu.DIRECTION_LEFT);
        resideMenu.addMenuItem(itemSavedSongs, ResideMenu.DIRECTION_LEFT);

        // Listen on the menu click on the toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resideMenu.openMenu(ResideMenu.DIRECTION_LEFT);
            }
        });

        if (!DEBUG_MODE) {
            mpHelper = MainPlayerHelper.getInstance(getApplicationContext());
            hueHelper = HueHelper.getInstance();
            srHelper = SavedRunsHelper.getInstance(getApplicationContext());
            db = srHelper.getWritableDatabase();
            savedRunStateManager = SavedRunStateManager.getInstance();
            playerStateManager = PlayerStateManager.getInstance();

            // Grab the lights into a map, and populate list using popLightList().
            lightsMap = hueHelper.getLights();
            // listView adapter is set in popLightList()
            listView = (ListView) findViewById(R.id.listview);
            popLightList();
        }

    }

    // onClick for menu options
    @Override
    public void onClick(View view) {
        Intent intent;
        if (view == itemSettings) {
            // Settings
            intent = new Intent(this, Settings.class);
            intent.putExtra("DEBUG_MODE", DEBUG_MODE);
            startActivity(intent);
        }
        if (view == itemSavedSongs) {
            // Saved Songs
            intent = new Intent(this, SavedSongs.class);
            intent.putExtra("DEBUG_MODE", DEBUG_MODE);
            startActivity(intent);
        }
        resideMenu.closeMenu();
    }

    @TargetApi(23)
    public void play(View view) {
        if (playerStateManager.getState() != PlayerState.PLAYING) {
            findViewById(R.id.play).setVisibility(View.GONE);
            findViewById(R.id.pause).setVisibility(View.VISIBLE);
        }

        if (!DEBUG_MODE) {
            mpHelper.start();
        }
    }

    /**
     * Pause the currently changing hueHelper-light
     * Stops the thread
     */
    public void pause(View view) {
        if (playerStateManager.getState() == PlayerState.PLAYING) {
            findViewById(R.id.play).setVisibility(View.VISIBLE);
            findViewById(R.id.pause).setVisibility(View.GONE);
        }

        if (!DEBUG_MODE) {
            if (playerStateManager.getState().equals(PlayerState.PLAYING)) {
                mpHelper.pause();
            } else if (savedRunStateManager.getState().equals(SavedRunStates.PLAYING))
                savedRunStateManager.pauseThread();
        }
    }

    /**
     * Stops the currently changing hueHelper-light (disables)
     * Stops the thread
     */
    public void stop(View view) {
        if (playerStateManager.getState() == PlayerState.PLAYING) {
            findViewById(R.id.play).setVisibility(View.VISIBLE);
            findViewById(R.id.pause).setVisibility(View.GONE);
        }

        if (!DEBUG_MODE) {
            if (!savedRunStateManager.getState().equals(SavedRunStates.STOPPED))
                savedRunStateManager.stopThread();
            else if (!PlayerStateManager.getInstance().getState().equals(PlayerState.STOPPED)) {
                mpHelper.stop();
            }
        }
    }

    @Override
    public void onBackPressed() {

    }

    // Populate the list view with the lights, creates customLightMap
    private void popLightList() {
        // Get the values to be added to list
        Iterator<String> lightIDs = lightsMap.keySet().iterator();
        String lightID;
        PHLight light;
        final ArrayList<String> lightNames = new ArrayList<>();
        while (lightIDs.hasNext()) {
            lightID = lightIDs.next();
            light = lightsMap.get(lightID);
            customLightsMap.put(light.getName(), light);
            lightNames.add(light.getName());
        }

        // Custom list view, text + toggle
        MyListView adapter = new MyListView(this, lightNames);
        listView.setAdapter(adapter);
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
