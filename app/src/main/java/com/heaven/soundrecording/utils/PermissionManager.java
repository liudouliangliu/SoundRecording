package com.heaven.soundrecording.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * 作者：hejinlong*
 * 创建时间：2017/8/16 13:12*
 * 类描述：*  权限管理类
 * 修改人：*
 * 修改内容:*
 * 修改时间：*
 */
public class PermissionManager {


    private PermissionCallBack mPermissionCallBack;
    private Activity mActivity;
    private int mRequestCode;

    public PermissionManager(Activity activity, PermissionCallBack permissionCallBack) {
        this.mPermissionCallBack = permissionCallBack;
        this.mActivity = activity;
    }

    public void requestPermission(@NonNull String permission, int requestCode) {
        /**
         * 判断版本是否大于23
         */
        if (Build.VERSION.SDK_INT >= 23) {
            /*判断读的权限*/
            if (ContextCompat.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_GRANTED) {
                /*有权限*/
                if (mPermissionCallBack != null)
                    mPermissionCallBack.havePermission(permission, requestCode);
            } else {
                /*是否之前拒绝过这个权限*/
                boolean isHold = ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permission);
                if (isHold) {/*之前没有申请过这个权限*/
                    if (mPermissionCallBack != null)
                        mPermissionCallBack.noHavePermission(permission);
                } else {
                    mRequestCode = requestCode;
                    ActivityCompat.requestPermissions(mActivity, new String[]{permission}, requestCode);
                }
            }
        } else {
            /**处理23以下的权限问题*/
            if (mPermissionCallBack != null) {
                mPermissionCallBack.havePermission(permission, requestCode);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == mRequestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                /*权限请求成功*/
                if (mPermissionCallBack != null) {
                    mPermissionCallBack.havePermission(permissions[0], requestCode);
                }
            } else {
                /**之前被拒绝弹出对话框提示为什么要申请这个权限*/
                if (mPermissionCallBack != null)
                    mPermissionCallBack.noHavePermission(permissions[0]);
            }
        }
    }
}
