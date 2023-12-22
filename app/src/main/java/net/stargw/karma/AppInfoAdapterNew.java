package net.stargw.karma;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;

public class AppInfoAdapterNew extends AppInfoAdapter

{

    public AppInfoAdapterNew(Context context, ArrayList<AppInfo> apps) {
        super(context, apps);

        // listener = we overide the action

    }

    @Override
    public void action(int position) {

        AppInfo app = getItem(position);

        AppInfo thisApp = Global.appListFW.get(app.UID2);

        // If we want to use 40 for new blocked then 40 becomes 20 here...
        if (thisApp != null) {
            if (thisApp.fw >= 30) {
                thisApp.fw = 10;
            } else {
                thisApp.fw = 30;
            }

            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
            p.edit().putInt("FW-" + thisApp.UID2, thisApp.fw).apply();

            AppWidgetManager man = AppWidgetManager.getInstance(mContext);
            int[] ids = man.getAppWidgetIds(
                    new ComponentName(mContext, Widget2Provider.class));
            for (int i=0; i<ids.length; i++) {
                int appWidgetId = ids[i];
                Intent updateIntent = new Intent();
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                // updateIntent.putExtra(MyWidgetProvider.WIDGET_DATA_KEY, data);
                mContext.sendBroadcast(updateIntent);
            }

            // adapter.notifyDataSetChanged(); // maybe need this
            notifyDataSetChanged();

            if (Global.getFirewallState() == true) {

                Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
                serviceIntent.putExtra("command", Global.FIREWALL_RESTART); // can we pass app
                Global.getContext().startService(serviceIntent);
            }

        }
    }
}
