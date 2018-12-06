package com.wgcorp.pedaljunky;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Activity for scanning and displaying available BLE devices.
 */
public class DeviceScanActivity extends Activity {

    private static final String TAG = "DeviceScanActivity";
    public static final int GENERIC_ACCESS_SERVICE_UUID = 0x1800;
    public static final int GENERIC_ATTRIBUTE_SERVICE_UUID = 0x1801;
    public static final int CYCLING_POWER_SERVICE_UUID = 0x1818;
    public static final int DEVICE_INFORMATION_SERVICE_UUID = 0x180F;
    public static final int BATTERY_SERVICE_UUID = 0x180A;

    public static final int DEVICE_NAME_CHARACTERISTIC_UUID = 0x2A00;
    public static final int CYCLING_POWER_MEASUREMENT_CHARACTERISTIC_UUID = 0x2A63;
    public static final int CYCLING_POWER_CONTROL_POINT_CHAR_UUID = 0x2A66;

    public static final int CLIENT_CHARACTERISTIC_CONFIG_UUID = 0x2902;

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeScanner mBluetoothLeScanner;

    private RecyclerView mRecyclerView;

    private LinearLayoutManager mLayoutManager;

    private MyAdapter mAdapter;

    private boolean mScanning;

    private boolean mConnected;

    private Handler mHandler;

    private LeDeviceListAdapter mLeDeviceListAdapter;

    private BtleScanCallback mScanCallback;

    private Map<String, BluetoothDevice> mScanResults;

    private BluetoothGatt mGatt;

    // Stops scanning after n seconds.
    private static final long SCAN_PERIOD = 20000;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        mRecyclerView = findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        String[] myDataset = {"AAAAA", "BBBBB", "CCCCC"};
        mAdapter = new MyAdapter(myDataset);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

//        // Create the ArrayAdapter use the item row layout and the list data.
//        ArrayAdapter<String> listDataAdapter = new ArrayAdapter<String>(this, R.layout., R.id.listRowTextView, listData);
//
//        // Set this adapter to inner ListView object.
//        this.setListAdapter(listDataAdapter);

//        mHandler = new Handler();

//        mLeDeviceListAdapter = new LeDeviceListAdapter(this, 2);
//        this.setListAdapter(mLeDeviceListAdapter);

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();

//        startScan();
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
                mGatt.discoverServices();
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

            // device name
            BluetoothGattService genericAccessService = mGatt.getService(convertFromInteger(GENERIC_ACCESS_SERVICE_UUID));
            BluetoothGattCharacteristic deviceNameCharacteristic = genericAccessService.getCharacteristic(convertFromInteger(DEVICE_NAME_CHARACTERISTIC_UUID));
            mGatt.readCharacteristic(deviceNameCharacteristic);

            // device infos
            BluetoothGattService cyclingPowerService = mGatt.getService(convertFromInteger(CYCLING_POWER_SERVICE_UUID));
            BluetoothGattCharacteristic cyclingPowerMeasurementCharacteristic = cyclingPowerService.getCharacteristic(convertFromInteger(CYCLING_POWER_MEASUREMENT_CHARACTERISTIC_UUID));

            // enable notifications
            mGatt.setCharacteristicNotification(cyclingPowerMeasurementCharacteristic, true);
            BluetoothGattDescriptor descriptor = cyclingPowerMeasurementCharacteristic.getDescriptor(convertFromInteger(CLIENT_CHARACTERISTIC_CONFIG_UUID));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            //            descriptor.setValue(new byte[]{1, 1});
            mGatt.writeDescriptor(descriptor);

//            BluetoothGattCharacteristic cyclingPowerControlPointChar = cyclingPowerService.getCharacteristic(convertFromInteger(CYCLING_POWER_CONTROL_POINT_CHAR_UUID));

            // Need to know here the code to start data streaming
//            cyclingPowerControlPointChar.setValue(new byte[]{});
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final
        BluetoothGattCharacteristic characteristic, int status) {
            final String value = characteristic.getStringValue(0);

            Log.i(TAG, "Device Name : " + value);

            runOnUiThread(() -> {
                mAdapter.add(value, 0);
                mAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharacteristicChanged : " + characteristic.getStringValue(0));
        }
    }

    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }


}
