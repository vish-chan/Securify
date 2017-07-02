package com.opendev.securifi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class GeoInfoActivity extends Activity {

	public static final String WHITELISTFILE = "/sdcard/SecurifiWhiteList.txt";
	public static final String TAG = "GeoInfoActivity";
	private static final double DIST_THRESHOLD = 300.0d;
	public static final Integer LOCATION_TRUSTED_STATE = 3;
	public static final Integer LOCATION_UNTRUSTED_STATE = 4;
	DoubleListAdapter mListadapter;
	static TextView mTVAddress;
	static TextView mTVLocation;
	static String mTrustedColor = "#00ff3f";
	static String mUntrustedColor = "#FF4040";
	static LinearLayout mLayout;
	ListView mSafeLocationsListView;
	GeoInfoActivtyHelper mGeohelper;
	static Boolean mInSafeLocation = false;
	static String mAddressData = new String();
	List<String> mLocNames;
	List<String> mLocParams;
	static String mCurentLocation = "Unknown";
	Integer mLocationStatus;
	List<Double> mSafeLatitutesList = new ArrayList<Double>();
	List<Double> mSafeLongitudesList = new ArrayList<Double>();
	List<String> mSafeLocationNameList = new ArrayList<String>();;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_geo_info);
		mSafeLocationsListView = (ListView)findViewById(R.id.lvcwhitelist);
		mTVLocation = (TextView) findViewById(R.id.tvLocation);
		mTVLocation.setText(mCurentLocation);
		mTVAddress = (TextView) findViewById(R.id.tvcurentlocation);
		mLayout = (LinearLayout)findViewById(R.id.layout1);
		InitWhiteListDataStructures();
		mGeohelper = new GeoInfoActivtyHelper(getApplicationContext(), this);
		if (!mGeohelper.canGetLocation)
			this.showSettingsAlert();
		mGeohelper.Update();
		mTVAddress.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// add location coordinates to safe list;
				if(!mInSafeLocation)
					showSaveLocationDialog();
				else 
					showRemoveLocationDialog(mCurentLocation, -1);
			}
		});

		//listview listener for whitelist
		mSafeLocationsListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                    final int position, long arg3) 
            {
            	showRemoveLocationDialog(mLocNames.get(position),position);
            	return true;
            }
        }); 
	}

	public void UpdateAddressText(String theText, Boolean clear) {
		if(clear) {
			mAddressData = theText;
			mhandler.sendEmptyMessage(1);
			Log.d(TAG, "Update Address Text called!");
		}
		else {
			mAddressData +=theText;
			mhandler.sendEmptyMessage(2);
		}
	}

	public static Handler mhandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1)
				mTVAddress.setText(mAddressData);
			else if(msg.what==2)
				mTVAddress.append(mAddressData);
			else if(msg.what==LOCATION_TRUSTED_STATE) {
				mInSafeLocation = true;
				//mTVAddress.setBackgroundColor(Color.parseColor(mTrustedColor));
				mLayout.setBackgroundColor(Color.parseColor(mTrustedColor));
				mTVLocation.setText("Location: "+mCurentLocation);
				Log.d(TAG, "Handler : trusted location!");
			}
			else if(msg.what==LOCATION_UNTRUSTED_STATE){
				mInSafeLocation = false;
				mCurentLocation = "Unknown";
				//mTVAddress.setBackgroundColor(Color.parseColor(mUntrustedColor));
				mLayout.setBackgroundColor(Color.parseColor(mUntrustedColor));
				mTVLocation.setText("Location: "+mCurentLocation);
				Log.d(TAG, "Handler : untrusted location!");
			}
		}
	};
	
	public void UpdateLocationStatus() {
		Double distance = FindNearestPointDist();
		Log.d(TAG, " Update location status called with distance: "+distance);
		if(distance>=0 && distance<=DIST_THRESHOLD) {
			mLocationStatus = LOCATION_TRUSTED_STATE;
		}
		else {
			mLocationStatus = LOCATION_UNTRUSTED_STATE;
		}
		UpdateTextViewState(mLocationStatus);
	}
	
	public void UpdateTextViewState(int LocationStatus) {
		mhandler.sendEmptyMessage(LocationStatus);
	}
	
	public Double FindNearestPointDist() {
		Double dist=0D;
		if(mSafeLatitutesList.size()==0) {
			return -1.0d;
		}
		Double mindist = mGeohelper.distBetweenPoints(mSafeLatitutesList.get(0),
				mSafeLongitudesList.get(0),
				mGeohelper.latitude, mGeohelper.longitude);
		mCurentLocation = mSafeLocationNameList.get(0);
		for(int i=1;i<mSafeLatitutesList.size();i++) {
			dist = mGeohelper.distBetweenPoints(mSafeLatitutesList.get(i),
												mSafeLongitudesList.get(i),
												mGeohelper.latitude, mGeohelper.longitude);
			if(dist<mindist) {
				mindist = dist;
				mCurentLocation = mSafeLocationNameList.get(i);
			}
		}
		return mindist;
	}
		
