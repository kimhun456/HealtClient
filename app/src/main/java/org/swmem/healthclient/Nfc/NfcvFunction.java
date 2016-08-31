package org.swmem.healthclient.Nfc;

/**
 * Created by Woo on 2016-08-29.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import org.swmem.healthclient.R;
import org.swmem.healthclient.service.InsertService;

import java.io.IOException;

/**
 * Created by Woo on 2016-08-27.
 */
public class NfcvFunction extends Activity{

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

        //read_data(실제로 그래프를 그릴 데이터)
        byte[] real_data = new byte[10000];
        int real_data_length=0;

        //temp data;(카드에서 받은 All 데이터)
        int temp_data_length = 0;
        byte[] temp_data = new byte[10000];

        //write command = 모듈이 "MAKE"가 아닌것을 확인 후 포인터 G = "READ " 기록
        byte[] write_response = new byte[]{(byte)0xFF};
        int write_start_position = 0;
        int write_end_position = 4;
        byte[] write_data = new byte[]{
                (byte)0x0A, (byte)0x21, //single block
                (byte) write_end_position, (byte) write_start_position, //address
                (byte)0x52, (byte)0x45, (byte)0x41, (byte)0x44}; // "READ"

        //write2 command = 데이터를 모두 수신 후 포인터 G =  " " 기록
        byte[] write2_response = new byte[]{(byte)0xFF};
        int write2_start_position = 0;
        int write2_end_position = 4;
        byte[] write2_data = new byte[]{
                (byte)0x0A, (byte)0x21, //single block
                (byte) write2_end_position, (byte) write2_start_position, //address
                (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20}; //"    "

        //write3 command = 포인터 D 갱신.
        byte[] write3_response = new byte[]{(byte)0xFF};
        int write3_start_position = 0;
        int write3_end_position = 2;
        byte write3_pointer_D1=0; //포인터 E(9메모리)임시 저장.(다음위치)
        byte write3_pointer_D2=0; //포인터 E(10메모리)임시 저장. (다음위치)


        //read할때마다 사용하는 Command
        int read_sector_position = 0;
        int read_moveblock_position = 0;
        int read_block = 31;

        byte[] readCmd = new byte[]{
                (byte) 0x0A, (byte) 0x23, //multiblock
                (byte) read_moveblock_position, //block address
                (byte) read_sector_position, //sector address
                (byte) read_block}; //31블락씩 read

        byte[] read_response = new byte[] {(byte) 0x0A};

        boolean checkMAKE=true; //MAKE 상태확인.
        boolean checkDE = true; //D,E 갱신 확인.

        int data_start_position=0; //데이터 시작 위치
        int data_end_position=0; //데이터 끝 위치
        int data_length=0; //데이터 길이.


        int error = 1;
        NfcV nfcvTag = NfcV.get(mytag);  //ISO 15693 NFCv 사용.
        while (error != 0) {
            try {
                nfcvTag.close();
                nfcvTag.connect();

                read_response = nfcvTag.transceive(readCmd); //readCmd 배열을 이용하여 해당 위치의 데이터를 읽어옴.

/*                Log.d(TAG, "sector_position" + read_sector_position);
                Log.d(TAG, "moveblock_position" + read_moveblock_position);*/

                for (int j = 1; j < read_response.length; j++)
                    temp_data[temp_data_length++] = read_response[j]; //임시 배열에 저장.

                //데이터 시작위치 & 데이터 끝위치.
                if(checkDE && temp_data_length>=8){
                    data_start_position = byteToint(temp_data[8], temp_data[9]);
                    data_end_position = byteToint(temp_data[10], temp_data[11]);
                    write3_pointer_D1 = temp_data[10];
                    write3_pointer_D2 = temp_data[11];
                    checkDE = false;

                    if(data_start_position < data_end_position){
                        data_length = data_end_position - data_start_position;
                    }
                    //메모리 시작위치가 끝위치보다 큰 경우.(순환큐 됐을 경우)
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
                                write_response = nfcvTag.transceive(write_data); //wrtie.

                                if(write_response[0]==(byte)0x00 || write_response[0]==(byte)0x01){ //제대로 write했는지 확인.
                                    Log.d(TAG, "Write -> READ complete");
                                    write_error=0;
                                    checkMAKE= false;
                                }
                            } catch (Exception e) {

                                write_error++;
                                if(write_error==3){
                                    Log.e(TAG, "write _ error");
                                    e.printStackTrace();
                                    return false;
                                }
                            }
                        }
                    }
                }

