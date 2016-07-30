package org.swmem.healthclient;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.swmem.healthclient.db.HealthContract;
import org.swmem.healthclient.service.BTCTemplateService;
import org.swmem.healthclient.utils.AppSettings;
import org.swmem.healthclient.utils.Constants;
import org.swmem.healthclient.utils.Logs;
import org.swmem.healthclient.utils.RecycleUtils;

import java.util.Timer;
import java.util.TimerTask;

public class BluetoothActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final java.lang.String TAG = "Main";
    private final String BLEUTOOTH_FRAGMENT_TAG = "BluetoothFragment";

    private BTCTemplateService mService;
    private Timer mRefreshTimer = null;
    private ActivityHandler mActivityHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivityHandler = new ActivityHandler();
        AppSettings.initializeAppSettings(getApplicationContext());

        setContentView(R.layout.activity_draw);

         // Toolbar Setting
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        Typeface font = Typeface.createFromAsset(this.getAssets(), "Galada.ttf");
        title.setTypeface(font);

        // Drawer Setting
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        // check BlueTooth  navigator
        navigationView.getMenu().getItem(0).setChecked(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new BluetoothFragment(), BLEUTOOTH_FRAGMENT_TAG)
                    .commit();
        }

        doStartService();
    }

    @Override
    public synchronized void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        // Stop the timer
        if(mRefreshTimer != null) {
            mRefreshTimer.cancel();
            mRefreshTimer = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        finalizeActivity();
    }

    @Override
    public void onLowMemory (){
        super.onLowMemory();
        // onDestroy is not always called when applications are finished by Android system.
        finalizeActivity();
    }

    public void insertDummyData(double value){

        ContentValues contentValues = new ContentValues();
        contentValues.put(HealthContract.GlucoseEntry.COLUMN_TYPE, HealthContract.GlucoseEntry.BLEUTOOTH);
        contentValues.put(HealthContract.GlucoseEntry.COLUMN_TIME,Utility.formatDate(Utility.getCurrentDate()));
        contentValues.put(HealthContract.GlucoseEntry.COLUMN_RAW_VALUE,value);
        contentValues.put(HealthContract.GlucoseEntry.COLUMN_GLUCOSE_VALUE,value);
        contentValues.put(HealthContract.GlucoseEntry.COLUMN_TEMPERATURE_VALUE,value);
        contentValues.put(HealthContract.GlucoseEntry.COLUMN_DEVICE_ID,"123");
        ContentResolver contentResolver = getContentResolver();
        contentResolver.insert(HealthContract.GlucoseEntry.CONTENT_URI,contentValues);

    }

    public void insertDummies(){

        long currentMilli = Utility.getCurrentDate();
        double prevValue = 92;
        ContentValues contentValues[] = new ContentValues[100];
        for(int i=0;i<100;i++){

            double rand = Math.random();
            long time =  currentMilli - 1000*60* i;
            String convertedTime = Utility.formatDate(time);

            Log.v("time : " , convertedTime);

            contentValues[i] = new ContentValues();
            if(rand < 0.5){
                contentValues[i].put(HealthContract.GlucoseEntry.COLUMN_TYPE, HealthContract.GlucoseEntry.BLEUTOOTH);
            }else{

                contentValues[i].put(HealthContract.GlucoseEntry.COLUMN_TYPE, HealthContract.GlucoseEntry.NFC);
            }

            contentValues[i].put(HealthContract.GlucoseEntry.COLUMN_TIME,convertedTime);
            contentValues[i].put(HealthContract.GlucoseEntry.COLUMN_RAW_VALUE,prevValue);
            contentValues[i].put(HealthContract.GlucoseEntry.COLUMN_GLUCOSE_VALUE,prevValue);
            contentValues[i].put(HealthContract.GlucoseEntry.COLUMN_TEMPERATURE_VALUE,prevValue);
            contentValues[i].put(HealthContract.GlucoseEntry.COLUMN_DEVICE_ID,"123");

            if(Math.random() > 0.5){

                prevValue += rand;

            }else{

                prevValue -= rand;

            }

        }

        ContentResolver contentResolver = getContentResolver();

        contentResolver.bulkInsert(HealthContract.GlucoseEntry.CONTENT_URI, contentValues);


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        switch (id){
            case R.id.plus :
                insertDummies();
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_bluetooth) {


        } else if (id == R.id.nav_info) {

        } else if (id == R.id.nav_nfc) {

        } else if (id == R.id.nav_setting) {

            Intent intent = new Intent(getBaseContext(), SettingActivity.class);
            startActivity(intent);


        } else if (id == R.id.nav_view) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private ServiceConnection mServiceConn = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {

            mService = ((BTCTemplateService.ServiceBinder) binder).getService();

            // Activity couldn't work with mService until connections are made
            // So initialize parameters and settings here. Do not initialize while running onCreate()
            initialize();
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    public class ActivityHandler extends Handler {
        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
        }
    }	// End of class ActivityHandler

    /**
     * Start service if it's not running
     */
    private void doStartService() {
        Log.d(TAG, "# Activity - doStartService()");
        startService(new Intent(this, BTCTemplateService.class));
        bindService(new Intent(this, BTCTemplateService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    }

    private void doStopService() {
        Log.d(TAG, "# Activity - doStopService()");
        mService.finalizeService();
        stopService(new Intent(this, BTCTemplateService.class));
    }

    /**
     * Initialization / Finalization
     */
    private void initialize() {
        Logs.d(TAG, "# Activity - initialize()");

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.bt_ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        mService.setupService(mActivityHandler);

        // If BT is not on, request that it be enabled.
        // RetroWatchService.setupBT() will then be called during onActivityResult
        if(!mService.isBluetoothEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
        }

        // Load activity reports and display
        if(mRefreshTimer != null) {
            mRefreshTimer.cancel();
        }

        // Use below timer if you want scheduled job
        //mRefreshTimer = new Timer();
        //mRefreshTimer.schedule(new RefreshTimerTask(), 5*1000);
    }

    private void finalizeActivity() {
        Logs.d(TAG, "# Activity - finalizeActivity()");
        if(!AppSettings.getBgService()) {
            doStopService();
        } else {

        }

        // Clean used resources
        RecycleUtils.recursiveRecycle(getWindow().getDecorView());
        System.gc();
    }

//    private void ensureDiscoverable() {
//        if (mService.getBluetoothScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
//            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//            startActivity(intent);
//        }
//    }
//
//    private class RefreshTimerTask extends TimerTask {
//        public RefreshTimerTask() {}
//
//        public void run() {
//            mActivityHandler.post(new Runnable() {
//                public void run() {
//                    // TODO:
//                    mRefreshTimer = null;
//                }
//            });
//        }
//    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        Logs.d(TAG, "onActivityResult " + resultCode);

        switch(requestCode) {
            case Constants.REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Attempt to connect to the device
                    if(address != null && mService != null) {
                        mService.connectDevice(address);
                    }
                }
                break;

            case Constants.REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a BT session
                    mService.setupBLE();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Logs.e(TAG, "BT is not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}