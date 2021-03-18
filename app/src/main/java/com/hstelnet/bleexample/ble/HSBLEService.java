package com.hstelnet.bleexample.ble;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hstelnet.bleexample.Global;
import com.hstelnet.bleexample.MainActivity;
import com.hstelnet.bleexample.R;

import java.util.List;
import java.util.Map;

import static com.hstelnet.bleexample.Global.bytesToHexString;

/**
 * This example is for Central(Scan, Looking for Advertisement) & GATT Client(Communication) of BLE
 *
 * GATT(Generic Attribute Profile): GATT는 두 BLE장치 간에 Service, Characteristic을 이용해서 데이터를 주고받는 방법을 정의한 것. (Like Communication Protocol)
 * ATT(Attribute Protocol): GATT는 ATT의 최상위 구현체이며, GATT/ATTT로 참조되기도 한다. 각각의 속성(Attribute)은 UUID를 가지며, 128bit로 구성된다. ATT에 의해 부여된 속성은 특성(Characteristic)과 서비스(Service)를 결정한다.
 * Characteristic: 하나의 특성(Characteristic)은 하나의 Value와 n개의 Descriptor를 포함한다.
 * Descriptor: Descriptor는 특성의 값을 기술한다.
 * Service: 하나의 서비스는 특성들의 집합이다. 예를 들어 "Heart Rate Monitor"라 불리는 서비스를 가지고 있다면 그 서비스는 "Heart Rate Measurement"같은 특성을 포함한다.
 * */

public class HSBLEService extends Service {
	private static final String LOG_TAG = HSBLEService.class.getSimpleName();
	public final static int REQUEST_CODE_PERMISSION = 100; //BLE Permission Code
	private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.

	/** For Scanning */
	private static Global global;
	private static BluetoothManager mBleManager;
	private static BluetoothAdapter mBleAdapter;
	private static BluetoothLeScanner mBleScanner;
	private static ScanSettings mScanSettings;
	private static boolean mScanning;
	private static ScanCallback mScanCallback;
	private static BluetoothGattCharacteristic writeCharacteristic, readCharacteristic, notifyCharacteristic;

	/** For Communication (Connect/Disconnect) */
	private static BluetoothGatt mBluetoothGatt;
	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;

	public final static String CATEGORY_BLUETOOTH_LOW_ENERGY = "com.example.bluetooth.le.CATEGORY_BLUETOOTH_LOW_ENERGY";
	public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_READ = "com.example.bluetooth.le.ACTION_DATA_READ";
	public final static String ACTION_DATA_WRITE = "com.example.bluetooth.le.ACTION_DATA_WRITE";
	public final static String ACTION_DATA_CHANGED = "com.example.bluetooth.le.ACTION_DATA_CHANGED";
	public final static String ACTION_SCAN_RESULT = "com.example.bluetooth.le.ACTION_SCAN_RESULT";
	public final static String EXTRA_UUID = "com.example.bluetooth.le.EXTRA_UUID";
	public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

	/** UUIDs [write, read, setNotification] */
	private final String UUID_DATA_NOTIFY = "0000fff1-0000-1000-80000-00805f9b34fb";
	private final String UUID_DATA_WRITE = "0000fff1-0000-1000-80000-00805f9b34fb";
	private final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-80000-00805f9b34fb";

	public HSBLEService(Global global) {
		this.global = global;

		mBleManager = (BluetoothManager) global.getSystemService(Context.BLUETOOTH_SERVICE);
		mBleAdapter = mBleManager.getAdapter();
		mBleScanner = mBleAdapter.getBluetoothLeScanner();

		mScanning = false;
	}

	public static boolean isSupportBluetoothLE(Context context) {
		// Use this check to determine whether BLE is supported on the device.
		// Then you can selectively disable BLE-related features.
		return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
	}

	public static boolean isAvailableBluetoothLE(Context context) {
		return ((BluetoothManager) context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter() != null &&
				((BluetoothManager) context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().isEnabled();
	}

	public static boolean isAvailableLocation(Context context) {
		LocationManager locationManager = (LocationManager) context.getApplicationContext().getSystemService(LOCATION_SERVICE);

		return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	/**
	 * @param mode: ScanSettings.SCAN_MODE_OPPORTUNISTIC,
	 *              ScanSettings.SCAN_MODE_LOW_POWER,
	 *              ScanSettings.SCAN_MODE_LOW_LATENCY,
	 *              ScanSettings.SCAN_MODE_BALANCED
	 */
	public void setScanMode(int mode) {
		mScanSettings = new ScanSettings.Builder().setScanMode(mode).build();
	}

	public boolean startScan(ScanCallback scanCallback) {
		if (mScanning || scanCallback == null)
			return false;

		mScanning = true;

		// Stops scanning after a pre-defined scan period.
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
			Log.e(LOG_TAG, "End Scanning...");
			mScanning = false;
			mBleScanner.stopScan(scanCallback);
		}, SCAN_PERIOD);


		mScanCallback = scanCallback;
		new Thread(() -> {
			Log.e(LOG_TAG, "Start Scan()");

			if (mScanSettings == null)
				mBleScanner.startScan(mScanCallback);
			else
				mBleScanner.startScan(null, mScanSettings, mScanCallback);
		}).start();

		return true;
	}

	public void stopScan() {
		if (!mScanning) return;

		mScanning = false;
		new Thread(() -> {
			Log.e(LOG_TAG, "Stop Scan()");

			mBleScanner.stopScan(mScanCallback);
		}).start();
	}

	public void connect(BluetoothDevice device) {
		if (mBluetoothGatt != null) {
			close();
		}

		Log.e(LOG_TAG, "Connect : " + device.getAddress());

		mBluetoothGatt = device.connectGatt(global, false, gattCallback);
	}

	public void close() {
		if (mBluetoothGatt == null)
			return;

		Log.e(LOG_TAG, "close : " + mBluetoothGatt.getDevice().getAddress());

		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	public void selectCharacteristicData() {
		Log.e(LOG_TAG, "# # # # # # # selectCharacteristicData() # # # # # # #\n");
		String logStr = "";
		for (BluetoothGattService service : getSupportedGattServices()) {
			for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
				/**
				 * Broadcast 					: 0x01
				 * Read 						: 0x02
				 * Write without Response 		: 0x04
				 * Write 						: 0x08
				 * Indicate 					: 0x20
				 * Authenticated Signed Writes 	: 0x40
				 * Extended Properties 			: 0x80
				 * */
				Log.e(LOG_TAG, "Property : " + characteristic.getProperties());
				logStr += "\tUUID : " + characteristic.getUuid();

				int charaProp = characteristic.getProperties();
				if (((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) |
					(charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
					logStr += " [PROPERTY_WRITE]";
					writeCharacteristic = characteristic;
				} else if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
					logStr += " [PROPERTY_READ]";
					readCharacteristic = characteristic;
				} else if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
					logStr += " [PROPERTY_NOTIFY]";
					notifyCharacteristic = characteristic;
					setCharacteristicNotification(characteristic, true);
				}

				logStr += "\n";
			}
		}

		Log.e(LOG_TAG, "UUID : " + logStr);
	}

	public void writeCharacteristic(String data) {
		if (writeCharacteristic != null) {
			writeCharacteristic.setValue(data);
			writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
			mBluetoothGatt.writeCharacteristic(writeCharacteristic);
		}
	}

	public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
		mBluetoothGatt.setCharacteristicNotification(characteristic, enable);
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(characteristic.getUuid());
		if (descriptor != null) {
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		}
	}

