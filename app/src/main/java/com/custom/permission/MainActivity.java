package com.custom.permission;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.custom.annotation.PermissionFail;
import com.custom.annotation.PermissionRequest;
import com.custom.annotation.PermissionSuccess;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @PermissionSuccess({Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS})
    public void requestContactsSuccess() {
        Log.d("Ysw", "requestContacts: 成功: 请求读取联系人权限");
        Toast.makeText(this, "成功: 请求读取联系人权限", Toast.LENGTH_SHORT).show();
    }

    @PermissionFail({Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS})
    public void requestContactsFail(PermissionRequest request, int resultCode) {
        switch (resultCode) {
            case 0:
                Log.d("Ysw", "requestContactsFail: 失败 : 请求读取联系人权限  未勾选不再提醒");
                Toast.makeText(this, "失败 : 请求读取联系人权限  未勾选不再提醒", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Log.d("Ysw", "requestContactsFail: 失败 : 请求读取联系人权限  已经勾选不再提醒");
                Toast.makeText(this, "失败 : 请求读取联系人权限  已经勾选不再提醒", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @PermissionSuccess({Manifest.permission.CAMERA})
    public void requestCamera() {
        Log.d("Ysw", "requestCamera: 相机权限请求成功");
        Toast.makeText(this, "相机权限请求成功", Toast.LENGTH_SHORT).show();
    }

    @PermissionFail({Manifest.permission.CAMERA})
    public void requestCameraFail(PermissionRequest request, int resultCode) {
        switch (resultCode) {
            case 0:
                Log.d("Ysw", "requestCameraFail: 相机权限请求失败   未勾选不再提醒");
                Toast.makeText(this, "相机权限请求失败   未勾选不再提醒", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Log.d("Ysw", "requestCameraFail: 相机权限请求失败   已经勾选不再提醒");
                Toast.makeText(this, "失败 : 相机权限请求失败   已经勾选不再提醒", Toast.LENGTH_SHORT).show();
                break;
        }
    }


    public void requestPermission(View view) {
        if (Build.VERSION.SDK_INT >= 23) {
            //检查某项权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                // shouldShowRequestPermissionRationale
                // 如果用户之前拒绝了该请求，该方法将返回 true；
                // 如果用户之前拒绝了某项权限并且选中了权限请求对话框中的不再询问选项，
                // 或者如果设备政策禁止该权限，该方法将返回 false
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                    Log.d("Ysw", "requestPermission: 用户之前拒绝了该请求,但未勾选不再询问---11111111111");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 1000);
                } else {
                    Log.d("Ysw", "requestPermission: 我们第一次申请权限 或 用户之前拒绝了该请求,并且勾选不再询问---22222222222");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 1000);
                }
            } else {
                Log.d("Ysw", "requestPermission: 已经有这个权限---333333333333");
            }
        } else {
            Log.d("Ysw", "requestPermission: 编译版本小于23");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1000: {
                // 权限通过
                for (int grantResult : grantResults) {
                    Log.d("Ysw", "onRequestPermissionsResult: " + grantResult);
                }
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Ysw", "onRequestPermissionsResult: 权限请求通过---44444444444");
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                        Log.d("Ysw", "onRequestPermissionsResult: 用户拒绝权限,但没有勾选不再提醒---55555555555");
                    } else {
                        Log.d("Ysw", "onRequestPermissionsResult: 用户拒绝权限,且勾选了不再提醒---66666666666");
                    }
                }
            }
            break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivity_PermissionRequest.onRequestPermissionResult(this, requestCode, grantResults);
    }

    public void IOCPermission(View view) {
        MainActivity_PermissionRequest.requestContactsSuccessPermissionCheck(this);
    }

    public void cameraPermission(View view) {
        MainActivity_PermissionRequest.requestCameraPermissionCheck(this);
    }
}
