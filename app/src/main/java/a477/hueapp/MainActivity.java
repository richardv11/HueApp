package a477.hueapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHHueParsingError;

import java.util.List;

import a477.hueapp.hue.HueSharedPreferences;
import a477.hueapp.hue.PHPushlinkActivity;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private PHHueSDK phHueSDK;
    public static final String TAG = "CS477Hue";
    private HueSharedPreferences prefs;
    private AccessPointListAdapter adapter;

    private boolean lastSearchWasIPScan = false;
    private boolean DEBUG_MODE = false;

    @TargetApi(21)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO}, 1);
        }
        String[] perms = new String[]{Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO};
        int permsRequestCode = 200;
        if (!hasPermissions(this, perms)) {
            ActivityCompat.requestPermissions(this, perms, permsRequestCode);
        } else{
            start();
        }
    }

    private void start() {
        phHueSDK = PHHueSDK.create();

        // Set the Device Name (name of your app). This will be stored in your bridge whitelist entry.
        phHueSDK.setAppName("CS477-Hue-Music");
        phHueSDK.setDeviceName(Build.MODEL);

        // Register the PHSDKListener to receive callbacks from the bridge.
        phHueSDK.getNotificationManager().registerSDKListener(listener);


        // Try to automatically connect to the last known bridge.  For first time use this will be empty so a bridge search is automatically started.
        prefs = HueSharedPreferences.getInstance(getApplicationContext());
        String lastIpAddress = prefs.getLastConnectedIPAddress();
        String lastUsername = prefs.getUsername();

        // Automatically try to connect to the last connected IP Address.  For multiple bridge support a different implementation is required.
        if (lastIpAddress != null && !lastIpAddress.equals("")) {
            PHAccessPoint lastAccessPoint = new PHAccessPoint();
            lastAccessPoint.setIpAddress(lastIpAddress);
            lastAccessPoint.setUsername(lastUsername);

            if (!phHueSDK.isAccessPointConnected(lastAccessPoint)) {
                PHWizardAlertDialog.getInstance().showProgressDialog(R.string.connecting, MainActivity.this);
                phHueSDK.connect(lastAccessPoint);
                if (phHueSDK.getSelectedBridge() == null) {
                    PHWizardAlertDialog.getInstance().closeProgressDialog();
                    Log.d(TAG, "onCreate: Failed to connect to bridge");
                    setContentView(R.layout.activity_welcome);
                }
            }
        } else {  // First time use, so perform a bridge search.
            // Display welcome screen
            setContentView(R.layout.activity_welcome);

            // Debugging purposes...
            findViewById(R.id.bulb).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DEBUG_MODE = true;
                    startMainActivity();
                }
            });

        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {

        switch (permsRequestCode) {

            case 200:

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // Gets an instance of the Hue SDK.
                    start();
                } else{
                    // TODO: Display message that the app can't run without these permissions
                    finish();
                }

                break;

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.w(TAG, "Inflating home menu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }


    // Local SDK Listener
    private PHSDKListener listener = new PHSDKListener() {

        @Override
        public void onAccessPointsFound(List<PHAccessPoint> accessPoint) {
            Log.w(TAG, "Access Points Found. " + accessPoint.size());

            PHWizardAlertDialog.getInstance().closeProgressDialog();
            if (accessPoint.size() > 0) {
                phHueSDK.getAccessPointsFound().clear();
                phHueSDK.getAccessPointsFound().addAll(accessPoint);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.updateData(phHueSDK.getAccessPointsFound());
                    }
                });
            }
        }

        @Override
        public void onCacheUpdated(List<Integer> arg0, PHBridge bridge) {
            Log.w(TAG, "On CacheUpdated");
        }

        @Override
        public void onBridgeConnected(PHBridge b, String username) {
            phHueSDK.setSelectedBridge(b);
            phHueSDK.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
            phHueSDK.getLastHeartbeat().put(b.getResourceCache().getBridgeConfiguration().getIpAddress(), System.currentTimeMillis());
            prefs.setLastConnectedIPAddress(b.getResourceCache().getBridgeConfiguration().getIpAddress());
            prefs.setUsername(username);
            PHWizardAlertDialog.getInstance().closeProgressDialog();
            startMainActivity();
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint accessPoint) {
            Log.w(TAG, "Authentication Required.");
            phHueSDK.startPushlinkAuthentication(accessPoint);
            startActivity(new Intent(MainActivity.this, PHPushlinkActivity.class));
        }

        @Override
        public void onConnectionResumed(PHBridge bridge) {
            if (MainActivity.this.isFinishing())
                return;

            Log.v(TAG, "onConnectionResumed" + bridge.getResourceCache().getBridgeConfiguration().getIpAddress());
            phHueSDK.getLastHeartbeat().put(bridge.getResourceCache().getBridgeConfiguration().getIpAddress(), System.currentTimeMillis());
            for (int i = 0; i < phHueSDK.getDisconnectedAccessPoint().size(); i++) {

                if (phHueSDK.getDisconnectedAccessPoint().get(i).getIpAddress().equals(bridge.getResourceCache().getBridgeConfiguration().getIpAddress())) {
                    phHueSDK.getDisconnectedAccessPoint().remove(i);
                }
            }
        }

        @Override
        public void onConnectionLost(PHAccessPoint accessPoint) {
            Log.v(TAG, "onConnectionLost : " + accessPoint.getIpAddress());
            if (!phHueSDK.getDisconnectedAccessPoint().contains(accessPoint)) {
                phHueSDK.getDisconnectedAccessPoint().add(accessPoint);
            }
        }

        @Override
        public void onError(int code, final String message) {
            Log.e(TAG, "on Error Called : " + code + ":" + message);

            if (code == PHHueError.NO_CONNECTION) {
                Log.w(TAG, "On No Connection");
            } else if (code == PHHueError.AUTHENTICATION_FAILED || code == PHMessageType.PUSHLINK_AUTHENTICATION_FAILED) {
                PHWizardAlertDialog.getInstance().closeProgressDialog();
            } else if (code == PHHueError.BRIDGE_NOT_RESPONDING) {
                Log.w(TAG, "Bridge Not Responding . . . ");
                PHWizardAlertDialog.getInstance().closeProgressDialog();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PHWizardAlertDialog.showErrorDialog(MainActivity.this, message, R.string.btn_ok);
                    }
                });

            } else if (code == PHMessageType.BRIDGE_NOT_FOUND) {

                if (!lastSearchWasIPScan) {  // Perform an IP Scan (backup mechanism) if UPNP and Portal Search fails.
                    phHueSDK = PHHueSDK.getInstance();
                    PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
                    sm.search(false, false, true);
                    lastSearchWasIPScan = true;
                } else {
                    PHWizardAlertDialog.getInstance().closeProgressDialog();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            PHWizardAlertDialog.showErrorDialog(MainActivity.this, message, R.string.btn_ok);
                        }
                    });
                }

            }
        }

        @Override
        public void onParsingErrors(List<PHHueParsingError> parsingErrorsList) {
            for (PHHueParsingError parsingError : parsingErrorsList) {
                Log.e(TAG, "ParsingError : " + parsingError.getMessage());
            }
        }
    };

    /**
     * Called when option is selected.
     *
     * @param item the MenuItem object.
     * @return boolean Return false to allow normal menu processing to proceed,  true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.find_new_bridge:
                doBridgeSearch();
                break;
        }
        return true;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(phHueSDK != null) {
            if (listener != null) {
                phHueSDK.getNotificationManager().unregisterSDKListener(listener);
            }
            phHueSDK.disableAllHeartbeat();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        PHAccessPoint accessPoint = (PHAccessPoint) adapter.getItem(position);

        PHBridge connectedBridge = phHueSDK.getSelectedBridge();

        if (connectedBridge != null) {
            String connectedIP = connectedBridge.getResourceCache().getBridgeConfiguration().getIpAddress();
            if (connectedIP != null) {   // We are already connected here:-
                phHueSDK.disableHeartbeat(connectedBridge);
                phHueSDK.disconnect(connectedBridge);
            }
        }
        PHWizardAlertDialog.getInstance().showProgressDialog(R.string.connecting, MainActivity.this);
        phHueSDK.connect(accessPoint);
    }

    public void doBridgeSearch() {
        PHWizardAlertDialog.getInstance().showProgressDialog(R.string.search_progress, MainActivity.this);
        PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
        // Start the UPNP Searching of local bridges.
        sm.search(true, true);
    }

    // Starting the main activity this way, prevents the PushLink Activity being shown when pressing the back button.
    public void startMainActivity() {
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("DEBUG_MODE", DEBUG_MODE);
        startActivity(intent);
    }

    public void infoButton(View view) {
        String message = "It looks like a new connection is required, simply tap the button to find a new one";
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    public void connectButton(View view) {

        // Switch to the layout for the bridge list
        setContentView(R.layout.bridgelistlinear);

        adapter = new AccessPointListAdapter(getApplicationContext(), phHueSDK.getAccessPointsFound());

        ListView accessPointList = (ListView) findViewById(R.id.bridge_list);
        accessPointList.setOnItemClickListener(this);
        accessPointList.setAdapter(adapter);

        doBridgeSearch();
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}