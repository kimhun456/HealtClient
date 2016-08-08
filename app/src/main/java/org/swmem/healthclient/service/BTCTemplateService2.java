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
import android.content.ContentResolver;
import android.content.ContentValues;
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
import org.swmem.healthclient.Utility;
import org.swmem.healthclient.bluetooth.BleManager;
import org.swmem.healthclient.bluetooth.ConnectionInfo;
import org.swmem.healthclient.bluetooth.TransactionBuilder;
import org.swmem.healthclient.bluetooth.TransactionReceiver;
import org.swmem.healthclient.db.HealthContract;
import org.swmem.healthclient.utils.AppSettings;
import org.swmem.healthclient.utils.Constants;
import org.swmem.healthclient.utils.Logs;


public class BTCTemplateService2 extends Service {
	private static final String TAG = "BTCTemplateService2";

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
	private BluetoothDevice mDefaultDevice = null;

	@Override
	public void onCreate() {
		Logs.d(TAG, "# Service - onCreate() starts here");
		
		mContext = getApplicationContext();

		initialize();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Logs.d(TAG, "# Service - onStartCommand() starts here");
		initialize();

		// If service returns START_STICKY, android restarts service automatically after forced close.
		// At this time, onStartCommand() method in service must handle null intent.
		if(intent != null) {
			address = intent.getExtras().getString("address");
			SharedPreferences pref = getSharedPreferences("Bledata", 0);
			SharedPreferences.Editor editor = pref.edit();
			editor.putString("ADDRESS", address);
			editor.commit();
		}
		else Logs.d(TAG, " intent = null !! ");

		{
			SharedPreferences pref = getSharedPreferences("Bledata", 0);
			address = pref.getString("ADDRESS",null);

			if (address != null) {
				Logs.d(TAG, address + "연결!!");
				connectDevice(address);
			}
		}

		return Service.START_STICKY;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		Logs.d(TAG, "# Service -Configuration changed");
		// This prevents reload after configuration changes
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Logs.d(TAG, " # Service - onBind()");
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Logs.d(TAG, "# Service - onUnbind()");
		return true;
	}
	
	@Override
	public void onDestroy() {
		Logs.d(TAG, "# Service - onDestroy()");
		finalizeService();
	}
	
	@Override
	public void onLowMemory (){
		Logs.d(TAG, "# Service - onLowMemory()");
		// onDestroy is not always called when applications are finished by Android system.
		finalizeService();
	}

	
	/*****************************************************
	 *	Private methods
	 ******************************************************/
	private void initialize() {
		Logs.d(TAG, "# Service : initialize ---");
		
		AppSettings.initializeAppSettings(mContext);
		startServiceMonitoring();
		
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

		Logs.d(TAG, "# Service : finalize ---");
		
		// Stop the bluetooth session
		mBluetoothAdapter = null;
		if (mBleManager != null) {
			mBleManager.finalize();
		}
		mBleManager = null;
	}
	
