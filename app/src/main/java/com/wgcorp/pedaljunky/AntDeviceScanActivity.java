package com.wgcorp.pedaljunky;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.math.BigDecimal;
import java.util.EnumSet;

public class AntDeviceScanActivity extends AppCompatActivity {

    AntPlusHeartRatePcc hrPcc = null;
    protected AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc> base_IPluginAccessResultReceiver =
            new AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
                // Handle the result, connecting to events on success or reporting failure to user.
                @Override
                public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode,
                                             DeviceState initialDeviceState) {
                    TextView customText = findViewById(R.id.customTextMessage);

                    switch (resultCode) {
                        case SUCCESS:
                            hrPcc = result;

                            customText.setText(result.getDeviceName() + ": " + initialDeviceState);
                            subscribeToHrEvents();
//                            if (!result.supportsRssi()) tv_rssi.setText("N/A");
                            break;
                        case CHANNEL_NOT_AVAILABLE:
                            Toast.makeText(AntDeviceScanActivity.this, "Channel Not Available", Toast.LENGTH_SHORT).show();
                            customText.setText("Error. Do Menu->Reset.");
                            break;
                        case ADAPTER_NOT_DETECTED:
                            Toast.makeText(AntDeviceScanActivity.this, "ANT Adapter Not Available. Built-in ANT hardware or external adapter required.", Toast.LENGTH_SHORT).show();
                            customText.setText("Error. Do Menu->Reset.");
                            break;
                        case BAD_PARAMS:
                            //Note: Since we compose all the params ourself, we should never see this result
                            Toast.makeText(AntDeviceScanActivity.this, "Bad request parameters.", Toast.LENGTH_SHORT).show();
                            customText.setText("Error. Do Menu->Reset.");
                            break;
                        case OTHER_FAILURE:
                            Toast.makeText(AntDeviceScanActivity.this, "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT).show();
                            customText.setText("Error. Do Menu->Reset.");
                            break;
                        case DEPENDENCY_NOT_INSTALLED:
                            customText.setText("Error. Do Menu->Reset.");
                            AlertDialog.Builder adlgBldr = new AlertDialog.Builder(AntDeviceScanActivity.this);
                            adlgBldr.setTitle("Missing Dependency");
                            adlgBldr.setMessage("The required service\n\"" + AntPlusHeartRatePcc.getMissingDependencyName() + "\"\n was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                            adlgBldr.setCancelable(true);
                            adlgBldr.setPositiveButton("Go to Store", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent startStore = null;
                                    startStore = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + AntPlusHeartRatePcc.getMissingDependencyPackageName()));
                                    startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                    AntDeviceScanActivity.this.startActivity(startStore);
                                }
                            });
                            adlgBldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                            final AlertDialog waitDialog = adlgBldr.create();
                            waitDialog.show();
                            break;
                        case USER_CANCELLED:
                            customText.setText("Cancelled. Do Menu->Reset.");
                            break;
                        case UNRECOGNIZED:
                            Toast.makeText(AntDeviceScanActivity.this,
                                    "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                                    Toast.LENGTH_SHORT).show();
                            customText.setText("Error. Do Menu->Reset.");
                            break;
                        default:
                            Toast.makeText(AntDeviceScanActivity.this, "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                            customText.setText("Error. Do Menu->Reset.");
                            break;
                    }
                }
            };
    // Receives state changes and shows it on the status display line
    protected AntPluginPcc.IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver =
            new AntPluginPcc.IDeviceStateChangeReceiver() {
                @Override
                public void onDeviceStateChange(final DeviceState newDeviceState) {
                    runOnUiThread(() -> {
                        TextView customText = findViewById(R.id.customTextMessage);
                        customText.setText(hrPcc.getDeviceName() + ": " + newDeviceState);
                    });
                }
            };
    PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ant_device_scan);

        handleReset();
    }

    /**
     * Resets the PCC connection to request access again and clears any existing display data.
     */
    protected void handleReset() {
        // Release the old access if it exists
        if (releaseHandle != null) {
            releaseHandle.close();
        }

        requestAccessToPcc();
    }

    protected void requestAccessToPcc() {
        // starts the plugins UI search
        releaseHandle = AntPlusHeartRatePcc.requestAccess(this, this,
                base_IPluginAccessResultReceiver, base_IDeviceStateChangeReceiver);
    }

    /**
     * Switches the active view to the data display and subscribes to all the data events
     */
    public void subscribeToHrEvents() {
        hrPcc.subscribeHeartRateDataEvent(new AntPlusHeartRatePcc.IHeartRateDataReceiver() {
            @Override
            public void onNewHeartRateData(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                           final int computedHeartRate, final long heartBeatCount,
                                           final BigDecimal heartBeatEventTime, final AntPlusHeartRatePcc.DataState dataState) {
                // Mark heart rate with asterisk if zero detected
                final String textHeartRate = String.valueOf(computedHeartRate)
                        + ((AntPlusHeartRatePcc.DataState.ZERO_DETECTED.equals(dataState)) ? "*" : "");

                // Mark heart beat count and heart beat event time with asterisk if initial value
                final String textHeartBeatCount = String.valueOf(heartBeatCount)
                        + ((AntPlusHeartRatePcc.DataState.INITIAL_VALUE.equals(dataState)) ? "*" : "");
                final String textHeartBeatEventTime = String.valueOf(heartBeatEventTime)
                        + ((AntPlusHeartRatePcc.DataState.INITIAL_VALUE.equals(dataState)) ? "*" : "");

                runOnUiThread(() -> {
                    TextView hrTextView = findViewById(R.id.hrValue);
                    hrTextView.setText(textHeartRate);
                });
            }
        });
    }
}
