package net.stargw.fok;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
// import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;


/**
 * Created by swatts on 11/02/18.
 */

public class FOKWidget1Provider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Log.w("FWWidget1",  "FWWidget1 onUpdate");

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Log.w("FWWidget1", "Loop W = " + String.valueOf(i));

            // Create an Intent to launch ExampleActivity
            // Intent intent = new Intent(context, Widgit1Action.class);

            Intent intent = new Intent(context, FOKWidget1Provider.class);
            intent.setAction(Global.TOGGLE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, 0);
            //PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.fok_widget1_layout);
            views.setOnClickPendingIntent(R.id.widgit1_switch, pendingIntent);

            if (Global.getFirewallState() == true) {
                views.setImageViewResource(R.id.widgit1_switch, R.drawable.fw_w_on);
                views.setViewVisibility(R.id.widgit1_disabled, View.INVISIBLE);
                views.setViewVisibility(R.id.widgit1_wall, View.VISIBLE);
            } else {
                views.setImageViewResource(R.id.widgit1_switch, R.drawable.fw_w_off);
                views.setViewVisibility(R.id.widgit1_disabled, View.VISIBLE);
                views.setViewVisibility(R.id.widgit1_wall, View.INVISIBLE);
            }

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);


        }

    }



    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);

        // Log.w("FWWidget1","FWWidget1 Action");

        if ( (intent.getAction() != null))
        {
            // Log.w("FWWidget1","FWWidget1 Action = " + intent.getAction());
        }

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        if ( (intent.getAction() != null) && (intent.getAction().equals(Global.TOGGLE))) {
            // Log.w("FWWidget1", "FWWidget1 Action TOGGLE");
            Bundle extras = intent.getExtras();
            if(extras!=null) {

                // Log.w("FWWidget1", "FWWidget1 Action EXTRA");

                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.fok_widget1_layout);

                if (Global.getFirewallState() == true) {
                    Intent serviceIntent = new Intent(Global.getContext(), FOKServiceFW.class);
                    serviceIntent.putExtra("command", Global.FIREWALL_STOP);
                    Global.getContext().startService(serviceIntent);
                    // Log.w("FWWidget1", "FWWidget1 Action Turn off");
                } else {
                    if (Global.getFirewallState() == false) {
                        // maybe a starting icon...
                        views.setImageViewResource(R.id.widgit1_switch, R.drawable.fw_w_wait);
                        ComponentName myWidget = new ComponentName(context, FOKWidget1Provider.class);
                        AppWidgetManager manager = AppWidgetManager.getInstance(context);
                        manager.updateAppWidget(myWidget, views);
                    }

                    Intent serviceIntent = new Intent(context, FOKServiceFW.class);
                    serviceIntent.putExtra("command", Global.FIREWALL_BOOT);

                    // Log.w("FWWidget1", "Restarting as a foreground service");

                    FOKServiceFW.byWidget = true;

                    ContextCompat.startForegroundService(context, serviceIntent);

                }

            }
        }

        // Receive update from the service - update icon
        if ((intent.getAction() != null) && (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())))
        {
            // Log.w("FWWidget1", "FWWidget1 Action STATE CHANGE");
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.fok_widget1_layout);

            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName myWidget = new ComponentName(context, FOKWidget1Provider.class);
            // String a = intent.getStringExtra("ID");

            Intent i = new Intent(context, FOKWidget1Provider.class);
            i.setAction(Global.TOGGLE);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, i, 0);


            if (Global.getFirewallState() == true) {
                views.setImageViewResource(R.id.widgit1_switch, R.drawable.fw_w_on);
                views.setViewVisibility(R.id.widgit1_disabled, View.INVISIBLE);
                views.setViewVisibility(R.id.widgit1_wall, View.VISIBLE);
            } else {
                views.setImageViewResource(R.id.widgit1_switch, R.drawable.fw_w_off);
                views.setViewVisibility(R.id.widgit1_disabled, View.VISIBLE);
                views.setViewVisibility(R.id.widgit1_wall, View.INVISIBLE);
            }

            // set the onclick again cos - onsuper and onupdate do not always work :-(
            views.setOnClickPendingIntent(R.id.widgit1_switch, pendingIntent);
            manager.updateAppWidget(myWidget, views);
        }

    }


}
