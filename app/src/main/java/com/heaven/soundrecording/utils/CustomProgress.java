package com.heaven.soundrecording.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.heaven.soundrecording.R;


/**
 * Created by zhen on 2016/8/17.
 */
public class CustomProgress extends View {
    // 画实心圆的画笔
    private Paint mCirclePaint;
    // 画圆环的画笔
    private Paint mRingPaint;
    // 画字体的画笔
    private Paint mTextPaint;
    // 画方形的画笔
    private Paint mRectPaint;
    // 圆形颜色
    private int mCircleColor;
    // 圆环颜色
    private int mRingColor;
    // 半径
    private float mRadius;
    // 圆环半径
    private float mRingRadius;
    // 圆环宽度
    private float mStrokeWidth;
    // 圆心x坐标
    private int mXCenter;
    // 圆心y坐标
    private int mYCenter;
    // 字的长度
    private float mTxtWidth;
    // 字的高度
    private float mTxtHeight;
    // 总进度
    private int mTotalProgress;
    // 当前进度
    private int mProgress;
    //大圆
    private Paint mBigPatient;
    //字体颜色
    private int mTextColor;
    //外圆颜色
    private int mBigCircleColor;
    //方形颜色
    private int mRectColor;

    public static final int NORMAL_STATUS = 0;
    public static final int START_STATUS = 1;
    public static final int FINISH_STATUS = 2;

    private String txt;

    private int STATUS;

