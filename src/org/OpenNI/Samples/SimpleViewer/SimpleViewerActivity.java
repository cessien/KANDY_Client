package org.OpenNI.Samples.SimpleViewer;

import java.io.File;
import java.io.IOException;

import org.OpenNI.Samples.Assistant.OpenNIBaseActivity;
import org.OpenNI.Samples.Assistant.Screen;

import org.OpenNI.Samples.SimpleViewer.R;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

//<Debug>
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;

import org.OpenNI.Samples.SimpleViewer.SimpleGestureFilter.SimpleGestureListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
//</Debug>

public class SimpleViewerActivity extends OpenNIBaseActivity implements OnInitListener, SimpleGestureListener{
	private SimpleViewer simpleViewer;
	
	private Screen theScreen;
//	private SurfaceView surfaceKinect;
	private Canvas x;
	/** Called when the activity is first created. */
	private Button VibrateTestButton;
	private Button BeepTestButton;
	private SeekBar depthbar;
	private TextView t;
	private int i=0;
	private Vibrator vib;
	private Thread cThread; 
	private Toast toast;
	private EditText serverIp;
	private boolean needToSend = false;
	private boolean isRunning = true;
	private DatagramPacket sendPacket;
	static int[] variables = {0,0,1,5,0,5};
	String read;
	//sound stuff
	private MediaPlayer mp;
	private MediaPlayer mp2;
	
	//TTS stuff
		private  int MY_DATA_CHECK_CODE = 0;	
		private TextToSpeech mTts;
		HashMap<String, String> myHashRender;
		
	private ViewPager aPager; // View Pager Stuff
	private static int NUM_VIEWS = 7;
	private Context cxt;
	private KandyPagerAdapter kandyAdapter;

	private SimpleGestureFilter detector; // Swipe detector Stuff

	private Button connectPhones;

	private String serverIpAddress = "";

	private boolean connected = false;

	private Handler handler = new Handler();

	InetAddress serverAddr = null;
	DatagramSocket socket = null;
	public static final int SERVERPORT = 10001;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//View Pager
		cxt = this;
		kandyAdapter = new KandyPagerAdapter();
		aPager = (ViewPager) findViewById(R.id.aKandypager);
		aPager.setAdapter(kandyAdapter);
		aPager.setCurrentItem(1);

		//Swipe Detector Stuff
		detector = new SimpleGestureFilter(this,this);

		//startService(new Intent(Kandy_ClientActivity.this, KinectService.class));
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		//TTS stuff
				Intent checkIntent = new Intent();
				myHashRender = new HashMap();
				checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
				startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);	
		
		Log.d("TTS",""+mTts.SUCCESS);
		
