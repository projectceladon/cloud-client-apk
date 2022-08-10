package com.intel.gamepad.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.intel.gamepad.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SplashActivity extends BaseActivity {
    public static boolean isDeviceRooted() {
        return checkRootMethodOne() || checkRootMethodTwo() || checkRootMethodThree();
    }

    private static boolean checkRootMethodOne() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private static boolean checkRootMethodTwo() {
        String[] paths = {"/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkRootMethodThree() {
        Process process = null;
        String line;
        BufferedReader in = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            line=in.readLine();
        } catch (Throwable t) {
            return false;
        } finally {
            if(in !=null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (process != null) process.destroy();
        }
        return line != null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        windowFullScreen();
        super.onCreate(savedInstanceState);
        if (isDeviceRooted()) {
            Toast.makeText(this, "This is rooted device!", Toast.LENGTH_LONG).show();
        }
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, GameDetailActivity.class);
        startActivity(intent);
    }

    private void windowFullScreen() {
        Window window = getWindow();
        window.requestFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        View decorView = window.getDecorView();
        if(decorView!=null){
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
}
