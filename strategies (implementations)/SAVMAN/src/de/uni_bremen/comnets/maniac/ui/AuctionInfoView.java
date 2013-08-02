package de.uni_bremen.comnets.maniac.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.uni_bremen.comnets.maniac.devices.Device;

/**
 * Created by Isaac Supeene on 7/10/13.
 */
public class AuctionInfoView extends LinearLayout {

    public AuctionInfoView(Context context) {
        super(context);
    }

    public AuctionInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AuctionInfoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAuctionInfo(Device.AuctionInfo auctionInfo) {
        // HACK!
        TextView view = (TextView)getChildAt(0);
        StringBuffer newText = new StringBuffer("TransactionID: " + auctionInfo.getTransactionID() + ";  Bid: " + auctionInfo.getBidAmount());
        if (auctionInfo.isSuccessfulBid() != null) {
            newText.append("\nDevice acquired package successfully.");
        }
        view.setText(newText.toString());
        // TODO: Some more useful stuff.
    }
}
