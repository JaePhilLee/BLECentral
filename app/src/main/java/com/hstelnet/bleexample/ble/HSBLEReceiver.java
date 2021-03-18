package com.hstelnet.bleexample.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hstelnet.bleexample.Global;


public class HSBLEReceiver extends BroadcastReceiver {
	private final static String LOG_TAG = HSBLEReceiver.class.getSimpleName();
	private Global global;

	public HSBLEReceiver(Global global) {
		this.global = global;

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addCategory(HSBLEService.CATEGORY_BLUETOOTH_LOW_ENERGY);
		intentFilter.addAction(HSBLEService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(HSBLEService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(HSBLEService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(HSBLEService.ACTION_SCAN_RESULT);
		intentFilter.addAction(HSBLEService.ACTION_DATA_WRITE);
		intentFilter.addAction(HSBLEService.ACTION_DATA_READ);
		intentFilter.addAction(HSBLEService.ACTION_DATA_CHANGED);
		LocalBroadcastManager.getInstance(this.global).registerReceiver(this, intentFilter);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		switch (intent.getAction()) {
			case HSBLEService.ACTION_GATT_CONNECTED:
				Log.e(LOG_TAG, "ACTION_GATT_CONNECTED");
				break;
			case HSBLEService.ACTION_GATT_DISCONNECTED:
				Log.e(LOG_TAG, "ACTION_GATT_DISCONNECTED");
				break;
			case HSBLEService.ACTION_GATT_SERVICES_DISCOVERED:
				Log.e(LOG_TAG, "ACTION_GATT_SERVICES_DISCOVERED");

				global.hsbleService.selectCharacteristicData();
				global.hsbleService.writeCharacteristic("Hello");
				break;
			case HSBLEService.ACTION_DATA_READ:
				Log.e(LOG_TAG, "ACTION_DATA_READ : From " + intent.getStringExtra(HSBLEService.EXTRA_UUID) +
						"\n\t Data : " + intent.getStringExtra(HSBLEService.EXTRA_DATA));
				break;
			case HSBLEService.ACTION_DATA_WRITE:
				Log.e(LOG_TAG, "ACTION_DATA_WRITE : From " + intent.getStringExtra(HSBLEService.EXTRA_UUID) +
						"\n\t Data : " + intent.getStringExtra(HSBLEService.EXTRA_DATA));

				global.hsbleService.close();
				break;
			case HSBLEService.ACTION_DATA_CHANGED:
				Log.e(LOG_TAG, "ACTION_DATA_CHANGED : From " + intent.getStringExtra(HSBLEService.EXTRA_UUID) +
						"\n\t Data : " + intent.getStringExtra(HSBLEService.EXTRA_DATA));
				break;
		}
	}

	public void unRegister() {
		LocalBroadcastManager.getInstance(global).unregisterReceiver(this);
	}
}
