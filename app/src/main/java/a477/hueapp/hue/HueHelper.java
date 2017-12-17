package a477.hueapp.hue;

import android.content.SharedPreferences;
import android.util.Log;

import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.media.CamcorderProfile.get;

@SuppressWarnings("unused")
public class HueHelper {
    private final String TAG = "HueHelper";
    public final String PREFS = "LIGHTS_IN_USE";
    private final PHHueSDK phHueSDK;
    private ArrayList<PHLight> lightsInUse;
    private ArrayList<String> lightsInUseNames;
    private int lastLight;
    private static HueHelper instance;
    private SharedPreferences sharedPreferences;

    private HueHelper() {
        phHueSDK = PHHueSDK.getInstance();
        lightsInUse = new ArrayList<>();
        lightsInUseNames = new ArrayList<>();
        lastLight = 0;
        // TESTING PURPOSES
//        lightsInUse.add(getLights().get("3"));
//        lightsInUse.add(getLights().get("12"));
    }

    public static HueHelper getInstance() {
        if (instance == null)
            instance = new HueHelper();
        return instance;
    }

    /**
     * Returns a map of all the lights in the selected bridge's resource cache
     *
     * @return The map of all lights
     */
    public Map<String, PHLight> getLights() {
        return phHueSDK.getSelectedBridge().getResourceCache().getLights();
    }

    /**
     * Returns the next light in the list of lights in use
     *
     * @return The next light
     * @throws HueHelperException Thrown if there are no lights in use
     */
    public synchronized PHLight getNextLight() throws HueHelperException {
        if (lightsInUse.size() < 0) throw new HueHelperException("No lights in use");
        else {
            if (lastLight >= lightsInUse.size())
                lastLight = 0;
            if(lightsInUse.isEmpty())
                return null;
            return lightsInUse.get(lastLight++);
        }
    }

    /**
     * Flips the light on
     *
     * @param light The light to change
     */
    public void toggleLightOn(PHLight light) throws HueHelperException {
        // Get the real status of the light
        List<PHLight> lights = phHueSDK.getSelectedBridge().getResourceCache().getAllLights();
        for (PHLight l : lights) {
            if (l.getName().equals(light.getName())) {
                light = l;
            }
        }
        if (light.getLastKnownLightState().isOn())
            throw new HueHelperException("Light is already on");
        else {
            PHLightState lightState = new PHLightState();
            lightState.setOn(true);
            Log.d(TAG, "toggleLightOn: Toggling Light State: " + lightState.isOn());
            phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
        }
    }

    /**
     * Flips the light off
     *
     * @param light The light to change
     */
    public void toggleLightOff(PHLight light) throws HueHelperException {
        // Get the real status of the light
        List<PHLight> lights = phHueSDK.getSelectedBridge().getResourceCache().getAllLights();
        for (PHLight l : lights) {
            if (l.getName().equals(light.getName())) {
                light = l;
            }
        }
        if (!light.getLastKnownLightState().isOn())
            throw new HueHelperException("Light is already off");
        else {
            PHLightState lightState = new PHLightState();
            lightState.setOn(false);
            Log.d(TAG, "toggleLightOn: Toggling Light State: " + lightState.isOn());
            phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
        }
    }

    /**
     * Sets the brightness of the passed light
     *
     * @param light The light to change
     * @param bri   The brightness to change to
     * @throws HueHelperException Thrown if brightness is out of bounds
     */
    public void setBrightness(PHLight light, int bri) throws HueHelperException {
        if (bri < 0 || bri > 254)
            throw new HueHelperException("Brightness must be between 0 and 254");
        else {
            PHLightState lightState = new PHLightState();
            lightState.setBrightness(bri);
            Log.d(TAG, "setBrightness: Setting brightness to " + bri);
            phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
        }
    }

    /**
     * Sets the brightness of the light with a transition time
     *
     * @param light The light to change
     * @param bri   The brightness to change to
     * @param tTime The transition time given as a multiple of 100ms
     * @throws HueHelperException Thrown if brightness is out of bounds
     */
    public void setBrightness(PHLight light, int bri, int tTime) throws HueHelperException {
        if (bri < 0 || bri > 254)
            throw new HueHelperException("Brightness must be between 0 and 254");
        else {
            PHLightState lightState = new PHLightState();
            lightState.setBrightness(bri);
            lightState.setTransitionTime(tTime);
            Log.d(TAG, "setBrightness: Setting brightness to " + bri);
            phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
        }
    }

