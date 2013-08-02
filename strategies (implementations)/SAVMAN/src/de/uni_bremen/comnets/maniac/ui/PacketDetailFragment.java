package de.uni_bremen.comnets.maniac.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.uni_bremen.comnets.maniac.R;
import de.uni_bremen.comnets.maniac.agents.HistoryAgent;

/**
 * Created by Isaac Supeene on 7/11/13.
 */
public class PacketDetailFragment extends Fragment {

    private HistoryAgent.DataPath path;

    ListView hopListView;
    public HopListAdapter getListAdapter() {
        return hopListView == null ? null : (HopListAdapter)hopListView.getAdapter();
    }
    public void setListAdapter(HopListAdapter adapter) {
        hopListView.setAdapter(adapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        hopListView = (ListView)getActivity().findViewById(R.id.list_hop_info);
        setListAdapter(new HopListAdapter(getActivity(), new ArrayList<HistoryAgent.DataPath.Hop>()));

        if (path != null) {
            getListAdapter().addAll(path.getHops());
        }
    }

    public void setPath(HistoryAgent.DataPath path) {
        this.path = path;
        if (getListAdapter() != null) {
            getListAdapter().clear();
            getListAdapter().addAll(path.getHops());
        }

        TextView titleView = (TextView)getActivity().findViewById(R.id.txt_packet_detail_title);
        titleView.setText("Data Packet " + path.getTransactionID());

        TextView subtitleView = (TextView)getActivity().findViewById(R.id.txt_packet_detail_subtitle);
        String source;
        if (path.getSource() != null) {
            source = path.getSource().toString().substring(1);
        }
        else {
            source = "Unknown";
        }
        String destination;
        if (path.getDestination() != null) {
            destination = path.getDestination().toString().substring(1);
        }
        else {
            destination = "Unknown";
        }
        String status;
        switch (path.getStatus()) {
            case HistoryAgent.SUCCESSFULLY_DELIVERED: status = "Success"; break;
            case HistoryAgent.DROPPED_OR_EXPIRED: status = "Failure"; break;
            case HistoryAgent.IN_PROGRESS: status = "In Progress"; break;
            default: status = "Unknown";
        }
        subtitleView.setText("Source: " + source + ";  " + "Destination: " + destination + ";  Status:  " + status);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_packet_detail, null);
    }

    public class HopListAdapter extends ArrayAdapter<HistoryAgent.DataPath.Hop> {

        public HopListAdapter(Context context, List<HistoryAgent.DataPath.Hop> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(getActivity()).inflate(R.layout.row_data_hop, null);
            }
            TextView hopTextView = (TextView)view.findViewById(R.id.txt_data_hop);
            HistoryAgent.DataPath.Hop hop = getItem(position);
            hopTextView.setText(hop.toString());

            return view;
        }
    }
}
