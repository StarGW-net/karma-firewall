package net.stargw.karma;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import android.app.Application;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

public class Global extends Application {

	
	private static Context mContext;

	// static ArrayList<AppInfo> appList = new ArrayList<AppInfo>();
	//static Map<Integer, AppInfo> appListFW = new HashMap<Integer, AppInfo>();
	// static ArrayList<HashMap<Integer, AppInfo>> appListFW = new ArrayList<HashMap<Integer, AppInfo>>(); // wow!!

	// This is the main one and only app list
	// This must be populated before the firewall cans start
	// This must be populated before the GUI can be shown
	static Map<Integer, AppInfo> appListFW = new ConcurrentHashMap<Integer, AppInfo>();

	static final int APPLIST_DOING = 1;
	static final int APPLIST_DONE = 2;
	static int appListState = 0;


	// to update the GUI
	static int packageMax = 0;
	static int packageCurrent = 0;

	static NotificationChannel newAppNotificationChannel;
	static NotificationManager notificationManager;

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;

		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

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
	static final String FIREWALL_BOOT = "fw_boot";
	static final String FIREWALL_REPLACE = "fw_replace";
	static final String FIREWALL_QS = "fw_start_qs";
	static final String FIREWALL_WIDGET = "fw_widget";
	static final String FIREWALL_STOP = "fw_stop";
	static final String FIREWALL_STATUS = "fw_status";

	static final String WIDGET_ACTION = "widget_action";

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





	public static void getAppListBackground()
	{

		// If we are already building an app list - don't!
		if (Global.appListState == APPLIST_DOING)
		{
			Logs.myLog("Applist Build in progress...skipping", 2);
			return;
		}

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

			// Logs.myLog("-------------------", 3);

			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction(Global.APPS_LOADING_INTENT);
			mContext.sendBroadcast(broadcastIntent);
		}

		Logs.myLog("Built an installed app list of: " +  Global.appListFW.size(), 2);

		// Tidy up deleted apps - by going through all the shared preference files

		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

