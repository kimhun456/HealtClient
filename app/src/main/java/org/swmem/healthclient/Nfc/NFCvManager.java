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
 *
 *
 */
public class NFCvManager {

    static String TAG = "NfcVFunction";
    private Tag mytag;
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    Context mContext;

    public NFCvManager(Context context) {
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
        byte[] write_response;
        int write_start_position = 0;
        int write_end_position = 4;
        byte[] write_data ;

        //write2 command = 데이터를 모두 수신 후 포인터 G =  " " 기록
        byte[] write2_response ;
        int write2_start_position = 0;
        int write2_end_position = 4;
        byte[] write2_data ;

        //write3 command = 포인터 D 갱신.
        byte[] write3_response;
        int write3_start_position = 0;
        int write3_end_position = 2;
        byte write3_pointer_D1=0; //포인터 E(9메모리)임시 저장.(다음위치)
        byte write3_pointer_D2=0; //포인터 E(10메모리)임시 저장. (다음위치)

        //read할때마다 사용하는 Command
        int read_sector_position = 0;
        int read_moveblock_position = 0;
        int read_block = 31;
        byte[] readCmd;
        byte[] read_response;

        boolean checkMAKE=true; //MAKE 상태확인.
        boolean checkDE = true; //D,E 갱신 확인.

        boolean Overwrite_flag = false; //오버라이팅 판단여부.
        int data_end_position=0; //데이터 끝 위치
        int data_length=0; //읽어야할 데이터 길이.

        int error = 1;
        NfcV nfcvTag = NfcV.get(mytag);  //ISO 15693 NFCv 사용.
        while (error != 0) {
            try {
                nfcvTag.close();
                nfcvTag.connect();

                read_response = new byte[] {(byte) 0x0A}; //초기화.

                readCmd = new byte[]{
                        (byte) 0x0A, (byte) 0x23, //multiblock
                        (byte) read_moveblock_position, //block address
                        (byte) read_sector_position, //sector address
                        (byte) read_block}; //31block씩 read*/

                read_response = nfcvTag.transceive(readCmd); //readCmd 배열을 이용하여 해당 위치의 데이터를 읽어옴.

                //Log.d(TAG, "StartAddress" + (byte)read_moveblock_position + " " + read_sector_position);

                for (int j = 1; j < read_response.length; j++) {
                    temp_data[temp_data_length++] = read_response[j]; //임시 배열에 저장
                }

                //데이터 시작위치 & 데이터 끝위치.
                if(checkDE && temp_data_length>=8){
                    int temp= byteToint(temp_data[8], temp_data[9]);
                    data_end_position = byteToint(temp_data[10], temp_data[11]);
                    write3_pointer_D1 = temp_data[10];
                    write3_pointer_D2 = temp_data[11];
                    checkDE = false;

                    Log.d(TAG, "temp : " + temp); //overwrite
                    Log.d(TAG, "end : " + data_end_position);

                    //오버라이팅 아닌경우.
                    if(temp == 0){
                        Overwrite_flag =false;
                        data_length = data_end_position;
                    }
                    //오버라이팅인 경우
                    else if(temp == 1){
                        Overwrite_flag = true;
                        data_length = 8192;
                    }
                    //시작위치와 끝점이 같음.(즉 데이터를 이미 읽은경우)
                    else{
                        Log.d(TAG, "Start == End");
                        Toast.makeText(mContext.getApplicationContext(), "기록할 최신 데이터가 없습니다.", Toast.LENGTH_LONG).show();
                        return false;
                    }

                }

                //"MAKE" 확인
                if (checkMAKE && temp_data_length >= 16) {
                    if ((temp_data[12] == 0x4D )&& (temp_data[13] == 0x41) && (temp_data[14] == 0x4B )&& (temp_data[15] == 0x45)) {
                        Toast.makeText(mContext.getApplicationContext(), "Device가 기록중입니다.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "MAKE error");
                        return false;
                    }

                    //"READ" 기록
                    else if (temp_data[12] == 0x20 && temp_data[13] == 0x20 && temp_data[14] == 0x20 && temp_data[15] == 0x20){
                        int write_error=1;

                        while(write_error!=0){
                            Log.d(TAG, "Write _ READ ");
                            try {

                                write_response = new byte[]{(byte)0xFF}; //초기화.

                                write_data = new byte[]{
                                        (byte)0x0A, (byte)0x21, //single block
                                        (byte) write_end_position, (byte) write_start_position, //address
                                        (byte)0x52, (byte)0x45, (byte)0x41, (byte)0x44}; // "READ"

                                write_response = nfcvTag.transceive(write_data); //wrtie.

                                if(write_response[0]==(byte)0x00 || write_response[0]==(byte)0x01){ //제대로 write했는지 확인.
                                    Log.d(TAG, "Write -> READ complete");
                                    write_error=0;
                                    checkMAKE= false;
                                }
                            } catch (Exception e) {

                                write_error++;
                                if(write_error==100){
                                    Log.e(TAG, "write _ error");
                                    e.printStackTrace();
                                    return false;
                                }
                            }
                        }
                    }
                    else{
                        return false;
                    }
                }

                //데이터 완전히 다 읽음.(오버라이팅 된 경우 & 안 된 경우)
                if ( (temp_data_length >= data_length) && read_response[0] == (byte) 0x00 || read_response[0] == (byte) 0x01){
                    error = 0;

                    //G="    "기록
                    int write2_error=1;
                    while(write2_error!=0){
                        Log.d(TAG, "Write2 _ null ");
                        try {
                            write2_response = new byte[]{(byte)0xFF};//초기화.

                            write2_data = new byte[]{
                                    (byte)0x0A, (byte)0x21, //single block
                                    (byte) write2_end_position, (byte) write2_start_position, //address
                                    (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20}; //"    "

                            write2_response = nfcvTag.transceive(write2_data); //write

                            if(write2_response[0]==(byte)0x00 || write2_response[0]==(byte)0x01){ //완전히 write했는지 확인.
                                Log.d(TAG, "Write2 -> G = '  'complete");
                                write2_error=0;
                                checkMAKE= false;
                            }
                        } catch (Exception e) {

                            write2_error++;

                            if(write2_error==100){
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
                            write3_response = new byte[]{(byte)0xFF};//초기화.

                            write3_response = nfcvTag.transceive(write3_data); //write

                            if(write3_response[0]==(byte)0x00 || write3_response[0]==(byte)0x01){ //완전히 write했는지 확인.
                                Log.d(TAG, "Write3 _ Pointer D complete ");
                                write3_error=0;
                                checkMAKE= false;

                            }
                        } catch (Exception e) {

                            write3_error++;

                            if(write3_error==100){
                                Log.e(TAG, "write3 _ error");
                                e.printStackTrace();
                                return false;
                            }
                        }
                    }

                }

                //읽는 position을 계속해서 증가시켜줌.
                read_moveblock_position = read_moveblock_position + 32;
                if(read_moveblock_position==256){
                    read_moveblock_position =0;
                    read_sector_position++;
                }


            } catch (Exception e) {

                error++;
                if(error==100){

                    Log.e(TAG, "Tag was lost");
                    e.printStackTrace();
                    nfcvTag.close();
                    return false;
                }
            } finally {
                nfcvTag.close();
            }

        }

        //temp check
        /*Log.d(TAG , " end : " + data_end_position);
        for(int i=0; i<=data_length; i++){
            Log.d(TAG, "temp_data : "+ i + " " + temp_data[i]);
        }*/

        //info_데이터 저장.(데이터 정보 , 센서ID , 베터리 값)
        for(int i=0; i<=5; i++){
            real_data[real_data_length++] = (temp_data[i]);
        }

        //혈당, 온도 데이터 저장(최근데이터 부터 넣어야 해서 끝지점 부터 저장합니다)
        //오버라이팅 경우
        if(Overwrite_flag){
            //end-> 32
            for(int i= data_end_position-1; i>=32; i--){
                real_data[real_data_length++] = temp_data[i];
            }

            //8191->end
            for(int i=8191; i>=data_end_position; i--){
                real_data[real_data_length++] = temp_data[i];

            }
        }
        //오버라이팅 아닌 경우
        else{
            //end->0
            for(int i=data_end_position-1; i>=32; i--){
                real_data[real_data_length++] = temp_data[i];
            }
        }

        //데이터 확인.
        Log.d(TAG, "real_data_length : "  + real_data_length);
        for(int i=0; i<real_data_length; i++){
            Log.d(TAG, "real_data : "+ i + " " + real_data[i]);
        }

        Log.d(TAG, "trans Intent to insertService");
        //데이터 배열 intent넘기기.
        Intent intent = new Intent(mContext.getApplicationContext(), InsertService.class);
        intent.putExtra("RealData", real_data);
        intent.putExtra("RealCnt", real_data_length);
        intent.putExtra("MyType", 1);
        mContext.getApplicationContext().startService(intent);

        return true;
    }

    public static int byteToint(byte first_buf, byte second_buf){
        return ((first_buf & 0xff)<<8 | (second_buf & 0xff));
    }

}