package org.swmem.healthclient;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class StartActivity extends AppCompatActivity {

    private final String TYPE = "type";
    private final String BLEUTOOTH = "bluetooth";
    private final String NFC = "nfc";

    private Button mBluetoothButton;
    private Button mNFCButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        TextView txt = (TextView) findViewById(R.id.title_text);
        Typeface font = Typeface.createFromAsset(this.getAssets(), "Galada.ttf");
        txt.setTypeface(font);


        mBluetoothButton = (Button) findViewById(R.id.start_bluetooth_button);
        mNFCButton = (Button) findViewById(R.id.start_nfc_button);

        mBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getBaseContext(),BluetoothActivity.class);
                intent.putExtra(TYPE, BLEUTOOTH);
                startActivity(intent);
                finish();
            }
        });

        mNFCButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getBaseContext(),BluetoothActivity.class);
                intent.putExtra(TYPE, NFC);
                startActivity(intent);
                finish();
            }
        });


    }
}
