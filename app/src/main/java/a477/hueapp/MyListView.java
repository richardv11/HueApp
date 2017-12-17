package a477.hueapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.philips.lighting.model.PHLight;

import java.util.ArrayList;

import a477.hueapp.hue.HueHelper;

/**
 * Created by Richard on 12/16/2017.
 * Custom ListView to enable text and also a toggle button
 * Logic for the lights the user desires for use in the run is done here
 */

public class MyListView extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;

    public MyListView(Context context, ArrayList<String> values) {
        super(context, R.layout.activity_main, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View rowView = inflater.inflate(R.layout.my_list_row, parent, false);

        final TextView textView = (TextView) rowView.findViewById(R.id.lightText);
        final ImageButton imageButton = (ImageButton) rowView.findViewById(R.id.toggle_image);

        for(PHLight light : HueHelper.getInstance().getLightsInUse()){
            if (values.get(position).equals(light.getName())) {
                imageButton.setSelected(true);
                break;
            }
        }

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Set the button's appearance
                imageButton.setSelected(!imageButton.isSelected());

                if (imageButton.isSelected()) {
                    // Light is desired by user for this run
                    for(PHLight light : HueHelper.getInstance().getLights().values()){
                        if(light.getName().equals(textView.getText().toString())){
                            HueHelper.getInstance().addLightInUse(light);
                            break;
                        }
                    }
                } else {
                    //Light is not desired by user for this run
                    for(PHLight light : HueHelper.getInstance().getLights().values()){
                        if(light.getName().equals(textView.getText().toString())){
                            HueHelper.getInstance().deleteLightInUse(light);
                            break;
                        }
                    }
                }
            }
        });
        textView.setText(values.get(position));
        return rowView;
    }
}