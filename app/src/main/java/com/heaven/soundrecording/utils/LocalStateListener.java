package com.heaven.soundrecording.utils;


/**
 * 作者： @author liuhaijian
 * 创建时间： 2018-03-19 10:02
 * 类描述：
 * 修改人：
 * 修改时间：
 */

public interface LocalStateListener extends RecordAudio.RecordStateListener {
    void recordNoPermission();
}