	/**
	 * Setting up bluetooth connection
	 * @param h
	 */
	public void setupService(Handler h) {
		Logs.d(TAG, "# Service - Setup Service");
		mActivityHandler2 = h;
		
		// Double check BT manager instance
		if(mBleManager == null)
			setupBLE();
		
		// Initialize transaction builder & receiver
		if(mTransactionBuilder == null)
			mTransactionBuilder = new TransactionBuilder(mBleManager, mActivityHandler2);
		if(mTransactionReceiver == null)
			mTransactionReceiver = new TransactionReceiver(mActivityHandler2);
		
		// TODO: If ConnectionInfo holds previous connection info,
		// try to connect using it.
		if(mConnectionInfo.getDeviceAddress() != null && mConnectionInfo.getDeviceName() != null) {
			//connectDevice(mConnectionInfo.getDeviceAddress());
		} 
		else {
			if (mBleManager.getState() == BleManager.STATE_NONE) {
				// Do nothing
			}
		}
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
     * Check bluetooth is enabled or not.
     */
	public boolean isBluetoothEnabled() {
		if(mBluetoothAdapter==null) {
			Log.e(TAG, "# Service - cannot find bluetooth adapter. Restart app.");
			return false;
		}
		return mBluetoothAdapter.isEnabled();
	}
	
	/**
	 * Get scan mode
	 */
	public int getBluetoothScanMode() {
		int scanMode = -1;
		if(mBluetoothAdapter != null)
			scanMode = mBluetoothAdapter.getScanMode();
		
		return scanMode;
	}
	
	
    /**
     * Connect to a remote device.
     * @param address  The BluetoothDevice to connect
     */
	public void connectDevice(String address) {
		if(address != null && mBleManager != null) {
			if(mBleManager.connectGatt(mContext, true, address)) {
				BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
				mConnectionInfo.setDeviceAddress(address);
				mConnectionInfo.setDeviceName(device.getName());
			}
		}
	}

    /**
     * Connect to a remote device.
     * @param device  The BluetoothDevice to connect
     */
	public void connectDevice(BluetoothDevice device) {
		if(device != null && mBleManager != null) {
			//mBleManager.disconnect();
			
			if(mBleManager.connectGatt(mContext, true, device)) {
				mConnectionInfo.setDeviceAddress(device.getAddress());				
				mConnectionInfo.setDeviceName(device.getName());
			}
		}
	}

	/**
	 * Get connected device name
	 */
	public String getDeviceName() {
		return mConnectionInfo.getDeviceName();
	}

	/**
	 * Send message to remote device using Bluetooth
	 */
	public void sendMessageToRemote(String message) {
		sendMessageToDevice(message);
	}
	
	/**
	 * Start service monitoring. Service monitoring prevents
	 * unintended close of service.
	 */
	public void startServiceMonitoring() {
		if(AppSettings.getBgService()) {
			Logs.d(TAG, "# Start Monitoring");
			ServiceMonitoring.startMonitoring(mContext);
		} else {
			ServiceMonitoring.stopMonitoring(mContext);
		}
	}
	
	
	
	/*****************************************************
	 *	Handler, Listener, Timer, Sub classes
	 ******************************************************/
	public class ServiceBinder extends Binder {
		public BTCTemplateService2 getService() {
			return BTCTemplateService2.this;
		}
	}
	
    /**
     * Receives messages from bluetooth manager
     */
	class ServiceHandler2 extends Handler
	{
		@Override
		public void handleMessage(Message msg) {
			Logs.d(TAG, "Service");
			switch(msg.what) {
			// Bluetooth state changed
			case BleManager.MESSAGE_STATE_CHANGE:
				// Bluetooth state Changed
				Logs.d(TAG, "Service - MESSAGE_STATE_CHANGE: " + msg.arg1);

				switch (msg.arg1) {
				case BleManager.STATE_NONE:
					Logs.d(TAG, "Service None");
					break;

				case BleManager.STATE_CONNECTING:
					Logs.d(TAG, "Service Connecting");
					break;

				case BleManager.STATE_CONNECTED:
					Logs.d(TAG, "Service Connected");
					break;

				case BleManager.STATE_IDLE:
					Logs.d(TAG, "Service Idle");
					break;
				}
				break;

			// If you want to send data to remote
			case BleManager.MESSAGE_WRITE:
				Logs.d(TAG, "Service - MESSAGE_WRITE: ");
				String message = (String) msg.obj;
				if(message != null && message.length() > 0)
					sendMessageToDevice(message);
				break;

			// Received packets from remote
			case BleManager.MESSAGE_READ:
				Logs.d(TAG, "Service - MESSAGE_READ: ");

				String strMsg = (String) msg.obj;
				Logs.d("Main", address+" : "+strMsg);

				int readCount = strMsg.length();
				// send bytes in the buffer to activity
				/*
				if(strMsg != null && strMsg.length() > 0) {
					mActivityHandler2.obtainMessage(Constants.MESSAGE_READ_CHAT_DATA, strMsg)
							.sendToTarget();
					int command = mCommandParser.setString(strMsg);
					if(command == TransactionReceiver.CommandParser.COMMAND_THINGSPEAK) {
						String parameters = mCommandParser.getParameterString();
						StringBuilder requestUrl = new StringBuilder("http://184.106.153.149/update?");
						if(parameters != null && parameters.length() > 0)
							requestUrl.append(parameters);
						
						//Logs.d("# Find thingspeak command. URL = "+requestUrl);

						mCommandParser.resetParser();
					}
				}*/
				break;
				
			case BleManager.MESSAGE_DEVICE_NAME:
				Logs.d(TAG, "Service - MESSAGE_DEVICE_NAME: ");
				
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
				Logs.d(TAG, "Service - MESSAGE_TOAST: ");
				
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_TOAST), 
						Toast.LENGTH_SHORT).show();
				break;
				
			}	// End of switch(msg.what)
			
			super.handleMessage(msg);
		}
	}	// End of class MainHandler
	public void insertDummyData(double value){

		ContentValues contentValues = new ContentValues();
		contentValues.put(HealthContract.GlucoseEntry.COLUMN_TYPE, HealthContract.GlucoseEntry.BLEUTOOTH);
		contentValues.put(HealthContract.GlucoseEntry.COLUMN_TIME, Utility.getCurrentDate());
		contentValues.put(HealthContract.GlucoseEntry.COLUMN_RAW_VALUE,value);
		contentValues.put(HealthContract.GlucoseEntry.COLUMN_GLUCOSE_VALUE,value);
		contentValues.put(HealthContract.GlucoseEntry.COLUMN_TEMPERATURE_VALUE,value);
		contentValues.put(HealthContract.GlucoseEntry.COLUMN_DEVICE_ID,"123");
		ContentResolver contentResolver = getContentResolver();
		contentResolver.insert(HealthContract.GlucoseEntry.CONTENT_URI,contentValues);

	}
}