	// send to Application Context
	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent();
		intent.addCategory(CATEGORY_BLUETOOTH_LOW_ENERGY);
		intent.setAction(action);

		LocalBroadcastManager.getInstance(global).sendBroadcast(intent);
	}
	private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent();
		intent.addCategory(CATEGORY_BLUETOOTH_LOW_ENERGY);
		intent.setAction(action);
		// This is special handling for the Heart Rate Measurement profile. Data
		// parsing is carried out as per profile specifications.
//		if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
//			int flag = characteristic.getProperties();
//			int format = -1;
//			if ((flag & 0x01) != 0) {
//				format = BluetoothGattCharacteristic.FORMAT_UINT16;
//				Log.d(LOG_TAG, "Heart rate format UINT16.");
//			} else {
//				format = BluetoothGattCharacteristic.FORMAT_UINT8;
//				Log.d(LOG_TAG, "Heart rate format UINT8.");
//			}
//			final int heartRate = characteristic.getIntValue(format, 1);
//			Log.d(LOG_TAG, String.format("Received heart rate: %d", heartRate));
//			intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
//		} else {
//			// For all other profiles, writes the data formatted in HEX.
//			final byte[] data = characteristic.getValue();
//			if (data != null && data.length > 0) {
//				final StringBuilder stringBuilder = new StringBuilder(data.length);
//				for(byte byteChar : data)
//					stringBuilder.append(String.format("%02X ", byteChar));
//				intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
//						stringBuilder.toString());
//			}
//		}

		intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());

		final byte[] data = characteristic.getValue();
		if (data != null && data.length > 0) {
			final StringBuilder stringBuilder = new StringBuilder(data.length);
			for(byte byteChar : data)
				stringBuilder.append(String.format("%02X ", byteChar));
			intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
					stringBuilder.toString());
		}

		LocalBroadcastManager.getInstance(global).sendBroadcast(intent);
	}

	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
											int newState) {
			String intentAction;
			if (newState == STATE_CONNECTED) {
				intentAction = ACTION_GATT_CONNECTED;
				broadcastUpdate(intentAction);
				Log.i(LOG_TAG, "Connected to GATT server.");
				Log.i(LOG_TAG, "Attempting to start service discovery:" +
						mBluetoothGatt.discoverServices());

			} else if (newState == STATE_DISCONNECTED) {
				intentAction = ACTION_GATT_DISCONNECTED;
				Log.i(LOG_TAG, "Disconnected from GATT server.");
				broadcastUpdate(intentAction);
			}
		}

		@Override
		// New services discovered
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
			} else {
				Log.w(LOG_TAG, "onServicesDiscovered received: " + status);
			}
		}

		@Override
		// Result of a characteristic read operation
		public void onCharacteristicRead(BluetoothGatt gatt,
										 BluetoothGattCharacteristic characteristic,
										 int status) {
			Log.e(LOG_TAG, "onCharacteristicRead()[" + status + "] : " + Global.bytesToHexString(characteristic.getValue()));
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_READ, characteristic);
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicWrite(gatt, characteristic, status);

			Log.e(LOG_TAG, "onCharacteristicWrite()[" + status + "] : " + Global.bytesToHexString(characteristic.getValue()));
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_WRITE, characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicChanged(gatt, characteristic);

			Log.e(LOG_TAG, "onCharacteristicChanged() : " + Global.bytesToHexString(characteristic.getValue()));
			broadcastUpdate(ACTION_DATA_CHANGED, characteristic);
		}
	};

	/**
	 * Retrieves a list of supported GATT services on the connected device. This should be
	 * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
	 *
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null) return null;

		return mBluetoothGatt.getServices();
	}


	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		HSBLEService getService() {
			return HSBLEService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that BluetoothGatt.close() is called
		// such that resources are cleaned up properly.  In this particular example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
		return super.onUnbind(intent);
	}

}
