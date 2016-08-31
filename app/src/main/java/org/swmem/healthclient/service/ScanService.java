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
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.swmem.healthclient.R;
import org.swmem.healthclient.utils.Utility;
import org.swmem.healthclient.bluetooth.BleManager;
import org.swmem.healthclient.bluetooth.ConnectionInfo;
import org.swmem.healthclient.bluetooth.TransactionBuilder;
import org.swmem.healthclient.bluetooth.TransactionReceiver;
import org.swmem.healthclient.db.HealthContract;
import org.swmem.healthclient.utils.AppSettings;
import org.swmem.healthclient.utils.Constants;
import org.swmem.healthclient.utils.Logs;


public class ScanService extends Service {
	private static final String TAG = "ScanService";

	// Context, System
	private Context mContext = null;
	private static Handler mActivityHandler = null;
	private ServiceHandler mServiceHandler = new ServiceHandler();
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



	@Override
	public void onCreate() {
		Logs.d(TAG, "# Service - onCreate() starts here");
		
		mContext = getApplicationContext();
		initialize();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Logs.d(TAG, "# Service - onStartCommand() starts here");
		
		// If service returns START_STICKY, android restarts service automatically after forced close.
		// At this time, onStartCommand() method in service must handle null intent.
		return Service.START_NOT_STICKY;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		Logs.d(TAG, "# Service -Configuration changed");
		// This prevents reload after configuration changes
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Logs.d(TAG, "# Service - onBind()");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Logs.d(TAG, "# Service - onUnbind()");
		stopSelf();
		return true;
	}
	
	@Override
	public void onDestroy() {
		Logs.d(TAG, "# Service - onDestroy()");
		finalizeService();
		mServiceHandler = null;
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
		//startServiceMonitoring();
		
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
		mActivityHandler = h;
		
		// Double check BT manager instance
		if(mBleManager == null)
			setupBLE();
		
		// Initialize transaction builder & receiver
		if(mTransactionBuilder == null)
			mTransactionBuilder = new TransactionBuilder(mBleManager, mActivityHandler);
		if(mTransactionReceiver == null)
			mTransactionReceiver = new TransactionReceiver(mActivityHandler);
		
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
        	mBleManager = BleManager.getInstance(mContext, mServiceHandler);
    }
	
    /**
     * Check bluetooth is enabled or not.
     */
	public boolean isBluetoothEnabled() {
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "# Service - cannot find bluetooth adapter. Restart app.");
			return false;
		}
		return mBluetoothAdapter.isEnabled();
	}
	
	/*****************************************************
	 *	Handler, Listener, Timer, Sub classes
	 ******************************************************/
	public class ServiceBinder extends Binder {
		public ScanService getService() {
			return ScanService.this;
		}
	}
	
    /**
     * Receives messages from bluetooth manager
     */
	class ServiceHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg) {

			super.handleMessage(msg);
		}
	}	// End of class MainHandler
}
