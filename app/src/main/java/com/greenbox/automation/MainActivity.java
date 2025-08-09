package com.greenbox.automation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.permission.FloatWindowManager;
import com.greenbox.automation.util.MemoryManager;
import com.greenbox.automation.util.Permissons4App;


public class MainActivity extends Activity {
    private final String TAG = "ATXMainActivity";
    private static final int REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 1001;

    private TextView tvInStorage;
    private TextView textViewIP;

    private FloatView floatView;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnFinish = findViewById(R.id.btn_finish);
        btnFinish.setOnClickListener(view -> {
            stopService(new Intent(MainActivity.this, Service.class));
            finish();
        });

        Button btnIdentify = findViewById(R.id.btn_identify);
        btnIdentify.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, IdentifyActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("theme", "RED");
            intent.putExtras(bundle);
            startActivity(intent);
        });

        findViewById(R.id.accessibility).setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        findViewById(R.id.development_settings).setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)));

        Intent intent = getIntent();
        boolean isHide = intent.getBooleanExtra("hide", false);
        if (isHide) {
            Log.i(TAG, "launch args hide:true, move to background");
            moveTaskToBack(true);
        }
        textViewIP = findViewById(R.id.ip_address);
        tvInStorage = findViewById(R.id.in_storage);

        String[] permissions = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS};
        Permissons4App.initPermissions(this, permissions);

        TextView viewVersion = (TextView) findViewById(R.id.app_version);
        viewVersion.setText("version: " + getAppVersionName());
    }

    private String getAppVersionName() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permissons4App.handleRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void requestIgnoreBatteryOptimizations(View view){
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
            // Check if the user granted the request or not
            Log.d(TAG, "requestIgnoreBatteryOptimizations resultCode: " + resultCode);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (isIgnoringBatteryOptimizations()) {
                    // User granted the request
                    showToast("requestIgnoreBatteryOptimizations success");
                } else {
                    showToast("requestIgnoreBatteryOptimizations failure");
                    // User didn't grant the request
                }
            }
        }
    }

    @RequiresApi(api=Build.VERSION_CODES.M)
    private boolean isIgnoringBatteryOptimizations() {
        boolean isIgnoring = false;
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            isIgnoring = powerManager.isIgnoringBatteryOptimizations(getPackageName());
        }
        return isIgnoring;
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void showFloatWindow(View view) {
        boolean floatEnabled = FloatWindowManager.getInstance().checkFloatPermission(MainActivity.this);
        if (!floatEnabled) {
            Log.i(TAG, "float permission not checked");
            return;
        }
        if (floatView == null) {
            floatView = new FloatView(MainActivity.this);
        }
        floatView.show();
    }

    public void dismissFloatWindow(View view) {
        if (floatView != null) {
            Log.d(TAG, "remove floatView immediate");
            floatView.hide();
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        tvInStorage.setText(Formatter.formatFileSize(this, MemoryManager.getAvailableInternalMemorySize()) + "/" + Formatter.formatFileSize(this, MemoryManager.getTotalExternalMemorySize()));
        checkNetworkAddress(null);
    }

    public void checkNetworkAddress(View v) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ip = wifiManager.getConnectionInfo().getIpAddress();
        String ipStr = (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
        textViewIP.setText(ipStr);
        textViewIP.setTextColor(Color.BLUE);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // must unbind service, otherwise it will leak memory
        // connection no need to set it to null
        Log.i(TAG, "unbind service");
    }
}

