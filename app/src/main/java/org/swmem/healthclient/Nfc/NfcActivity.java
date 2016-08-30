package org.swmem.healthclient.Nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import org.swmem.healthclient.BluetoothFragment;
import org.swmem.healthclient.R;

import java.io.IOException;

/**
 * Created by Woo on 2016-08-02.
 */

public class NfcActivity extends Activity {

    static String TAG = "NfcActivity";
    public static PendingIntent pendingIntent; //intent값을 옮겨넣는다.
    NfcAdapter nfcAdapter = BluetoothFragment.nfcAdapter;

    private Tag mytag;
    private NfcvFunction myNfcvFunction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "oncreate");

        super.onCreate(savedInstanceState);

        myNfcvFunction =  new NfcvFunction(getApplicationContext());

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setContentView(R.layout.activity_nfc);

        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //현재 액티비티에서 NFC데이터 처리.
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

/*        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] mFilters = new IntentFilter[] {ndef,};
        String[][] mTechLists = new String[][] {new String[]{android.nfc.tech.NfcV.class.getName()}};


        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, mFilters, mTechLists);*/

        onNewIntent(getIntent());

    }

    public void onWindowFocusChanged(boolean hasFocus){
        Log.d(TAG, "onWindowFocusChanged");

        ImageView imgView = (ImageView) findViewById(R.id.animationImage);
        imgView.setVisibility(ImageView.VISIBLE);
        imgView.setBackgroundResource(R.drawable.nfc_tag);

        AnimationDrawable frameAnimation = (AnimationDrawable) imgView.getBackground();
        frameAnimation.start();
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        String action = intent.getAction();

        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {

            Log.d(TAG, "tag intent get");
            mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            try {

                if(myNfcvFunction.read(mytag) == false){
                    Toast.makeText(getApplicationContext(),"다시 태깅해주세요.", Toast.LENGTH_SHORT).show();
                    finish();
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
            finish();
        }
    }


    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }

    }


}
