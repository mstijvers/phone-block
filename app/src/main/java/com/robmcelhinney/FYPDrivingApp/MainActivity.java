package com.robmcelhinney.FYPDrivingApp;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.math.BigDecimal;
import java.util.*;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    //    private NotificationManager mNotificationManager;
    private TextToSpeech textToSpeech;

    private TextView greatestProb;
    private TextView sittingcarTextView;
    private TextView currText;
    private TextView BTtextView;

    private Switch switchPant;
    private Switch switchShirt;
    private Switch switchDetection;
    private Switch switchBT;
    private Switch switchOtherApps;

    private ToggleButton toggleButtonActive;

    private Button appsButton;

    private int notificationId = 1;

    public static final String MY_PREFS_NAME = "MyPrefsFile";
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    public final static int REQUEST_CODE_OVERLAY = 123;
    public final static int REQUEST_CODE_USAGE = 124;

    public static final String CHANNEL_ID = "com.robmcelhinney.FYPDrivingApp.ANDROID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startDisturbService();

        startDNDService();

        startUtiliesService();

        settings = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        editor = settings.edit();


        // Splash Screen first time launch
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean("pref_previously_started", false);
        if (!previouslyStarted) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean("pref_previously_started", Boolean.TRUE);
            edit.commit();
            startActivity(new Intent(MainActivity.this, PermissionsSplashActivity.class));
        }


        greatestProb = findViewById(R.id.greatestProb);
        sittingcarTextView = findViewById(R.id.sittingcar_prob);
        currText = findViewById(R.id.currText);
        BTtextView = findViewById(R.id.BTtextView);

        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setLanguage(Locale.US);

        toggleButtonActive = (ToggleButton) findViewById(R.id.toggleButtonActive);
        toggleButtonActive.setChecked(settings.getBoolean("buttonActive", false));
        toggleButtonActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                DisturbService.doNotDisturb();
            } else {
                DisturbService.userSelectedDoDisturb();
            }
            }
        });

        appsButton = (Button) findViewById(R.id.appsButton);
        final Intent installedAppsActivityIntent = new Intent(this, InstalledAppsActivity.class);
        appsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(installedAppsActivityIntent);
            }
        });

        switchPant = (Switch) findViewById(R.id.switchPant);
//        switchPant.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if(isChecked) {
//                    startDetectDrivingService();
//                }
//                else {
//                    stopDetectDrivingService();
//                }
//            }
//
//        });

        switchShirt = (Switch) findViewById(R.id.switchShirt);
/*        switchShirt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                }
                else {
                }
            }

        });*/


        switchDetection = (Switch) findViewById(R.id.switchDetection);
        switchDetection.setChecked(settings.getBoolean("switchkey", false));
        switchDetection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                startDetectDrivingService();
                editor.putBoolean("switchkey", true);
            } else {
                stopDetectDrivingService();
                editor.putBoolean("switchkey", false);
            }
            editor.commit();
            }
        });

        switchBT = (Switch) findViewById(R.id.switchBT);
        switchBT.setChecked(settings.getBoolean("switchBT", false));
        switchBT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        editor.putBoolean("switchBT", isChecked ?  true : false);
        editor.commit();
            }
        });

        switchOtherApps = (Switch) findViewById(R.id.switchOtherApps);
        switchOtherApps.setChecked(settings.getBoolean("switchOtherApps", false));
        switchOtherApps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                //checks if is view app running in Foreground.
                // TODO add explanation of why permission is needed.
                try {
                    ApplicationInfo applicationInfo = MainActivity.this.getPackageManager().getApplicationInfo(MainActivity.this.getPackageName(), 0);
                    if(((AppOpsManager) MainActivity.this.getSystemService(Context.APP_OPS_SERVICE))
                            .checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName)
                            != AppOpsManager.MODE_ALLOWED) {
                        Toast.makeText(MainActivity.this, "Please grant permission in order to block other applications while driving", Toast.LENGTH_LONG).show();
                        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), REQUEST_CODE_USAGE);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                //checks if is allowed to overlay on top of other apps, if not then send user to settings.
                // TODO add explanation of why permission is needed.
                if(!Settings.canDrawOverlays(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "Please grant permission in order to block other applications while driving", Toast.LENGTH_LONG).show();
                    startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION), REQUEST_CODE_OVERLAY);
                }
                editor.putBoolean("switchOtherApps", true);
            } else {
                editor.putBoolean("switchOtherApps", false);
            }
            editor.commit();
            }
        });

