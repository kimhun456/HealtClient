package org.swmem.healthclient.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.BuildConfig;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import org.swmem.healthclient.db.HealthContract;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.channels.FileChannel;

import static android.os.Environment.getDataDirectory;
import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by hyunjae on 16. 8. 5.
 *
 * 데이터베이스를 공유하게 해주는 AsyncTask..
 * 현재까지 저장된 데이터베이스를 share할 수 있다.
 * 데이터베이스는 sqlite 형식의 glucose.db라는 파일로 공유된다.
 *
 */
public class ShareDataBaseTask extends AsyncTask<Void,Void,Void> {


    private String backupDBPath = "backup.db";
    private String csvPath = "glucare.csv";
    private Context context;
    public ShareDataBaseTask(Context context){
        this.context = context.getApplicationContext();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");

        File root = getExternalStorageDirectory();
        File file = new File(root, csvPath);
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(context, "Attachment Error", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(context,
                "org.swmem.healthclient.provider",
                file);

        intent.putExtra(Intent.EXTRA_STREAM, uri);

        Intent openInChooser = Intent.createChooser(intent, "Share with...");

        PendingIntent pendingIntent = PendingIntent.getActivity(context,0,openInChooser,0);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    public void exportDatabase(String databaseName) {
        try {
            File sd = getExternalStorageDirectory();


            if (sd.canWrite()) {

                String[] selectionArgs = {""};

                Cursor cursor = context.getContentResolver().query(
                        HealthContract.GlucoseEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        null
                );

                Log.v ("cursor" ,"se");

                File exportDir = new File(Environment.getExternalStorageDirectory(), "");
                if (!exportDir.exists())
                {
                    exportDir.mkdirs();
                }

                File file = new File(exportDir, csvPath);
                try
                {
                    file.createNewFile();
                    CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
//                    Log.v ("cursor" ,"date : " + cursor.getColumnNames());
                    csvWrite.writeNext(cursor.getColumnNames());
                    while(cursor.moveToNext())
                    {
                        //Which column you want to exprort
                        String arrStr[] ={
                                cursor.getString(0),
                                cursor.getString(1),
                                cursor.getString(2),
                                cursor.getString(3),
                                cursor.getString(4),
                                cursor.getString(5),
                                cursor.getString(6)

                        };

                        Log.v("cursor : ", "" + cursor.getString(1));

                        csvWrite.writeNext(arrStr);
                    }
                    csvWrite.close();
                    cursor.close();
                }
                catch(Exception sqlEx)
                {
                    Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
                }
//
//                if (currentDB.exists()) {
//                    FileChannel src = new FileInputStream(currentDB).getChannel();
//                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
//                    dst.transferFrom(src, 0, src.size());
//                    src.close();
//                    dst.close();
//                }
            }
        } catch (Exception e) {
            Log.e("Database Export",e.toString());
        }
    }


    @Override
    protected Void doInBackground(Void... voids) {

        exportDatabase("glucare.db");
        return null;
    }
}
