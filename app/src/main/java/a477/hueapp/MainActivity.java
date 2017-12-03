package a477.hueapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    // THIS IS A TEST
    ResideMenu resideMenu;
    private ResideMenuItem itemHome, itemSavedSongs, itemSettings;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Attach menu to current activity, only left side
        resideMenu = new ResideMenu(this);
        resideMenu.setBackground(R.drawable.ligh_background);
        resideMenu.attachToActivity(this);
        resideMenu.setSwipeDirectionDisable(ResideMenu.DIRECTION_RIGHT);

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
    }

    // onClick for menu options
    @Override
    public void onClick(View view) {
        if (view == itemHome) {
            // Home
            Toast.makeText(getApplicationContext(), "Home tapped", Toast.LENGTH_SHORT).show();

        }else if (view == itemSettings) {
            // Settings
            Toast.makeText(getApplicationContext(), "Settings tapped", Toast.LENGTH_SHORT).show();
        }else if(view == itemSavedSongs){
            // Saved Songs
            Toast.makeText(getApplicationContext(), "Saved Songs tapped", Toast.LENGTH_SHORT).show();
        }
        resideMenu.closeMenu();
    }
}
