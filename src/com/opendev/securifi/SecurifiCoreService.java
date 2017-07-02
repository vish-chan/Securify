package com.opendev.securifi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class SecurifiCoreService extends Service{

	protected static final String TAG = "SecurifiService";
	protected static final double DIST_THRESHOLD = 300.0d;
	public static String WIFILISTFILE = "SecurifiWifiList.txt";
	public static String WHITELISTFILE = "SecurifiWhiteList.txt";
	public static final Integer LOCATION_TRUSTED_STATE = 3;
	public static final Integer LOCATION_UNTRUSTED_STATE = 4;
	static Boolean mInSafeLocation = false;
	List<Double> mSafeLatitudesList = new ArrayList<Double>();
	List<Double> mSafeLongitudesList = new ArrayList<Double>();
	List<String> mSafeLocationNameList = new ArrayList<String>();
	List<String> mAlreadyNotifiedSSIDs = new ArrayList<String>();
	static String mCurentLocation = "Unknown";
	Integer mCurentLocationStatus = LOCATION_UNTRUSTED_STATE;
	Integer mPreviousLocationStatus = LOCATION_UNTRUSTED_STATE;
	WifiScanReceiverForService mWifiScanReceiver;
	WifiStateChangeReceiver mWifiSCReceiver;
	WifiManager mWifiManager;
	GeoInfoServiceHelper mGeoInfoHelper;
	NotificationManager mNotificationManager;
	List<WifiConfiguration> mConfiguredNetworks = new ArrayList<WifiConfiguration>();
	List<ScanResult> mScanResult = new ArrayList<ScanResult>();
	SharedPreferences mSharedPrefs;
	Boolean mWriteToFlag = true, mCheckMaliciousFlag = false, mAsyncTaskRunning=false ;
	String mCommand  = "iwpriv wlan0 setrandMACFlag ";
	Integer mOnParam = 1, mOffParam = 0, mRandServiceType = 1,mCheckMaliciousInterval = -1;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		String extstorage =  Environment.getExternalStorageDirectory().getPath();
		WIFILISTFILE = extstorage.concat("/"+WIFILISTFILE);
		WHITELISTFILE = extstorage.concat("/"+WHITELISTFILE);
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	    mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	    mSharedPrefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
	    mCheckMaliciousFlag = mSharedPrefs.getBoolean("MALICIOUS_FLAG", false);
	    SetUpRandomizationOptions();
	    InitWhiteListDataStructures();
	    mGeoInfoHelper = new GeoInfoServiceHelper(getApplicationContext(),this);
		if(!mGeoInfoHelper.canGetLocation)
			Toast.makeText(getApplicationContext(),
						"Please enable location services for proper functioning!",
						Toast.LENGTH_LONG).show();
		mGeoInfoHelper.Update();
		mWifiScanReceiver = new WifiScanReceiverForService();
		mWifiSCReceiver = new WifiStateChangeReceiver();
		__RegisterWifiReceivers();
		this.mForceCheckForMaliciousAP();
	}
	public void SetUpRandomizationOptions() {
		int opt = mSharedPrefs.getInt("RAND_OPTS", 2);
		switch(opt){
    	case 1:
    		mOnParam = 1; mOffParam = 1;
    		mRandServiceType = 1;
    		break;
    	case 2:
    		mOnParam = 1; mOffParam = 0;
    		mRandServiceType = 2;
    		break;
    	case 3:
    		mOnParam = 0; mOffParam = 0;
    		mRandServiceType = 3;
    		break;
    	}
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	      Toast.makeText(this, "Securifi service started", Toast.LENGTH_LONG).show();
	      return START_STICKY;
	}
	   @Override
	public void onDestroy() {
	      super.onDestroy();
	      unregisterReceiver(mWifiSCReceiver);
	      unregisterReceiver(mWifiScanReceiver);
	      Toast.makeText(this, "Securify service stopped", Toast.LENGTH_LONG).show();
	}  
	void __RegisterWifiReceivers() {
		registerReceiver(mWifiScanReceiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		registerReceiver(mWifiSCReceiver,new IntentFilter(
				WifiManager.NETWORK_STATE_CHANGED_ACTION));
	}
	
	public void InitWhiteListDataStructures() {
		Integer errno = mReadDataFromWhiteListFile();
		if(errno==0) {
			Log.d(TAG, "Initialized whitelist data structures: successful");
		}
		else
			Log.e(TAG, "Initialized whitelist data structures: Unsuccessful");
	}
	
	
	private Integer mReadDataFromWhiteListFile() {
		Integer errno = 0;
		File file = new File(WHITELISTFILE);
		mSafeLatitudesList = new ArrayList<Double>();
		mSafeLongitudesList = new ArrayList<Double>();
		mSafeLocationNameList = new ArrayList<String>();
		try {
		    Scanner scanner = new Scanner(file);
		    while (scanner.hasNextLine()) {
		        String line = scanner.nextLine();
				String[] words = line.split("\\|");
		           if(words.length==4) {
			           mSafeLatitudesList.add(Double.parseDouble(words[1]));
			           mSafeLongitudesList.add(Double.parseDouble(words[2]));
			           mSafeLocationNameList.add(words[0]);
			           Log.d(TAG,mSafeLatitudesList.toString() +"---"+mSafeLocationNameList.toString()
			        		   				+":"+mSafeLongitudesList.toString());
		           }
		    }
		    scanner.close();
	    } catch(FileNotFoundException e) { 
	    	errno = -1;
	    }
		return errno;
	}
	
	public void UpdateLocationStatusService() {
		Double distance = this.FindNearestPointDistService();
		Log.d(TAG, " Update location status called with distance: "+distance);
		if(distance>=0 && distance<=DIST_THRESHOLD) {
			mCurentLocationStatus = LOCATION_TRUSTED_STATE;
			mInSafeLocation = true;
			if(mChangeRandomisationStatus(0)<0)
				Log.e(TAG,"Couldnot change status!");
			else
				Log.d(TAG,"ADB: Status changed successfully!");
			if(mPreviousLocationStatus==LOCATION_UNTRUSTED_STATE) {
				//show a notfication
				if(mRandServiceType==2)
					displayNoRandomMACNotification();
				mPreviousLocationStatus = LOCATION_TRUSTED_STATE;
			}
		} else {
			mCurentLocationStatus = LOCATION_UNTRUSTED_STATE;
			mCurentLocation = "Unknown";
			mInSafeLocation = false;
			if(mChangeRandomisationStatus(1)<0)
				Log.e(TAG,"Couldnot change status!");
			else
				Log.d(TAG,"ADB: Status changed successfully!");
			//show a notification
			if(mPreviousLocationStatus==LOCATION_TRUSTED_STATE) {
				if(mRandServiceType==2)
					displayRandomMACNotification();
				mPreviousLocationStatus = LOCATION_UNTRUSTED_STATE;
			}
		}
	}
	public Double FindNearestPointDistService() {
		Double dist=0D;
		if(mSafeLatitudesList.size()==0) {
			return -1.0d;
		}
		Double mindist = mGeoInfoHelper.distBetweenPoints(mSafeLatitudesList.get(0),
				mSafeLongitudesList.get(0),
				mGeoInfoHelper.latitude, mGeoInfoHelper.longitude);
		mCurentLocation = mSafeLocationNameList.get(0);
		for(int i=1;i<mSafeLatitudesList.size();i++) {
			dist = mGeoInfoHelper.distBetweenPoints(mSafeLatitudesList.get(i),
												mSafeLongitudesList.get(i),
												mGeoInfoHelper.latitude, mGeoInfoHelper.longitude);
			if(dist<mindist) {
				mindist = dist;
				mCurentLocation = mSafeLocationNameList.get(i);
			}
		}
		return mindist;
	}
	
	public Integer mChangeRandomisationStatus(Integer theParam){
		Integer errno = 0;
		String CompoundCommand = mCommand+
								(theParam==1?mOnParam:mOffParam);
		Runtime runtime = Runtime.getRuntime();
		try {
			runtime.exec(CompoundCommand);
			Log.d(TAG,"Command executed: "+CompoundCommand);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			errno = -1;
		}
		return errno;
	}
	public void mForceCheckForMaliciousAP() {
		Log.d(TAG,"ForceCheckForMaliciousAP called!");
		mRefreshConfigNetworkList();
		mWifiManager.startScan();
	}
	
	public void mRefreshConfigNetworkList() {
		Log.d(TAG,"mRefreshConfigNetworkList: called" );
		mConfiguredNetworks.clear();
		List<WifiConfiguration> ConfiguredNetworks = mWifiManager.getConfiguredNetworks();
		if(ConfiguredNetworks==null) {
			Log.e(TAG,"Error: WifiManager returned null");
			return;
		}
		for(WifiConfiguration wc:ConfiguredNetworks) {
			if(wc.getAuthType()==0) 
				mConfiguredNetworks.add(wc);
		}
	}
	
	private class CheckForMaliciousAPTask extends AsyncTask<Void, Void, String> {
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			mAsyncTaskRunning = true;
		}

		@Override
		protected void onPostExecute(String address) {
			// Display the current address in the UI
			mAsyncTaskRunning = false;
			
		}

		@Override
		protected String doInBackground(Void... params) {
			mCheckForMaliciousAP();
			return null;
		}
	}
	
	public void mCheckForMaliciousAP() {
		Log.d(TAG,"mCheckForMaliciousAP: called" );
		int flag = 1;
		for(WifiConfiguration wc:mConfiguredNetworks) {
			flag = 1;
			for(String s:mAlreadyNotifiedSSIDs) {
				if(wc.SSID.equals(s)) {
					Log.d(TAG,"mCheckForMaliciousAP: already notified!");
					flag=0;
					break;
				}
			}
			if(flag==1) {
				for(ScanResult sr:mScanResult) {
					if(('"'+(sr.SSID)+'"').equals(wc.SSID))
							if(!sr.BSSID.equals(wc.BSSID)) {
									if(!mWifiManager.disconnect())
										Log.e(TAG,"Malicious network detected: disconnection unsuccessful!");
									if(!mWifiManager.removeNetwork(wc.networkId))
										Log.e(TAG,"Malicious network detected: removal unsuccessful!");
									mWifiManager.saveConfiguration();
									mAlreadyNotifiedSSIDs.add(wc.SSID);
									displayMaliciousAPNotification();
							} else {Log.d(TAG,"mCheckForMaliciousAP: equal BSSIDs!" );}
				}
			}
		}
	}
	
	protected void displayNoRandomMACNotification() {
	      Log.i(TAG, "notification");
	      NotificationCompat.Builder  mBuilder = new NotificationCompat.Builder(this);	
	      mBuilder.setTicker("Welcome to "+mCurentLocation);
	      mBuilder.setSmallIcon(R.drawable.ic_launcher);
	      NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
	      String[] events = new String[2];
	      events[0] = new String("Now you are in a safe location");
	      events[1] = new String("Original mac restored!");
	      inboxStyle.setBigContentTitle("MAC Randomization stopped");
	      for (int i=0; i < events.length; i++) 
	    	  inboxStyle.addLine(events[i]);
	      mBuilder.setStyle(inboxStyle);
	      mNotificationManager.notify(1100, mBuilder.build());
	}
	
	protected void displayRandomMACNotification() {
	      Log.i(TAG, "notification");
	      NotificationCompat.Builder  mBuilder = new NotificationCompat.Builder(this);	
	      mBuilder.setTicker("Unknown location detected!");
	      mBuilder.setSmallIcon(R.drawable.ic_launcher);
	      NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
	      String[] events = new String[2];
	      events[0] = new String("You are out of the safe zone");
	      events[1] = new String("MAC is being randomized");
	      inboxStyle.setBigContentTitle("MAC Randomization started");
	      for (int i=0; i < events.length; i++) 
	    	  inboxStyle.addLine(events[i]);
	      mBuilder.setStyle(inboxStyle);
	      mNotificationManager.notify(1200, mBuilder.build());
	}
	
	
	
	protected void displayMaliciousAPNotification() {
	      Log.i(TAG, "notification");

	      NotificationCompat.Builder  mBuilder = 
	      new NotificationCompat.Builder(this);	
	      mBuilder.setTicker("Securify alert!");
	      mBuilder.setSmallIcon(R.drawable.ic_launcher);
	      
	      NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
	      String[] events = new String[2];
	      events[0] = new String("Current AP seems malicious");
	      events[1] = new String("Forgetting AP. Manually connect at your own risk!");
	      inboxStyle.setBigContentTitle("Suspicious activity detected!");
	      for (int i=0; i < events.length; i++) 
	    	  inboxStyle.addLine(events[i]);
	      mBuilder.setStyle(inboxStyle);
	      mNotificationManager.notify(1000, mBuilder.build());
	}
	
	public Boolean mCheckWifiConnectivity() {
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    	return mWifi.isConnected(); 
	}
	
	public class WifiScanReceiverForService extends BroadcastReceiver {
		@Override
		public void onReceive(Context c, Intent intent) {
			Log.d(TAG,"WifiScanCompleteReceiver called ");
			mScanResult.clear();
			List<ScanResult> ScanResults = mWifiManager.getScanResults();
			if(ScanResults==null) {
				Log.e(TAG, "Wifimanager returned null");
				return;
			}
			if(!mCheckMaliciousFlag)
				return;
			for(ScanResult sc:ScanResults) {
				if(sc.capabilities.equals("[ESS]"))
					mScanResult.add(sc);
			}
			if(!mAsyncTaskRunning) {
				new CheckForMaliciousAPTask().execute();
			}
		} 
	}
	public class WifiStateChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive( Context context, Intent intent ) {
			Log.d(TAG,"WifiStateChangeReceiver called");
		}
	}
}
	
