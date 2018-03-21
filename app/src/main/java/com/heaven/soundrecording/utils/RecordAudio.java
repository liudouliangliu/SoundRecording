package com.heaven.soundrecording.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.czt.mp3recorder.MP3Recorder;

import java.io.File;

/**
 * Created by asus on 2015/5/3.
 */
public class RecordAudio {

    //   private MediaRecorder mediaRecorder;
    private String mDir;
    private String mCurFilePath;//mp3 格式的音频路径
    private String mRawFilePath;//raw格式的音频路径

    private RecordStateListener mListener;

    private boolean isPrepared;

    //   private MP3Recorder  mp3Recorder;
    private MP3Recorder recorder;
    private static Context context1;

    public String getFilePath() {
        return mCurFilePath;
    }


    public interface RecordStateListener {
        void wellPrepared();
    }

    public void setRecordStataListener(RecordStateListener listener) {
        mListener = listener;
    }


    private static RecordAudio mInstance;

    private RecordAudio() {
    }

    public static RecordAudio getInstance(String filePath, Context context) {
        context1 = context;
        if (mInstance == null) {
            synchronized (RecordAudio.class) {
                if (mInstance == null) {
                    mInstance = new RecordAudio();
                    mInstance.mDir = filePath;
                }
            }
        }
        return mInstance;
    }


    public void prepareAudio() {
        isPrepared = false;
        File dir = new File(mDir);
        if (!dir.exists()) dir.mkdirs();
        String mp3fileName = generateMp3FileName();
        final File file = new File(dir, mp3fileName);
        mCurFilePath = file.getAbsolutePath();//获取绝对路径MP3
        recorder = new MP3Recorder(mCurFilePath, 44100);
        recorder.setHandle(new Handler() {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MP3Recorder.MSG_REC_STARTED:
                    case MP3Recorder.MSG_REC_RESTORE:
                        break;
                    case MP3Recorder.MSG_REC_STOPPED:
                        break;
                    case MP3Recorder.MSG_REC_PAUSE:
                        break;
                    case MP3Recorder.MSG_ERROR_GET_MIN_BUFFERSIZE:
                        Toast.makeText(context1, "你的手机不支持此采样率录音失败", Toast.LENGTH_LONG);
                        break;
                    case MP3Recorder.MSG_ERROR_REC_START:

                        break;
                    case MP3Recorder.MSG_ERROR_AUDIO_RECORD:
                        //没有权限 提示开通权限
                        if (mListener instanceof LocalStateListener){
                            ((LocalStateListener) mListener).recordNoPermission();
                        }
                        break;
                }
            }
        });
        recorder.start();

        isPrepared = true;
        if (mListener != null) {
            mListener.wellPrepared();
        }

    }

    private String generateMp3FileName() {
        return System.currentTimeMillis() + ".mp3";

    }


    public int getVoiceLevel(int maxLevel) {
        if (isPrepared) {
            try {

                return recorder.getVoiceVolume();

            } catch (Exception e) {

            }
        }
        return 1;
    }


    public void release() {
        if (recorder != null || isPrepared) {
            recorder.stop();
        }
    }

    public void cancel() {
        release();
        mCurFilePath = null;
        if (mCurFilePath != null) {
            File file = new File(mCurFilePath);
            file.delete();
            mCurFilePath = null;
            file = null;
        }
    }

}