    public CustomProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 获取自定义的属性
        initAttrs(context, attrs);
        initVariable();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typeArray = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.CustomProgress, 0, 0);
        mRadius = typeArray.getDimension(R.styleable.CustomProgress_CustomProgress_radius, 300);
        mStrokeWidth = typeArray.getDimension(R.styleable.CustomProgress_CustomProgress_strokeWidth, 20);
        mCircleColor = typeArray.getColor(R.styleable.CustomProgress_CustomProgress_circleColor, Color.BLUE);
        mRingColor = typeArray.getColor(R.styleable.CustomProgress_CustomProgress_ringColor, Color.RED);
        mTotalProgress = typeArray.getInt(R.styleable.CustomProgress_CustomProgress_totalProgress, 100);
        mTextColor = typeArray.getColor(R.styleable.CustomProgress_CustomProgress_textColor, Color.WHITE);
        mBigCircleColor = typeArray.getColor(R.styleable.CustomProgress_CustomProgress_bigCircleColor, Color.WHITE);
        mRectColor = typeArray.getColor(R.styleable.CustomProgress_CustomProgress_rectColor, Color.WHITE);
        txt = "1";
        typeArray.recycle();//注意这里要释放掉

        mRingRadius = mRadius + mStrokeWidth / 2;
    }

    //初始化画笔
    private void initVariable() {
        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setColor(mCircleColor);
        mCirclePaint.setStrokeCap(Paint.Cap.ROUND);
        mCirclePaint.setStyle(Paint.Style.FILL);

        mRingPaint = new Paint();
        mRingPaint.setAntiAlias(true);
        mRingPaint.setColor(mRingColor);
        mRingPaint.setStrokeCap(Paint.Cap.ROUND);
        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setStrokeWidth(mStrokeWidth / 3 * 2);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mRadius);


        mBigPatient = new Paint();
        mBigPatient.setColor(mTextColor);
        mBigPatient.setAntiAlias(true);
        mBigPatient.setStyle(Paint.Style.STROKE);
        mBigPatient.setStrokeWidth(mStrokeWidth / 2);

        mRectPaint = new Paint();
        mRectPaint.setAntiAlias(true);
        mRectPaint.setStyle(Paint.Style.FILL);
        mRectPaint.setColor(mTextColor);
        mRectPaint.setStrokeWidth(mStrokeWidth / 2);

        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        mTxtHeight = (int) Math.ceil(fm.descent - fm.ascent);

    }

    private boolean isFinish = false;
    private boolean isNormal = true;

    @Override
    protected void onDraw(Canvas canvas) {
        //计算中心点
        mXCenter = getWidth() / 2;
        mYCenter = getHeight() / 2;
        //未开始播放状态，默认显示文字，这里默认设置为1
        if (isNormal) {
            //计算文字长度
            mTxtWidth = mTextPaint.measureText(txt, 0, txt.length());
            //在中心点画文字
            canvas.drawText(txt, mXCenter - mTxtWidth / 2, mYCenter + mTxtHeight / 4, mTextPaint);
            return;
        }
        //正在播放状态或者结束状态的时候 画圆环
        canvas.drawCircle(mXCenter, mYCenter, mRadius + mStrokeWidth / 2, mBigPatient);
        canvas.drawCircle(mXCenter, mYCenter, mRadius, mCirclePaint);
        //判断是正在播放还是结束状态
        if (!isFinish) {
            //正在播放状态，画出中间双竖
            canvas.drawLine(mXCenter - mRadius / 4, mYCenter - mRadius / 2 - mStrokeWidth / 4, mXCenter - mRadius / 4, mYCenter + mRadius / 2 + mStrokeWidth / 4, mRectPaint);
            canvas.drawLine(mXCenter + mRadius / 4, mYCenter - mRadius / 2 - mStrokeWidth / 4, mXCenter + mRadius / 4, mYCenter + mRadius / 2 + mStrokeWidth / 4, mRectPaint);
        } else {
            //结束播放状态，画三角形
            canvas.drawLine(mXCenter - mRadius / 4, mYCenter - mRadius / 2 - mStrokeWidth / 5, mXCenter - mRadius / 4, mYCenter + mRadius / 2 + mStrokeWidth / 5, mRectPaint);
            canvas.drawLine(mXCenter + mRadius / 2, mYCenter, mXCenter - mRadius / 4, mYCenter - mRadius / 2, mRectPaint);
            canvas.drawLine(mXCenter + mRadius / 2 + mStrokeWidth / 5, mYCenter, mXCenter - mRadius / 4, mYCenter + mRadius / 2, mRectPaint);
            return;
        }
        //根据进度画圆弧
        if (mProgress > 0) {
            RectF oval = new RectF();
            oval.left = (mXCenter - mRingRadius);
            oval.top = (mYCenter - mRingRadius);
            oval.right = mRingRadius * 2 + (mXCenter - mRingRadius);
            oval.bottom = mRingRadius * 2 + (mYCenter - mRingRadius);
            canvas.drawArc(oval, -90, ((float) mProgress / mTotalProgress) * 360, false, mRingPaint); //
            if (mProgress == mTotalProgress) {
                isFinish = true;
                mBigPatient.setColor(mRectColor);
            }
        }
    }

    //设置进度的方法
    public void setProgress(int progress) {
        mProgress = progress;
        postInvalidate();
    }

    //设置总进度
    public void setmTotalProgress(int totalProgress) {
        mTotalProgress = totalProgress;
    }

    public int getSTATUS() {
        return STATUS;
    }

    //设置当前的按钮的播放状态
    public void setStatus(int status) {
        STATUS = status;
        switch (status) {
            case NORMAL_STATUS:
                isNormal = true;
                mBigPatient.setColor(Color.WHITE);
                postInvalidate();
                break;
            case START_STATUS:
                isFinish = false;
                isNormal = false;
                mBigPatient.setColor(Color.parseColor("#95ffffff"));
                break;
            case FINISH_STATUS:
                isFinish = true;
                isNormal = false;
                mBigPatient.setColor(mRectColor);
                postInvalidate();
                break;
        }
    }

    //设置默认状态的文字
    public void setText(String text) {
        this.txt = text;
    }

    //设置中间双竖的方法
    public void setRectColor(int color) {
        this.mRectColor = color;
        mRectPaint.setColor(mRectColor);
    }
}