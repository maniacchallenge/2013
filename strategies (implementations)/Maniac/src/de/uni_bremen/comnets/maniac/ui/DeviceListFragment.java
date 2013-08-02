package de.uni_bremen.comnets.maniac.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import de.uni_bremen.comnets.maniac.R;
import de.uni_bremen.comnets.maniac.agents.TopologyAgent;
import de.uni_bremen.comnets.maniac.devices.Device;

/**
 * Created by Isaac Supeene on 7/10/13.
 */
public class DeviceListFragment extends Fragment implements TopologyAgent.DeviceListChangedListener {

    private ListView deviceListView;
    public void setListAdapter(DeviceListAdapter adapter) {
        deviceListView.setAdapter(adapter);
    }
    public DeviceListAdapter getListAdapter() {
        return (DeviceListAdapter)deviceListView.getAdapter();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        deviceListView = (ListView)getActivity().findViewById(R.id.list_device);

        setListAdapter(new DeviceListAdapter(getActivity(), new ArrayList<Device>()));
        ((DeviceListActivity)getActivity()).getManiac().getTopologyAgent().addDeviceChangedListener(this);
//        try { // testing
//            getListAdapter().add(new Device((Inet4Address)Inet4Address.getByName("127.0.0.1"), ((DeviceListActivity) getActivity()).getManiac().getHistoryAgent(), ((DeviceListActivity) getActivity()).getManiac().getTopologyAgent()));
//        }
//        catch (UnknownHostException ex) {
//
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_list, null);
    }

    public class DeviceListAdapter extends ArrayAdapter<Device> {
        public DeviceListAdapter(Context context,List<Device> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(getActivity()).inflate(R.layout.row_device, null);
            }
            assert view != null;

            ((DeviceInfoView)view).setDevice(getItem(position));

            return view;
        }
    }

    @Override
    public void onDeviceListChanged(final List<Device> newList) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    getListAdapter().clear();
                    getListAdapter().addAll(newList);
                }
            });
        }
    }
}
