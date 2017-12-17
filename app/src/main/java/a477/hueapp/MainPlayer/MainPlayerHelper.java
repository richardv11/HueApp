package a477.hueapp.MainPlayer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.philips.lighting.model.PHLight;

import a477.hueapp.PlayerState;
import a477.hueapp.PlayerStateManager;
import a477.hueapp.hue.HueHelper;
import a477.hueapp.hue.HueHelperException;
import a477.hueapp.savedRuns.SavedRunStateManager;
import a477.hueapp.savedRuns.SavedRunStates;
import a477.hueapp.savedRuns.SavedRunsHelper;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class MainPlayerHelper {

    private static MainPlayerHelper instance;
    private PlayerStateManager playerStateManager;
    private SavedRunStateManager srStateManager;
    private HueHelper hueHelper;
    private SavedRunsHelper srHelper;
    private SQLiteDatabase db;
    private Context context;

    private MainPlayerHelper(Context context) {
        hueHelper = HueHelper.getInstance();
        srHelper = SavedRunsHelper.getInstance(context);
        db = srHelper.getWritableDatabase();
        playerStateManager = PlayerStateManager.getInstance();
        srStateManager = SavedRunStateManager.getInstance();
        this.context = context;
    }

    public static MainPlayerHelper getInstance(Context context) {
        if (instance == null)
            instance = new MainPlayerHelper(context);
        return instance;
    }

    public void start() {
        // Make sure the Saved Run player isn't running
        if (srStateManager.getState().equals(SavedRunStates.STOPPED)) {
            // If we are not already started then start
            if (!playerStateManager.getState().equals(PlayerState.PLAYING)) {
                // Do setup
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
                // Change state to playing
                playerStateManager.playerStarted();

                MainPlayerRunner runner = new MainPlayerRunner(context);
                Thread mainPlayerThread = new Thread(runner);
                playerStateManager.setMainPlayerThread(mainPlayerThread);
                mainPlayerThread.start();

            } else {
                // TODO: Warn user that the saved run player must be stopped before starting a saved run.

            }
        }
    }

    public void pause() {
        // If we are playing then pause. Otherwise do nothing
        if (playerStateManager.getState().equals(PlayerState.PLAYING))
            playerStateManager.playerPaused();
    }

    public void stop() {
        // If we are playing then stop. If Saved Run player is playing then stop it.
        if (playerStateManager.getState().equals(PlayerState.PLAYING)) {
            playerStateManager.playerStopped();

            // Stop the wrapper thread
            playerStateManager.getMainPlayerThread().interrupt();

            // We stopped the player so we need to turn off the lights
            try {
                for (PHLight light : hueHelper.getLightsInUse())
                    hueHelper.toggleLightOff(light);
            } catch (HueHelperException e) {
                e.printStackTrace();
            }

            // Then check to see if the user wants to save the run
            // TODO: Prompt the user to save the run.
            // TODO: Save the run.
        }
    }


}