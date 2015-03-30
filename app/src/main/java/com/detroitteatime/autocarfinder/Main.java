package com.detroitteatime.autocarfinder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.detroitteatime.autocarfinder.R;

public class Main extends FragmentActivity implements OnClickListener,
		LocationSource, LocationListener {

	private double carLt = 0;
	private double carLg = 0;

	static final int UNIQUE_ID = 123365;

	public static final String COLOR = "#506560";

	private FrameLayout monitor;
	private Button start;

	private Button manual;
	private ProgressBar progress;

	boolean naughtData;

	Intent i;

	private static SharedPreferences data1;
	private SharedPreferences.Editor editor1;

	private boolean serviceStopped;

	private NotificationManager nm;

	public static final String START_SLEEP_HOUR_KEY = "start_hour";
	public static final String END_SLEEP_HOUR_KEY = "end_hour";
	public static final String START_SLEEP_MINUTE_KEY = "start_min";
	public static final String END_SLEEP_MINUTE_KEY = "end_min";
	public static final String SLEEP_MODE_KEY = "sleep_mode";
	public static final String WAKE_LOCK_SETTING = "wake_lock_setting";

	public static final int SLEEP_MODE_TRUE = 1;
	public static final int SLEEP_MODE_FALSE = 0;
	private Notification n;
	private AlarmManager mgr;
	private PendingIntent pi;

	private Menu myMenu;

	GoogleMap map;

	long stop;

	int x, y;

	Drawable car, you;
	public static final int ZOOM_LEVEL = 20;
	private LocationManager manager;
	private Criteria criteria;
	private String provider;

	boolean yourAvailable;
	boolean carAvailable;

	double lat = 0;
	double longi = 0;

	double carLat = 0;
	double carLongi = 0;

	Button navigate, type;

	final int RQS_GooglePlayServices = 1;

	LatLng carPos, yourPos;
	LatLngBounds bounds;

	Marker youMarker, carMarker;

	boolean first = true;
	private double d;
	private int mapType;
	private SupportMapFragment mySupportMapFragment;

	TextView distance;
	LinearLayout mainLayout;
	private Polyline polyline;
	private PolylineOptions rectOptions;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mainLayout = (LinearLayout) this.getLayoutInflater().inflate(
				R.layout.main, null);

		setContentView(mainLayout);

		// Possible work around for market launches. See
		// http://code.google.com/p/android/issues/detail?id=2373
		// for more details. Essentially, the market launches the main activity
		// on top of other activities.
		// we never want this to happen. Instead, we check if we are the root
		// and if not, we finish.
		if (!isTaskRoot()) {
			final Intent intent = getIntent();
			final String intentAction = intent.getAction();
			if (intent.hasCategory(Intent.CATEGORY_LAUNCHER)
					&& intentAction != null
					&& intentAction.equals(Intent.ACTION_MAIN)) {
				// Log.w("My Code",
				// "Main Activity is not the root.  Finishing Main Activity instead of launching.");
				finish();
				return;
			}
		}

		// set up buttons
		start = (Button) findViewById(R.id.start);

		manual = (Button) findViewById(R.id.manual);
		// progress = (ProgressBar) findViewById(R.id.progressBar1);

		start.setOnClickListener(this);
		start.getBackground().setColorFilter(Color.parseColor(COLOR),
				PorterDuff.Mode.MULTIPLY);

		manual.setOnClickListener(this);
		manual.getBackground().setColorFilter(Color.parseColor(COLOR),
				PorterDuff.Mode.MULTIPLY);

		monitor = (FrameLayout) findViewById(R.id.frameLayout1);

		data1 = this.getSharedPreferences("storage", 0);
		editor1 = data1.edit();

		// firstTime = data1.getBoolean("first_time", true);

		editor1.putBoolean("first_time", false);
		editor1.commit();

		pi = PendingIntent
				.getActivity(this, 0, new Intent(this, Main.class), 0);

		data1 = getSharedPreferences("storage", 0);
		editor1 = data1.edit();

		// set a global layout listener which will be called when the layout
		// pass is completed and the view is drawn

		FragmentManager myFragmentManager = getSupportFragmentManager();
		mySupportMapFragment = (SupportMapFragment) myFragmentManager
				.findFragmentById(R.id.map);

		if (MapsInitializer.initialize(this) != ConnectionResult.SUCCESS) {
			Toast.makeText(this, "Map failed to initialize.",
					Toast.LENGTH_SHORT).show();

		}

		map = mySupportMapFragment.getMap();

		manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Define the criteria how to select the locatioin provider -> use
		// default
		criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		provider = manager.getBestProvider(criteria, false);
		manager.requestLocationUpdates(provider, 1000, 1, this);

		navigate = (Button) findViewById(R.id.navigate);
		navigate.setOnClickListener(this);

		type = (Button) findViewById(R.id.satellite);
		type.setOnClickListener(this);

	}

	@Override
	protected void onResume() {
		super.onResume();

		isGoogleMapsInstalled();

		you = getResources().getDrawable(R.drawable.you);// set to person
		car = getResources().getDrawable(R.drawable.car);// set to car

		lat = carLat = Double.valueOf(data1.getString("carLat", "0"));
		longi = carLongi = Double.valueOf(data1.getString("carLng", "0"));

		yourPos = new LatLng(lat, longi);

		if (youMarker == null)
			youMarker = map
					.addMarker(new MarkerOptions().position(yourPos).icon(
							BitmapDescriptorFactory
									.fromResource(R.drawable.you)));
		else
			youMarker.setPosition(yourPos);

		carPos = new LatLng(carLat, carLongi);

		if (carMarker == null)
			carMarker = map
					.addMarker(new MarkerOptions().position(carPos).icon(
							BitmapDescriptorFactory
									.fromResource(R.drawable.car)));
		else
			carMarker.setPosition(carPos);

		// get distance between two points
		float[] results = new float[1];

		Location.distanceBetween(carLat, carLongi, lat, longi, results);
		d = results[0];

		distance = (TextView) findViewById(R.id.distance);

		distance.setText("Distance to car: " + Math.round(d) + " m");

		rectOptions = new PolylineOptions().add(yourPos) // Same longitude, and
				// 16km to the south
				.add(carPos).width(5).color(Color.BLUE); // Closes the polyline.

		// Get back the mutable Polyline

		if (lat == 0 && longi == 0) {

			Toast.makeText(
					this,
					"No location data yet, setting initial position to coordinates 0,0, making car icon invisible",
					Toast.LENGTH_LONG).show();
		}

		mapType = data1.getInt("map_type", GoogleMap.MAP_TYPE_NORMAL);

		map.setMapType(mapType);

		if (mapType == GoogleMap.MAP_TYPE_NORMAL) {

			type.setText("Change to hybrid");

		} else {

			type.setText("Change to normal");

		}

		serviceStopped = data1.getBoolean("shut_down_service", true);

		if (!serviceStopped) {

			start.setText("Stop monitoring");
			monitor.setBackgroundColor(Color.GREEN);

		} else {

			start.setText("Start monitoring");
			monitor.setBackgroundColor(Color.RED);

		}

		first = data1.getBoolean("first", true);

		editor1.putBoolean("first", false);

		if (first) {
			Builder alertDialogBuilder = new Builder(
					this);

			// set title
			alertDialogBuilder.setTitle("First Time");

			// set dialog message
			alertDialogBuilder
					.setMessage(
							"Welcome! Right now you have no car data, so your car icon will not be displayed until you drive somewhere or set your car location manually. "
									+ "If you see a blue screen with a stick man icon, the app hasn't got any data for your location yet either, and has set your coordinates to 0,0, "
									+ "which is in the Atlantic Ocean somewhere off the coast of Africa."
									+ "Also, it might be a good idea to read the info section in the menu to find out more about how this app works. Thanks for downloading"
									+ " Auto Car Finder!")
					.setCancelable(false)
					.setNegativeButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// if this button is clicked, just close
									// the dialog box and do nothing
									dialog.cancel();
								}
							});

			// create alert dialog
			AlertDialog alertDialog = alertDialogBuilder.create();

			// show it
			alertDialog.show();
		}

		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(getApplicationContext());

		if (resultCode == ConnectionResult.SUCCESS) {
			// Toast.makeText(getApplicationContext(),
			// "isGooglePlayServicesAvailable SUCCESS", Toast.LENGTH_LONG)
			// .show();

			// Register for location updates using a Criteria, and a callback on
			// the specified looper thread.
			manager.requestLocationUpdates(0L, // minTime
					0.0f, // minDistance
					criteria, // criteria
					this, // listener
					null); // looper

			// Replaces the location source of the my-location layer.
			map.setLocationSource(this);

		} else {
			GooglePlayServicesUtil.getErrorDialog(resultCode, this,
					RQS_GooglePlayServices);
		}

		mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					public void onGlobalLayout() {

						setZoom();

					}
				});

	}

	@Override
	protected void onPause() {
		super.onPause();
		manager.removeUpdates(this);
	}

	@Override
	public void onClick(View v) {

		int id = v.getId();
		if (id == R.id.start) {
			serviceStopped = data1.getBoolean("shut_down_service", true);
			if (serviceStopped) {
				makeSureGPSIsOn(this);
				startService(new Intent(Main.this, SensorService.class));

				start.setText("Stop monitoring");
				monitor.setBackgroundColor(Color.GREEN);

				nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				String body = "Monitoring";
				String titleString = "Car Finder Service";
				n = new Notification(R.drawable.car_notification, body,
						System.currentTimeMillis());

				n.setLatestEventInfo(this, titleString, body, pi);

				nm.notify(UNIQUE_ID, n);

				// Log.i("My Code", "Start Service clicked");

				editor1.putBoolean("shut_down_service", false);
				editor1.commit();
			} else {

				stopMonitoring();

			}
		} else if (id == R.id.manual) {
			new CarTask().execute();
			Intent intent = new Intent(Main.this, Main.class);
			startActivity(intent);
		} else if (id == R.id.navigate) {
			Intent i = new Intent(
					Intent.ACTION_VIEW,
					Uri.parse("http://maps.google.com/maps?" + "saddr=" + lat
							+ "," + longi + "&daddr=" + carLat + "," + carLongi));
			startActivity(i);
		} else if (id == R.id.satellite) {
			if (mapType == GoogleMap.MAP_TYPE_NORMAL) {

				editor1.putInt("map_type", GoogleMap.MAP_TYPE_HYBRID);
				editor1.commit();

			} else {

				editor1.putInt("map_type", GoogleMap.MAP_TYPE_NORMAL);
				editor1.commit();

			}
			Intent intent1 = new Intent(Main.this, Main.class);
			startActivity(intent1);
		}
	}

	public static void makeSureGPSIsOn(Context c) {
		LocationManager service = (LocationManager) c
				.getSystemService(LOCATION_SERVICE);
		boolean enabled = service
				.isProviderEnabled(LocationManager.GPS_PROVIDER);

		// Check if enabled and if not send user to the GSP settings
		// Better solution would be to display a dialog and suggesting to
		// go to the settings
		if (!enabled) {
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			c.startActivity(intent);
		}

	};

	public void stopMonitoring() {

		start.setText("Start Monitoring");
		monitor.setBackgroundColor(Color.RED);

		if (nm != null)
			nm.cancel(UNIQUE_ID);

		if (mgr != null && pi != null)
			mgr.cancel(pi);

		// cancel service if running
		editor1.putBoolean("shut_down_service", true);
		editor1.commit();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);

		myMenu = menu;

		int setting = data1.getInt(WAKE_LOCK_SETTING,
				PowerManager.PARTIAL_WAKE_LOCK);

		if (setting == PowerManager.PARTIAL_WAKE_LOCK) {

			myMenu.findItem(R.id.set_wakelock).setTitle(
					"Change to FULL_WAKE_LOCK");

		} else {

			myMenu.findItem(R.id.set_wakelock).setTitle(
					"Change to PARTIAL_WAKE_LOCK");

		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int itemId = item.getItemId();
		if (itemId == R.id.sleep) {
			Intent intent = new Intent(Main.this, SleepTimeDialog.class);
			startActivity(intent);
			return true;
		} else if (itemId == R.id.no_sleep) {
			editor1.putInt(Main.SLEEP_MODE_KEY, Main.SLEEP_MODE_FALSE);
			editor1.commit();
		} else if (itemId == R.id.set_wakelock) {
			int setting = data1.getInt(WAKE_LOCK_SETTING,
					PowerManager.PARTIAL_WAKE_LOCK);
			if (setting == PowerManager.PARTIAL_WAKE_LOCK) {

				editor1.putInt(WAKE_LOCK_SETTING, PowerManager.FULL_WAKE_LOCK);
				editor1.commit();

				myMenu.findItem(R.id.set_wakelock).setTitle(
						"Change to PARTIAL_WAKE_LOCK");

			} else {

				editor1.putInt(WAKE_LOCK_SETTING,
						PowerManager.PARTIAL_WAKE_LOCK);
				editor1.commit();

				myMenu.findItem(R.id.set_wakelock).setTitle(
						"Change to FULL_WAKE_LOCK");

			}
		} else if (itemId == R.id.legal) {
			String LicenseInfo = GooglePlayServicesUtil
					.getOpenSourceSoftwareLicenseInfo(getApplicationContext());
			Builder LicenseDialog = new Builder(
					Main.this);
			LicenseDialog.setTitle("Legal Notices");
			LicenseDialog.setMessage(LicenseInfo);
			LicenseDialog.show();
			return true;
		} else if (itemId == R.id.info) {
			String info = getString(R.string.how_to);
			Builder InfoDialog = new Builder(Main.this);
			InfoDialog.setTitle("Legal Notices");
			InfoDialog.setMessage(info);
			InfoDialog.show();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	public static boolean checkSleeping(SharedPreferences data1) {

		int sleepMode = data1.getInt(SLEEP_MODE_KEY, SLEEP_MODE_FALSE);

		if (sleepMode == SLEEP_MODE_TRUE) {

			long curTime = System.currentTimeMillis();
			int begHour = data1.getInt(START_SLEEP_HOUR_KEY, 0);
			int begMin = data1.getInt(START_SLEEP_MINUTE_KEY, 0);
			int endHour = data1.getInt(END_SLEEP_HOUR_KEY, 0);
			int endMin = data1.getInt(END_SLEEP_MINUTE_KEY, 0);

			Date bT = getDate(begHour, begMin);
			long end = getNextServiceTime(begHour, begMin, endHour, endMin);
			Date eT = new Date(end);

			long begTime = bT.getTime();
			long endTime = eT.getTime();

			// for testing purposes
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm aaa");
			Log.i("myCode", "Start sleep: " + bT);
			Log.i("myCode", "End sleep: " + eT);
			Log.i("myCode",
					"Current Time: "
							+ df.format(Calendar.getInstance().getTime()));

			if (begTime < curTime && curTime < endTime)
				return true;

		}
		return false;
	}

	public static long getNextServiceTime(int begHour, int begMin, int endHour,
			int endMin) {

		// determine if startup hour/minute is greater or less than than begin
		// sleep
		// hour/minute.
		// if greater, set alarm for same day, if less, set for next day.

		long result = -1;
		Calendar c = Calendar.getInstance();
		Date d1 = getDate(endHour, endMin);

		c.setTime(d1);

		if (endHour < begHour) {
			// set wakeup for next day

			c.add(Calendar.DATE, 1);
			d1 = c.getTime();

			result = d1.getTime();

		} else if (endHour > begHour) {
			d1 = c.getTime();
			result = d1.getTime();
		} else {

			if (endMin < begMin) {
				c.add(Calendar.DATE, 1);
				d1 = c.getTime();
				result = d1.getTime();
			} else {
				d1 = c.getTime();
				result = d1.getTime();
			}

		}

		return result;

	}

	public static Date getDate(int hour, int minute) {

		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minute);

		return c.getTime();

	}

	@Override
	public void onLocationChanged(Location location) {

		lat = location.getLatitude();
		longi = location.getLongitude();

		yourPos = new LatLng(lat, longi);

		youMarker.setPosition(yourPos);

		float[] results = new float[1];

		Location.distanceBetween(carLat, carLongi, lat, longi, results);
		d = results[0];

		if (carLat == 0 && carLongi == 0) {
			distance.setText("");

		} else {
			setLine();
			distance.setText("Distance to car: " + Math.round(d) + " m");
		}

		setZoom();

	}

	public void setZoom() {

		if (carLat == 0 && carLongi == 0) {

			CameraUpdate center = CameraUpdateFactory.newLatLng(yourPos);
			CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
			carMarker.setVisible(false);

			map.moveCamera(center);
			map.animateCamera(zoom);

		} else if (carLat == lat && carLongi == longi) {
			CameraUpdate center = CameraUpdateFactory.newLatLng(yourPos);
			CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
			map.moveCamera(center);
			map.animateCamera(zoom);
		} else {
			bounds = getBounds(lat, longi, carLat, carLongi);
			map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

			if (!carMarker.isVisible())
				carMarker.setVisible(true);

		}

	}

	public void setLine() {
		if (!(carLat == 0 && carLongi == 0 || (carLat == lat && carLongi == longi))) {
			if (polyline == null)
				polyline = map.addPolyline(rectOptions);

			List<LatLng> points = new ArrayList<LatLng>();
			points.add(yourPos);
			points.add(carPos);
			polyline.setPoints(points);
		}

	}

	public LatLngBounds getBounds(double lat1, double lng1, double lat2,
			double lng2) {

		LatLng otherSidePos = new LatLng(2 * lat1 - lat2, 2 * lng1 - lng2);

		LatLngBounds bounds = new LatLngBounds.Builder().include(yourPos)
				.include(carPos).include(otherSidePos).build();

		return bounds;
	}

	public static String getReadableDate(Date d) {
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
		String text = df.format(d);

		return text;
	}

	// /Check if GoogleMaps is installed methods

	public boolean isGoogleMapsInstalled() {
		try {
			ApplicationInfo info = getPackageManager().getApplicationInfo(
					"com.google.android.apps.maps", 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {

			Builder builder = new Builder(this);
			builder.setMessage("Install Google Maps");
			builder.setCancelable(false);
			builder.setPositiveButton("Install",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(
									Intent.ACTION_VIEW,
									Uri.parse("market://details?id=com.google.android.apps.maps"));
							startActivity(intent);

							// Finish the activity so they can't circumvent the
							// check
							finish();

						}
					});
			AlertDialog dialog = builder.create();
			dialog.show();

			return false;
		}
	}

	// ///////////

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void activate(OnLocationChangedListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deactivate() {
		// TODO Auto-generated method stub

	}

	// set car position
	// manually////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private class CarTask extends AsyncTask<Void, Void, Void> implements
			LocationListener {

		private LocationManager manager;
		private Criteria criteria;
		private String provider;
		private Location location;

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub

			location = manager.getLastKnownLocation(provider);
			if (location != null) {
				onLocationChanged(location);

			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

			if (carLt == 0 && carLg == 0) {
				Toast.makeText(Main.this,
						"Failed aquiring location, try again.",
						Toast.LENGTH_SHORT).show();
				// progress.setVisibility(View.GONE);
			} else {

				// progress.setVisibility(View.GONE);
				editor1.putString("carLat", String.valueOf(carLt));
				editor1.putString("carLng", String.valueOf(carLg));
				editor1.putString("your_lat", String.valueOf(carLt));
				editor1.putString("your_long", String.valueOf(carLg));
				editor1.commit();
				Toast.makeText(Main.this, "Car location set.",
						Toast.LENGTH_SHORT).show();

			}

		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();

			// progress.setVisibility(View.VISIBLE);
			stopMonitoring();
			manager = (LocationManager) getSystemService(LOCATION_SERVICE);

			criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
			criteria.setAltitudeRequired(false);
			criteria.setBearingRequired(false);
			criteria.setCostAllowed(false);

			provider = manager.getBestProvider(criteria, false);

		}

		@Override
		public void onLocationChanged(Location location) {
			if (location != null) {

				carLt = (location.getLatitude());
				carLg = (location.getLongitude());
			}

		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}

	}
	// set car position
	// manually////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
