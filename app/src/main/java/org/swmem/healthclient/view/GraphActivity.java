package org.swmem.healthclient.view;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.swmem.healthclient.R;
import org.swmem.healthclient.db.HealthContract;
import org.swmem.healthclient.service.BTCTemplateService;
import org.swmem.healthclient.service.InsertService;
import org.swmem.healthclient.service.ScanService;
import org.swmem.healthclient.utils.AppSettings;
import org.swmem.healthclient.utils.Constants;
import org.swmem.healthclient.utils.DeviceManager;
import org.swmem.healthclient.utils.ShareDataBaseTask;

import java.util.Timer;

public class GraphActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final java.lang.String TAG = "GraphActivity";
    private final String BLEUTOOTH_FRAGMENT_TAG = "GraphFragment";

    private ScanService mService;
    private Timer mRefreshTimer = null;
    private ActivityHandler mActivityHandler;
    private DeviceManager deviceManager;
    static  private Intent Blecon = null;

    private ImageView mImageBT = null;
    private TextView mTextStatus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //권한 설정
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION ,android.Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
        mActivityHandler = new ActivityHandler();
        AppSettings.initializeAppSettings(getApplicationContext());
        setContentView(R.layout.activity_draw);

         // Toolbar Setting
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Drawer Setting
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext());
                TextView nameText = (TextView)drawerView.findViewById(R.id.nav_user_name);
                TextView emailText = (TextView)drawerView.findViewById(R.id.nav_user_email);

                nameText.setText(sharedPreferences.getString(getString(R.string.pref_user_name_key)
                        , getString(R.string.pref_user_name_default)));
                emailText.setText(sharedPreferences.getString(getString(R.string.pref_user_email_key)
                        ,getString(R.string.pref_user_email_default)));

                deviceManager = new DeviceManager(getApplicationContext());
                sessionManage(deviceManager);
            }
        };

        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // check BlueTooth  navigator
        navigationView.getMenu().getItem(0).setChecked(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new GraphFragment(), BLEUTOOTH_FRAGMENT_TAG)
                    .commit();
        }
        doStartService();
    }


    public void sessionManage(DeviceManager deviceManager){

        // 여기부터 셋팅
        deviceManager.sync();

        Log.v(TAG,"Session Manage");


        if(deviceManager.getExist()){

            View view = findViewById(R.id.divider);
            view.setVisibility(View.VISIBLE);

            LinearLayout linearLayout1 = (LinearLayout)findViewById(R.id.device_id_layout);
            linearLayout1.setVisibility(View.VISIBLE);
            LinearLayout linearLayout2 = (LinearLayout)findViewById(R.id.remain_time_layout);
            linearLayout2.setVisibility(View.VISIBLE);
            LinearLayout linearLayout3 = (LinearLayout)findViewById(R.id.start_time_layout);
            linearLayout3.setVisibility(View.VISIBLE);

            LinearLayout batteryLayout = (LinearLayout)findViewById(R.id.battery_layout);
            batteryLayout.setVisibility(View.VISIBLE);

            TextView deviceIDtext = (TextView)findViewById(R.id.device_id);
            deviceIDtext.setText(deviceManager.getDeviceID());

            String deviceConnectTimeStr = deviceManager.formatDate(deviceManager.getDeviceConnectTime());
            TextView startTimeText = (TextView)findViewById(R.id.start_time);
            startTimeText.setText(deviceConnectTimeStr);

            String diffStr = deviceManager.getRemainTime(System.currentTimeMillis(), deviceManager.getDeviceConnectTime());
            TextView remain_time = (TextView)findViewById(R.id.remain_time);
            remain_time.setText(diffStr);

            int batteryPercent = deviceManager.getBatteryPercent();
            String batteryString = batteryPercent+"%";
            TextView batteryText = (TextView)findViewById(R.id.battery_content);
            batteryText.setText(batteryString);

            ImageView img = (ImageView)findViewById(R.id.battery_image);
            if(batteryPercent >75){
                img.setImageResource(R.drawable.battery_100);
            }else if(batteryPercent>50){
                img.setImageResource(R.drawable.battery_75);
            }else if(batteryPercent>25){
                img.setImageResource(R.drawable.battery_50);
            }else{
                img.setImageResource(R.drawable.battery_25);
            }


        }else{

            View view = findViewById(R.id.divider);
            view.setVisibility(View.GONE);
            LinearLayout linearLayout1 = (LinearLayout)findViewById(R.id.device_id_layout);
            linearLayout1.setVisibility(View.GONE);
            LinearLayout linearLayout2 = (LinearLayout)findViewById(R.id.remain_time_layout);
            linearLayout2.setVisibility(View.GONE);
            LinearLayout linearLayout3 = (LinearLayout)findViewById(R.id.start_time_layout);
            linearLayout3.setVisibility(View.GONE);

        }

    }


    @Override
    public void onStop() {
        Log.d(TAG, "# stop");
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
        // ScanService UnBind
        unbindService(mServiceConn);
        Log.d(TAG, "# Destroy");
    }

    @Override
    public void onLowMemory (){
        super.onLowMemory();
        Log.d(TAG, "# Memory");
        // onDestroy is not always called when applications are finished by Android system.
        // ScanService UnBind
        unbindService(mServiceConn);
    }


    public void insertDummies(){
        Intent intent = new Intent(getApplicationContext(),InsertService.class);
        // Random Data insert
        intent.putExtra("MyType",2);
        startService(intent);
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

            case R.id.delete_menu:
                deleteAllData();
                break;

            case R.id.discon_menu:
                // 버튼을 누를시 블루투스 연결 해제

                // Address 초기화
                SharedPreferences pref = getSharedPreferences("Bledata", 0);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("ADDRESS", null);
                editor.apply();

                // background service로 블루투스가 작동되므로, activity에서 bluetooth restart.
                // 기기에 따라 블루투스 restart시간 차이가 있어서 안전하게 3번 실행.
                BluetoothAdapter.getDefaultAdapter().disable();

                while(!BluetoothAdapter.getDefaultAdapter().isEnabled());

                if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    BluetoothAdapter.getDefaultAdapter().enable();
                }

                break;
        }

        return super.onOptionsItemSelected(item);
    }




    public void deleteAllData(){

        getApplicationContext().getContentResolver().delete(HealthContract.GlucoseEntry.CONTENT_URI, HealthContract.GlucoseEntry._ID +" >= 0",null);


    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_graph) {

        }else if (id == R.id.nav_setting) {

            Intent intent = new Intent(getBaseContext(), SettingActivity.class);
            startActivity(intent);


        }else if (id == R.id.user_setting) {

            Intent intent = new Intent(getBaseContext(), UserSettingActivity.class);
            startActivity(intent);

        }else if (id == R.id.nav_share) {

            new ShareDataBaseTask(this).execute();

        }else if (id == R.id.nav_call) {

            SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
            String doctorPhone = sharedPreferences.getString(getString(R.string.pref_doctor_phone_key), getString(R.string.pref_doctor_phone_default));

            if(doctorPhone.equals(getString(R.string.pref_doctor_phone_default))){

                Snackbar.make(getCurrentFocus(),"먼저 의사 정보를 입력하세요.",Snackbar.LENGTH_LONG).show();

            }else{
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + doctorPhone));
                startActivity(intent);
            }


        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private ServiceConnection mServiceConn = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {

            mService = ((ScanService.ServiceBinder) binder).getService();

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
       // Bluetooth Scan 과정은 activity가 필요하므로, bindService를 이용하여 Scan실행
        bindService(new Intent(this, ScanService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    }
    /**
     * Initialization / Finalization
     */
    private void initialize() {
        Log.d(TAG, "# Activity - initialize()");

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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        Log.d(TAG, "onActivityResult " + resultCode);

        switch(requestCode) {
            case Constants.REQUEST_CONNECT_DEVICE:
                // When BLEDeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(BLEDeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Attempt to connect to the device
                    if(address != null) {
                        // 새로운 Bluetooth를 연결할 때 이미 연결되어있는 Service를 찾아서 Bluetooth 해제시킴.
                        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                        am.restartPackage(getPackageName());

                        // Bluetooth 연결하는 Service로 Address를 넘기고 시작.
                        Blecon = new Intent(this, BTCTemplateService.class);
                        Blecon.putExtra("address", address);
                        startService(Blecon);
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
                    Log.e(TAG, "BT is not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
