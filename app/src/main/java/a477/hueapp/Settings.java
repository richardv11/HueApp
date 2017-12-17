package a477.hueapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.madrapps.pikolo.HSLColorPicker;
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener;
import com.philips.lighting.model.PHLight;

import a477.hueapp.MainPlayer.MainPlayerHelper;
import a477.hueapp.hue.HueHelper;
import a477.hueapp.hue.HueHelperException;

public class Settings extends AppCompatActivity implements View.OnClickListener{

    ResideMenu resideMenu;
    Toolbar toolbar;
    private ResideMenuItem itemHome, itemSavedSongs;
    private boolean DEBUG_MODE;
    private HueHelper hueHelper;
    private MainPlayerHelper mpHelper;
    SharedPreferences sharedpreferences;
    private final String PREF_NAME = "hue_pref";

    int red;
    int green;
    int blue;

    private void init_base_ui() {
        // Attach menu to current activity, only left side
        resideMenu = new ResideMenu(this);
        resideMenu.setBackground(R.drawable.ligh_background);
        resideMenu.attachToActivity(this);
        resideMenu.setSwipeDirectionDisable(ResideMenu.DIRECTION_RIGHT);
        resideMenu.setSwipeDirectionDisable(ResideMenu.DIRECTION_LEFT);

        // Create menu items
        itemHome = new ResideMenuItem(this, R.drawable.home48, "Home");
        itemSavedSongs = new ResideMenuItem(this, R.drawable.play48, "Saved Songs");

        // Add the onClickListener for each menu option
        itemHome.setOnClickListener(this);
        itemSavedSongs.setOnClickListener(this);

        // Now add options to the menu
        resideMenu.addMenuItem(itemHome, ResideMenu.DIRECTION_LEFT);
        resideMenu.addMenuItem(itemSavedSongs, ResideMenu.DIRECTION_LEFT);

        // Listen on the menu click on the toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resideMenu.openMenu(ResideMenu.DIRECTION_LEFT);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        DEBUG_MODE = getIntent().getBooleanExtra("DEBUG_MODE", false);
        init_base_ui();

        final HSLColorPicker colorPicker = (HSLColorPicker) findViewById(R.id.colorPicker);
        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        if (!DEBUG_MODE) {
            mpHelper = MainPlayerHelper.getInstance(getApplicationContext());
        }

        sharedpreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        red = sharedpreferences.getInt("red", 30);
        green = sharedpreferences.getInt("green", 225);
        blue = sharedpreferences.getInt("blue", 225);
        int color = Color.argb(255, red, green, blue);
        imageView.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);


        /**
         * FYI, but I think this may need to check the STATE and current thread to change colors.
         */
        colorPicker.setColorSelectionListener(new SimpleColorSelectionListener() {
            @Override
            public void onColorSelected(int color) {
                // conversion from RGB to CIE XY
                red = Color.red(color);
                green = Color.green(color);
                blue = Color.blue(color);
                float[] f = convertToXY(red, green, blue);
                Log.i("SETTINGS_X", String.valueOf(f[0]));
                Log.i("SETTINGS_Y", String.valueOf(f[1]));

                if (!DEBUG_MODE) {
                    try {
                        mpHelper.stop();
                        hueHelper = mpHelper.getHueHelper();
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
                }

                imageView.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            }
        });
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        if (view == itemHome) {
            // Home
            intent = new Intent(this, HomeActivity.class);
            intent.putExtra("DEBUG_MODE", DEBUG_MODE);
            startActivity(intent);

        }
        if(view == itemSavedSongs){
            // Saved Songs
            intent = new Intent(this, SavedSongs.class);
            intent.putExtra("DEBUG_MODE", DEBUG_MODE);
            startActivity(intent);
        }
        resideMenu.closeMenu();
    }


    // save current color (we might not need to if we use sharedpreferences)
    @Override
    public void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putInt("red", red);
        editor.putInt("green", green);
        editor.putInt("blue", blue);
        editor.commit();
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
