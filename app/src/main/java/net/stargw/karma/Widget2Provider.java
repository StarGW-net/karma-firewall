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
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import static net.stargw.karma.Global.getContext;

public class Widget2Provider extends AppWidgetProvider {
    

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Logs.myLog("Widget2 - onUpdate()", 3);
        // We update manually
    }


    public int getView() {
        // Log.w("KarmaWidget2","getView R.layout.widget2_layout");
        return R.layout.widget2_layout;
    }

    public void updateGUIAll(Context context)
    {
        Logs.myLog("Widget2 - updateGUIAll()", 3);

        // Update all the widgets - we need to do this in case two
        // widgets have the same app on them
        // Loop through all Widgets of this class - updating
        AppWidgetManager man = AppWidgetManager.getInstance(context);
        int[] ids = man.getAppWidgetIds(
                new ComponentName(context, Widget2Provider.class));
        Logs.myLog("Widget2 - updateGUIAll() - Number of Widgets = " + ids.length,3);

        for (int i = 0; i < ids.length; i++) {
            Logs.myLog("Widget2 - updateGUIAll () - Updating Widget: " + ids[i], 3);

            updateGUI(context, ids[i]);
        }
    }

    public void updateGUI(Context context, int appWidgetId)
    {
        Logs.myLog("Widget2 - updateGUI() - Widget ID = " + appWidgetId, 3);

        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        // Test what happens if we delete an app and widget stil left
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

        int uid = p.getInt("W-" + appWidgetId,-1);

        // Gets info on itself somehow!
        RemoteViews views = new RemoteViews(context.getPackageName(), getView());

        if (Global.getFirewallState() == true) {
            views.setViewVisibility(R.id.widgit2_wall, View.VISIBLE);
            views.setViewVisibility(R.id.widgit2_disabled, View.INVISIBLE);
        } else {
            views.setViewVisibility(R.id.widgit2_disabled, View.VISIBLE);
            views.setViewVisibility(R.id.widgit2_wall, View.INVISIBLE);
        }

        if (uid == -1)
        {
            // Means we have no record of the Widget
            // Not sure we would ever get here
            Logs.myLog("Widget2 - updateGUI() - [a] Could not get UID of app!", 3);

            views.setImageViewBitmap(R.id.widgit2_icon, Global.drawableToBitmap(getContext().getResources().getDrawable(R.drawable.alert)));
            views.setViewVisibility(R.id.widgit2_deny, View.INVISIBLE);
            views.setViewVisibility(R.id.widgit2_allow, View.INVISIBLE);
            // This may remove the pressable intent
            manager.updateAppWidget(appWidgetId, views);
            return;
        }

        int state = p.getInt("FW-" + uid,-1);

        if (state == -1)
        {
            // Widget exists but app has been deleted
            Logs.myLog("Widget2 - updateGUI() - [a] App no longer exists: " + uid, 3);

            views.setImageViewBitmap(R.id.widgit2_icon, Global.drawableToBitmap(getContext().getResources().getDrawable(R.drawable.alert)));
            views.setViewVisibility(R.id.widgit2_deny, View.INVISIBLE);
            views.setViewVisibility(R.id.widgit2_allow, View.INVISIBLE);
            // maybe we should delete the widget?
            // This may remove the pressable intent
            manager.updateAppWidget(appWidgetId, views);
            return;
        }

        // May not have icon in App list yet list
        views.setImageViewBitmap(R.id.widgit2_icon, Global.drawableToBitmap(getIcon2(uid)));



        if (state >= 30) {
            views.setViewVisibility(R.id.widgit2_deny, View.VISIBLE);
            views.setViewVisibility(R.id.widgit2_allow, View.INVISIBLE);
        } else {
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

        if (Global.getFirewallState() == true) {
            views.setViewVisibility(R.id.widgit2_wall, View.VISIBLE);
            views.setViewVisibility(R.id.widgit2_disabled, View.INVISIBLE);
        } else {
            views.setViewVisibility(R.id.widgit2_disabled, View.VISIBLE);
            views.setViewVisibility(R.id.widgit2_wall, View.INVISIBLE);
        }

        // Create an Intent to launch Activity
        Intent intent = new Intent(context, Widget2Provider.class);
        intent.setAction(Global.TOGGLEAPP);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, 0);

        // Attach an on-click listener to the button
        views.setOnClickPendingIntent(R.id.widgit2_icon, pendingIntent);


        // Tell the AppWidgetManager to perform an update on the current app widget
        manager.updateAppWidget(appWidgetId, views);

    }



    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);

        Logs.myLog("Widget2 - onReceive()", 3);

        if ((intent.getAction() != null)) {
            Logs.myLog("Widget2 - onReceive() - Action = " + intent.getAction(), 3);
        } else {
            Logs.myLog("Widget2 - onReceive() - No Action, doing nothing!",3);
            return;
        }


        // Receive update from the service - update icon - may just set to not firewalled...
        if ((intent.getAction() != null) && (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())))
        {
            String action = intent.getStringExtra(Global.WIDGET_ACTION);
            Logs.myLog("Widget2 - onReceive() - getStringExtra: " + action,3);

            updateGUIAll(context);
            /*
            if ( (action!= null) && (action.equals("ALL")) ) {
                updateGUIAll(context);
            }
            */

        }

        // These come direct from Intents, not Broadcasts
        if ( (intent.getAction() != null) && (intent.getAction().equals(Global.TOGGLEAPP))) {
            Logs.myLog("Widget2 - onReceive() - Action TOGGLEAPP",3);
            // Loop through all Widgets of this class - toggling?
            // do we need to - if two of same app widgets!

            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

            // Need to check that apps still exist?
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                Logs.myLog("Widget2 - onReceive() - Cannot proceed without Widget ID", 3);
            }

            int uid = p.getInt("W-" + appWidgetId,-1);

            if (uid == -1) {
                // Means we have no record of the Widget
                // Not sure we would ever get here
                Logs.myLog("Widget2 - onReceive() - [b] Could not get UID of app!",3);

                updateGUIAll(context);
                return;
            }

            int state = p.getInt("FW-" + uid,-1);

            if (state == -1)
            {
                // Widget exists but app has been deleted
                Logs.myLog("Widget2 - onReceive() - [b] App no longer exists: " + uid,3);

                updateGUIAll(context);
                return;
            }
            if (state >= 30) {
                // Write to the file
                p.edit().putInt("FW-" + uid, 10).apply();
                // And update the list held in memory
                AppInfo app = Global.appListFW.get(uid);
                if (app != null) {
                    Logs.myLog("Widget2 - onReceive() - Toggle App: " + app.name,3);
                    app.fw = 10;
                } else {
                    Logs.myLog("Widget2 - onReceive() - Toggle App - AppList Not Available!",3);
                }
            } else {
                // Write to the file
                p.edit().putInt("FW-" + uid, 30).apply();
                // And update the list held in memory
                AppInfo app = Global.appListFW.get(uid);
                if (app != null) {
                    Logs.myLog("Widget2 - onReceive() - Toggle App: " + app.name,3);
                    app.fw = 30;
                } else {
                    Logs.myLog("Widget2 - onReceive() - Toggle App - AppList Not Available!",3);
                }
            }

            updateGUIAll(context);

            // Restart the firewall for changes to take effect
            if (Global.getFirewallState() == true) {
                Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
                serviceIntent.putExtra("command", Global.FIREWALL_RESTART); // can we pass app
                Global.getContext().startService(serviceIntent);
            }
        }

    }


    private Drawable getIcon2(int uid)
    {

        if (Global.appListFW != null) {
            AppInfo app = Global.appListFW.get(uid);
            if (app != null) {
                return app.icon;
            }
        }

        Drawable icon;

        PackageManager pm = Global.getContext().getPackageManager();
        String packageName = pm.getNameForUid(uid);

        // Huh claims Firefox packageName is: org.mozilla.firefox.sharedID
        // But I cannot find any other app that shares the same UID
        // So this is not great

        PackageManager pManager = Global.getContext().getPackageManager();

        try {
            icon = pManager.getApplicationIcon(packageName);
        } catch (Exception e) {
            icon = getContext().getResources().getDrawable(R.drawable.android);
            Logs.myLog("Widget2 - onReceive() - Cannot get icon for : " + packageName + " - UID: " + uid,3);
        }

        return icon;

    }
}
