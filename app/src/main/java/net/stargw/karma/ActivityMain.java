package net.stargw.karma;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import static net.stargw.karma.Global.APPLIST_DOING;


// public class ActivityMain extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener, ActivityMainListener  {
public class ActivityMain extends Activity implements ActivityMainListener{

    // private static long currentProgress = 0;

    private Dialog dialogAppRebuildProgress = null;

    private Dialog dialogNewApps = null;

    private BroadcastListener mReceiver;

    // Master list of apps - copied from a global list built by the service
    ArrayList<AppInfo> appListCopy;
    private AppInfoAdapterMain appListCopyAdaptor;
    private ListView appListCopyListview;

    Context myContext;

    private class BroadcastListener extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Logs.myLog("ActivityMain Received intent: " + intent.getAction(), 4);

            if (Global.REBUILD_APPS_DONE.equals(intent.getAction()))
            {
                Logs.myLog("Process Global.REBUILD_APPS_DONE", 2);
                // close dialog just in case we are stil showing it
                if (dialogAppRebuildProgress != null) {
                    Logs.myLog("Close dialog", 3);
                    dialogAppRebuildProgress.cancel();
                    dialogAppRebuildProgress.dismiss();
                    dialogAppRebuildProgress = null;
                }

                // Catch for a previous change not processed
                Logs.myLog("Global.rebuildGUIappList: " + String.valueOf(Global.rebuildGUIappList).toUpperCase(), 2);

                if (Global.rebuildGUIappList == true)
                {
                    updateAppListCopy();
                } else {
                    if (appListCopy == null)
                    {
                        // This can happen if service is running, but GUI has only
                        // just been created
                        Logs.myLog("appListCopy = null, so need to build", 2);
                        updateAppListCopy();
                    } else {
                        updateAppListCopyListView();
                    }
                }

            }

            if (Global.REBUILD_APPS_IN_PROGRESS.equals(intent.getAction())) {
                // Logs.myLog("App Loading intent received", 2);
                // Global.myLog("App Received intent to update saving", 2);
                // close dialog just in case we are stil showing it
                if (dialogAppRebuildProgress != null) {
                    ProgressBar progBar = (ProgressBar) dialogAppRebuildProgress.findViewById(R.id.progBar);
                    int x1 = (int) (Global.packageCurrent /(float)Global.packageMax*100);
                    progBar.setProgress(x1);
                }
            }

            if (Global.FIREWALL_STATE_CHANGE.equals(intent.getAction()) ) {
                Logs.myLog("Process Global.FIREWALL_STATE_CHANGE", 2);
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

        // Cancel any start notifications
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(100);

        setContentView(R.layout.activity_main2);

        myContext = this;

        Global.getSettings();
        Logs.getLoggingLevel(); // gets level + housekeeping

        // When app starts show user apps
        Global.settingsEnableExpert = false;

        Logs.myLog("ActivityMain - onCreate()", 2);

        createGUI();

    }

