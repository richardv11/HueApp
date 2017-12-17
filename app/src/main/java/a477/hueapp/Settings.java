package a477.hueapp;

import android.content.Intent;
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
import a477.hueapp.hue.HueHelperException;

public class Settings extends AppCompatActivity implements View.OnClickListener{

    ResideMenu resideMenu;
    Toolbar toolbar;
    private ResideMenuItem itemHome, itemSavedSongs;
    private boolean DEBUG_MODE;
    private MainPlayerHelper mpHelper;

    int[] rgb = new int[3];

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

        if (!DEBUG_MODE) {
            mpHelper = MainPlayerHelper.getInstance(getApplicationContext());
            rgb = mpHelper.load_settings();
            mpHelper.showUserColor(this, rgb);
        }

        /**
         * FYI, but I think this may need to check the STATE and current thread to change colors.
         */
        colorPicker.setColorSelectionListener(new SimpleColorSelectionListener() {
            @Override
            public void onColorSelected(int color) {
                // conversion from RGB to CIE XY
                if (!DEBUG_MODE) {
                    mpHelper.changeHueColor(Settings.this, rgb);
                }
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

        mpHelper.save_settings(rgb);
    }

}
