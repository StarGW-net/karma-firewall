package net.stargw.karma;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;


import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static net.stargw.karma.Global.getContext;


// public class ActivityMain extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener, ActivityMainListener  {
public class ActivityMain extends Activity implements ActivityMainListener{

    private static long currentProgress = 0;

    private Dialog appLoad = null;
    private static final String TAG = "FOK";

    private boolean appCreated = false;

    private BroadcastListener mReceiver;

    // ArrayList<AppInfo> appInfo;

    // Master list of apps - copied from a global list built by the service
    ArrayList<AppInfo> appInfoSource;
    private AppInfoAdapterFW adapter;
    private ListView listView;

    Context myContext;

    private class BroadcastListener extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            // Logs.myLog("App Received intent", 2);

            // Log.w("FWMain", "Got Action = " + intent.getAction());

            if (Global.SCREEN_REFRESH_INTENT.equals(intent.getAction()))
            {
                Logs.myLog("App Received intent to update screen", 2);
                // close dialog just in case we are stil showing it
                if (appLoad != null) {
                    Logs.myLog("Close dialog", 2);

                    appLoad.cancel();  // can I take progress input from service?
                    appLoad.dismiss();
                    appLoad = null;
                }
                screenRefresh();
            }

            if (Global.APPS_LOADING_INTENT.equals(intent.getAction())) {
                // Logs.myLog("App Loading intent received", 2);
                // Global.myLog("App Received intent to update saving", 2);
                // close dialog just in case we are stil showing it
                if (appLoad != null) {
                    ProgressBar progBar = (ProgressBar) appLoad.findViewById(R.id.progBar);
                    currentProgress = currentProgress + 1;
                    int x1 = (int) (currentProgress /(float)Global.packageMax*100);
                    // Global.myLog("Progress = " + x1, 2);
                    // Global.myLog("Progress = " + currentProgress + "/" +Global.packageMax, 2);
                    progBar.setProgress(x1);
                }
            }

            if (Global.APPS_REFRESH_INTENT.equals(intent.getAction()))
            {
                Logs.myLog("App Received intent to update apps", 2);
                if (Global.appListFW != null)
                {
                    Logs.myLog("Redisplay Apps", 2);

                    // reset adapter to app list built by service

                    Logs.myLog("Rebuilt app list", 2);

                    // close the dialog box if we had one open
                    if (appLoad != null) {
                        Logs.myLog("Close dialog", 2);

                        appLoad.cancel();  // can I take progress input from service?
                        appLoad.dismiss();
                        appLoad = null;

                        AppWidgetManager man = AppWidgetManager.getInstance(myContext);
                        int[] ids = man.getAppWidgetIds(
                                new ComponentName(myContext,FOKWidget2Provider.class));
                        for (int i=0; i<ids.length; i++) {
                            int appWidgetId = ids[i];
                            Intent updateIntent = new Intent();
                            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                            // updateIntent.putExtra(MyWidgetProvider.WIDGET_DATA_KEY, data);
                            myContext.sendBroadcast(updateIntent);
                        }
                    }
                    appRefresh();
                    // createGUI(); // recreate..?

                }
            }


