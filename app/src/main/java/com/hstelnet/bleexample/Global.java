package com.hstelnet.bleexample;

import android.app.Application;
import android.bluetooth.le.ScanSettings;

import com.hstelnet.bleexample.ble.HSBLEService;
import com.hstelnet.bleexample.ble.HSBLEReceiver;

public class Global extends Application {
	private static final String LOG_TAG = Global.class.getSimpleName();

	public HSBLEService hsbleService;
	private HSBLEReceiver hsbleReceiver;

	@Override
	public void onCreate() {
		super.onCreate();

		hsbleService = new HSBLEService(this);
		hsbleService.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);

		hsbleReceiver = new HSBLEReceiver(this);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();

		hsbleReceiver.unRegister();
	}

	public static String bytesToHexString(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		for (byte aByte : bytes) {
			result.append(String.format("%02x", aByte & 0xFF));
		}

		return result.toString();
	}

//	public void startBluetoothLEService() {
//		if (HSBLEService.serviceIntent == null) {
//			Intent intent = new Intent(this, HSBLEService.class);
//			intent.setAction(HSBLEService.HS_ACTION_START_SERVICE);
//			startService(intent);
//		}
//	}
//
//	public void stopBluetoothLEService() {
//		if (HSBLEService.serviceIntent != null) {
//			Intent intent = new Intent(this, HSBLEService.class);
//			intent.setAction(HSBLEService.HS_ACTION_STOP_SERVICE);
//			startService(intent);
//		}
//	}
}