//        myBroadcastReceiver = new MyBroadcastReceiver();
        //register BroadcastReceiver for ActivityRecognizedService
//        IntentFilter intentFilter = new IntentFilter(ActivityRecognizedService.ACTION_ActivityRecognizedService);
//        intentFilter.addAction(ActivityRecognizedService.ACTION_ActivityRecognizedService);
//        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
//        registerReceiver(myBroadcastReceiver, intentFilter);

        if (switchDetection.isChecked()) {
            startDetectDrivingService();
        }


//        // checks if user gave permission to change notification policy. If not, then launch
//        // settings to get them to give permission.
//        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
//            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
//            startActivity(intent);
//        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("intentKey"));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiverToggleButton, new IntentFilter("intentToggleButton"));
    }

    private void startUtiliesService() {
        Intent intent = new Intent(this, UtilitiesService.class);
        startService(intent);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        // Get extra data included in the Intent
        String message = intent.getStringExtra("text");
        if (intent.hasExtra("BTText")) {
            BTtextView.setText(message);
        } else if (intent.hasExtra("currText")) {
            currText.setText(message);
        } else if (intent.hasExtra("greatestProb")) {
            greatestProb.setText(message);
        } else if (intent.hasExtra("sittingCarText")) {
            sittingcarTextView.setText(message);
        }
        }
    };

    private BroadcastReceiver mMessageReceiverToggleButton = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        // Get extra data included in the Intent
        boolean value = intent.getBooleanExtra("valueBool", false);
        toggleButtonActive.setChecked(value);
        editor.putBoolean("buttonActive", value).apply();
        editor.commit();
        }
    };


    @Override
    public void onInit(int status) {

    }


    private static float round(float d) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private SensorManager getSensorManager() {
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (textToSpeech != null) {

            textToSpeech.stop();
            textToSpeech.shutdown();
        }


        //un-register BroadcastReceiver
//        unregisterReceiver(myBroadcastReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    public void startDNDService() {
        Intent intent = new Intent(this, ChangeDNDService.class);
        startService(intent);
    }

    public void stopDNDService() {
        Intent intent = new Intent(this, ChangeDNDService.class);
        stopService(intent);
    }

    public void startDetectDrivingService() {
        Intent intent = new Intent(this, DetectDrivingService.class);
        startService(intent);
    }

    public void stopDetectDrivingService() {
        Intent intent = new Intent(this, DetectDrivingService.class);
        stopService(intent);
    }

    public void startDisturbService() {
        Intent intent = new Intent(this, DisturbService.class);
        startService(intent);
    }

    public void stopDisturbService() {
        Intent intent = new Intent(this, DisturbService.class);
        stopService(intent);
    }


    private void displayNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setContentText(message);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(getString(R.string.app_name));
        NotificationManagerCompat.from(this).notify(notificationId, builder.build());
        notificationId++;
    }

    public void saveInfo() {
        SharedPreferences sharedPref = getSharedPreferences("activeInfo", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("activeBool", UtilitiesService.isActive());

        editor.apply();
    }




//    private void userInstalledApps() {
//        List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(0);
//
//        List<ApplicationInfo> installedApps = new ArrayList<ApplicationInfo>();
//
//        for(ApplicationInfo app : apps) {
//            //checks for flags; if flagged, check if updated system app
//            if((app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
//                installedApps.add(app);
//                //it's a system app, not interested
//            } else if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
//                //Discard this one
//                //in this case, it should be a user-installed app
//            } else {
//                installedApps.add(app);
//            }
//        }
//    }


}

