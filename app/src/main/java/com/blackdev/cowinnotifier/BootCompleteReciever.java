package com.blackdev.cowinnotifier;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;
import java.util.TimeZone;

import static android.content.Context.ALARM_SERVICE;

public class BootCompleteReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if( intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            setAlarmsAgain(context);
        }
    }

    private void setAlarmsAgain(Context context) {
        SharedPreferences mySharedPref = context.getSharedPreferences("Pincodes", context.MODE_PRIVATE);
        final int count = mySharedPref.getInt("Count",0);
        for(int i=0;i<count;i++) {
            final int pincode = Integer.parseInt(mySharedPref.getString("pinCode"+i,"0"));
            final float interval = mySharedPref.getFloat("interval"+i,5);
            final int age1 = mySharedPref.getInt("age"+i,18);
            Calendar calendar = null;

            // check if its greater than current time or not
            calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+5:30"));
            calendar.setTime(calendar.getTime());
            calendar.set(Calendar.SECOND, 0);

            Intent intent = new Intent(context, NotifierAlarm.class);
            intent.putExtra("Pincode", pincode);
            intent.putExtra("Age", age1);
            PendingIntent intent1 = PendingIntent.getBroadcast(context
                    , pincode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            Log.i("Date", "CALENDAR: " + calendar.getTime().toString()+" Pin "+pincode);
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), (long) ((AlarmManager.INTERVAL_FIFTEEN_MINUTES)*((float)interval/15)), intent1);
            //alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + i*10*1000, intent1);
        }
        Log.i("Setting Time Table","Inserted Successfully");

    }
}
