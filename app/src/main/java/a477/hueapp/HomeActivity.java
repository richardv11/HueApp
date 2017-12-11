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
import android.widget.TextView;

import com.philips.lighting.model.PHLight;

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
            requestPermissions(perms, permsRequestCode);
            state = PlayerState.PLAYING;
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
                                        if(!light.getLastKnownLightState().isOn())
                                            hueHelper.toggleLightOn(light);
                                        ((TextView) findViewById(R.id.textView)).setText(String.valueOf((int) pitchInHz));
                                        // TODO: Make brightness a config value?
                                        //hueHelper.setBrightness(light, (int) rms);
                                        // TODO: Make saturation a config value?
                                        hueHelper.setSaturation(light, 150);
                                        hueHelper.setHue(light, (int) (pitchInHz*1000)%65535);
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
            try {
                hueHelper.toggleLightOn(hueHelper.getNextLight());
            } catch (HueHelperException e) {
                e.printStackTrace();
            }
        }

        this.state = PlayerState.STOPPED;
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {

        switch (permsRequestCode) {

            case 200:

                boolean audioAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                break;

        }

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

}