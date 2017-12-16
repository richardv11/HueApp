package a477.hueapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.madrapps.pikolo.HSLColorPicker;
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener;

import a477.hueapp.hue.HueHelper;
import a477.hueapp.hue.HueHelperException;

public class Settings extends AppCompatActivity implements View.OnClickListener{

    ResideMenu resideMenu;
    Toolbar toolbar;
    private ResideMenuItem itemHome, itemSavedSongs;
    private boolean DEBUG_MODE;
    private HueHelper hueHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        DEBUG_MODE = getIntent().getBooleanExtra("DEBUG_MODE", false);

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

        final HSLColorPicker colorPicker = (HSLColorPicker) findViewById(R.id.colorPicker);
        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        if (!DEBUG_MODE) {
            hueHelper = new HueHelper();
        }

        colorPicker.setColorSelectionListener(new SimpleColorSelectionListener() {
            @Override
            public void onColorSelected(int color) {
                // conversion from RGB to CIE XY
                float red = Color.red(color) / 255;
                float green = Color.green(color) / 255;
                float blue = Color.blue(color) / 255;
                red = (red > 0.04045f) ? (float) Math.pow((red + 0.055f) / (1.0f + 0.055f), 2.4f) : (red / 12.92f);
                green = (green > 0.04045f) ? (float) Math.pow((green + 0.055f) / (1.0f + 0.055f), 2.4f) : (green / 12.92f);
                blue = (blue > 0.04045f) ? (float) Math.pow((blue + 0.055f) / (1.0f + 0.055f), 2.4f) : (blue / 12.92f);
                float X = red * 0.649926f + green * 0.103455f + blue * 0.197109f;
                float Y = red * 0.234327f + green * 0.743075f + blue * 0.022598f;
                float Z = red * 0.0000000f + green * 0.053077f + blue * 1.035763f;

                float x = X / (X + Y + Z);
                float y = Y / (X + Y + Z);

                if (!DEBUG_MODE) {
                    try {
                        hueHelper.setXY(hueHelper.getNextLight(), x, y);
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


    // save current color
    @Override
    public void onPause() {
        super.onPause();


    }
}