//		System.load("/system/lib/libOpenNI.jni");
		System.loadLibrary("usb");
		System.loadLibrary("OpenNI");
		System.loadLibrary("OpenNI.jni");
		System.loadLibrary("nimRecorder");
		System.loadLibrary("nimMockNodes");
		System.loadLibrary("nimCodecs");
		System.loadLibrary("XnCore");
		System.loadLibrary("XnFormats");
		System.loadLibrary("XnDDK");
		System.loadLibrary("XnDeviceFile");
		System.loadLibrary("XnDeviceSensorV2");

		try {
			retrieveXml(SimpleViewer.SAMPLE_XML_FILE);
		} catch (IOException e) {
			Log.e(TAG, "onCreate() Failed!", e);
			return;
		}
		
		vib = (Vibrator) getSystemService(SimpleViewerActivity.VIBRATOR_SERVICE);
		Log.d(TAG, "create done");
	}
	
	private void initTTS(){		
		mTts.setLanguage(Locale.ENGLISH);
		File sampleFile = new File(Environment.getExternalStorageDirectory()+"/Kandy/TTS/", "Off.wav");
		sampleFile.mkdirs();

		myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Off");
		int test=mTts.synthesizeToFile("Off", myHashRender, sampleFile.getPath());
		Log.d("TTS",sampleFile.getPath()+" render file result: "+test);
		mTts.addSpeech("Off", sampleFile.getPath());

		myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "On");
		mTts.synthesizeToFile("On", myHashRender, "/sdcard/Kandy/TTS/On.wav");
		mTts.addSpeech("On", "/sdcard/Kandy/TTS/On.wav");

		myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Vibrate and Beep");
		mTts.synthesizeToFile("Vibrate and Beep", myHashRender, "/sdcard/Kandy/TTS/VaB.wav");
		mTts.addSpeech("Vibrate and Beep", "/sdcard/Kandy/TTS/VaB.wav");

		myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Vibrate Only");
		mTts.synthesizeToFile("Vibrate Only", myHashRender, "/sdcard/Kandy/TTS/VO.wav");
		mTts.addSpeech("Vibrate Only", "/sdcard/Kandy/TTS/VO.wav");

		myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Beep Only");
		mTts.synthesizeToFile("Beep Only", myHashRender, "/sdcard/Kandy/TTS/BO.wav");
		mTts.addSpeech("Beep Only", "/sdcard/Kandy/TTS/BO.wav");

		myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Debug");
		mTts.synthesizeToFile("Debug", myHashRender, "/sdcard/Kandy/TTS/Debug.wav");
		mTts.addSpeech("Debug", "/sdcard/Kandy/TTS/Debug.wav");

		myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Main Menu");
		mTts.synthesizeToFile("Main Menu", myHashRender, "/sdcard/Kandy/TTS/Menu.wav");
		mTts.addSpeech("Main Menu", "/sdcard/Kandy/TTS/Menu.wav");

		myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Text to Speech");
		mTts.synthesizeToFile("Text to Speech", myHashRender, "/sdcard/Kandy/TTS/TTS.wav");
		mTts.addSpeech("Text to Speech", "/sdcard/Kandy/TTS/TTS.wav");

		myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Alert Rate");
		mTts.synthesizeToFile("Alert Rate", myHashRender, "/sdcard/Kandy/TTS/AR.wav");
		mTts.addSpeech("Alert Rate", "/sdcard/Kandy/TTS/AR.wav");

		myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Alert Type");
		mTts.synthesizeToFile("Alert Type", myHashRender, "/sdcard/Kandy/TTS/AT.wav");
		mTts.addSpeech("Alert Type", "/sdcard/Kandy/TTS/AT.wav");

		myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Distance Threshold");
		mTts.synthesizeToFile("Distance Threshold", myHashRender, "/sdcard/Kandy/TTS/DT.wav");
		mTts.addSpeech("Distance Threshold", "/sdcard/Kandy/TTS/DT.wav");

		myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Depth Stream");
		mTts.synthesizeToFile("Depth Stream", myHashRender, "/sdcard/Kandy/TTS/DS.wav");
		mTts.addSpeech("Depth Stream", "/sdcard/Kandy/TTS/DS.wav");

	}
	private void initScreen(int width, int height) {
//		surfaceKinect = (SurfaceView)findViewById(R.id.kinectSurface);
		
		theScreen = new Screen(this);
		theScreen.setDimensions(width, height);
		theScreen.showFPS(true);
//		ViewGroup g = (ViewGroup)findViewById(R.layout.distancethres);
//		g.addView(theScreen);
//		setContentView(theScreen);
//		aPager.addView(theScreen,0);
//		aPager.setCurrentItem(0);
		
		new Thread(){
			public void run(){
				for(;;){
					if(variables[4] == 0 || variables[4] == 1){
						vibrate();
						try {
							sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}.start();
		
		mp = MediaPlayer.create(this, R.raw.beep);
		new Thread(){
			public void run(){
				for(;;){
					if(variables[4] == 0 || variables[4] == 2){
						mp.start();
						try {
							sleep((long) (2000*(1.0f - SimpleViewer.vibFactor/100.0f)));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}.start();
	}

	public void draw(Bitmap bmp) {
		theScreen.setBitmap(bmp);
		theScreen.redraw();
	}

	@Override
	protected void myInitFunction() {
		// TODO Auto-generated method stub
		Log.d(TAG, "init");
		
		simpleViewer = new SimpleViewer();
//		simpleViewer.init();
		initScreen(simpleViewer.width, simpleViewer.height);

		Log.d(TAG, "init done");
	}

	@Override
	protected void myCleanupFunction() {
		// TODO Auto-generated method stub
		simpleViewer.Cleanup();
	}

	@Override
	protected boolean myMainLoopContent() {
		try {
			simpleViewer.updateDepth();
			draw(simpleViewer.drawBitmap());
		} 
		catch (Exception e) {
			Log.e(TAG, "An exception was caught during mainLoop:", e);
			return false;
		}
		
		
		return true;
	}
	
	
	protected boolean vibrate(){
		if(SimpleViewer.vibFactor > 0.0f){
			long[] pattern = {0, 50, (long) (500*(1.0f - SimpleViewer.vibFactor/100.0f))};
//			Log.d("pattern",(1000*(1.0f - SimpleViewer.vibFactor/100.0f)) + "");
//			long[] pattern = {(long) 10, 100};
			vib.cancel();
			vib.vibrate(pattern, 0);
		}
		
		return true;
	}
	
	public void onStop() {		
		Log.d("Kandy", "Closing Program...");

		if(socket!=null)
			socket.close();

		if(vib!=null)
			vib.cancel();

		isRunning = false;
		cThread=null; 

		if(mp != null) {
			mp.release();
			mp2.release();
		}
		
		if(mTts!=null){
			mTts.stop();
			mTts.shutdown();
		}


		Log.d("Kandy", "Bye Bye!");
		android.os.Process.killProcess(android.os.Process.myPid());

		finish();

		super.onStop();


	}
	
	private void toast( String s )
	{
		if( toast == null )
		{
			toast = Toast.makeText( cxt, s, Toast.LENGTH_SHORT );
		}
		else
		{
			toast.setText( s );
		}

		toast.show();
	}

	public boolean dispatchTouchEvent(MotionEvent me){
		this.detector.onTouchEvent(me);
		return super.dispatchTouchEvent(me);
	}

	public void onSwipe(int direction) {
		String str = "";
		read = "";
		int dir=0;
		
		switch (direction) {

		case SimpleGestureFilter.SWIPE_DOWN :  str = "Swipe Down"; dir=-1;
		break;
		case SimpleGestureFilter.SWIPE_UP :    str = "Swipe Up"; dir=1;
		break;
		}
		
		int position = aPager.getCurrentItem();
		switch (position) {	
		case 2:
			if(variables[position]==1)
				variables[position]=0;
			else variables[position] = 1;
			read = (variables[position] == 0)?"Off":"On";
			break;
		case 3:
			variables[position] = Math.min(Math.max(variables[position]+=dir,0),10);
			read = ""+variables[position];
			break;
		case 4:
			variables[position] = Math.min(Math.max(variables[position]+=dir,0),2);
			if(variables[position] == 0){
				 read = "Vibrate and Beep";
			} else if (variables[position] == 1) {
				read = "Vibrate Only";
			} else {
				read = "Beep Only";
			}
			break;     
		case 5:
			variables[position] = Math.min(Math.max(variables[position]+=dir,0),10);
			read = ""+variables[position];
			break;	           
		}
		new Thread(){ 
			public void run() {					
				if(mTts!=null)
					mTts.speak(read, TextToSpeech.QUEUE_FLUSH, myHashRender);
				else
					Log.d("Value Changer", "TTS is null");
			}}.start();
			toast(str+" "+read);
	}

	public void onDoubleTap() {
		Toast.makeText(this, "Double Tap", Toast.LENGTH_SHORT).show();		
	}

	private class KandyPagerAdapter extends PagerAdapter{


		@Override
		public int getCount() {
			return NUM_VIEWS;
		}

		/**
		 * Create the page for the given position.  The adapter is responsible
		 * for adding the view to the container given here, although it only
		 * must ensure this is done by the time it returns from
		 * {@link #finishUpdate()}.
		 *
		 * @param container The containing View in which the page will be shown.
		 * @param position The page position to be instantiated.
		 * @return Returns an Object representing the new page.  This does not
		 * need to be a View, but can be some other container of the page.
		 */
		private String whichView;
		public Object instantiateItem(View collection, int position) {
			if(mTts!=null){
				mTts.setLanguage(Locale.ENGLISH);
			}
			else
				Log.d("Kandy", "TTS is not loaded");
			
		
			LayoutInflater inflater = (LayoutInflater) collection.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			int resId = 0;
			
				            		
			switch (position) {
			case 0:
				resId = R.layout.debug;
				break;	           	       
			case 1:
				resId = R.layout.defaultview;
				break;
			case 2:
				resId = R.layout.tts;
				break;
			case 3:
				resId = R.layout.alertrate;
				break;
			case 4:
				resId = R.layout.alerttype;
				break;     
			case 5:
				resId = R.layout.distancethres;
				break;	           
			case 6:
				((ViewPager) collection).addView(theScreen, 0);
				return theScreen;
			}
			

			View view = inflater.inflate(resId, null);

			((ViewPager) collection).addView(view, 0);
			
			return view;
		}

		/**
		 * Remove a page for the given position.  The adapter is responsible
		 * for removing the view from its container, although it only must ensure
		 * this is done by the time it returns from {@link #finishUpdate()}.
		 *
		 * @param container The containing View from which the page will be removed.
		 * @param position The page position to be removed.
		 * @param object The same object that was returned by
		 * {@link #instantiateItem(View, int)}.
		 */
		@Override
		public void destroyItem(View collection, int position, Object view) {
			((ViewPager) collection).removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {			
			return view==((View)object);
		}


		/**
		 * Called when the a change in the shown pages has been completed.  At this
		 * point you must ensure that all of the pages have actually been added or
		 * removed from the container as appropriate.
		 * @param container The containing View which is displaying this adapter's
		 * page views.
		 */
		@Override
		public void finishUpdate(View arg0) {			
		}
		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {}
		@Override
		public Parcelable saveState() {
			return null;
		}
		@Override
		public void startUpdate(View arg0) {
			Log.d("Page",""+aPager.getCurrentItem());
			int position = aPager.getCurrentItem();
			switch (position) {
			case 0:
				whichView="Debug";			
				Log.d("View","Going to Debug View");			
				break;	           	       
			case 1:
				whichView="Main Menu ";				
				Log.d("View","Going to Default View");
				break;
			case 2:
				whichView="Text to Speech";
				Log.d("View","Going to TTS View");
				break;
			case 3:
				whichView="Alert Rate";
				Log.d("View","Going to Alert Rate View");
				break;
			case 4:
				whichView="Alert Type";
				Log.d("View","Going to Alert Type View");
				break;     
			case 5:
				whichView="Distance Threshold";
				Log.d("View","Going to Distance Threshold View");
				break;	
			case 6:
				whichView="Depth Stream";
				Log.d("View","Going to Distance Depth Stream");
				break;
			}
			
			new Thread(){ 
				public void run() {	
					if(variables[2] == 1){
						if(mTts!=null)
							mTts.speak(whichView, TextToSpeech.QUEUE_FLUSH, null);
						else
							Log.d("Page Changer", "TTS is null");
					}
				}}.start();
			
	}	}
		
	protected void onActivityResult(
			int requestCode, int resultCode, Intent data) {
		if (requestCode == MY_DATA_CHECK_CODE) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				// success, create the TTS instance
				mTts = new TextToSpeech(this, this);
			} else {
				// missing data, install it
				Intent installIntent = new Intent();
				installIntent.setAction(
						TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}
	}

	private OnClickListener connectListener = new OnClickListener() {
		public void onClick(View v) {
			switch(v.getId()){
			
			}
		}
	};

	public void onInit(int arg0) {
		initTTS();
		
	}
}
