package org.swmem.healthclient.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import static android.os.Environment.getDataDirectory;
import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by hyunjae on 16. 8. 5.
 */
public class ShareDataBaseTask extends AsyncTask<Void,Void,Void> {


    private String backupDBPath = "backup.db";
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
        File file = new File(root, backupDBPath);
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(context, "Attachment Error", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.fromFile(file);
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,0);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    public void exportDatabse(String databaseName) {
        try {
            File sd = getExternalStorageDirectory();
            File data = getDataDirectory();

            if (sd.canWrite()) {
                String currentDBPath = "//data//"+context.getPackageName()+"//databases//"+databaseName+"";
                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {

        }
    }

    @Override
    protected Void doInBackground(Void... voids) {


        exportDatabse("glucare.db");

        return null;
    }
}
