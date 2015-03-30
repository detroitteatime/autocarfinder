package com.detroitteatime.autocarfinder;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

public class SleepTimeDialog extends Activity implements OnTimeChangedListener,
		OnClickListener {

	private TimePicker startTimePicker, endTimePicker;
	private Button set, cancel;
	int begHour, endHour, begMin, endMin;
	SharedPreferences data1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.time_dialog);

		data1 = this.getSharedPreferences("storage", 0);
		begHour = data1.getInt(Main.START_SLEEP_HOUR_KEY, 0);
		endHour = data1.getInt(Main.END_SLEEP_HOUR_KEY, 0);
		begMin = data1.getInt(Main.START_SLEEP_MINUTE_KEY, 0);
		endMin = data1.getInt(Main.END_SLEEP_MINUTE_KEY, 0);

		// set up timepickers
		startTimePicker = (TimePicker) findViewById(R.id.begPicker);
		startTimePicker.setCurrentHour(begHour);
		startTimePicker.setCurrentMinute(begMin);
		startTimePicker.setOnTimeChangedListener(this);
		

		endTimePicker = (TimePicker) findViewById(R.id.endPicker);
		endTimePicker.setCurrentHour(endHour);
		endTimePicker.setCurrentMinute(endMin);
		endTimePicker.setOnTimeChangedListener(this);
		

		// set up buttons

		set = (Button) findViewById(R.id.set);
		set.setOnClickListener(this);

		cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(this);

	}

	@Override
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {

		int id = view.getId();
		if (id == R.id.begPicker) {
			begHour = hourOfDay;
			begMin = minute;
		} else if (id == R.id.endPicker) {
			endHour = hourOfDay;
			endMin = minute;
		}

	}

	@Override
	public void onClick(View arg0) {

		int id = arg0.getId();
		if (id == R.id.set) {
			Editor editor1 = data1.edit();
			editor1.putInt(Main.START_SLEEP_HOUR_KEY, begHour);
			editor1.putInt(Main.END_SLEEP_HOUR_KEY, endHour);
			editor1.putInt(Main.START_SLEEP_MINUTE_KEY, begMin);
			editor1.putInt(Main.END_SLEEP_MINUTE_KEY, endMin);
			editor1.putInt(Main.SLEEP_MODE_KEY, Main.SLEEP_MODE_TRUE);
			editor1.commit();
			// create or get alarm to wake up and update.
			//start service to reset alarm
			startService(new Intent(SleepTimeDialog.this, SensorService.class));
			finish();
		} else if (id == R.id.cancel) {
			finish();
		}
	}

}