    /**
     * Sets the hue of the light
     *
     * @param light The light to change
     * @param hue   The hue to change to
     * @throws HueHelperException Thrown if hue is out of bounds
     */
    public void setHue(PHLight light, int hue) throws HueHelperException {
        if (hue < 0 || hue > 65535) throw new HueHelperException("Hue must be between 0 and 65535");
        else {
            PHLightState lightState = new PHLightState();
            lightState.setHue(hue);
            Log.d(TAG, "setHue: Setting hue to " + hue);
            phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
        }
    }

    /**
     * Sets the hue of the light with a transition time
     *
     * @param light The light to change
     * @param hue   The hue to change to
     * @param tTime The transition time given as a multiple of 100ms
     * @throws HueHelperException Thrown if the hue is out of bounds
     */
    public void setHue(PHLight light, int hue, int tTime) throws HueHelperException {
        if (hue < 0 || hue > 65535) throw new HueHelperException("Hue must be between 0 and 65535");
        else {
            PHLightState lightState = new PHLightState();
            lightState.setHue(hue);
            lightState.setTransitionTime(tTime);
            Log.d(TAG, "setHue: Setting hue to " + hue);
            phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
        }
    }

    /**
     * Sets the saturation of the light
     *
     * @param light The light to change
     * @param sat   The saturation to change to
     * @throws HueHelperException Thrown if the saturation is out of bounds
     */
    public void setSaturation(PHLight light, int sat) throws HueHelperException {
        if (sat < 0 || sat > 254)
            throw new HueHelperException("Saturation must be between 0 and 254");
        else {
            PHLightState lightState = new PHLightState();
            lightState.setSaturation(sat);
            Log.d(TAG, "setSaturation: Setting Saturation to " + sat);
            phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
        }
    }

    /**
     * Sets the saturation of the light with a transition time
     *
     * @param light The light to change
     * @param sat   The saturation to change to
     * @param tTime The transition time given as a multiple of 100ms
     * @throws HueHelperException Thrown if the saturation is out of bounds
     */
    public void setSaturation(PHLight light, int sat, int tTime) throws HueHelperException {
        if (sat < 0 || sat > 254)
            throw new HueHelperException("Saturation must be between 0 and 254");
        else {
            PHLightState lightState = new PHLightState();
            lightState.setSaturation(sat);
            lightState.setTransitionTime(tTime);
            Log.d(TAG, "setSaturation: Setting Saturation to " + sat);
            phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
        }
    }

    /**
     * Sets the X and Y coordinates in CIE color space for a light
     *
     * @param light The light to change
     * @param x     The x-coordinate of the color in CIE color space
     * @param y     The y-coordinate of the color in CIE color space
     * @throws HueHelperException Thrown if the x or y coordinates are out of bounds
     */
    public void setXY(PHLight light, float x, float y) throws HueHelperException {
        if (x < 0 || x > 1) throw new HueHelperException("X must be between 0.0 and 1.0");
        else if (y < 0 || y > 1) throw new HueHelperException("Y must be between 0.0 and 1.0");
        else {
            PHLightState lightState = new PHLightState();
            lightState.setX(x);
            lightState.setY(y);
            Log.d(TAG, "setXY: Setting X to " + x + " and Y to " + y);
            phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
        }
    }

    /**
     * Sets the X and Y coordinates in CIE color space for a light with a transition time
     *
     * @param light The light to change
     * @param x     The x-coordinate of the color in CIE color space
     * @param y     The y-coordinate of the color in CIE color space
     * @param tTime The transition time given as a multiple of 100ms
     * @throws HueHelperException Thrown if the x or y coordinates are out of bounds
     */
    public void setXY(PHLight light, float x, float y, int tTime) throws HueHelperException {
        if (x < 0 || x > 1) throw new HueHelperException("X must be between 0.0 and 1.0");
        else if (y < 0 || y > 1) throw new HueHelperException("Y must be between 0.0 and 1.0");
        else {
            PHLightState lightState = new PHLightState();
            lightState.setX(x);
            lightState.setY(y);
            lightState.setTransitionTime(tTime);
            Log.d(TAG, "setXY: Setting X to " + x + " and Y to " + y);
            phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
        }
    }

