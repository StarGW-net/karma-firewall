package net.stargw.karma;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import static net.stargw.karma.Global.getContext;

/**
 * Created by swatts on 11/02/18.
 */

public class FOKWidget2Provider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Log.w("FWWidget2",  "FWWidget2 onUpdate");

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Log.w("FWWidget2", "Loop W = " + String.valueOf(i));

            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

            int uid = p.getInt("W-" + appWidgetId,0);

            if (uid == 0)
            {
                // Log.w("FWWidget2","Could not get UID of app!");
                continue;
            }

            // Create an Intent to launch ExampleActivity
            // Intent intent = new Intent(context, Widgit1Action.class);

            Intent intent = new Intent(context, FOKWidget2Provider.class);
            intent.setAction(Global.TOGGLEAPP);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, 0);
            //PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.fok_widget2_layout);

            views.setImageViewBitmap(R.id.widgit2_icon, Global.drawableToBitmap(getIcon2(uid)));    // <--- do not have icon list

            boolean state = p.getBoolean("FW-" + uid,false);

            if (state == true) {
                // views.setImageViewResource(R.id.widgit2_icon, R.drawable.fw_w_app_on);
                views.setViewVisibility(R.id.widgit2_deny, View.VISIBLE);
                views.setViewVisibility(R.id.widgit2_allow, View.INVISIBLE);
            } else {
                // views.setImageViewResource(R.id.widgit2_icon, R.drawable.fw_w_app_off);
                views.setViewVisibility(R.id.widgit2_allow, View.VISIBLE);
                views.setViewVisibility(R.id.widgit2_deny, View.INVISIBLE);
            }

            if (Global.getFirewallState() == true) {
                views.setViewVisibility(R.id.widgit2_wall, View.VISIBLE);
                views.setViewVisibility(R.id.widgit2_disabled, View.INVISIBLE);
            } else {
                views.setViewVisibility(R.id.widgit2_disabled, View.VISIBLE);
                views.setViewVisibility(R.id.widgit2_wall, View.INVISIBLE);
            }

            views.setOnClickPendingIntent(R.id.widgit2_icon, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);


        }


    }



    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);

        // Log.w("FWWidget2","FWWidget2 Action");

        if ( (intent.getAction() != null))
        {
            // Log.w("FWWidget2","FWWidget2 Action = " + intent.getAction());
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.fok_widget2_layout);

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName myWidget = new ComponentName(context, FOKWidget2Provider.class);
        // String a = intent.getStringExtra("ID");

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        // Log.w("FWWidget2","App Widget ID = " + appWidgetId);

        int uid = p.getInt("W-" + appWidgetId,0);

        if (uid == 0)
        {
            // Log.w("FWWidget2","Could not get UID of app!");
            return;
        }

        views.setImageViewBitmap(R.id.widgit2_icon, Global.drawableToBitmap(getIcon(uid)));

        if ( (intent.getAction() != null) && (intent.getAction().equals(Global.TOGGLEAPP))) {
            // Log.w("FWWidget2", "FWWidget2 Action TOGGLEAPP");
            Bundle extras = intent.getExtras();
            if(extras!=null) {

                // Log.w("FWWidget2", "FWWidget2 Action EXTRA");

                boolean state = p.getBoolean("FW-" + uid,false);


                Intent i = new Intent(context, FOKWidget2Provider.class);
                i.setAction(Global.TOGGLEAPP);
                i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, i, 0);

                views.setImageViewBitmap(R.id.widgit2_icon, Global.drawableToBitmap(getIcon(uid)));

                if (state == true) {
                    views.setViewVisibility(R.id.widgit2_deny, View.INVISIBLE);
                    views.setViewVisibility(R.id.widgit2_allow, View.VISIBLE);
                    // views.setImageViewResource(R.id.widgit2_icon, R.drawable.fw_w_app_off);
                    p.edit().putBoolean("FW-" + uid, false).apply();
                    AppInfo app = Global.appListFW.get(uid);
                    if (app != null) {
                        app.fw = false;
                    }
                } else {
                    views.setViewVisibility(R.id.widgit2_allow, View.INVISIBLE);
                    views.setViewVisibility(R.id.widgit2_deny, View.VISIBLE);
                    // views.setImageViewResource(R.id.widgit2_icon, R.drawable.fw_w_app_on);
                    p.edit().putBoolean("FW-" + uid, true).apply();
                    AppInfo app = Global.appListFW.get(uid);
                    if (app != null) {
                        app.fw = true;
                    }
                }

                // Need to let front end know as well!!

                // set the onclick again cos - onsuper and onupdate do not always work :-(
                views.setOnClickPendingIntent(R.id.widgit2_icon, pendingIntent);
                manager.updateAppWidget(appWidgetId, views);


                if (Global.getFirewallState() == true) {

                    Intent serviceIntent = new Intent(Global.getContext(), FOKServiceFW.class);
                    serviceIntent.putExtra("command", Global.FIREWALL_RESTART); // can we pass app
                    // serviceIntent.putExtra("restart", "app");
                    Global.getContext().startService(serviceIntent);
                    // thisApp.bytesFWOut = TrafficStats.getUidTxBytes(thisApp.UID2);
                    // thisApp.bytesFWIn = TrafficStats.getUidRxBytes(thisApp.UID2);
                }


            }

        }

        // Receive update from the service - update icon - may just set to not firewalled...
        if ((intent.getAction() != null) && (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())))
        {
            // Log.w("FWWidget2", "FWWidget2 Action STATE CHANGE");

            Intent i = new Intent(context, FOKWidget2Provider.class);
            i.setAction(Global.TOGGLEAPP);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, i, 0);

            boolean state = p.getBoolean("FW-" + uid,false);

            if (state == true) {
                // views.setImageViewResource(R.id.widgit2_icon, R.drawable.fw_w_app_on);
                views.setViewVisibility(R.id.widgit2_deny, View.VISIBLE);
                views.setViewVisibility(R.id.widgit2_allow, View.INVISIBLE);
            } else {
                // views.setImageViewResource(R.id.widgit2_icon, R.drawable.fw_w_app_off);
                views.setViewVisibility(R.id.widgit2_allow, View.VISIBLE);
                views.setViewVisibility(R.id.widgit2_deny, View.INVISIBLE);
            }

            if (Global.getFirewallState() == true) {
                views.setViewVisibility(R.id.widgit2_wall, View.VISIBLE);
                views.setViewVisibility(R.id.widgit2_disabled, View.INVISIBLE);
            } else {
                views.setViewVisibility(R.id.widgit2_disabled, View.VISIBLE);
                views.setViewVisibility(R.id.widgit2_wall, View.INVISIBLE);
            }
            // set the onclick again cos - onsuper and onupdate do not always work :-(
            views.setOnClickPendingIntent(R.id.widgit2_icon, pendingIntent);
            manager.updateAppWidget(appWidgetId, views);
        }

    }

    private Drawable getIcon(int uid)
    {
        Drawable d = Global.getContext().getResources().getDrawable(R.drawable.fw_w_app);

        if (Global.appListFW != null)
        {
            AppInfo app = Global.appListFW.get(uid);
            if (app != null) {
                return app.icon;
            }
        }

        /*
        if (Global.appListFW != null) {
            Log.w("FWWidget2", "Number of apps = " + Global.appListFW.size());
            for (int i = 0, l = Global.appListFW.size(); i < l; i++) {
                AppInfo app = Global.appListFW.get(i);
                if (app.UID2 == uid) {
                    return app.icon;
                }
            }
        }
        */

        return d;
    }


    private Drawable getIcon2(int uid)
    {

        Drawable icon;

        PackageManager pm = Global.getContext().getPackageManager();
        String packageName = pm.getNameForUid(uid);

        PackageManager pManager = Global.getContext().getPackageManager();

        try {
            icon = pManager.getApplicationIcon(packageName);
        } catch (Exception e) {
            icon = getContext().getResources().getDrawable(R.drawable.android);
            Logs.myLog("Cannot get icon!", 3);
        }

        return icon;

    }
}
