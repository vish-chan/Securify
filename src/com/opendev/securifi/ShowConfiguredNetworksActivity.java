package com.opendev.securifi;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

public class ShowConfiguredNetworksActivity extends Activity {

	public static final String TAG = "ScanConfiguredNetworksActivity";
	ListView mLVConfiguredAPs;
	List<String> mWifiNames = new ArrayList<String>();
	List<String> mWifiDescriptions =  new ArrayList<String>();
	WifiManager mWifiManager;
	List<WifiConfiguration> mListConfNets = new ArrayList<WifiConfiguration>();
	WifiStateChangeReceiver mWifiSCReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_shownetworks);
		mLVConfiguredAPs = (ListView) findViewById(R.id.lvnetworks);
		mWifiSCReceiver = new WifiStateChangeReceiver();
		registerReceiver(mWifiSCReceiver,new IntentFilter(
				WifiManager.NETWORK_STATE_CHANGED_ACTION));
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if(mWifiManager!=null)
			mRefreshConfigNetworkList();
		
		
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mWifiSCReceiver);
		
	}
	public void mRefreshConfigNetworkList() {
		mListConfNets.clear();
		mWifiNames.clear();
		mWifiDescriptions.clear();
		mListConfNets = mWifiManager.getConfiguredNetworks();
		if(mListConfNets==null) {
			Log.e(TAG, "Wifi Manager returned null!");
			return;
		}
		for(WifiConfiguration wc:mListConfNets) {
			mWifiNames.add(wc.SSID);
			mWifiDescriptions.add(""+wc.networkId);
		}
		DoubleListAdapter mWifiAdapter = new DoubleListAdapter(getApplicationContext(), mWifiNames,
				mWifiDescriptions);
		mLVConfiguredAPs.setAdapter(mWifiAdapter);
		
	}
	
	public Boolean mCheckWifiConnectivity() {
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    	return mWifi.isConnected(); 
	}
	
	public class WifiStateChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive( Context context, Intent intent ) {
			if(mCheckWifiConnectivity())
				mRefreshConfigNetworkList();
			Log.d(TAG,"WifiStateChangeReceiver called");
		}
	}

	
}
