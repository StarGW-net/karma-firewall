package net.stargw.karma;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class BroadcastReceiverBoot extends BroadcastReceiver {

    // private static final String TAG = "AxeMan";


    @Override
    public void onReceive(Context myContext, Intent intent) {

        Logs.getLoggingLevel();
        Global.getSettings();

        Logs.myLog("BroadcastReceiver Received a Broadcast Intent!", 2);

        if ( (intent.getAction() != null))
        {
            Logs.myLog("Broadcast Receiver Action = " + intent.getAction(),2);
            if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
            {
                if (Global.settingsEnableBoot)
                {
                    ServiceFW.boot(myContext);
                } // otherwise do nothing
                return;
            }
            if (intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED"))
            {
                ServiceFW.replace(myContext);
            }
        }





    }

    private static void refreshAppBroadcast(Context myContext) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Global.APPS_REFRESH_INTENT );
        myContext.sendBroadcast(broadcastIntent);
    }

}
