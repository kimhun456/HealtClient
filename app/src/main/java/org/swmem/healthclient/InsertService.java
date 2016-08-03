package org.swmem.healthclient;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class InsertService extends IntentService {


    public InsertService() {
        super("InsertService");
    }


    /**
     *
     *  multi Thread를 자동으로 생성하기 때문에 여기에 그냥 생성하면 된다.
     *
     * @param intent startservice()에서 전달하는 intent를 이용하여 처리시킨다.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {



        }
    }

}
