package de.uni_bremen.comnets.maniac.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.net.Inet4Address;

import de.uni_bremen.comnets.maniac.Maniac;
import de.uni_bremen.comnets.maniac.R;
import de.uni_bremen.comnets.maniac.Strategy;

/**
 * Created by Isaac Supeene on 7/14/13.
 */
public class OptionsActivity extends Activity {
    public static final String OPTION_PARTNER_IP = "PARTNER_IP";
    public static final String OPTION_DROPPING_THRESHOLD = "DROPPING_THRESHOLD";
    public static final String OPTION_TRY_UNKNOWN_ADVERTS = "TRY_UNKNOWN_ADVERTS";
    public static final String OPTION_BID_ON_EVERYTHING = "BID_ON_EVERYTHING";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        EditText ipTextBox = (EditText)findViewById(R.id.txt_partner_ip);
        ipTextBox.setText(getSharedPreferences(Maniac.SHARED_PREFERENCES_NAME, 0).getString(OPTION_PARTNER_IP, ""));

        Button commitPartnerIPButton = (Button)findViewById(R.id.btn_commit_partner_ip);
        commitPartnerIPButton.setOnClickListener(commitPartnerIPButtonClickListener);

        EditText droppingTextBox = (EditText)findViewById(R.id.txt_dropping_threshold);
        droppingTextBox.setText(String.valueOf(getSharedPreferences(Maniac.SHARED_PREFERENCES_NAME, 0).getInt(OPTION_DROPPING_THRESHOLD, 10)));

        Button commitDroppingThresholdButton = (Button)findViewById(R.id.btn_commit_dropping_threshold);
        commitDroppingThresholdButton.setOnClickListener(commitDroppingThresholdClickListener);

        CheckBox unknownAdvertCheckBox = (CheckBox)findViewById(R.id.chk_try_unknown_adverts);
        unknownAdvertCheckBox.setChecked(getSharedPreferences(Maniac.SHARED_PREFERENCES_NAME, 0).getBoolean(OPTION_TRY_UNKNOWN_ADVERTS, true));

        Button commitTryUnknownAdvertsButton = (Button)findViewById(R.id.btn_commit_try_unknown_adverts);
        commitTryUnknownAdvertsButton.setOnClickListener(commitTryUnknownAdvertsClickListener);

        CheckBox bidOnEverythingCheckBox = (CheckBox)findViewById(R.id.chk_bid_on_everything);
        bidOnEverythingCheckBox.setChecked(getSharedPreferences(Maniac.SHARED_PREFERENCES_NAME, 0).getBoolean(OPTION_BID_ON_EVERYTHING, false));

        Button commitBidOnEverythingButton = (Button)findViewById(R.id.btn_bid_on_everything);
        commitBidOnEverythingButton.setOnClickListener(commitBidOnEverythingClickListener);
    }

    private View.OnClickListener commitPartnerIPButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            EditText ipTextBox = (EditText)findViewById(R.id.txt_partner_ip);
            if (ipTextBox.getText() == null) {
                Toast.makeText(OptionsActivity.this, "ipTextBox.getText() == null", Toast.LENGTH_SHORT).show();
                return;
            }
            String ip = ipTextBox.getText().toString();
            SharedPreferences preferences = getSharedPreferences(Maniac.SHARED_PREFERENCES_NAME, 0);
            if (!ip.equals(preferences.getString(OPTION_PARTNER_IP, ""))) {
                try {
                    getManiac().getTopologyAgent().setPartnerAddress((Inet4Address)Inet4Address.getByName(ip));
                    preferences.edit().putString(OPTION_PARTNER_IP, ip).apply();
                    Toast.makeText(OptionsActivity.this, "Successfully set partner IP: " + ipTextBox.getText().toString(), Toast.LENGTH_SHORT).show();
                }
                catch (Exception ex) {
                    Toast.makeText(OptionsActivity.this, "Invalid partner IPv4 address: " + ipTextBox.getText().toString(), Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(OptionsActivity.this, "Partner IPv4 address remaining at " + ipTextBox.getText().toString(), Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener commitDroppingThresholdClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            EditText droppingTextBox = (EditText)findViewById(R.id.txt_dropping_threshold);
            if (droppingTextBox.getText() == null) {
                Toast.makeText(OptionsActivity.this, "droppingTextBox.getText() == null", Toast.LENGTH_SHORT).show();
                return;
            }
            int threshold = 10;
            try {
                threshold = Math.max(0, Integer.parseInt(droppingTextBox.getText().toString()));
            }
            catch (NumberFormatException ex) { }
            SharedPreferences preferences = getSharedPreferences(Maniac.SHARED_PREFERENCES_NAME, 0);
            if (threshold != preferences.getInt(OPTION_DROPPING_THRESHOLD, 10)) {
                getManiac().getAuctionAgent().setDroppingThreshold(threshold);
                preferences.edit().putInt(OPTION_DROPPING_THRESHOLD, threshold).apply();
                Toast.makeText(OptionsActivity.this, "Successfully set dropping threshold: " + threshold, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(OptionsActivity.this, "Dropping threshold remaining at " + threshold, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener commitTryUnknownAdvertsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            CheckBox unknownAdvertCheckBox = (CheckBox)findViewById(R.id.chk_try_unknown_adverts);
            boolean tryUnknowns = unknownAdvertCheckBox.isChecked();
            SharedPreferences preferences = getSharedPreferences(Maniac.SHARED_PREFERENCES_NAME, 0);
            if (tryUnknowns != preferences.getBoolean(OPTION_TRY_UNKNOWN_ADVERTS, true)) {
                getManiac().getAuctionAgent().setTryUnknownAdverts(tryUnknowns);
                preferences.edit().putBoolean(OPTION_TRY_UNKNOWN_ADVERTS, tryUnknowns).apply();
                Toast.makeText(OptionsActivity.this, "Successfully set Try Unknown Adverts: " + tryUnknowns, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(OptionsActivity.this, "Try Unknown Adverts remaining at " + tryUnknowns, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener commitBidOnEverythingClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            CheckBox bidOnEverythingCheckBox = (CheckBox)findViewById(R.id.chk_bid_on_everything);
            boolean bidOnEverything = bidOnEverythingCheckBox.isChecked();
            SharedPreferences preferences = getSharedPreferences(Maniac.SHARED_PREFERENCES_NAME, 0);
            if (bidOnEverything != preferences.getBoolean(OPTION_BID_ON_EVERYTHING, false)) {
                preferences.edit().putBoolean(OPTION_BID_ON_EVERYTHING, bidOnEverything).apply();
                Toast.makeText(OptionsActivity.this, "Successfully set Bid On Everything: " + bidOnEverything, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(OptionsActivity.this, "Bid On Everything remaining at " + bidOnEverything, Toast.LENGTH_SHORT).show();
            }
        }
    };

    public Maniac getManiac() {
        return (Maniac)getApplication();
    }
}
