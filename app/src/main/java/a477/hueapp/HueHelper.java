package a477.hueapp;

import com.philips.lighting.hue.sdk.PHHueSDK;

public class HueHelper {

    private final PHHueSDK phHueSDK;
        private static final String appName = "";

    public HueHelper(){
        phHueSDK = PHHueSDK.create();
        phHueSDK.setAppName("");     // e.g. phHueSDK.setAppName("QuickStartApp");
        phHueSDK.setDeviceName("");  // e.g. If you are programming for Android: phHueSDK.setDeviceName(android.os.Build.MODEL);
    }

}
