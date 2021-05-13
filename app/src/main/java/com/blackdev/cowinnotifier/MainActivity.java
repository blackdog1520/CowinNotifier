package com.blackdev.cowinnotifier;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionItemTarget;
import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    MaterialButton button;
    RadioGroup intervalRadioGroup, ageGroup;
    TextInputEditText pincodeEditText;
    TextInputLayout pincodeLayout;
    ListView enteredPincodes;
    LottieAnimationView lottieAnimationView;
    SharedPreferences mySharedPref;
    SharedPreferences.Editor editor;
    ArrayAdapter<String> adapter;
    ExtendedFloatingActionButton fab;
    LinearLayout parent;
    private FirebaseAnalytics mFirebaseAnalytics;
    String[] prevCodes;
    boolean firstTime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
//        AdView adView = findViewById(R.id.adView);
//        AdView adView1 = findViewById(R.id.adView2);
//        adView.loadAd(new AdRequest.Builder().build());
//        adView1.loadAd(new AdRequest.Builder().build());

        fab = findViewById(R.id.book_slot_fab);
        parent = findViewById(R.id.parent);
        lottieAnimationView = findViewById(R.id.lottie);
        button = findViewById(R.id.submit_button);
        intervalRadioGroup = findViewById(R.id.interval_group);
        ageGroup = findViewById(R.id.age_group);
        pincodeEditText = findViewById(R.id.pincodeEditText);
        pincodeLayout = findViewById(R.id.pincodeLayout);
        enteredPincodes = findViewById(R.id.entered_pincode_list);
        mySharedPref = this.getSharedPreferences("Pincodes", MODE_PRIVATE);
        firstTime = mySharedPref.getBoolean("FirstTime",true);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        editor = mySharedPref.edit();
        if(firstTime) {
            intervalRadioGroup.setVisibility(View.INVISIBLE);
            ageGroup.setVisibility(View.INVISIBLE);
            pincodeLayout.setVisibility(View.INVISIBLE);
            pincodeEditText.setVisibility(View.INVISIBLE);
            lottieAnimationView.setVisibility(View.INVISIBLE);
            new ShowcaseView.Builder(this)
                    .setTarget(new ViewTarget( ((View) findViewById(R.id.submit_button)) ))
                    .setContentTitle("Submit Button")
                    .setContentText("After entering Pin code and interval we keep checking the available slots for the next 7 days.You will receive a notification when slots will be available to any Pin code.")
                    .hideOnTouchOutside()
                    .build().setOnShowcaseEventListener(new OnShowcaseEventListener() {
                @Override
                public void onShowcaseViewHide(ShowcaseView showcaseView) {
                    intervalRadioGroup.setVisibility(View.VISIBLE);
                    ageGroup.setVisibility(View.VISIBLE);
                    pincodeLayout.setVisibility(View.VISIBLE);
                    pincodeEditText.setVisibility(View.VISIBLE);
                    lottieAnimationView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                }

                @Override
                public void onShowcaseViewShow(ShowcaseView showcaseView) {
                }

                @Override
                public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

                }
            });
            editor.putBoolean("FirstTime",false);
            editor.apply();
            parent.setAlpha(1f);
        }
        editor = mySharedPref.edit();
        final int count = mySharedPref.getInt("Count",0);
        prevCodes = new String[count];
        Log.i("MainActivity",""+count);
        for(int i=0;i<count;i++) {
            final String pincode = mySharedPref.getString("pinCode"+i,"");
            final float interval = mySharedPref.getFloat("interval"+i,10);
            final int age1 = mySharedPref.getInt("age"+i,18);
            prevCodes[i] = "Pin code: "+pincode + " for age " +age1+ "+";
            //alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + i*10*1000, intent1);
        }

        adapter = new ArrayAdapter<String>(this, R.layout.list_item_pin, android.R.id.text1, prevCodes);
        enteredPincodes.setAdapter(adapter);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkData()) {
                    final String pincode = pincodeEditText.getText().toString();
                    final int count = mySharedPref.getInt("Count",0);

                    float interval = 5;
                    switch (intervalRadioGroup.getCheckedRadioButtonId()) {
                        case R.id.time_1:
                            interval = (float) 1;
                            break;
                        case R.id.time_3:
                            interval = 3;
                            break;
                        case R.id.time_5:
                            interval = 5;
                            break;
                        case R.id.time_10:
                            interval = 10;
                            break;
                    }

                    int age = 18;
                    if (ageGroup.getCheckedRadioButtonId() == R.id.age_45) {
                        age = 45;
                    }
                    editor.putInt("Count",count+1);
                    editor.putString("pinCode"+count,pincode);
                    editor.putFloat("interval"+count,interval);
                    editor.putInt("age"+count,age);
                    editor.apply();
                    prevCodes = new String[count+1];
                    Log.i("MainActivity",""+count);
                    for(int i=0;i<count+1;i++) {
                        final String pincode1 = mySharedPref.getString("pinCode"+i,"");
                        final float interval1 = mySharedPref.getFloat("interval"+i,5);
                        final int age1 = mySharedPref.getInt("age"+i,18);
                        prevCodes[i] = "Pin code: "+pincode1 + " for age " +age1+ "+";
                        //alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + i*10*1000, intent1);
                    }

                    adapter = new ArrayAdapter<String>(MainActivity.this, R.layout.list_item_pin, android.R.id.text1, prevCodes);
                    enteredPincodes.setAdapter(adapter);
                    addToQueue(interval,Integer.parseInt(pincode),age);
                    pincodeEditText.setText("");
                    ageGroup.clearCheck();
                    intervalRadioGroup.clearCheck();

                } else {
                    Toast.makeText(MainActivity.this, "Please fill details",Toast.LENGTH_SHORT).show();
                }
            }
        });


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               startActivity(new Intent(MainActivity.this,SlotRegistrationActivity.class));
            }
        });

    }

    private void addToQueue(float interval, int pincode, int age) {
        Calendar calendar = null;

        calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+5:30"));
        calendar.setTime(calendar.getTime());
        calendar.set(Calendar.SECOND,0);


        Intent intent = new Intent(this, NotifierAlarm.class);
        intent.putExtra("Pincode", pincode);
        intent.putExtra("Age", age);
        PendingIntent intent1 = PendingIntent.getBroadcast(this, pincode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        //Log.i("Date", "" + newDate.getTime().toString() + "CALENDAR: " + calendar.getTime().toString());
        Log.i("MainActivityCowin",""+(long) ((AlarmManager.INTERVAL_FIFTEEN_MINUTES)*((float)interval/15)));
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(), (long) ((AlarmManager.INTERVAL_FIFTEEN_MINUTES)*((float)interval/15)),intent1);
        //alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + i*10*1000, intent1);

        Toast.makeText(this,"Inserted Successfully",Toast.LENGTH_SHORT).show();
    }

    private boolean checkData() {
        final String pincode = pincodeEditText.getText().toString();
        if(pincode == null || pincode.isEmpty() ){
            pincodeLayout.setError("Required");
            pincodeLayout.setFocusable(true);
            return false;
        } else if( pincode.length() != 6) {
            pincodeLayout.setError("Incorrect Pincode");
            pincodeLayout.setFocusable(true);
            return false;
        } else {
            for (int i = 0; i < 6; i++) {
                if (pincode.charAt(i) < '0' && pincode.charAt(i) > '9') {
                    pincodeLayout.setError("Incorrect Pincode");
                    pincodeLayout.setFocusable(true);
                    return false;
                }
            }
            return true;
        }
    }
}