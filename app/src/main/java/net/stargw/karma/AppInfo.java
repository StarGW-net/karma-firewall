package net.stargw.karma;

import android.graphics.drawable.Drawable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AppInfo implements Cloneable, Serializable {

    public AppInfo() {
        // TODO Auto-generated constructor stub
    }

    public String name;

    // Cannot seralise object and save with drawable;
    // So we make it transient and exclude it
    public transient Drawable icon;

    // I should combine these into a class and include enabled
    // public ArrayList<String> packageNames = null;
    // public ArrayList<String> appNames = null;

    List<AppInfoExtra> appInfoExtra;

    // static Map<Integer, String> packageDetail = new ConcurrentHashMap<Integer, String>();


    public int UID2; // No UID 1

    // public boolean fw = false;

    public int fw = 10; // Allowed
    // 10 = Allowed
    // 20 = New (Allowed)
    // 30 = Firewalled

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

    This Map has to be built before the app can be used
    1) We need it in the GUI to display stuff
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
