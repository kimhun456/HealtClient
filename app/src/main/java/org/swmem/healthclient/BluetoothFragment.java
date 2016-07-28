package org.swmem.healthclient;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.swmem.healthclient.db.HealthContract;
import org.swmem.healthclient.service.BTCTemplateService;
import org.swmem.healthclient.utils.Constants;
import org.swmem.healthclient.utils.Logs;
import org.swmem.healthclient.graph.MyEntry;
import org.swmem.healthclient.graph.MyMarkerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.sql.Date;


public class BluetoothFragment extends Fragment {


    private static final java.lang.String TAG = "BluetoothFragment";
    TextView lastValueText;
    LineChart chart;
    long limitDays;

    private final int SECONDS = 1000;
    private final int MINUTES = 60 * SECONDS;
    private final int HOURS = 60 * MINUTES;
    private final int DAYS = 24 * HOURS;

    private BTCTemplateService mService;

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


        limitDays = Long.parseLong(PreferenceManager
                .getDefaultSharedPreferences(getActivity().getBaseContext())
                .getString(getString(R.string.pref_limit_day_key),"1"));

        View rootView = inflater.inflate(R.layout.fragment_bluetooth, container, false);


        lastValueText = (TextView)rootView.findViewById(R.id.lastValueText);


        updateData(rootView);


