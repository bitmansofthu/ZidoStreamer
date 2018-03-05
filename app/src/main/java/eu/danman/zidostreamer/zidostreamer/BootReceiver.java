package eu.danman.zidostreamer.zidostreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        Log.d("boot broadcast", "onReceive");

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Boolean start_after_boot = settings.getBoolean("start_after_boot", false);

        if (start_after_boot){
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);

        }

    }
}
