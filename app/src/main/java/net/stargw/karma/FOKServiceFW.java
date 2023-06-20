package net.stargw.karma;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.TrafficStats;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
// import android.support.v4.content.ContextCompat;
import android.text.format.Time;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Created by swatts on 10/06/16.
 */
public class FOKServiceFW extends VpnService implements Runnable {

    private static final String TAG = "FOKLog";

    private Thread mThread;

    static ParcelFileDescriptor vpnInterface = null;

    // ArrayList<AppInfo> appInfo;

    int z = 0;

    private static boolean booted = false;
    public static boolean byWidget = false;

    NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        Global.getSettings();
        Logs.getLoggingLevel();

        // This never seems to get called!
        // Log.w(TAG, "Android onCreate");

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


    }



    public void SonCreate() {

        Global.getSettings();

        String message = "Firewall Started on Boot";
        // appInfo = new ArrayList<AppInfo>();

        // notificationManager = NotificationManagerCompat.from(this);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Log.w(TAG, "Booting = " + booted);

        if ( (booted) || (byWidget) )
        {
            // Log.w(TAG, "Booting cos booted = true");
            if (byWidget)
            {
                message = "Firewall Started by Widget";
            }

            booted = false;
            byWidget = false;
            // For Oreo

            if (Build.VERSION.SDK_INT >= 26) {
                Log.w(TAG, "Booting Android API26+");
                String channelId = createNotificationChannel();
                int icon = R.drawable.ic_lock_idle_lock2;

                PendingIntent pIntent = PendingIntent.getActivity(this, 0,
                        new Intent(this, ActivityMain.class), PendingIntent.FLAG_UPDATE_CURRENT);

                Notification n = new Notification.Builder(this, channelId)
                        .setContentTitle(Global.getContext().getString(R.string.app_name))
                        .setContentText(message)
                        .setSmallIcon(icon)
                        .setAutoCancel(true)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.fw7))
                        .setContentIntent(pIntent).build();

                startForeground(100, n);
                // Log.w(TAG, "foreground notification");
            }
            // Log.w(TAG, "Boot building an App List Steve");
            Global.getAppList();
            startVPN(Global.FIREWALL_BOOT);
        }
        booted = false;

        Logs.getLoggingLevel();
        Logs.myLog("Firewall Service created.", 2);

    }



    private String createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "FW";
            String channelName = "FW On Boot Alert";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            // omitted the LED color
            channel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
            return channelId;
        } else {
            return "none";
        }
    }

    private void sendAppBroadcast(String message) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(message);

        sendBroadcast(broadcastIntent);

        Log.w("FWWService",  "update widget:" + message);

        updateMyWidgets(Global.getContext());

    }

    public static void updateMyWidgets(Context context) {
        AppWidgetManager man = AppWidgetManager.getInstance(context);
        int[] ids = man.getAppWidgetIds(
                new ComponentName(context,FOKWidget1Provider.class));
        for (int i=0; i<ids.length; i++) {
            int appWidgetId = ids[i];
            Intent updateIntent = new Intent();
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            // updateIntent.putExtra(MyWidgetProvider.WIDGET_DATA_KEY, data);
            context.sendBroadcast(updateIntent);
        }

        // tell all the widgets that the firewall is diabled
        ids = man.getAppWidgetIds(
                new ComponentName(context,FOKWidget2Provider.class));
        for (int i=0; i<ids.length; i++) {
            int appWidgetId = ids[i];
            Intent updateIntent = new Intent();
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            // updateIntent.putExtra(MyWidgetProvider.WIDGET_DATA_KEY, data);
            context.sendBroadcast(updateIntent);
        }
    }


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private Builder buildVPN(String firewallCommand)
    {

        Builder builder = new Builder();

        builder.setSession(getString(R.string.app_name));

        // VPN address
        builder.addAddress("10.6.6.6", 32); // could be a prob??
        // builder.addAddress("23.58.6.66", 32); // could be a prob??
        builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128);

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
        Global.settingsSubnet = p.getString("settingsSubnet", "");

        if (Global.settingsSubnet != "")
        {

            boolean error = false;

            String[] parts2 = Global.settingsSubnet.split("/");
            int mask = 24;
            try {
                mask = Integer.parseInt(parts2[1]);
            } catch (Exception e) {
                error = true;
            }

            List<IPUtil.CIDR> listExclude = new ArrayList<>();
            // listExclude.add(new IPUtil.CIDR("10.0.0.0", 24)); // localhost
            listExclude.add(new IPUtil.CIDR(parts2[0], mask));

            Collections.sort(listExclude);


            try {
                InetAddress start = InetAddress.getByName("0.0.0.0");
                for (IPUtil.CIDR exclude : listExclude) {
                    Logs.myLog("Exclude: " + exclude.getStart().getHostAddress() + "..." + exclude.getEnd().getHostAddress(), 2);
                    for (IPUtil.CIDR include : IPUtil.toCIDR(start, IPUtil.minus1(exclude.getStart())))
                        try {
                            Logs.myLog("Include: " + include.address + "/" + include.prefix, 2);
                            builder.addRoute(include.address, include.prefix);
                        } catch (Throwable ex) {
                            Logs.myLog("Exclude Fail: " + ex.toString(), 2);
                            error = true;
                        }
                    start = IPUtil.plus1(exclude.getEnd());
                    InetAddress end = InetAddress.getByName("255.255.255.255");
                    for (IPUtil.CIDR include : IPUtil.toCIDR(start, IPUtil.minus1(end)))
                        try {
                            Logs.myLog("Include: " + include.address + "/" + include.prefix, 2);
                            builder.addRoute(include.address, include.prefix);
                        } catch (Throwable ex) {
                            Logs.myLog("Exclude Fail: " + ex.toString(), 2);
                            error = true;
                        }
                }

                if (error == true)
                {
                    // safety
                    Logs.myLog("Exclude Error: " + Global.settingsSubnet, 2);
                    Logs.myLog("Excluding nothing!", 2);
                    builder.addRoute("0.0.0.0", 0);
                } else {
                    Logs.myLog("Exclude Local Subnet: " + parts2[0] + "/" + mask, 2);
                }
            } catch (UnknownHostException ex) {
                Logs.myLog("Exclude Error: " + ex.toString(), 2);
            }
        } else {
            builder.addRoute("0.0.0.0", 0);
        }

        builder.addRoute("0:0:0:0:0:0:0:0", 0);

        Iterator<Integer> it = Global.appListFW.keySet().iterator();

        while (it.hasNext())
        {
            int key = it.next();
            AppInfo app = Global.appListFW.get(key);
            // SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
            // app.fw = p.getBoolean("FW-" + app.UID2, false);

            //
            // Add our app to make sure at least one app is blocked
            try {
                builder.addAllowedApplication(Global.getContext().getString(R.string.package_name));
            } catch (PackageManager.NameNotFoundException e) {
                Logs.myLog("FW Cannot FW App: "  + Global.getContext().getString(R.string.package_name), 2);
                e.printStackTrace();
            }
            //
            if ((app.enabled) && (app.internet)) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                {
                    for(int i = 0; i < app.packageNames.size(); i++) {
                        Logs.myLog("Block App: " + app.UID2 + " " + app.packageNames.get(i), 1);
                    }
                } else {
                    if (app.fw) {
                        for(int i = 0; i < app.packageNames.size(); i++) {
                            try {
                                builder.addAllowedApplication(app.packageNames.get(i));
                                Logs.myLog("Block App: " + app.appNames.get(i) + " [" + app.packageNames.get(i) + "]", 1);
                                // Logs.myLog("Block App: " + app.appNames.get(i) + " " + app.UID2 + " " + app.packageNames.get(i), 1);
                            } catch (PackageManager.NameNotFoundException e) {
                                Logs.myLog("FW Cannot Block App: " + app.appNames.get(i) + " [" + app.packageNames.get(i) + "]", 1);
                                e.printStackTrace();
                            }
                        }
                    } else {
                        for(int i = 0; i < app.packageNames.size(); i++) {
                            Logs.myLog("Allow App: "  + app.appNames.get(i) + " [" + app.packageNames.get(i) + "]", 2);
                            // Logs.myLog("Allow App: "  + app.UID2 + " " + app.packageNames.get(i), 2);
                            // builder.addDisallowedApplication(app.packageNames.get(i));
                        }
                    }
                }

            }


        }
        return builder;
    }

    // Hook to start FW from BOOT
    public static void start(Context context) {
        Intent serviceIntent = new Intent(context, FOKServiceFW.class);
        serviceIntent.putExtra("command", Global.FIREWALL_BOOT);

        Log.w(TAG, "start called");

        booted = true;
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    private void startVPN(String firewallCommand) {

        //Global.setFirewallState(false);
        // effectively a restart
        Builder builder = buildVPN(firewallCommand);

        if (VpnService.prepare(this) == null)
        {
            Logs.myLog("VPNService prepared.",2);
        } else {
            Logs.myLog("VPNService Not prepared",2);
        }
        // close an old VPN
        if (vpnInterface != null)
        {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            vpnInterface = null;
        }

        String errorText = "";
        try {
            vpnInterface = builder.establish();
        } catch (SecurityException ex) {
            errorText = "Firewall Security Exception";
            Logs.myLog(errorText + ": " + ex, 2);
            Logs.myLog("This is a KNOWN Android bug :-( ", 2);
            vpnInterface = null;
        } catch (IllegalStateException ex2) {
            errorText = "Firewall State Exception";
            Logs.myLog(errorText + ": " + ex2, 2);
            vpnInterface = null;
        } catch (IllegalArgumentException ex3) {
            errorText = "Firewall Argument Exception";
            Logs.myLog(errorText + ": " + ex3, 2);
            vpnInterface = null;
        }

        // Sometimes the VPN will return null even when prepared.
        // this is usually when trying to claim the VPN back from another service
        // only option is to start the activity again!

        if (vpnInterface == null) {
            Logs.myLog("VPN interface null.",2);
            // Logs.myLog("Cannot Start Firewall!" , 0);
            if (errorText.equals("")) { // if boot...
                if (firewallCommand.equals(Global.FIREWALL_BOOT)) {
                    notifyFirewallState(Global.getContext().getString(R.string.notify_firewall_fail3));
                } else {
                    if (firewallCommand.equals(Global.FIREWALL_DESTROY_RESTART)) {
                        notifyFirewallState(Global.getContext().getString(R.string.notify_firewall_fail4));
                    } else {
                        notifyFirewallState(Global.getContext().getString(R.string.notify_firewall_fail1));
                    }
                }
            } else {
                notifyFirewallState(String.format(Global.getContext().getString(R.string.notify_firewall_fail2),errorText));
            }
        } else {

            if (firewallCommand.equals(Global.FIREWALL_START)) {
                Logs.myLog("Firewall Enabled!", 1);
            }

            if (firewallCommand.equals(Global.FIREWALL_BOOT)) {
                Logs.myLog("Firewall Enabled On Boot!", 1);
            }

            if (firewallCommand.equals(Global.FIREWALL_DESTROY_RESTART)) {
                Logs.myLog("Firewall Enabled After Termination!", 1);
            }

            if (firewallCommand.equals(Global.FIREWALL_RESTART)) {
                Logs.myLog("Firewall Restarted due to config change!", 2);
            }


            // Stop the previous session by interrupting the thread.
            if (mThread != null) {
                mThread.interrupt();
            }

            // Start a new session by creating a new thread.
            mThread = new Thread(this, getString(R.string.app_name));
            mThread.start();
            if (notificationManager != null) {
                notificationManager.cancel(666);
            }

            Global.setFirewallState(true);
        }
        sendAppBroadcast(Global.SCREEN_REFRESH_INTENT );

    }

    private void stopVPN()
    {

        Global.setFirewallState(false);
        if (vpnInterface != null)
        {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            vpnInterface = null;
        }
        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }



        sendAppBroadcast(Global.SCREEN_REFRESH_INTENT );

    }

    @Override
    public void onRevoke() {
        Logs.myLog("Firewall Service received OnRevoke!", 2);

        if (Global.getFirewallState() == true)
        {
            Global.setFirewallState(false);
            // Logs.myLog("Firewall Terminated!" , 0);

            stopVPN();

        }
        super.onRevoke();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String command = intent.getStringExtra("command");
            if (command != null) {
                Logs.myLog("Firewall Service received intent: " + command, 2);
                if (command.equals(Global.FIREWALL_BOOT)) {
                    Logs.myLog("Firewall Service received boot intent", 2);
                    Logs.myLog("Let onCreate do the actual start", 2);
                    // startVPN(command);
                    // onCreate never called
                    SonCreate();
                }
                if (command.equals(Global.FIREWALL_START)) {
                    Logs.myLog("Firewall Service received start intent", 2);
                    startVPN(command);
                }
                if (command.equals(Global.FIREWALL_RESTART)) {
                    Logs.myLog("Firewall Service received restart intent", 2);
                    startVPN(command);
                }
                if (command.equals(Global.FIREWALL_DESTROY_RESTART)) {
                    Logs.myLog("Firewall Service received destroy restart intent", 2);
                    startVPN(command);
                }
                if (command.equals(Global.FIREWALL_STOP)) {        // close an old VPN
                    Logs.myLog("Firewall Service received stop intent", 2);
                    stopVPN();
                    Logs.myLog("Firewall Disabled!" , 1);
                }
                if (command.equals(Global.FIREWALL_STATUS)) {        // close an old VPN
                    Logs.myLog("Firewall Service received status intent. Status: " + Global.getFirewallState(), 2);
                    if (vpnInterface == null) {
                        Global.setFirewallState(false);
                    } else {
                        Global.setFirewallState(true);
                    }
                }
            } else {
                Logs.myLog("Firewall Service received empty command intent", 2);
            }
        } else {
            Logs.myLog("Firewall Service received no intent", 2);
        }
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Logs.myLog("Firewall Service destroyed.", 2);
        // stopVPN();
        sendAppBroadcast(Global.FIREWALL_STATE_CHANGE);
        super.onDestroy();
    }

    public void run() {
        // Log.i(TAG, "Started");
        Logs.myLog("Firewall Service Running.", 2);
    }


    private void notifyFirewallState(String message)
    {
        Intent intent = new Intent(this, ActivityMain.class);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        // PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        PendingIntent pIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ActivityLogs.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Logs.myLog(message , 1);

        // build notification
        // the addAction re-use the same intent to keep the example short

        int icon = R.drawable.alert;
        if(Global.getFirewallState()) {
            icon = R.drawable.ic_lock_idle_lock2;
        }

        Notification n  = new Notification.Builder(this)
                .setContentTitle(Global.getContext().getString(R.string.app_name))
                .setContentText(message)
                .setSmallIcon(icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.fw7))
                .setAutoCancel(true)
                .setContentIntent(pIntent).build();


        notificationManager.notify(666, n);


    }



}
