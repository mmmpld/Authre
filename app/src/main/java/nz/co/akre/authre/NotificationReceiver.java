package nz.co.akre.authre;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.DateFormat;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Richard on 02-Sep-16.
 */
public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String agent = intent.getStringExtra("auth_agent");
        String message = intent.getStringExtra("auth_message");
        MessageParser mp = new MessageParser(context);
        if (mp.isAuthMessage(message)) {
            Log.i("AUTH", agent + ": " + message);
            UpdateAuthLog(agent, message, context);
            try {
                MessageSender.requestUrl(agent, message, context);
            } catch (Exception ex) {
                // message didn't post
                Log.e("POST", "Error sending auth post", ex);
            }
            Toast.makeText(context, "Auth for " + agent + " received!", Toast.LENGTH_SHORT).show();
        }
    }

    private void UpdateAuthLog(final String agent, final String message, final Context context) {
        // update auth log
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("MMM dd");
        String formattedDate = df.format(c.getTime());
        String txtView = prefs.getString("log_text", "");
        String logText = formattedDate + " " + agent + ": " + message + "\n" + txtView;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("log_text", logText);
        editor.apply();
    }
}