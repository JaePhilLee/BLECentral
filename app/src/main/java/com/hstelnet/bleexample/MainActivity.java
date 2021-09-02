package com.hstelnet.bleexample;

import android.Manifest;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.hstelnet.bleexample.ble.HSBLEService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hstelnet.bleexample.Global.bytesToHexString;
import static com.hstelnet.bleexample.ble.HSBLEService.REQUEST_CODE_PERMISSION;

/***
 * BLE Example
 * Testing device [Android S5[API 6.0]], [Android S8[API 8.0]], [Android S20 Ultra[API 11.0]]
 *
 * Manifests.xml
 * 		<uses-permission android:name="android.permission.BLUETOOTH"/>
 * 		<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
 * 		<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
 *
 */

public class MainActivity extends AppCompatActivity {
	private FloatingActionButton mFloatingBtn;
	private Global global;

	/** Bluetooth related list */
	Map<String, ScanResult> results; //<address, result>

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		global = (Global) getApplicationContext();

		/** Bluetooth related list */
		results = new HashMap<>();

		mFloatingBtn = findViewById(R.id.fab);
		mFloatingBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!HSBLEService.isSupportBluetoothLE(MainActivity.this)) {
					Snackbar.make(view, "Doesn't support BLE.", Snackbar.LENGTH_LONG)
							.setAction("Action", null).show();
					return;
				}

				if (!HSBLEService.isAvailableBluetoothLE(MainActivity.this)) {
					Snackbar.make(view, "Please enable to bluetooth.", Snackbar.LENGTH_LONG)
							.setAction("Action", null).show();
					return;
				}

				if (!HSBLEService.isAvailableLocation(MainActivity.this)) {
					Snackbar.make(view, "Please enable to location.", Snackbar.LENGTH_LONG)
							.setAction("Action", null).show();
					return;
				}

				checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/** Bluetooth related list */
	private boolean addList(ScanResult result) {
		if (results.containsKey(result.getDevice().getAddress())) {
			return false;
		}

		results.put(result.getDevice().getAddress(), result);

		return true;
	}

	private void startScan() {
		if (!global.hsbleService.startScan(new ScanCallback() {
			@Override
			public void onScanResult(int callbackType, ScanResult result) {
				super.onScanResult(callbackType, result);
				// Pattern : https://coding-factory.tistory.com/529
				// \\d{6}
//				if (result.getDevice().getName() == null)
//					return;

				String logStr = ".\n\t# # # # # " + result.getDevice().getName() + " # # # # #" + "\n" +
								"\t# Address : " + result.getDevice().getAddress() + "\n" +
								"\t# RSSI : " + result.getRssi() + "\n" +
								"\t# Alias : " + result.getDevice().getAlias() + "\n" +
								"\t# Device Name : " + result.getScanRecord().getDeviceName() + "\n";
				for (int i=0;result.getDevice().getUuids() != null && i<result.getDevice().getUuids().length;i++) {
					logStr += "\t UUID : " + result.getDevice().getUuids()[i] + "\n";
				}

				/** Bluetooth related list */
				if (addList(result)) {
					//Test Code : Advertising
//					String uuidStr = null, dataHexStr = null;
//
//					// get Advertising Data
//					Map<ParcelUuid, byte[]> map = result.getScanRecord().getServiceData();
//					for (ParcelUuid uuid : map.keySet()) {
//						logStr += "\t# " + uuid + " : " + bytesToHexString(map.get(uuid)) + "\n";
//
//						if (uuid.toString().contains("181c")) {
//							uuidStr = uuid.toString();
//							dataHexStr = bytesToHexString(map.get(uuid));
//							break;
//						}
//					}
					Log.e("onScanResult", logStr);
//
//					//Test Code : Connect
//					if (uuidStr != null && dataHexStr != null) {
//						global.hsbleService.stopScan();
//
//						global.hsbleService.connect(result.getDevice());
//					}




					//=========================
					if (result.getDevice().getName() != null && result.getDevice().getName().contains("BT")) {
						Log.e("BLE", "Find BLE : " + result.getDevice().getName());
						global.hsbleService.stopScan();
						global.hsbleService.connect(result.getDevice());
						/**     Galaxy S8 [BLE Peripheral Simulator App]
						 * 		UUID : 00002a05-0000-1000-8000-00805f9b34fb
						 *     	UUID : 00002a00-0000-1000-8000-00805f9b34fb [PROPERTY_READ]
						 *     	UUID : 00002a01-0000-1000-8000-00805f9b34fb [PROPERTY_READ]
						 *     	UUID : 00002aa6-0000-1000-8000-00805f9b34fb [PROPERTY_READ]
						 *     	UUID : 00002a37-0000-1000-8000-00805f9b34fb [PROPERTY_NOTIFY]
						 *     	UUID : 00002a38-0000-1000-8000-00805f9b34fb [PROPERTY_READ]
						 *     	UUID : 00002a39-0000-1000-8000-00805f9b34fb [PROPERTY_WRITE]
						 * */
					}
				}
			}

			@Override
			public void onBatchScanResults(List<ScanResult> results) {
				super.onBatchScanResults(results);
			}
		})) {
			Toast.makeText(this, "이미 스캔 중입니다.", Toast.LENGTH_LONG).show();
			return;
		}

		Snackbar.make(mFloatingBtn, "BLE Scanning Start...", Snackbar.LENGTH_LONG)
				.setAction("Action", null).show();
	}



	private void checkPermission(String permission) {
		int permissionStatus = ActivityCompat.checkSelfPermission(this, permission);

		if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
				ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_CODE_PERMISSION);
			} else {
				ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_CODE_PERMISSION);
//				Toast.makeText(this, "스캔하려면 필요합니다. [1]", Toast.LENGTH_LONG).show();
			}
		} else {
			startScan();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		String perms = "";
		for (int i=0;i<permissions.length;i++) perms += permissions[i] + "[" + (grantResults[i]==0?"GRANT":"DENIED") + "], ";
		Log.e("Permission", "onRequestPermissionsResult() : " + requestCode + " : " + perms);

		switch (requestCode) {
			case HSBLEService.REQUEST_CODE_PERMISSION:
				if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
	//				ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION);
					Toast.makeText(this, "스캔하려면 필요합니다. [2]", Toast.LENGTH_LONG).show();
				} else {
					startScan();
				}
				break;
			default:
				break;
		}
	}
}