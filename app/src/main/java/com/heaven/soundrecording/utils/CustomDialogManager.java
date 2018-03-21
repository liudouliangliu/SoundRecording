package com.heaven.soundrecording.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;

/**
 * 作者： @author liuhaijian
 * 创建时间： 2017-07-12 14:09
 * 类描述：
 * 修改人：
 * 修改时间：
 */
public class CustomDialogManager {

    // 图层管理对话框
    private AlertDialog mDialog;
    private LayoutParams mDialogParams;
    private LinearLayout mDialogLinear;
    private CustomAllDialog mCustomDialog;

    private DialogButtonOnClicListener mDialogButtonOnClicListener;

    public void setDialogButtonOnClicListener(DialogButtonOnClicListener dialogButtonOnClicListener) {
        mDialogButtonOnClicListener = dialogButtonOnClicListener;
    }

    /**
     * @author liuhaijian
     * @time 2017-07-12 14:25
     * 方法描述：初始化对话框
     * context:当前调用对话框的activity
     * layoutId：对话框布局文件id
     * cancelable：对话框点击其他地方是否可以消失
     * width：对话框宽度
     * height：对话框高度
     */
    public void createDialog(Activity context, int layoutId, boolean cancelable, double width, double height) {
        // 图层管理对话框
        mDialog = new AlertDialog.Builder(context).create();
        // 得到layer的view，然后就可以从中得到其中的控件
        mDialogLinear = (LinearLayout) context.getLayoutInflater().inflate(
                layoutId, null);
        mDialogParams = new LayoutParams();
        // 设置弹出的MeasureDialog的大小
        mDialogParams.width = (int) width;
        mDialogParams.height = (int) height;
        // 点击其他地方的时对话框消失
        mDialog.setCanceledOnTouchOutside(cancelable);
    }

    public void showDialog() {
        if (mDialog == null) {
            return;
        }
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.cancel();
        }
        mDialog.show();
        // 设置弹出的MeasureDialog的布局
        mDialog.getWindow().setAttributes(
                mDialogParams);
        mDialog.getWindow().setBackgroundDrawable(
                new BitmapDrawable());
        mDialog.getWindow().setContentView(mDialogLinear);
    }

    /**
     * @author liuhaijian
     * @time 2017-07-12 14:22
     * 方法描述：获取view 用于获取dialog中的控件方便添加监听
     */
    public LinearLayout getmDialogLinear() {
        return mDialogLinear;
    }


    /**
     * 显示对话框方法
     *
     * @param context
     * @param layoutId
     */
    public void showCustomDialog(Context context, int layoutId) {
        cancelCustomDialog();
        mCustomDialog = new CustomAllDialog.Builder().create(context, layoutId);
        mCustomDialog.setOnActiveButtonClicListener(new CustomAllDialog.OnButtonClicListener() {
            @Override
            public void onClick() {
                if (mDialogButtonOnClicListener != null) {
                    mDialogButtonOnClicListener.onPositiveClick();
                }
                mCustomDialog.cancel();


            }
        });
        mCustomDialog.setOnNativeButtonClicListener(new CustomAllDialog.OnButtonClicListener() {
                                                        @Override
                                                        public void onClick() {
                                                            if (mDialogButtonOnClicListener != null) {
                                                                mDialogButtonOnClicListener.onNagitiveClick();
                                                            }
                                                            mCustomDialog.cancel();

                                                        }
                                                    }

        );
        mCustomDialog.setmOnCloseClickListener(new CustomAllDialog.OnButtonClicListener()

                                               {
                                                   @Override
                                                   public void onClick() {
                                                       mCustomDialog.cancel();
                                                   }
                                               }

        );
        mCustomDialog.show();
    }

    private void cancelCustomDialog() {
        if (mCustomDialog != null && mCustomDialog.isShowing()) {
            mCustomDialog.cancel();
        }
    }

    public interface DialogButtonOnClicListener {
        void onNagitiveClick();

        void onPositiveClick();

    }


}
