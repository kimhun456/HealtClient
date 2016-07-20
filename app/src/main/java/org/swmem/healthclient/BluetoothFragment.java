package org.swmem.healthclient;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.swmem.healthclient.data.HealthContract;


public class BluetoothFragment extends Fragment {


    TextView lastValueText;

    public BluetoothFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_bluetooth, container, false);


        lastValueText = (TextView)rootView.findViewById(R.id.lastValueText);


        ContentResolver contentResolver = getActivity().getContentResolver();
        Cursor cursor = contentResolver.query(HealthContract.GlucoseEntry.CONTENT_URI,null,null,null,null);


        if (cursor != null) {
            try{

                if(cursor.moveToLast()){
                    int index = cursor.getColumnIndex(HealthContract.GlucoseEntry.COLUMN_RAW_VALUE);
                    lastValueText.setText(Double.toString(cursor.getDouble(index)));
                }

            } finally {
                cursor.close();
            }

        }

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.bluetooth, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        //noinspection SimplifiableIfStatement

        switch (id){
            case R.id.scan:

                Toast.makeText(getActivity().getBaseContext(),"scan",Toast.LENGTH_LONG).show();

                break;

        }

        return super.onOptionsItemSelected(item);
    }
}
