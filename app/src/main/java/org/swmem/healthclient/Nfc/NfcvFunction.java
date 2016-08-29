package org.swmem.healthclient.Nfc;

/**
 * Created by Woo on 2016-08-29.
 */

import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.util.Log;
import android.widget.Toast;

import org.swmem.healthclient.service.InsertService;

import java.io.IOException;

/**
 * Created by Woo on 2016-08-27.
 */
public class NfcvFunction {

    static String TAG = "NfcVFunction";
    private Tag mytag;
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    Context mContext;

    NfcvFunction(Context context) {
        this.mContext = context;
    }

    public boolean read(Tag tag) throws IOException {
        Log.d(TAG, "read");
        mytag = tag;

        //temp data;
        int temp_data_length = 0;
        byte[] temp_data = new byte[10000];

        //write command = "READ " 기록
        byte[] write_response = new byte[]{(byte)0xFF};
        int write_start_position = 0;
        int write_end_position = 4;
        byte[] write_data = new byte[]{
                (byte)0x0A, (byte)0x21,
                (byte) write_end_position, (byte) write_start_position,
                (byte)0x52, (byte)0x45, (byte)0x41, (byte)0x44}; //low data rate / multi block  / address / "READ"

        //write2 command = " " 기록
        byte[] write2_response = new byte[]{(byte)0xFF};
        int write2_start_position = 0;
        int write2_end_position = 4;
        byte[] write2_data = new byte[]{
                (byte)0x0A, (byte)0x21,
                (byte) write2_end_position, (byte) write2_start_position,
                (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20}; //low data rate / multi block / address / "    "

        //write3 command = " " 기록
        byte[] write3_response = new byte[]{(byte)0xFF};
        int write3_start_position = 0;
        int write3_end_position = 2;
        byte write3_pointer_D1=0;
        byte write3_pointer_D2=0;

        //read commnad
        int read_sector_position = 0;
        int read_moveblock_position = 0;
        int read_block = 31;


        byte[] readCmd = new byte[]{
                (byte) 0x0A, (byte) 0x23,
                (byte) read_moveblock_position,
                (byte) read_sector_position,
                (byte) read_block};

        byte[] read_response = new byte[] {(byte) 0x0A};

        boolean checkMAKE=true;
        boolean checkDE = true;
        int data_start_position=0;
        int data_end_position=0;
        int data_length=0;

        int error = 1;
        NfcV nfcvTag = NfcV.get(mytag);
        while (error != 0) {
            try {
                nfcvTag.close();
                nfcvTag.connect();

                read_response = nfcvTag.transceive(readCmd);

                Log.d(TAG, "sector_position" + read_sector_position);
                Log.d(TAG, "moveblock_position" + read_moveblock_position);


                for (int j = 1; j < read_response.length; j++)
                    temp_data[temp_data_length++] = read_response[j];

                //데이터 시작위치 & 데이터 끝위치.
                if(checkDE && temp_data_length>=12){
                    data_start_position = byteToint(temp_data[12], temp_data[13]);
                    data_end_position = byteToint(temp_data[14], temp_data[15]);
                    write3_pointer_D1 = temp_data[14];
                    write3_pointer_D2 = temp_data[15];
                    checkDE = false;

                    if(data_start_position < data_end_position){
                        data_length = data_end_position - data_start_position;
                    }
                    else{
                        data_length = 8192-data_start_position + data_end_position;
                    }
                }

                //"MAKE" 확인
                if (checkMAKE && temp_data_length >= 16) {
                    if (temp_data[12] == 0x4D && temp_data[13] == 0x41 && temp_data[14] == 0x4B && temp_data[15] == 0x45) {
                        return false;
                    }

                    //"READ" 기록
                    else {
                        int write_error=1;

                        while(write_error!=0){
                            Log.d(TAG, "Write _ READ ");
                            try {
                                write_response = nfcvTag.transceive(write_data);

                                if(write_response[0]==(byte)0x00 || write_response[0]==(byte)0x01){
                                    write_error=0;
                                    checkMAKE= false;
                                }
                            } catch (Exception e) {

                                write_error++;

                                if(write_error==3){
                                    Log.e(TAG, "write _ error");
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }

                read_moveblock_position = read_moveblock_position + 32;

                //sector++;
                if(read_moveblock_position==256){
                    read_moveblock_position=0;
                    read_sector_position++;
                }

                System.out.println("temp_data_length : " + temp_data_length);

                //데이터 완전히 다 읽음.
                if ( (8192 == temp_data_length) && read_response[0] == (byte) 0x00 || read_response[0] == (byte) 0x01){
                    error = 0;

                    //G="    "기록
                    int write2_error=1;
                    while(write2_error!=0){
                        Log.d(TAG, "Write2 _ null ");
                        try {
                            write2_response = nfcvTag.transceive(write_data);

                            if(write2_response[0]==(byte)0x00 || write2_response[0]==(byte)0x01){
                                write2_error=0;
                                checkMAKE= false;
                            }
                        } catch (Exception e) {

                            write2_error++;

                            if(write2_error==3){
                                Log.e(TAG, "write2 _ error");
                                e.printStackTrace();
                            }
                        }
                    }

                    //포인터(D)갱신
                    int write3_error=1;
                    byte[] write3_data = new byte[]{
                            (byte)0x0A, (byte)0x21,
                            (byte) write3_end_position, (byte) write3_start_position,
                            (byte)write3_pointer_D1, (byte)write3_pointer_D2, (byte)write3_pointer_D1, (byte)write3_pointer_D2}; //low data rate / multi block / address / "    "

                    while(write3_error!=0){
                        Log.d(TAG, "Write3 _ Pointer D ");
                        try {
                            write3_response = nfcvTag.transceive(write_data);

                            if(write3_response[0]==(byte)0x00 || write3_response[0]==(byte)0x01){
                                write3_error=0;
                                checkMAKE= false;
                            }
                        } catch (Exception e) {

                            write3_error++;

                            if(write3_error==3){
                                Log.e(TAG, "write3 _ error");
                                e.printStackTrace();
                            }
                        }
                    }

                }
            } catch (Exception e) {

                error++;
                if(error==3){

                    Log.e(TAG, "Tag was lost");
                    e.printStackTrace();
                    nfcvTag.close();

                }
            } finally {
                nfcvTag.close();
            }

        }

        //Toast.makeText(mContext.getApplicationContext(), "넘어옴.", Toast.LENGTH_LONG).show();

        //data check
        for(int i=0; i<100; i++){
            Log.d(TAG, "temp_data : "+ temp_data[i]);
        }

        Log.d(TAG, "trans Intent to insertService");
        //데이터 배열 intent넘기기.
        /*Intent intent = new Intent(mContext.getApplicationContext(), InsertService.class);
        intent.putExtra("RealData", temp_data);
        intent.putExtra("RealCnt", temp_data_length);
        mContext.getApplicationContext().startService(intent);*/

        return true;
    }




    public static  String byteTostr(byte first_buf, byte second_buf){
        return String.valueOf(((first_buf)&0xff)<<8 | second_buf&0xff);
    }

    public static int byteToint(byte first_buf, byte second_buf){
        return ((first_buf & 0xff)<<8 | (second_buf & 0xff));
    }

}