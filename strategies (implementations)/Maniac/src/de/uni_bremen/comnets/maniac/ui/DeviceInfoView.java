package de.uni_bremen.comnets.maniac.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.uni_bremen.comnets.maniac.devices.Device;

/**
 * Created by Isaac Supeene on 7/10/13.
 */
public class DeviceInfoView extends LinearLayout implements Device.DeviceInfoChangedListener {
    private Device device;

    public DeviceInfoView(Context context) {
        super(context);
    }

    public DeviceInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DeviceInfoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setDevice(Device device) {
        if (this.device == device) {
            return;
        }

        if (this.device != null) {
            this.device.removeDeviceInfoListener(this);
        }

        this.device = device;
        this.device.addDeviceInfoListener(this);

        onDeviceInfoChanged();
    }

    @Override
    public void onDeviceInfoChanged() {
        ((Activity)getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("DeviceInfoView", "onDeviceInfoChanged for device " + device);
                if (device != null) {
                    // HACK!
                    TextView titleView = (TextView) getChildAt(0);
                    TextView descriptionView = (TextView) getChildAt(1);

                    if (device.getAddress().equals(device.getTopologyAgent().getOurIP())) {
                        titleView.setText(device.getAddress().toString().substring(1) + " (me)");
                    }
                    else if (device.isBackbone()) {
                        titleView.setText(device.getAddress().toString().substring(1) + " (backbone)");
                    }
                    else if (device.getAddress().equals(device.getTopologyAgent().getPartnerIP())) {
                        titleView.setText(device.getAddress().toString().substring(1) + " (partner)");
                    }
                    else {
                        titleView.setText(device.getAddress().toString().substring(1));
                    }
                    // TODO: don't show balance if it's a backbone
                    descriptionView.setText("Address: " + device.getAddress().toString().substring(1) + ";  Balance: " + device.getBalance()); // TODO: more info?
                }
            }
        });
    }
}
