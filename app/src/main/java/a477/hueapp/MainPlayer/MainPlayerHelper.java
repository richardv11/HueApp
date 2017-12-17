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
    private AudioDispatcher dispatcher;
    private double pitchInHz;

    private MainPlayerHelper(Context context) {
        hueHelper = HueHelper.getInstance();
        srHelper = SavedRunsHelper.getInstance(context);
        db = srHelper.getWritableDatabase();
        playerStateManager = PlayerStateManager.getInstance();
        srStateManager = SavedRunStateManager.getInstance();
    }

    public static MainPlayerHelper getInstance(Context context) {
        if (instance == null)
            instance = new MainPlayerHelper(context);
        return instance;
    }

    public void start() {
        if (hueHelper.getLightsInUse().size() > 0) {
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
                    createThread();

                } else {
                    // TODO: Warn user that the saved run player must be stopped before starting a saved run?

                }
            }
        } else {
            // TODO: Notify the user that a light is needed?
            // Toast.makeText(this, "Please enable a light first", Toast.LENGTH_SHORT).show();
        }
    }

    public void pause() {
        // If we are playing then pause. Otherwise do nothing
        if (playerStateManager.getState().equals(PlayerState.PLAYING))
            playerStateManager.playerPaused();
    }

    public void stop() {
        // If we are playing then stop. If Saved Run player is playing then stop it.
        if (!playerStateManager.getState().equals(PlayerState.STOPPED)) {
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

    private void createThread() {
        // 22050, 1024, 0
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(48000, 8000, 0);
        PitchDetectionHandler handler = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                // Only do stuff if we are playing
                if (playerStateManager.getState().equals(PlayerState.PLAYING)) {
                    // Get the pitch
                    pitchInHz = result.getPitch();
                    if (pitchInHz != -1) {
                        try {
                            // If we change our state before we start processing then interrupt ourself
                            if (!playerStateManager.getState().equals(PlayerState.PLAYING)) {
                                throw new InterruptedException("Thread is stopped/paused");
                            }
                            Log.i("TARSOS_PITCH", String.valueOf(pitchInHz));

                            // Try and change the light
                            try {
                                // Add a new note to the current run
                                srHelper.addNote("" + pitchInHz + " " + System.currentTimeMillis());

                                // Change the light
                                PHLight light = hueHelper.getNextLight();
                                if (light != null) {
                                    hueHelper.setSaturation(light, 150);
                                    hueHelper.setHue(light, (int) (pitchInHz * 1000) % 65535);
                                }
                            } catch (HueHelperException e2) {
                                // Something went wrong with the note change
                                Log.e("HUE APP", "handlePitch: ", e2);
                            }
                        } catch (InterruptedException e3) {
                            // Our state changed
                            if (playerStateManager.getState().equals(PlayerState.STOPPED)) {
                                // We got stopped so we need to stop listening.
                                if (dispatcher != null) {
                                    dispatcher.stop();
                                }
                            }
                        }
                    }
                }
            }
        };

        // Add the chosen audio processor to the dispatcher
        PitchProcessor processor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 48000, 8000, handler);
        dispatcher.addAudioProcessor(processor);

        // Start the thread
        Thread mainPlayerThread = new Thread(dispatcher, "Audio Dispatcher");
        playerStateManager.setMainPlayerThread(mainPlayerThread);
        mainPlayerThread.start();
    }

    /**
     * not really the best way.. but for access from Settings
     */
    public HueHelper getHueHelper() {
        return this.hueHelper;
    }
}