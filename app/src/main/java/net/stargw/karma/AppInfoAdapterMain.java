package net.stargw.karma;

import android.content.Context;

import java.util.ArrayList;

public class AppInfoAdapterMain extends AppInfoAdapter

{

    public AppInfoAdapterMain(Context context, ArrayList<AppInfo> apps) {
        super(context, apps);

        listener = (ActivityMain) context;
    }
}
