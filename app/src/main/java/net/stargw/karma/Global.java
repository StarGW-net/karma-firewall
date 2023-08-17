package net.stargw.karma;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Global extends Application {

	
	private static Context mContext;

	// static ArrayList<AppInfo> appList = new ArrayList<AppInfo>();
	//static Map<Integer, AppInfo> appListFW = new HashMap<Integer, AppInfo>();
	// static ArrayList<HashMap<Integer, AppInfo>> appListFW = new ArrayList<HashMap<Integer, AppInfo>>(); // wow!!

	// This is the main one and only app list
	// This must be populated before the firewall cans start
	// This must be populated before the GUI can be shown
	static Map<Integer, AppInfo> appListFW = new ConcurrentHashMap<Integer, AppInfo>();

	static final int APPLIST_NONE = 0;
	static final int APPLIST_DOING = 1;
	static final int APPLIST_DONE = 2;
	static final int APPLIST_OLD = 3;
	static int appListState = 0;

	static boolean packageDone = false;

	// to update the GUI
	static int packageMax = 0;
	static int packageCurrent = 0;

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;



		Global.getAppListBackground();
	}

	static final String LOG_FILE = "Karma-FW-log.txt";

	static final String APPS_REFRESH_INTENT = "net.stargw.karma.intent.action.APPS_REFRESH";
	static final String APPS_LOADING_INTENT = "net.stargw.karma.intent.action.APPS_LOADING";
	static final String SCREEN_REFRESH_INTENT = "net.stargw.karma.intent.action.REFRESH";
	static final String FIREWALL_STATE_CHANGE = "net.stargw.karma.intent.action.FIREWALL";
	static final String FIREWALL_STATE_ON = "net.stargw.karma.intent.action.FIREWALL_ON";
	static final String FIREWALL_STATE_OFF = "net.stargw.karma.intent.action.FIREWALL_OFF";
	static final String TOGGLE = "net.stargw.karma.intent.action.TOGGLE";
	static final String TOGGLEAPP = "net.stargw.karma.intent.action.TOGGLEAPP";
	static final String TOGGLEAPP_REFRESH = "net.stargw.karma.intent.action.TOGGLEAPP_REFRESH";

	static final String FIREWALL_START = "fw_start";
	static final String FIREWALL_RESTART = "fw_restart";
	static final String FIREWALL_DESTROY_RESTART = "fw_destroy_restart";
	static final String FIREWALL_BOOT = "fw_boot";
	static final String FIREWALL_STOP = "fw_stop";
	static final String FIREWALL_STATUS = "fw_status";

	static int focusUID = 0;





	static Boolean settingsEnableExpert;
	private static Boolean settingsFirewallStateOn = false;
	static Boolean settingsEnableNotifications = true;
	static Boolean settingsEnableBoot = false;
	static Boolean settingsEnableRestart = false;
	static String settingsSubnet = "";
	static int settingsLoggingLevel = 0;

	static int settingsSortOption = 0;

	public static Context getContext(){
		return mContext;
	}

	public static void infoMessageLeft(final Context context, String header, String message)
	{
		infoMessageDo(context, header, message, Gravity.LEFT);
	}

	public static void infoMessage(final Context context, String header, String message)
	{
		infoMessageDo(context, header, message, Gravity.CENTER_HORIZONTAL);

	}


	public static Boolean getFirewallState()
	{
		// SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
		// settingsFirewallStateOn = p.getBoolean("settingsFirewallStateOn", false);

		return settingsFirewallStateOn;
	}

	public static void setFirewallState(Boolean state)
	{
		Intent broadcastIntent = new Intent();
		if (state != settingsFirewallStateOn)
		{
			broadcastIntent.setAction(Global.FIREWALL_STATE_CHANGE);
		} else {
			if (state)
			{
				broadcastIntent.setAction(Global.FIREWALL_STATE_ON);
			} else {
				broadcastIntent.setAction(Global.FIREWALL_STATE_OFF);
			}
		}
		settingsFirewallStateOn = state;
		mContext.sendBroadcast(broadcastIntent);

		Logs.myLog("Firewall Service sent Firewall state broadcast!", 2);

	}

	//
	// Display a popup info screen
	//
	public static void infoMessageDo(final Context context, String header, String message, int i)
	{
		final Dialog info = new Dialog(context);

		info.setContentView(R.layout.dialog_info);
		info.setTitle(header);
		
		TextView text = (TextView) info.findViewById(R.id.infoMessage);
		text.setText(message);
		text.setGravity(i);
		
		Button dialogButton = (Button) info.findViewById(R.id.infoButton);

		
		dialogButton.setOnClickListener(new OnClickListener() {
			// @Override
			public void onClick(View v) {
				// notificationCancel(context);
				info.cancel();
			}
		});
		

		info.show();
		Logs.myLog(header + ":" + message,3);
	}



	public static void getSettings()
	{
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

		settingsEnableNotifications = p.getBoolean("settingsEnableNotifications", true);
		settingsEnableBoot = p.getBoolean("settingsEnableBoot", false);
		// settingsDetailedLogs = p.getBoolean("settingsDetailedLogs", false);
		settingsEnableRestart = p.getBoolean("settingsEnableRestart", false);
		settingsLoggingLevel = p.getInt("settingsLoggingLevel", 1);
		settingsSortOption = p.getInt("settingsSortOption", 0);
		settingsSubnet = p.getString("settingsSubnet", "");
	}

	public static void saveSetings()
	{
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

		p.edit().putBoolean("settingsEnableNotifications", settingsEnableNotifications).apply();
		p.edit().putInt("settingsLoggingLevel", settingsLoggingLevel).commit();
		p.edit().putBoolean("settingsEnableBoot", settingsEnableBoot).apply();
		p.edit().putBoolean("settingsEnableRestart", settingsEnableRestart).apply();
		p.edit().putInt("settingsSortOption", settingsSortOption).apply();
		p.edit().putString("settingsSubnet", settingsSubnet).apply();
	}




	public static void fastAppCheckBackground()
	{

		// Build apps
		Thread thread = new Thread() {
			@Override
			public void run() {
				Global.fastAppCheck();
			}
		};

		thread.start();
	}

	public static void fastAppCheck()
	{
		// If we are building an app list - don't!
		if (Global.appListState != APPLIST_DONE)
		{
			return;
		}

		boolean rebuild = false;

		// Quickly check apps and see if there is any new ones...
		PackageInfo packageInfo;
		List<PackageInfo> packageInfoList = Global.getContext().getPackageManager().getInstalledPackages(0);


		for (int i = 0; i < packageInfoList.size(); i++) {
			Global.packageCurrent = i;
			try {
				packageInfo = packageInfoList.get(i);
				// Global.appList.add(Global.getAppListApp(packageInfo));
			} catch (Exception e) {
				Logs.myLog("Cannot get package info...skipping", 3);
				continue;
			}

			// STEVE
			ApplicationInfo applicationInfo = packageInfo.applicationInfo;
			int uid = applicationInfo.uid;

			boolean internet = false;
			if (packageInfo.requestedPermissions == null)
			{
				internet = false;
			} else {
				for (String permission : packageInfo.requestedPermissions) {
					if (TextUtils.equals(permission, android.Manifest.permission.INTERNET)) {
						internet = true;
						break;
					}
				}
			}

			if (internet == false)
			{
				continue;
			}

			// Check UID against appList...
			if (!(appListFW.containsKey(uid)))
			{
				rebuild = true;
				break;
			}
		}
		if (rebuild == true)
		{
			// signal app change...
		}
	}

	public static void getAppListBackground()
	{

		// Build apps
		Thread thread = new Thread() {
			@Override
			public void run() {
				Global.getAppList();
			}
		};

		thread.start();
	}

	public static void getAppList()
	{

		// If we are building an app list - don't!
		if (Global.appListState == APPLIST_DOING)
		{
			return;
		}

		Global.appListState = APPLIST_DOING;

		// better than iterator?
		// for(int i = 0, l = appListFW.size(); i < l; i++)

		Iterator<Integer> it = Global.appListFW.keySet().iterator();

		while (it.hasNext())
		{
			int key = it.next();
			AppInfo app = Global.appListFW.get(key);
			app.flush = true;
		}

		PackageInfo packageInfo;
		List<PackageInfo> packageInfoList = Global.getContext().getPackageManager().getInstalledPackages(0);

		Global.packageMax = packageInfoList.size();
		Global.packageCurrent = 0;

		for (int i = 0; i < packageInfoList.size(); i++)
		{
			Global.packageCurrent = i;
			try {
				packageInfo = packageInfoList.get(i);
				// Global.appList.add(Global.getAppListApp(packageInfo));
			} catch (Exception e) {
				Logs.myLog("Cannot get package info...skipping", 3);
				continue;
			}


			getAppDetail(packageInfo);
			// Logs.myLog(id + "added: " + app.name + " " + appList.size(), 1);


			Logs.myLog("-------------------", 3);

			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction(Global.APPS_LOADING_INTENT);
			mContext.sendBroadcast(broadcastIntent);
		}


		Logs.myLog("Built an installed app list of: " +  Global.appListFW.size(), 2);


		it = Global.appListFW.keySet().iterator();

		while (it.hasNext())
		{
			int key = it.next();
			AppInfo app = Global.appListFW.get(key);

			if (app.flush == true)
			{
				SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
				p.edit().remove("FW-" + app.UID2).apply();
				Logs.myLog("Will remove: " +  app.UID2, 2);
				// Global.appListFW.remove(key);
				it.remove();
			}
		}

		// Write to file - STEVE
		// writeAppListFWFile(Global.appListFW);

		// Global.duplicateUIDs();
		Global.appListState = APPLIST_DONE;

		// Send to GUI if its listening
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(Global.APPS_REFRESH_INTENT );
		mContext.sendBroadcast(broadcastIntent);

		return;

	}




	public static void getAppDetail(PackageInfo packageInfo) {

		long t1 = System.currentTimeMillis();

		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
		int LoggingLevel = p.getInt("LoggingLevel",1);

		AppInfo app = new AppInfo();

		ApplicationInfo applicationInfo = packageInfo.applicationInfo;

		PackageManager pManager = mContext.getPackageManager();
		// ActivityManager aManager = (ActivityManager) mContext.getSystemService(mContext.ACTIVITY_SERVICE);

		String packageName = packageInfo.packageName;
		// app.versionName = packageInfo.versionName;
		// app.sourcePath = applicationInfo.sourceDir;

		app.internet = false;
		try {
			packageInfo = Global.getContext().getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);

			if (packageInfo.requestedPermissions == null)
			{

			} else {

				for (String permission : packageInfo.requestedPermissions) {
					if (TextUtils.equals(permission, android.Manifest.permission.INTERNET)) {
						app.internet = true;
						break;
					}
				}
			}

		} catch (PackageManager.NameNotFoundException e) {
			if (LoggingLevel > 2) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			if (LoggingLevel > 2) {
				e.printStackTrace();
			}
		}

		if (app.internet == false)
		{
			// We are not interested in package
			return;
		}

		app.name = pManager.getApplicationLabel(applicationInfo).toString();
		if (app.name == null)
		{
			app.name = packageName;
		}

		/*
		Configuration config = new Configuration();
		config.locale = Resources.getSystem().getConfiguration().locale;
		final Resources galleryRes;
		try {
			galleryRes = pManager.getResourcesForApplication(app.name);
			galleryRes.updateConfiguration(config, mContext.getResources().getDisplayMetrics());
			String localizedLabel = galleryRes.getString(applicationInfo.labelRes);
			app.name = localizedLabel;
			Logs.myLog("Localised name: " + app.name, 3);
		} catch (Exception e) {
			if (LoggingLevel > 2) {
				Logs.myLog("Cannot get localised name: " + app.name, 3);
			}
		}
		*/

		app.enabled = applicationInfo.enabled;
		app.UID2 = applicationInfo.uid; // change to a string??

		// String[] parts = applicationInfo.processName.split(":");
		// app.processName = parts[0];

		app.system = false;
		// Is it a system apps
		if (!((applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0)) {
			app.system = true;
		}

		try {
			// override system app if the app has a launcher
			if (pManager.getLaunchIntentForPackage(packageInfo.packageName) != null)
			{
				app.system = false;
			}
		} catch (Exception e) {
			if (LoggingLevel > 2) {
				e.printStackTrace();
			}
		}

		// override system app if the app has a UID of 1000 or smaller
		if (app.UID2 <= 1000)
		{
			app.system = true;
		}


		// override
		// app.system = Global.checkOverride(packageInfo.packageName, app.system);



/*
		// We leave getting the icons to the GUI one demand
		try {
			app.icon = pManager.getApplicationIcon(packageName);
		} catch (Exception e) {
			app.icon = getContext().getResources().getDrawable(R.drawable.android);
			Logs.myLog("Cannot get icon!", 3);
		}
*/
		app.icon = null;

		if ( (app.internet == true) && (app.enabled == true) )
		{
			AppInfo appFW;
			if (appListFW.containsKey(app.UID2)) {
				appFW = appListFW.get(app.UID2);
				// Logs.myLog("Existing UID " + app.UID2 + " " + packageName, 2 );
				// appFW.icon = getContext().getDrawable(R.drawable.android);

			} else {
				appFW = new AppInfo();
				// appFW.icon = app.icon;
				appFW.icon = null;
				appFW.name = app.name;

				appListFW.put(app.UID2,appFW); // replacing?
			}

			appFW.UID2 = app.UID2;

			// How to handle removal of a system package with shared UID ???

			if (appFW.packageNames == null) {
				Logs.myLog("New UID, New package name " + packageName, 3 );
				appFW.packageNames = new ArrayList<String>();
				appFW.packageNames.add(packageName);
				appFW.appNames = new ArrayList<String>();
				appFW.appNames.add(app.name);
			} else {
				boolean exists = false;
				for (int i =0; i< appFW.packageNames.size(); i++)
				{
					String ap = appFW.packageNames.get(i);
					if ( (ap != null) && (ap.equals(packageName)) )
					{
						// Logs.myLog("Existing package name " + packageName, 2 );
						exists = true;
						break;
					}
				}
				if (exists == false) {
					// Logs.myLog("Existing UID, New package name " + packageName, 2 );
					appFW.packageNames.add(packageName);
					appFW.appNames.add(app.name); // CRASH HERE
					// java.lang.NullPointerException: Attempt to invoke virtual method 'boolean java.util.ArrayList.add(java.lang.Object)' on a null object reference
				}
			}

			if (appFW.packageNames.size() > 1) {
				// appFW.icon = getContext().getResources().getDrawable(R.drawable.android);
				appFW.name = "_Apps (UID " + app.UID2 + ")";
			}

			/*
			if (appFW.appNames == null) {
				appFW.appNames = new ArrayList<String>();
			}
			appFW.appNames.add(app.name);
			*/

			appFW.enabled = app.enabled;
			appFW.internet = true;
			appFW.system = app.system;


			// SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

			// Get if its firewalled or not.
			// Maybe test to see if key exists and if not write false
			appFW.fw = p.getBoolean("FW-" + app.UID2, false);

			Logs.myLog("App Name:      " + app.name, 3);
			Logs.myLog("Package Name:  " + packageName, 3);
			// Logs.myLog("Process Name:  " + app.processName, 3);
			Logs.myLog("App UID2:      " + app.UID2, 3);
			Logs.myLog("System:        " + app.system, 3);

			appFW.flush = false;

		}

	}

	public static Bitmap drawableToBitmap (Drawable drawable) {
		Bitmap bitmap = null;

		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if(bitmapDrawable.getBitmap() != null) {
				return bitmapDrawable.getBitmap();
			}
		}

		if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
		} else {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		}

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	public static void getIcon (PackageManager pManager, AppInfo apps)
	{
		if (apps.icon == null) {
			// Logs.myLog("App get icon: " + apps.name, 1);

			if (apps.packageNames.size() > 1) {
				apps.icon = getContext().getResources().getDrawable(R.drawable.android);
			} else {
				try {
					apps.icon = pManager.getApplicationIcon(apps.packageNames.get(0));
				} catch (Exception e) {
					apps.icon = getContext().getResources().getDrawable(R.drawable.android);
					Logs.myLog("Cannot get icon: " + apps.name, 3);
				}
			}
		}
	}

	//
	// Write the list of firewall apps object to file
	//
	public static void writeAppListFWFile(Map<Integer, AppInfo> appData) {
		File appDir = mContext.getFilesDir();
		File mypath = new File(appDir, "fwapps");
		try {
			FileOutputStream myFile = new FileOutputStream(mypath);
			// FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			ObjectOutputStream os = new ObjectOutputStream(myFile);
			os.writeObject(appData);
			os.close();
			myFile.close();
		} catch (IOException e) {
			Logs.myLog("Cannot write FW objects to: " + "fwapps", 3);
		}
	}


}
