package net.stargw.fok;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by swatts on 13/11/15.
 */
public class BroadcastReceiverBoot extends BroadcastReceiver {

    // private static final String TAG = "AxeMan";


    @Override
    public void onReceive(Context myContext, Intent intent) {

        Logs.getLoggingLevel();
        Global.getSettings();

        Logs.myLog("BroadcastReceiver Received a Broadcast Intent!", 2);

        if ( (intent.getAction() != null))
        {
            Logs.myLog("FOX Broadcast Receiver Action = " + intent.getAction(),2);
            if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
            {
                if (Global.settingsEnableBoot)
                {
                    // Build the app list
                    // Logs.myLog("Boot building an App List", 2);
                    // Global.getAppList();

    /*
                Intent serviceIntent = new Intent(myContext, FOKServiceFW.class);
                serviceIntent.putExtra("command", Global.FIREWALL_BOOT);
                myContext.startService(serviceIntent);
    */

                    FOKServiceFW.start(myContext);


                } // otherwise do nothing
                return;
            }
            if (intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED"))
            {
                FOKServiceFW.start(myContext);
            }
        }





    }

    private static void refreshAppBroadcast(Context myContext) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Global.APPS_REFRESH_INTENT );
        myContext.sendBroadcast(broadcastIntent);
    }

}
