package org.swmem.healthclient.Nfc;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
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



    public void SetTag(Tag nfcTag) {
        Log.d(TAG, "SetTag");

        mytag = nfcTag;
        System.out.println("READ NFC");

        read(mytag);
    }

    public void read(Tag tag){
        Log.d(TAG, "read");
        byte[] real_data = new byte[8192];
        byte[] temp_data = new byte[8192];
        byte[] write_data = new byte[]{0x02, 0x21, (byte)4, 0x42, 0x45, 0x42, 0x44 }; //low data rate / single block / address / "READ"

        int real_data_index=0;
        int write_data_index=0;
        int read_data_index=0;
        boolean update = true;

        // Toast.makeText(getApplicationContext(), "TAG Check 1", Toast.LENGTH_SHORT).show();

        if(tag != null){


            byte[] id = tag.getId();
            byte[] readCmd = new byte[3+id.length];
            readCmd[0] = 0x20;
            readCmd[1] = 0x20;
            System.arraycopy(id, 0, readCmd, 2, id.length);
            readCmd[2+id.length] = (byte)0;

            NfcV tech = NfcV.get(tag);
            //Toast.makeText(getApplicationContext(), "TAG Check 3", Toast.LENGTH_SHORT).show();

            if (tech != null) {
                // send read command

                Log.d(TAG, "NfcV tech get");
                try {
                    tech.connect();
                    Log.d(TAG, "tech connect");

                    for(int i=0; i<10; i++){ //block 변경 해야함. 10개블락 => 2147블락
                        Log.d(TAG, "I : "+ i);
                        readCmd[2+id.length]= (byte)i;
                        temp_data = tech.transceive(readCmd);

                        for(int j=1; j< temp_data.length; j++){
                            Log.d(TAG, "info_data : "+ temp_data[j]);
                            real_data[real_data_index++] = temp_data[j]; //copy.
                        }

                        //"MAKE" 확인
                        if(real_data_index == 16){
                            if(real_data[12]==0x4D && real_data[13]==0x41 && real_data[14]==0x4B && real_data[15]==0x45){
                                //1초대기 후 다시 read
                                Thread.sleep(1000);
                                //초기화.
                                real_data_index=0;
                                i=-1;
                            }

                            //"READ" 기록
                            else{
                                try{
                                    tech.transceive(write_data);
                                }
                                catch(Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    Log.d(TAG, "real_data_index : " + real_data_index);
                    //데이터 모두 전송받으면 인텐트 전송.
                    if(real_data_index == 40){ //40->8192
                        Log.d(TAG, "trans Intent to insertService");
                        //데이터 배열 intent넘기기.
                        Intent intent = new Intent(this, InsertService.class);
                        intent.putExtra("RealData", real_data);
                        intent.putExtra("RealCnt", read_data_index);
                        startService(intent);
                    }
                    else{
                        Log.d(TAG, "trans Error");
                        tech.close();
                        Toast.makeText(getApplicationContext(),"다시 태깅해주세요.", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        tech.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }




    public static  String byteTostr(byte first_buf, byte second_buf){
        return String.valueOf(((first_buf)&0xff)<<8 | second_buf&0xff);
    }

    public static int byteToint(byte first_buf, byte second_buf){
        return ((first_buf & 0xff)<<8 | (second_buf & 0xff));
    }

}