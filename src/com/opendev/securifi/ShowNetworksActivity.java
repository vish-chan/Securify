package com.opendev.securifi;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ListView;

public class ShowNetworksActivity extends Activity {

	public static final String TAG = "ScanNetworksActivity";
	ListView mLVAvailableAPs;
	List<String> mWifiNames = new ArrayList<String>();
	List<String> mWifiDescriptions =  new ArrayList<String>();
	WifiScanReceiver mWifiReciever;
	WifiManager mWifiManager;
	Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_shownetworks);
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		mLVAvailableAPs = (ListView) findViewById(R.id.lvnetworks);
		mWifiReciever = new WifiScanReceiver();
		registerReceiver(mWifiReciever, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		mWifiManager.startScan();
		mScanWifiAPs();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mWifiReciever);
	}

	public void mScanWifiAPs() {
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (!mWifiManager.startScan()) {
					DoubleListAdapter mWifiAdapter = new DoubleListAdapter(
							getApplicationContext(), new ArrayList<String>(0),
													new ArrayList<String>(0));
					mLVAvailableAPs.setAdapter(mWifiAdapter);
				}
				mScanWifiAPs();
			}
		}, 4000);
	}

	public class WifiScanReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context c, Intent intent) {
			List<ScanResult> wifiScanList = mWifiManager.getScanResults();
			mWifiNames = new ArrayList<String>(wifiScanList.size());
			mWifiDescriptions =  new ArrayList<String>(wifiScanList.size());
			for (int i = 0; i < wifiScanList.size(); i++) {
				mWifiNames.add(wifiScanList.get(i).SSID);
				mWifiDescriptions.add(wifiScanList.get(i).capabilities);
			}
			DoubleListAdapter mWifiAdapter = new DoubleListAdapter(c, mWifiNames,
					mWifiDescriptions);
			mLVAvailableAPs.setAdapter(mWifiAdapter);
			Log.d(TAG,"Scan result broadcast received successfully");
		}
	}
}
