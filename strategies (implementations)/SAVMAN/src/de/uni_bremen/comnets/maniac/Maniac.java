package de.uni_bremen.comnets.maniac;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.PowerManager;

import java.net.Inet4Address;
import java.net.SocketException;
import java.net.UnknownHostException;

import de.fu_berlin.maniac.general.Mothership;
import de.uni_bremen.comnets.maniac.agents.AuctionAgent;
import de.uni_bremen.comnets.maniac.agents.BenefitAnalysisAgent;
import de.uni_bremen.comnets.maniac.agents.HistoryAgent;
import de.uni_bremen.comnets.maniac.agents.TopologyAgent;
import de.uni_bremen.comnets.maniac.log.Tracer;
import de.uni_bremen.comnets.maniac.ui.OptionsActivity;

/**
 *
 *
 * Created by Isaac Supeene on 5/28/13
 */
public class Maniac extends Application {
    private static final String TAG = "Maniac Application";
    public static final String SHARED_PREFERENCES_NAME = "MANIAC";

    private PowerManager.WakeLock wakeLock;
	
	private Mothership mothership;
    private Strategy strategy;
    private Brain brain;

    private TopologyAgent topologyAgent;
    private HistoryAgent historyAgent;
    private AuctionAgent auctionAgent;
    private BenefitAnalysisAgent benefitAnalysisAgent;

    public TopologyAgent getTopologyAgent() {
        return topologyAgent;
    }

    public void setTopologyAgent(TopologyAgent topologyAgent) {
        this.topologyAgent = topologyAgent;
        SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_NAME, 0);
        if (preferences.contains(OptionsActivity.OPTION_PARTNER_IP)) {
            try {
                topologyAgent.setPartnerAddress((Inet4Address)Inet4Address.getByName(preferences.getString(OptionsActivity.OPTION_PARTNER_IP, "")));
            }
            catch (UnknownHostException ex) { }
        }
    }

    public HistoryAgent getHistoryAgent() {
        return historyAgent;
    }

    public void setHistoryAgent(HistoryAgent historyAgent) {
        this.historyAgent = historyAgent;
    }

    public AuctionAgent getAuctionAgent() {
        return auctionAgent;
    }

    public void setAuctionAgent(AuctionAgent auctionAgent) {
        this.auctionAgent = auctionAgent;
    }

    public BenefitAnalysisAgent getBenefitAnalysisAgent() {
        return benefitAnalysisAgent;
    }

    public void setBenefitAnalysisAgent(BenefitAnalysisAgent benefitAnalysisAgent) {
        this.benefitAnalysisAgent = benefitAnalysisAgent;
    }

    @Override
	public void onCreate() {
        Tracer t = new Tracer(TAG);

        PowerManager powerMan = (PowerManager)getSystemService(POWER_SERVICE);
        wakeLock = powerMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Maniac Wake Lock");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();

		try {
			mothership = new Mothership(this);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// Most likely retry a few times, then request human intervention.
		}
        brain = new Brain(mothership, this);
        strategy = new Strategy(brain, this);
        mothership.setStrategy(strategy);
		mothership.start();

        t.finish();
	}

    // To be called manually when the main activity is destroyed.
    public void onDestroy() {
        wakeLock.release();
        brain.interrupt();
        mothership.interrupt(); // NOTE: we slightly modified the API so we didn't have to use mothership.stop().
    }

    public Brain getBrain() {
        return brain;
    }
}
