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

public class HomeActivity extends AppCompatActivity implements View.OnClickListener{

    ResideMenu resideMenu;
    private ResideMenuItem itemHome, itemSavedSongs, itemSettings;
    Toolbar toolbar;

    HueHelper hue;

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

        this.hue = new HueHelper();
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
        HueHelper hh = new HueHelper();
        hh.toggleLightOn(hh.getLights().get("3"));
    }

    @TargetApi(23)
    public void play(View view) {
        if (state != PlayerState.PLAYING) {
            requestPermissions(perms, permsRequestCode);
            state = PlayerState.PLAYING;
            this.dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
            this.handler = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                    final float pitchInHz = result.getPitch();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // this should probably only run in every X intervals...
                            try {
                                if (state != PlayerState.PLAYING) {
                                    throw new InterruptedException("Thread is stopped/paused");
                                }
                                Log.i("TARSOS_PITCH", String.valueOf(pitchInHz));
                                try {
                                    process(pitchInHz, lastPitch);
                                } catch (Exception e) {
                                    // when lastPitch isn't initialized, forgot the exception name
                                    process(pitchInHz, (float) 0.0);
                                }
                                lastPitch = pitchInHz;
                                // do the hue stuff here
                            } catch (InterruptedException e) {
                                Log.e("TARSOS", "THREAD STOPPED");
                                dispatcher.stop();
                                Log.i("TARSOS_PITCH", String.valueOf(pitchInHz));
                                Log.i("TARSOS_STATE", state.toString());
                            }
                        }
                    });
                }
            };
            this.processor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, handler);
            dispatcher.addAudioProcessor(processor);
            this.thread = new Thread(dispatcher, "Audio Dispatcher");
            this.thread.start();
        }
    }

    /**
     * Pause the currently changing hue-light
     * Stops the thread
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
     * Stops the currently changing hue-light (disables)
     * Stops the thread
     */
    public void stop(View view) {
        if (this.state == PlayerState.PLAYING) {
            thread.interrupt();
        }

        if (this.state != PlayerState.STOPPED) {
            try {
                hue.toggleLightOn(hue.getNextLight());
            } catch (HueHelperException e) {
                e.printStackTrace();
            }
        }

        this.state = PlayerState.STOPPED;
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){

        switch(permsRequestCode){

            case 200:

                boolean audioAccepted = grantResults[0]== PackageManager.PERMISSION_GRANTED;

                break;

        }

    }

    private void process(float prev, float curr) {
        // set brightness : sets brightness
        // set hue : sets Hue
        // set saturation : sets Saturation
        // set XY : sets XY coordinates in color space
        // set CT : sets MIRED COLOR TEMP

        PHLight lNext = null;
        try {
            lNext = hue.getNextLight();
            // call hueProcessor.process with the pitch and light
        } catch (HueHelperException e) {
            e.printStackTrace();
        }
    }
}
