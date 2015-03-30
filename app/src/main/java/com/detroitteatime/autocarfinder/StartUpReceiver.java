package com.detroitteatime.autocarfinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartUpReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Intent i = new Intent(arg0, SensorService.class);
		arg0.startService(i);
		
	}

}
