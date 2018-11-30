package com.wgcorp.pedaljunky;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

class LeDeviceListAdapter extends ArrayAdapter {

    public LeDeviceListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public void addDevice(BluetoothDevice device) {
        add("test");
    }
}