    void displayAppDialog()
    {
        Logs.myLog("ActivityMain - displayAppDialog()", 2);

        // remove any old dialogs
        if (dialogAppRebuildProgress != null) {
            Logs.myLog("Close dialog", 2);
            dialogAppRebuildProgress.cancel();
            dialogAppRebuildProgress.dismiss();
            dialogAppRebuildProgress = null;
        }

        // Display a loading dialog box until the app list is prepared by the service.
        dialogAppRebuildProgress = new Dialog(myContext);

        dialogAppRebuildProgress.setContentView(R.layout.dialog_progress);
        dialogAppRebuildProgress.setTitle(R.string.LoadingApps);

        TextView text = (TextView) dialogAppRebuildProgress.findViewById(R.id.infoMessage);
        text.setText(R.string.BuildingApps);

        text = (TextView) dialogAppRebuildProgress.findViewById(R.id.infoMessage2);
        text.setText(R.string.InfoLoadingApps);


        dialogAppRebuildProgress.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Logs.myLog("ActivityMain - onActivityResult()", 2);

        // catch the result for FW prep

        Logs.myLog("Request Code Received: " + requestCode, 2);
        if (requestCode == 666)
        {
            Logs.myLog("Result Code Received: " + resultCode, 2);
            if (resultCode == RESULT_OK) {
                Intent serviceIntent = new Intent(myContext, ServiceFW.class);
                serviceIntent.putExtra("command", Global.FIREWALL_START);
                myContext.startService(serviceIntent);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }


    @Override
    protected void onResume() {
        super.onResume();

        Logs.myLog("ActivityMain - onResume()", 2);

        updateAppListCopyListView();
        updateFirewallStateDisplay(false);

        Logs.checkLogSize();

        // register receiver
        mReceiver = new BroadcastListener();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Global.FIREWALL_STATE_CHANGE);

        mIntentFilter.addAction(Global.REBUILD_APPS_DONE);
        mIntentFilter.addAction(Global.REBUILD_APPS_IN_PROGRESS);

        mIntentFilter.addAction(Global.TOGGLEAPP_REFRESH);

        registerReceiver(mReceiver, mIntentFilter);

        //
        // Applist building is kicked off by Global application onCreate
        //
        if (Global.appListState == APPLIST_DOING)
        {
            displayAppDialog();
        } else {
            // Check if we have nothing. Do not display a blank screen
            if (appListCopy == null)
            {
                if ( (Global.appListFW == null) || (Global.appListFW.isEmpty()) )
                {
                    displayAppDialog();
                } else {
                    // we don't have a copy, but an appListFW exists
                    updateAppListCopy();
                }
            }
            // A full rebuild in the background
            Logs.myLog("ActivityMain -> onResume() -> Global.getAppListBackground()", 2);
            Global.getAppListBackground();
        }

    }



    @Override
    protected void onStop() {
        super.onStop();  // Always call the superclass method first
        Logs.myLog("ActivityMain - onStop()", 2);

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

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

        item = m.findItem(R.id.action_autofw);
        int autoFW = p.getInt("settingsAutoFW",0);
        if (autoFW == 1) {
            item.setChecked(true);
        } else {
            item.setChecked(false);
        }

        item = m.findItem(R.id.action_subnet);

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
                        updateAppListCopy();
                        return true;
                    case R.id.action_apps_toggle:
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                        {
                            // nothing
                        } else {
                            toggleAll(myContext);
                            updateAppListCopyListView();
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
                            text.setText(R.string.dialog_enable_boot_info);
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
                        text.setText(R.string.dialog_enable_always_on_info);
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
                            // If first time user interaction required
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
                    case R.id.action_autofw:
                        if (autoFW == 1) {
                            p.edit().putInt("settingsAutoFW", 0).commit();
                        } else {
                            p.edit().putInt("settingsAutoFW", 1).commit();
                            Global.infoMessage(myContext, getString(R.string.dialog_warning), getString(R.string.notify_autofw));

                        }
                        return true;
                    case R.id.action_refresh:
                        displayAppDialog();
                        Global.rebuildGUIappList = true;
                        Logs.myLog("ActivityMain -> Menu Force Refresh -> Global.getAppListBackground()", 2);
                        Global.getAppListBackground();
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
        Logs.myLog("ActivityMain - onPause()", 2);

        if(mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onPause();
    }

    @Override
    public void onBackPressed()
    {
        Logs.myLog("ActivityMain - onBackPressed()", 2);

        EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);

        if (myFilter.getVisibility() == View.VISIBLE) {
            myFilter.setVisibility(View.GONE);
            myFilter.setText("");
            appListCopyAdaptor.clearExpanded();

        } else {
            super.onBackPressed();
        }
    }

    public void updateAppListCopyListView()
    {
        Logs.myLog("ActivityMain - updateAppListCopyListView()", 2);

        if (appListCopy == null)
        {
            Logs.myLog("updateAppListCopyListView() - appListCopy = null!", 2);
        } else {
            Logs.myLog("updateAppListCopyListView() - appListCopy = " + appListCopy.size(), 2);
        }

        if (appListCopyAdaptor != null) {
            appListCopyAdaptor.notifyDataSetChanged();
        } else {
            Logs.myLog("updateAppListCopyListView() - appListCopyAdaptor = null!", 2);
        }
    }


    // rebuild the copy and refresh adaptors and listviews
    public void updateAppListCopy()
    {
        Logs.myLog("ActivityMain - updateAppListCopy()", 2);

        if ((Global.appListFW == null) || (Global.appListFW.size() == 0))
        {
            Logs.myLog("appListFW empty. Nothing to copy to GUI!", 2);

            // Reset that the GUI has processed
            Global.rebuildGUIappList = false;
            return;
        }

        // rebuild the app list
        Iterator<Integer> it = Global.appListFW.keySet().iterator();
        appListCopy = new ArrayList<AppInfo>();

        while (it.hasNext())
        {
            int key = it.next();
            AppInfo thisApp = Global.appListFW.get(key);
            if (thisApp.system == Global.settingsEnableExpert) {
                if (thisApp.flush == false) { // deleted apps
                    appListCopy.add(thisApp);
                    Logs.myLog("Copy app to GUI: " + thisApp.name, 4);

                }
            }
        }

        if (Global.settingsEnableExpert) {
            Logs.myLog("updateAppListCopy() - Number of apps = " + appListCopy.size() + " (system)", 2);
        } else {
            Logs.myLog("updateAppListCopy() - Number of apps = " + appListCopy.size() + " (user)", 2);

        }

        switch (Global.settingsSortOption) {
            case 0:
                mySort0(appListCopy);
                break;
            case 1:
                mySort1(appListCopy);
                break;
            case 2:
                mySort2(appListCopy);
                break;
            default:
                mySort0(appListCopy);
                break;
        }

        appListCopyAdaptor = new AppInfoAdapterMain(myContext, appListCopy);

        appListCopyListview = (ListView) findViewById(R.id.listViewApps);
        appListCopyListview.setAdapter(appListCopyAdaptor);

        // Reset that the GUI has processed
        Global.rebuildGUIappList = false;

        // After building a copy check for new apps if any
        showNewApps();

    }



    private void createGUI() {

        Logs.myLog("ActivityMain - createGUI()", 2);

        final Context c = myContext;

        TextView text1 = (TextView) findViewById(R.id.activity_main_title);
        text1.setText(R.string.activity_main_menu_title);

        ImageView btn = (ImageView) findViewById(R.id.activity_main_menu_icon);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showOptionsMenu(v);
            }
        });


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
                            appListCopyAdaptor.getFilter().filter(s.toString());
                        }
                    });
