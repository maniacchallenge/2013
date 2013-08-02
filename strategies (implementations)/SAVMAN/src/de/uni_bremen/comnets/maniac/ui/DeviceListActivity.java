package de.uni_bremen.comnets.maniac.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import de.uni_bremen.comnets.maniac.Maniac;
import de.uni_bremen.comnets.maniac.R;
import de.uni_bremen.comnets.maniac.devices.Device;

/**
 * Created by Isaac Supeene on 7/10/13.
 */
public class DeviceListActivity extends Activity {
    public static final String EXTRA_SELECTED_DEVICE = "SELECTED_DEVICE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_list);

        ListView deviceList = (ListView)findViewById(R.id.list_device);
        deviceList.setOnItemClickListener(deviceClickListener);

        ListView auctionList = (ListView)findViewById(R.id.list_auction_info);
        auctionList.setOnItemClickListener(auctionClickListener);
    }

    @Override
    protected void onDestroy() {
        getManiac().onDestroy();
        super.onDestroy();
    }

    public Maniac getManiac() {
        return (Maniac)getApplication();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(EXTRA_SELECTED_DEVICE)) {
            try {
                Device d = getManiac().getTopologyAgent().getDevice((Inet4Address)Inet4Address.getByName(intent.getStringExtra(EXTRA_SELECTED_DEVICE)));
                viewDeviceDetail(d);
            }
            catch (UnknownHostException ex) { }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_packet_list: switchToPacketListActivity(); return true;
            case R.id.action_options: switchToOptionsActivity(); return true;
        }
        return false;
    }

    private void switchToPacketListActivity() {
        Intent intent = new Intent(this, PacketListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void switchToPacketListActivity(Integer transactionID) {
        Intent intent = new Intent(this, PacketListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(PacketListActivity.EXTRA_SELECTED_PACKET, transactionID);
        startActivity(intent);
    }

    private void switchToOptionsActivity() {
        Intent intent = new Intent(this, OptionsActivity.class);
        startActivity(intent);
    }

    public void viewDeviceDetail(Device device) {
        ((DeviceDetailFragment)getFragmentManager().findFragmentById(R.id.fragment_device_detail)).setDevice(device);
    }

    private AdapterView.OnItemClickListener deviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            viewDeviceDetail((Device) adapterView.getItemAtPosition(i));
        }
    };

    private AdapterView.OnItemClickListener auctionClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            switchToPacketListActivity(((Device.AuctionInfo)adapterView.getItemAtPosition(i)).getTransactionID());
        }
    };

    public void onRadioButtonClicked(View view) {
        View historyView = findViewById(R.id.list_auction_info);
        View profileView = findViewById(R.id.list_device_profile_info);

        switch(view.getId()) {
            case R.id.rad_device_history:
                profileView.setVisibility(View.GONE);
                historyView.setVisibility(View.VISIBLE);
                break;
            case R.id.rad_device_profile:
                historyView.setVisibility(View.GONE);
                profileView.setVisibility(View.VISIBLE);
                break;
        }
    }
}