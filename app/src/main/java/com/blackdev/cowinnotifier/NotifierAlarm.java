package com.blackdev.cowinnotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import androidx.core.app.NotificationCompat;


public class NotifierAlarm extends BroadcastReceiver {

    private static String TAG = "CoWinResponse";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"Check pincode");
        final int pincode = intent.getIntExtra("Pincode",0);
        final int age = intent.getIntExtra("Age",18);

        checkSlot(pincode,context,age);

//        if(intent.getIntExtra("AlarmNumber",Constants.MAX_ALARM) == Constants.MAX_ALARM || intent.getIntExtra("AlarmNumber",Constants.MAX_ALARM) == Constants.MAX_ALARM - 1 ) {
//            int id = intent.getIntExtra("ChannelID",-1);
//            if(id !=-1) {
//                addFutureClass(reminder, id);
//            }
//        }
    }

    private void checkSlot(int pincode, Context context, int age) {
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        Date date = new Date();
        final String dateString = dateFormat.format(date);
        Log.i("CoWinResponse","No centers available");
        final String URL ="https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByPin?pincode="+pincode+"&date="+dateString;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,URL,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response!=null && response.has("centers")) {
                    // got content
                    try {
                        JSONArray centers = (JSONArray) response.get("centers");
                        if(centers.length() == 0) {
                            Log.i("CoWinResponse","No centers available");
                        } else {
                            for(int i=0;i<centers.length();i++) {
                                JSONObject object = centers.getJSONObject(i);
                                final String name = object.getString("name");
                                if(object.has("sessions")) {
                                    JSONArray sessions = (JSONArray) object.get("sessions");
                                    if(sessions.length() == 0) {
                                        Log.i("CoWinResponse","No sessions available");
                                    } else {
                                        SharedPreferences myShared = context.getSharedPreferences("Sessions",Context.MODE_PRIVATE);
                                        for(int j=0;j<sessions.length();j++) {
                                            final JSONObject currentSession = sessions.getJSONObject(j);
                                            if (currentSession.has("session_id")) {
                                                if(currentSession.getInt("available_capacity")>0 && currentSession.getInt("min_age_limit") == age) {
                                                    if(myShared.getBoolean(currentSession.getString("session_id")+ currentSession.getString("date"),false)) {
                                                        Log.i(TAG,"Already notified");
                                                    } else {
                                                        createNotification(context, pincode, age,name);
                                                        SharedPreferences.Editor editor = myShared.edit();
                                                        editor.putBoolean(currentSession.getString("session_id") + currentSession.getString("date"),true);
                                                        editor.apply();
                                                    }
                                                } else {
                                                    Log.i("CoWinResponse","Criteria not matched");
                                                }
                                            } else {
                                                Log.i("CoWinResponse","No session id available");
                                            }
                                        }
                                    }

                                }
                            }

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.d("CoWinResponse","sendNotification: "+response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("CoWinResponse","sendNotification: "+error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> map = new HashMap<>();
                map.put("accept","application/json");
                return map;
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        request.setRetryPolicy(new DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    void createNotification(Context context, int pincode,int age, String name) {
        {
            Uri alarmsound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

            Intent intent1 = new Intent(context, MainActivity.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
            taskStackBuilder.addParentStack(MainActivity.class);
            taskStackBuilder.addNextIntent(intent1);

            PendingIntent intent2 = taskStackBuilder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "10001");

            NotificationChannel channel = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel("my_channel_01", "Cowin Notifier", NotificationManager.IMPORTANCE_HIGH);
            }
            Notification notification = builder.setContentTitle("Slots Available")
                    .setContentText("Slots for "+age+"+ available now at "+name+" (" + pincode+")").setAutoCancel(true)
                    .setSound(alarmsound).setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentIntent(intent2)
                    .setChannelId("my_channel_01")
                    .build();

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(channel);
            }
            notificationManager.notify(new Random().nextInt(85 - 65), notification);
        }
    }
}
