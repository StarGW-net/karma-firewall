package net.stargw.karma;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class ActivityLogs extends Activity {

    LogAdapter adapter;
    ListView lv;

    private BroadcastListener mReceiver;

    private boolean viewBlocked = true;
    boolean debugLog = false;
    Context myContext;


    private class BroadcastListener extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            // Logs.myLog("App Received intent", 2);
            if (Global.REBUILD_APPS_DONE.equals(intent.getAction())) {
                Logs.myLog("ActivityLogs Received REBUILD_APPS_DONE", 2);
                if (adapter != null) {
                    refreshLogs();
                }
            }
        }
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_logs);

        myContext = this;

        Global.getSettings();
        Logs.getLoggingLevel();  // gets level + housekeeping

        Logs.myLog("ActivityLogs Activity started" , 2);

        lv = (ListView) findViewById(R.id.activity_logs_listview);
        refreshLogs();

        ImageView btn = (ImageView) findViewById(R.id.activity_logs_menu_icon);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showOptionsMenu(v);
            }
        });



        EditText myFilter = (EditText) findViewById(R.id.activity_logs_filter_text);
        myFilter.setVisibility(View.GONE);

        ImageView mySearch = (ImageView) findViewById(R.id.activity_logs_search_icon);

        mySearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //v.getId() will give you the image id

                EditText myFilter = (EditText) findViewById(R.id.activity_logs_filter_text);

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
                            adapter.getFilter().filter(s.toString());
                        }
                    });
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(myFilter.getWindowToken(), 0);

                    myFilter.setVisibility(View.GONE);
                    myFilter.setText("");
                }

            }
        });


    }



    @Override
    public void onBackPressed()
    {
        EditText myFilter = (EditText) findViewById(R.id.activity_logs_filter_text);

        // override back if we are showing the search filter. Dismiss this first
        if (myFilter.getVisibility() == View.VISIBLE) {
            myFilter.setVisibility(View.GONE);
            myFilter.setText("");
        } else {
            super.onBackPressed();
        }
    }

    void refreshLogs()
    {
        // adapter.clear();

        if (Global.settingsLoggingLevel < 5)
        {
            ArrayList<String> logBuffer = Logs.getLogBufferList();

            /*
            for (int i = logBuffer.size() -1; i >= 0; i--)
            {
                String logLine = logBuffer.get(i);
                if (Build.VERSION.SDK_INT > 28)
                {
                    if (!viewBlocked && !(logLine.contains(getString(R.string.allowed)))) {
                        logBuffer.remove(i);
                    }
                    if (viewBlocked && !(logLine.contains(getString(R.string.blocked)))) {
                        logBuffer.remove(i);
                    }
                }
            }
            */

            adapter = new LogAdapter(this, logBuffer);

            // adapter = new LogAdapter(myContext, Logs.getLogBufferList());
            adapter.updateFull();
            // adapter.getFilter().filter("deny: ");
        } else {
            // Show adb logcat...
            try {
                Process process = Runtime.getRuntime().exec("logcat -v time -d");
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                // StringBuilder log=new StringBuilder();
                String line = "";
                ArrayList<String> theLog = new ArrayList<String>();
                while ((line = bufferedReader.readLine()) != null) {
                    theLog.add(line);
                }
                adapter = new LogAdapter(myContext, theLog);
                adapter.updateFull();

            } catch (IOException e) {
                Logs.myLog("Error getting ADB log" + e.toString(),2);
                Global.settingsLoggingLevel = 2;
                refreshLogs();
            }
        }


        // adapter.notifyDataSetChanged();
        lv.setAdapter(adapter);
        lv.setSelection(lv.getAdapter().getCount() - 1);


    }

    public void showOptionsMenu(View v) {
        PopupMenu menuLogs = new PopupMenu(this, v);
        menuLogs.inflate(R.menu.menu_logs);

        final Menu m = menuLogs.getMenu();
        MenuItem item;

        item = m.findItem(R.id.action_logcat);
        if (Global.settingsEnableLogcat == true)
        {
            item.setChecked(true);
        } else {
            item.setChecked(false);
        }

        switch (Global.settingsLoggingLevel) {
            case 0:
                item = m.findItem(R.id.action_log_0);
                item.setChecked(true);
                break;
            case 1:
                item = m.findItem(R.id.action_log_1);
                item.setChecked(true);
                break;
            case 2:
                item = m.findItem(R.id.action_log_2);
                item.setChecked(true);
                break;
            case 3:
                item = m.findItem(R.id.action_log_3);
                item.setChecked(true);
                break;
            default:
                item = m.findItem(R.id.action_log_4);
                item.setChecked(true);
                break;
        }

        invalidateOptionsMenu();

        menuLogs.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            // Handles all, including sub-menu
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.action_refresh2:
                        refreshLogs();
                        return true;
                    case R.id.action_share:
                        // Logs.shareLog();
                        Logs.shareLogcat(); // Shares Karma log as well
                        return true;
                    case R.id.action_logcat:
                        enableLogcat();
                        return true;
                    case R.id.action_clear:
                        Logs.clearLog();
                        refreshLogs();
                        return true;
                    case R.id.action_log_0:
                        Global.settingsLoggingLevel = 0;
                        Global.saveSetings();
                        logWarn();
                        refreshLogs();
                        return true;
                    case R.id.action_log_1:
                        Global.settingsLoggingLevel = 1;
                        Global.saveSetings();
                        logWarn();
                        refreshLogs();
                        return true;
                    case R.id.action_log_2:
                        Global.settingsLoggingLevel = 2;
                        Global.saveSetings();
                        logWarn();
                        refreshLogs();
                        return true;
                    case R.id.action_log_3:
                        Global.settingsLoggingLevel = 3;
                        Global.saveSetings();
                        logWarn();
                        refreshLogs();
                        return true;
                    default:
                        return false;
                }
            }

        });
        menuLogs.show();
    }

    private void enableLogcat()
    {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

        if (Global.settingsEnableLogcat == true) {
            Global.settingsEnableLogcat = false;
        } else {
            Global.settingsEnableLogcat = true;
            // Global.infoMessage(myContext, getString(R.string.dialog_warning), getString(R.string.notify_autofw));
        }
        p.edit().putBoolean("settingsEnableLogcat", Global.settingsEnableLogcat).commit();

    }

    private void logWarn()
    {

        // String s = myContext.getString(R.string.activity_logs_menu_log_level);
        Global.infoMessage(myContext,
                myContext.getString(R.string.activity_logs_menu_log_level),
                        myContext.getString(R.string.log_level_message1));
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Runs TWICE on startup. WHY? WHY? WHY?
        Logs.myLog("ActivityLogs Activity Resumed", 2);

        mReceiver = new BroadcastListener();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Global.REBUILD_APPS_DONE);
        registerReceiver(mReceiver, mIntentFilter);

        viewBlocked = true;
        debugLog = false;

        refreshLogs();

    }
}