                //읽는 position을 계속해서 증가시켜줌. position은 0->32->64->96-> -128 -> -96 -> -64 -> -32 -> 0 순으로 반복.
                read_moveblock_position = read_moveblock_position + 32;
                if(read_moveblock_position == 0){
                    read_sector_position++;
                }
                else if(read_moveblock_position == 128){
                    read_moveblock_position *= -1;
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
                            write2_response = nfcvTag.transceive(write_data); //write

                            if(write2_response[0]==(byte)0x00 || write2_response[0]==(byte)0x01){ //완전히 write했는지 확인.
                                Log.d(TAG, "Write2 -> G = '  'complete");
                                write2_error=0;
                                checkMAKE= false;
                            }
                        } catch (Exception e) {

                            write2_error++;

                            if(write2_error==3){
                                Log.e(TAG, "write2 _ error");
                                e.printStackTrace();
                                return false;
                            }
                        }
                    }

                    //포인터(D)갱신
                    int write3_error=1;
                    byte[] write3_data = new byte[]{
                            (byte)0x0A, (byte)0x21, //Single write
                            (byte) write3_end_position, (byte) write3_start_position, //address
                            (byte)write3_pointer_D1, (byte)write3_pointer_D2, (byte)write3_pointer_D1, (byte)write3_pointer_D2}; //다음 시작위치.

                    while(write3_error!=0){
                        try {
                            write3_response = nfcvTag.transceive(write_data); //write

                            if(write3_response[0]==(byte)0x00 || write3_response[0]==(byte)0x01){ //완전히 write했는지 확인.
                                Log.d(TAG, "Write3 _ Pointer D complete ");
                                write3_error=0;
                                checkMAKE= false;

                            }
                        } catch (Exception e) {

                            write3_error++;

                            if(write3_error==3){
                                Log.e(TAG, "write3 _ error");
                                e.printStackTrace();
                                return false;
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
                    return false;
                }
            } finally {
                nfcvTag.close();
            }

        }

        //Toast.makeText(mContext.getApplicationContext(), "넘어옴.", Toast.LENGTH_LONG).show();

        //data check

        Log.d(TAG , "start : "+data_start_position + "end : " + data_end_position);
        for(int i=0; i<100; i++){
            Log.d(TAG, "temp_data : "+ i + " " + temp_data[i]);
        }

//데이터 저장(최근데이터 부터 넣어야 해서 끝지점 부터 저장합니다)
        //시작지점이 끝지점 보다 작은경우.
        if(data_start_position < data_end_position){
            for(int i=data_end_position-1; i>=data_start_position; i--){
                real_data[real_data_length++] = temp_data[i];
            }
        }
        //시작지점이 끝지점 보다 큰 경우(메모리가 8192를 넘었을 경우 순환큐 방식으로 저장되어있기 때문에, 두 구간으로 나눠 각각 저장)
        else if(data_start_position > data_end_position){
            //end->0
            for(int i= data_end_position-1; i>=0; i--){
                real_data[real_data_length++] = temp_data[i];
            }

            //8192->start
            for(int i=8191; i>=data_start_position; i--){
                real_data[real_data_length++] = temp_data[i];
            }
        }

        //데이터 확인.
        for(int i=0; i<real_data_length; i++){
            Log.d(TAG, "real_data : "+ i + " " + real_data[i]);
        }

        Log.d(TAG, "trans Intent to insertService");
        //데이터 배열 intent넘기기.
/*        Intent intent = new Intent(mContext.getApplicationContext(), InsertService.class);
        intent.putExtra("RealData", real_data);
        intent.putExtra("RealCnt", real_data_length);
        intent.putExtra("MyType", 1);
        mContext.getApplicationContext().startService(intent);*/

        return true;
    }

    public static int byteToint(byte first_buf, byte second_buf){
        return ((first_buf & 0xff)<<8 | (second_buf & 0xff));
    }

}