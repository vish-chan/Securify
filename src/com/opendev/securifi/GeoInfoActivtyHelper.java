package com.opendev.securifi;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class GeoInfoActivtyHelper extends GeoInfoHelperBase {

	GeoInfoActivity mGeoInfoActivty = new GeoInfoActivity();

	public GeoInfoActivtyHelper(Context context, GeoInfoActivity theGIA) {
		super(context);
		this.mGeoInfoActivty = theGIA;
		if (!canGetLocation) {
			Log.d(GeoInfoActivity.TAG, "Can't find locations: GPS off");
			return;
		}
	}

	public void Update() {
		super.UpdateLocation();
		new GetAddressTask().execute();
		mGeoInfoActivty.UpdateLocationStatus();
		Log.d(GeoInfoActivity.TAG, "Found new location!");
	}

	private class GetAddressTask extends AsyncTask<Void, Void, String> {
		/*
		 * When the task finishes, onPostExecute() displays the address.
		 */
		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onPostExecute(String address) {
			// Display the current address in the UI
			String Address = String.format("(%s, %s)\n\n",mHelperfunctions
								.round(latitude, 5),mHelperfunctions.round(longitude, 5))+
							 mAddress;
			mGeoInfoActivty.UpdateAddressText(Address,true);
		}

		@Override
		protected String doInBackground(Void... params) {
			return UpdateAddress();

		}
	}
}
