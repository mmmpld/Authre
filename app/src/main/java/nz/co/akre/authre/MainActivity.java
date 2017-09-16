package nz.co.akre.authre;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.io.*;
import java.net.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

//    public TextView txtView;
//    private NotificationReceiver nReceiver;



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Log.i("CLICK", "going to settings from onOptionsItemSelected");
                // User chose the "Settings" item, show the app settings UI...
                Intent i = new Intent(this, MyPreferenceActivity.class);
                startActivity(i);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // show the main auth log screen
        setContentView(R.layout.log_view);
        // add the top toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        updateLog();
        // listen for log updates
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Log.i("AUTHRE", "preferences changed: " + key);
                if (key.equals("log_text")) {
                    updateLog();
                }
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(listener);

        // ask for notification access if not already granted
        requestNotificationAccess(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unregisterReceiver(nReceiver);
    }

//    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
//        if (key.equals("Log_Text")) {
//            //Preference pref = findPreference(key);
//            //pref.setDefaultValue(prefs.getString(key, "bob"));
//            updateLog();
//        }
//    }

    public void updateLog() {
        // update the log to show the saved log text
        TextView txtView = (TextView) findViewById(R.id.textView);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String logText = prefs.getString("log_text", getString(R.string.default_log_text));
        txtView.setText(logText);
    }

    public void requestNotificationAccess(Context context) {
        if (!NLService.isNotificationAccessEnabled) {
            Log.i("AUTHRE", "We don't have notification access, lets ask for it");
            new AlertDialog.Builder(context)
                    .setTitle("Notification access")
                    .setMessage(getString(R.string.app_name) + " requires notification access to read your Ingress comm notifications. It will ignore any non-Ingress messages. Please allow it on the next screen.")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(R.mipmap.authre_logo)
                    .show();
        } else {
            Log.i("AUTHRE", "We already have notification access!");
        }
    }
}
