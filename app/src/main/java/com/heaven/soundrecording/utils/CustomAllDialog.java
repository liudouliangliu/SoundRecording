package com.heaven.soundrecording.utils;

import android.app.Dialog;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.heaven.soundrecording.R;

/**
 * 作者： @author liuhaijian
 * 创建时间： 2016-12-12 15:50
 * 类描述：
 * 修改人：
 * 修改时间：
 */
public class CustomAllDialog extends Dialog {

    private static WebView view;
    private static View mCurrentView;
    private static SpannableStringBuilder des;

    public CustomAllDialog(Context context) {
        super(context);
    }

    public CustomAllDialog(Context context, int theme) {
        super(context, theme);
    }

    protected CustomAllDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    private static OnButtonClicListener onActiveButtonClicListener;
    private static String webviewContent;
    private static OnButtonClicListener onNativeButtonClicListener;
    private static OnButtonClicListener mOnCloseClickListener;

    public void setmOnCloseClickListener(OnButtonClicListener mOnCloseClickListener) {
        CustomAllDialog.mOnCloseClickListener = mOnCloseClickListener;
    }

    public static void setWebviewContent(String onFillViewListener) {
        CustomAllDialog.webviewContent = onFillViewListener;
    }

    public static void setTextDes(SpannableStringBuilder des) {
        CustomAllDialog.des = des;
    }

    public void setOnNativeButtonClicListener(OnButtonClicListener onNativeButtonClicListener) {
        this.onNativeButtonClicListener = onNativeButtonClicListener;
    }

    public void setOnActiveButtonClicListener(OnButtonClicListener onButtonClicListener) {
        this.onActiveButtonClicListener = onButtonClicListener;
    }

    public interface OnButtonClicListener {
        void onClick();
    }


    private static Button btnNative;
    private static Button btnActive;

    public void setBtnNativeEnable(boolean enable) {
        if (btnNative != null) {
            btnNative.setEnabled(enable);
        }
    }

    public void setBtnActiveEnable(boolean enable) {
        if (btnActive != null) {
            btnActive.setEnabled(enable);
        }
    }

    public static class Builder {
        private Context context;

        public CustomAllDialog create(Context context, int layoutId) {
            this.context = context;
            LayoutInflater inflater = LayoutInflater.from(context);
            final CustomAllDialog dialog = new CustomAllDialog(context, R.style.Dialog);
            View layout = createView(inflater, layoutId, dialog);
            dialog.setCanceledOnTouchOutside(false);
            dialog.addContentView(layout, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT
                    , ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog.setContentView(layout);
            return dialog;
        }

        private View createView(LayoutInflater inflater, int layoutId, final CustomAllDialog dialog) {
            View layout = inflater.inflate(layoutId, null);
            mCurrentView = layout;
            btnNative = (Button) layout.findViewById(R.id.btn_native_cancel);
            if (btnNative != null) {
                btnNative.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (onNativeButtonClicListener != null) {
                            onNativeButtonClicListener.onClick();
                        } else {
                            dialog.cancel();
                        }
                    }
                });
            }
            TextView tv_des = (TextView) layout.findViewById(R.id.tv_des);
            if (tv_des != null && des != null) {
                tv_des.setText(des);
            }

            View close = layout.findViewById(R.id.img_close);
            if (close != null) {
                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnCloseClickListener != null) {
                            mOnCloseClickListener.onClick();
                        } else {
                            dialog.cancel();
                        }
                    }
                });
            }

            return layout;
        }

    }

    public View getView() {
        return mCurrentView;
    }

    @Override
    public void cancel() {
        super.cancel();
        if (view != null) {
            view.destroy();
            view = null;
        }

        if (mCurrentView != null) {
            mCurrentView = null;
        }
        if (onActiveButtonClicListener != null) {
            onActiveButtonClicListener = null;
        }
        if (onNativeButtonClicListener != null) {
            onNativeButtonClicListener = null;
        }
        if (mOnCloseClickListener != null) {
            mOnCloseClickListener = null;
        }
        if (btnNative != null) {
            btnNative = null;
        }
        if (btnActive != null) {
            btnActive = null;
        }
        if (des != null) {
            des.clear();
            des = null;
        }
    }
}
