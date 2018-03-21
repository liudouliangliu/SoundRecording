package com.heaven.soundrecording;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.heaven.soundrecording.utils.CustomDialogManager;
import com.heaven.soundrecording.utils.CustomProgress;
import com.heaven.soundrecording.utils.LocalStateListener;
import com.heaven.soundrecording.utils.PermissionCallBack;
import com.heaven.soundrecording.utils.PermissionManager;
import com.heaven.soundrecording.utils.RecordAudio;
import com.heaven.soundrecording.utils.VoiceManager;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PermissionCallBack, LocalStateListener, VoiceManager.VoicePlayCallBack, VoiceManager.VoiceRecordCallBack {
    private PermissionManager permissionManager;
    /*录音的请求码*/
    private final int RECORD_AUDIO_CODE = 1;
    private final int MP3_RECORD_AUDIO_CODE = 2;
    private boolean isRecording = false;
    private TextView tvRecoding;
    private VoiceManager voiceManager;//m4a录音和所有播放所用的控制器
    private RecordAudio audio;//mp3录音所用的控制器
    private long mTime;
    private View layoutItem;
    private String filePath;
    private CustomProgress progress;
    private String timeStr;
    private TextView soundLength;
    private TextView tvCount;
    private ImageView ivDelete;

    //录音倒计时
    private CountDownTimer mCountDownTimer = new CountDownTimer((long) (60 * 1000), 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            tvCount.setText("" + millisUntilFinished / 1000);
        }

        @Override
        public void onFinish() {
            stopRecord();
        }
    };
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initManager();
    }

    private void initView() {
        tvRecoding = findViewById(R.id.tv_recoding);
        layoutItem = findViewById(R.id.layout_recoding_item);
        layoutItem.setVisibility(View.GONE);
        progress = findViewById(R.id.custom_progress);
        progress.setOnClickListener(this);
        soundLength = findViewById(R.id.tv_sound_duration);
        progress.setStatus(CustomProgress.FINISH_STATUS);
        tvCount = findViewById(R.id.tv_count);
        tvCount.setVisibility(View.GONE);
        tvRecoding.setVisibility(View.GONE);
        permissionManager = new PermissionManager(this, this);
        ivDelete = findViewById(R.id.iv_close);
        ivDelete.setOnClickListener(this);
    }

    private void initManager() {
        voiceManager = VoiceManager.getInstance(this);
        voiceManager.setVoicePlayListener(this);
        voiceManager.setVoiceRecordListener(this);
        String dir = getsdSaveFile(this).getAbsolutePath();
        try {
            File file = new File(dir);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        audio = RecordAudio.getInstance(dir, this);
        audio.setRecordStataListener(this);
    }

    private File getsdSaveFile(Context mainActivity) {
        String sdpath = Environment.getExternalStorageDirectory() + "/" + mainActivity.getPackageName() + "/audio/";
        file = new File(sdpath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.m4a:
                if (voiceManager.isRecordDoing()){
                    stopM4a();
                    return;
                }
                if (isRecording) {
                    stopMp3();
                    isRecording = false;
                    return;
                }
                permissionManager.requestPermission(Manifest.permission.RECORD_AUDIO, RECORD_AUDIO_CODE);
                break;
            case R.id.mp3:
                if (voiceManager.isRecordDoing()){
                    stopM4a();
                    return;
                }
                if (isRecording) {
                    stopMp3();
                    isRecording = false;
                    return;
                }
                permissionManager.requestPermission(Manifest.permission.RECORD_AUDIO, MP3_RECORD_AUDIO_CODE);
                break;

            case R.id.custom_progress:
                //播放声音
                playLocalSound();
                break;
            case R.id.iv_close:
                deleteLocalSound();
                break;
        }
    }

    //删除本地录音
    private void deleteLocalSound() {
        if (filePath!=null){
            File file = new File(filePath);
            if (file.exists()){
                file.delete();
            }
        }

    }

    //停止录音
    private void stopRecord() {
        tvRecoding.setVisibility(View.GONE);
        if (voiceManager.isPlaying()) {
            voiceManager.stopPlay();
        }
        tvCount.setVisibility(View.GONE);
        stopMp3();
        stopM4a();
    }

    private void stopM4a() {
        if (voiceManager.isRecordDoing()) {
            voiceManager.stopVoiceRecord();
            tvRecoding.setVisibility(View.GONE);
            mCountDownTimer.cancel();
        }
    }

    private void stopMp3() {
        audio.release();
        filePath = audio.getFilePath();
        String munite = mTime / 60 >= 10 ? mTime / 60 + ":" : "0" + mTime / 60 + ":";
        String miao = mTime % 60 >= 10 ? mTime % 60 + "" : "0" + mTime % 60;
        timeStr = munite + miao;
        if (mTime > 2) {
            showItem();
        }
        tvRecoding.setVisibility(View.GONE);
        mCountDownTimer.cancel();
        resetAudio();
    }

    private void resetAudio() {
        isRecording = false;
        mTime = 0;
    }

    /**
     * 录制m4a视频
     */
    private void startM4aRecord() {
        tvRecoding.setText("m4a录制中。。。");
        tvRecoding.setVisibility(View.VISIBLE);
        voiceManager.startVoiceRecord(file.getAbsolutePath());
    }

    /**
     * 录制mp3视频
     */
    private void startMp3Record() {
        tvRecoding.setText("mp3录制中。。。");
        tvRecoding.setVisibility(View.VISIBLE);
        tvCount.setVisibility(View.VISIBLE);
        mCountDownTimer.start();
        audio.prepareAudio();
        isRecording = true;
        new Thread(mGetVoiceLevelRunnable).start();

    }

    private void showToast(String m4a) {
        Toast.makeText(getApplicationContext(), m4a, Toast.LENGTH_LONG).show();
    }

    //有权限
    @Override
    public void havePermission(String permission, int code) {
        switch (code) {
            case MP3_RECORD_AUDIO_CODE:
                startMp3Record();
                break;
            case RECORD_AUDIO_CODE:
                startM4aRecord();
                break;
        }
    }

    //没有权限
    @Override
    public void noHavePermission(String permission) {
        showNoPermissionDialog();
    }

    private void showNoPermissionDialog() {
        CustomDialogManager manager = new CustomDialogManager();
        manager.setDialogButtonOnClicListener(new CustomDialogManager.DialogButtonOnClicListener() {
            @Override
            public void onNagitiveClick() {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
            }

            @Override
            public void onPositiveClick() {

            }
        });
        manager.showCustomDialog(this, R.layout.request_permission_dialog);
    }

    //mp3格式录制中没有权限的回调函数
    @Override
    public void recordNoPermission() {
        stopMp3();
        showNoPermissionDialog();
    }

    //mp3格式音频开始录制
    @Override
    public void wellPrepared() {
        isRecording = true;
        mCountDownTimer.start();
    }

    @Override
    public void voiceTotalLength(long time, String strTime) {
        progress.setmTotalProgress((int) (time));
    }

    @Override
    public void playDoing(long time, String strTime) {
        progress.setProgress((int) (time));
    }

    @Override
    public void playPause() {

    }

    @Override
    public void playStart() {

    }

    @Override
    public void playFinish() {
        progress.setStatus(CustomProgress.FINISH_STATUS);
        layoutItem.setVisibility(View.GONE);
    }


    private Runnable mGetVoiceLevelRunnable = new Runnable() {
        @Override
        public void run() {
            while (isRecording) {
                try {
                    Thread.sleep(998);
                    mTime += 1;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
        stopRecord();
    }

    @Override
    public void recDoing(long time, String strTime) {
        if (!voiceManager.isHavePermition()) {
            showNoPermissionDialog();
            stopM4a();
        }
    }

    @Override
    public void recVoiceGrade(int grade) {

    }

    @Override
    public void recStart(boolean init) {
        tvCount.setVisibility(View.VISIBLE);
        mCountDownTimer.start();
    }

    @Override
    public void recPause(String str) {

    }

    @Override
    public void recFail() {

    }

    @Override
    public void recFinish(long length, String strLength, String path, String currTime) {
        //停止录音 显示条目
        timeStr = strLength;
        filePath = path;
        mCountDownTimer.cancel();
        tvCount.setVisibility(View.GONE);
        showItem();
    }

    private void showItem() {
        layoutItem.setVisibility(View.VISIBLE);
        soundLength.setText(timeStr);
    }

    //播放本地声音
    private void playLocalSound() {
        int status = progress.getSTATUS();
        switch (status) {
            case CustomProgress.FINISH_STATUS://结束状态
                startPlaySound();
                break;
            case CustomProgress.START_STATUS://播放状态
                stopPlaySound();
                break;
        }
    }

    //停止播放录音
    private void stopPlaySound() {
        progress.setStatus(CustomProgress.FINISH_STATUS);
        if (voiceManager.isPlaying()) {
            voiceManager.stopPlay();
            progress.setStatus(CustomProgress.FINISH_STATUS);
        }
    }

    //开始播放
    private void startPlaySound() {
        voiceManager.startPlay(filePath);
        progress.setStatus(CustomProgress.START_STATUS);
    }


}
