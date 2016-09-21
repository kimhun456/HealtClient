/*
 * Copyright (C) 2014 Bluetooth Connection Template
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.swmem.healthclient.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.swmem.healthclient.R;
import org.swmem.healthclient.bluetooth.BleManager;
import org.swmem.healthclient.bluetooth.ConnectionInfo;
import org.swmem.healthclient.bluetooth.TransactionBuilder;
import org.swmem.healthclient.bluetooth.TransactionReceiver;
import org.swmem.healthclient.utils.AppSettings;
import org.swmem.healthclient.utils.Constants;
import org.swmem.healthclient.utils.MyNotificationManager;

import java.util.Timer;
import java.util.TimerTask;


public class BTCTemplateService extends Service {
	private static final String TAG = "BTCTemplateService";

	// Context, System
	private Context mContext = null;
	private static Handler mActivityHandler2 = null;
	private ServiceHandler2 mServiceHandler2 = new ServiceHandler2();
	private final IBinder mBinder = new ServiceBinder();
	
	// Bluetooth
	private BluetoothAdapter mBluetoothAdapter = null;		// local Bluetooth adapter managed by Android Framework
	//private BluetoothManager mBtManager = null;
	private BleManager mBleManager = null;
	private boolean mIsBleSupported = true;
	private ConnectionInfo mConnectionInfo = null;		// Remembers connection info when BT connection is made
	private TransactionReceiver.CommandParser mCommandParser = null;
	
	private TransactionBuilder mTransactionBuilder = null;
	private TransactionReceiver mTransactionReceiver = null;

	static String address = null;
	private int flag = 1, MyCnt=0;
	private byte[] MySource = new byte[1000];
	private int StartTimer = 0;
	private int write_packet1=0, write_packet2=0;

	@Override
	public void onCreate() {
		Log.d(TAG, "# Service - onCreate() starts here");
		
		mContext = getApplicationContext();

		// 블루투스 연결상태 초기화
		SharedPreferences pref = getSharedPreferences("Connstat", 0);
		SharedPreferences.Editor editor = pref.edit();
		editor.putInt("stat", 1);
		editor.apply();

		initialize();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "# Service - onStartCommand() starts here");
		initialize();

		// If service returns START_STICKY, android restarts service automatically after forced close.
		// At this time, onStartCommand() method in service must handle null intent.
		if(intent != null) {
			// Scan을 통해 얻은 Address를 받아서 사용
			address = intent.getExtras().getString("address");
			// Address를 저장
			SharedPreferences pref = getSharedPreferences("Bledata", 0);
			SharedPreferences.Editor editor = pref.edit();
			editor.putString("ADDRESS", address);
			editor.apply();
		}
		else Log.d(TAG, " intent is null");

		// Service가 재시작 됬을 때 Address를 얻어와서 시작
		SharedPreferences pref = getSharedPreferences("Bledata", 0);
		address = pref.getString("ADDRESS",null);

		if (address != null) {
			Log.d(TAG, address + "연결!!");
			connectDevice(address);
		}
		return Service.START_STICKY;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		Log.d(TAG, "# Service -Configuration changed");
		// This prevents reload after configuration changes
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, " # Service - onBind()");
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "# Service - onUnbind()");
		return true;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "# Service - onDestroy()");
		finalizeService();
	}

	@Override
	public void onLowMemory (){
		Log.d(TAG, "# Service - onLowMemory()");
		// onDestroy is not always called when applications are finished by Android system.
		finalizeService();
	}


	/*****************************************************
	 *	Private methods
	 ******************************************************/
	private void initialize() {
		Log.d(TAG, "# Service : initialize ---");

		AppSettings.initializeAppSettings(mContext);

		// Use this check to determine whether BLE is supported on the device. Then
		// you can selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
		    Toast.makeText(this, R.string.bt_ble_not_supported, Toast.LENGTH_SHORT).show();
		    mIsBleSupported = false;
		}

		// Make instances
		mConnectionInfo = ConnectionInfo.getInstance(mContext);
		mCommandParser = new TransactionReceiver.CommandParser();

		// Get local Bluetooth adapter
		if(mBluetoothAdapter == null)
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) {
			// BT is not on, need to turn on manually.
			// Activity will do this.
		} else {
			if(mBleManager == null && mIsBleSupported) {
				setupBLE();
			}
		}
	}

	/**
	 * Send message to device.
	 * @param message		message to send
	 */
	private void sendMessageToDevice(String message) {
		if(message == null || message.length() < 1)
			return;
		TransactionBuilder.Transaction transaction = mTransactionBuilder.makeTransaction();
		transaction.begin();
		transaction.setMessage(message);
		transaction.settingFinished();
		transaction.sendTransaction();
	}

	/*****************************************************
	 *	Public methods
	 ******************************************************/
	public void finalizeService() {

		Log.d(TAG, "# Service : finalize ---");

		// Stop the bluetooth session
		mBluetoothAdapter = null;
		if (mBleManager != null) {
			mBleManager.finalize();
		}
		mBleManager = null;
	}

    /**
     * Setup and initialize BLE manager
     */
	public void setupBLE() {
        Log.d(TAG, "Service - setupBLE()");

        // Initialize the BluetoothManager to perform bluetooth le scanning
        if(mBleManager == null)
        	mBleManager = BleManager.getInstance(mContext, mServiceHandler2);
    }

    /**
     * Connect to a remote device.
     * @param address  The BluetoothDevice to connect
     */
	public void connectDevice(String address) {
		// Service가 종료되도 패킷을 저장하고 로드 가능.

		if(address != null && mBleManager != null) {
			// Bluetooth 연결
			if(mBleManager.connectGatt(mContext, true, address)) {
				BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
				mConnectionInfo.setDeviceAddress(address);
				mConnectionInfo.setDeviceName(device.getName());
			}
		}
	}

	public void DetectErrorStartTimer(){ // 프로토콜이 틀렸을 시 실행.
		// 이미 켜져있음
		if(StartTimer == 1) return;
		Log.d(TAG, "# Start Timer Error");
		TimerTask SendError = new TimerTask() {
			@Override
			public void run() {
				Log.d(TAG, "# Error Send!!!");

				// 현재까지 받은 패킷을 불러와서 혈당기기에 전송
				SharedPreferences pref = getSharedPreferences("Bledata", 0);
				write_packet2 = pref.getInt("packet2",-1);
				write_packet1 = pref.getInt("packet1",-1);

				byte[] send = new byte[]{(byte) 0xff, 0x00, 0x02, 0x00, 0x00, 0x00};
				send[3] = (byte) write_packet1;
				send[4] = (byte) write_packet2;
				for (int i = 0; i < 6; i++) send[5] ^= send[i];
				mBleManager.write(null, send);

				// bluetooth disconnect!!
				BluetoothAdapter.getDefaultAdapter().disable();

				// Disconnect상태
				new MyNotificationManager(mContext).makeNotification(" Disconnected ", "Bluetooth 연결 상태를 확인해주세요."  );
				pref = getSharedPreferences("Connstat", 0);
				SharedPreferences.Editor editor = pref.edit();
				editor.putInt("stat", 1);
				editor.apply();

				if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
					BluetoothAdapter.getDefaultAdapter().enable();
				}
				if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
					BluetoothAdapter.getDefaultAdapter().enable();
				}
				if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
					BluetoothAdapter.getDefaultAdapter().enable();
				}
				if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
					BluetoothAdapter.getDefaultAdapter().enable();
				}
				if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
					BluetoothAdapter.getDefaultAdapter().enable();
				}

				StartTimer = 0;
			}
		};
		StartTimer = 1;
		Timer timer = new Timer();
		// 10초 후 실행
		timer.schedule(SendError, 10000);
	}


	/*****************************************************
	 *	Handler, Listener, Timer, Sub classes
	 ******************************************************/
	public class ServiceBinder extends Binder {
		public BTCTemplateService getService() {
			return BTCTemplateService.this;
		}
	}

    /**
     * Receives messages from bluetooth manager
     */
	class ServiceHandler2 extends Handler
	{
		@Override
		public void handleMessage(Message msg) {
			SharedPreferences pref = getSharedPreferences("Connstat", 0);
			switch(msg.what) {
			// Bluetooth state changed
			case BleManager.MESSAGE_STATE_CHANGE:
				// Bluetooth state Changed
				Log.d(TAG, "Service - MESSAGE_STATE_CHANGE: " + msg.arg1);

				switch (msg.arg1) {
				case BleManager.STATE_NONE:
					Log.d(TAG, "Service None");
					break;

				case BleManager.STATE_CONNECTING:
					Log.d(TAG, "Service Connecting");
					break;

				case BleManager.STATE_CONNECTED:
					Log.d(TAG, "Service Connected");
					// 저장 패킷 초기화 부분
					pref = getSharedPreferences("Bledata", 0);
					SharedPreferences.Editor editor = pref.edit();
					editor = pref.edit();
					editor.putInt("packet1", 0);
					editor.putInt("packet2", 0);
					editor.apply();

					// Bluetooth 연결 상태를 저장하고 얻어옴
					pref = getSharedPreferences("Connstat", 0);
					flag = pref.getInt("stat",1);
					if(flag == 1) {
						new MyNotificationManager(mContext).makeNotification(" Connected ", "Bluetooth 대기 중 입니다. (최대 1분 소요)");

						pref = getSharedPreferences("Connstat", 0);
						editor = pref.edit();
						editor.putInt("stat", 0);
						editor.apply();
					}
					break;

				case BleManager.STATE_IDLE:
					Log.d(TAG, "Service Idle");

					pref = getSharedPreferences("Bledata", 0);
					write_packet2 = pref.getInt("packet2",-1);
					write_packet1 = pref.getInt("packet1",-1);

					if(write_packet1 == 0 && write_packet2 == 0) {
						// Disconnect상태
						new MyNotificationManager(mContext).makeNotification(" Disconnected ", "Bluetooth 연결 상태를 확인해주세요.");
						pref = getSharedPreferences("Connstat", 0);
						editor = pref.edit();
						editor.putInt("stat", 1);
						editor.apply();
					}
					break;
				}
				break;

			// Received packets from remote
			case BleManager.MESSAGE_READ:
				Log.d(TAG, "Service - MESSAGE_READ: ");

				// 외주분 확인용 Write (지울 예정)
				{
					//byte[] send = new byte[]{(byte) 0xff, 0x02};
					//mBleManager.write(null, send);
				}

				// 저장된 패킷을 얻어옴
				pref = getSharedPreferences("Bledata", 0);
				write_packet2 = pref.getInt("packet2",-1);
				write_packet1 = pref.getInt("packet1",-1);
				// 패킷 로드 실패
				if(write_packet1 == -1 || write_packet2 == -1){
					Log.d(TAG, "Packet Load Error!!");
					break;
				}
			Log.d(TAG, "Now Packet1 : "+write_packet1+" Packet2 : "+write_packet2);

				byte[] data = (byte[]) msg.obj;
				// 외주분 확인용 Read (지울 예정)
				//Toast.makeText(getApplicationContext(),"READ : "+data[0], Toast.LENGTH_SHORT).show();

				// 프로토콜, 패킷 확인 과정
				if(data.length > 3 && (0xff&data[0]) == 255 && (0xff&data[1]) == write_packet2) {
					int checksum = 0;

					if (data.length != data[2] + 4) {
						Log.d(TAG, "Data Length Error!!");
						DetectErrorStartTimer();
						break;
					}
					for (int i = 0; i < data.length - 1; i++)
						checksum ^= data[i];
					if (checksum != data[data.length - 1]) {
						Log.d(TAG, "Check Sum Error!!");
						DetectErrorStartTimer();
						break;
					}
					if (data[2] == 0) { // insert!! (마지막 프로토콜 일 시)
						// insert를 하기위해 byte배열과 갯수를 넘김
						Intent intent = new Intent(getApplicationContext(), InsertService.class);
						intent.putExtra("RealData", MySource);
						intent.putExtra("RealCnt", MyCnt);
						intent.putExtra("MyType",0); // 0:Bluetooth Type
						Log.d(TAG, "RealCnt : " + MyCnt);
						startService(intent);

						// 정상 받은 상태를 write
						byte[] send = new byte[]{(byte) 0xff, 0x00, 0x02, 0x00, 0x00, 0x00};
						send[3] = (byte)write_packet1;
						send[4] = (byte)write_packet2;
						for (int i = 0; i < 5; i++) send[5] ^= send[i];
						mBleManager.write(null, send);
						Log.d(TAG, "Write!!");

						// 패킷 증가
						write_packet2++;
						if (write_packet2 == 256) {
							write_packet1++;
							write_packet2 = 0;
						}
						// 패킷 저장
						pref = getSharedPreferences("Bledata", 0);
						SharedPreferences.Editor editor = pref.edit();
						editor = pref.edit();
						editor.putInt("packet1", write_packet1);
						editor.putInt("packet2", write_packet2);
						editor.apply();
						MyCnt = 0;
						break;
					}
					// 받은 Data를 배열에 저장
					for (int i = 3; i < 3 + data[2]; i++) {
						Log.d(TAG, "Data : " + (0xff & data[i]));
						MySource[MyCnt++] = data[i];
					}
					// 패킷 증가
					write_packet2++;
					if (write_packet2 == 256) {
						write_packet1++;
						write_packet2 = 0;
					}
					// 패킷 저장
					pref = getSharedPreferences("Bledata", 0);
					SharedPreferences.Editor editor = pref.edit();
					editor = pref.edit();
					editor.putInt("packet1", write_packet1);
					editor.putInt("packet2", write_packet2);
					editor.apply();
				}else{
					Log.d(TAG, "Protocol Error!!");
							DetectErrorStartTimer();
				}
				// send bytes in the buffer to activity
				break;

			case BleManager.MESSAGE_DEVICE_NAME:
				Log.d(TAG, "Service - MESSAGE_DEVICE_NAME: ");
				
				// save connected device's name and notify using toast
				String deviceAddress = msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_ADDRESS);
				String deviceName = msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_NAME);
				
				if(deviceName != null && deviceAddress != null) {
					// Remember device's address and name
					mConnectionInfo.setDeviceAddress(deviceAddress);
					mConnectionInfo.setDeviceName(deviceName);
					
					Toast.makeText(getApplicationContext(),
							"Connected to " + deviceName, Toast.LENGTH_SHORT).show();
				}
				break;
				
			case BleManager.MESSAGE_TOAST:
				Log.d(TAG, "Service - MESSAGE_TOAST: ");
				
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_TOAST), 
						Toast.LENGTH_SHORT).show();
				break;

			}	// End of switch(msg.what)
			
			super.handleMessage(msg);
		}

	}	// End of class MainHandler

}
