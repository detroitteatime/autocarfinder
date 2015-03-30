package com.detroitteatime.autocarfinder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.detroitteatime.autocarfinder.R;


public class MyMapActivity extends FragmentActivity implements LocationSource,
		LocationListener {

	GoogleMap map;
	long start;
	long stop;

	int x, y;

	Drawable car, you;
	public static final int ZOOM_LEVEL = 20;
	private LocationManager manager;
	private Criteria criteria;
	private String provider;

	private SharedPreferences storage;
	private SharedPreferences.Editor editor;

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

	Marker youMarker;
	Circle youCircle;

	boolean first = true;
	private double d;
	private int mapType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		storage = getSharedPreferences("storage", 0);
		editor = storage.edit();

		RelativeLayout mainLayout = (RelativeLayout) this.getLayoutInflater()
				.inflate(R.layout.main, null);

		// set a global layout listener which will be called when the layout
		// pass is completed and the view is drawn

		setContentView(mainLayout);

		FragmentManager myFragmentManager = getSupportFragmentManager();
		SupportMapFragment mySupportMapFragment = (SupportMapFragment) myFragmentManager
				.findFragmentById(R.id.map);
		map = mySupportMapFragment.getMap();

		mapType = storage.getInt("map_type", GoogleMap.MAP_TYPE_NORMAL);

		map.setMapType(mapType);

		you = getResources().getDrawable(R.drawable.you);// set to person
		car = getResources().getDrawable(R.drawable.car);// set to car

		// placing pinpoint at location

		carLat = Double.valueOf(storage.getString("carLat", "0"));
		carLongi = Double.valueOf(storage.getString("carLng", "0"));

		Log.i("My Code", "Car lat: " + carLat);
		Log.i("My Code", "Car long: " + carLongi);

		carPos = new LatLng(carLat, carLongi);

		lat = Double.valueOf(storage.getString("your_lat",
				String.valueOf(carLat)));
		longi = Double.valueOf(storage.getString("your_long",
				String.valueOf(carLongi)));

		yourPos = new LatLng(lat, longi);

		// get distance between two points
		float[] results = new float[1];

		Location.distanceBetween(carLat, carLongi, lat, longi, results);
		d = results[0];

		if (carLat != 0 || carLongi != 0) {

			map.addMarker(new MarkerOptions().position(carPos).icon(
					BitmapDescriptorFactory.fromResource(R.drawable.car)));

		} else {
			Toast.makeText(this, "Car location not available",
					Toast.LENGTH_LONG).show();

		}

		lat = Double.valueOf(storage.getString("your_lat",
				String.valueOf(carLat)));
		longi = Double.valueOf(storage.getString("your_long",
				String.valueOf(carLongi)));

		yourPos = new LatLng(lat, longi);

		if (lat != 0 || longi != 0) {

			youMarker = map
					.addMarker(new MarkerOptions().position(yourPos).icon(
							BitmapDescriptorFactory
									.fromResource(R.drawable.you)));

			map.moveCamera(CameraUpdateFactory.newLatLng(yourPos));

		CircleOptions circleOptions = new CircleOptions().center(yourPos) // set
					// center
					.radius(d * 1.1) // set radius in meters
					.fillColor(Color.TRANSPARENT) // default
					.strokeColor(0x553377ff).strokeWidth(5);

			youCircle = map.addCircle(circleOptions);

		} else {

			Toast.makeText(this, "Your location not available",
					Toast.LENGTH_SHORT).show();

		}

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
		navigate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent i = new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://maps.google.com/maps?" + "saddr=" + lat
								+ "," + longi + "&daddr=" + carLat + ","
								+ carLongi));
				startActivity(i);

			}
		});

		type = (Button) findViewById(R.id.satellite);
		type.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mapType == GoogleMap.MAP_TYPE_NORMAL) {

					editor.putInt("map_type", GoogleMap.MAP_TYPE_HYBRID);
					editor.commit();

				} else {

					editor.putInt("map_type", GoogleMap.MAP_TYPE_NORMAL);
					editor.commit();

				}

				Intent intent = new Intent(MyMapActivity.this,
						MyMapActivity.class);

				startActivity(intent);

			}
		});

		if (mapType == GoogleMap.MAP_TYPE_NORMAL) {

			type.setText("Change to hybrid");

		} else {

			type.setText("Change to normal");

		}
		mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					public void onGlobalLayout() {

						setZoom(lat, longi);
					}
				});

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();

		manager.removeUpdates(this);

	}

	@Override
	protected void onResume() {

		super.onResume();

		first = true;

		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(getApplicationContext());

		if (resultCode == ConnectionResult.SUCCESS) {
			Toast.makeText(getApplicationContext(),
					"isGooglePlayServicesAvailable SUCCESS", Toast.LENGTH_LONG)
					.show();

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

	}

	@Override
	public void onLocationChanged(Location location) {

		lat = location.getLatitude();
		longi = location.getLongitude();

		yourPos = new LatLng(lat, longi);

		youMarker.setPosition(yourPos);

		if (first) {

			float[] results = new float[1];

			Location.distanceBetween(carLat, carLongi, lat, longi, results);
			d = results[0];
			youCircle.setRadius(d * 1.1);
			setZoom(lat, longi);

			first = false;

		}
		youCircle.setCenter(yourPos);

	}

	public void setZoom(double lt, double lg) {

		if (lt == carLat || lg == carLongi) {

			map.animateCamera(CameraUpdateFactory.newLatLng(carPos));

			map.animateCamera(CameraUpdateFactory.zoomTo(20));

		} else {

			yourPos = new LatLng(lt, lg);

			youMarker.setPosition(yourPos);

			// bounds = new LatLngBounds.Builder().include(carPos)
			// .include(yourPos).build();

			bounds = getBounds(lt, lg, carLat, carLongi);
			map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
		}

	}
	
	public LatLngBounds getBounds(double lat1, double lng1, double lat2,
			double lng2) {
		
		LatLng otherSidePos = new LatLng(2*lat1 - lat2, 2*lng1 - lng2);
		
		LatLngBounds bounds = new LatLngBounds.Builder().include(yourPos).include(carPos).include(otherSidePos)
                .build();
		
		return bounds;
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


	@Override
	public void activate(OnLocationChangedListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deactivate() {
		// TODO Auto-generated method stub

	}

}
