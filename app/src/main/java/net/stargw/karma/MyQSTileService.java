package net.stargw.karma;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MyQSTileService extends TileService {

    // Called when the user adds your tile.
    @Override
    public void onTileAdded() {
        super.onTileAdded();

    }

    // Called when your app can update your tile.
    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();

        if (Global.getFirewallState() == false) {
            tile.setState(Tile.STATE_INACTIVE);
            Icon icon = Icon.createWithResource(Global.getContext(),R.drawable.vpn_connected2);
            tile.setIcon(icon);
            tile.setLabel("FW OFF");
        } else {
            Icon icon = Icon.createWithResource(Global.getContext(),R.drawable.vpn_connected2);
            tile.setIcon(icon);
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel("FW ON");
        }
        tile.updateTile();
    }

    // Called when your app can no longer update your tile.
    @Override
    public void onStopListening() {
        super.onStopListening();

        Tile tile = getQsTile();
        tile.setState(Tile.STATE_INACTIVE);
        Icon icon = Icon.createWithResource(Global.getContext(),R.drawable.vpn_connected2);
        tile.setIcon(icon);
        tile.setLabel("FW OFF");
        tile.updateTile();
    }

    // Called when the user taps on your tile in an active or inactive state.
    @Override
    public void onClick() {
        super.onClick();

        Tile tile = getQsTile();

        if (Global.getFirewallState() == true) {
            Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
            serviceIntent.putExtra("command", Global.FIREWALL_STOP);
            Global.getContext().startService(serviceIntent);
            // Log.w("FWWidget1", "FWWidget1 Action Turn off");
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel("FW OFF");
            Icon icon = Icon.createWithResource(Global.getContext(),R.drawable.vpn_connected2);
            tile.setIcon(icon);
        } else {
            Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
            serviceIntent.putExtra("command", Global.FIREWALL_QS);

            ContextCompat.startForegroundService(Global.getContext(), serviceIntent);
            Icon icon = Icon.createWithResource(Global.getContext(),R.drawable.vpn_connected2);
            tile.setIcon(icon);
            tile.setLabel("FW ON");
            tile.setState(Tile.STATE_ACTIVE);
        }
        tile.updateTile();

    }

    // Called when the user removes your tile.
    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }
}

