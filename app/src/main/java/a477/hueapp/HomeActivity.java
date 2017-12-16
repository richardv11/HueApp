package a477.hueapp;

import android.annotation.TargetApi;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.philips.lighting.model.PHLight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import a477.hueapp.hue.HueHelper;
import a477.hueapp.hue.HueHelperException;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private Toolbar toolbar;
    private ResideMenu resideMenu;
    private ResideMenuItem itemHome, itemSavedSongs, itemSettings;

    private HueHelper hueHelper;

    private Map<String, PHLight> lightsMap;                             // Map of lights, key is the ID
    private Map<String, PHLight> customLightsMap = new HashMap<>();     // Custom map of lights, key is the light name

    private ListView listView;
    private AudioDispatcher dispatcher;
    private PitchDetectionHandler handler;
    private AudioProcessor processor;
    private static PlayerState state;
    private Thread thread;
    private float lastPitch;
    private double rms;
    private float pitchInHz;

    private boolean DEBUG_MODE;

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
        itemHome = new ResideMenuItem(this, R.drawable.home48, "Home");
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

        state = PlayerState.STOPPED;

        if (!DEBUG_MODE) {
            this.hueHelper = new HueHelper();

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

    public void test(View view) {
        if (state != PlayerState.PLAYING)
            play(view);
        HueHelper hh = new HueHelper();
//        hh.toggleLightOn(hh.getLights().get("3"));
        try {
            hh.setHue(hh.getLights().get("3"), (int) pitchInHz);
            hh.setBrightness(hh.getLights().get("3"), (int) (rms * 1000) % 256);
            hh.setSaturation(hh.getLights().get("3"), 150);
        } catch (HueHelperException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(23)
    public void play(View view) {
        if (state != PlayerState.PLAYING) {
            findViewById(R.id.play).setVisibility(View.GONE);
            findViewById(R.id.pause).setVisibility(View.VISIBLE);
            state = PlayerState.PLAYING;

            if (DEBUG_MODE) {
                Log.i("BUTTON_STATE", state.toString());
                return ;
            }
            for (PHLight light : hueHelper.getLightsInUse()) {
                try {
                    hueHelper.toggleLightOn(light);
                    hueHelper.setBrightness(light, 100);
                    hueHelper.setSaturation(light, 150);
                    hueHelper.setHue(light, 65535);
                } catch (HueHelperException e) {
                    Log.d("HUE APP", "play: Failed to start up lights");
                }
            }
            // 22050, 1024, 0
            this.dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(48000, 8000, 0);
            this.handler = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                    pitchInHz = result.getPitch();
                    rms = (e.getRMS() * 1000) % 256;
                    if (pitchInHz != -1) {
                        try {
                            if (state != PlayerState.PLAYING) {
                                throw new InterruptedException("Thread is stopped/paused");
                            }
                            Log.i("TARSOS_PITCH", String.valueOf(pitchInHz));
                            Log.i("TARSOS_RMS", String.valueOf(rms));
                            try {
                                PHLight light = hueHelper.getNextLight();
                                ((TextView) findViewById(R.id.textView)).setText(String.valueOf((int) pitchInHz));
                                // TODO: Make brightness a config value?
                                //hueHelper.setBrightness(light, (int) rms);
                                // TODO: Make saturation a config value?
                                hueHelper.setSaturation(light, 150);
                                hueHelper.setHue(light, (int) (pitchInHz * 1000) % 65535);
                            } catch (HueHelperException e2) {
                                Log.e("HUE APP", "handlePitch: ", e2);
                            }
                            lastPitch = pitchInHz;
                        } catch (InterruptedException e3) {
                            Log.e("TARSOS", "THREAD STOPPED");
                            dispatcher.stop();
                            Log.i("TARSOS_PITCH", String.valueOf(pitchInHz));
                            Log.i("TARSOS_STATE", state.toString());
                        }
                    }
                }
            };
            this.processor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 48000, 8000, handler);
            dispatcher.addAudioProcessor(processor);
            this.thread = new Thread(dispatcher, "Audio Dispatcher");
            this.thread.start();
        }
    }

    /**
     * Pause the currently changing hueHelper-light
     * Stops the thread
     *
     * @param view
     */
    public void pause(View view) {
        if (this.state == PlayerState.PLAYING) {
            findViewById(R.id.play).setVisibility(View.VISIBLE);
            findViewById(R.id.pause).setVisibility(View.GONE);
            this.state = PlayerState.PAUSED;
            if (DEBUG_MODE) {
                return ;
            }
            thread.interrupt();                       // Has to be after DEBUG_MODE block
        }
        // pause hue_light should be done in the run() by not doing anything
        // this.state = PlayerState.PAUSED;
    }

    /**
     * Stops the currently changing hueHelper-light (disables)
     * Stops the thread
     */
    public void stop(View view) {
        if (this.state == PlayerState.PLAYING) {
            findViewById(R.id.play).setVisibility(View.VISIBLE);
            findViewById(R.id.pause).setVisibility(View.GONE);
            this.state = PlayerState.STOPPED;
            if (DEBUG_MODE) {
                return ;
            }
            thread.interrupt();                       // Has to be after DEBUG_MODE block
        }

        if (this.state != PlayerState.STOPPED) {
            if (DEBUG_MODE) {
                return ;
            }
            for (PHLight light : hueHelper.getLightsInUse())
                try {
                    hueHelper.toggleLightOff(light);
                } catch (HueHelperException e) {
                    Log.e("HUE APP", "stop: ", e);
                }

        }

        // this.state = PlayerState.STOPPED;
    }

    @Override
    public void onBackPressed() {

    }

    // Populate the list view with the lights, creates customLightMap
    private void popLightList() {
        Iterator<String> lightIDs = lightsMap.keySet().iterator();
        String lightID;
        PHLight light;
        final ArrayList<String> lightNames = new ArrayList<String>();
        while (lightIDs.hasNext()) {
            lightID = lightIDs.next();
            light = lightsMap.get(lightID);
            customLightsMap.put(light.getName(), light);
            lightNames.add(light.getName());
        }

        // Set the array adapater for the lsit
        listView.setAdapter(new ArrayAdapter<>(getApplicationContext(), R.layout.line, lightNames));
        // Set the listener for the list
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                PHLight currLight = customLightsMap.get(lightNames.get(position));
                Toast.makeText(getApplicationContext(), currLight.getName(), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }
}
