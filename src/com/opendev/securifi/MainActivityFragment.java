package com.opendev.securifi;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
 
public class MainActivityFragment extends Fragment{
	public static final String TAG = "CMainFragmentActivity";
	static Switch mSwitchSecurifiState;
	Switch mSwitchSecurifiServiceState, mSwitchMaliciousAPDetection;
	TextView mTVOriginalMAC;
	String mOriginalMAC;
	Intent mService;
	RadioGroup mRGRandopts;
	WifiOnOffReceiver mWifiOnOffReceiver;
	WifiManager mWifiManager;
	WifiInfo mWifiInfo;
	SharedPreferences mSharedPrefs;
 
	@Override
	public void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		mWifiOnOffReceiver = new WifiOnOffReceiver();
		mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
		mWifiInfo = mWifiManager.getConnectionInfo();
		mOriginalMAC = mWifiInfo.getMacAddress();
		getActivity();
		mSharedPrefs = getActivity().getSharedPreferences(getActivity().getPackageName(), Context.MODE_PRIVATE);
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.main_layout, container, false);
        mTVOriginalMAC = (TextView) v.findViewById(R.id.tvoriginalmac1);
        mSwitchSecurifiState = (Switch) v.findViewById(R.id.switchstartwifi);
		mSwitchSecurifiServiceState = (Switch)v.findViewById(R.id.switchstartsecurifiservice);
		mSwitchMaliciousAPDetection = (Switch)v.findViewById(R.id.switchmaliciousap);
		mRGRandopts = (RadioGroup)v.findViewById(R.id.rGmacrandomtype);
		setCheckOnRadioButton();
        getActivity().getActionBar().setTitle("SeQurifi Options");
        if(mOriginalMAC!=null)
        	mTVOriginalMAC.append(mOriginalMAC);
		mSwitchSecurifiState.setChecked(mWifiManager.isWifiEnabled());
        mSwitchSecurifiState
		.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0,
					boolean arg1) {
				// TODO Auto-generated method stub
				mWifiManager.setWifiEnabled(arg1);
			}
		});
        mSwitchMaliciousAPDetection.setChecked(mSharedPrefs.getBoolean("MALICIOUS_FLAG", false));
        mSwitchMaliciousAPDetection
		.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0,
					boolean arg1) {
				// TODO Auto-generated method stub
				Editor e = mSharedPrefs.edit();
				e.putBoolean("MALICIOUS_FLAG", arg1);
				e.apply();
			}
		});
        
        mSwitchSecurifiServiceState
			.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton arg0,
				boolean arg1) {
			// TODO Auto-generated method stub
			if(arg1) {
				mService = new Intent(getActivity(),SecurifiCoreService.class);
				getActivity().startService(mService);
			}
			else {
				if(mService!=null)
					getActivity().stopService(mService);
			}
		}});
        mRGRandopts.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener() {
        	@Override
            public void onCheckedChanged(RadioGroup group, int checkedId) 
               {
        		Editor e = mSharedPrefs.edit();
        		String text = "MAC Randomization: ";
            	switch(checkedId) {
                case R.id.ralways:
                    e.putInt("RAND_OPTS", 1);
                    text+="Always on";
                    break;
                case R.id.rgeolocation:
                	e.putInt("RAND_OPTS", 2);
                	text+="Geolocation based";
                    break;
                case R.id.roff:
                	e.putInt("RAND_OPTS", 3);
                	text+="Always off";
                    break;
                }
            	e.apply();
            	Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
            }
        });
        return v;
    }
    public void setCheckOnRadioButton() {
    	int i = mSharedPrefs.getInt("RAND_OPTS", 1);
    	switch(i){
    	case 1:
    		mRGRandopts.check(R.id.ralways);
    		break;
    	case 2:
    		mRGRandopts.check(R.id.rgeolocation);
    		break;
    	case 3:
    		mRGRandopts.check(R.id.roff);
    		break;
    	}
    }
    
    @Override
    public void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    	getActivity().registerReceiver(mWifiOnOffReceiver, new IntentFilter(
				WifiManager.WIFI_STATE_CHANGED_ACTION));
    }
    
    @Override
    public void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    	getActivity().unregisterReceiver(mWifiOnOffReceiver);
    }
    
    public static Handler mhandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
				mSwitchSecurifiState.setChecked(msg.what==1?true:false);
		}
	};
    
    public class WifiOnOffReceiver extends BroadcastReceiver {
		@Override
		public void onReceive( Context context, Intent intent ) {
			int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
			if(state == WifiManager.WIFI_STATE_ENABLED) {
				mhandler.sendEmptyMessage(1);
				Log.d(TAG,"WifiStateChange: enabled");
			}
			else if(state == WifiManager.WIFI_STATE_DISABLED) {
				mhandler.sendEmptyMessage(0);
				Log.d(TAG,"WifiStateChange: disabled");			
			}
			else
				Log.d(TAG,"WifiStateChange: Unknown");
		}
	}
}
