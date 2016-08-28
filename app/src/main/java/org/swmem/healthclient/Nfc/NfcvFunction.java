package org.swmem.healthclient.Nfc;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.nfc.FormatException;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.util.Log;
import android.widget.Toast;

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
    static int data_length;
    static int data_start;
    static int data_end;
    static double rawData=0;
    static double temperature=0;


    public void SetTag(Tag nfcTag) {
        Log.d(TAG, "SetTag");

        mytag = nfcTag;
        System.out.println("READ NFC");

        read(mytag);
    }

    public void read(Tag tag){
        Log.d(TAG, "read");
        byte[] real_data = null;
        byte[] info_data = new byte[8192];
        byte[] write_data = null;

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
            if (tech != null) {
                // send read command

                Toast.makeText(getApplicationContext(), "TAG Check 3", Toast.LENGTH_SHORT).show();

                Log.d(TAG, "NfcV tech get");
                try {
                    tech.connect();
                    Log.d(TAG, "tech connect");

                    for(int i=0; i<=2; i++){

                        readCmd[2+id.length]= (byte)i;
                        info_data = tech.transceive(readCmd);

                        Log.d(TAG, "info_ length :" +info_data.length);
                        for(int j=1; j< info_data.length; j++){
                            Log.d(TAG, "info_data : "+info_data[j]);
                            //write_data[write_data_index++] = info_data[j]; //copy.
                        }
                    }

                    type = String.valueOf(info_data[0]);
                    deviceID = byteTostr(info_data[2], info_data[3]);
                    battery = byteToint(info_data[4], info_data[5]);

                    data_start = byteToint(info_data[8], info_data[9]);
                    data_end = byteToint(info_data[10], info_data[11]);

                    if(data_start < data_end){
                        data_length = data_end - data_start;
                    }
                    //end가 8188을 넘은경우.
                    else{
                        data_length = (data_end - 32) + (8192 - data_start);
                        update = false;
                    }

                    // F: MAKE확인 & G:READ확인.
                    if(info_data[12]==0x4D || (char)info_data[13]==0x41 || (char)info_data[14]==0x4B || (char)info_data[15]==0x45){
                        //read기록.
                        write_data[16]=0x52;
                        write_data[17]=0x45;
                        write_data[18]=0x41;
                        write_data[19]=0x44;
                        tech.transceive(write_data);
                    }

                    //data 저장.
                    int a, b, c, d, e;
                    int x=0;
                    read_data_index = data_length-1;

                    if(update){

                        for(int i=data_start; i< data_end; i++){
                            real_data[read_data_index--] = info_data[i];

                            if(x==0){
                                a = info_data[i];
                            }
                            else if(x==1){
                                b = info_data[i];
                            }
                            else if(x==2){
                                c = info_data[i];
                                //raw_data 조합(a,b,c)
                            }
                            else if(x==3){
                                d = info_data[i];
                            }
                            else if(x==4){
                                e = info_data[i];
                                //temp 조합(d,e);
                                x=0;
                                continue;
                            }
                            x++;
                        }
                    }
                    else{
                        for(int i=data_start; i<8192; i++ ){
                            real_data[read_data_index--] = info_data[i];

                            if(x==0){
                                a = info_data[i];
                            }
                            else if(x==1){
                                b = info_data[i];
                            }
                            else if(x==2){
                                c = info_data[i];
                            }
                            else if(x==3){
                                d = info_data[i];
                            }
                            else if(x==4){
                                e = info_data[i];
                                x=0;
                                continue;
                            }
                            x++;
                        }
                        for(int i=0; i<data_end; i++){
                            real_data[read_data_index--] = info_data[i];

                            if(x==0){
                                a = info_data[i];
                            }
                            else if(x==1){
                                b = info_data[i];
                            }
                            else if(x==2){
                                c = info_data[i];
                            }
                            else if(x==3){
                                d = info_data[i];
                            }
                            else if(x==4){
                                e = info_data[i];
                                x=0;
                                continue;
                            }
                            x++;
                        }


                    }





                } catch (IOException e) {
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