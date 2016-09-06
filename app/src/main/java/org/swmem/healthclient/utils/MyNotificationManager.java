package org.swmem.healthclient.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.app.NotificationCompat;

import org.swmem.healthclient.view.GraphActivity;
import org.swmem.healthclient.R;

/**
 * Created by hyunjae on 16. 8. 4.
 *
 * 알림을 관리하는 매니저
 */
public class MyNotificationManager {

    private Context context;

    public MyNotificationManager(Context context){

        this.context = context;

    }

    public void makeNotification(String title, String contents){

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.blood_drop)
                        .setContentTitle(title)
                        .setOngoing(true)
                        .setAutoCancel(true)
                        .setContentText(contents);

        if(title.equals(" Disconnected ")) {
            //Log.d("chang", "진동");
            mBuilder.setVibrate(new long[]{0,500}); // 0.5초
        }

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, GraphActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(GraphActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);


        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int id  = 0;
        mNotificationManager.notify(id, mBuilder.build());

    }



}
