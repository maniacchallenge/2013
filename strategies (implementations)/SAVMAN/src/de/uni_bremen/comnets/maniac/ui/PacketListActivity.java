package de.uni_bremen.comnets.maniac.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import de.uni_bremen.comnets.maniac.Maniac;
import de.uni_bremen.comnets.maniac.R;
import de.uni_bremen.comnets.maniac.agents.HistoryAgent;
import de.uni_bremen.comnets.maniac.devices.Device;

/**
 * Created by Isaac Supeene on 7/10/13.
 */
public class PacketListActivity extends Activity {
    public static final String EXTRA_SELECTED_PACKET = "SELECTED_PACKET";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packet_list);

        ListView packetListView = (ListView)findViewById(R.id.list_packet);
        packetListView.setOnItemClickListener(packetClickListener);

        ListView hopListView = (ListView)findViewById(R.id.list_hop_info);
        hopListView.setOnItemClickListener(hopClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.packet_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_device_list: switchToDeviceListActivity(); return true;
            case R.id.action_options: switchToOptionsActivity(); return true;
        }
        return false;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(EXTRA_SELECTED_PACKET)) {
            viewPacketDetail(getManiac().getHistoryAgent().getPacket(intent.getIntExtra(EXTRA_SELECTED_PACKET, 0)));
        }
    }

    private void switchToDeviceListActivity() {
        Intent intent = new Intent(this, DeviceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void switchToDeviceListActivity(Device device) {
        Intent intent = new Intent(this, DeviceListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(DeviceListActivity.EXTRA_SELECTED_DEVICE, device.getAddress().toString().substring(1));
        startActivity(intent);
    }

    private void switchToOptionsActivity() {
        Intent intent = new Intent(this, OptionsActivity.class);
        startActivity(intent);
    }

    public void viewPacketDetail(HistoryAgent.DataPath path) {
        ((PacketDetailFragment)getFragmentManager().findFragmentById(R.id.fragment_packet_detail)).setPath(path);
    }

    private AdapterView.OnItemClickListener packetClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            viewPacketDetail((HistoryAgent.DataPath)adapterView.getItemAtPosition(i));
        }
    };

    private AdapterView.OnItemClickListener hopClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView adapterView, View view, int i, long l) {
            Device d = ((HistoryAgent.DataPath.Hop)adapterView.getItemAtPosition(i)).getImportantDevice();
            if (d != null) {
                switchToDeviceListActivity(d);
            }
        }
    };

    public Maniac getManiac() {
        return (Maniac)getApplication();
    }
}