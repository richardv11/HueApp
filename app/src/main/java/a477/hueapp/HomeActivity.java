package a477.hueapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.philips.lighting.hue.sdk.PHHueSDK;

import a477.hueapp.hue.HueHelper;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener{

    ResideMenu resideMenu;
    private ResideMenuItem itemHome, itemSavedSongs, itemSettings;
    Toolbar toolbar;

    // Tarsos stuff
    AudioDispatcher dispatcher;
    PitchDetectionHandler handler;
    AudioProcessor processor;
    PlayerState state;

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

    public void play(View view) {
        this.dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
        this.handler = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                final float pitchInHz = result.getPitch();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HueHelper helper = new HueHelper();
                        // run some type of algorithm to determine the strength of the light / color / etc.
                    }
                });
            }
        };
        this.processor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, handler);
        dispatcher.addAudioProcessor(processor);
        new Thread(dispatcher,"Audio Dispatcher").start();
    }
}
