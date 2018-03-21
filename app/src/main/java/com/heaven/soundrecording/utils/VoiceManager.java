package com.heaven.soundrecording.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


/**
 * 作者：xiangzhidan
 * 创建时间：2017-07-25 10:12
 * 类描述：录音管理类（包括录音和播放）
 * 修改人：
 * 修改时间：
 */
public class VoiceManager {

    private static volatile VoiceManager voiceManager;
    public final int MSG_TIME_INTERVAL = 100;
    // 多媒体例如声音的状态
    public final int MEDIA_STATE_UNDEFINE = 200;
    public final int MEDIA_STATE_RECORD_STOP = 210;
    public final int MEDIA_STATE_RECORD_DOING = 220;
    public final int MEDIA_STATE_RECORD_PAUSE = 230;
    public final int MEDIA_STATE_PLAY_STOP = 310;
    public final int MEDIA_STATE_PLAY_DOING = 320;
    public final int MEDIA_STATE_PLAY_PAUSE = 330;
    public final int INITMEDIA = 331;

    private ArrayList<File> mRecList = new ArrayList<File>();
    private Context mContext = null;
    private SeekBar mSBPlayProgress;
    public int mSavedState = MEDIA_STATE_UNDEFINE;
    public int mDeviceState = MEDIA_STATE_UNDEFINE;
    private MediaRecorder mMediaRecorder = null;
    private MediaPlayer mMediaPlayer = null;
    private String mRecTimePrev;
    private long mRecTimeSum = 0;
    /**
     * 录音文件存放的位置(文件夹)
     */
    private String recordFilePath = "";
    /**
     * 播放音频文件位置
     */
    private String playFilePath;
    /**
     * 录音监听
     */
    private VoiceRecordCallBack voiceRecordCallBack;
    private ObtainDecibelThread mThread;
    /**
     * 播放监听
     */
    private VoicePlayCallBack voicePlayCallBack;

