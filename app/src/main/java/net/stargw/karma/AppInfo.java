package net.stargw.karma;

import android.graphics.drawable.Drawable;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by swatts on 17/11/15.
 */
public class AppInfo implements Cloneable, Serializable {



    public AppInfo() {
        // TODO Auto-generated constructor stub
    }

    public String name;

    // Cannot searlise object and save with drawable;
    public transient Drawable icon;

    public ArrayList<String> packageNames = null;
    public ArrayList<String> appNames = null;

    public int UID2; // No UID 1

    public boolean fw =false;
    public boolean system;
    public boolean internet = false;
    public boolean enabled = true;

    public boolean expandView = false;

    public boolean flush = false;

    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            return null;
        }
    }

    /*

    We hold details of all apps in a Global HashMap:

   	static Map<Integer, AppInfo> appListFW = new ConcurrentHashMap<Integer, AppInfo>();

    This contains a drawable so cannot be serialised and saved.
    But maybe we can use transient on drawable object?

    This has to be built before the app can be used
    1) We need it in the GUI to display stiff
    2) We need it in the service to log UID to App Name

    So we either build in GUI and on Boot - how to do just one?

    And how to stop GUI and Service Clashing?

    Then apps that are firewalled are saved in SharedPreferences:

    p.edit().putBoolean("FW-" + app.UID2, app.fw).apply();

    use:

    if (sharedpreferences.contains("FW-" + app.UID2"))

    or

    Map<String, ?> allEntries = prefA.getAll();
    for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
        Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
    }

    For logging we can get app info from UID

    PackageManager pm = getApplicationContext().getPackageManager();
    String packageNames[] = pm.getPackagesForUid(1000);

    Then just build the appinfo from the GUI

    And use shareprefs key to firewall?
    No cos VPN is built using packageNames even though its underlying UIDs which count

    builder.addAllowedApplication(app.packageNames.get(i));

    We can get all package names from UID but also includes a lot of non
    internet packages for shared system UIDs - so not great.


     */
}
