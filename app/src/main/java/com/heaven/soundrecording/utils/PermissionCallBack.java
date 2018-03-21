package com.heaven.soundrecording.utils;

/**
 * 作者：hejinlong*
 * 创建时间：2017/8/16 13:51*
 * 类描述：* 权限管理者的回调
 * 修改人：*
 * 修改内容:*
 * 修改时间：*
 */
public interface PermissionCallBack {
    /**
     * 有权限
     *
     * @param permission 相应的权限
     */

    void havePermission(String permission, int code);


    /**
     * 没有权限
     *
     * @param permission 相应的权限
     */
    void noHavePermission(String permission);
}
