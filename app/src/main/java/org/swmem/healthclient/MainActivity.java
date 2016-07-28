package org.swmem.healthclient;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
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

import org.swmem.healthclient.db.HealthContract;
import org.swmem.healthclient.db.HealthDbHelper;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String BLEUTOOTH_FRAGMENT_TAG = "BluetoothFragment";

    private final String TYPE = "type";
    private final String BLEUTOOTH = "bluetooth";
    private final String NFC = "nfc";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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


        // type 에 따라서 Bluetooth 나 NFC Service 소환
        String type = getIntent().getStringExtra(TYPE);

        if(type.equals(BLEUTOOTH)){

            navigationView.getMenu().getItem(0).setChecked(true);



        }else if(type.equals(NFC)){

            navigationView.getMenu().getItem(1).setChecked(true);

        }


        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new BluetoothFragment(), BLEUTOOTH_FRAGMENT_TAG)
                    .commit();
        }


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
}
