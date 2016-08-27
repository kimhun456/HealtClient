package org.swmem.healthclient.graph;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.swmem.healthclient.R;
import org.swmem.healthclient.db.HealthContract;
import org.swmem.healthclient.utils.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by hyunjae on 16. 7. 29.
 */
public class GraphLoadTask extends AsyncTask<Void,Void,LineData>{

    private final String TAG = "GraphLoadTask";

    private final int DOUBLE_UP_ARROW = 0;
    private final int UP_ARROW = 1;
    private final int CURRENT_ARROW = 2;
    private final int DOWN_ARROW = 3;
    private final int DOUBLE_DOWN_ARROW = 4;

    private final int DIFF_THREE = 3;
    private final int DIFF_ONE = 1;

    private final int SECONDS = 1000;
    private final int MINUTES = 60 * SECONDS;
    private final int HOURS = 60 * MINUTES;
    private final int DAYS = 24 * HOURS;

    private LineChart chart;
    private TextView lastValueText;
    private ImageView currentArrowImage;
    private ImageView currentArrowImage2;


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
    private long limitHours;
    private long dataInterval;
    private String dataFormat;
    private int lastDataIndex = 0;
    private long lastDate = 0;
    private double lastValue = 0;
    private int arrowState = 2;
    private float highGlucose;
    private float lowGlucose;
    private  AnimationDrawable animation1;
    private  AnimationDrawable animation2;

