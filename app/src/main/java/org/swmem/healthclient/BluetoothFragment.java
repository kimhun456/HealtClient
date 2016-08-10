package org.swmem.healthclient;

import android.content.Intent;
import android.database.Cursor;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

import org.swmem.healthclient.db.HealthContract;
import org.swmem.healthclient.graph.GraphLoadTask;
import org.swmem.healthclient.graph.MyMarkerView;
import org.swmem.healthclient.utils.Constants;
import org.swmem.healthclient.utils.Utility;


public class BluetoothFragment extends Fragment implements LoaderManager .LoaderCallbacks<Cursor>{



    private final int SECONDS = 1000;
    private final int MINUTES = 60 * SECONDS;
    private final int HOURS = 60 * MINUTES;
    private final int DAYS = 24 * HOURS;
    private long limitHours;
    private View rootView;
    private LineChart chart;

    public static NfcAdapter nfcAdapter;

    private static final String[] DETAIL_COLUMNS = {
            HealthContract.GlucoseEntry.TABLE_NAME + "." + HealthContract.GlucoseEntry._ID,
            HealthContract.GlucoseEntry.COLUMN_GLUCOSE_VALUE,
            HealthContract.GlucoseEntry.COLUMN_TEMPERATURE_VALUE,
            HealthContract.GlucoseEntry.COLUMN_RAW_VALUE,
            HealthContract.GlucoseEntry.COLUMN_DEVICE_ID,
            HealthContract.GlucoseEntry.COLUMN_TIME,
            HealthContract.GlucoseEntry.COLUMN_TYPE
    };

    public static final int GRAPH_LOADER_ID = 0;

    private static final java.lang.String TAG = "BluetoothFragment";


    public BluetoothFragment() {

    }

    @Override
    public void onResume() {
        super.onResume();
        setUpChart(chart);

        if(limitHours != Long.parseLong(
                PreferenceManager.getDefaultSharedPreferences(getContext())
                        .getString(getContext().getString(R.string.pref_limit_hours_key),
                        getContext().getString(R.string.pref_limit_hours_24)))){

            updateData();

        }


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        limitHours = Long.parseLong(PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .getString(getContext().getString(R.string.pref_limit_hours_key), getContext().getString(R.string.pref_limit_hours_24)));
        getLoaderManager().initLoader(GRAPH_LOADER_ID,null,this);
        rootView = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        chart = (LineChart) rootView.findViewById(R.id.chart);
        setUpChart(chart);

        return rootView;
    }



    private void updateData(){
        new GraphLoadTask(getContext(), rootView).execute();
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
            case R.id.scan_menu:
                doScan();
                break;

            case R.id.nfc_menu:
                doNfc();
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
                .getString(getString(R.string.pref_Hyperglycemia_key),"200"));
        float lowGlucose = Float.parseFloat(PreferenceManager
                .getDefaultSharedPreferences(getActivity().getBaseContext())
                .getString(getString(R.string.pref_Hypoglycemia_key),"80"));

        float textSize = 16;


        // 리미트 라인 설정하는 곳
        LimitLine ll1 = new LimitLine(highGlucose);
        ll1.setLineWidth(1.5f);
        ll1.setLineColor(UPPER_LIMIT_COLOR);

        LimitLine ll2 = new LimitLine(lowGlucose);
        ll2.setLineWidth(1.5f);
        ll2.setLineColor(DOWN_LIMIT_COLOR);

        chart.getAxisRight().setEnabled(false);


        //X축 셋팅

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextSize(11f);



        // Y축 세팅
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextSize(textSize);
        leftAxis.removeAllLimitLines();
        leftAxis.addLimitLine(ll1);
        leftAxis.addLimitLine(ll2);

        leftAxis.setAxisMinValue(40f);
        leftAxis.setAxisMaxValue(400f);



        // 레헨드 셋팅
        Legend legend = chart.getLegend();
        legend.setTextSize(textSize);
        legend.setCustom(new int[]{BLUETOOTH_COLOR,NFC_COLOR}, new String[] { "BlueTooth", "NFC" });


        // 차트 설정들
        chart.setDescription("");
        chart.setTouchEnabled(true);
        chart.setVisibleXRangeMinimum(5f);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setMarkerView(new MyMarkerView(getContext(), R.layout.marker_view));

        Log.v(TAG,"Setup Graph");
    }

    private void doScan() {
        Intent intent = new Intent(getActivity(), DeviceListActivity.class);
        getActivity().startActivityForResult(intent, Constants.REQUEST_CONNECT_DEVICE);
    }

    private void doNfc(){
        nfcAdapter = NfcAdapter.getDefaultAdapter(getContext());

        if(nfcAdapter.isEnabled()){

            Intent intent =new Intent(getActivity(), NfcActivity.class);

            startActivity(intent);
        }
        else{
            Snackbar.make(rootView,"NFC를 활성화 해주세요",Snackbar.LENGTH_LONG).show();
            // 4.2.2 (API 17) 부터 NFC 설정 환경이 변경됨.
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                Log.e("NFC", "nfc_setting");
                startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
            } else {
                Log.e("NFC", "wireless_setting");
                startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        long pastMilliseconds = System.currentTimeMillis() - (limitHours * HOURS);
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
        updateData();

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.v(TAG , "onLoaderReset");

    }
}
