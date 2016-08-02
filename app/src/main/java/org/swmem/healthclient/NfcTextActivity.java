package org.swmem.healthclient;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;

import java.util.Arrays;

/**
 * Created by Woo on 2016-08-02.
 */
public class NfcTextActivity extends Activity {

    TextView mTextView;
    NfcAdapter nfcAdapter = BluetoothFragment.nfcAdapter;
    PendingIntent pendingIntent = NfcActivity.pendingIntent;
    // IntentFilter[] mIntentFilters; //intent값을 필터링
    //String[][]mNFCTechLists;

    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_nfc_text);
        mTextView = (TextView)findViewById(R.id.textMessage);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter == null){
            mTextView.setText("This phone is not NFC enable");
            return;
        }

        mTextView.setText("Scan a NFC tag");

        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);//현재 액티비티에서 NFC데이터 처리.
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        //데이터를 수신하기 전 Intent값을 필터링 한다.
       /* IntentFilter iFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try{
           // iFilter.addDataType("**");;
            mIntentFilters = new IntentFilter[]{iFilter};
        }catch(Exception e){
            mTextView.setText("Make IntentFilter error");
        }
        mNFCTechLists = new String[][]{new String[] {NfcF.class.getName()}};
        */
    }

    public void onResume(){
        super.onResume();
        if(nfcAdapter !=null){
            //nfcAdapter.enableForegroundDispatch(this, pendingIntent, mIntentFilters, mNFCTechLists);

            //필터링없이 모든 태그의 정보를 읽고 다음액티비티로 전송한다.
            //필터링은 발신쪽과 프로토콜 설정할경우.
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null,null);
        }

        //NDEF intent인지 확인,
        if(nfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()))
            onNewIntent(getIntent());
    }

    public void onPause(){
        super.onPause();
        if(nfcAdapter !=null){
            nfcAdapter.disableForegroundDispatch(this);
            //finish();
        }
    }

    public void onNewIntent(Intent intent){
        String action = intent.getAction();

        String tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG).toString();
        String strMsg = "first :" + action + "\n\n" + "second :"+ tag;
        mTextView.setText(strMsg);

        Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if(messages ==null) return;

        for(int i=0; i<messages.length; i++){
            showMsg((NdefMessage)messages[i]);
        }
    }

    public void showMsg(NdefMessage mMessage){
        String strMsg = "", strRec ="";
        NdefRecord[] recs = mMessage.getRecords();

        for(int i=0; i<recs.length; i++){

            NdefRecord record = recs[i];
            byte[] payload = record.getPayload();

            if(Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)){
                strRec = byteDecoding(payload);
                strRec = "Text: " + strRec;

            }
            else if(Arrays.equals(record.getType(), NdefRecord.RTD_URI)){
                strRec = new String(payload, 0, payload.length);
                strRec = "URI: " + strRec;
            }
            strMsg += ("\n\nNdefRecord[" + i + "]:\n" + strRec);
        }

        mTextView.append(strMsg);
    }

    public String byteDecoding(byte[] buf){
        String strText="";
        String textEncoding = ((buf[0]) & 0200)==0  ? "UTF-8" : "UTF-16";
        int langCodeLen = buf[0]&0077;

        try{
            strText = new String(buf, langCodeLen+1, buf.length - langCodeLen -1, textEncoding);
        }catch(Exception e){
            Log.d("tag1", e.toString());
        }
        return strText;
    }
}
