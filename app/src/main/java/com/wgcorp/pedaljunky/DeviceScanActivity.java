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
import android.view.View;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;
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
    public static final int HEARTH_RATE_SERVICE_UUID = 0x180D;

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

    private BtleScanCallback mScanCallback;

    private List<BluetoothDevice> mScanResults;

    private BluetoothGatt mGatt;

    // Stops scanning after n seconds.
    private static final long SCAN_PERIOD = 10000;

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

        mAdapter = new MyAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        configureOnClickRecyclerView();

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startScan();
    }

    // 1 - Configure item click on RecyclerView
    private void configureOnClickRecyclerView() {
        ItemClickSupport.addTo(mRecyclerView, R.layout.device_view)
                .setOnItemClickListener((recyclerView, position, v) -> {
                    BluetoothDevice device = mScanResults.get(position);
                    Log.i(TAG, "Click on device : " + device);
                    connectDevice(device);
                });
    }

    private void startScan() {
        if (!hasPermissions() || mScanning) {
            return;
        }

        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder()
//                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        // TODO implements scan filter

        mScanResults = new ArrayList<>();
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
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        if (mScanResults.isEmpty()) {
            return;
        }

        for (BluetoothDevice device : mScanResults) {
            Log.d(TAG, "Found device: " + device.getAddress() + " - " + device.getName());
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

    private void connectDevice(BluetoothDevice device) {
        Log.i(TAG, "connect device : " + device);
        GattClientCallback gattClientCallback = new GattClientCallback();
        mGatt = device.connectGatt(this, false, gattClientCallback);
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
            BluetoothDevice device = scanResult.getDevice();

            // check if not already in the list
            BluetoothDevice existingDevice = mScanResults.stream().filter(btDevice -> btDevice.getAddress().equals(device.getAddress())).findAny().orElse(null);
            if (existingDevice == null) {
                mScanResults.add(device);

                CustomDevice customDevice = new CustomDevice(device.getAddress(), device.getName());
                runOnUiThread(() -> {
                    mAdapter.add(customDevice);
                    mAdapter.notifyDataSetChanged();
                });
            }
        }
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
            Log.i(TAG, "onConnectionStateChange : " + gatt.getDevice().getAddress());
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

            // Get list of services
            for (BluetoothGattService gattService : gatt.getServices()) {
                if (convertFromInteger(GENERIC_ACCESS_SERVICE_UUID).equals(gattService.getUuid())) {
                    Log.i(TAG, "Discovered service GENERIC_ACCESS_SERVICE");
                }

                if (convertFromInteger(GENERIC_ATTRIBUTE_SERVICE_UUID).equals(gattService.getUuid())) {
                    Log.i(TAG, "Discovered service GENERIC_ATTRIBUTE_SERVICE");
                }

                if (convertFromInteger(DEVICE_INFORMATION_SERVICE_UUID).equals(gattService.getUuid())) {
                    Log.i(TAG, "Discovered service DEVICE_INFORMATION_SERVICE");
                }

                if (convertFromInteger(BATTERY_SERVICE_UUID).equals(gattService.getUuid())) {
                    Log.i(TAG, "Discovered service BATTERY_SERVICE");
                }

                if (convertFromInteger(CYCLING_POWER_SERVICE_UUID).equals(gattService.getUuid())) {
                    Log.i(TAG, "Discovered service CYCLING_POWER_SERVICE");

                    enablePowerNotifications();
                }

                if (convertFromInteger(HEARTH_RATE_SERVICE_UUID).equals(gattService.getUuid())) {
                    Log.i(TAG, "Discovered service HEARTH_RATE_SERVICE");
                }

                Log.i(TAG, "Service UUID : " + gattService.getUuid());
            }

//            // device name
//            BluetoothGattService genericAccessService = mGatt.getService(convertFromInteger(GENERIC_ACCESS_SERVICE_UUID));
//            BluetoothGattCharacteristic deviceNameCharacteristic = genericAccessService.getCharacteristic(convertFromInteger(DEVICE_NAME_CHARACTERISTIC_UUID));
//            mGatt.readCharacteristic(deviceNameCharacteristic);
        }

        private void enablePowerNotifications() {
            BluetoothGattService cyclingPowerService = mGatt.getService(convertFromInteger(CYCLING_POWER_SERVICE_UUID));
            BluetoothGattCharacteristic cyclingPowerMeasurementCharacteristic = cyclingPowerService.getCharacteristic(convertFromInteger(CYCLING_POWER_MEASUREMENT_CHARACTERISTIC_UUID));
            mGatt.setCharacteristicNotification(cyclingPowerMeasurementCharacteristic, true);
            BluetoothGattDescriptor descriptor = cyclingPowerMeasurementCharacteristic.getDescriptor(convertFromInteger(CLIENT_CHARACTERISTIC_CONFIG_UUID));

            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean res = mGatt.writeDescriptor(descriptor);
            if (res) {
                Log.d(TAG, "write descriptor success");
            } else {
                Log.d(TAG, "error when trying to write descriptor");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final
        BluetoothGattCharacteristic characteristic, int status) {
//            final String value = characteristic.getStringValue(0);

//            Log.i(TAG, "Device Name : " + value);

//            runOnUiThread(() -> {
//                mAdapter.add(value);
//                mAdapter.notifyDataSetChanged();
//            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (convertFromInteger(CYCLING_POWER_MEASUREMENT_CHARACTERISTIC_UUID).equals(characteristic.getUuid())) {
                int val = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 1);
                Log.i(TAG, "POWER : " + val);
            }
        }
    }

    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }


}
