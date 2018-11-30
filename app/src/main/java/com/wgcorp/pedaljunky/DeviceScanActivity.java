package com.wgcorp.pedaljunky;

import android.Manifest;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Activity for scanning and displaying available BLE devices.
 */
public class DeviceScanActivity extends ListActivity {

    private static final String TAG = "DeviceScanActivity";

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeScanner mBluetoothLeScanner;

    private boolean mScanning;
    private boolean mConnected;

    private Handler mHandler;

    private LeDeviceListAdapter mLeDeviceListAdapter;

    private BtleScanCallback mScanCallback;

    private Map<String, BluetoothDevice> mScanResults;

    private BluetoothGatt mGatt;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 20000;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        mLeDeviceListAdapter = new LeDeviceListAdapter(this, 2);
        this.setListAdapter(mLeDeviceListAdapter);

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();


    }

    @Override
    protected void onResume() {
        super.onResume();

        startScan();
    }

    private void startScan() {
        if (!hasPermissions() || mScanning) {
            return;
        }

        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();


        mScanResults = new HashMap<>();
        mScanCallback = new BtleScanCallback();

        // handler to stop the scan after some time
        mHandler = new Handler();
        mHandler.postDelayed(this::stopScan, SCAN_PERIOD);

        // start the scan
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
        mScanning = true;
    }

    private void stopScan() {
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            scanComplete();
        }

        mScanCallback = null;
        mScanning = false;
        mHandler = null;
    }

    private void scanComplete() {
        if (mScanResults.isEmpty()) {
            return;
        }

        for (String deviceAddress : mScanResults.keySet()) {
            Log.d(TAG, "Found device: " + deviceAddress);
        }
    }

    private boolean hasPermissions() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
            return false;
        } else if (!hasLocationPermissions()) {
            requestLocationPermission();
            return false;
        }
        return true;
    }

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        Log.d(TAG, "Requested user enables Bluetooth. Try starting the scan again");
    }

    private boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            Log.d(TAG, "coarse location permission granted");
//            this.scanLeDevice(true);
//        } else {
//            Log.i(TAG, "coarse location permission refused, unable to scan");
//        }
//
//    }

//    private void scanLeDevice(final boolean enable) {
//        if (enable) {
//            // Stops scanning after a pre-defined scan period.
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    Log.i(TAG, "Stop scanning...");
//                    mScanning = false;
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                }
//            }, SCAN_PERIOD);
//
//            Log.i(TAG, "Start scanning...");
//            mScanning = true;
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
//        } else {
//            mScanning = false;
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//        }
//    }

//    // Device scan callback.
//    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
//        @Override
//        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Log.i(TAG, "New device detected: " + device.getAddress());
////                    mLeDeviceListAdapter.addDevice(device);
////                    mLeDeviceListAdapter.notifyDataSetChanged();
//                }
//            });
//        }
//    };

    private class BtleScanCallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with code " + errorCode);
        }

        private void addScanResult(ScanResult scanResult) {
            stopScan();
            BluetoothDevice device = scanResult.getDevice();
//            String deviceAddress = device.getAddress();
//            mScanResults.put(deviceAddress, device);
            connectDevice(device);
        }
    }

    private void connectDevice(BluetoothDevice device) {
        GattClientCallback gattClientCallback = new GattClientCallback();
        mGatt = device.connectGatt(this, false, gattClientCallback);


    }

    public void disconnectGattServer() {
        mConnected = false;
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
    }

    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "# onConnectionStateChange : " + gatt.getDevice().getAddress());
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer();
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnected = true;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }

            UUID gapService = convertFromInteger(0x1800);
            UUID deviceNameCharacteristic = convertFromInteger(0x2A00);

            BluetoothGattService service = mGatt.getService(gapService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(deviceNameCharacteristic);

            mGatt.readCharacteristic(characteristic);

            Log.i(TAG, "Device Name : TODO");

//            BluetoothGattService service = gatt.getServices().get(0);
//            BluetoothGattCharacteristic characteristic = service.getCharacteristics().get(0);


//            BluetoothGattService service = gatt.getService(SERVICE_UUID);
//            BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final
        BluetoothGattCharacteristic characteristic, int status) {
            final String value = characteristic.getStringValue(0);
        }
    }

    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }
}
