package a477.hueapp.MainPlayer;

import android.content.Context;
import android.util.Log;

import com.philips.lighting.model.PHLight;

import a477.hueapp.PlayerState;
import a477.hueapp.PlayerStateManager;
import a477.hueapp.hue.HueHelper;
import a477.hueapp.hue.HueHelperException;
import a477.hueapp.savedRuns.SavedRunStateManager;
import a477.hueapp.savedRuns.SavedRunsHelper;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class MainPlayerRunner implements Runnable {
    private AudioDispatcher dispatcher;
    private PlayerStateManager playerStateManager;
    private SavedRunStateManager srStateManager;
    private SavedRunsHelper srHelper;
    private HueHelper hueHelper;
    private double pitchInHz;
    private Thread thread;

    public MainPlayerRunner(Context context){
        hueHelper = HueHelper.getInstance();
        srHelper = SavedRunsHelper.getInstance(context);
        playerStateManager = PlayerStateManager.getInstance();
        srStateManager = SavedRunStateManager.getInstance();
    }

    @Override
    public void run() {
        createThread();
        boolean sent = true;
        while(sent){
            Log.d("MainPlayerWrapper", "run: Thread Running");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Log.d("MainPlayerWrapper", "run: Thread Interrupted");
                if(thread != null)
                    thread.interrupt();
                sent = false;
            }
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
         thread = new Thread(dispatcher, "Audio Dispatcher");
        thread.start();
    }
}
