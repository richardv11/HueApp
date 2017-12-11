package a477.hueapp;

import com.philips.lighting.model.PHLight;

import a477.hueapp.hue.HueHelper;

/**
 * Created by hshin23 on 12/10/2017.
 */


class HueProcessor {

    float prev_brightness;
    float prev_hue;
    float prev_saturation;
    float prev_x;
    float prev_y;
    float prev_ct;

    HueProcessor() {
        prev_brightness = (float) 0.0;
        prev_hue = (float) 0.0;
        prev_saturation = (float) 0.0;
        prev_x = (float) 0.0;
        prev_y = (float) 0.0;
        prev_ct = (float) 0.0;
    }

    // list of HueHelper functions
    // set brightness : sets brightness
    // set hue : sets Hue
    // set saturation : sets Saturation
    // set XY : sets XY coordinates in color space
    // set CT : sets MIRED COLOR TEMP
    void process(HueHelper hue, float prev_pitch, float curr_pitch) {
        // do stuff with HueHelper
    }


}
