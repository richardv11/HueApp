package a477.hueapp.savedRuns;

import android.util.Log;

import com.philips.lighting.model.PHLight;

import a477.hueapp.hue.HueHelper;
import a477.hueapp.hue.HueHelperException;

public class SavedRunStateManager {

    private static SavedRunStateManager instance;

    private SavedRunStates playerState;
    private int lastNoteIndex;
    private Thread runThread;

    private SavedRunStateManager() {
        playerState = SavedRunStates.STOPPED;
        lastNoteIndex = 0;
    }

    public static SavedRunStateManager getInstance() {
        if (instance == null)
            instance = new SavedRunStateManager();
        return instance;
    }

    public synchronized void playerStarted() {
        playerState = SavedRunStates.PLAYING;
    }

    public synchronized void playerPaused() {
        playerState = SavedRunStates.PAUSED;
    }

    public synchronized void playerStopped() {
        playerState = SavedRunStates.STOPPED;
        lastNoteIndex = 0;
    }

    public SavedRunStates getState() {
        return playerState;
    }

    public synchronized void setLastNoteIndex(int idx) {
        lastNoteIndex = idx;
    }

    public int getLastNoteIndex() {
        return lastNoteIndex;
    }

    public synchronized void setRunThread(Thread runThread) {
        this.runThread = runThread;
    }

    public synchronized void pauseThread() {
        if (playerState.equals(SavedRunStates.PLAYING)) {
            runThread.interrupt();
            playerPaused();
        }
    }

    public synchronized void stopThread() {
        if (!playerState.equals(SavedRunStates.STOPPED)) {
            playerStopped();
            if (runThread.getState().equals(Thread.State.TERMINATED)) {
                HueHelper hueHelper = HueHelper.getInstance();
                for (PHLight light : hueHelper.getLightsInUse()) {
                    try {
                        hueHelper.toggleLightOff(light);
                    } catch (HueHelperException e) {
                        Log.e("HUE APP", "stop: ", e);
                    }
                }
            }
            if (runThread != null)
                runThread.interrupt();
        }
    }
}
