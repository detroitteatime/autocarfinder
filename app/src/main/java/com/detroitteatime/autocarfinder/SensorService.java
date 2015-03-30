package com.detroitteatime.autocarfinder;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

public class SensorService extends Service implements LocationListener,
		SensorEventListener {

	private LocationManager manager;
	private Criteria criteria;
	private String provider;
	private double lattitude;
	private double longitude;

	private double linearX;
	private double linearY;
	private double linearZ;
	private double sumLin;
	private double avgAcc;

	private SensorManager sensorManager;
	private Sensor sensorLinear;
	private double speed;
	private boolean driving;
	private boolean sensorRegistered;
	private long timeParked;
	private CountDownTimer timer;

	private WakeLock mWakeLock;
	private PowerManager pm;

	private ArrayBlockingQueue<Double> queue;
	private ArrayBlockingQueue<Double> altQueue;

	private SharedPreferences data1;
	private SharedPreferences.Editor editor1;

	private final IBinder mBinder = new LocalBinder();

	private int wakeLockType = PowerManager.PARTIAL_WAKE_LOCK;
	private WakeLock fWakelock;
	private boolean beenDriving;

	public class LocalBinder extends Binder {
		SensorService getService() {
			return SensorService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();

		data1 = this.getSharedPreferences("storage", 0);
		editor1 = data1.edit();

		manager = (LocationManager) getSystemService(LOCATION_SERVICE);

		criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(false);

		provider = manager.getBestProvider(criteria, false);
		manager.getLastKnownLocation(provider);

		boolean enabled = manager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);

		// Check if enabled and if not send user to the GSP settings
		// Better solution would be to display a dialog and suggesting to
		// go to the settings
		if (!enabled) {
			if (mWakeLock != null && mWakeLock.isHeld())
				mWakeLock.release();
			
			if (fWakelock != null && fWakelock.isHeld())
				fWakelock.release();
			
			this.stopSelf();
		}

		queue = new ArrayBlockingQueue<Double>(3);
		altQueue = new ArrayBlockingQueue<Double>(3);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		if (manager != null) {
			manager.removeUpdates(this);
		}

		disableSensor();

		if (mWakeLock != null && mWakeLock.isHeld())
			mWakeLock.release();
		
		if (fWakelock != null && fWakelock.isHeld())
			fWakelock.release();
		
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		
		beenDriving = false;
		Log.i("myCode", "onStartCommand called");

		if (data1.getBoolean("shut_down_service", true)) {
			// Log.i("myCode", "end service instance");
			disableSensor();
			if (mWakeLock != null && mWakeLock.isHeld())
				mWakeLock.release();
			
			if (fWakelock != null && fWakelock.isHeld())
				fWakelock.release();

			this.stopSelf();
		}

		// check if servie is supposed to be sleeping
		// if so, set next starttime and shutdown

		if (Main.checkSleeping(data1)) {

			long time = Main.getNextServiceTime(
					data1.getInt(Main.START_SLEEP_HOUR_KEY, 0),
					data1.getInt(Main.START_SLEEP_MINUTE_KEY, 0),
					data1.getInt(Main.END_SLEEP_HOUR_KEY, 0),
					data1.getInt(Main.END_SLEEP_MINUTE_KEY, 0));

			Date d = new Date(time);

			Toast.makeText(this, "Time for service to sleep. Waking at: " + d,
					Toast.LENGTH_LONG).show();
			Log.i("myCode", "Time for service to sleep. Waking at: " + d);

			shutDown(time);

			return 0;

		}

		timeParked = System.currentTimeMillis();

		setSensors();

		wakeLockType = data1.getInt(Main.WAKE_LOCK_SETTING,
				PowerManager.PARTIAL_WAKE_LOCK);

		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

		mWakeLock = pm
				.newWakeLock(
						(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP),
						"bbb");

		mWakeLock.acquire();

		manager.requestLocationUpdates(provider, 10000, 1, this);
		Log.i("myCode", "Service started");
		Log.i("myCode", "location updates called every " + 10000 + " msec");

		timer = new CountDownTimer(180000, 180000) {

			public void onTick(long millisUntilFinished) {

			}

			public void onFinish() {

				Log.i("myCode",
						"onLocationChanged never called, shutting down service instance");

				shutDown(System.currentTimeMillis() + 300000);
			}

		};

		timer.start();

		// Toast.makeText(this, "Monitoring", Toast.LENGTH_LONG).show(); for
		// testing

		return super.onStartCommand(intent, flags, startId);

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		Sensor source = event.sensor;

		if (source.equals(sensorLinear)) {
			linearX = event.values[0];
			linearY = event.values[1];
			linearZ = event.values[2];

			sumLin = Math.abs(linearX) + Math.abs(linearY) + Math.abs(linearZ);

			if (queue.size() == 3)
				queue.poll();

			queue.offer(sumLin);

			// Log.i("myCode", "sumLin: " + sumLin);
			// Log.i("myCode", "Queue: " + queue.toString());

		}

	}

	@Override
	public void onLocationChanged(Location location) {

		timer.cancel();// restart no onLocationChanged called timer
		timer.start();

		if (data1.getBoolean("shut_down_service", false)) {
			Log.i("myCode", "end service instance");
			disableSensor();
			if (mWakeLock != null && mWakeLock.isHeld())
				mWakeLock.release();
			if (fWakelock != null && fWakelock.isHeld())
				fWakelock.release();
			
			this.stopSelf();
		}

		if (location != null) {

			boolean enabled = manager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);

			// Check if enabled and if not send user to the GSP settings
			// Better solution would be to display a dialog and suggesting to
			// go to the settings
			if (!enabled) {
				if (mWakeLock != null && mWakeLock.isHeld())
					mWakeLock.release();
				if (fWakelock != null && fWakelock.isHeld())
					fWakelock.release();
				this.stopSelf();
			}

			lattitude = location.getLatitude();
			longitude = location.getLongitude();

			Log.i("myCode", "OnLocationChanged called: lattitude: " + lattitude
					+ " longitude: " + longitude);

			

			avgAcc = getAvgAcc();

			if (location.hasSpeed())
				speed = location.getSpeed();

			// don't keep service on if person isn't moving.

			// warn user if gps info is bad
			long gpsTime = location.getTime();
			float accuracy = location.getAccuracy();
			if (System.currentTimeMillis() - gpsTime > 10000 || accuracy > 200) {

				Toast.makeText(this, "GPS data may be inaccurate",
						Toast.LENGTH_LONG).show();
			}

			// logic for updating car location

			// The idea here is that if the phone is moving faster than 3 m/s
			// and the acceleration values are lower than 3, then
			// the person is probably driving (the user might also be on a bus
			// or train), as any person on foot moving faster than 3 m/s would
			// jar the phone considerably.

			// initialize carLat or carLong to stored or to lattitude and
			// longitdue if they are zero.

			if (speed > 5) {
				driving = true;

			}

			// when driving
			if (driving) {

				// If the person driving we want to keep track of her location,
				// but we don't want to drain the phone battery.

				if (speed > 5) {
					manager.removeUpdates(this);
					manager.requestLocationUpdates(provider, 10000, 1, this);

					Log.i("myCode", "location updates called every " + 10000
							+ " msec");

				}

				// When driving, the location of the car and the phone are
				// assumed to be the same. carLat et al are updated here.

				// When speed is less than 5 m/s fine mode begins, where
				// location is detected every 1/2 second. Also enable
				// accelerometer here.
				if (speed < 5) {
					enableSensor();
					manager.removeUpdates(this);
					manager.requestLocationUpdates(provider, 500, 1, this);
					Log.i("myCode", "location updates called every " + 500
							+ " msec");

				}

				// If the phone is moving less than 2 m/s and the accelerometer
				// values are high, the user is probably walking/running and
				// stopped driving.

				if (speed < 2 && avgAcc > 3) {
					driving = false;
					beenDriving = true;

					timeParked = System.currentTimeMillis();
					Log.i("myCode", "Car parked. lat: " + lattitude + " long: "
							+ longitude + " speed: " + speed + " accel: "
							+ avgAcc);

					// store location
					editor1.putString("carLat", String.valueOf(lattitude));
					editor1.putString("carLng", String.valueOf(longitude));
					editor1.commit();
					Toast.makeText(this, "Car parked, location set",
							Toast.LENGTH_LONG).show();

				}

				// when not driving
			} else {

				// end service after 1 min, unless moved over 1km from last
				// parked location

				long now = System.currentTimeMillis();

				long diff = now - timeParked;
				
				
				//if previously driving during this instance, stay awake longer than if not. If the service goes immediately 
				//to non-driving mode, shut down quicker
				

				double carLat = Double.valueOf(data1.getString("carLat", "0"));
				double carLongi = Double.valueOf(data1.getString("carLng", "0"));
				
				// get distance between two points
				float[] results = new float[1];

				Location.distanceBetween(carLat, carLongi, lattitude, longitude, results);
				float d = results[0];
				
				Log.i("myCode", "Sensorservice dstance from last parked: " + d);

				
				long delay = 0;
				
				if(beenDriving || d > 500)
					delay = 180000;
				else
					delay = 30000;
				
				

				if (diff > delay) {
					Log.i("myCode", "Shutting down, not driving");
					shutDown(System.currentTimeMillis() + 300000);

				}

			}// end not driving

		}// end has location

	}

	public void shutDown(long value) {
		AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(this, StartUpReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);

		mgr.set(AlarmManager.RTC_WAKEUP, value, pi);

		Log.i("myCode", "Shutdown: end service instance");
		disableSensor();
		
		if (mWakeLock != null && mWakeLock.isHeld())
			mWakeLock.release();
		
		if (fWakelock != null && fWakelock.isHeld())
			fWakelock.release();
		
		if(timer != null)
		timer.cancel();
		
		this.stopSelf();

	}

	public void enableSensor() {
		
		if (wakeLockType == PowerManager.FULL_WAKE_LOCK) {
			fWakelock = pm
					.newWakeLock(
							(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
									| wakeLockType | PowerManager.ACQUIRE_CAUSES_WAKEUP),
							"bbb");
			fWakelock.acquire();
		
		}
		
		
		if (sensorLinear == null) {
			sensorRegistered = false;
		} else if (sensorRegistered) {

		} else {
			sensorManager.registerListener(this, sensorLinear, 500);
			sensorRegistered = true;
		}

	}

	public void disableSensor() {
		
		if (fWakelock != null && fWakelock.isHeld()) {
			
			fWakelock.release();

		
		}

		if (sensorLinear == null) {
			sensorRegistered = false;
		} else if (sensorRegistered) {
			if (sensorManager != null)
				sensorManager.unregisterListener(this);
			sensorRegistered = false;
		}

	}

	public void setSensors() {

		if (sensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION).size() != 0) {
			sensorLinear = sensorManager
					.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

		} else {
			Toast.makeText(this,
					"You don't have an linear acceleration sensor",
					Toast.LENGTH_SHORT).show();
		}

	}

	public double getAvgAcc() {
		double result = 0;
		Object[] accArray = queue.toArray();
		for (int i = 0; i < queue.size(); i++) {
			if (accArray[i] != null)
				result += (Double) accArray[i];
		}

		if (queue.size() != 0)
			result = result / queue.size();
		else
			result = 0;

		return result;
	}

	public double getAvgAlt() {
		double result = 0;
		Object[] altArray = altQueue.toArray();
		for (int i = 0; i < altQueue.size(); i++) {
			if (altArray[i] != null)
				result += (Double) altArray[i];
		}

		if (altQueue.size() != 0)
			result = result / altQueue.size();
		else
			result = 0;

		return result;
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
