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
        byte[] temp_data = new byte[8192];

        //write command
        byte[] write_response = new byte[]{(byte)0xFF};
        int write_start_position = 0;
        int write_end_position = 4;
        byte[] write_data = new byte[]{
                (byte)0x0A, (byte)0x21,
                (byte) write_end_position, (byte) write_start_position,
                (byte)0x52, (byte)0x45, (byte)0x41, (byte)0x44}; //low data rate / single block / address / "READ"

        //read commnad
        int read_start_position = 0;
        int read_move_position = 0;
        int read_block = 31;


        byte[] readCmd = new byte[]{
                (byte) 0x0A, (byte) 0x23,
                (byte) read_move_position,
                (byte) read_start_position,
                (byte) read_block};

        byte[] read_response = new byte[]{(byte) 0x0A};

        boolean checkMAKE=true;

        int error = 1;
        NfcV nfcvTag = NfcV.get(mytag);
        while (error != 0) {
            try {
                nfcvTag.close();
                nfcvTag.connect();

                read_response = nfcvTag.transceive(readCmd);

                for (int j = 1; j < read_response.length; j++)
                    temp_data[temp_data_length++] = read_response[j];

                //"MAKE" 확인
                if (checkMAKE && temp_data_length >= 16) {
                    if (temp_data[12] == 0x4D && temp_data[13] == 0x41 && temp_data[14] == 0x4B && temp_data[15] == 0x45) {
                        checkMAKE = false;
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
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "write _ error");
                                e.printStackTrace();
                            }
                        }
                    }
                }

                read_move_position = read_move_position + 32;

                System.out.println("temp_data_length : " + temp_data_length);

                if (temp_data_length >= 4000 && read_response[0] == (byte) 0x00 || read_response[0] == (byte) 0x01)
                    error = 0;

            } catch (Exception e) {
                Log.e(TAG, "Tag was lost");
                e.printStackTrace();
                nfcvTag.close();
            } finally {
                nfcvTag.close();
            }

        }

        Toast.makeText(mContext.getApplicationContext(), "넘어옴.", Toast.LENGTH_LONG).show();
        for (int i = 0; i < 36; i++)
            System.out.println("temp_data :"+  i + " " + temp_data[i]);

        if (temp_data_length == 40) { //40->8192
            Log.d(TAG, "trans Intent to insertService");
            //데이터 배열 intent넘기기.
            Intent intent = new Intent(mContext.getApplicationContext(), InsertService.class);
            intent.putExtra("RealData", temp_data);
            intent.putExtra("RealCnt", temp_data_length);
            mContext.getApplicationContext().startService(intent);
        } else {
            Log.d(TAG, "trans Error");
            Toast.makeText(mContext.getApplicationContext(), "다시 태깅해주세요.", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }




    public static  String byteTostr(byte first_buf, byte second_buf){
        return String.valueOf(((first_buf)&0xff)<<8 | second_buf&0xff);
    }

    public static int byteToint(byte first_buf, byte second_buf){
        return ((first_buf & 0xff)<<8 | (second_buf & 0xff));
    }

}