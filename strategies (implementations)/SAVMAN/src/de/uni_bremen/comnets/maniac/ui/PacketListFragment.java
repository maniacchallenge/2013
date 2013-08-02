package de.uni_bremen.comnets.maniac.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.fu_berlin.maniac.packet_builder.BidWin;
import de.uni_bremen.comnets.maniac.R;
import de.uni_bremen.comnets.maniac.agents.HistoryAgent;

/**
 * Created by Isaac Supeene on 7/10/13.
 */
public class PacketListFragment extends Fragment implements HistoryAgent.DataPathInfoListener {

    private ListView packetListView;
    public void setListAdapter(PacketListAdapter adapter) {
        packetListView.setAdapter(adapter);
    }
    public PacketListAdapter getListAdapter() {
        return (PacketListAdapter)packetListView.getAdapter();
    }

    public class TestBidWin extends BidWin { // only for testing. TODO: remove
        protected TestBidWin(int transactionID, Inet4Address winnerIP, int winningBid, int fine) {
            super(transactionID, winnerIP, winningBid, fine);
        }
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        packetListView = (ListView)getActivity().findViewById(R.id.list_packet);

        setListAdapter(new PacketListAdapter(getActivity(), new ArrayList<HistoryAgent.DataPath>()));
        ((PacketListActivity)getActivity()).getManiac().getHistoryAgent().addPathInfoListener(this);

//        try { // TESTING
//            HistoryAgent agent = ((PacketListActivity) getActivity()).getManiac().getHistoryAgent();
//            HistoryAgent.DataPath testPath = agent.new DataPath(0);
//            testPath.newHop(new TestBidWin(0, (Inet4Address)Inet4Address.getByName("127.0.0.1"), 120, 100));
//            getListAdapter().add(testPath); // Testing
//        }
//        catch (UnknownHostException ex) {
//
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_packet_list, null);
    }

    @Override
    public void onDetach() {
        Log.e("PacketListFragment", "PacketListFragment was detached from the PacketListActivity!!!");
        super.onDetach();
        new RuntimeException("PacketListFragment was detached from the PacketListActivity!!!").printStackTrace();
    }

    @Override
    public void onNewPathInfo(final List<HistoryAgent.DataPath> dataPaths) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getListAdapter().clear();
                    getListAdapter().addAll(dataPaths);
                    getListAdapter().sort(new Comparator<HistoryAgent.DataPath>() {
                        @Override
                        public int compare(HistoryAgent.DataPath dataPath, HistoryAgent.DataPath dataPath2) {
                            return dataPath.getTransactionID() - dataPath2.getTransactionID();
                        }
                    });
                }
            });
        }
    }

    public class PacketListAdapter extends ArrayAdapter<HistoryAgent.DataPath> {

        public PacketListAdapter(Context context, List<HistoryAgent.DataPath> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(getActivity()).inflate(R.layout.row_data_path, null);
            }
            assert view != null;

            ((DataPathView)view).setDataPath(getItem(position));

            return view;
        }
    }
}
