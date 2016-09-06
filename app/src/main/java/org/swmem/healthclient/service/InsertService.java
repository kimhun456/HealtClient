package org.swmem.healthclient.service;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import org.swmem.healthclient.db.GlucoseData;
import org.swmem.healthclient.utils.MyNotificationManager;
import org.swmem.healthclient.R;
import org.swmem.healthclient.utils.SessionManager;
import org.swmem.healthclient.utils.Utility;
import org.swmem.healthclient.db.HealthContract;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * Created By  HyunJae
 *
 *
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 *
 *  BlueTooth or NFC 에서 새로운 데이터가 들어오게 된다면
 *  Insert Service 에서 데이터를 판별하고 데이터가 적절하다면
 *  알고리즘을 거쳐서 데이터베이스에 들어가게 된다.
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

        Log.v(TAG,"Insert Start !!! ");
//        Toast.makeText(this, "Inserting..." , Toast.LENGTH_SHORT).show();
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

            SessionManager sessionManager = new SessionManager(getApplicationContext());
            sessionManager.setExist(true);
            sessionManager.setDeviceConnectTime(System.currentTimeMillis());
            sessionManager.setDeviceID("deviceID");
            byte[] test = null;
            int len = 0, MyType = -1;

            // intent로 받은 byte배열과 숫자, 타입을 받음
            test = intent.getByteArrayExtra("RealData");
            len = intent.getIntExtra("RealCnt",0);
            MyType = intent.getIntExtra("MyType",-1);

            if (test != null) {
                for(int i=0; i<len; i++)
                    Log.d(TAG, ""+ (0xff&test[i]));
            }

            HashMap<String, GlucoseData> insertMap;
            if(MyType == 2) { // 랜덤 Data일 때
                insertMap = makeRandomInsertMap();
            }else { // 그 외의 정상 테이터를 받았을 때 (MyType 0 : Bluetooth, MyType 1 : NFC)
                insertMap = byteDecoding(test, len, MyType,sessionManager);
            }
            HashMap<String, GlucoseData> dbMap = getDBmap(currentTimeMillis);

            dbMap = convertDBMap(insertMap, dbMap);

            takeAlgorithm(dbMap);

            insertDataBase(dbMap);

            doNotification(dbMap);

        }
    }

    public boolean doNotification(HashMap<String , GlucoseData> insertMap){

        long currentmiili = Utility.getCurrentDate();
        long min = 5 *DAYS;
        String lastKey = null;
        for(String key : insertMap.keySet()){

            long date = Utility.cursorDateToLong(key);


            long diff = (currentmiili - date);

            if(diff < min){
                min = diff;
                lastKey = key;
            }
        }

        if(lastKey == null){

            Log.v(TAG, "do not found last date");
            return false;
        }

        boolean realTimeNotiEnable = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getBoolean(getString(R.string.pref_enable_real_time_notifications_key),false);

        boolean hyperNotiEnable = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getBoolean(getString(R.string.pref_enable_Hyperglycemia_notifications_key),false);

        boolean hypoNotiEnable = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getBoolean(getString(R.string.pref_enable_Hypoglycemia_notifications_key),false);

        float highGlucose = Float.parseFloat(PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(getString(R.string.pref_Hyperglycemia_key),"200"));
        float lowGlucose = Float.parseFloat(PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(getString(R.string.pref_Hypoglycemia_key),"80"));

        GlucoseData glucoseData = insertMap.get(lastKey);


        Log.v(TAG, lastKey + "");

        if(realTimeNotiEnable){
            double data;
            if(glucoseData.isConverted()){
                data = glucoseData.getConvertedData();

            }else{
                data = glucoseData.getRawData();
            }
            new MyNotificationManager(getApplicationContext()).makeNotification("현재 혈당량",  String.format("%.2f",data) + " " + getString(R.string.mgdl) );

        }


        if(hyperNotiEnable){

            double data;
            if(glucoseData.isConverted()){
                data = glucoseData.getConvertedData();

            }else{
                data = glucoseData.getRawData();
            }

            if(data > highGlucose){
                new MyNotificationManager(getApplicationContext()).makeNotification("고혈당 위험! ", "현재 혈당 :  " +  String.format("%.2f",data)+ " " + getString(R.string.mgdl)  );
            }
        }


        if(hypoNotiEnable){

            double data;
            if(glucoseData.isConverted()){
                data = glucoseData.getConvertedData();

            }else{
                data = glucoseData.getRawData();
            }

            if(data < lowGlucose){
                new MyNotificationManager(getApplicationContext()).makeNotification("저혈당 위험! ", "현재 혈당 : " +  String.format("%.2f",data)+ " " + getString(R.string.mgdl) );
            }
        }

        return true;
    }


    @Override
    public void onDestroy() {

        Log.v(TAG,"Inserting End!!!");
//        Toast.makeText(this, "Inserting... END!" , Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }


    public HashMap<String , GlucoseData> byteDecoding(byte[] buf, int len, int MyType, SessionManager sessionManager){


        HashMap<String,GlucoseData> map = new HashMap<>();

        String type = "";
        String deviceID ="";
        int numbering;
        int battery;
        double rawData=0;
        double temperature=0;
        int count = 0;

        //NFC 데이터 처리.
        if(MyType==1){
            Log.d(TAG, "MyType == 1");
            //type = HealthContract.GlucoseEntry.NFC;//byteTostr(buf[0], buf[1]);
            deviceID = byteTostr(buf[2],buf[3]);
            sessionManager.setDeviceID(deviceID);
            battery = byteToint(buf[4],buf[5]);
            numbering = (len - 6)/5;

            for(int i=0; i< numbering; i++){

                rawData = byteToint(buf[6 + 5*i], buf[6 + 5*i + 1]);
                temperature = byteToint(buf[ 6 + 5*i + 2], buf[6 + 5*i + 3], buf[6 + 5*i + 4]);

                Log.d(TAG, "rawData :" + rawData);
                Log.d(TAG, "temperature : "+temperature);

                GlucoseData data = new GlucoseData();
                data.setRawData(rawData);
                data.setTemperature(temperature);
                String date = Utility.formatDate(Utility.getCurrentDate() - (count * MINUTES));
                data.setType(HealthContract.GlucoseEntry.NFC);
                data.setDate(date);
                data.setDeviceID(deviceID);
                data.setModifed(false);
                data.setConvert(false);
                data.setInDataBase(false);
                map.put(date,data);
                count++;
            }
        }
        else{
            for(int i=0; i<len; i++){

                //type
                if(i==0){
                    type = String.valueOf(0xff&buf[0]);
                    //type = String.valueOf(buf[0]);
                    type = byteTostr(buf[0]);
                    System.out.println("type : " + type);
                }
                //deviceID
                else if(i==1){
                    deviceID = String.valueOf((0xff&buf[2]<<8) | (0xff&buf[1]));
                    //deviceID = String.valueOf(buf[1]<<8 | buf[2]);
                    deviceID = byteTostr(buf[2],buf[1]);
                    System.out.println("deviceID : "+ deviceID);
                }
                //nubmering
                else if(i==3){
                    numbering = (0xff&buf[5]<<16)  | (0xff&buf[4]<<8) | (0xff&buf[3]);
                    //numbering = buf[3]<<16  | buf[4]<<8 | buf[5];
                    numbering = byteToint(buf[5], buf[4], buf[3]);
                    System.out.println("numbering : "+ numbering);
                }
                //battery
                else if(i==6){
                    battery = (0xff&buf[7]<<8) | (0xff&buf[6]);
                    //battery = buf[6]<<8 | buf[7];
                    battery = byteToint(buf[7],buf[6]);
                    System.out.println("battery : "+ battery);
                }
                //수정 앞으로 해야될 부분
                //gluecoseData & temperature;
                else{
                    // 첫번째 정수
                    if((i-8)%5 == 0){
                        rawData = byteTodouble(buf[i]);
                    }
                    // 두번째 정수
                    if((i-9)%5 == 0 && buf[i] != 0){
                        rawData += 0xff&buf[i] << 8;
                    }
                    if((i-10)%5 == 0 && buf[i] != 0) {
                        rawData += 0xff&buf[i] << 16;
                    }
                    else if((i-11)%5 == 0){
                        temperature = 0xff&buf[i];
                    }
                    else if(i>=12 && (i-12)%5 == 0){
                        temperature += 0xff&buf[i]<<8;

                        //insert
                        System.out.print("rawData : "+ rawData);
                        System.out.println("  temperature : "+ temperature);


                        GlucoseData data = new GlucoseData();
                        if(type.equals(HealthContract.GlucoseEntry.NFC)){
                            data.setType(HealthContract.GlucoseEntry.NFC);
                        }else{
                            data.setType(HealthContract.GlucoseEntry.BLUETOOTH);
                        }

                        String date = Utility.formatDate(Utility.getCurrentDate() - (count * MINUTES));
                        if(MyType == 0) data.setType(HealthContract.GlucoseEntry.BLUETOOTH);
                        else data.setType(HealthContract.GlucoseEntry.NFC);
                        data.setDate(date);
                        data.setRawData(rawData);
                        data.setTemperature(temperature);
                        data.setDeviceID(deviceID);
                        data.setModifed(false);
                        data.setConvert(false);
                        data.setInDataBase(false);
                        map.put(date,data);
                        count++;

                    }
                }

            }
        }

        
        //// TODO: 2016-09-06 배터리로 노티피케이션 하는 부분.



        return map;
    }


    public int byteToint(byte first_buf, byte second_buf){
        return ((first_buf & 0xff)<<8 | (second_buf & 0xff));
    }
    public int byteToint(byte first_buf, byte second_buf, byte third_buf){
        return ((first_buf & 0xff)<<16 | (second_buf & 0xff)<<8 | (third_buf & 0xff));
    }
    public String byteTostr(byte buf){
        return String.valueOf((buf&0xff));
    }
    public String byteTostr(byte first_buf, byte second_buf){
        return String.valueOf(((first_buf)&0xff)<<8 | second_buf&0xff);
    }
    public double byteTodouble(byte buf){
        return buf&0xff;
    }


    private HashMap<String , GlucoseData> makeRandomInsertMap(){

        HashMap<String,GlucoseData> map = new HashMap<>();

        long currentMilli = Utility.getCurrentDate();
        double prevValue = 92;
        for(int i=0;i<1000;i++){
            double rand = Math.random();
            long time =  currentMilli - 1000*60* i;
            String convertedTime = Utility.formatDate(time);
//            Log.v("time : " , convertedTime);

            GlucoseData data = new GlucoseData();

            if(rand < 0.5){
                data.setType(HealthContract.GlucoseEntry.NFC);
            }else{
                data.setType(HealthContract.GlucoseEntry.BLUETOOTH);
            }

            data.setDate(convertedTime);
            data.setRawData(prevValue);
            data.setTemperature(prevValue);
            data.setDeviceID("device1");
            data.setModifed(false);
            data.setConvert(false);
            data.setInDataBase(false);

            if(Math.random() > 0.5){

                prevValue += rand*3;

            }else{

                prevValue -= rand*3;

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

//            Log.v(TAG , " DB 안에 있던 데이터들 : " + date);

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

//                    Log.v(TAG , " DB에 있지만 수정된 데이터들 : " + key);
                    operations.add(ContentProviderOperation
                            .newUpdate(HealthContract.GlucoseEntry.CONTENT_URI)
                            .withSelection(HealthContract.GlucoseEntry.COLUMN_TIME+" = ?",new String[]{data.getDate()})
                            .withValue(HealthContract.GlucoseEntry.COLUMN_GLUCOSE_VALUE,
                                    data.getConvertedData())
                            .build());
                }
            }else{


//                Log.v(TAG , " insert db 에서 DB XXXXXX 데이터들 : " + key);

                if(data.getType().equals(HealthContract.GlucoseEntry.BLUETOOTH)){
                    contentValues.put(HealthContract.GlucoseEntry.COLUMN_TYPE,
                            HealthContract.GlucoseEntry.BLUETOOTH);
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
