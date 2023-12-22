package net.stargw.karma;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

public class Widget1Provider extends AppWidgetProvider {


    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Logs.myLog("Widget1 - onUpdate()",4);
        // We update manually
    }


    public int getView() {
        // Log.w("KarmaWidget1","getView R.layout.widget1_layout");
        return R.layout.widget1_layout;
    }

    public void updateGUI(Context context, int appWidgetId)
    {
        Logs.myLog("Widget1 - updateGUI() - Widget ID = " + appWidgetId,4);

        // Gets info on itself somehow! Via context?
        RemoteViews views = new RemoteViews(context.getPackageName(), getView());

        if (Global.getFirewallState() == true) {
            views.setImageViewResource(R.id.widgit1_switch, R.drawable.fw_w_on);
            views.setViewVisibility(R.id.widgit1_disabled, View.INVISIBLE);
            views.setViewVisibility(R.id.widgit1_wall, View.VISIBLE);
        } else {
            views.setImageViewResource(R.id.widgit1_switch, R.drawable.fw_w_off);
            views.setViewVisibility(R.id.widgit1_disabled, View.VISIBLE);
            views.setViewVisibility(R.id.widgit1_wall, View.INVISIBLE);
        }



        // Create an Intent to launch Activity
        Intent intent = new Intent(context, Widget1Provider.class);
        intent.setAction(Global.TOGGLE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra("VIEW", R.layout.widget1_layout);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, 0);
        //PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, 0);

        // Attach an on-click listener to the button
        views.setOnClickPendingIntent(R.id.widgit1_switch, pendingIntent);



        // Tell the AppWidgetManager to perform an update on the current app widget
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(appWidgetId, views);

    }


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Logs.myLog("Widget1 - onReceive()",4);


        if ((intent.getAction() != null)) {
            Logs.myLog("Widget1 - onReceive() - Action = " + intent.getAction(),4);
        } else {
            Logs.myLog("Widget1 - onReceive() - No Action, doing nothing!",4);
            return;
        }

        // Toggle the firewall - via specific targeted Intent
        if ((intent.getAction() != null) && (intent.getAction().equals(Global.TOGGLE))) {
            Logs.myLog("Widget1 - onReceive() - Action TOGGLE",4);

            // Do the ACTION - do not care which widget1 instance triggered it
            if (Global.getFirewallState() == true) {
                Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
                serviceIntent.putExtra("command", Global.FIREWALL_STOP);
                Global.getContext().startService(serviceIntent);
                // Log.w("KarmaWidget1", "Action - Turn off");
                Logs.myLog("Widget1 - onReceive() - Action - Turn off",4);

            } else {
                showWaiting(context);
                Intent serviceIntent = new Intent(context, ServiceFW.class);
                serviceIntent.putExtra("command", Global.FIREWALL_WIDGET);
                // Log.w("KarmaWidget1", "Action - Start as a foreground service");
                Logs.myLog("Widget1 - onReceive() - Action - Start as a foreground service",4);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
            // No need to update widgets - firewall state change will update
        }

        // Receive update from the service - update icon
        if ((intent.getAction() != null) && (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE))) {
            // Loop through all Widgets of this class - updating
            AppWidgetManager man = AppWidgetManager.getInstance(context);
            int[] ids = man.getAppWidgetIds(
                    new ComponentName(context, Widget1Provider.class));
            Logs.myLog("Widget1 - onReceive() - Number of Widgets = " + ids.length,4);
            for (int i = 0; i < ids.length; i++) {
                // Log.w("KarmaWidget1", "Updating Widget: " + ids[i]);
                Logs.myLog("Widget1 - onReceive() - Updating Widget: " + ids[i],4);

                updateGUI(context, ids[i]);
            }
        }


    }

    void showWaiting(Context context)
    {
        AppWidgetManager man = AppWidgetManager.getInstance(context);
        int[] ids = man.getAppWidgetIds(
                new ComponentName(context, Widget1Provider.class));
        Logs.myLog("Widget1 - showWaiting() - Number of Widgets = " + ids.length,4);
        for (int i = 0; i < ids.length; i++) {
            // Log.w("KarmaWidget1", "Updating Widget: " + ids[i]);
            Logs.myLog("Widget1 - showWaiting() - Updating Widget: " + ids[i],4);

            // Gets info on itself somehow! Via context?
            RemoteViews views = new RemoteViews(context.getPackageName(), getView());

            if (Global.getFirewallState() == false) {
                views.setImageViewResource(R.id.widgit1_switch, R.drawable.fw_w_wait);
                views.setViewVisibility(R.id.widgit1_disabled, View.INVISIBLE);
                views.setViewVisibility(R.id.widgit1_wall, View.VISIBLE);
            }

            // Create an Intent to launch Activity
            Intent intent = new Intent(context, Widget1Provider.class);
            intent.setAction(Global.TOGGLE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, ids[i]);
            intent.putExtra("VIEW", R.layout.widget1_layout);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ids[i], intent, 0);
            //PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, 0);

            // Attach an on-click listener to the button
            views.setOnClickPendingIntent(R.id.widgit1_switch, pendingIntent);



            // Tell the AppWidgetManager to perform an update on the current app widget
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            manager.updateAppWidget(ids[i], views);

        }
    }

}
