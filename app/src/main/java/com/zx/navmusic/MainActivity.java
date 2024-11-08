package com.zx.navmusic;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.zx.navmusic.common.App;
import com.zx.navmusic.common.LocalStore;
import com.zx.navmusic.databinding.ActivityMainBinding;
import com.zx.navmusic.util.PermissionUtils;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        App.App_Name = getString(R.string.app_name);
        App.MainActivity = this;


        if (!PermissionUtils.checkFilePermission(this)) {
            Log.d(App.App_Name, "开始申请权限");
        } else {
            Log.d(App.App_Name, "具备权限");
        }


        // 监听蓝牙连接和断开
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
//        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
//        activity.registerReceiver(bluetoothReceiver, filter);

        Intent intent = new Intent(this, MusicService.class);
        startForegroundService(intent);

        LocalStore.loadFile(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissions.length == 0) {
            return;
        }

        String permissionName = "";
        switch (requestCode) {
            case PermissionUtils.FILE_PERMISSION_REQUEST_CODE:
                permissionName = "文件读写权限";
                break;
        }

        boolean result = true;
        for (int grantResult : grantResults) {
            if (PackageManager.PERMISSION_GRANTED != grantResult) {
                result = false;
                break;
            }
        }

//        Toast.makeText(this, permissionName + "申请" + (result ? "成功" : "失败"), Toast.LENGTH_SHORT).show();
        Log.d(App.App_Name, String.format("[权限申请结果] requestCode: %s, permissions: %s, grantResults: %s",
                requestCode, Arrays.toString(permissions), Arrays.toString(grantResults)));
    }
}