//reads whitelist from file and udpates the data structures
	public Integer mReadDataFromFile() {
		Integer errno = 0;
		File file = new File(WHITELISTFILE);
		mLocNames = new ArrayList<String>();
		mLocParams = new ArrayList<String>();
		mSafeLatitutesList = new ArrayList<Double>();
		mSafeLongitudesList = new ArrayList<Double>();
		mSafeLocationNameList = new ArrayList<String>();
		try {
		    Scanner scanner = new Scanner(file);
		    while (scanner.hasNextLine()) {
		        String line = scanner.nextLine();
				String[] words = line.split("\\|");
		           if(words.length==4) {
			           mLocNames.add(words[0]);
			           mLocParams.add(words[3]+": "+words[1]+", "+words[2]);
			           mSafeLatitutesList.add(Double.parseDouble(words[1]));
			           mSafeLongitudesList.add(Double.parseDouble(words[2]));
			           mSafeLocationNameList.add(words[0]);
		           }
		    }
		    scanner.close();
	    } catch(FileNotFoundException e) { 
	    	errno = -1;
	    }
		return errno;
	}
	
	
	public void InitWhiteListDataStructures() {
		Integer errno = mReadDataFromFile();
		if(errno==0) {
			Log.d(TAG, "Initialized whitelist data structures: successful");
			mListadapter = new DoubleListAdapter(getApplicationContext(), mLocNames, mLocParams);
			mSafeLocationsListView.setAdapter(mListadapter);
		}
		else
			Log.e(TAG, "Initialized whitelist data structures: Unsuccessful");
	}
	
	public int mAddDataToWhiteList(String theLoc) {
		int errno = 0;
		errno =  mAddToWhiteListFile(theLoc);
		if(errno<0)
			return errno;
		AddDataToWhitelistDataStructures(theLoc);
		UpdateLocationStatus();
		return errno;
	}
	
	public void AddDataToWhitelistDataStructures(String mLoc) {
		mLocNames.add(mLoc);
		mLocParams.add(mGeohelper.mLocality+": "+mGeohelper.latitude+", "+mGeohelper.longitude);
		mListadapter = new DoubleListAdapter(getApplicationContext(), mLocNames, mLocParams);
		mSafeLocationsListView.setAdapter(mListadapter);
		mSafeLatitutesList.add(mGeohelper.latitude);
		mSafeLongitudesList.add(mGeohelper.longitude);
		mSafeLocationNameList.add(mLoc);
		Log.d(TAG, " AddDataToWhitelistDataStructures called and added: "+mLoc);
	}
	public int mRemoveDataFromWhiteList(String theLoc, int theIdx) {
		int errno = 0;
		if(theIdx<0) {
			for(int i=0;i<mLocNames.size();i++)
				if(theLoc.equals(mLocNames.get(i))) {
					theIdx = i;
					break;
				}
		}
		if(theIdx<0)
			return -1;
		errno = mRemoveFromWhiteListFile(theLoc);
		if(errno<0)
			return errno;
		RemoveDataFromWhitelistDataStructures(theLoc,theIdx);
		UpdateLocationStatus();
		return errno;
	}
	public void RemoveDataFromWhitelistDataStructures(String mLoc,int theIdx) {
		mLocNames.remove(theIdx);
		mLocParams.remove(theIdx);
		mListadapter = new DoubleListAdapter(getApplicationContext(), mLocNames, mLocParams);
		mSafeLocationsListView.setAdapter(mListadapter);
		mSafeLatitutesList.remove(theIdx);
		mSafeLongitudesList.remove(theIdx);
		mSafeLocationNameList.remove(theIdx);
		Log.d(TAG, " RemoveDataFromWhitelistDataStructures called and removed idx: "+theIdx);
	}
	public void showSaveLocationDialog() {
		final Dialog dialog = new Dialog(GeoInfoActivity.this);
		dialog.setContentView(R.layout.geo_info_addlocation);
		dialog.setTitle("Add Location");
		// set the custom dialog components - text, image and button
		final EditText text = (EditText) dialog.findViewById(R.id.editTextlocname);
		Button addButton = (Button) dialog.findViewById(R.id.buttonsave);
		Button cancelButton = (Button) dialog.findViewById(R.id.buttoncancel);
		// if button is clicked, close the custom dialog
		cancelButton.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            dialog.dismiss();
	        }
	    });
		addButton.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            if(isEmpty(text)) {
	            	Toast.makeText(getApplicationContext(), "Please enter a location name.", Toast.LENGTH_LONG).show();
	            	return;
	            }
	            if(mAddDataToWhiteList(text.getText().toString())==0) {
	            	Toast.makeText(getApplicationContext(), "Successfully added to whitelist!", Toast.LENGTH_SHORT).show();
	            	dialog.dismiss();
	            } else {
	            	Toast.makeText(getApplicationContext(), "Error: Couldnot add to whitelist!", Toast.LENGTH_SHORT).show();
	            	dialog.dismiss();
	            }	
	        }
	    });

		dialog.show();
	}
	
	public void showRemoveLocationDialog(final String theLoc, final Integer theIdx) {
		final Dialog removedialog = new Dialog(GeoInfoActivity.this);
		removedialog.setContentView(R.layout.geo_info_removelocation);
		removedialog.setTitle("Remove "+theLoc+"?");
		// set the custom dialog components - text, image and button
		Button removeButton = (Button) removedialog.findViewById(R.id.buttonremove);
		Button cancelButton = (Button) removedialog.findViewById(R.id.buttoncancel2);
		// if button is clicked, close the custom dialog
		cancelButton.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            removedialog.dismiss();
	        }
	    });
		removeButton.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            if(mRemoveDataFromWhiteList(theLoc,theIdx)==0) {
	            	Toast.makeText(getApplicationContext(), "Successfully removed from whitelist!", Toast.LENGTH_SHORT).show();
	            	removedialog.dismiss();
	            } else {
	            	Toast.makeText(getApplicationContext(), "Error: Couldnot remove from whitelist!", Toast.LENGTH_SHORT).show();
	            	removedialog.dismiss();
	            }	
	        }
	    });
		removedialog.show();
	}
	
	public boolean isEmpty(EditText etText) {
		if (etText.getText().toString().trim().length() > 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public Integer mAddToWhiteListFile(String mLoc) {
		Writer writer = null;
		Integer errno = 0;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(
			          new FileOutputStream(WHITELISTFILE, true)));
			writer.write(mLoc+"|"+mGeohelper.latitude+"|"+mGeohelper.longitude+"|"+mGeohelper.mLocality+"\n");
		} catch (IOException ex) {
			errno = -1;
		} finally { try {writer.close();} catch (Exception ex) {}}
		return errno;
	}
	
	public Integer  mRemoveFromWhiteListFile(String theLoc) {
		int errno = 0;
		try {
		      File inFile = new File(WHITELISTFILE);
		      if (!inFile.isFile()) {
		        System.out.println("Parameter is not an existing file");
		        return -1;
		      }
		      //Construct the new file that will later be renamed to the original filename.
		      File tempFile = new File(inFile.getAbsolutePath() + ".tmp");
		      BufferedReader br = new BufferedReader(new FileReader(WHITELISTFILE));
		      PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
		      String line = null;
		      //Read from the original file and write to the new
		      //unless content matches data to be removed.
		      while ((line = br.readLine()) != null) {
		    	  String[] words = line.split("\\|");
		          if (!words[0].equals(theLoc)) {
		        	  pw.println(line);
		        	  pw.flush();
		        }
		      }
		      pw.close();
		      br.close();
		      //Delete the original file
		      if (!inFile.delete()) {
		        System.out.println("Could not delete file");
		        return -1;
		      }
		      //Rename the new file to the filename the original file had.
		      if (!tempFile.renameTo(inFile)) {
		        System.out.println("Could not rename file");
		        return -1;
		      }
		    }
		    catch (FileNotFoundException ex) {
		      ex.printStackTrace();
		      errno = -1;
		    }
		    catch (IOException ex) {
		      ex.printStackTrace();
		      errno = -1;
		    }
		return errno;
	}

	public void showSettingsAlert() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(
				GeoInfoActivity.this);

		// Setting Dialog Title
		alertDialog.setTitle("GPS settings");

		// Setting Dialog Message
		alertDialog
				.setMessage("GPS is not enabled. Do you want to go to settings menu?");

		// On pressing Settings button
		alertDialog.setPositiveButton("Settings",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(
								Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivity(intent);
					}
				});

		// on pressing cancel button
		alertDialog.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						finish();
						//add code to finish this activity
					}
				});

		// Showing Alert Message
		alertDialog.show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mGeohelper.stopUsingGPS();
	};

}