        // Inflate the layout for this fragment
        return rootView;
    }


    private void updateData(View rootView){

        updateChart(rootView);

        // 모든 데이터를 불러오게 된다.
        Cursor cursor = getActivity().getContentResolver().query(
                HealthContract.GlucoseEntry.CONTENT_URI,
                DETAIL_COLUMNS,
                null,
                null,
                null);


        long lastDate = 0;
        double lastValue = 0;
        while (cursor.moveToNext()) {
            long currentDate = cursor.getLong(COL_GLUCOSE_TIME);
            if(currentDate > lastDate){
                lastDate = currentDate;
                lastValue = cursor.getDouble(COL_GLUCOSE_GLUCOSE_VALUE);
            }

        }

        if(lastValue != 0){
            lastValueText.setText(""+lastValue);
        }
        cursor.close();
    }

    private void updateChart(View rootView){

        // 모든 데이터를 불러오게 된다.
        Cursor cursor = getActivity().getContentResolver().query(
                HealthContract.GlucoseEntry.CONTENT_URI,
                DETAIL_COLUMNS,
                null,
                null,
                null);

        if(cursor == null || cursor.getCount() == 0){

            Log.v(TAG,"Cursor has null or no data");
            return ;
        }

        int BLUETOOTH_COLOR = ContextCompat.getColor(getContext(),R.color.deep_blue);
        int NFC_COLOR = ContextCompat.getColor(getContext(),R.color.deep_orange);
        int UPPER_LIMIT_COLOR = ContextCompat.getColor(getContext(),R.color.deep_red);
        int DOWN_LIMIT_COLOR = ContextCompat.getColor(getContext(),R.color.sunshine_dark_blue);

        int lastDataIndex = 0;

        Float.parseFloat(PreferenceManager
                .getDefaultSharedPreferences(getActivity().getBaseContext())
                .getString(getString(R.string.pref_Hyperglycemia_key),"120"));

        float highGlucose = Float.parseFloat(PreferenceManager
                .getDefaultSharedPreferences(getActivity().getBaseContext())
                .getString(getString(R.string.pref_Hyperglycemia_key),"120"));
        float lowGlucose = Float.parseFloat(PreferenceManager
                .getDefaultSharedPreferences(getActivity().getBaseContext())
                .getString(getString(R.string.pref_Hypotension_key),"80"));

        long currentMilliseconds = System.currentTimeMillis();
        long pastMilliseconds = currentMilliseconds - (limitDays * DAYS);

        ArrayList<String> xaxisValues = getXaxisValues(currentMilliseconds);

        ArrayList<MyEntry> myEntries = new ArrayList<>();
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        ArrayList<String> xAxisValues = getXAxisValues(currentMilliseconds);


        chart = (LineChart) rootView.findViewById(R.id.chart);

        while (cursor.moveToNext()) {

            long currentDate = Utility.cursorDateToLong(cursor.getString(COL_GLUCOSE_TIME));

            if(currentDate >= pastMilliseconds && currentDate <= currentMilliseconds){

                float rawValue = (float)cursor.getDouble(COL_GLUCOSE_GLUCOSE_VALUE);
                String type = cursor.getString(COL_GLUCOSE_TYPE);
                int index = getIndexOfEntries(currentDate,currentMilliseconds);

                Log.v ("cursor" ,"date : " +  Utility.formatDate(currentDate));
                Log.v ("cursor" ,"type : " +  type);
                Log.v("cursor",  "RAW VALUE :  " +rawValue);
                Log.v ("cursor" ,"index : " +  index );
                Log.v ("cursor" ,"______________________");

                if(index < 0){
                    continue;
                }
                if(index > lastDataIndex){
                    lastDataIndex = index;
                }

                if(type.equals(HealthContract.GlucoseEntry.BLEUTOOTH)){
                    myEntries.add(new MyEntry(index,rawValue,BLUETOOTH_COLOR));
                }else{
                    myEntries.add(new MyEntry(index,rawValue,NFC_COLOR));
                }

            }
        }

        // sort the entries ascending order by index.
        Collections.sort(myEntries, new Comparator<MyEntry>() {
            @Override
            public int compare(MyEntry t1, MyEntry t2) {
                if(t1.getIndex() > t2.getIndex())
                    return 0;
                else
                    return -1;
            }
        });


        for(MyEntry myEntry : myEntries){
            entries.add(new Entry(myEntry.getValue(), myEntry.getIndex()));
            colors.add(myEntry.getColor());
        }


        // 리미트 라인 설정하는 곳
        LimitLine ll1 = new LimitLine(highGlucose, getString(R.string.upper_limit));
        ll1.setLineWidth(4f);
        ll1.enableDashedLine(10f, 5f, 0f);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll1.setTextSize(14f);
        ll1.setTextColor(UPPER_LIMIT_COLOR);
        ll1.setLineColor(UPPER_LIMIT_COLOR);

        LimitLine ll2 = new LimitLine(lowGlucose, getString(R.string.down_limit));
        ll2.setLineWidth(4f);
        ll2.enableDashedLine(10f, 5f, 0f);
        ll2.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        ll2.setTextSize(14f);
        ll2.setTextColor(DOWN_LIMIT_COLOR);
        ll2.setLineColor(DOWN_LIMIT_COLOR);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.addLimitLine(ll1);
        leftAxis.addLimitLine(ll2);


        // 데이터 세트 설정
        LineDataSet lineDataSet = new LineDataSet(entries, "RAW VALUE");
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setValueTextSize(0f);
        lineDataSet.setCircleColors(colors);
        lineDataSet.setColors(colors);
        lineDataSet.setHighLightColor(ContextCompat.getColor(getContext(),R.color.black));
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet);
        LineData data = new LineData(xAxisValues, dataSets);


        // 레헨드 셋팅
        Legend legend = chart.getLegend();
        legend.setCustom(new int[]{BLUETOOTH_COLOR,NFC_COLOR}, new String[] { "BlueTooth", "NFC" });


        // 차트 설정들
        chart.setDescription(getString(R.string.chart_description));
        chart.setTouchEnabled(true);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setScaleYEnabled(false);
        chart.setData(data);
        chart.zoom(30f,1f,1f,1f);

        if(lastDataIndex - 40 > 0){
            chart.moveViewToX(lastDataIndex-40);
        }else if(lastDataIndex - 20 > 0){
            chart.moveViewToX(lastDataIndex-20);
        }else if(lastDataIndex - 10 > 0){
            chart.moveViewToX(lastDataIndex-10);
        }else{
            chart.moveViewToX(lastDataIndex);
        }

        chart.setKeepPositionOnRotation(true);
        chart.setMarkerView(new MyMarkerView(getContext(), R.layout.marker_view));
        chart.invalidate(); // refresh

        cursor.close();
    }

    private ArrayList<String> getXAxisValues(long currentTimeMillis){

        ArrayList<String> xValues = new ArrayList<String>();


        Log.v("current TIme" , Utility.formatDate(currentTimeMillis));

        currentTimeMillis -= limitDays * DAYS;

        Log.v("past TIme" , Utility.formatDate(currentTimeMillis));


        for(long i = 0; i<= limitDays * DAYS; i+=MINUTES ){

            xValues.add(Utility.getGraphDateFormat(currentTimeMillis + i));
        }

        return xValues;
    }

    private int getIndexOfEntries(long findMiiliSeconds , long currentTimeMillis){

        long pastMilliseconds = currentTimeMillis - limitDays * DAYS;


        long diff = findMiiliSeconds - pastMilliseconds;


        int index  = (int) (diff /=MINUTES);

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

                doScan();
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private void doScan() {
        Intent intent = new Intent(getActivity(), DeviceListActivity.class);
        getActivity().startActivityForResult(intent, Constants.REQUEST_CONNECT_DEVICE);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}
