package com.zx.navmusic.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.activity.ComponentActivity;
import androidx.core.content.ContextCompat;

public class PermissionUtils {

    public static final String[] FILE_PERMISSION = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
    };
    public static final int FILE_PERMISSION_REQUEST_CODE = 777;

    public static boolean checkFilePermission(ComponentActivity ca) {
        if (!checkPermission(ca, FILE_PERMISSION)) {
            requireFilePermission(ca);
            return false;
        }
        return true;
    }

    public static void requireFilePermission(Activity context) {
        requirePermission(context, FILE_PERMISSION, FILE_PERMISSION_REQUEST_CODE);
    }

    public static boolean checkPermission(Context context, String[] permissions) {
        for (String permission : permissions) {
            int checked = ContextCompat.checkSelfPermission(context, permission);
            if (PackageManager.PERMISSION_GRANTED != checked) {
                return false;
            }
        }
        return true;
    }

    public static void requirePermission(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }
}
