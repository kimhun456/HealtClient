package org.swmem.healthclient;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;

import org.swmem.healthclient.db.HealthContract;
import org.swmem.healthclient.graph.GraphLoadTask;
import org.swmem.healthclient.utils.Constants;
import org.swmem.healthclient.graph.MyMarkerView;


public class BluetoothFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{



    private final int SECONDS = 1000;
    private final int MINUTES = 60 * SECONDS;
    private final int HOURS = 60 * MINUTES;
    private final int DAYS = 24 * HOURS;
    private long limitDays;
    private View rootView;

    private static final String[] DETAIL_COLUMNS = {
            HealthContract.GlucoseEntry.TABLE_NAME + "." + HealthContract.GlucoseEntry._ID,
            HealthContract.GlucoseEntry.COLUMN_GLUCOSE_VALUE,
            HealthContract.GlucoseEntry.COLUMN_TEMPERATURE_VALUE,
            HealthContract.GlucoseEntry.COLUMN_RAW_VALUE,
            HealthContract.GlucoseEntry.COLUMN_DEVICE_ID,
            HealthContract.GlucoseEntry.COLUMN_TIME,
            HealthContract.GlucoseEntry.COLUMN_TYPE
    };

    public static final int GRAPH_LODAER_ID = 0;

    private static final java.lang.String TAG = "BluetoothFragment";


    public BluetoothFragment() {
        // Required empty public constructor

    }

    @Override
    public void onResume() {
        super.onResume();
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
                .getDefaultSharedPreferences(getContext())
                .getString(getContext().getString(R.string.pref_limit_day_key),"1"));
        getLoaderManager().initLoader(GRAPH_LODAER_ID,null,this);
        rootView = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        LineChart chart = (LineChart) rootView.findViewById(R.id.chart);
        setUpChart(chart);

//        updateData(rootView);

        return rootView;
    }



    private void updateData(Cursor cursor){
        new GraphLoadTask(getContext(), rootView).execute(cursor);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.bluetooth, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id){
            case R.id.scan:
                doScan();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    private void setUpChart(LineChart chart){


        int BLUETOOTH_COLOR = ContextCompat.getColor(getContext(),R.color.deep_blue);
        int NFC_COLOR = ContextCompat.getColor(getContext(),R.color.deep_orange);
        int UPPER_LIMIT_COLOR = ContextCompat.getColor(getContext(),R.color.deep_red);
        int DOWN_LIMIT_COLOR = ContextCompat.getColor(getContext(),R.color.sunshine_dark_blue);

        float highGlucose = Float.parseFloat(PreferenceManager
                .getDefaultSharedPreferences(getActivity().getBaseContext())
                .getString(getString(R.string.pref_Hyperglycemia_key),"120"));
        float lowGlucose = Float.parseFloat(PreferenceManager
                .getDefaultSharedPreferences(getActivity().getBaseContext())
                .getString(getString(R.string.pref_Hypotension_key),"80"));


        // 리미트 라인 설정하는 곳
        LimitLine ll1 = new LimitLine(highGlucose, getString(R.string.upper_limit));
        ll1.setLineWidth(4f);
        ll1.enableDashedLine(10f, 5f, 0f);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
        ll1.setTextSize(14f);
        ll1.setTextColor(UPPER_LIMIT_COLOR);
        ll1.setLineColor(UPPER_LIMIT_COLOR);

        LimitLine ll2 = new LimitLine(lowGlucose, getString(R.string.down_limit));
        ll2.setLineWidth(4f);
        ll2.enableDashedLine(10f, 5f, 0f);
        ll2.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_BOTTOM);
        ll2.setTextSize(14f);
        ll2.setTextColor(DOWN_LIMIT_COLOR);
        ll2.setLineColor(DOWN_LIMIT_COLOR);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.addLimitLine(ll1);
        leftAxis.addLimitLine(ll2);

        // 레헨드 셋팅
        Legend legend = chart.getLegend();
        legend.setCustom(new int[]{BLUETOOTH_COLOR,NFC_COLOR}, new String[] { "BlueTooth", "NFC" });


        // 차트 설정들
        chart.setDescription(getString(R.string.chart_description));
        chart.setTouchEnabled(true);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setScaleYEnabled(false);
        chart.zoom(30f,1f,1f,1f);
        chart.setKeepPositionOnRotation(true);
        chart.setMarkerView(new MyMarkerView(getContext(), R.layout.marker_view));
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


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {


        long pastMilliseconds = System.currentTimeMillis() - (limitDays * DAYS);
        String[] selectionArgs = {""};
        selectionArgs[0] =  Utility.formatDate(pastMilliseconds);
        String WHERE_DATE_BY_LIMIT_DAYS = HealthContract.GlucoseEntry.COLUMN_TIME + " > ?" ;

        return new CursorLoader(getActivity(),
                HealthContract.GlucoseEntry.CONTENT_URI,
                DETAIL_COLUMNS,
                WHERE_DATE_BY_LIMIT_DAYS,
                selectionArgs,
                null
        );

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        Log.v(TAG , "onLoadFinished");
        updateData(data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        Log.v(TAG , "onLoaderReset");

    }

}
