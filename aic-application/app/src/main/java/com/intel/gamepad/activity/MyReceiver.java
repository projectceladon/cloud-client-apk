package com.intel.gamepad.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String action = intent.getAction();
        if(action.equals("com.intel.gamepad.sendfiletoaic")) {
            String uri = intent.getStringExtra("uri");
            Log.e("MyReceiver", "To aic uri = " + uri);
            sendFiletoAIC(uri);
        } else if (action.equals("com.intel.gamepad.sendfiletoapp")) {
            String uri = intent.getStringExtra("uri");
            Log.e("MyReceiver", "To app uri = " + uri);
            sendFiletoApp(uri);
        }
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    private void sendFiletoAIC(String uri){

    }

    private void sendFiletoApp(String uri){

    }
}