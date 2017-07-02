package com.opendev.securifi;

import android.content.Context;
import android.util.Log;

public class GeoInfoServiceHelper extends GeoInfoHelperBase{

	SecurifiCoreService mService;
	
	public GeoInfoServiceHelper(Context context,SecurifiCoreService theService) {
		super(context);
		this.mService = theService;
		if (!canGetLocation) {
			Log.e(SecurifiCoreService.TAG, "Couldnot get location");
			return;
		}
		// TODO Auto-generated constructor stub
	}
	
	public void Update() {
		super.Update();
		Log.d(SecurifiCoreService.TAG, "Location Updated with latitude: "+latitude+" longitude: "+longitude);
		mService.UpdateLocationStatusService();
	}
}
