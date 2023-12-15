package net.stargw.karma;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class ServiceFW extends VpnService implements Runnable {

    private static final String TAG = "KarmaLog";

    private Thread mThread;

    static ParcelFileDescriptor vpnInterface = null;

    // ArrayList<AppInfo> appInfo;

    NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        Global.getSettings();
        Logs.getLoggingLevel();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    }



    public void SonCreate(String flag, String message) {

        Global.getSettings();

        // appInfo = new ArrayList<AppInfo>();

        // notificationManager = NotificationManagerCompat.from(this);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Log.w(TAG, "Starting = " + message);


        // For Oreo+
        if (Build.VERSION.SDK_INT >= 26) {
            // String channelId = createBootNotificationChannel();

            NotificationChannel notificationChannel = Global.createNotificationChannel("FW1","FW Start Alert");

            int icon = R.drawable.ic_lock_idle_lock2;

            PendingIntent pIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, ActivityMain.class), PendingIntent.FLAG_UPDATE_CURRENT);

            Notification n = new Notification.Builder(this, notificationChannel.getId())
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
        // Global.getAppList();
        // App list will be built by Global.onCreate
        // Service will rebuild every few mins
        startVPN(flag);

        // booted = false;

        Logs.getLoggingLevel();
        Logs.myLog("Firewall Service created.", 2);

    }


    private String createBootNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "FW";
            String channelName = "FW Start Alert";
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

        Global.updateMyWidgets();

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

            Logs.myLog("Trying to Exclude Local Subnet: " + Global.settingsSubnet, 2);

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
                    for (IPUtil.CIDR include : IPUtil.toCIDR(start, IPUtil.minus1(exclude.getStart()))) {
                        try {
                            Logs.myLog("Include: " + include.address + "/" + include.prefix, 2);
                            builder.addRoute(include.address, include.prefix);
                        } catch (Throwable ex) {
                            Logs.myLog("Include Fail: " + ex.toString(), 2);
                            error = true;
                        }
                    }
                    Logs.myLog("Exclude: " + exclude.getStart().getHostAddress() + "..." + exclude.getEnd().getHostAddress(), 2);
                    start = IPUtil.plus1(exclude.getEnd());
                    InetAddress end = InetAddress.getByName("255.255.255.255");
                    for (IPUtil.CIDR include : IPUtil.toCIDR(start, IPUtil.minus1(end))) {
                        try {
                            Logs.myLog("Include: " + include.address + "/" + include.prefix, 2);
                            builder.addRoute(include.address, include.prefix);
                        } catch (Throwable ex) {
                            Logs.myLog("Include Fail: " + ex.toString(), 2);
                            error = true;
                        }
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

        if (Global.appListFW == null)
        {
            Logs.myLog("No apps! Cannot start VPN", 1);
            return null;
        } else {
            Logs.myLog("Start VPN with number of apps = " + Global.appListFW.size(), 1);
        }
        Iterator<Integer> it = Global.appListFW.keySet().iterator();

        while (it.hasNext())
        {
            int key = it.next();
            AppInfo app = Global.appListFW.get(key);

            //
            // Add our app to make sure at least one app is blocked
            try {
                builder.addAllowedApplication(Global.getContext().getString(R.string.package_name));
            } catch (PackageManager.NameNotFoundException e) {
                Logs.myLog("FW Cannot FW App: "  + Global.getContext().getString(R.string.package_name), 2);
                e.printStackTrace();
            }

            if ((app.internet) && (app.appInfoExtra != null)) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                {
                    for (int i = 0; i < app.appInfoExtra.size(); i++) {
                        Logs.myLog("Block App: " + app.UID2 + " " + app.appInfoExtra.get(i).packageFQDN, 1);
                    }
                } else {
                    if (app.fw >= 30) {
                        // App is firewalled by package name, but in reality
                        // it is by UID
                        for (int i = 0; i < app.appInfoExtra.size(); i++) {
                            try {
                                builder.addAllowedApplication(app.appInfoExtra.get(i).packageFQDN);
                                Logs.myLog("Block App: " + app.appInfoExtra.get(i).packageName + " [" + app.appInfoExtra.get(i).packageFQDN + "]", 1);
                                // Logs.myLog("Block App: " + app.appNames.get(i) + " " + app.UID2 + " " + app.packageNames.get(i), 1);
                            } catch (PackageManager.NameNotFoundException e) {
                                Logs.myLog("Cannot Block App: " + app.appInfoExtra.get(i).packageName + " [" + app.appInfoExtra.get(i).packageFQDN + "]", 1);
                                e.printStackTrace();
                            }
                        }
                    } else {
                        for (int i = 0; i < app.appInfoExtra.size(); i++) {
                            Logs.myLog("Allow App: " + app.appInfoExtra.get(i).packageName + " [" + app.appInfoExtra.get(i).packageFQDN + "]", 2);
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
    public static void boot(Context context) {
        Intent serviceIntent = new Intent(context, ServiceFW.class);
        serviceIntent.putExtra("command", Global.FIREWALL_BOOT);

        Log.w(TAG, "boot called");

        ContextCompat.startForegroundService(context, serviceIntent);
    }

    // Hook to start FW from BOOT
    public static void replace(Context context) {
        Intent serviceIntent = new Intent(context, ServiceFW.class);
        serviceIntent.putExtra("command", Global.FIREWALL_REPLACE);

        Log.w(TAG, "replace called");

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
                Logs.myLog("Problem closing VPN interface", 2);
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
        } catch (Exception ex4) {
            errorText = "Firewall Argument Exception";
            Logs.myLog(errorText + ": " + ex4, 2);
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
                    notifyFirewallState(Global.getContext().getString(R.string.notify_firewall_fail1));
                }
            } else {
                notifyFirewallState(String.format(Global.getContext().getString(R.string.notify_firewall_fail2),errorText));
            }
        } else {

            Logs.myLog("Created VPN interface", 3);

            if (firewallCommand.equals(Global.FIREWALL_START)) {
                Logs.myLog("Firewall Enabled!", 1);
            }

            if (firewallCommand.equals(Global.FIREWALL_BOOT)) {
                Logs.myLog("Firewall Enabled On Boot!", 1);
            }

            if (firewallCommand.equals(Global.FIREWALL_WIDGET)) {
                Logs.myLog("Firewall Enabled By Widget!", 1);
            }

            if (firewallCommand.equals(Global.FIREWALL_QS)) {
                Logs.myLog("Firewall Enabled By Quick Panel!", 1);
            }

            if (firewallCommand.equals(Global.FIREWALL_REPLACE)) {
                Logs.myLog("Firewall Enabled By Upgrade!", 1);
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
                    // Logs.myLog("Let onCreate do the actual start", 2);
                    // startVPN(command);
                    // onCreate never called
                    SonCreate(Global.FIREWALL_BOOT,"Started on Boot");
                }
                if (command.equals(Global.FIREWALL_WIDGET)) {
                    Logs.myLog("Firewall Service received widget intent", 2);
                    // Logs.myLog("Let onCreate do the actual start", 2);
                    // startVPN(command);
                    // onCreate never called
                    SonCreate(Global.FIREWALL_WIDGET,"Started by Widget");
                }
                if (command.equals(Global.FIREWALL_QS)) {
                    Logs.myLog("Firewall Service received Quick Panel intent", 2);
                    // Logs.myLog("Let onCreate do the actual start", 2);
                    // startVPN(command);
                    // onCreate never called
                    SonCreate(Global.FIREWALL_QS,"Started by Quick Panel");
                }
                if (command.equals(Global.FIREWALL_START)) { // Only possible via GUI
                    Logs.myLog("Firewall Service received start intent", 2);
                    startVPN(command);
                }
                if (command.equals(Global.FIREWALL_RESTART)) {
                    Logs.myLog("Firewall Service received restart intent", 2);
                    startVPN(command);
                }
                if (command.equals(Global.FIREWALL_REPLACE)) {
                    Logs.myLog("Firewall Service received replace restart intent", 2);
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
        notifyFirewallState("Firewall destroyed by Android!");
        Global.setFirewallState(false);
        Global.updateMyWidgets();
        sendAppBroadcast(Global.FIREWALL_STATE_CHANGE);
        super.onDestroy();
    }

    public void run() {
        // Log.i(TAG, "Started");
        Logs.myLog("Firewall Service Running.", 2);

        while (!(Thread.currentThread().isInterrupted())) {
            try {
                // Sleep
                Thread.sleep(300000); // 300 secs
                Logs.myLog("Service Checking apps HERE...", 2);
                if (Global.getAppList() == false)
                {
                    Logs.myLog("Service Housekeeping restart...", 1);
                    Logs.myLog("NOT restarting. See Issue #16 :-(", 1);
                    // startVPN(Global.FIREWALL_RESTART);
                }
            } catch (InterruptedException e) {
                Logs.myLog("Firewall Service Thread Interrupted.", 2);
                return; // actually leave the thread!!
            }
        }
    }


    private void notifyFirewallState(String message)
    {

        Logs.myLog("notifyFirewallState:" + message, 2);

        Intent intent = new Intent(this, ActivityMain.class);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        // PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        PendingIntent pIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ActivityMain.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Logs.myLog(message , 1);

        int icon = R.drawable.alert;
        if(Global.getFirewallState()) {
            icon = R.drawable.ic_lock_idle_lock2;
        }

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel notificationChannel = Global.createNotificationChannel("FW3", "FW Warning");

            Notification n = new Notification.Builder(this, notificationChannel.getId())
                    .setContentTitle(Global.getContext().getString(R.string.app_name))
                    .setContentText(message)
                    .setSmallIcon(icon)
                    .setAutoCancel(true)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.fw7))
                    .setContentIntent(pIntent).build();

            notificationManager.notify(555, n);

        } else {
            Notification n = new Notification.Builder(this)
                    .setContentTitle(Global.getContext().getString(R.string.app_name))
                    .setContentText(message)
                    .setSmallIcon(icon)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.fw7))
                    .setAutoCancel(true)
                    .setContentIntent(pIntent).build();

            notificationManager.notify(555, n);

        }

    }



}
