package a477.hueapp.savedRuns;

import android.util.Log;

import com.philips.lighting.model.PHLight;

import java.sql.Timestamp;

import a477.hueapp.hue.HueHelper;
import a477.hueapp.hue.HueHelperException;

public class SavedRunRunner implements Runnable {
    private String run;
    private int startingIndex;

    @Override
    public void run() {
        HueHelper hueHelper = HueHelper.getInstance();
        String[] notes = run.split(",");
        int i = 0;
        for (i = startingIndex; i < notes.length; i++) {
            SavedRunStateManager.getInstance().setLastNoteIndex(i);
            String[] tmp = notes[i].split(" ");
            String freq = tmp[0];
            Timestamp start = new Timestamp(Long.valueOf(tmp[1]));
            // Calc the duration
            if (i != notes.length - 1) {
                String[] next = notes[i + 1].split(" ");
                Timestamp stop = new Timestamp(Long.valueOf(next[1]));
                long duration = stop.getTime() - start.getTime();
                try {

                    PHLight light = hueHelper.getNextLight();
                    if (light != null) {
                        hueHelper.setBrightness(light, 100);
                        hueHelper.setSaturation(light, 150);
                        hueHelper.setHue(light, (int) (Float.valueOf(freq) * 1000) % 65535);
                    }
                } catch (HueHelperException e) {
                    Log.e("SavedRunRunner", "run: ", e);
                }

                try {
                    Thread.sleep(Long.valueOf("" + duration));
                } catch (InterruptedException e) {
                    // TODO: How do we want to handle this? This run method should be done
                    // TODO: outside of the ui thread for sure. And we can have a start, stop,
                    // TODO: and pause button similar to the home page. With these we can
                    // TODO: interrupt the thread this is running on.
                    break;
//                    e.printStackTrace();
                }
            }
        }

        // If we get to the end of the notes array then we are done with this run
        if (i == notes.length) {
            SavedRunStateManager.getInstance().playerStopped();
        }

        // Turn off the lights if we are stopped
        if (SavedRunStateManager.getInstance().getState().equals(SavedRunStates.STOPPED)) {
            for (PHLight light : hueHelper.getLightsInUse()) {
                try {
                    hueHelper.toggleLightOff(light);
                } catch (HueHelperException e) {
                    Log.e("SavedSongs", "run: ", e);
                }
            }
        }

    }

    public SavedRunRunner(String run, int idx) {
        this.run = run;
        this.startingIndex = idx;
    }
}
