package de.uni_bremen.comnets.maniac.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.uni_bremen.comnets.maniac.agents.HistoryAgent;

/**
 * Created by Isaac Supeene on 7/10/13.
 */
public class DataPathView extends LinearLayout {

    public DataPathView(Context context) {
        super(context);
    }

    public DataPathView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DataPathView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setDataPath(HistoryAgent.DataPath dataPath) {
        TextView view = (TextView)getChildAt(0);
        StringBuilder newText = new StringBuilder("Transaction ID: " + dataPath.getTransactionID() + ";  Status: ");
        if (dataPath.getStatus() == HistoryAgent.DROPPED_OR_EXPIRED) {
            newText.append("Failure");
        }
        else if (dataPath.getStatus() == HistoryAgent.SUCCESSFULLY_DELIVERED) {
            newText.append("Success");
        }
        else {
            newText.append("In Progress");
        }

        if (dataPath.getSource() != null) {
            newText.append("\nSource: " + dataPath.getSource().toString().substring(1));
        }
        else {
            newText.append("\nSource: Unknown");
        }

        if (dataPath.getDestination() != null) {
            newText.append("\nDestination: " + dataPath.getDestination().toString().substring(1));
        }
        else {
            newText.append("\nDestination: Unknown");
        }

        view.setText(newText.toString());
    }
}