		Map<String, ?> allEntries = p.getAll();
		for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
			String k = entry.getKey();
			// Logs.myLog("FW App Stored: " + k + ": " + entry.getValue().toString(),2);
			if (k.contains("FW-")) {
				String u = entry.getKey().substring(3);
				int uid = 0;
				// Logs.myLog("Got: " + u, 2);
				try {
					uid = Integer.parseInt(u);
					if (Global.appListFW.containsKey(uid)) {
						AppInfo app = Global.appListFW.get(uid);
						if (app.flush == true)
						{
							// remove
							p.edit().remove("FW-" + uid).apply();
							Logs.myLog("App Removed via flush: " + uid + " " + app.name, 3);
							Global.appListFW.remove(uid);
						}
					} else {
						p.edit().remove("FW-" + uid).apply();
						Logs.myLog("App Removed via File: " +  uid, 3);
					}
				} catch(NumberFormatException nfe) {
					continue;
				}
			}

		}

		// Notify new apps
		String newAppText = "";

		it = Global.appListFW.keySet().iterator();

		boolean restart = false;

		while (it.hasNext())
		{
			int key = it.next();
			AppInfo thisApp = Global.appListFW.get(key);
			if (thisApp.fw == 25)
			{
				Logs.myLog("Notify App: " +  thisApp.name, 3);
				newAppText = newAppText + thisApp.name + "\n";
				// Change so does not notify again, but still new
				thisApp.fw = 20;
				p.edit().putInt("FW-" + thisApp.UID2, 20).apply();
			}
			if (thisApp.fw == 45)
			{
				restart = true;
				Logs.myLog("Notify App: " +  thisApp.name, 3);
				newAppText = newAppText + thisApp.name + "\n";
				// Change so does not notify again, but still new
				thisApp.fw = 40;
				p.edit().putInt("FW-" + thisApp.UID2, 40).apply();
			}
		}

		if (!newAppText.isEmpty())
		{
			notifyNewApp(newAppText);
		}

		if ( (restart == true) && (Global.getFirewallState() == true) )
		{
			Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
			serviceIntent.putExtra("command", Global.FIREWALL_RESTART); // can we pass app
			Global.getContext().startService(serviceIntent);
		}

		// Write to file - STEVE
		// writeAppListFWFile(Global.appListFW);


		// Finished building - let others build appList
		Global.appListState = APPLIST_DONE;

		p.edit().putBoolean("settingsFirstRun", false).apply();

		// Send to GUI if its listening
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(Global.APPS_REFRESH_INTENT );
		mContext.sendBroadcast(broadcastIntent);

		Global.updateMyWidgets();

		return;

	}


	public static void getAppDetail(PackageInfo packageInfo) {

		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
		int LoggingLevel = p.getInt("LoggingLevel",1);

		AppInfo app = new AppInfo();

		AppInfo appNew = null; // = new AppInfo();

		ApplicationInfo applicationInfo = packageInfo.applicationInfo;

		PackageManager pManager = mContext.getPackageManager();

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


		// We leave getting the icons to the GUI on demand

		try {
			app.icon = pManager.getApplicationIcon(packageName);
		} catch (Exception e) {
			app.icon = getContext().getResources().getDrawable(R.drawable.android);
			Logs.myLog("Cannot get icon!", 3);
		}


		// app.icon = getIcon(pManager,app);

		// app.icon = null;

		if ( (app.internet == true) && (app.enabled == true) )
		{
			AppInfo appFW;
			if (appListFW.containsKey(app.UID2)) {
				appFW = appListFW.get(app.UID2);
				// Logs.myLog("Existing UID " + app.UID2 + " " + packageName, 2 );
				// appFW.icon = getContext().getDrawable(R.drawable.android);

			} else {
				appFW = new AppInfo();
				appFW.icon = app.icon;
				// appFW.icon = null;
				appFW.name = app.name;

				appListFW.put(app.UID2,appFW); // replacing?
			}

			appFW.UID2 = app.UID2;

			// How to handle removal of a system package with shared UID ???

			if (appFW.packageNames == null) {
				Logs.myLog("Got UID, package name " + packageName, 3 );
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

			// Get if its firewalled or not.
			if (p.contains("FW-" + app.UID2))
			{
				appFW.fw = p.getInt("FW-" + app.UID2, 20);
				Logs.myLog("Known App" + app.name, 3);
			} else {
				// write key as false - lets us track apps

				if (p.getBoolean("settingsFirstRun",true) == false) {
					// New app!
					Logs.myLog("New App: " + app.name, 3);

					int autoFW = p.getInt("settingsAutoFW",0);
					if (autoFW == 0) {
						p.edit().putInt("FW-" + app.UID2, 25).apply();
						appFW.fw = 25;
					} else { // Mark for auto firewalling
						p.edit().putInt("FW-" + app.UID2, 45).apply();
						appFW.fw = 45;
					}
				} else {
					p.edit().putInt("FW-" + app.UID2, 10).apply();
					Logs.myLog("UnKnown App" + app.name, 3);
					appFW.fw = 10;
				}
			}

/*
			Logs.myLog("App Name:      " + app.name, 3);
			Logs.myLog("Package Name:  " + packageName, 3);
			// Logs.myLog("Process Name:  " + app.processName, 3);
			Logs.myLog("App UID2:      " + app.UID2, 3);
			Logs.myLog("System:        " + app.system, 3);
*/
			appFW.flush = false;

		}

		return;
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

	// Notify of new app...but what if in GUI at the time...

	public static String createNewAppNotificationChannel() {
		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			String channelId = "FW2";
			String channelName = "FW New App";
			newAppNotificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
			// omitted the LED color
			newAppNotificationChannel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
			newAppNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
			notificationManager.createNotificationChannel(newAppNotificationChannel);
			return channelId;
		} else {
			return "none";
		}
	}


	public static void notifyNewApp(String myText) {
		Intent intent = new Intent(mContext, ActivityMain.class);
		// use System.currentTimeMillis() to have a unique ID for the pending intent
		// PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

		Logs.myLog("Notify New App: " + myText , 2);

		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
				new Intent(mContext, ActivityMain.class), PendingIntent.FLAG_UPDATE_CURRENT);
				// new Intent(mContext, ActivityLogs.class), PendingIntent.FLAG_UPDATE_CURRENT);


		if (Build.VERSION.SDK_INT >= 26) {

			if (newAppNotificationChannel == null) {
				createNewAppNotificationChannel();
			}

			Notification n = new NotificationCompat.Builder(mContext, newAppNotificationChannel.getId())
					.setContentTitle(Global.getContext().getString(R.string.app_name))
					.setContentText("New Apps Found!")
					.setSmallIcon(R.drawable.ic_lock_idle_lock2)
					.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.fw7))
					.setStyle(new NotificationCompat.BigTextStyle()
							.bigText(myText))

					/*
					.setStyle(
							new NotificationCompat.InboxStyle()
									.addLine(myText)
									.addLine("line ""))

					 */
					.setAutoCancel(true)
					.setContentIntent(contentIntent)
					.setAutoCancel(true).build();
			notificationManager.notify(200, n); // we overwrite the current notitication



		} else {

			// build notification
			// the addAction re-use the same intent to keep the example short
			Notification n = new NotificationCompat.Builder(mContext)
					.setContentTitle(Global.getContext().getString(R.string.app_name))
					.setContentText("New Apps Found!")

					.setStyle(new NotificationCompat.BigTextStyle()
							.bigText(myText))

					/*
					.setStyle(
							new NotificationCompat.InboxStyle()
									.addLine(myText)
									.addLine("line ""))

					 */
					.setSmallIcon(R.drawable.ic_lock_idle_lock2)
					.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.fw7))
					.setAutoCancel(true)
					.setContentIntent(contentIntent).build();
			notificationManager.notify(200, n); // we overwrite the current notitication
		}



	}


	public static void updateMyWidgets() {

		Logs.myLog("Updating widgets", 2);

		// Basically all widgets receive all the broadcasts
		// No matter how many Widgets there are the Widget Class
		// is only called once.

		// This is why you should cycle through Widget IDs in the
		// widget itself - not from here...

		Intent updateIntent = new Intent();
		// Action has to be a ACTION_APPWIDGET_* otherwise Widgets
		// will not receive
		updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		updateIntent.putExtra(WIDGET_ACTION, "ALL");
		mContext.sendBroadcast(updateIntent);

	}

}