    private VoiceManager(Context context) {
        this.mContext = context;
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static VoiceManager getInstance(Context context) {
        if (voiceManager == null) {
            synchronized (VoiceManager.class) {
                if (voiceManager == null) {
                    voiceManager = new VoiceManager(context);
                }
            }
        }
        return voiceManager;
    }

    /**
     * 播放器结束监听
     */
    private MediaPlayer.OnCompletionListener mPlayCompetedListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mDeviceState = MEDIA_STATE_PLAY_STOP;
            mHandler.removeMessages(MSG_TIME_INTERVAL);
            if (mMediaPlayer != null) {
                try {
                    mMediaPlayer.stop();
                } catch (IllegalStateException e) {
                    mMediaPlayer.reset();
                }
            }

            try {
                Thread.sleep(300);
                VoiceManager.this.mMediaPlayer.release();
                VoiceManager.this.mMediaPlayer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mSBPlayProgress != null) {
                mSBPlayProgress.setProgress(0);
            }
            if (voicePlayCallBack != null) {
                voicePlayCallBack.playFinish();
            }
        }
    };
    /**
     * 播放或录音handler
     */
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            VoiceTimeUtils ts;
            int current;
            try {
                switch (msg.what) {
                    case MSG_TIME_INTERVAL:
                        if (mDeviceState == MEDIA_STATE_RECORD_DOING) {//录音
                            ts = VoiceTimeUtils.timeSpanToNow(mRecTimePrev);
                            mRecTimeSum += ts.mDiffSecond;
                            mRecTimePrev = VoiceTimeUtils.getTimeStrFromMillis(ts.mNowTime);
                            ts = VoiceTimeUtils.timeSpanSecond(mRecTimeSum);
                            //回调录音时间
                            if (voiceRecordCallBack != null) {
                                voiceRecordCallBack.recDoing(mRecTimeSum, String.format("%02d:%02d", ts.mSpanMinute, ts.mSpanSecond));
                            }
                            mHandler.sendEmptyMessageDelayed(MSG_TIME_INTERVAL, 1000);
                        } else if (mDeviceState == MEDIA_STATE_PLAY_DOING) { //播放
                            current = mMediaPlayer.getCurrentPosition();
                            if (mSBPlayProgress != null) {
                                mSBPlayProgress.setProgress(current);
                            }
                            ts = VoiceTimeUtils.timeSpanSecond(current / 1000);
                            //回调播放进度
                            if (voicePlayCallBack != null) {
                                voicePlayCallBack.playDoing(current / 1000, String.format("%02d:%02d", ts.mSpanMinute, ts.mSpanSecond));
                            }
                            mHandler.sendEmptyMessageDelayed(MSG_TIME_INTERVAL, 1000);
                        }
                        break;
                    case INITMEDIA://初始化播放声音
                        boolean init = (boolean) msg.obj;
                        Bundle bundle = msg.getData();
                        long totalTime = bundle.getLong("totalTime");
                        String voiceLength = bundle.getString("voiceLength");
                        if (voicePlayCallBack != null) {
                            voicePlayCallBack.voiceTotalLength(totalTime, voiceLength);
                            voicePlayCallBack.playDoing(0, "00:00");
                        }
                        if (mSBPlayProgress != null) {
                            mSBPlayProgress.setMax(Math.max(1, mMediaPlayer.getDuration()));
                        }
                        if (init) {
                            if (mSBPlayProgress != null) {
                                mSBPlayProgress.setProgress(0);
                            }
                            seektoMedia(mMediaPlayer, 0);
                        } else {
                            seektoMedia(mMediaPlayer, mSBPlayProgress.getProgress());
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
            }
        }
    };
    /*********************************录音操作begin***************************/
    /**
     * 录音监听
     *
     * @param callBack
     */
    public void setVoiceRecordListener(VoiceRecordCallBack callBack) {
        voiceRecordCallBack = callBack;
    }

    /**
     * 开始录音（外部调）
     *
     * @param filePath 音频存放文件夹
     */
    public void startVoiceRecord(String filePath) {
        if (!isSDCardAvailable()) return;
        this.recordFilePath = filePath;
        startVoiceRecord(true);
    }

    /**
     * 继续或暂停录音
     */
    public void pauseOrStartVoiceRecord() {
        if (mDeviceState == MEDIA_STATE_RECORD_DOING) {
            mDeviceState = MEDIA_STATE_RECORD_PAUSE;
            stopRecorder(mMediaRecorder, true);
            mMediaRecorder = null;
            voiceRecordCallBack.recPause("已暂停");
        } else {
            startVoiceRecord(false);
        }
    }

    /**
     * 完成录音
     */
    public void stopVoiceRecord() {
        try {
            stopRecorder(mMediaRecorder, true);
            mMediaRecorder = null;
            if (mRecTimeSum >= 2) {
                if (mRecList == null || mRecList.size() == 0) {
                    return;
                }

                File file = getOutputVoiceFile(mRecList);
                if (file != null && file.length() > 0) {
                    cleanFieArrayList(mRecList);
                    final VoiceTimeUtils ts = VoiceTimeUtils.timeSpanSecond(mRecTimeSum);
                    //完成录音
                    if (voiceRecordCallBack != null) {
                        voiceRecordCallBack.recFinish(mRecTimeSum, String.format("%02d:%02d", ts.mSpanMinute, ts.mSpanSecond), file.getAbsolutePath(), VoiceTimeUtils.getTime());
                    }
                }
            }
            mHandler.removeMessages(MSG_TIME_INTERVAL);
            mDeviceState = MEDIA_STATE_RECORD_STOP;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始录音(内部调)
     *
     * @param init 是否是初始化录音还是暂停后再录音
     */

    private void startVoiceRecord(boolean init) {
        if (!isSDCardAvailable()) return;
        if (init) {
            mRecTimeSum = 0;
            cleanFieArrayList(mRecList);
        }
        //录音前停止播放回调
        if (voicePlayCallBack != null) {
            voicePlayCallBack.playFinish();
        }
        stopRecorder(mMediaRecorder, true);
        mMediaRecorder = null;

        stopMedia(mMediaPlayer, true);
        mMediaPlayer = null;
        mMediaRecorder = new MediaRecorder();
        File file = prepareRecorder(mMediaRecorder, true);
        if (file != null) {
            //开始录音回调
            if (voiceRecordCallBack != null) {
                voiceRecordCallBack.recStart(init);
            }
            mDeviceState = MEDIA_STATE_RECORD_DOING;
            mRecTimePrev = VoiceTimeUtils.getTimeStrFromMillis(System.currentTimeMillis());
            mRecList.add(file);


            mHandler.removeMessages(MSG_TIME_INTERVAL);
            mHandler.sendEmptyMessage(MSG_TIME_INTERVAL);
        }
    }

    /**
     * 监听录音声音频率大小
     */
    private class ObtainDecibelThread extends Thread {

        private volatile boolean running = true;

        public void exit() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mMediaRecorder == null || !running) {
                    break;
                }
                try {
                    final double ratio = mMediaRecorder.getMaxAmplitude() / 150;
                    if (ratio != 0 && voiceRecordCallBack != null) {
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                double db = 0;// 分贝
                                if (ratio > 1)
                                    db = (int) (20 * Math.log10(ratio));
                                voiceRecordCallBack.recVoiceGrade((int) db);
                            }
                        });
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * 合并录音
     *
     * @param list
     * @return
     */
    private File getOutputVoiceFile(ArrayList<File> list) {
        String mMinute1 = VoiceTimeUtils.getTime();
        File recDirFile = recAudioDir(recordFilePath);

        // 创建音频文件,合并的文件放这里
        File resFile = new File(recDirFile, System.currentTimeMillis() + ".m4a");
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(resFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // list里面为暂停录音 所产生的 几段录音文件的名字，中间几段文件的减去前面的6个字节头文件
        for (int i = 0; i < list.size(); i++) {
            File file = list.get(i);
            try {
                if (file.length() == 0) {
                    if (voiceRecordCallBack != null) {
                        voiceRecordCallBack.recFail();
                    }
                    return null;
                }
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] myByte = new byte[fileInputStream.available()];
                // 文件长度
                int length = myByte.length;
                if (length > 0) {
                    while (fileInputStream.read(myByte) != -1) {
                        fileOutputStream.write(myByte, 0, length);
                    }
                } else {
                    if (voiceRecordCallBack != null) {
                        voiceRecordCallBack.recFail();
                    }
                }
                fileOutputStream.flush();
                fileInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 结束后关闭流
        try {
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resFile;
    }


    public boolean isHavePermition() {
        // 创建音频文件,合并的文件放这里
        // list里面为暂停录音 所产生的 几段录音文件的名字，中间几段文件的减去前面的6个字节头文件
        for (int i = 0; i < mRecList.size(); i++) {
            File file = mRecList.get(i);
            try {
                if (file.length() == 0) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 结束后关闭流

        return true;
    }

    /**
     * 清空暂停录音所产生的几段录音文件
     *
     * @param list
     */
    private void cleanFieArrayList(ArrayList<File> list) {
        try {
            if (list != null && list.size() > 0) {
                for (File file : list) {
                    file.delete();
                }
                list.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*********************************录音操作end***************************/


    /*********************************播放操作end***************************/
    /**
     * 播放监听
     *
     * @param callBack
     */
    public void setVoicePlayListener(VoicePlayCallBack callBack) {
        voicePlayCallBack = callBack;
    }

    /**
     * 播放SeekBar监听
     *
     * @param seekBar
     */
    public void setSeekBarListener(SeekBar seekBar) {
        mSBPlayProgress = seekBar;
        if (mSBPlayProgress != null) {
            mSBPlayProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mHandler.removeMessages(MSG_TIME_INTERVAL);
                    mSavedState = mDeviceState;
                    if (mSavedState == MEDIA_STATE_PLAY_DOING) {
                        pauseMedia(mMediaPlayer);
                    }
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mHandler.removeMessages(MSG_TIME_INTERVAL);
                    VoiceTimeUtils ts = VoiceTimeUtils.timeSpanSecond(progress / 1000);
                    //播放进度
                    if (voicePlayCallBack != null) {
                        voicePlayCallBack.playDoing(progress / 1000, String.format("%02d:%02d", ts.mSpanMinute, ts.mSpanSecond));
                    }
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    seektoMedia(mMediaPlayer, mSBPlayProgress.getProgress());

                    if (mSavedState == MEDIA_STATE_PLAY_DOING) {
                        playMedia(mMediaPlayer);
                        mHandler.sendEmptyMessage(MSG_TIME_INTERVAL);
                    }
                }
            });
        }
    }

    /**
     * 开始播放（外部调）
     *
     * @param filePath 音频存放文件夹
     */
    public void startPlay(String filePath) {
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            if (voicePlayCallBack != null) {
                voicePlayCallBack.playFinish();
            }
            Toast.makeText(mContext, "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        } else {
            playFilePath = filePath;
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    startPlay(true);
                }
            }.start();
//            startPlay(true);
        }
    }

    /**
     * 开始播放（外部调）
     *
     * @param uri 音频uri
     */
    public void startPlayUri(String uri) {
        if (TextUtils.isEmpty(uri)) {
            if (voicePlayCallBack != null) {
                voicePlayCallBack.playFinish();
            }
            Toast.makeText(mContext, "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        } else {
            playFilePath = uri;
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    startPlay(true);
                }
            }.start();
//            startPlay(true);
        }
    }

    /**
     * 开始播放（内部调）
     *
     * @param init
     */
    private void startPlay(boolean init) {
        try {
            stopRecorder(mMediaRecorder, true);
            mMediaRecorder = null;

            stopMedia(mMediaPlayer, true);
            mMediaPlayer = null;

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(playFilePath);
            mMediaPlayer.setOnCompletionListener(mPlayCompetedListener);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (mMediaPlayer == null) {
                        return;
                    }
                    mMediaPlayer.start();
                    mHandler.removeMessages(MSG_TIME_INTERVAL);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mHandler.sendEmptyMessage(MSG_TIME_INTERVAL);
                        }
                    }, 100);
                }
            });
            if (mMediaPlayer == null) {
                return;
            }

            mMediaPlayer.prepare();
            mDeviceState = MEDIA_STATE_PLAY_DOING;
            //总时间长度
            long totalTime = mMediaPlayer.getDuration() / 1000;
            VoiceTimeUtils ts = VoiceTimeUtils.timeSpanSecond(totalTime);
            String voiceLength = String.format("%02d:%02d", ts.mSpanMinute, ts.mSpanSecond);
            //播放进度回调
            Message msg = mHandler.obtainMessage();
            msg.what = INITMEDIA;
            msg.obj = init;
            Bundle bundle = new Bundle();
            bundle.putString("voiceLength", voiceLength);
            bundle.putLong("totalTime", totalTime);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        } catch (Exception e) {
            Log.e("播放出错了", e.getMessage());
        }
    }

    /**
     * 继续或暂停播放
     */
    public void continueOrPausePlay() {
        if (mDeviceState == MEDIA_STATE_PLAY_DOING) {
            mDeviceState = MEDIA_STATE_PLAY_PAUSE;
            pauseMedia(mMediaPlayer);
            //暂停
            if (voicePlayCallBack != null) {
                voicePlayCallBack.playPause();
            }
        } else if (mDeviceState == MEDIA_STATE_PLAY_PAUSE) {
            mDeviceState = MEDIA_STATE_PLAY_DOING;
            playMedia(mMediaPlayer);
            //播放中
            mHandler.removeMessages(MSG_TIME_INTERVAL);
            mHandler.sendEmptyMessage(MSG_TIME_INTERVAL);
        } else if (mDeviceState == MEDIA_STATE_PLAY_STOP) {
            //播放
            if (!TextUtils.isEmpty(playFilePath)) {
                startPlay(false);
            }
        }
    }

    /**
     * 停止播放
     */
    public void stopPlay() {
        mHandler.removeMessages(MSG_TIME_INTERVAL);
        mDeviceState = MEDIA_STATE_PLAY_STOP;
        stopMedia(mMediaPlayer, true);
        mMediaPlayer = null;
    }

    /**
     * 是否在播放中
     *
     * @return
     */
    public boolean isPlaying() {
        return mDeviceState == MEDIA_STATE_PLAY_DOING;
    }

    /**
     * 是否在录音中
     *
     * @return
     */
    public boolean isRecordDoing() {
        return mDeviceState == MEDIA_STATE_RECORD_DOING;
    }

    /*********************************播放操作end***************************/


    /**
     * 播放录音准备工作
     *
     * @param mp
     * @param file
     * @return
     */
    private boolean prepareMedia(MediaPlayer mp, String file) {
        boolean result = false;
        try {
            mp.setDataSource(file);
            mp.prepare();
            result = true;
        } catch (Exception e) {
            mp.reset();
            mp.release();
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 播放录音开始
     *
     * @param mp
     * @return
     */
    private boolean playMedia(MediaPlayer mp) {
        boolean result = false;
        try {
            if (mp != null) {
                mp.start();
                result = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 拖动播放进度条
     *
     * @param mp
     * @param pos
     * @return
     */
    private boolean seektoMedia(MediaPlayer mp, int pos) {
        boolean result = false;
        try {
            if (mp != null && pos >= 0) {
                mp.seekTo(pos);
                result = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 停止播放
     *
     * @param mp
     * @param release
     * @return
     */
    private boolean stopMedia(MediaPlayer mp, boolean release) {
        boolean result = false;
        try {
            if (mp != null) {
                try {
                    mp.stop();
                    mp.reset();
                    mp.release();
                    mp = null;
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                result = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 暂停播放
     *
     * @param mp
     * @return
     */
    private boolean pauseMedia(MediaPlayer mp) {
        boolean result = false;
        try {
            if (mp != null) {
                mp.pause();
                result = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 最后停止录音
     *
     * @param mRecorder
     * @param release
     * @return
     */
    private boolean stopRecorder(MediaRecorder mRecorder, boolean release) {
        boolean result = false;
        try {
            if (mRecorder != null) {
                try {
                    Thread.sleep(300);
                    mRecorder.stop();
                } catch (RuntimeException | InterruptedException e) {
                    mRecorder.reset();
                    mRecorder.release();
                }
                result = true;
            }

            if (mThread != null) {
                mThread.exit();
                mThread = null;
            }
        } catch (Exception e) {
            if (mThread != null) {
                mThread.exit();
                mThread = null;
            }
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 录音准备工作 ，开始录音
     *
     * @param mr
     * @param start
     * @return
     */
    @SuppressWarnings("deprecation")
    private File prepareRecorder(MediaRecorder mr, boolean start) {
        File recFile = null;
        if (mr == null) return null;
        try {
            String path = recAudioDir(recordFilePath).getAbsolutePath();
            recFile = new File(path, System.currentTimeMillis() + ".m4a");
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setAudioEncodingBitRate(12000);
            mMediaRecorder.setAudioChannels(1);
            mMediaRecorder.setAudioSamplingRate(8000);
            mr.setOutputFile(recFile.getAbsolutePath());
            mr.prepare();
            if (start) {
                mr.start();
                if (mThread == null) {
                    mThread = new ObtainDecibelThread();
                    mThread.start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recFile;
    }

    /**
     * 停止录音和播放
     */
    public void stopRecordAndPlay() {
        stopRecorder(mMediaRecorder, true);
        mDeviceState = MEDIA_STATE_RECORD_STOP;
        mMediaRecorder = null;
        stopMedia(mMediaPlayer, true);
        mMediaPlayer = null;
    }

    /**
     * 录音回调监听
     */
    public interface VoiceRecordCallBack {
        /**
         * 录音中
         */
        void recDoing(long time, String strTime);

        /**
         * 录音中的声音频率等级
         */
        void recVoiceGrade(int grade);

        /**
         * 录音开始
         */
        void recStart(boolean init);

        /**
         * 录音暂停
         */
        void recPause(String str);

        /**
         * 录音失败
         */
        void recFail();

        /**
         * 录音结束
         */
        void recFinish(long length, String strLength, String path, String currTime);
    }

    /**
     * 播放录音回调监听
     */
    public interface VoicePlayCallBack {

        /**
         * 音频长度 指定的某个时间段，以秒为单位
         */
        void voiceTotalLength(long time, String strTime);

        /**
         * 播放中 指定的某个时间段，以秒为单位
         */
        void playDoing(long time, String strTime);

        /**
         * 播放暂停
         */
        void playPause();

        /**
         * 播放开始
         */
        void playStart();

        /**
         * 播放结束
         */
        void playFinish();
    }

    /**
     * SD卡是否可用
     */
    public static boolean isSDCardAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static File recAudioDir(String path) {
        File file = null;
        try {
            file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }
}