    /**
     * Sets the Mired Color Temperature of the light
     *
     * @param light The light to change
     * @param ct    The Mired Color Temperature to change to
     * @throws HueHelperException Thrown if ct is out of bounds
     */
    public void setCT(PHLight light, int ct) throws HueHelperException {
        if (ct < 0 || ct > 65535)
            throw new HueHelperException("Mired Color must be between 0 and 65535");
        else {
            PHLightState lightState = new PHLightState();
            lightState.setCt(ct);
            Log.d(TAG, "setCT: Setting ct to " + ct);
            phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
        }
    }

    /**
     * Sets the Mired Color Temperature of the light
     *
     * @param light The light to change
     * @param ct    The Mired Color Temperature to change to
     * @param tTime The transition time given as a multiple of 100ms
     * @throws HueHelperException Thrown if ct is out of bounds
     */
    public void setCT(PHLight light, int ct, int tTime) throws HueHelperException {
        if (ct < 0 || ct > 65535)
            throw new HueHelperException("Mired Color must be between 0 and 65535");
        else {
            PHLightState lightState = new PHLightState();
            lightState.setCt(ct);
            lightState.setTransitionTime(tTime);
            Log.d(TAG, "setCT: Setting ct to " + ct);
            phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
        }
    }

    private PHLightListener lightListener = new PHLightListener() {
        @Override
        public void onReceivingLightDetails(PHLight phLight) {
            Log.d(TAG, "onReceivingLightDetails: ");
        }

        @Override
        public void onReceivingLights(List<PHBridgeResource> list) {
            Log.d(TAG, "onReceivingLights: ");
        }

        @Override
        public void onSearchComplete() {
            Log.d(TAG, "onSearchComplete: ");
        }

        @Override
        public void onSuccess() {
            Log.d(TAG, "onSuccess: ");
        }

        @Override
        public void onError(int i, String s) {
            Log.d(TAG, "onError: ");
        }

        @Override
        public void onStateUpdate(Map<String, String> map, List<PHHueError> list) {
            Log.d(TAG, "onStateUpdate: ");
        }
    };

    public synchronized ArrayList<PHLight> getLightsInUse() {
        return lightsInUse;
    }

    public synchronized void setLightsInUse(ArrayList<PHLight> lightsInUse) {
        this.lightsInUse = lightsInUse;
    }

    public synchronized void addLightInUse(PHLight light) {
        lightsInUse.add(light);
        lightsInUseNames.add(light.getName());
        if(sharedPreferences != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String tmp = "";
            for(String lName : lightsInUseNames)
                tmp += lName + ",";
            editor.putString(PREFS,tmp.substring(0,tmp.length()-1));
            editor.apply();
        }
    }

    public synchronized PHLight deleteLightInUse(PHLight light) {
        PHLight toReturn = null;
        for (int i = 0; i < lightsInUse.size(); i++) {
            PHLight l = lightsInUse.get(i);
            if (l.getName().equals(light.getName())) {
                toReturn = l;
                lightsInUseNames.remove(l.getName());
                lightsInUse.remove(i);
                break;
            }
        }
        if(sharedPreferences != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String tmp = "";
            for(String lName : lightsInUseNames)
                tmp += lName + ",";
            if(tmp.length() > 0)
                editor.putString(PREFS,tmp.substring(0,tmp.length()-1));
            else
                editor.putString(PREFS,tmp);
            editor.apply();
        }
        return toReturn;
    }

    public void rebuildLightsInUse() throws HueHelperException{
        if(sharedPreferences == null){
            throw new HueHelperException("No shared preferences");
        }
        lightsInUse = new ArrayList<>();
        lightsInUseNames = new ArrayList<>();
        String lightString = sharedPreferences.getString(PREFS,"");
        Map<String, PHLight> lights = getLights();
        for(String light : lightString.split(",")){
            for(PHLight l : lights.values()){
                if(l.getName().equals(light)){
                    lightsInUse.add(l);
                    lightsInUseNames.add(l.getName());
                }
            }
        }
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }
}
