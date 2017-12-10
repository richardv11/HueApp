package a477.hueapp;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

/**
 * Created by Hansol Shin on 12/8/2017.
 */
public class TarsosHelper {

    private static TarsosHelper instance;

    private static AudioDispatcher dispatcher;
    private static PitchDetectionHandler handler;
    private static AudioProcessor processor;

    private TarsosHelper(final Activity activity) {
        this.dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
        this.handler = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent audioEvent) {
                final float pitchInHz = result.getPitch();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView text = (TextView) activity.findViewById(R.id.textView1);
                        text.setText("" + pitchInHz);
                    }
                });


            }
        };
        this.processor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, handler);
    }

    public TarsosHelper getInstance(Context context) {
        if (this.instance == null) {


        }
        return this.instance;
    }

}
