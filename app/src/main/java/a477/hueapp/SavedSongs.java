package a477.hueapp;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.philips.lighting.model.PHLight;

import java.util.HashMap;

import a477.hueapp.hue.HueHelper;
import a477.hueapp.hue.HueHelperException;
import a477.hueapp.savedRuns.SavedRunContract;
import a477.hueapp.savedRuns.SavedRunRunner;
import a477.hueapp.savedRuns.SavedRunStateManager;
import a477.hueapp.savedRuns.SavedRunStates;
import a477.hueapp.savedRuns.SavedRunsHelper;

import static android.R.id.edit;

public class SavedSongs extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "HUE_APP_SavedSongs";

    ResideMenu resideMenu;
    private ResideMenuItem itemHome; //, itemSettings;
    Toolbar toolbar;

    private boolean DEBUG_MODE;

    private ArrayAdapter<String> adapter;
    private SavedRunsHelper srHelper;
    private HueHelper hueHelper;
    private SavedRunStateManager stateManager;
    private SQLiteDatabase db;
    private String selected;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_songs);
        context = this;

        DEBUG_MODE = getIntent().getBooleanExtra("DEBUG_MODE", false);

        hueHelper = HueHelper.getInstance();
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
        //itemSettings = new ResideMenuItem(this, R.drawable.settings48, "Settings");

        // Add the onClickListener for each menu option
        itemHome.setOnClickListener(this);
        //itemSettings.setOnClickListener(this);

        // Now add options to the menu
        resideMenu.addMenuItem(itemHome, ResideMenu.DIRECTION_LEFT);
        //resideMenu.addMenuItem(itemSettings, ResideMenu.DIRECTION_LEFT);

        // Listen on the menu click on the toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resideMenu.openMenu(ResideMenu.DIRECTION_LEFT);
            }
        });

        final ListView savedRunList = (ListView) findViewById(R.id.savedRunList);
        adapter = new ArrayAdapter<>(this, R.layout.line);

        adapter.addAll(srHelper.getAllSavedRunNames(db));

        savedRunList.setAdapter(adapter);

        savedRunList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selected = adapter.getItem(position);
            }
        });

        savedRunList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final int pos = position;
                // See if the user wants to save or delete
                AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                builder1.setMessage("Would you like to edit or delete");
                builder1.setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
                        builder2.setTitle("Give this run a name:");
                        final EditText input = new EditText(context);
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        builder2.setView(input);
                        builder2.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (input.getText().toString().length() > 0) {
                                    changeSavedRunName(adapter.getItem(pos), input.getText().toString());
                                    adapter.clear();
                                    adapter.addAll(srHelper.getAllSavedRunNames(db));
                                } else {
                                    Toast.makeText(SavedSongs.this, "Name can't be empty.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder2.show();
                    }
                });
                builder1.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Ask user if they want to delete this item
                        AlertDialog.Builder builder3 = new AlertDialog.Builder(context);
                        builder3.setMessage("Would you like to delete this song?");
                        builder3.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                boolean toReturn = srHelper.deleteSavedRun(db, adapter.getItem(pos));
                                if (toReturn)
                                    adapter.remove(adapter.getItem(pos));
                            }
                        });
                        builder3.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                        AlertDialog dialog3 = builder3.create();
                        dialog3.show();

                    }
                });
                AlertDialog dialog = builder1.create();
                dialog.show();
                return false;
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
                Log.e(TAG, "run: ", e);
            }
        }

        SavedRunRunner runner = new SavedRunRunner(run, startingIndex);
        Thread runThread = new Thread(runner);
        stateManager.setRunThread(runThread);
        runThread.start();


    }

    @Override
    public void onClick(View view) {
        Intent intent;
        if (view == itemHome) {
            // Home
            intent = new Intent(this, HomeActivity.class);
            intent.putExtra("DEBUG_MODE", DEBUG_MODE);
            startActivity(intent);

        }
        /*if (view == itemSettings) {
            // Settings
            intent = new Intent(this, Settings.class);
            intent.putExtra("DEBUG_MODE", DEBUG_MODE);
            startActivity(intent);
        }*/
        resideMenu.closeMenu();
    }

    public void stopSavedSong(View view) {
        if (!stateManager.getState().equals(SavedRunStates.STOPPED)) {
            stateManager.stopThread();
        } else if (!PlayerStateManager.getInstance().getState().equals(PlayerState.STOPPED)) {
            PlayerStateManager.getInstance().stopPlayer();

            for (PHLight light : hueHelper.getLightsInUse()) {
                try {
                    hueHelper.toggleLightOff(light);
                } catch (HueHelperException e) {
                    Log.e(TAG, "stop: ", e);
                }
            }
            PlayerStateManager.getInstance().playerStopped();

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Would you like to save this run?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Save the run, prompt for name
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
                    builder2.setTitle("Give this run a name:");
                    final EditText input = new EditText(context);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    builder2.setView(input);
                    builder2.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                if (input.getText().toString().length() > 0) {
                                    srHelper.saveSavedRun(db, input.getText().toString());
                                    adapter.clear();
                                    adapter.addAll(srHelper.getAllSavedRunNames(db));
                                } else {
                                    Toast.makeText(SavedSongs.this, "Name can't be empty.", Toast.LENGTH_SHORT).show();
                                }
                            } catch (HueHelperException e) {
                                Toast.makeText(SavedSongs.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        }
                    });
                    builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder2.show();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public void playSavedSong(View view) {
        if (hueHelper.getLightsInUse().size() > 0) {
            if (PlayerStateManager.getInstance().getState().equals(PlayerState.STOPPED)) {
                // If we were paused restart the run. Else start a new one.
                if (stateManager.getState().equals(SavedRunStates.PAUSED)) {
                    run(selected, stateManager.getLastNoteIndex());
                } else if (selected != null && !selected.equals("") && stateManager.getState().equals(SavedRunStates.STOPPED))
                    run(selected, 0);
                stateManager.playerStarted();
            } else {
                // Warn user that the saved run player must be stopped before starting a saved run?
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Looks like you're playing a saved song. Once you you stop that, you can begin listening!");
                builder.setPositiveButton("Got it!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        } else {
            // Notify the user that a light is needed?
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Choose some lights to begin listening!");
            builder.setPositiveButton("Will do", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public void pauseSavedSong(View view) {
        if (PlayerStateManager.getInstance().getState().equals(PlayerState.PLAYING)) {

        } else if (stateManager.getState().equals(SavedRunStates.PLAYING)) {
            stateManager.pauseThread();
        }
    }

    public void changeSavedRunName(String oldName, String newName) {
        HashMap<String, String> oldRun = srHelper.getSavedRun(db, oldName);
        srHelper.deleteSavedRun(db, oldName);
        srHelper.saveSavedRun(db, newName, oldRun.get(SavedRunContract.SavedRunEntry.RUN_PATTERN));
    }
}