    public GraphLoadTask(Context context, View rootView){

        highGlucose = Float.parseFloat(PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_Hyperglycemia_key),"200"));
        lowGlucose = Float.parseFloat(PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_Hypoglycemia_key),"80"));

        this.context =  context;
        lastDataIndex = 0;
        lastDate = 0;
        lastValue = 0;
        dataInterval = Long.parseLong(PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_data_interval_key),context.getString(R.string.pref_data_interval_one)));

        dataFormat = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_data_format_key),context.getString(R.string.pref_data_format_mgdl));

        limitHours = Long.parseLong(PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_limit_hours_key),context.getString(R.string.pref_limit_hours_24)));

        chart = (LineChart) rootView.findViewById(R.id.chart);
        lastValueText = (TextView) rootView.findViewById(R.id.lastValueText);
        currentArrowImage = (ImageView) rootView.findViewById(R.id.current_data_image);
        currentArrowImage2 = (ImageView) rootView.findViewById(R.id.current_data_image2);
    }

    @Override
    protected void onPostExecute(LineData lineData) {
        super.onPostExecute(lineData);

        if(lastValue != 0){



            if(dataFormat.equals(context.getString(R.string.pref_data_format_mmol))){

                lastValueText.setText(String.format("%.1f",lastValue));
            }else{

                lastValueText.setText(String.format("%.0f",lastValue));
            }

            if(lastValue > highGlucose) {
                lastValueText.setTextColor(ContextCompat.getColor(context, R.color.deep_red));
            }
            else if(lastValue < lowGlucose){
                lastValueText.setTextColor(ContextCompat.getColor(context, R.color.deep_blue));
            }
            else{
                lastValueText.setTextColor(ContextCompat.getColor(context, R.color.black));
            }

            switch (arrowState){

                case DOUBLE_UP_ARROW :

                    currentArrowImage.setImageResource(R.drawable.transparent);
                    currentArrowImage.setBackgroundResource(R.drawable.up_arrow_animation);
                    currentArrowImage2.setVisibility(View.VISIBLE);
                    currentArrowImage2.setBackgroundResource(R.drawable.up_arrow_animation);

                    animation1 = (AnimationDrawable) currentArrowImage
                            .getBackground();
                    animation1.start();
                    animation2 = (AnimationDrawable) currentArrowImage2
                            .getBackground();
                    animation2.start();



                    break;
                case UP_ARROW :

                    if(animation1 != null){
                        animation1.stop();
                    }
                    if(animation2 != null){
                        animation2.stop();
                    }
                    currentArrowImage.setBackgroundResource(R.drawable.transparent);
                    currentArrowImage.setImageResource(R.drawable.up_arrow);
                    currentArrowImage2.setVisibility(View.GONE);
                    break;
                case CURRENT_ARROW :

                    if(animation1 != null){
                        animation1.stop();
                    }
                    if(animation2 != null){
                        animation2.stop();
                    }

                    currentArrowImage.setBackgroundResource(R.drawable.transparent);
                    currentArrowImage.setImageResource(R.drawable.current_arrow_1);
                    currentArrowImage2.setVisibility(View.GONE);
                    break;


                case DOWN_ARROW :

                    if(animation1 != null){
                        animation1.stop();
                    }
                    if(animation2 != null){
                        animation2.stop();
                    }
                    currentArrowImage.setBackgroundResource(R.drawable.transparent);
                    currentArrowImage.setImageResource(R.drawable.down_arrow);
                    currentArrowImage2.setVisibility(View.GONE);

                    break;
                case DOUBLE_DOWN_ARROW :


                    currentArrowImage.setImageResource(R.drawable.transparent);
                    currentArrowImage.setBackgroundResource(R.drawable.down_arrow_animation);
                    currentArrowImage2.setVisibility(View.VISIBLE);
                    currentArrowImage2.setBackgroundResource(R.drawable.down_arrow_animation);

                    animation1 = (AnimationDrawable) currentArrowImage
                            .getBackground();
                    animation1.start();
                    animation2 = (AnimationDrawable) currentArrowImage2
                            .getBackground();
                    animation2.start();

                    break;

                default:

                    if(animation1 != null){
                        animation1.stop();
                    }
                    if(animation2 != null){
                        animation2.stop();
                    }

                    currentArrowImage.setImageResource(R.drawable.current_arrow_1);
                    currentArrowImage2.setVisibility(View.GONE);
                    break;

            }

        }

        chart.setData(lineData);


        // zooming 과 과련된 부분들

        chart.getViewPortHandler().fitScreen();

        chart.setVisibleXRangeMaximum(10);

//
//        if(limitHours == Long.parseLong(getString(R.string.pref_limit_hours_6))){
//            Log.v("zoom", "6");
//            chart.zoom(1.3f,1f,1f,1f);
//        }else if(limitHours == Long.parseLong(getString(R.string.pref_limit_hours_12))){
//            Log.v("zoom", "12");
//            chart.zoom(3.3f,1f,1f,1f);
//        }else if(limitHours == Long.parseLong(getString(R.string.pref_limit_hours_24))){
//            Log.v("zoom", "24");
//            chart.zoom(7f,1f,1f,1f);
//        }else if(limitHours == Long.parseLong(getString(R.string.pref_limit_hours_72))){
//            Log.v("zoom", "72");
//            chart.zoom(19f,1f,1f,1f);
//        }


        if(lastDataIndex - 10 > 0){
            chart.moveViewToX(lastDataIndex-10);
        }else{
            chart.moveViewToX(lastDataIndex);
        }

        chart.invalidate();

        chart.setVisibleXRangeMaximum(1000000000);

        Log.v(TAG,"Load Graph Data");

    }

    @Override
    protected LineData doInBackground(Void... voids) {


        long currentMilliseconds = System.currentTimeMillis();
        long pastMilliseconds = currentMilliseconds - (limitHours * HOURS);
        String[] selectionArgs = {""};
        selectionArgs[0] =  Utility.formatDate(pastMilliseconds);
        String WHERE_DATE_BY_LIMIT_DAYS = HealthContract.GlucoseEntry.COLUMN_TIME + " > ?" ;

        Cursor cursor = context.getContentResolver().query(
                HealthContract.GlucoseEntry.CONTENT_URI,
                DETAIL_COLUMNS,
                WHERE_DATE_BY_LIMIT_DAYS,
                selectionArgs,
                null
        );

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
        HashMap<String, Boolean> dateMap = getCorrectDate(currentMilliseconds);

        while (cursor.moveToNext()) {

            String currentDateString = cursor.getString(COL_GLUCOSE_TIME);
            long currentDate = Utility.cursorDateToLong(currentDateString);

            if(currentDate >= pastMilliseconds && currentDate <= currentMilliseconds){

                float convertedData = (float)cursor.getDouble(COL_GLUCOSE_GLUCOSE_VALUE);

                if(convertedData ==0.0){
                    convertedData = (float)cursor.getDouble(COL_GLUCOSE_RAW_VALUE);
                    Log.v ("cursor" ,"rawdata is insert : " + (float)cursor.getDouble(COL_GLUCOSE_RAW_VALUE));
                }


                if(dataFormat.equals(context.getString(R.string.pref_data_format_mmol))){
                    convertedData = Utility.mgdlTommol(convertedData);
                }

                String type = cursor.getString(COL_GLUCOSE_TYPE);
                int index = getIndexOfEntries(currentDate,currentMilliseconds);

                Log.v ("cursor" ,"date : " +  Utility.formatDate(currentDate));
                Log.v ("cursor" ,"type : " +  type);
                Log.v("cursor",  "Converted VALUE :  " +convertedData);
                Log.v("cursor",  "RAW VALUE :  " +(float)cursor.getDouble(COL_GLUCOSE_RAW_VALUE));
                Log.v ("cursor" ,"index : " +  index );
                Log.v ("cursor" ,"______________________");

                if(index < 0){
                    continue;
                }
                if(index > lastDataIndex){
                    lastDataIndex = index;
                }

                if(type.equals(HealthContract.GlucoseEntry.BLEUTOOTH)){
                    myEntries.add(new MyEntry(index,convertedData,BLUETOOTH_COLOR,Utility.formatDate(currentDate)));
                }else{
                    myEntries.add(new MyEntry(index,convertedData,NFC_COLOR,Utility.formatDate(currentDate)));
                }

            }
        }


        // sort the entries ascending order by index.
        Collections.sort(myEntries, new Comparator<MyEntry>() {
            @Override
            public int compare(MyEntry t1, MyEntry t2) {
                if(t1.getLongDate() > t2.getLongDate())
                    return 0;
                else
                    return -1;
            }
        });


        // 화살표 설정
        if( myEntries.size() >= 2){

            MyEntry lastEntry = myEntries.get(myEntries.size()-1);
            MyEntry prevEntry = myEntries.get(myEntries.size()-2);

            lastValue = lastEntry.getValue();

            float diff = lastEntry.getValue() - prevEntry.getValue();


            if(dataFormat.equals(context.getString(R.string.pref_data_format_mmol))){
                diff = Utility.mmolTomgdL(diff);
            }

            Log.v(TAG , "lastEntry value : " +  lastEntry.getValue());
            Log.v(TAG , "prevEntry value : " +  prevEntry.getValue());
            Log.v(TAG , "diff value : " +  diff);

            if(diff>= DIFF_THREE){
                arrowState = DOUBLE_UP_ARROW;
            }else if(diff >=DIFF_ONE){
                arrowState = UP_ARROW;
            }else if(diff <=-DIFF_ONE && diff > -DIFF_THREE){
                arrowState = DOWN_ARROW;
            }else if(diff <=-DIFF_THREE){
                arrowState = DOUBLE_DOWN_ARROW;
            }else{
                arrowState = CURRENT_ARROW;
            }
        }
        else{
            Log.v(TAG , " Entries is less than 2 ");
        }


        for(MyEntry myEntry : myEntries){

            if(dateMap.get(myEntry.getDate()) == null){
                continue;
            }

            entries.add(new Entry(myEntry.getValue(), myEntry.getIndex()));
            colors.add(myEntry.getColor());

        }



        // 데이터 세트 설정
        LineDataSet lineDataSet = new LineDataSet(entries, "RAW VALUE");
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setValueTextSize(0f);
        lineDataSet.setCircleColors(colors);
        lineDataSet.setColors(colors);
        lineDataSet.setHighlightLineWidth(0f);
        lineDataSet.setHighLightColor( ContextCompat.getColor(context,R.color.transparent));
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet);
        LineData data = new LineData(xAxisValues, dataSets);

        cursor.close();

        return data;

    }



    private ArrayList<String> getXAxisValues(long currentTimeMillis){

        ArrayList<String> xValues = new ArrayList<String>();
        currentTimeMillis -= limitHours * HOURS;

        for(long i = 0; i<= limitHours * HOURS; i+=(MINUTES * dataInterval) ){

            xValues.add(Utility.getGraphDateFormat(currentTimeMillis + i));

//            Log.v(TAG, Utility.getGraphDateFormat(currentTimeMillis + i));
        }

        return xValues;
    }

    private HashMap<String, Boolean> getCorrectDate(long currentTimeMillis){

        HashMap<String, Boolean> map = new HashMap<>();

        currentTimeMillis -= limitHours * HOURS;

        for(long i = 0; i<= limitHours * HOURS; i+=(MINUTES * dataInterval) ){

            Date date = new Date(currentTimeMillis + i);
            date.setSeconds(0);
            map.put(Utility.formatDate(date.getTime()),true);
//            Log.v(TAG, Utility.formatDate(date.getTime()));
        }

        return map;
    }

    private int getIndexOfEntries(long findMiiliSeconds , long currentTimeMillis){

        long pastMilliseconds = currentTimeMillis - limitHours * HOURS;

        long diff = findMiiliSeconds - pastMilliseconds;


        int index  = (int) (diff / (MINUTES * dataInterval)) + 1;

        return index;

    }


}
