package com.wgcorp.pedaljunky;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "extra_message";
    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        // Check if bluetooth LOW Energy is supported
//        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
//            finish();
//        }
//
//        // Initializes Bluetooth adapter.
//        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//
//        // Ensures Bluetooth is available on the device and it is enabled. If not, displays a dialog
//        // requesting user permission to enable Bluetooth.
//        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }

        setContentView(R.layout.activity_main);
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        // Is the device BLE capable?
//        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//            finish();
//        }
//    }

    /**
     * Called when the user taps the Send button
     */
    public void sendMessage(View view) {
        Intent intent = new Intent(this, DeviceScanActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
}
