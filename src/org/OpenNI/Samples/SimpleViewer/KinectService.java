package org.OpenNI.Samples.SimpleViewer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class KinectService extends Service {
	
	static String VibrateString = "no Vibration yet";
	
	@Override
	public int onStartCommand(Intent intent, int flag, int startID){
		
		Log.d("KinectService", "Kinect Service was started!");		
				
//		String serviceString = Context.LOCATION_SERVICE;
//		LocationManager locationManager;
//		locationManager = (LocationManager)getSystemService(serviceString);
//		String provider = LocationManager.GPS_PROVIDER;
//		locationManager.requestLocationUpdates(provider, 0, 0, locationListener);
		
//		Intent intent = new Intent();
//		intent.putExtra("locMsg", "Latitude = " + location.getLatitude() + ", Longitude = " + location.getLongitude());
//		sendBroadcast(intent);
		
		return Service.START_STICKY;
	}
	
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
