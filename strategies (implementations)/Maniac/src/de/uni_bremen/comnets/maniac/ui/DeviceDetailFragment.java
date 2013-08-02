package de.uni_bremen.comnets.maniac.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_bremen.comnets.maniac.R;
import de.uni_bremen.comnets.maniac.devices.Device;

/**
 * Created by Isaac Supeene on 7/10/13.
 */
public class DeviceDetailFragment extends Fragment implements Device.DeviceInfoChangedListener {

    private Device device;

    private ListView auctionListView;
    public void setAuctionListAdapter(AuctionListAdapter adapter) {
        auctionListView.setAdapter(adapter);
    }
    public AuctionListAdapter getAuctionListAdapter() {
        return auctionListView == null ? null : (AuctionListAdapter)auctionListView.getAdapter();
    }

    private ExpandableListView profileListView;
    public void setProfileListAdapter(SimpleExpandableListAdapter adapter) {
        profileListView.setAdapter(adapter);
    }
    public BaseAdapter getProfileListAdapter() {
        return profileListView == null ? null : (BaseAdapter)profileListView.getAdapter();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        auctionListView = (ListView)getActivity().findViewById(R.id.list_auction_info);

        setAuctionListAdapter(new AuctionListAdapter(getActivity(), new ArrayList<Device.AuctionInfo>()));
        refreshAuctionListAdapter();

        profileListView = (ExpandableListView)getActivity().findViewById(R.id.list_device_profile_info);

        refreshProfileListAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_detail, null);
    }

    public void setDevice(Device device) {
        if (this.device != null) {
            this.device.removeDeviceInfoListener(this);
        }
        this.device = device;
        this.device.addDeviceInfoListener(this);

        TextView titleView = (TextView)getActivity().findViewById(R.id.txt_device_detail_title);
        titleView.setText("Device " + device.getAddress().toString().substring(1));

        refreshAuctionListAdapter();
        refreshProfileListAdapter();
    }

    @Override
    public void onDeviceInfoChanged() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (getAuctionListAdapter() != null) {
                        refreshAuctionListAdapter();
                    }
                    if (getProfileListAdapter() != null) {
                        refreshProfileListAdapter();
                    }
                }
            });
        }
    }

    public class AuctionListAdapter extends ArrayAdapter<Device.AuctionInfo> {

        public AuctionListAdapter(Context context, List<Device.AuctionInfo> infoList) {
            super(context, 0, infoList);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(getActivity()).inflate(R.layout.row_auction_info, null);
            }
            assert view != null;

            ((AuctionInfoView)view).setAuctionInfo(getItem(position));

            return view;
        }
    }

    private void refreshAuctionListAdapter() {
        if (getAuctionListAdapter() != null && device != null) {
            getAuctionListAdapter().clear();
            getAuctionListAdapter().addAll(device.getAuctionHistory().values());
        }
    }

    private void refreshProfileListAdapter() {
        if (profileListView != null && device != null) { // TODO: the real thing
            List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
            Map<String, String> groupMap1 = new HashMap<String, String>();
            Map<String, String> groupMap2 = new HashMap<String, String>();
            Map<String, String> groupMap3 = new HashMap<String, String>();
            Map<String, String> groupMap4 = new HashMap<String, String>();
            groupData.add(groupMap1);
            groupData.add(groupMap2);
            groupData.add(groupMap3);
            groupData.add(groupMap4);


            groupMap1.put("Title", device.getBiddingProfile().getProfileName());
            groupMap2.put("Title", device.getAuctionProfile().getProfileName());
            groupMap3.put("Title", device.getReliabilityProfile().getProfileName());
            groupMap4.put("Title", "Standard Vulnerability Profile");
            String[] groupFrom = { "Title" };
            int[] groupTo = { R.id.group_profile };

            List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
            List<Map<String, String>> innerList1 = new ArrayList<Map<String, String>>();
            Map<String, String> childMap1 = new HashMap<String, String>();
            List<Map<String, String>> innerList2 = new ArrayList<Map<String, String>>();
            Map<String, String> childMap2 = new HashMap<String, String>();
            List<Map<String, String>> innerList3 = new ArrayList<Map<String, String>>();
            Map<String, String> childMap3 = new HashMap<String, String>();
            List<Map<String, String>> innerList4 = new ArrayList<Map<String, String>>();
            Map<String, String> childMap4 = new HashMap<String, String>();

            childMap1.put("Details", device.getBiddingProfile().getProfileDetails());
            childMap2.put("Details", device.getAuctionProfile().getProfileDetails());
            childMap3.put("Details", device.getReliabilityProfile().getProfileDetails());
            childMap4.put("Details", "Vulnerability to low-budget trick: " + device.getVulnerabilityToLowBudgetTrick() +
                                   "\nVulnerability to dead-packet trick: " + device.getVulnerabilityToDeadPacketTrick());
            innerList1.add(childMap1);
            innerList2.add(childMap2);
            innerList3.add(childMap3);
            innerList4.add(childMap4);
            childData.add(innerList1);
            childData.add(innerList2);
            childData.add(innerList3);
            childData.add(innerList4);

            String[] childFrom = { "Details" };
            int[] childTo = { R.id.child_profile };

            setProfileListAdapter(new SimpleExpandableListAdapter(getActivity(), groupData, R.layout.group_row_profile, groupFrom, groupTo,
                                                                                 childData, R.layout.child_row_profile, childFrom, childTo));
        }
    }
}
