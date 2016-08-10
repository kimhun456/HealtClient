package org.swmem.healthclient.graph;

import android.content.Context;
import android.preference.PreferenceManager;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;

import org.swmem.healthclient.R;


public class MyMarkerView extends MarkerView {

    private TextView tvContent;
    private Context context;
    private String dataFormat;

    public MyMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        // this markerview only displays a textview
        tvContent = (TextView) findViewById(R.id.tvContent);
        this.context = context;
        dataFormat = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_data_format_key),context.getString(R.string.pref_data_format_mgdl));
    }

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @Override
    public void refreshContent(Entry e, Highlight highlight) {

        if(dataFormat.equals(context.getString(R.string.pref_data_format_mmol))){

            tvContent.setText(String.format("%.1f",e.getVal()));
        }else{
            tvContent.setText(String.format("%.0f",e.getVal()));
        }

    }

    @Override
    public int getXOffset(float xpos) {
        // this will center the marker-view horizontally
        return -(getWidth() / 2);
    }

    @Override
    public int getYOffset(float ypos) {
        // this will cause the marker-view to be above the selected value
        return -getHeight() + 9;
    }
}
