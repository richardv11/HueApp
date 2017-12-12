package a477.hueapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.philips.lighting.model.PHLight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    ResideMenu resideMenu;
    private ResideMenuItem itemHome, itemSavedSongs, itemSettings;
    Toolbar toolbar;

    HueHelper hueHelper;

    // Map of lights, key is the ID
    Map<String, PHLight> lightsMap;
    // Custom map of lights, key is the light name
    Map<String, PHLight> customLightsMap = new HashMap<>();
    ListView listView;


    // Tarsos stuff
    String[] perms = {"android.permission.RECORD_AUDIO"};
    int permsRequestCode = 200;
    AudioDispatcher dispatcher;
    PitchDetectionHandler handler;
    AudioProcessor processor;
    static PlayerState state;
    Thread thread;
    float lastPitch;
    HueProcessor hueProcessor;
    private double rms;
    private float pitchInHz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
        }

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

        if (savedInstanceState == null) {
            state = PlayerState.NO_FILE_LOADED;
        } else {
            state = PlayerState.FILE_LOADED;
        }

        this.hueHelper = new HueHelper();
        this.hueProcessor = new HueProcessor();

        // Grab the lights into a map, and populate list using popLightList().
        lightsMap = hueHelper.getLights();
        // listView adapter is set in popLightList()
        listView = (ListView) findViewById(R.id.listview);
        popLightList();

    }

    // onClick for menu options
    @Override
    public void onClick(View view) {
        Intent intent;
        if (view == itemSettings) {
            // Settings
            intent = new Intent(this, Settings.class);
            startActivity(intent);
        }
        if (view == itemSavedSongs) {
            // Saved Songs
            intent = new Intent(this, SavedSongs.class);
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
            state = PlayerState.PLAYING;
            for(PHLight light : hueHelper.getLightsInUse()) {
                try {
                    hueHelper.toggleLightOn(light);
                    hueHelper.setBrightness(light,100);
                    hueHelper.setSaturation(light, 150);
                    hueHelper.setHue(light, 65535);
                } catch (HueHelperException e) {
                    Log.d("HUE APP", "play: Failed to start up lights");
                }

            }
            // 22050, 1024, 0
            this.dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(48000,8000, 0);
            this.handler = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                    pitchInHz = result.getPitch();
                    rms = (e.getRMS() * 1000) % 256;
                    if (pitchInHz != -1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // this should probably only run in every X intervals...
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
                                    } catch (Exception e) {
                                        // when lastPitch isn't initialized, forgot the exception name
                                        process(pitchInHz, (float) 0.0);
                                    }
                                    lastPitch = pitchInHz;
                                    // do the hueHelper stuff here

                                } catch (InterruptedException e) {
                                    Log.e("TARSOS", "THREAD STOPPED");
                                    dispatcher.stop();
                                    Log.i("TARSOS_PITCH", String.valueOf(pitchInHz));
                                    Log.i("TARSOS_STATE", state.toString());
                                }
                            }
                        });
                    }
                }
            };
            this.processor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 48000, 8000,handler);
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
            thread.interrupt();
        }

        // pause hue_light should be done in the run() by not doing anything
        this.state = PlayerState.PAUSED;
    }

    /**
     * Stops the currently changing hueHelper-light (disables)
     * Stops the thread
     */
    public void stop(View view) {
        if (this.state == PlayerState.PLAYING) {
            thread.interrupt();
        }

        if (this.state != PlayerState.STOPPED) {
            for (PHLight light : hueHelper.getLightsInUse())
                try {
                    hueHelper.toggleLightOff(light);
                } catch (HueHelperException e){
                    Log.e("HUE APP", "stop: ", e);
                }

        }

        this.state = PlayerState.STOPPED;
    }

    private void process(float prev, float curr) {
        // set brightness : sets brightness
        // set hueHelper : sets Hue
        // set saturation : sets Saturation
        // set XY : sets XY coordinates in color space
        // set CT : sets MIRED COLOR TEMP

        PHLight lNext = null;
        try {
            lNext = hueHelper.getNextLight();
            // call hueProcessor.process with the pitch and light
        } catch (HueHelperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed(){

    }

    // Populate the list view with the lights, creates customLightMap
    private void popLightList(){
        Iterator<String> lightIDs = lightsMap.keySet().iterator();
        String lightID;
        PHLight light;
        final ArrayList<String> lightNames = new ArrayList<String>();
        while(lightIDs.hasNext()){
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

                Toast.makeText(getApplicationContext(), currLight.getLuminaireUniqueId(), Toast.LENGTH_SHORT ).show();
                return false;
            }
        });
    }
}
