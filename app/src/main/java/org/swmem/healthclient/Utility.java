/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.swmem.healthclient;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Utility {

    private static final int rateINC_MORE = 2;
    private static final int rateINC_LESS = 1;
    private static final int rateDEC_INC = 1;
    private static final int rateINC_DEC = -1;
    private static final int rateDEC_LESS = -1;
    private static final int rateDEC_MORE = -2;


    public static List<GlucoseData> getConvertedList(List<GlucoseData> inputList){

        for(int i=6; i< inputList.size(); i++){
            GlucoseData glucoseData = inputList.get(i);

            if(glucoseData.isConverted()){
                continue;
            }


            double currentData = glucoseData.getRawData();
            double threeMinPastData = inputList.get(i-3).getRawData();
            double sixMinPastData = inputList.get(i-6).getRawData();


            double Diff_F = currentData - threeMinPastData;
            double Diff_S = threeMinPastData - sixMinPastData;
            double Diff_Diff = Diff_F - Diff_S;

            double Compensated_Glimp_change;

            if( (Math.abs(Diff_F) < 1.5) && (Math.abs(Diff_S) < 1.5) )
                if(currentData < 130)
                    Compensated_Glimp_change = Diff_F-6;
                else
                    Compensated_Glimp_change = Diff_F-2;
            else
                if(Diff_F >= 9) {
                    if (Diff_S >= 9)
                        Compensated_Glimp_change = Diff_F + rateINC_MORE * Diff_Diff + 30;
                    else
                        Compensated_Glimp_change = Diff_F + rateINC_MORE * Diff_Diff + 30;
                }
                else if((Diff_F >= 0)&&(Diff_F < 9)) {
                    if ((Diff_F * 10) * (Diff_S * 10) >= 0) {
                        if (Diff_Diff >= 0)
                            Compensated_Glimp_change = Diff_F + rateINC_MORE * Diff_Diff + 10;
                        else
                            Compensated_Glimp_change = Diff_F - rateINC_LESS * Diff_Diff + 10;
                    } else {
                        Compensated_Glimp_change = Diff_F + rateDEC_INC * Math.abs(Diff_F + Diff_S);
                    }
                }
                else if(Diff_F <= -9)
                    Compensated_Glimp_change = Diff_F + rateDEC_MORE*Diff_Diff - 10;
                else{
                    if((Diff_F*10)*(Diff_S*10) >= 0) {
                        if (Diff_Diff >= 0)
                            Compensated_Glimp_change = Diff_F + rateDEC_LESS * Diff_Diff - 5;
                        else
                            Compensated_Glimp_change = Diff_F + rateDEC_MORE * Diff_Diff - 10;
                    }
                    else {
                        Compensated_Glimp_change = Diff_F + rateINC_DEC * Math.abs(Diff_F + Diff_S);
                    }
                }


            if(currentData > 185)
                Compensated_Glimp_change = Compensated_Glimp_change+10;


            glucoseData.setConvertedData(currentData + Compensated_Glimp_change);


        }

        return inputList;
    }

    static String formatDate(long dateInMilliseconds) {
        Date date = new Date(dateInMilliseconds);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(date);
    }




    public static long getCurrentDate(){
        return System.currentTimeMillis();
    }


    public static String getGraphDateFormat(long dateInMilliseconds){

        Date date = new Date(dateInMilliseconds);
        return new SimpleDateFormat("HH:mm", Locale.KOREA).format(date);
    }




}