package net.stargw.karma;

import android.content.Context;

import java.util.ArrayList;

public class AppInfoAdapterWidget extends AppInfoAdapter

{

    public AppInfoAdapterWidget(Context context, ArrayList<AppInfo> apps) {
        super(context, apps);

        listener = (Widget2Configure) context;
    }
}
