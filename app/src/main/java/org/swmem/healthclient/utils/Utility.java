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
package org.swmem.healthclient.utils;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Utility {

    final String TAG = "UTILLITY";


    public static String formatDate(long dateInMilliseconds) {
        Date date = new Date(dateInMilliseconds);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(date);
    }


    public static long cursorDateToLong(String date) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
        Date d = null;
        try {
            d = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return d.getTime();
    }


    public static long getCurrentDate(){


        long currentMilli = System.currentTimeMillis();
        Date date = new Date(currentMilli);
        date.setSeconds(0);
        return date.getTime();
    }


    public static String getGraphDateFormat(long dateInMilliseconds){

        Date date = new Date(dateInMilliseconds);
        return new SimpleDateFormat("HH:mm", Locale.KOREA).format(date);
    }




}