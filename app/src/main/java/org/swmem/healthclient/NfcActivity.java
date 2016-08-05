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
import android.view.Window;
import android.widget.ImageView;

import org.swmem.healthclient.db.GlucoseData;

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
            byte[] test = {
                    0x01, 0x00,0x32, 0x00, 0x00, 0x08, 0x00, 0x64,
                    0x5C, 0x00, 0x00, 0x24, 0x00,
                    0x5C, 0x01, 0x21, 0x24, 0x00,
                    0x5C, 0x01, 0x42, 0x25, 0x00,
                    0x5D, 0x00, 0x00, 0x24, 0x00,
                    0x5D, 0x01, 0x21, 0x24, 0x00,
                    0x5D, 0x01, 0x42, 0x25, 0x00,
                    0x5E, 0x00, 0x00, 0x24, 0x00,
                    0x5E, 0x01, 0x21, 0x24, 0x00
            };

            if(Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)){
                byteDecoding(test);
            }
            else if(Arrays.equals(record.getType(), NdefRecord.RTD_URI)){
                //
            }
        }
    }

    public void byteDecoding(byte[] buf){

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
                type = String.valueOf(buf[0]);
                System.out.println("type : " + type);
            }
            //deviceID
            else if(i==1){
                deviceID = String.valueOf(buf[1]<<8 | buf[2]);
                System.out.println("deviceID : "+ deviceID);
            }
            //nubmering
            else if(i==3){
                numbering = buf[3]<<16  | buf[4]<<8 | buf[5];
                System.out.println("numbering : "+ numbering);
            }
            //battery
            else if(i==6){
                battery = buf[6]<<8 | buf[7];
                System.out.println("battery : "+ battery);
            }

            //gluecoseData & temperature;
            else{
                //정수
                if((i-8)%5 == 0){
                    rawData = buf[i];
                }
                //소수점 확인.
                else if((i-9)%5 == 0 && buf[i] != 0){
                    rawData += (buf[i+1])*0.01;
                }
                else if((i-11)%5 == 0){
                    temperature = buf[i];
                }
                else if((i>=12) && (i-12)%5 == 0){
                    //insert
                    System.out.print("rawData : "+ rawData);
                    System.out.println("  temperature : "+ temperature);
                }
            }

        }

    }

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

