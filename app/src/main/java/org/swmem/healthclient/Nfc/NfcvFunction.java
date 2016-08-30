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
import java.io.PipedInputStream;

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

        //info data
        int info_data_length=0;
        int info_read_sector_position = 0;
        int info_read_moveblock_position = 0;
        int info_read_block = 31;
        byte[] info_data = new byte[10000];
        byte[] info_readCmd = new byte[]{
                (byte) 0x0A, (byte) 0x23,
                (byte) info_read_moveblock_position,
                (byte) info_read_sector_position,
                (byte) info_read_block};

        byte[] info_read_response = new byte[] {(byte) 0x0A};


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


        //temp read commnad
        int temp_read_sector_position = 0;
        int temp_read_moveblock_position = 0;
        int temp_read_block = 0;

        byte[] temp_readCmd = new byte[]{
                (byte) 0x0A, (byte) 0x23,
                (byte) temp_read_moveblock_position,
                (byte) temp_read_sector_position,
                (byte) temp_read_block};

        byte[] temp_read_response = new byte[] {(byte) 0x0A};


        boolean update = true;
        boolean checkMAKE=true;
        boolean checkDE = true;
        boolean firstCheck = true;
        int data_start_position=0;
        int data_end_position=0;
        int data_length=0;

        int info_error = 1;

        NfcV nfcvTag = NfcV.get(mytag);

        //info_data;
        while(info_error != 0){
            Log.d(TAG, "info read");
            try{
                nfcvTag.close();
                nfcvTag.connect();

                info_read_response = nfcvTag.transceive(info_readCmd);

                read_moveblock_position += 32;

                if(firstCheck){
                    for (int j = 1; j <= 16; j++)
                        info_data[info_data_length++] = info_read_response[j];
                    firstCheck=false;
                }

                Log.d(TAG, "info_data_length :" + info_data_length);

                //데이터 시작위치 & 데이터 끝위치.
                if(checkDE && info_data_length>=12){
                    Log.d(TAG, "start_position & end_position");
                    data_start_position = byteToint(info_data[8], info_data[9]);
                    data_end_position = byteToint(info_data[10], info_data[11]);
                    write3_pointer_D1 = info_data[10];
                    write3_pointer_D2 = info_data[11];
                    checkDE = false;

                    if(data_start_position < data_end_position){
                        data_length = data_end_position - data_start_position;
                        update = true;
                    }
                    else{
                        data_length = 8192-data_start_position + data_end_position;
                        update = false;
                    }
                    Log.e(TAG, "cal_byte :" + info_data[8] + " " +info_data[9]+ " "+ info_data[10] + " "+info_data[11]);
                    Log.e(TAG, "cal_length :" + data_length + " "+data_start_position + " "+ data_end_position);
                }

                //"MAKE" 확인
                if (checkMAKE && info_data_length == 16) {
                    if (info_data[12] == 0x4D && info_data[13] == 0x41 && info_data[14] == 0x4B && info_data[15] == 0x45) {
                        Log.d(TAG, "info MAKE");
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
                                    info_error=0;
                                }
                            } catch (Exception e) {

                                write_error++;

                                if(write_error==10){
                                    Log.e(TAG, "write _ error");
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
            catch(Exception e){
                Log.e(TAG, "info_error");
                e.printStackTrace();
            }
        }

        if(update){

            Log.d(TAG, "start < end");
            read_moveblock_position = data_start_position%256;
            read_sector_position = data_start_position/256;
            read_block = 1;

            int temp_error=1;

            while (temp_error != 0) {
                try {
                    nfcvTag.close();
                    nfcvTag.connect();

                    if(read_sector_position == 31 && read_moveblock_position > 127){
                        read_block = (read_moveblock_position - 127)/4;
                        read_response = nfcvTag.transceive(readCmd);

                        if(read_moveblock_position%4 != 0){
                            temp_read_sector_position=31;
                            temp_read_moveblock_position=252;
                            temp_read_block=1;

                            temp_read_response = nfcvTag.transceive(temp_readCmd);
                        }

                        for (int j = 1; j<read_response.length; j++){
                            info_data[info_data_length++] = read_response[j];
                        }

                        if(temp_read_response[0] == (byte)0x00 || temp_read_response[0] == (byte)0x01){

                            for(int i=(read_moveblock_position%4), j=1; i<4; i++, j++){
                                info_data[info_data_length++] = temp_read_response[j];
                            }
                        }

                    }
                    else {
                        read_response = nfcvTag.transceive(readCmd);

                        for (int j = 1; info_data_length - 16 <=data_length; j++){
                            info_data[info_data_length++] = read_response[j];
                        }
                    }

                    read_moveblock_position += 32;

                    Log.d(TAG, "sector_position" + read_sector_position);
                    Log.d(TAG, "moveblock_position" + read_moveblock_position);
                    Log.d(TAG,"info_data_length : " + data_length );

                    read_moveblock_position = read_moveblock_position + 4;
                    read_sector_position = read_moveblock_position/256;

                    //데이터 완전히 다 읽음.
                    if ( (info_data_length-16 == data_length) && read_response[0] == (byte) 0x00 || read_response[0] == (byte) 0x01){
                        temp_error = 0;

                        //G="    "기록
                        int write2_error=1;
                        while(write2_error!=0){
                            Log.d(TAG, "Write2 _ null ");
                            try {
                                write2_response = nfcvTag.transceive(write2_data);

                                if(write2_response[0]==(byte)0x00 || write2_response[0]==(byte)0x01){
                                    write2_error=0;
                                    checkMAKE= false;
                                }
                            } catch (Exception e) {

                                write2_error++;

                                if(write2_error==10){
                                    Log.e(TAG, "write2 _ error");
                                    return false;
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

                                if(write3_error==10){
                                    Log.e(TAG, "write3 _ error");
                                    return false;
                                }
                            }
                        }

                    }
                } catch (Exception e) {

                    temp_error++;
                    if(temp_error==10){
                        Log.e(TAG, "start < end error");
                        e.printStackTrace();
                        nfcvTag.close();
                        return false;
                    }
                } finally {
                    nfcvTag.close();
                }

            }


        }

        /////////////////////////////////////////////////////////////////////////////////////start_postion > end_position-------------------------
        else{
            Log.d(TAG, "start > end");

            read_moveblock_position = data_start_position%256;
            read_sector_position = data_start_position/256;
            read_block = 31;

            // start -> 8192
            int temp_error=1;
            while (temp_error != 0) {
                Log.d(TAG, "start -> 8192");
                try {
                    nfcvTag.close();
                    nfcvTag.connect();

                    if(read_sector_position>= 31 && read_moveblock_position >= 127){
                        read_block = (read_moveblock_position - 127)/4;
                        read_response = nfcvTag.transceive(readCmd);

                        if(read_moveblock_position%4 != 0){
                            temp_read_sector_position=31;
                            temp_read_moveblock_position=252;
                            temp_read_block=1;

                            temp_read_response = nfcvTag.transceive(temp_readCmd);
                        }

                        for (int j = 1; j<read_response.length; j++){
                            info_data[info_data_length++] = read_response[j];
                        }

                        if(temp_read_response[0] == (byte)0x00 || temp_read_response[0] == (byte)0x01){

                            for(int i=(read_moveblock_position%4), j=1; i<4; i++, j++){
                                info_data[info_data_length++] = temp_read_response[j];
                            }
                        }

                    }
                    else {
                        read_response = nfcvTag.transceive(readCmd);

                        for (int j = 1; j <= 8192 - data_start_position; j++){
                            info_data[info_data_length++] = read_response[j];
                        }
                    }

                    Log.d(TAG, "sector_position" + read_sector_position);
                    Log.d(TAG, "moveblock_position" + read_moveblock_position);

                    read_moveblock_position = read_moveblock_position + 4;
                    read_sector_position = read_moveblock_position/256;

                    System.out.println("info_data_length : " + info_data_length);

                    //데이터 완전히 다 읽음.
                    if ( (8192-data_start_position == info_data_length -16) && read_response[0] == (byte) 0x00 || read_response[0] == (byte) 0x01){
                        temp_error = 0;


                        int end_error=1;
                        while(end_error != 0) {

                            Log.d(TAG, "0 -> end");
                            read_moveblock_position = 0;
                            read_sector_position = 0;
                            read_block = 31;

                            try {
                                nfcvTag.close();
                                nfcvTag.connect();

                                read_response = nfcvTag.transceive(readCmd);

                                Log.d(TAG, "sector_position" + read_sector_position);
                                Log.d(TAG, "moveblock_position" + read_moveblock_position);


                                for (int j = 1; j <= data_end_position; j++)
                                    info_data[info_data_length++] = read_response[j];

                                read_moveblock_position = read_moveblock_position + 4;
                                read_sector_position = read_moveblock_position / 256;

                                System.out.println("info_data_length : " + info_data_length);

                                //데이터 완전히 다 읽음.
                                if ((info_data_length-16 == data_length) && read_response[0] == (byte) 0x00 || read_response[0] == (byte) 0x01) {
                                    end_error = 0;
                                }
                            } catch (Exception e) {
                                end_error++;

                                if(end_error==10){
                                    Log.d(TAG, "0 -> end error");
                                    return false;
                                }
                            }
                        }

                        //G="    "기록
                        int write2_error=1;
                        while(write2_error!=0){
                            Log.d(TAG, "Write2 _ null ");
                            try {
                                write2_response = nfcvTag.transceive(write2_data);

                                if(write2_response[0]==(byte)0x00 || write2_response[0]==(byte)0x01){
                                    write2_error=0;
                                    checkMAKE= false;
                                }
                            } catch (Exception e) {

                                write2_error++;

                                if(write2_error==10){
                                    Log.e(TAG, "write2 _ error");
                                    return false;
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
                                write3_response = nfcvTag.transceive(write3_data);

                                if(write3_response[0]==(byte)0x00 || write3_response[0]==(byte)0x01){
                                    write3_error=0;
                                    checkMAKE= false;
                                }
                            } catch (Exception e) {

                                write3_error++;

                                if(write3_error==10){
                                    Log.e(TAG, "write3 _ error");
                                    return false;
                                }
                            }
                        }

                    }
                } catch (Exception e) {
                    temp_error++;
                    if(temp_error==10){

                        Log.e(TAG, "start > end error");
                        nfcvTag.close();
                        return false;
                    }
                } finally {
                    nfcvTag.close();
                }

            }
        }

        //data check
        Log.d(TAG, "info_data_length:" + info_data_length);
        for(int i=0; i<data_length; i++){
            Log.d(TAG, "info_data : "+ i+ " " + info_data[i]);
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