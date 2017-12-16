package a477.hueapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class SavedSongs extends AppCompatActivity implements View.OnClickListener {

    ResideMenu resideMenu;
    private ResideMenuItem itemHome, itemSettings;
    Toolbar toolbar;
    private boolean DEBUG_MODE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_songs);

        DEBUG_MODE = getIntent().getBooleanExtra("DEBUG_MODE", false);

        // Attach menu to current activity, only left side
        resideMenu = new ResideMenu(this);
        resideMenu.setBackground(R.drawable.ligh_background);
        resideMenu.attachToActivity(this);
        resideMenu.setSwipeDirectionDisable(ResideMenu.DIRECTION_RIGHT);
        resideMenu.setSwipeDirectionDisable(ResideMenu.DIRECTION_LEFT);

        // Create menu items
        itemHome = new ResideMenuItem(this, R.drawable.home48, "Home");
        itemSettings = new ResideMenuItem(this, R.drawable.settings48, "Settings");

        // Add the onClickListener for each menu option
        itemHome.setOnClickListener(this);
        itemSettings.setOnClickListener(this);

        // Now add options to the menu
        resideMenu.addMenuItem(itemHome, ResideMenu.DIRECTION_LEFT);
        resideMenu.addMenuItem(itemSettings, ResideMenu.DIRECTION_LEFT);

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
    public void onClick(View view) {
        Intent intent;
        if (view == itemHome) {
            // Home
            intent = new Intent(this, HomeActivity.class);
            intent.putExtra("DEBUG_MODE", DEBUG_MODE);
            startActivity(intent);

        }
        if(view == itemSettings){
            // Settings
            intent = new Intent(this, Settings.class);
            intent.putExtra("DEBUG_MODE", DEBUG_MODE);
            startActivity(intent);
        }
        resideMenu.closeMenu();
    }
}
