package net.stargw.karma;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

public class Widget1Provider extends AppWidgetProvider {


    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Log.w("KarmaWidget1","onUpdate");
        // We update manually
    }


    public int getView() {
        // Log.w("KarmaWidget1","getView R.layout.widget1_layout");
        return R.layout.widget1_layout;
    }

    public void updateGUI(Context context, int appWidgetId)
    {
        // Log.w("KarmaWidget1",  "updateGUI - Widget ID = " + appWidgetId + " update the display");

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

        // Log.w("KarmaWidget1", "onReceive");

        if ((intent.getAction() != null)) {
            // Log.w("KarmaWidget1", "Widget Action = " + intent.getAction());
        } else {
            // Log.w("KarmaWidget1", "Widget No Action, doing nothing!");
            return;
        }

        // Toggle the firewall - via specific targeted Intent
        if ((intent.getAction() != null) && (intent.getAction().equals(Global.TOGGLE))) {
            // Log.w("KarmaWidget1", "KarmaWidget1 Action TOGGLE");

            // Do the ACTION - do not care which widget instance triggered it
            if (Global.getFirewallState() == true) {
                Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
                serviceIntent.putExtra("command", Global.FIREWALL_STOP);
                Global.getContext().startService(serviceIntent);
                // Log.w("KarmaWidget1", "Action - Turn off");
            } else {
                Intent serviceIntent = new Intent(context, ServiceFW.class);
                serviceIntent.putExtra("command", Global.FIREWALL_WIDGET);
                // Log.w("KarmaWidget1", "Action - Start as a foreground service");
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
            for (int i = 0; i < ids.length; i++) {
                    // Log.w("KarmaWidget1", "Updating Widget: " + ids[i]);
                    updateGUI(context, ids[i]);
            }
        }


    }


}