            if (Global.FIREWALL_STATE_CHANGE.equals(intent.getAction()))
            {
                Logs.myLog("App Received intent to update firewall state", 2);
                if (adapter != null)
                {
                    if (Global.getFirewallState() == true) {
                        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)) {
                            Global.infoMessage(myContext, getString(R.string.dialog_warning), getString(R.string.notify_firewall_block_all));
                        }

                        Iterator<Integer> it = Global.appListFW.keySet().iterator();

                        while (it.hasNext())
                        {
                            int key = it.next();
                            AppInfo thisApp = Global.appListFW.get(key);
                            thisApp.date = 0;
                        }
                    }
                    updateFirewallStateDisplay(true);
                }
            }
            if (Global.FIREWALL_STATE_ON.equals(intent.getAction()) || Global.FIREWALL_STATE_OFF.equals(intent.getAction())) {
                updateFirewallStateDisplay(true);
            }

        }
    };

    void updateFirewallStateDisplay(boolean fromService)
    {
        if (Global.getFirewallState() == false) {
            findViewById(R.id.activity_main_firewall_message).setVisibility(View.VISIBLE);
            ImageView i = (ImageView) findViewById(R.id.activity_main_firewall_icon);
            i.setColorFilter(Color.RED);
        } else {
            findViewById(R.id.activity_main_firewall_message).setVisibility(View.GONE);
            ImageView i = (ImageView) findViewById(R.id.activity_main_firewall_icon);
            i.setColorFilter(Color.GREEN);
        }
    }

    @Override
     protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main2);

        appCreated = true;

        myContext = this;

        Global.getSettings();
        Logs.getLoggingLevel();

        // When app starts show user apps
        Global.settingsEnableExpert = false;

        Logs.myLog("ActivityMain App Created", 2);

        // Checking the state variable here would probably work
        /*
        Intent serviceIntentFW = new Intent(getBaseContext(), FOKServiceFW.class);
        serviceIntentFW.putExtra("command", Global.FIREWALL_STATUS);
        myContext.startService(serviceIntentFW);
        */

        createGUI();





    }

    void displayAppDialog()
    {
        // Display a loading dialog box until the app list is prepared by the service.
        appLoad = new Dialog(myContext);
        // appLoad = new ProgressDialog(myContext);

        currentProgress = 0;

        // appLoad.setContentView(R.layout.dialog_load);
        appLoad.setContentView(R.layout.dialog_progress);
        appLoad.setTitle(R.string.LoadingApps);

        TextView text = (TextView) appLoad.findViewById(R.id.infoMessage);
        text.setText(R.string.BuildingApps);

        text = (TextView) appLoad.findViewById(R.id.infoMessage2);
        text.setText(R.string.InfoLoadingApps);
        // text.setGravity(i);

        // Runs on GUI thread (should I use a service)
        Thread thread = new Thread() {
            @Override
            public void run() {
                Global.getAppList();
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(Global.APPS_REFRESH_INTENT );
                myContext.sendBroadcast(broadcastIntent);
            }
        };

        thread.start();

        appLoad.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Logs.myLog("VPNService Prepare onActivityResult", 2);

        // catch the result for FW prep

        Logs.myLog("Request Code Received: " + requestCode, 2);
        if (requestCode == 666)
        {
            Logs.myLog("Result Code Received: " + resultCode, 2);
            if (resultCode == RESULT_OK) {
                // ServiceSinkhole.start("prepared", this);
                Intent serviceIntent = new Intent(myContext, FOKServiceFW.class);
                serviceIntent.putExtra("command", Global.FIREWALL_START);
                myContext.startService(serviceIntent);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

        // Global.FW = true;
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Runs TWICE on startup. WHY? WHY? WHY?
        Logs.myLog("Activity Main App Resumed", 2);

        // Get a  list of apps a current data?
        screenRefresh();
        updateFirewallStateDisplay(false);

        Logs.checkLogSize();

        // register receiver
        mReceiver = new BroadcastListener();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Global.FIREWALL_STATE_CHANGE);
        mIntentFilter.addAction(Global.FIREWALL_STATE_OFF);
        mIntentFilter.addAction(Global.FIREWALL_STATE_ON);
        mIntentFilter.addAction(Global.SCREEN_REFRESH_INTENT);
        mIntentFilter.addAction(Global.APPS_REFRESH_INTENT);
        mIntentFilter.addAction(Global.APPS_LOADING_INTENT);
        mIntentFilter.addAction(Global.TOGGLEAPP_REFRESH);

        registerReceiver(mReceiver, mIntentFilter);

        // Count packages - a quick and dirty way to see if there have been any changes
        // Rather than using a package broadcast receiver
        List<PackageInfo> packageInfoList = Global.getContext().getPackageManager().getInstalledPackages(0);
        int packages = packageInfoList.size();

        Logs.myLog("Previous packages = " + Global.packageMax, 2);
        Logs.myLog("Current packages = " + packages, 2);

        // App build list should be underway
        if ( (Global.packageDone == false) || (Global.packageMax != packages) ) {
            displayAppDialog();
        } else {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());

            Iterator<Integer> it = Global.appListFW.keySet().iterator();

            while (it.hasNext()) {
                int key = it.next();
                AppInfo app = Global.appListFW.get(key);
                app.fw = p.getBoolean("FW-" + app.UID2, false);
            }

            appRefresh();
        }

    }



    @Override
    protected void onStop() {
        super.onStop();  // Always call the superclass method first
        Logs.myLog("Activity Main App Stopped", 2);

    }


    public void showOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.menu_main);
        // getMenuInflater().inflate(R.menu.menu_main, menu);

        EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);
        if (myFilter.getVisibility() == View.VISIBLE) {
            myFilter.setVisibility(View.GONE);
            myFilter.setText("");
        }


        Menu m = popup.getMenu();
        MenuItem item = m.findItem(R.id.action_apps_system);


        if (Global.settingsEnableExpert == true) {
            item.setTitle(R.string.activity_main_menu_apps_user);
        } else {
            item.setTitle(R.string.activity_main_menu_apps_system);
        }

        item = m.findItem(R.id.action_boot);
        item.setChecked(Global.settingsEnableBoot);

        // item = m.findItem(R.id.action_restart);
        // item.setChecked(Global.settingsEnableRestart);

        item = m.findItem(R.id.action_subnet);

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
        Global.settingsSubnet = p.getString("settingsSubnet", "");

        if (Global.settingsSubnet != "")
        {
            item.setTitle(getResources().getString(R.string.activity_main_menu_subnet) + Global.settingsSubnet);
        } else {
            item.setTitle(getResources().getString(R.string.activity_main_menu_subnet) + "None");
        }


        // popup.getMenu().findItem(R.id.menu_name).setTitle(text);
        invalidateOptionsMenu();


        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {


            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_logs:
                        Intent intent = new Intent(myContext, ActivityLogs.class);
                        startActivity(intent);
                        return true;
                    case R.id.action_apps_system:
                        if (Global.settingsEnableExpert == true) {
                            Global.settingsEnableExpert = false;
                        } else {
                            Global.settingsEnableExpert = true;
                        }
                        Global.saveSetings();
                        appRefresh();
                        return true;
                    case R.id.action_apps_toggle:
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                        {
                            // nothing
                        } else {
                            toggleAll(myContext);
                            screenRefresh();
                        }
                        return true;

                    case R.id.action_boot:
                        if(Global.settingsEnableBoot)
                        {
                            Global.settingsEnableBoot = false;
                        } else {
                            Global.settingsEnableBoot = true;
                            // Global.infoMessage(myContext,"Start on Boot","You may also need to enable Always-on VPN in Android settings");
                            final Dialog info = new Dialog(myContext);

                            info.setContentView(R.layout.dialog_info);
                            info.setTitle(R.string.activity_main_menu_boot);

                            TextView text = (TextView) info.findViewById(R.id.infoMessage);
                            text.setText("You may also need to enable Always-on VPN in Android settings...");
                            // text.setGravity(i);

                            Button dialogButton = (Button) info.findViewById(R.id.infoButton);


                            dialogButton.setOnClickListener(new View.OnClickListener() {
                                // @Override
                                public void onClick(View v) {
                                    // notificationCancel(context);
                                    info.cancel();
                                    Intent i = new Intent("android.net.vpn.SETTINGS");
                                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(i);
                                }
                            });
                            info.show();
                        }
                        Global.saveSetings();
                        return false;
                    case R.id.action_always:
                        final Dialog info = new Dialog(myContext);

                        info.setContentView(R.layout.dialog_info);
                        info.setTitle(R.string.activity_main_menu_always);

                        TextView text = (TextView) info.findViewById(R.id.infoMessage);
                        text.setText("Please enable Always-on VPN in Android settings...");
                        // text.setGravity(i);

                        Button dialogButton = (Button) info.findViewById(R.id.infoButton);


                        dialogButton.setOnClickListener(new View.OnClickListener() {
                            // @Override
                            public void onClick(View v) {
                                // notificationCancel(context);
                                info.cancel();
                                Intent i = new Intent("android.net.vpn.SETTINGS");
                                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                            }
                        });
                        info.show();

                        if (Global.getFirewallState() == false) {
                            // turn on the firewall as well..
                            Logs.myLog("VPNService Prepare...", 2);
                            Intent i = VpnService.prepare(myContext);
                            if (i != null) {
                                Logs.myLog("VPNService Prepare Intent Not Null", 2);
                                startActivityForResult(i, 666); // Add a custom code?
                            } else {
                                Logs.myLog("VPNService Prepare Intent Null", 2);
                                onActivityResult(666, RESULT_OK, null);
                            }
                        }

                        return false;
                    case R.id.action_stats:
                        try {
                            //Open battery stats page
                            // Intent intent2 = new Intent(Intent.ACTION_MANAGE_NETWORK_USAGE);
                            // startActivity(intent2);
                            Intent intent2 = new Intent(Intent.ACTION_MAIN);
                            intent2.setComponent(new ComponentName("com.android.settings",
                                    "com.android.settings.Settings$DataUsageSummaryActivity"));
                            intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent2);

                        } catch ( ActivityNotFoundException e ) {
                            Toast.makeText(myContext, "Cannot " + myContext.getString(R.string.activity_main_menu_stats), Toast.LENGTH_SHORT).show();
                            // Toast.makeText(myContext, "Cannot show stats!", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    case R.id.action_refresh:
                        displayAppDialog();
                        return true;
                    case R.id.action_subnet:
                        enterSubnet();
                        return true;
                    case R.id.action_help:
                        showHelp();
                        return true;
                    case R.id.action_donate:
                        showDonate();
                        return true;
                    case R.id.action_apps_sort:
                        sortOptionPickerDropdown();
                        return true;
                    default:
                        return false;
                }
            }

        });
        popup.show();
    }

    @Override
    protected void onPause() {
        if(mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onPause();
    }

    @Override
    public void onBackPressed()
    {
        EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);

        if (myFilter.getVisibility() == View.VISIBLE) {
            myFilter.setVisibility(View.GONE);
            myFilter.setText("");
        } else {
            super.onBackPressed();
        }
    }

    public void screenRefresh()
    {
        // nothing
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void appRefresh()
    {
        // rebuild the app list
        Iterator<Integer> it = Global.appListFW.keySet().iterator();
        appInfoSource = new ArrayList<AppInfo>();

        while (it.hasNext())
        {
            int key = it.next();
            AppInfo thisApp = Global.appListFW.get(key);
            if (thisApp.system == Global.settingsEnableExpert) {
                appInfoSource.add(thisApp);
                Logs.myLog("Add GUI app: " + thisApp.name, 3);
            }
        }

        switch (Global.settingsSortOption) {
            case 0:
                mySort0(appInfoSource);
                break;
            case 1:
                mySort1(appInfoSource);
                break;
            case 2:
                mySort2(appInfoSource);
                break;
            case 3:
                mySort3(appInfoSource);
                break;
            case 4:
                mySort4(appInfoSource);
                break;
            case 5:
                mySort5(appInfoSource);
                break;
            default:
                break;
        }


        adapter = new AppInfoAdapterFW(myContext, appInfoSource);
        adapter.updateFull();
        // notify?
        // adapter.notifyDataSetChanged();
        listView = (ListView) findViewById(R.id.listViewApps);
        listView.setAdapter(adapter);

        setListViewFocus();
    }



    private void createGUI() {

        final Context c = myContext;

        TextView text1 = (TextView) findViewById(R.id.activity_main_title);
        text1.setText(R.string.activity_main_menu_title);

        ImageView btn = (ImageView) findViewById(R.id.activity_main_menu_icon);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showOptionsMenu(v);
            }
        });

        // show an apps list if we have one.
        // if we do not have one it should be being built by the service
        // then we get a notification and will update screen
        /*
        if (Global.appListFW != null) {
            listView = (ListView) findViewById(R.id.listViewApps);
            listView.setAdapter(adapter);
        }
        */

        EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);
        myFilter.setVisibility(View.GONE);

        ImageView mySearch = (ImageView) findViewById(R.id.activity_main_filter_icon);

        mySearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //v.getId() will give you the image id

                EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);

                if (myFilter.getVisibility() == View.GONE) {

                    myFilter.setVisibility(View.VISIBLE);
                    myFilter.setFocusableInTouchMode(true);
                    myFilter.requestFocus();

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    // imm.showSoftInput(myFilter, InputMethodManager.SHOW_IMPLICIT);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

                    myFilter.addTextChangedListener(new TextWatcher() {

                        public void afterTextChanged(Editable s) {
                        }

                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                            // Logs.myLog("Filter on text: " + s , 3);
                            adapter.getFilter().filter(s.toString()); // CRASH REPORTED HERE
                        }
                    });
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(myFilter.getWindowToken(), 0);

                    myFilter.setVisibility(View.GONE);
                    myFilter.setText("");
                    // adapter.getFilter().filter(null);
                }

            }
        });

        findViewById(R.id.activity_main_firewall_icon).setVisibility(View.VISIBLE);

        ImageView toggle = (ImageView) findViewById(R.id.activity_main_firewall_icon);
        if (Global.getFirewallState() == false) {
            toggle.setColorFilter(Color.RED);
            findViewById(R.id.activity_main_firewall_message).setVisibility(View.VISIBLE);
        } else {
            toggle.setColorFilter(Color.GREEN);
            findViewById(R.id.activity_main_firewall_message).setVisibility(View.GONE);
        }

        findViewById(R.id.activity_main_firewall_icon).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Logs.myLog("Toggle Firewall State Prompt", 2);
                confirmFirewallStateChange(c);
            }
        });


    }

    //
    // Display the help screen
    //
    private void showHelp()
    {

        String verName = "latest";
        try {

            PackageInfo pInfo = myContext.getPackageManager().getPackageInfo(getPackageName(), 0);
            verName = pInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            Logs.myLog("Could not get version number", 3);
        }

        String url = "https://www.stargw.net/android/fok/help.html?ver=" + verName;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);

    }

    //
    // Display the help screen
    //
    private void showDonate()
    {

        String appName = getString(R.string.app_name);

        String url = "https://www.stargw.net/android/donate.html?app=" + appName;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);

    }


    private void enterSubnet()
    {

        Context c = myContext;

        final Dialog info = new Dialog(c);
        // final AppInfo thisApp = app;

        info.setContentView(R.layout.dialog_confirm_subnet);

        TextView text = (TextView) info.findViewById(R.id.confirmMessageSubnet);

        text.setGravity(Gravity.CENTER_HORIZONTAL);

        text.setText(R.string.dialog_subnet_message);
        info.setTitle(R.string.dialog_subnet);

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
        Global.settingsSubnet = p.getString("settingsSubnet", "");

        int v1 = 10;
        int v2 = 0;
        int v3 = 0;
        int v4 = 0;
        int v5 = 24;

        if (Global.settingsSubnet != "")
        {
            Logs.myLog("Read Subnet: " + Global.settingsSubnet, 2);
            String[] parts = Global.settingsSubnet.split("\\.");
            try {
                v1 = Integer.parseInt(parts[0].toString());
                v2 = Integer.parseInt(parts[1].toString());
                v3 = Integer.parseInt(parts[2].toString());
                String[] parts2 = parts[3].split("/");
                v4 = Integer.parseInt(parts2[0].toString());
                v5 = Integer.parseInt(parts2[1].toString());
            } catch (Exception nfe) {
                v1 = 10;
                v2 = 0;
                v3 = 0;
                v4 = 0;
                v5 = 24;
                Logs.myLog("Cannot convert to num reading subnet!", 2);
            }
        }

        EditText et1 = (EditText) info.findViewById(R.id.subnet1);
        et1.setText(Integer.toString(v1));

        EditText et2 = (EditText) info.findViewById(R.id.subnet2);
        et2.setText(Integer.toString(v2));


        EditText et3 = (EditText) info.findViewById(R.id.subnet3);
        et3.setText(Integer.toString(v3));


        EditText et4 = (EditText) info.findViewById(R.id.subnet4);
        et4.setText(Integer.toString(v4));


        EditText et5 = (EditText) info.findViewById(R.id.subnetMask);
        et5.setText(Integer.toString(v5));

        Button noButton = (Button) info.findViewById(R.id.subnetCancelButton);


        noButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);
                info.cancel();
            }
        });

        Button allowButton = (Button) info.findViewById(R.id.subnetAllowButton);

        allowButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {

                EditText et1 = (EditText) info.findViewById(R.id.subnet1);
                String s1 = isNumber(et1.getText().toString(),255);

                EditText et2 = (EditText) info.findViewById(R.id.subnet2);
                String s2 = isNumber(et2.getText().toString(),255);

                EditText et3 = (EditText) info.findViewById(R.id.subnet3);
                String s3 = isNumber(et3.getText().toString(),255);

                EditText et4 = (EditText) info.findViewById(R.id.subnet4);
                String s4 = isNumber(et4.getText().toString(),255);

                EditText et5 = (EditText) info.findViewById(R.id.subnetMask);
                String s5 = isNumber(et5.getText().toString(),32);

                if ( (s1 == null) || (s2 == null)  || (s3 == null)  || (s4 == null)  || (s5 == null) )
                {
                    Toast.makeText(myContext, "Invalid IP Subnet MASK", Toast.LENGTH_SHORT).show();
                } else {

                    String subnet = s1 + "." + s2 + "." + s3 + "." + s4 + "/" + s5;

                    Logs.myLog("Write Subnet: " + subnet, 2);

                    Global.settingsSubnet = subnet;
                    Global.saveSetings();

                    info.cancel();

                    if (Global.getFirewallState() == true) {
                        Intent serviceIntent = new Intent(Global.getContext(), FOKServiceFW.class);
                        serviceIntent.putExtra("command", Global.FIREWALL_RESTART); // can we pass app
                        Global.getContext().startService(serviceIntent);
                    }
                }
            }

        });

        Button blockButton = (Button) info.findViewById(R.id.subnetClearButton);

        blockButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);
                // loop through all apps...
                Global.settingsSubnet = "";
                Global.saveSetings();
                info.cancel();
                if (Global.getFirewallState() == true) {
                    Intent serviceIntent = new Intent(Global.getContext(), FOKServiceFW.class);
                    serviceIntent.putExtra("command", Global.FIREWALL_RESTART); // can we pass app
                    Global.getContext().startService(serviceIntent);
                }
            }

        });

        info.show();
        // Logs.myLog(header + ":" + message,3);

    }

    private String isNumber(String s, int max)
    {

        try {
            int i = Integer.parseInt(s.toString());
            if ( (i < 0) || (i > max))
            {
                return null;
            }
            return s;
        } catch (NumberFormatException nfe) {
            // usually a blank cell - we check for this later
            Logs.myLog("Cannot convert to num: " + s.toString(), 3);
            return "0";
        }

    }

    public void changeSelectedItem(int position) {
        AppInfo thisApp = adapter.getItem(position);
        toggleFirewallApp(thisApp);
        // adapter.notifyDataSetChanged();
        // Do your remove functionality here
    }

    @Override
    public void setListViewFocus() {
        Logs.myLog("setListViewFocus", 3);

        ListView listView = (ListView) findViewById(R.id.listViewApps);

        // update the list that drives the adaptor
        // appInfo.clear(); // clear it quickly
        // appInfo.addAll(appInfoSource); // then replace
        adapter.notifyDataSetChanged();
        adapter.updateFull();

        if (Global.focusUID != 0 ) {


            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).UID2 == Global.focusUID) {
                    if ( (i < listView.getFirstVisiblePosition()) || (i >listView.getLastVisiblePosition()) ) {
                        listView.setSelection(i);
                        Logs.myLog("setting focus to: " + i, 3);
                    } else {
                        Logs.myLog("Already on screen! " + i, 3);
                    }
                    Global.focusUID = 0; // we only do it once!
                    break;
                }
            }
        }
    }


    private void toggleFirewallApp(AppInfo thisApp)
    {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            // old version - apps cannot be changed
            return;
        }

        if (thisApp.fw == true) {
            thisApp.fw = false;
        } else {
            thisApp.fw = true;
        }

        // change the global - which may have been destroyed!!
        if (Global.appListFW.containsKey(thisApp.UID2)) {
            Global.appListFW.get(thisApp.UID2).date = 1;
            Global.appListFW.get(thisApp.UID2).fw = thisApp.fw;
        }

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
        p.edit().putBoolean("FW-" + thisApp.UID2, thisApp.fw).apply();
        // STEVE should update Global!

        AppWidgetManager man = AppWidgetManager.getInstance(myContext);
        int[] ids = man.getAppWidgetIds(
                new ComponentName(myContext,FOKWidget2Provider.class));
        for (int i=0; i<ids.length; i++) {
            int appWidgetId = ids[i];
            Intent updateIntent = new Intent();
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            // updateIntent.putExtra(MyWidgetProvider.WIDGET_DATA_KEY, data);
            myContext.sendBroadcast(updateIntent);
        }

        // need to refocus on app cos may change if sorted!
        Global.focusUID = thisApp.UID2;

        // adapter.notifyDataSetChanged(); // maybe need this

        if (Global.getFirewallState() == true) {

            Intent serviceIntent = new Intent(Global.getContext(), FOKServiceFW.class);
            serviceIntent.putExtra("command", Global.FIREWALL_RESTART); // can we pass app
            // serviceIntent.putExtra("restart", "app");
            Global.getContext().startService(serviceIntent);
            // thisApp.bytesFWOut = TrafficStats.getUidTxBytes(thisApp.UID2);
            // thisApp.bytesFWIn = TrafficStats.getUidRxBytes(thisApp.UID2);
        }

        screenRefresh(); // cos firewall may be off

        Logs.myLog("Firewall Changed App: " + thisApp.name, 3);
    }


    //
    // Display a popup info screen
    //
    public void confirmFirewallStateChange(Context c) {
        final Dialog info = new Dialog(c);

        // final AppInfo thisApp = app;

        info.setContentView(R.layout.dialog_confirm);

        TextView text = (TextView) info.findViewById(R.id.confirmMessage);

        ImageView toggle = (ImageView) findViewById(R.id.activity_main_firewall_icon);
        if (Global.getFirewallState() == true) {
            text.setText(R.string.disable_firewall_message);
            info.setTitle(R.string.disable_firewall);
        } else {
            text.setText(R.string.enable_firewall_message);
            info.setTitle(R.string.enable_firewall);
        }

        text.setGravity(Gravity.CENTER_HORIZONTAL);

        Button noButton = (Button) info.findViewById(R.id.noButton);


        noButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);
                info.cancel();
            }
        });

        Button yesButton = (Button) info.findViewById(R.id.yesButton);

        yesButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);

                if (Global.getFirewallState() == true) {
                    Intent serviceIntent = new Intent(Global.getContext(), FOKServiceFW.class);
                    serviceIntent.putExtra("command", Global.FIREWALL_STOP);
                    Global.getContext().startService(serviceIntent);
                } else {
                    // Intent intent = VpnService.prepare(getApplicationContext());
                    Logs.myLog("VPNService Prepare...",2);
                    Intent intent = VpnService.prepare(myContext);
                    if (intent != null) {
                        Logs.myLog("VPNService Prepare Intent Not Null",2);
                        startActivityForResult(intent, 666); // Add a custom code?
                    } else {
                        Logs.myLog("VPNService Prepare Intent Null",2);
                        onActivityResult(666, RESULT_OK, null);
                    }

                }
                info.cancel();

            }

        });

        info.show();
        // Logs.myLog(header + ":" + message,3);
    }


    private void toggleAll(Context c) {
        final Dialog info = new Dialog(c);

        // final AppInfo thisApp = app;

        info.setContentView(R.layout.dialog_confirm_toggle);

        TextView text = (TextView) info.findViewById(R.id.confirmMessageB);

        text.setGravity(Gravity.CENTER_HORIZONTAL);

        if (Global.settingsEnableExpert) {
            text.setText(R.string.dialog_toggleAll_message_system);
        } else {
            text.setText(R.string.dialog_toggleAll_message);
        }
        info.setTitle(R.string.dialog_toggleAll);

        Button noButton = (Button) info.findViewById(R.id.cancelButton);


        noButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);
                info.cancel();
            }
        });

        Button allowButton = (Button) info.findViewById(R.id.allowButton);

        allowButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);
                // loop through all apps...
                toggleApps(false);
                info.cancel();
                screenRefresh();
            }

        });

        Button blockButton = (Button) info.findViewById(R.id.blockButton);

        blockButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);
                // loop through all apps...
                toggleApps(true);
                info.cancel();
                screenRefresh();
            }

        });

        info.show();
        // Logs.myLog(header + ":" + message,3);
    }

    private void toggleApps(boolean state) {
        Iterator<Integer> it = Global.appListFW.keySet().iterator();

        while (it.hasNext()) {
            int key = it.next();
            AppInfo app = Global.appListFW.get(key);

            // check system is viewable now...
            if ((app.enabled) && (app.internet) && (app.system == Global.settingsEnableExpert) ) {
                app.fw = state;
                app.date = 1;
                SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
                p.edit().putBoolean("FW-" + app.UID2, app.fw).apply();

                Logs.myLog(app.name + ": done!" ,3);

                adapter.notifyDataSetChanged(); // maybe need this



            }
        }

        if (Global.getFirewallState() == true) {

            Intent serviceIntent = new Intent(Global.getContext(), FOKServiceFW.class);
            serviceIntent.putExtra("command", Global.FIREWALL_RESTART); // can we pass app
            // serviceIntent.putExtra("restart", "app");
            Global.getContext().startService(serviceIntent);

        }

    }

    //
    // change the log level
    //
    protected void sortOptionPickerDropdown()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(myContext);

        //         final String
        String options[] = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            options = myContext.getResources().getStringArray(R.array.sortOptions);
        } else {
            options = myContext.getResources().getStringArray(R.array.sortOptionsNew);

        }

        builder.setTitle(myContext.getString(R.string.sort_option_set));

        int selected = Global.settingsSortOption;

        builder.setSingleChoiceItems(options, selected, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                dialog.cancel();
                Global.settingsSortOption = item;
                appRefresh();
                Logs.myLog("New sort option: " +Global.settingsSortOption,3);
                SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
                p.edit().putInt("settingsSortOption", Global.settingsSortOption).apply();
            }});

        builder.show();
    }


    // Sort all alphabetically
    void mySort0( ArrayList<AppInfo> appInfo2)
    {
        Collections.sort(appInfo2, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });
    }






    // Sort accept and deny into two alpha lists
    void mySort1( ArrayList<AppInfo> appInfo2)
    {

        ArrayList<AppInfo> appList1 = new ArrayList<AppInfo>();
        ArrayList<AppInfo> appList2 = new ArrayList<AppInfo>();

        for (int i =0 ; i < appInfo2.size(); i++ )
        {
            AppInfo app = appInfo2.get(i);
            if (app.fw)
            {
                appList1.add(app);
            } else {
                appList2.add(app);
            }
        }

        Collections.sort(appList1, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });

        Collections.sort(appList2, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });

        appInfo2.clear();
        appInfo2.addAll(appList2);
        appInfo2.addAll(appList1);


        /*
        Collections.sort(appInfo, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });
        */
    }

    // Sort accept and deny into two alpha lists
    void mySort2( ArrayList<AppInfo> appInfo2)
    {

        ArrayList<AppInfo> appList1 = new ArrayList<AppInfo>();
        ArrayList<AppInfo> appList2 = new ArrayList<AppInfo>();

        for (int i =0 ; i < appInfo2.size(); i++ )
        {
            AppInfo app = appInfo2.get(i);
            if (app.fw)
            {
                appList1.add(app);
            } else {
                appList2.add(app);
            }
        }

        Collections.sort(appList1, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });

        Collections.sort(appList2, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });

        appInfo2.clear();
        appInfo2.addAll(appList1);
        appInfo2.addAll(appList2);



        /*
        Collections.sort(appInfo, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });
        */
    }


    // Sort by date
    void mySort3 (ArrayList<AppInfo> appInfo2)
    {
        ArrayList<AppInfo> appList1 = new ArrayList<AppInfo>();
        ArrayList<AppInfo> appList2 = new ArrayList<AppInfo>();

        for (int i =0 ; i < appInfo2.size(); i++ )
        {
            AppInfo app = appInfo2.get(i);
            if (app.date > 10)
            {
                appList1.add(app);
            } else {
                appList2.add(app);
            }
        }

        Collections.sort(appList1, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return Long.compare(appInfoB.date,appInfoA.date);
            }
        });

        Collections.sort(appList2, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });

        appInfo2.clear();
        appInfo2.addAll(appList1);
        appInfo2.addAll(appList2);

    }

    // Sort accept and deny into two date lists
    void mySort4( ArrayList<AppInfo> appInfo2)
    {

        ArrayList<AppInfo> appList1 = new ArrayList<AppInfo>();
        ArrayList<AppInfo> appList2 = new ArrayList<AppInfo>();

        ArrayList<AppInfo> appList3 = new ArrayList<AppInfo>();
        ArrayList<AppInfo> appList4 = new ArrayList<AppInfo>();

        for (int i =0 ; i < appInfo2.size(); i++ )
        {
            AppInfo app = appInfo2.get(i);
            if (!(app.fw))
            {
                if (app.date > 10)
                {
                    appList1.add(app);
                } else {
                    appList2.add(app);
                }
            } else {
                if (app.date > 10)
                {
                    appList3.add(app);
                } else {
                    appList4.add(app);
                }
            }
        }

        Collections.sort(appList1, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return Long.compare(appInfoB.date,appInfoA.date);
            }
        });

        Collections.sort(appList2, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return Long.compare(appInfoB.date,appInfoA.date);
            }
        });

        Collections.sort(appList3, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });

        Collections.sort(appList4, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });

        appInfo2.clear();
        appInfo2.addAll(appList1);
        appInfo2.addAll(appList2);
        appInfo2.addAll(appList3);
        appInfo2.addAll(appList4);

        /*
        Collections.sort(appInfo, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });
        */
    }

    void mySort5( ArrayList<AppInfo> appInfo2)
    {

        ArrayList<AppInfo> appList1 = new ArrayList<AppInfo>();
        ArrayList<AppInfo> appList2 = new ArrayList<AppInfo>();

        ArrayList<AppInfo> appList3 = new ArrayList<AppInfo>();
        ArrayList<AppInfo> appList4 = new ArrayList<AppInfo>();

        for (int i =0 ; i < appInfo2.size(); i++ )
        {
            AppInfo app = appInfo2.get(i);
            if ((app.fw))
            {
                if (app.date > 10)
                {
                    appList1.add(app);
                } else {
                    appList2.add(app);
                }
            } else {
                if (app.date > 10)
                {
                    appList3.add(app);
                } else {
                    appList4.add(app);
                }
            }
        }

        Collections.sort(appList1, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return Long.compare(appInfoB.date,appInfoA.date);
            }
        });

        Collections.sort(appList3, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return Long.compare(appInfoB.date,appInfoA.date);
            }
        });

        Collections.sort(appList2, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });

        Collections.sort(appList4, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });

        appInfo2.clear();
        appInfo2.addAll(appList1);
        appInfo2.addAll(appList2);
        appInfo2.addAll(appList3);
        appInfo2.addAll(appList4);

        /*
        Collections.sort(appInfo, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });
        */
    }



}
