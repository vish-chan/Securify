package com.opendev.securifi;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class GeoInfoHelperBase implements LocationListener {

	private final Context mContext;

	protected HelperFunctions mHelperfunctions;
	// flag for GPS status
	protected boolean isGPSEnabled = false;

	// flag for network status
	protected boolean isNetworkEnabled = false;

	// flag for GPS status
	protected boolean canGetLocation = false;

	protected Location location; // location
	protected double latitude = 0D; // latitude
	protected double longitude = 0D; // longitude
	protected String mAddress = new String("Unknown");
	protected String mLocality = new String("Unknown");
	protected String mPincode = new String("Unknown");
	protected String mCountry = new String("Unknown");

	// The minimum distance to change Updates in meters
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 10 meters

	// The minimum time between updates in milliseconds
	private static final long MIN_TIME_BW_UPDATES = 1000 * 1; // 1 minute

	// Declaring a Location Manager
	protected LocationManager locationManager;

	protected String mProvider = new String();
	protected Geocoder mGeocoder;

	public GeoInfoHelperBase(Context context) {
		this.mContext = context;
		mHelperfunctions = new HelperFunctions();
		Init();
	}

	public void Init() {
		locationManager = (LocationManager) mContext
				.getSystemService(Context.LOCATION_SERVICE);
		if (locationManager == null) {
			Toast.makeText(mContext, "Couldnot find location manager!",
					Toast.LENGTH_SHORT).show();
			;
			return;
		}
		mGeocoder = new Geocoder(mContext, Locale.getDefault());
		try {
			// getting GPS status
			isGPSEnabled = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);

			// getting network status
			isNetworkEnabled = locationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (!isGPSEnabled && !isNetworkEnabled) {
				this.canGetLocation = false;
				return;
				// no network provider is enabled
			} else {
				this.canGetLocation = true;
				// First get location from Network Provider
				if (isNetworkEnabled) {
					mProvider = LocationManager.NETWORK_PROVIDER;
					Log.d("Network", "Network");
				}
				// if GPS Enabled get lat/long using GPS Services
				else if (isGPSEnabled) {
					mProvider = LocationManager.GPS_PROVIDER;
					Log.d("GPS Enabled", "GPS Enabled");

				}
				locationManager.requestLocationUpdates(mProvider,
						MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES,
						this);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void Update() {
		UpdateLocation();
		UpdateAddress();
	}

	public Location UpdateLocation() {
		if (locationManager != null && canGetLocation) {
			location = locationManager.getLastKnownLocation(mProvider);
			if (location != null) {
				latitude = location.getLatitude();
				longitude = location.getLongitude();

			}
		}
		return location;
	}

	public String UpdateAddress() {
		mAddress = new String("No address for this location.");
		if (location != null) {
			try {
				List<Address> addresses = mGeocoder.getFromLocation(latitude,
						longitude, 1);
				StringBuilder sb = new StringBuilder();
				if (addresses.size() > 0) {
					Address address = addresses.get(0);
					for (int i = 0; address.getAddressLine(i) != null; i++) {
						sb.append(address.getAddressLine(i)).append("\n");
					}
					mLocality=(address.getLocality()!=null?address.getLocality():"Unknown");
					//sb.append(mLocality).append("\n");
					mPincode = (address.getPostalCode()!=null?address.getPostalCode():"Unknown");
					//sb.append("Pin code: " + mPincode).append("\n");
					mCountry = (address.getCountryName()!=null?address.getCountryName():"Unknown");
					//sb.append("Country: " + mCountry);
				}
				mAddress = sb.toString();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return mAddress;
	}
	
	public double distBetweenPoints(double lat1, double lng1, double lat2, double lng2) {
	    double earthRadius = 6371000.0D;//meters
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    double sindLat = Math.sin(dLat / 2);
	    double sindLng = Math.sin(dLng / 2);
	    double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
	            * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = earthRadius * c;
	    return dist;
	}


	/**
	 * Stop using GPS listener Calling this function will stop using GPS in your
	 * app
	 * */
	public void stopUsingGPS() {
		if (locationManager != null) {
			locationManager.removeUpdates(GeoInfoHelperBase.this);
		}
	}

	/**
	 * Function to get latitude
	 * */
	public double getLatitude() {
		if (location != null) {
			latitude = location.getLatitude();
		}
		// return latitude
		return latitude;
	}

	/**
	 * Function to get longitude
	 * */
	public double getLongitude() {
		if (location != null) {
			longitude = location.getLongitude();
		}
		// return longitude
		return longitude;
	}

	public String getAddress() {
		return mAddress;
	}

	/**
	 * Function to check GPS/wifi enabled
	 * 
	 * @return boolean
	 * */
	public boolean canGetLocation() {
		return this.canGetLocation;
	}

	/**
	 * Function to show settings alert dialog On pressing Settings button will
	 * lauch Settings Options
	 * */

	@Override
	public void onLocationChanged(Location location) {
		this.Update();
	}

	@Override
	public void onProviderDisabled(String provider) {
		// getting GPS status
		if (locationManager != null) {
			isGPSEnabled = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);

			// getting network status
			isNetworkEnabled = locationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (!isGPSEnabled && !isNetworkEnabled) {
				this.canGetLocation = false;
			}
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(mContext, "Provider: "+provider +" enabled", Toast.LENGTH_SHORT).show();
		if(!canGetLocation) {
			this.Init();
			this.Update();
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if(!canGetLocation && status==LocationProvider.AVAILABLE) {
			this.Init();
			this.Update();
		}
	}

}