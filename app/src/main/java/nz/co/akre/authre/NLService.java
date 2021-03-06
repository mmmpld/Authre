package nz.co.akre.authre;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
//import android.support.v7.app.NotificationCompat;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;

public class NLService extends NotificationListenerService {
    public static boolean isNotificationAccessEnabled = false;
    private String TAG = this.getClass().getSimpleName();
    private NLServiceReceiver nlservicereciver;

    @Override
    public void onCreate() {
        super.onCreate();
        nlservicereciver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("nz.co.akre.authre.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
        registerReceiver(nlservicereciver,filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("AuthreLocalService", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nlservicereciver);
    }

    @Override
    public IBinder onBind(Intent mIntent) {
        IBinder mIBinder = super.onBind(mIntent);
        isNotificationAccessEnabled = true;
        return mIBinder;
    }

    @Override
    public boolean onUnbind(Intent mIntent) {
        boolean mOnUnbind = super.onUnbind(mIntent);
        isNotificationAccessEnabled = false;
        return mOnUnbind;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String ingressPackageName = "com.nianticproject.ingress"; // Scanner redacted
        String primePackageName = "com.nianticlabs.ingress.prime.qa";
        Bundle extras = sbn.getNotification().extras;
        if (sbn.getPackageName().equals(ingressPackageName)) { // process classic Ingress notifications
            Intent i = IngressClassicIntent(extras);
            NotificationReceiver.parseNotification(this, i);
        } else if (sbn.getPackageName().equals(primePackageName)) { // process Ingress Prime notifications
            Intent i = IngressPrimeIntent(extras);
            NotificationReceiver.parseNotification(this, i);
        }
    }

    private Intent IngressClassicIntent(Bundle extras) {
        Log.i("NOTIFY", "Processing Ingress Classic notification");
        SpannableString agentSpanString;
        SpannableString messageSpanString;
        String agent = "";
        String message = "";
        // set for single message, overwrite later if multiple messages
        agentSpanString = (SpannableString) extras.get("android.title");
        if (agentSpanString != null) agent = agentSpanString.toString();
        messageSpanString = (SpannableString) extras.get("android.text");
        if (messageSpanString != null) message = messageSpanString.toString();
        // test for multiple messages
        CharSequence[] textLineCharSequence = extras.getCharSequenceArray("android.textLines");
        if (textLineCharSequence != null) { // text lines is null if only single message
            Log.i("NOTIFY", "Detected multiple notifications");
            String latestMessage = textLineCharSequence[0].toString(); // multiple messages, first is latest. format is agentName[space]message
            String[] messageSplit = latestMessage.split(" ", 2); // get two parts (agent and message)
            // set for multiple messages
            agent = messageSplit[0];
            message = messageSplit[1];
        }
        // broadcast to NotificationReceiver
        Intent i = new Intent("nz.co.akre.authre.NOTIFICATION_LISTENER_EXAMPLE");
        i.putExtra("auth_agent",agent);
        i.putExtra("auth_message",message);
//            sendBroadcast(i);
        return i;
    }

    private Intent IngressPrimeIntent(Bundle extras) {
        Log.i("NOTIFY", "Processing Ingress Prime notification");
        String agent = extras.getString("android.title");
        String message = extras.getString("android.text");
        // test for multiple messages
        CharSequence[] textLineCharSequence = extras.getCharSequenceArray("android.textLines");
        if (textLineCharSequence != null) { // text lines is null if only single message
            Log.i("NOTIFY", "Detected multiple notifications");
            String latestMessage = textLineCharSequence[0].toString(); // multiple messages, first is latest. format is agentName[space]message
            String[] messageSplit = latestMessage.split(" ", 2); // get two parts (agent and message)
            // set for multiple messages
            agent = messageSplit[0];
            message = messageSplit[1];
        }
        // broadcast to NotificationReceiver
        Intent i = new Intent("nz.co.akre.authre.NOTIFICATION_LISTENER_EXAMPLE");
        i.putExtra("auth_agent",agent);
        i.putExtra("auth_message",message);
//            sendBroadcast(i);
        return i;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // this must be overridden if api is below 21, even though we don't need it
    }

    class NLServiceReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("command").equals("clearall")){
                NLService.this.cancelAllNotifications();
            }
            else if(intent.getStringExtra("command").equals("list")){
                Intent i1 = new  Intent("nz.co.akre.authre.NOTIFICATION_LISTENER_EXAMPLE");
                i1.putExtra("notification_event","=====================");
                sendBroadcast(i1);
                int i=1;
                for (StatusBarNotification sbn : NLService.this.getActiveNotifications()) {
                    Intent i2 = new  Intent("nz.co.akre.authre.NOTIFICATION_LISTENER_EXAMPLE");
                    i2.putExtra("notification_event",i +" " + sbn.getPackageName() + "\n");
                    sendBroadcast(i2);
                    i++;
                }
                Intent i3 = new  Intent("nz.co.akre.authre.NOTIFICATION_LISTENER_EXAMPLE");
                i3.putExtra("notification_event","===== Notification List ====");
                sendBroadcast(i3);
            }
        }
    }
}
