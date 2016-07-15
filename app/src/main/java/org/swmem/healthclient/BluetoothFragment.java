package org.swmem.healthclient;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.swmem.healthclient.data.HealthContract;


public class BluetoothFragment extends Fragment {


    TextView lastValueText;

    public BluetoothFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_bluetooth, container, false);


        lastValueText = (TextView)rootView.findViewById(R.id.lastValueText);


        ContentResolver contentResolver = getActivity().getContentResolver();
        Cursor cursor = contentResolver.query(HealthContract.InsulinEntry.CONTENT_URI,null,null,null,null);


        if (cursor != null) {
            try{

                if(cursor.moveToLast()){
                    int index = cursor.getColumnIndex(HealthContract.InsulinEntry.COLUMN_RAW_VALUE);
                    lastValueText.setText(Double.toString(cursor.getDouble(index)));
                }

            } finally {
                cursor.close();
            }

        }




        // Inflate the layout for this fragment
        return rootView;
    }


}