/*
                    myFilter.setOnKeyListener((z, keyCode, event) -> {
                        if(event.getAction() == KeyEvent.ACTION_DOWN) {
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_BACK:
                                    appListCopyAdaptor.clearExpanded();
                                    break;
                            }
                        }
                        return false;
                    });
*/
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(myFilter.getWindowToken(), 0);

                    myFilter.setVisibility(View.GONE);
                    myFilter.setText("");

                    appListCopyAdaptor.clearExpanded();

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

        String url = "https://www.stargw.net/android/karma/help.html?ver=" + verName;
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
        Logs.myLog("ActivityMain - enterSubnet()", 2);

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
                        Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
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
                    Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
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
        AppInfo thisApp = appListCopyAdaptor.getItem(position);
        toggleFirewallApp(thisApp);
        updateAppListCopyListView();
    }


    private void toggleFirewallApp(AppInfo thisApp)
    {
        Logs.myLog("ActivityMain - toggleFirewallApp()", 2);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            // old version - apps cannot be changed
            return;
        }

        Logs.myLog("toggleFirewallApp: " + thisApp.name, 2);

        // If we want to use 40 for new blocked then 40 becomes 20 here...
        if (thisApp.fw == 30) {
            thisApp.fw = 10;
        } else {
            thisApp.fw = 30;
        }

        // change the global - checking app is still there
        if (Global.appListFW.containsKey(thisApp.UID2)) {
            Global.appListFW.get(thisApp.UID2).fw = thisApp.fw;
        }

        // Save the change
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
        p.edit().putInt("FW-" + thisApp.UID2, thisApp.fw).apply();

        if (Global.getFirewallState() == true) {

            Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
            serviceIntent.putExtra("command", Global.FIREWALL_RESTART); // can we pass app
            Global.getContext().startService(serviceIntent);
         } else {
            Global.updateMyWidgets();

        }

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
                    Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
                    serviceIntent.putExtra("command", Global.FIREWALL_STOP);
                    Global.getContext().startService(serviceIntent);
                } else {
                    ImageView toggle = (ImageView) findViewById(R.id.activity_main_firewall_icon);
                    toggle.setColorFilter(Color.YELLOW);

                    // If first time, user interaction required
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

        Logs.myLog("ActivityMain - toggleAll()", 2);

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
                toggleApps(10);
                info.cancel();
                updateAppListCopyListView();
            }

        });

        Button blockButton = (Button) info.findViewById(R.id.blockButton);

        blockButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // notificationCancel(context);
                // loop through all apps...
                toggleApps(30);
                info.cancel();
                updateAppListCopyListView();
            }

        });

        info.show();
        // Logs.myLog(header + ":" + message,3);
    }

    private void toggleApps(int state) {
        Logs.myLog("ActivityMain - toggleApps(): " + state, 2);

        Iterator<Integer> it = Global.appListFW.keySet().iterator();

        while (it.hasNext()) {
            int key = it.next();
            AppInfo app = Global.appListFW.get(key);

            // check system is viewable now...
            if ( (app.internet) && (app.system == Global.settingsEnableExpert) ) {
                app.fw = state;
                SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
                p.edit().putInt("FW-" + app.UID2, app.fw).apply();

                Logs.myLog(app.name + ": done!" ,3);

                appListCopyAdaptor.notifyDataSetChanged(); // maybe need this


            }
        }

        if (Global.getFirewallState() == true) {

            Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
            serviceIntent.putExtra("command", Global.FIREWALL_RESTART); // can we pass app
            Global.getContext().startService(serviceIntent);

        }

    }

    //
    // change the log level
    //
    protected void sortOptionPickerDropdown()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(myContext);

        // final String
        String options[] = myContext.getResources().getStringArray(R.array.sortOptionsNew);


        builder.setTitle(myContext.getString(R.string.sort_option_set));

        int selected = Global.settingsSortOption;

        builder.setSingleChoiceItems(options, selected, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                dialog.cancel();
                Global.settingsSortOption = item;
                updateAppListCopy();
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
            if (app.fw >= 30)
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

    }

    // Sort accept and deny into two alpha lists
    void mySort2( ArrayList<AppInfo> appInfo2)
    {

        ArrayList<AppInfo> appList1 = new ArrayList<AppInfo>();
        ArrayList<AppInfo> appList2 = new ArrayList<AppInfo>();

        for (int i =0 ; i < appInfo2.size(); i++ )
        {
            AppInfo app = appInfo2.get(i);
            if (app.fw >= 30)
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



    }

    private void showNewApps()
    {
        Logs.myLog("ActivityMain - showNewApps()", 2);

        if ((Global.appListFW == null) || (Global.appListFW.size() == 0) )
        {
            return;
        }

        if (dialogNewApps != null)
        {
            // only open one dialog
            return;
        }

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());


        final ArrayList newApps = new ArrayList<AppInfo>();
        AppInfoAdapterNew newAppsAdaptor;

        Iterator<Integer> it = Global.appListFW.keySet().iterator();
        while (it.hasNext())
        {
            int key = it.next();
            AppInfo thisApp = Global.appListFW.get(key);
            if (thisApp.fw == 20) {
                newApps.add(thisApp);
            }
            if (thisApp.fw == 25) { // probably not see this
                thisApp.fw = 20;
                p.edit().putInt("FW-" + thisApp.UID2, thisApp.fw).apply();
                newApps.add(thisApp);
            }
            if (thisApp.fw == 40) {
                newApps.add(thisApp);
            }
            if (thisApp.fw == 45) { // probably not see this
                thisApp.fw = 40;
                p.edit().putInt("FW-" + thisApp.UID2, thisApp.fw).apply();
                newApps.add(thisApp);
            }
        }

        if ( (newApps == null) || (newApps.size() == 0) )
        {
            // No new apps
            Logs.myLog("No new apps!", 2);
            return;
        }

        Logs.myLog("New apps = " + newApps.size(), 2);


        mySort0(newApps);

        // Cancel the notifications
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(200);

        dialogNewApps = new Dialog(myContext);

        newAppsAdaptor = new AppInfoAdapterNew(myContext, newApps);

        dialogNewApps.setContentView(R.layout.dialog_records);

        Window window = dialogNewApps.getWindow();
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // Show dialog in upper part of screen to stop it jumping when keyboard displayed
        WindowManager.LayoutParams wmlp = window.getAttributes();
        // wmlp.gravity = Gravity.TOP | Gravity.LEFT;
        // wmlp.x = 100;   //x position
        wmlp.y = 160;   //y position

        dialogNewApps.setTitle("New Apps to Firewall");

        dialogNewApps.getWindow().setGravity(Gravity.TOP);
        dialogNewApps.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // pretty much full screen
        int width = (int)(getResources().getDisplayMetrics().widthPixels*1);
        int height = (int)(getResources().getDisplayMetrics().heightPixels*0.93);

        dialogNewApps.getWindow().setLayout(width, height);

        ListView newAppsListView = (ListView) dialogNewApps.findViewById(R.id.recordListView);

        newAppsListView.setAdapter(newAppsAdaptor);

        newAppsListView.setClickable(true);



        dialogNewApps.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                newAppsAdaptor.clearExpanded();
                showNewAppsDismiss(p);
            }

        });

        Button dialogButton = (Button) dialogNewApps.findViewById(R.id.buttonOK);

        dialogButton.setOnClickListener(new View.OnClickListener() {
                // @Override
                public void onClick(View v) {
                    newAppsAdaptor.clearExpanded();
                    showNewAppsDismiss(p);
                }
            });

        dialogNewApps.show();

    }

    void showNewAppsDismiss (SharedPreferences p)
    {
        Logs.myLog("ActivityMain - showNewAppsDismiss()", 2);

        // reset app so not new, and not shown again
        Iterator<Integer>  it = Global.appListFW.keySet().iterator();

        while (it.hasNext())
        {
            int key = it.next();

            AppInfo app = Global.appListFW.get(key);
            if (app != null) {
                if ( (app.fw == 20) || (app.fw == 25) ) {
                    // reset so not new
                    app.fw = 10;
                    p.edit().putInt("FW-" + app.UID2, app.fw).apply();
                }
                if ( (app.fw == 40) || (app.fw == 45) ){
                    // reset so not new
                    app.fw = 30;
                    p.edit().putInt("FW-" + app.UID2, app.fw).apply();
                }
            }
        }



        dialogNewApps.dismiss();
        dialogNewApps = null;
    }

}

