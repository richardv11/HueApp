package a477.hueapp.MainPlayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;

import com.philips.lighting.model.PHLight;

import a477.hueapp.PlayerState;
import a477.hueapp.PlayerStateManager;
import a477.hueapp.R;
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

import static android.content.Context.MODE_PRIVATE;

public class MainPlayerHelper {

    private final String TAG = "HUE_APP_MainPlayHelper";
    private static MainPlayerHelper instance;
    private PlayerStateManager playerStateManager;
    private SavedRunStateManager srStateManager;
    private HueHelper hueHelper;
    private SavedRunsHelper srHelper;
    private SQLiteDatabase db;
    private AudioDispatcher dispatcher;
    private double pitchInHz;
    private SharedPreferences sharedpreferences;
    private Context context;
    private final String PREF_NAME = "hue_pref";

    private MainPlayerHelper(Context context) {
        hueHelper = HueHelper.getInstance();
        srHelper = SavedRunsHelper.getInstance(context);
        db = srHelper.getWritableDatabase();
        playerStateManager = PlayerStateManager.getInstance();
        srStateManager = SavedRunStateManager.getInstance();
        sharedpreferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    public static MainPlayerHelper getInstance(Context context) {
        if (instance == null)
            instance = new MainPlayerHelper(context);
        return instance;
    }

    public void setContext(Context context){
        this.context = context;
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
                            Log.d(TAG, "play: Failed to start up lights");
                        }
                    }
                    // Change state to playing
                    playerStateManager.playerStarted();
                    createThread();
                }
            } else {
                // Warn user that the saved run player must be stopped before starting a saved run?
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Looks like you're playing a saved song. Once you you stop that, you can begin listening!");
                builder.setPositiveButton("Got it!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }

        } else

        {
            // Notify the user that a light is needed?
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Choose some lights to begin listening!");
            builder.setPositiveButton("Will do", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
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
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Would you like to save this run?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Save the run, prompt for name
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
                    builder2.setTitle("Give this run a name:");
                    final EditText input = new EditText(context);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    builder2.setView(input);
                    builder2.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                srHelper.saveSavedRun(db,input.getText().toString());
                            } catch (HueHelperException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder2.show();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
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
                            Log.i(TAG, "Tarsos-Pitch: " + String.valueOf(pitchInHz));

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
                                Log.e(TAG, "handlePitch: ", e2);
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

    public void save_settings(int[] rgb) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putInt("red", rgb[0]);
        editor.putInt("green", rgb[1]);
        editor.putInt("blue", rgb[2]);
        editor.commit();
    }

    public int[] load_settings() {
        int red = sharedpreferences.getInt("red", 30);
        int green = sharedpreferences.getInt("green", 225);
        int blue = sharedpreferences.getInt("blue", 225);

        return new int[]{red, green, blue};
    }

    public void changeHueColor(Activity activity, int[] rgb) {
        int color = Color.argb(255, rgb[0], rgb[1], rgb[2]);
        rgb[0] = Color.red(color);
        rgb[1] = Color.green(color);
        rgb[2] = Color.blue(color);
        float[] f = convertToXY(rgb[0], rgb[1], rgb[2]);
        final ImageView imageView = (ImageView) activity.findViewById(R.id.imageView);

        try {
            stop();
            try {
                for (PHLight light : hueHelper.getLightsInUse())
                    hueHelper.toggleLightOn(light);
            } catch (HueHelperException e) {
                e.printStackTrace();
            }
            hueHelper.setXY(hueHelper.getNextLight(), f[0], f[1]);
        } catch (HueHelperException e) {
            e.printStackTrace();
        }

        imageView.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
    }

    public void showUserColor(Activity activity, int[] rgb) {
        final ImageView imageView = (ImageView) activity.findViewById(R.id.imageView);
        int color = Color.argb(255, rgb[0], rgb[1], rgb[2]);
        imageView.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
    }

    private float[] convertToXY(float red, float green, float blue) {
        red /= 255;
        green /= 255;
        blue /= 255;

        float r = (red > 0.04045f) ? (float) Math.pow((red + 0.055f) / (1.0f + 0.055f), 2.4f) : (red / 12.92f);
        float g = (green > 0.04045f) ? (float) Math.pow((green + 0.055f) / (1.0f + 0.055f), 2.4f) : (green / 12.92f);
        float b = (blue > 0.04045f) ? (float) Math.pow((blue + 0.055f) / (1.0f + 0.055f), 2.4f) : (blue / 12.92f);
        float X = r * 0.649926f + g * 0.103455f + b * 0.197109f;
        float Y = r * 0.234327f + g * 0.743075f + b * 0.022598f;
        float Z = r * 0.0000000f + g * 0.053077f + b * 1.035763f;

        float x = X / (X + Y + Z);
        float y = Y / (X + Y + Z);

        float[] f = new float[2];
        f[0] = x;
        f[1] = y;

        return f;
    }
}