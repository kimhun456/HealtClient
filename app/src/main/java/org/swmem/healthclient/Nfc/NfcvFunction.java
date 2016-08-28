package org.swmem.healthclient.Nfc;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import android.widget.Toast;

import org.swmem.healthclient.service.InsertService;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Woo on 2016-08-27.
 */
public class NfcvFunction extends Activity{

    static String TAG = "NfcVFunction";
    private Tag mytag;
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    static String type;
    static String deviceID;
    static int battery;
    static int data_start;
    static int data_end;
    static double rawData=0;
    static double temperature=0;


    public void read(Tag tag) {
        Log.d(TAG, "read");
        mytag = tag;

        //read
        byte[] read_response = new byte[]{(byte) 0x0A};
        byte[] read_data = new byte[8192];
        int read_data_length = 0;
        byte[] ReadSingleBlockFrame;
        byte read_startAddress = 0x0000;
        //ReadSingleBlockFrame = new byte[]{(byte) 0x02, (byte) 0x20, read_startAddress}; //ReadSingle command, startAddress.

        int offset=0;
        int blocks = 2047;
        ReadSingleBlockFrame = new byte[]{(byte) 0x60, (byte) 0x23,(byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)(offset&0x0ff), (byte)((blocks -1 ) & 0x0ff)}; //Read Multi command, startAddress.

        //write
        byte[] write_response = new byte[]{(byte)0xFF};
        byte[] WriteSingleBlockFrame;
        byte write_startAddress = 0x10;
        WriteSingleBlockFrame = new byte[]{(byte)0x02, (byte)0x21, write_startAddress, 0x52, 0x45, 0x41, 0x44 };
        int write_errorOccured=1;

        int errorOccured = 1;

        NfcV nfcvTag = NfcV.get(tag);
        try{
            nfcvTag.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        //while (errorOccured != 0) {
            try {

                read_response = nfcvTag.transceive(ReadSingleBlockFrame);

                Log.d(TAG, "try");
                for (int j = 0; j < read_response.length; j++) {
                    read_data[read_data_length++] = read_response[j];
                    System.out.println(" read data : " + read_response[j]);
                }
                //read_startAddress = (byte) (read_startAddress | 0x0004);

                //MAKE확인.
                if(read_data_length == 16){
                    if(read_data[12] == 0x4D && read_data[13]==0x41 && read_data[14] ==0x4B && read_data[15] == 0x45 ){
                        //1초대기. => 다시 search.
                        Thread.sleep(1000);
                        read_data_length=0;
                        read_startAddress=0x0000;
                    }

                    //"READ" 기록.
                    else{
                        while(write_errorOccured!=0){
                            write_response = nfcvTag.transceive(WriteSingleBlockFrame);
                            if(write_response[0] == (byte)0x00 || write_response[0] == (byte) 0x01){
                                write_errorOccured = 0;
                            }
                        }
                    }
                }

                if (read_response[0] == (byte) 0x00 || read_response[0] == (byte) 0x01)
                    errorOccured = 0;

            } catch (Exception e) {
                e.printStackTrace();
            }
        //

        //데이터 누락없이 모두 받았을 경우.
        if (read_data_length == 8192) {
            Log.d(TAG, "trans Intent to insertService");
            //데이터 배열 intent넘기기.
            Intent intent = new Intent(getApplicationContext(), InsertService.class);
            intent.putExtra("RealData", read_data);
            intent.putExtra("RealCnt", read_data_length);
            startService(intent);
        }
        else{
            Log.d(TAG, "trans error");
            Toast.makeText(getApplicationContext(),"다시 시도해 주십시오.", Toast.LENGTH_LONG).show();
        }


    }
}