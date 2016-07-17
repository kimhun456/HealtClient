package org.swmem.healthclient;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.swmem.healthclient.data.HealthContract;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String BLEUTOOTH_FRAGMENT_TAG = "BluetoothFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new BluetoothFragment(), BLEUTOOTH_FRAGMENT_TAG)
                    .commit();
        }


    }

    public void insertDummyData(double value){

        Time time = new Time();   time.setToNow();
        Log.d("TIME TEST", Long.toString(time.toMillis(false)));

        ContentValues contentValues = new ContentValues();
        contentValues.put(HealthContract.InsulinEntry.COLUMN_TYPE,HealthContract.InsulinEntry.BLEUTOOTH);
        contentValues.put(HealthContract.InsulinEntry.COLUMN_TIME,time.toMillis(false));
        contentValues.put(HealthContract.InsulinEntry.COLUMN_RAW_VALUE,value);
        contentValues.put(HealthContract.InsulinEntry.COLUMN_GLUCOSE_VALUE,value);
        contentValues.put(HealthContract.InsulinEntry.COLUMN_TEMPERATURE_VALUE,value);
        contentValues.put(HealthContract.InsulinEntry.COLUMN_DEVICE_ID,"123");
        ContentResolver contentResolver = getContentResolver();
        contentResolver.insert(HealthContract.InsulinEntry.CONTENT_URI,contentValues);
        Cursor cursor = getContentResolver().query(HealthContract.InsulinEntry.CONTENT_URI,null,null,null,null);

        if(cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    int index = cursor.getColumnIndex(HealthContract.InsulinEntry.COLUMN_RAW_VALUE);
                    Log.e("hi", Integer.toString(cursor.getInt(index)));
                }
            } finally {
                cursor.close();
            }
        }


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


        //noinspection SimplifiableIfStatement

        switch (id){
            case R.id.bluetooth_menu:

                insertDummyData(11.11);
                Snackbar.make(getCurrentFocus()," Bluetooth icon selected",Snackbar.LENGTH_SHORT).show();
                break;
            case R.id.nfc_menu:

                Snackbar.make(getCurrentFocus()," NFC icon selected",Snackbar.LENGTH_SHORT).show();
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
            // Handle the camera action
        } else if (id == R.id.nav_info) {

        } else if (id == R.id.nav_nfc) {

        } else if (id == R.id.nav_setting) {

        } else if (id == R.id.nav_view) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
