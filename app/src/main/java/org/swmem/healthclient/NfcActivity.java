package org.swmem.healthclient;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import org.swmem.healthclient.db.GlucoseData;
import org.swmem.healthclient.db.HealthContract;
import org.swmem.healthclient.service.InsertService;
import org.swmem.healthclient.utils.Utility;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Created by Woo on 2016-08-02.
 */

public class NfcActivity extends Activity {

    public static PendingIntent pendingIntent; //intent값을 옮겨넣는다.
    NfcAdapter nfcAdapter = BluetoothFragment.nfcAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setContentView(R.layout.activity_nfc);

        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //현재 액티비티에서 NFC데이터 처리.
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        onNewIntent(getIntent());

    }

    public void onWindowFocusChanged(boolean hasFocus){
        ImageView imgView = (ImageView) findViewById(R.id.animationImage);
        imgView.setVisibility(ImageView.VISIBLE);
        imgView.setBackgroundResource(R.drawable.nfc_tag);

        AnimationDrawable frameAnimation = (AnimationDrawable) imgView.getBackground();
        frameAnimation.start();
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))
        {
            //Intent nintent =new Intent(getApplicationContext(), NfcTextActivity.class);
            //startActivity(nintent);

            Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if(messages == null) return;

            for(int i=0; i<messages.length; i++){
                insertDB((NdefMessage)messages[i]);
            }
            finish();
        }

    }

    public void insertDB(NdefMessage mMessage){

        NdefRecord[] recs = mMessage.getRecords();

        //원래는 이렇게 긁어옴.
        for(int i=0; i< recs.length; i++){

            NdefRecord record = recs[i];
            byte[] payload = record.getPayload();


            //임시 데이터.
            /*byte[] test = {
                    0x01, 0x01,(byte)0xF4, 0x00, 0x01, (byte)0x98, 0x03, (byte)0xE8,
                    0x5C, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x96, 0x01, 0x21, 0x24, 0x00,
                    0x5C, 0x01, 0x42, 0x25, 0x00,
                    (byte)0xC8, 0x00, 0x00, 0x24, 0x00,
                    0x5D, 0x01, 0x21, 0x24, 0x00,
                    (byte)0xFF, 0x01, (byte)0xFF, 0x25, 0x00,
                    0x5E, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x80, 0x01, (byte)0x80, 0x24, 0x00,
                    0x5C, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x96, 0x01, 0x21, 0x24, 0x00,
                    0x5C, 0x01, 0x42, 0x25, 0x00,
                    (byte)0xC8, 0x00, 0x00, 0x24, 0x00,
                    0x5D, 0x01, 0x21, 0x24, 0x00,
                    (byte)0xFF, 0x01, (byte)0xFF, 0x25, 0x00,
                    0x5E, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x80, 0x01, (byte)0x80, 0x24, 0x00,
                    0x5C, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x96, 0x01, 0x21, 0x24, 0x00,
                    0x5C, 0x01, 0x42, 0x25, 0x00,
                    (byte)0xC8, 0x00, 0x00, 0x24, 0x00,
                    0x5D, 0x01, 0x21, 0x24, 0x00,
                    (byte)0xFF, 0x01, (byte)0xFF, 0x25, 0x00,
                    0x5E, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x80, 0x01, (byte)0x80, 0x24, 0x00,
                    0x5C, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x96, 0x01, 0x21, 0x24, 0x00,
                    0x5C, 0x01, 0x42, 0x25, 0x00,
                    (byte)0xC8, 0x00, 0x00, 0x24, 0x00,
                    0x5D, 0x01, 0x21, 0x24, 0x00,
                    (byte)0xFF, 0x01, (byte)0xFF, 0x25, 0x00,
                    0x5E, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x80, 0x01, (byte)0x80, 0x24, 0x00,
                    0x5C, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x96, 0x01, 0x21, 0x24, 0x00,
                    0x5C, 0x01, 0x42, 0x25, 0x00,
                    (byte)0xC8, 0x00, 0x00, 0x24, 0x00,
                    0x5D, 0x01, 0x21, 0x24, 0x00,
                    (byte)0xFF, 0x01, (byte)0xFF, 0x25, 0x00,
                    0x5E, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x80, 0x01, (byte)0x80, 0x24, 0x00,
                    0x5C, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x96, 0x01, 0x21, 0x24, 0x00,
                    0x5C, 0x01, 0x42, 0x25, 0x00,
                    (byte)0xC8, 0x00, 0x00, 0x24, 0x00,
                    0x5D, 0x01, 0x21, 0x24, 0x00,
                    (byte)0xFF, 0x01, (byte)0xFF, 0x25, 0x00,
                    0x5E, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x80, 0x01, (byte)0x80, 0x24, 0x00,
                    0x5C, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x96, 0x01, 0x21, 0x24, 0x00,
                    0x5C, 0x01, 0x42, 0x25, 0x00,
                    (byte)0xC8, 0x00, 0x00, 0x24, 0x00,
                    0x5D, 0x01, 0x21, 0x24, 0x00,
                    (byte)0xFF, 0x01, (byte)0xFF, 0x25, 0x00,
                    0x5E, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x80, 0x01, (byte)0x80, 0x24, 0x00,
                    0x5C, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x96, 0x01, 0x21, 0x24, 0x00,
                    0x5C, 0x01, 0x42, 0x25, 0x00,
                    (byte)0xC8, 0x00, 0x00, 0x24, 0x00,
                    0x5D, 0x01, 0x21, 0x24, 0x00,
                    (byte)0xFF, 0x01, (byte)0xFF, 0x25, 0x00,
                    0x5E, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x80, 0x01, (byte)0x80, 0x24, 0x00,
                    0x5C, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x96, 0x01, 0x21, 0x24, 0x00,
                    0x5C, 0x01, 0x42, 0x25, 0x00,
                    (byte)0xC8, 0x00, 0x00, 0x24, 0x00,
                    0x5D, 0x01, 0x21, 0x24, 0x00,
                    (byte)0xFF, 0x01, (byte)0xFF, 0x25, 0x00,
                    0x5E, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x80, 0x01, (byte)0x80, 0x24, 0x00,
                    0x5C, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x96, 0x01, 0x21, 0x24, 0x00,
                    0x5C, 0x01, 0x42, 0x25, 0x00,
                    (byte)0xC8, 0x00, 0x00, 0x24, 0x00,
                    0x5D, 0x01, 0x21, 0x24, 0x00,
                    (byte)0xFF, 0x01, (byte)0xFF, 0x25, 0x00,
                    0x5E, 0x00, 0x00, 0x24, 0x00,
                    (byte)0x80, 0x01, (byte)0x80, 0x24, 0x00
            };*/

            if(Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)){
                Intent intent = new Intent(getApplicationContext(),InsertService.class);
                intent.putExtra("RealData",payload);
                startService(intent);

            }
            else if(Arrays.equals(record.getType(), NdefRecord.RTD_URI)){
                //
                Toast toast = Toast.makeText(getApplicationContext(), "지원하지 않는 NFC입니다.", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
    }

    /*public void byteDecoding(byte[] buf){


        GlucoseData glucoseData = new GlucoseData();

        String type;
        String deviceID;
        int numbering;
        int battery;
        double rawData=0;
        double temperature=0;


        for(int i=0; i<buf.length; i++){

            //type
            if(i==0){
                //type = String.valueOf(buf[0]);
                type = byteTostr(buf[0]);
                System.out.println("type : " + type);
            }
            //deviceID
            else if(i==1){
                //deviceID = String.valueOf(buf[1]<<8 | buf[2]);
                deviceID = byteTostr(buf[1],buf[2]);
                System.out.println("deviceID : "+ deviceID);
            }
            //nubmering
            else if(i==3){
                //numbering = buf[3]<<16  | buf[4]<<8 | buf[5];
                numbering = byteToint(buf[3], buf[4], buf[5]);
                System.out.println("numbering : "+ numbering);
            }
            //battery
            else if(i==6){
                //battery = buf[6]<<8 | buf[7];
                battery = byteToint(buf[6],buf[7]);
                System.out.println("battery : "+ battery);
            }

            //gluecoseData & temperature;
            else{
                //정수
                if((i-8)%5 == 0){
                    //rawData = buf[i];
                    rawData = byteTodouble(buf[i]);
                }
                //소수점 확인.
                else if((i-9)%5 == 0 && buf[i+1] != 0){
                    //rawData += (buf[i+1])*0.01;

                    double temp = byteTodouble(buf[i+1]);
                    //1자리.
                    if(temp>=100){
                        rawData += (temp*0.001);
                    }
                    //2자리.
                    else if(temp>=10){
                        rawData += (temp*0.01);
                    }
                    //3자리.
                    else if(temp>=1){
                        rawData += (temp*0.1);
                    }
                }
                else if((i-11)%5 == 0){
                    //temperature = buf[i];
                    temperature = (byteTodouble(buf[i]));
                }
                else if((i>=12) && (i-12)%5 == 0){
                    //insert
                    System.out.print("rawData : "+ rawData);
                    System.out.println("  temperature : "+ temperature);


                }
            }

        }

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
    }*/



    @Override
    protected void onPause() {
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }

    }
}

