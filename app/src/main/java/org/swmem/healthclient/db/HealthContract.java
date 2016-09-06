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
package org.swmem.healthclient.db;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;


/**
 * Defines table and column names for the weather database.
 */
public class HealthContract {

    public static final String CONTENT_AUTHORITY = "org.swmem.healthclient";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_GLUCOSE = "glucose";


    /* Inner class that defines the table contents of the Glucose table */
    public static final class GlucoseEntry implements BaseColumns {

        public static final String BLUETOOTH = "bluetooth";
        public static final String NFC = "nfc";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_GLUCOSE).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_GLUCOSE;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_GLUCOSE;

        // Table name
        public static final String TABLE_NAME = "glucose_table";

        public static final String COLUMN_GLUCOSE_VALUE = "glucose";
        public static final String COLUMN_TEMPERATURE_VALUE = "temperature";
        public static final String COLUMN_RAW_VALUE = "raw";
        public static final String COLUMN_DEVICE_ID = "device_id";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_TYPE = "type";


        public static Uri buildLocationUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }



}
