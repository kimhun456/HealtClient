package org.swmem.healthclient.graph;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.swmem.healthclient.R;
import org.swmem.healthclient.Utility;
import org.swmem.healthclient.db.HealthContract;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by hyunjae on 16. 7. 29.
 */
public class GraphLoadTask extends AsyncTask<Void,Void,LineData>{

    private final String TAG = "GraphLoadTask";


    private final int SECONDS = 1000;
    private final int MINUTES = 60 * SECONDS;
    private final int HOURS = 60 * MINUTES;
    private final int DAYS = 24 * HOURS;

    private LineChart chart;
    private TextView lastValueText;


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

    private Context context;
    private long limitDays;
    private int lastDataIndex = 0;
    private long lastDate = 0;
    private double lastValue = 0;


    public GraphLoadTask(Context context, View rootView){
        this.context =  context;
        lastDataIndex = 0;
        lastDate = 0;
        lastValue = 0;
        limitDays = Long.parseLong(PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_limit_day_key),"1"));
        chart = (LineChart) rootView.findViewById(R.id.chart);
        lastValueText = (TextView) rootView.findViewById(R.id.lastValueText);
    }


    @Override
    protected void onPostExecute(LineData lineData) {
        super.onPostExecute(lineData);


        if(lastValue != 0){
            lastValueText.setText(""+lastValue);
        }

        if(lastDataIndex - 40 > 0){
            chart.moveViewToX(lastDataIndex-40);
        }else if(lastDataIndex - 20 > 0){
            chart.moveViewToX(lastDataIndex-20);
        }else if(lastDataIndex - 10 > 0){
            chart.moveViewToX(lastDataIndex-10);
        }else{
            chart.moveViewToX(lastDataIndex);
        }
        chart.setData(lineData);
        chart.invalidate();

    }

    @Override
    protected LineData doInBackground(Void... voids) {


        long currentMilliseconds = System.currentTimeMillis();
        long pastMilliseconds = currentMilliseconds - (limitDays * DAYS);


        // select * from glucose_table where time > date('now', '-3 days');
        // 모든 데이터를 불러오게 된다.


        String[] selectionArgs = {""};
        selectionArgs[0] =  Utility.formatDate(pastMilliseconds);
        String WHERE_DATE_BY_LIMIT_DAYS = HealthContract.GlucoseEntry.COLUMN_TIME + " > ?" ;


        Cursor cursor = context.getContentResolver().query(
                HealthContract.GlucoseEntry.CONTENT_URI,
                DETAIL_COLUMNS,
                WHERE_DATE_BY_LIMIT_DAYS,
                selectionArgs,
                null);


        if(cursor == null || cursor.getCount() == 0){

            Log.v(TAG,"Cursor has null or no data");
            return null;
        }

        int BLUETOOTH_COLOR = ContextCompat.getColor(context,R.color.deep_blue);
        int NFC_COLOR = ContextCompat.getColor(context,R.color.deep_orange);




        ArrayList<MyEntry> myEntries = new ArrayList<>();
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        ArrayList<String> xAxisValues = getXAxisValues(currentMilliseconds);

        while (cursor.moveToNext()) {

            long currentDate = Utility.cursorDateToLong(cursor.getString(COL_GLUCOSE_TIME));

            if(currentDate >= pastMilliseconds && currentDate <= currentMilliseconds){

                float rawValue = (float)cursor.getDouble(COL_GLUCOSE_GLUCOSE_VALUE);
                String type = cursor.getString(COL_GLUCOSE_TYPE);
                int index = getIndexOfEntries(currentDate,currentMilliseconds);

//                Log.v ("cursor" ,"date : " +  Utility.formatDate(currentDate));
//                Log.v ("cursor" ,"type : " +  type);
//                Log.v("cursor",  "RAW VALUE :  " +rawValue);
//                Log.v ("cursor" ,"index : " +  index );
//                Log.v ("cursor" ,"______________________");

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

                if(currentDate > lastDate){
                    lastDate = currentDate;
                    lastValue = cursor.getDouble(COL_GLUCOSE_GLUCOSE_VALUE);
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

        // 데이터 세트 설정
        LineDataSet lineDataSet = new LineDataSet(entries, "RAW VALUE");
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setValueTextSize(0f);
        lineDataSet.setCircleColors(colors);
        lineDataSet.setColors(colors);
        lineDataSet.setHighLightColor(ContextCompat.getColor(context,R.color.black));
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet);
        LineData data = new LineData(xAxisValues, dataSets);

        //cursor 닫기
        cursor.close();

        return data;

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


}
