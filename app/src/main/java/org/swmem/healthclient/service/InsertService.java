package org.swmem.healthclient.service;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import org.swmem.healthclient.GlucoseData;
import org.swmem.healthclient.SessionManager;
import org.swmem.healthclient.Utility;
import org.swmem.healthclient.db.HealthContract;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 */
public class InsertService extends IntentService {

    private final String TAG = "InsertService";
    final int SECONDS = 1000;
    final int MINUTES = 60 * SECONDS;
    final int HOURS = 60 * MINUTES;
    final int DAYS = 24 * HOURS;



    public InsertService() {
        super("InsertService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(this, "Insert Service Start" , Toast.LENGTH_SHORT).show();

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     *
     *  multi Thread를 자동으로 생성하기 때문에 여기에 그냥 생성하면 된다.
     *
     * @param intent startservice()에서 전달하는 intent를 이용하여 처리시킨다.
     *
     *  insert into wait-queue
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        // 현재시간
        long currentTimeMillis =  System.currentTimeMillis();

        if (intent != null) {

            // TODO: intent에서 byte[]를 받아서 insertMap을 만들기

            SessionManager sessionManager = new SessionManager(getApplicationContext());
            sessionManager.setExist(true);
            sessionManager.setDeviceConnectTime(System.currentTimeMillis());
            sessionManager.setDeviceID("deviceID");


            HashMap<String, GlucoseData> insertMap = makeRandomInsertMap();

            HashMap<String, GlucoseData> dbMap = getDBmap(currentTimeMillis);

            dbMap = convertDBMap(insertMap, dbMap);

            takeAlgorithm(dbMap);

            insertDataBase(dbMap);

        }
    }


    @Override
    public void onDestroy() {

        Toast.makeText(this, "Insert Service END" , Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }


    private HashMap<String , GlucoseData> makeRandomInsertMap(){

        HashMap<String,GlucoseData> map = new HashMap<>();

        long currentMilli = Utility.getCurrentDate();
        double prevValue = 92;
        for(int i=0;i<1000;i++){
            double rand = Math.random();
            long time =  currentMilli - 1000*60* i;
            String convertedTime = Utility.formatDate(time);
            Log.v("time : " , convertedTime);


            GlucoseData data = new GlucoseData();

            if(rand < 0.5){
                data.setType(HealthContract.GlucoseEntry.NFC);
            }else{
                data.setType(HealthContract.GlucoseEntry.BLEUTOOTH);
            }

            data.setDate(convertedTime);
            data.setRawData(prevValue);
            data.setTemperature(prevValue);
            data.setDeviceID("device1");
            data.setModifed(false);
            data.setConvert(false);
            data.setInDataBase(false);

            if(Math.random() > 0.5){

                prevValue += rand;

            }else{

                prevValue -= rand;

            }

            map.put(data.getDate(),data);

        }

        return map;

    }



    private HashMap< String ,GlucoseData> getDBmap(long currentTimeMillis){

        HashMap< String, GlucoseData> glucoseDataHashMap = new HashMap<>();

        final String[] DETAIL_COLUMNS = {
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
        final int COL_GLUCOSE_ID = 0;
        final int COL_GLUCOSE_GLUCOSE_VALUE = 1;
        final int COL_GLUCOSE_TEMPEATURE_VALUE = 2;
        final int COL_GLUCOSE_RAW_VALUE = 3;
        final int COL_GLUCOSE_DEVICE_ID = 4;
        final int COL_GLUCOSE_TIME = 5;
        final int COL_GLUCOSE_TYPE = 6;

        long pastMilliseconds = currentTimeMillis - (1 * DAYS);
        String[] selectionArgs = {""};
        selectionArgs[0] =  Utility.formatDate(pastMilliseconds);
        String WHERE_DATE_BY_LIMIT_DAYS = HealthContract.GlucoseEntry.COLUMN_TIME + " > ?" ;

        Cursor cursor = getApplicationContext().getContentResolver().query(
                HealthContract.GlucoseEntry.CONTENT_URI,
                DETAIL_COLUMNS,
                WHERE_DATE_BY_LIMIT_DAYS,
                selectionArgs,
                null
        );


        if(cursor == null || cursor.getCount() == 0){
            Log.v(TAG,"Cursor has null or no data");
            return glucoseDataHashMap;
        }

        while(cursor.moveToNext()) {

            double rawData = cursor.getDouble(COL_GLUCOSE_RAW_VALUE);
            double convertedData = cursor.getDouble(COL_GLUCOSE_GLUCOSE_VALUE);
            double temperature = cursor.getDouble(COL_GLUCOSE_TEMPEATURE_VALUE);
            String sesorID = cursor.getString(COL_GLUCOSE_DEVICE_ID);
            String date = cursor.getString(COL_GLUCOSE_TIME);
            String type = cursor.getString(COL_GLUCOSE_TYPE);
            boolean isConverted;

            if(convertedData == 0.0){
                isConverted = false;
            }else{
                isConverted = true;
            }
            GlucoseData data = new GlucoseData(rawData,convertedData,temperature,sesorID, date ,type, isConverted, true , false );
            glucoseDataHashMap.put(date,data);

        }

        cursor.close();

        return glucoseDataHashMap;
    }


    private HashMap< String ,GlucoseData> convertDBMap(HashMap< String, GlucoseData> insertMap, HashMap< String, GlucoseData> dbMap){

        for(String key : insertMap.keySet()){

            //데이터 베이스에 없으면 넣는다.
            if(dbMap.get(key) == null){
                dbMap.put(key, insertMap.get(key));
            }

            // 디비에 값이 있으면
            else{

                GlucoseData dataBaseData = dbMap.get(key);
                // 디비가 convert 되어있지 않으면 덮어쓴다.
                if(!dataBaseData.isConverted()){
                    insertMap.get(key).setInDataBase(false);
                    dbMap.put(key,insertMap.get(key));
                }
            }
        }
        return dbMap;
    }


    private HashMap< String ,GlucoseData>  takeAlgorithm(HashMap< String, GlucoseData> insertMap){


        final int rateINC_MORE = 2;
        final int rateINC_LESS = 1;
        final int rateDEC_INC = 1;
        final int rateINC_DEC = -1;
        final int rateDEC_LESS = -1;
        final int rateDEC_MORE = -2;

        for(String key : insertMap.keySet()){

            GlucoseData glucoseData = insertMap.get(key);

            if(glucoseData.isConverted()){

//                Log.v(TAG, "date : " + glucoseData.getDate() + " is converted");
                continue;
            }

            String currentDate  = glucoseData.getDate();

            String threeDayAgoKey = getPrevKey(currentDate,3);
            String sixDayAgoKey = getPrevKey(currentDate,6);

            if(insertMap.get(threeDayAgoKey) == null ||  insertMap.get(sixDayAgoKey) == null){
                glucoseData.setConvert(false);
//                Log.v(TAG, " date : " + glucoseData.getDate() + "  3 or 6 day ago is not possible");
                continue;
            }

            double currentData = glucoseData.getRawData();
            double threeMinPastData = insertMap.get(threeDayAgoKey).getRawData();
            double sixMinPastData = insertMap.get(sixDayAgoKey).getRawData();
            double Diff_F = currentData - threeMinPastData;
            double Diff_S = threeMinPastData - sixMinPastData;
            double Diff_Diff = Diff_F - Diff_S;
            double Compensated_Glimp_change;

            if( (Math.abs(Diff_F) < 1.5) && (Math.abs(Diff_S) < 1.5) )
                if(currentData < 130)
                    Compensated_Glimp_change = Diff_F-6;
                else
                    Compensated_Glimp_change = Diff_F-2;
            else{
                if(Diff_F >= 9) {
                    if (Diff_S >= 9)
                        Compensated_Glimp_change = Diff_F + rateINC_MORE * Diff_Diff + 30;
                    else
                        Compensated_Glimp_change = Diff_F + rateINC_MORE * Diff_Diff + 30;
                }
                else if((Diff_F >= 0)&&(Diff_F < 9)) {
                    if ((Diff_F * 10) * (Diff_S * 10) >= 0) {
                        if (Diff_Diff >= 0)
                            Compensated_Glimp_change = Diff_F + rateINC_MORE * Diff_Diff + 10;
                        else
                            Compensated_Glimp_change = Diff_F - rateINC_LESS * Diff_Diff + 10;
                    } else {
                        Compensated_Glimp_change = Diff_F + rateDEC_INC * Math.abs(Diff_F + Diff_S);
                    }
                }
                else if(Diff_F <= -9)
                    Compensated_Glimp_change = Diff_F + rateDEC_MORE*Diff_Diff - 10;
                else{
                    if((Diff_F*10)*(Diff_S*10) >= 0) {
                        if (Diff_Diff >= 0)
                            Compensated_Glimp_change = Diff_F + rateDEC_LESS * Diff_Diff - 5;
                        else
                            Compensated_Glimp_change = Diff_F + rateDEC_MORE * Diff_Diff - 10;
                    }
                    else {
                        Compensated_Glimp_change = Diff_F + rateINC_DEC * Math.abs(Diff_F + Diff_S);
                    }
                }
            }

            if(currentData > 185)
                Compensated_Glimp_change = Compensated_Glimp_change+10;

            glucoseData.setConvertedData(currentData + Compensated_Glimp_change);
            glucoseData.setConvert(true);

//            Log.v(TAG , " converted Data is " +(currentData + Compensated_Glimp_change));

            if(glucoseData.isInDataBase()){
                glucoseData.setModifed(true);
            }

        }

        return insertMap;

    }


    private boolean insertDataBase(HashMap< String, GlucoseData> insertMap){

        ArrayList<ContentValues> addList = new ArrayList<>();

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();


        for(String key : insertMap.keySet()){

            GlucoseData data = insertMap.get(key);

            ContentValues contentValues = new ContentValues();
            if(data.isInDataBase()){
                if(data.isModifed()){

                    operations.add(ContentProviderOperation
                            .newUpdate(HealthContract.GlucoseEntry.CONTENT_URI)
                            .withSelection(HealthContract.GlucoseEntry.COLUMN_TIME+" = ?",new String[]{data.getDate()})
                            .withValue(HealthContract.GlucoseEntry.COLUMN_GLUCOSE_VALUE,
                                    data.getConvertedData())
                            .build());
                }
            }else{

//                Log.v(TAG , " date : " + data.getDate() + " is in the db " + data.isInDataBase() );

                if(data.getType().equals(HealthContract.GlucoseEntry.BLEUTOOTH)){
                    contentValues.put(HealthContract.GlucoseEntry.COLUMN_TYPE,
                            HealthContract.GlucoseEntry.BLEUTOOTH);
                }else{
                    contentValues.put(HealthContract.GlucoseEntry.COLUMN_TYPE,
                            HealthContract.GlucoseEntry.NFC);
                }
                contentValues.put(HealthContract.GlucoseEntry.COLUMN_TIME, data.getDate());
                contentValues.put(HealthContract.GlucoseEntry.COLUMN_RAW_VALUE, data.getRawData());

                if(data.isConverted()){
                    contentValues.put(HealthContract.GlucoseEntry.COLUMN_GLUCOSE_VALUE,
                            data.getConvertedData());
                }else{
                    contentValues.putNull(HealthContract.GlucoseEntry.COLUMN_GLUCOSE_VALUE);
                }

                contentValues.put(HealthContract.GlucoseEntry.COLUMN_TEMPERATURE_VALUE,
                        data.getTemperature());
                contentValues.put(HealthContract.GlucoseEntry.COLUMN_DEVICE_ID,
                        data.getDeviceID());
                addList.add(contentValues);
            }
        }


        ContentValues contentValues[] = new ContentValues[addList.size()];
        for(int i=0;i<addList.size();i++){
            contentValues[i] = addList.get(i);

        }


        // add한다.
        getApplicationContext().getContentResolver().bulkInsert(
                HealthContract.GlucoseEntry.CONTENT_URI,
                contentValues
        );


        try {
            getApplicationContext().getContentResolver().applyBatch(
                    HealthContract.CONTENT_AUTHORITY,operations);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }


        return true;
    }


    private String getPrevKey(String currentDate, int minutes){

        long currentMiili = Utility.cursorDateToLong(currentDate);

        currentMiili -= minutes * MINUTES;

        String prevDate = Utility.formatDate(currentMiili);

        return prevDate;

    }


}
