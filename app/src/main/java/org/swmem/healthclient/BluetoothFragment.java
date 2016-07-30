package org.swmem.healthclient;

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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;

import org.swmem.healthclient.db.HealthContract;
import org.swmem.healthclient.graph.GraphLoadTask;
import org.swmem.healthclient.utils.Constants;
import org.swmem.healthclient.graph.MyMarkerView;


public class BluetoothFragment extends Fragment {


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

        View rootView = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        LineChart chart = (LineChart) rootView.findViewById(R.id.chart);
        setUpChart(chart);
        updateData(rootView);

        return rootView;
    }



    private void updateData(View rootView){

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
}
