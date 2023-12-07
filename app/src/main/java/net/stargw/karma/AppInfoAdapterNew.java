package net.stargw.karma;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;


public class AppInfoAdapterNew extends ArrayAdapter<AppInfo> implements Filterable {

    ActivityMainListener listener;

    private ArrayList<AppInfo> appsSorted; // for filtering
    private ArrayList<AppInfo> appsOriginal; // for filtering

    Context mContext;

    // PackageManager pManager;

    public AppInfoAdapterNew(Context context, ArrayList<AppInfo> apps) {
        super(context, 0, apps);

        // listener = (ActivityMain) context;

        mContext = context;

        // pManager = mContext.getPackageManager();
    }


    // Take a copy of the list when it changes for filtering later
    public void updateFull()
    {
        setNotifyOnChange(false);
        this.appsOriginal = new ArrayList<AppInfo>();

        for(int i = 0, l = getCount(); i < l; i++) {
            AppInfo app = getItem(i);
            this.appsOriginal.add(app);
        }

        super.notifyDataSetChanged();
    }

    /*
    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
    */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        AppInfo apps = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_main_row2, parent, false);
        }
        // Lookup view for data population
        TextView text1 = (TextView) convertView.findViewById(R.id.activity_main_row_app_name);

        if (apps.enabled == false)
        {
            text1.setPaintFlags(text1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            text1.setPaintFlags( text1.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
        }

        TextView text2 = (TextView) convertView.findViewById(R.id.activity_main_row_app_traffic);

        // We don't use text2 line any more
        text2.setText("");
        text2.setTypeface(null, Typeface.ITALIC);
        text2.setTextColor(Color.WHITE);

        ImageView icon = (ImageView) convertView.findViewById(R.id.activity_main_row_app_icon);
        // ToggleButton tog = (ToggleButton) convertView.findViewById(R.id.rowToggle);
        ImageView tog = (ImageView) convertView.findViewById(R.id.activity_main_row_fwstate_icon);

        // Populate the data into the template view using the data object
        text1.setText(apps.name);
        // text1.setText(apps.name + " (" + apps.UID2 +")");
        // text2.setText(apps.packageName);

        // Load icons once on the fly didn't really work
        // Global.getIcon(pManager,apps);
        if (apps.icon != null)
        {
            icon.setImageDrawable(apps.icon);
        }

        // tog.setChecked(apps.kill);
        tog.setVisibility(View.VISIBLE);

        if (apps.fw >= 30)
        {
            tog.setImageDrawable(getContext().getResources().getDrawable(R.drawable.fw_app_off));
            // tog.setImageDrawable(getContext().getResources().getDrawable(R.drawable.no3));
        } else {
            tog.setImageDrawable(getContext().getResources().getDrawable(R.drawable.fw_app_on));
            // tog.setImageDrawable(getContext().getResources().getDrawable(R.drawable.yes3));
        }

        final int pos = position;


        tog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSelectedItem(pos);
            }
        });

        text1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSelectedItem(pos);
            }
        });


        final View vp = convertView;
        final LinearLayout expand = (LinearLayout) convertView.findViewById(R.id.activity_main_row_app_expand);

        if (apps.expandView == true)
        {
            expand.setVisibility(View.VISIBLE);
            expandApp(vp, pos);
        } else {
            expand.setVisibility(View.GONE);
        }

        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppInfo app = getItem(pos);
                if (app.expandView == true)
                {
                    expand.setVisibility(View.GONE);
                    app.expandView = false;
                } else {
                    expand.setVisibility(View.VISIBLE);
                    app.expandView = true;
                }
                expandApp(vp, pos);
            }
        });


        // Return the completed view to render on screen
        return convertView;

    }

    private void expandApp(View v, int position) {

        AppInfo app = getItem(position);

        /*
        long up = TrafficStats.getUidTxBytes(app.UID2);
        long down = TrafficStats.getUidRxBytes(app.UID2);

        String appUp = showTraffic(up, true);
        String appDown = showTraffic(down, false);
        */

        LinearLayout expand = (LinearLayout) v.findViewById(R.id.activity_main_row_app_expand_text);
        expand.removeAllViews();

        if (app.appInfoExtra.size() == 1)
        {
            TextView text2 = new TextView(Global.getContext());
            text2.setTextColor(Color.WHITE);
            text2.setTypeface(null, Typeface.ITALIC);
            text2.setEllipsize(TextUtils.TruncateAt.END);
            // text1.setTextSize();
            text2.setSingleLine(true);
            text2.setMaxLines(1);

            text2.setText("(" + app.appInfoExtra.get(0).packageFQDN + ")");

            if (app.appInfoExtra.get(0).packageEnabled == false)
            {
                text2.setPaintFlags(text2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                text2.setPaintFlags(text2.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
            }

            expand.addView(text2);

            TextView text4 = (TextView) v.findViewById(R.id.activity_main_row_app_uid);
            text4.setText("UID: " + app.UID2);
        } else {
            for(int i = 0; i < app.appInfoExtra.size(); i++) {
                // TextView text1 = (TextView) v.findViewById(R.id.activity_main_row_app_packname);
                // TextView text1 = new TextView(Global.getContext(),null, R.layout.activity_main_row_expand_text);
                TextView text1 = new TextView(Global.getContext());
                text1.setTextColor(Color.WHITE);
                text1.setEllipsize(TextUtils.TruncateAt.END);
                // text1.setTextSize();
                text1.setSingleLine(true);
                text1.setMaxLines(1);

                text1.setText(app.appInfoExtra.get(i).packageName);
                expand.addView(text1);

                TextView text2 = new TextView(Global.getContext());
                text2.setTextColor(Color.WHITE);
                text2.setTypeface(null, Typeface.ITALIC);
                text2.setEllipsize(TextUtils.TruncateAt.END);
                // text1.setTextSize();
                text2.setSingleLine(true);
                text2.setMaxLines(1);

                text2.setText("(" + app.appInfoExtra.get(i).packageFQDN + ")");

                if (app.appInfoExtra.get(i).packageEnabled == false)
                {
                    // Logs.myLog("CONTAINS: " + packageName, 2 );
                    text1.setPaintFlags(text1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    text2.setPaintFlags(text2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    text1.setPaintFlags(text1.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
                    text2.setPaintFlags(text2.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
                }

                expand.addView(text2);
            }
            TextView text4 = (TextView) v.findViewById(R.id.activity_main_row_app_uid);
            text4.setText("");
        }



    }

    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                appsSorted = (ArrayList<AppInfo>)results.values;
                notifyDataSetChanged();
                clear();
                for(int i = 0, l = appsSorted.size(); i < l; i++)
                    add(appsSorted.get(i));
                notifyDataSetInvalidated();

                notifyDataSetChanged();

            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();

                constraint = constraint.toString().toLowerCase();
                FilterResults result = new FilterResults();
                if(constraint != null && constraint.toString().length() > 0)
                {
                    ArrayList<AppInfo> filteredItems = new ArrayList<AppInfo>();

                    for(int i = 0, l = appsOriginal.size(); i < l; i++)
                    {
                        AppInfo app = appsOriginal.get(i);
                        // Logs.myLog("Filter match: " + constraint + " v " + app.packageName , 3);
                        if( (app.name.toString().toLowerCase().contains(constraint) ) ) {
                        // ) || (app.packageName.toString().toLowerCase().contains(constraint)) ) {
                            filteredItems.add(app);
                        }
                        // Loop over package names...
                        for(int i2 = 0; i2 < app.appInfoExtra.size(); i2++) {
                            if( (app.appInfoExtra.get(i2).packageName.toLowerCase().contains(constraint) ) ) {
                                boolean newApp = true;
                                for(int i4 = 0; i4 < filteredItems.size() ; i4++)
                                {
                                    if (filteredItems.get(i4).UID2 == app.UID2)
                                    {
                                        newApp = false;
                                        break;
                                    }
                                }
                                if (newApp == true) {
                                    filteredItems.add(app); // miltiple??
                                }
                            }
                        }
                    }
                    result.count = filteredItems.size();
                    result.values = filteredItems;
                }
                else
                {
                    synchronized(this)
                    {
                        result.values = appsOriginal;
                        result.count = appsOriginal.size();
                    }
                }
                return result;
            }

        };

        return filter;
    }

    public void changeSelectedItem(int position) {

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
                // serviceIntent.putExtra("restart", "app");
                Global.getContext().startService(serviceIntent);
                // thisApp.bytesFWOut = TrafficStats.getUidTxBytes(thisApp.UID2);
                // thisApp.bytesFWIn = TrafficStats.getUidRxBytes(thisApp.UID2);
            }

        }
    }

}
