package org.swmem.healthclient;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.swmem.healthclient.db.HealthContract;

import java.util.ArrayList;


public class BluetoothFragment extends Fragment {


    TextView lastValueText;
    LineChart chart;
    long limitDays;

    private final int SECONDS = 1000;
    private final int MINUTES = 60 * SECONDS;
    private final int HOURS = 60 * MINUTES;
    private final int DAYS = 24 * HOURS;

    private static final String[] DETAIL_COLUMNS = {
            HealthContract.GlucoseEntry.TABLE_NAME + "." + HealthContract.GlucoseEntry._ID,
            HealthContract.GlucoseEntry.COLUMN_GLUCOSE_VALUE,
            HealthContract.GlucoseEntry.COLUMN_TEMPERATURE_VALUE,
            HealthContract.GlucoseEntry.COLUMN_RAW_VALUE,
            HealthContract.GlucoseEntry.COLUMN_DEVICE_ID,
            HealthContract.GlucoseEntry.COLUMN_TIME,
            HealthContract.GlucoseEntry.COLUMN_TYPE
    };

    // These indices are tied to DETAIL_COLUMNS.  If DETAIL_COLUMNS changes, these
    // must change.
    public static final int COL_GLUCOSE_ID = 0;
    public static final int COL_GLUCOSE_GLUCOSE_VALUE = 1;
    public static final int COL_GLUCOSE_TEMPEATURE_VALUE = 2;
    public static final int COL_GLUCOSE_RAW_VALUE = 3;
    public static final int COL_GLUCOSE_DEVICE_ID = 4;
    public static final int COL_GLUCOSE_TIME = 5;
    public static final int COL_GLUCOSE_TYPE = 6;



    public BluetoothFragment() {
        // Required empty public constructor


    }

    @Override
    public void onResume() {
        super.onResume();
//        chart.invalidate();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        String limitday = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext()).getString(getString(R.string.pref_limit_day_key),"1");
        limitDays = Long.parseLong(limitday);


        Log.v("cursor", "asdfasfsd");

        View rootView = inflater.inflate(R.layout.fragment_bluetooth, container, false);


        lastValueText = (TextView)rootView.findViewById(R.id.lastValueText);


        updateData(rootView);


        // Inflate the layout for this fragment
        return rootView;
    }


    private void updateData(View rootView){



        ContentResolver contentResolver = getActivity().getContentResolver();

        // 모든 데이터를 불러오게 된다.
        Cursor cursor = contentResolver.query(HealthContract.GlucoseEntry.CONTENT_URI,
                DETAIL_COLUMNS,
                null,
                null,
                null);


        updateChart(rootView,cursor);

        if (cursor != null) {
            try{

                if(cursor.moveToLast()){
                    lastValueText.setText(Double.toString(cursor.getDouble(COL_GLUCOSE_RAW_VALUE)));
                }

            } finally {
                cursor.close();
            }

        }

    }

    private void updateChart(View rootView,Cursor cursor){

        chart = (LineChart) rootView.findViewById(R.id.chart);

//        chart.setDescription("asdf");
        chart.setTouchEnabled(true);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setScaleYEnabled(false);


        long currentMilliseconds = System.currentTimeMillis();
        long pastMilliseconds = currentMilliseconds - limitDays * DAYS;

        ArrayList<String> xaxisValues = getXaxisValues(currentMilliseconds);

        ArrayList<Entry> entries = new ArrayList<Entry>();


        cursor.moveToFirst();

//        while (cursor.moveToNext()) {
//
//            Log.v("cursor",  "ID = " + cursor.getInt(COL_GLUCOSE_ID) + " RAW VALUE =  " + cursor.getDouble(COL_GLUCOSE_RAW_VALUE));
//            Log.v ("cursor" ,"date : " +  Utility.formatDate(cursor.getLong(COL_GLUCOSE_TIME)));
//
//
//            long currentDate = cursor.getLong(COL_GLUCOSE_TIME);
//
//            if(currentDate >= pastMilliseconds && currentDate <= currentMilliseconds){
//
//
//                float rawValue = (float)cursor.getDouble(COL_GLUCOSE_RAW_VALUE);
//
//                Log.v("cursor", "value : " + rawValue+ "index : " + Utility.getIndexOfEntry(currentDate,currentMilliseconds));
//
//
//                entries.add(new Entry(rawValue, Utility.getIndexOfEntry(currentDate,currentMilliseconds)));
//
//
//            }
//        }



        for(long i=pastMilliseconds ; i<=currentMilliseconds; i+=MINUTES){

            float rawValue = (float) (Math.random()*40 + 60);

            entries.add(new Entry(rawValue, getIndexOfEntry(i,currentMilliseconds)));

        }


        LineDataSet lineDataSet = new LineDataSet(entries, "RAW VALUE");
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(lineDataSet);
        LineData data = new LineData(xaxisValues, dataSets);


        chart.setData(data);
        chart.zoom(22f,1f,1f,1f);
        chart.moveViewToX(xaxisValues.size()-1);

        chart.invalidate(); // refresh
    }

    private ArrayList<String> getXaxisValues(long currentTimeMillis){

        ArrayList<String> xValues = new ArrayList<String>();


        Log.v("current TIme" , Utility.formatDate(currentTimeMillis));

        currentTimeMillis -= limitDays * DAYS;

        Log.v("past TIme" , Utility.formatDate(currentTimeMillis));


        for(long i = 0; i<= limitDays * DAYS; i+=MINUTES ){


            xValues.add(Utility.getGraphDateFormat(currentTimeMillis + i));

        }

        return xValues;
    }

    private int getIndexOfEntry(long findMiiliSeconds , long currentTimeMillis){

        long pastMilliseconds = currentTimeMillis - limitDays * DAYS;


        long diff = findMiiliSeconds - pastMilliseconds;


        int index  = (int) (diff /=MINUTES) + 1;

        return index;

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.bluetooth, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        //noinspection SimplifiableIfStatement

        switch (id){
            case R.id.scan:

                Toast.makeText(getActivity().getBaseContext(),"scan",Toast.LENGTH_LONG).show();

                break;

        }

        return super.onOptionsItemSelected(item);
    }
}
