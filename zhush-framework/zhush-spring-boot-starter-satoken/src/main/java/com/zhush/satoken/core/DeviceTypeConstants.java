package com.zhush.satoken.core;

/**
 * 登录设备类型常量
 *
 * <p>用于 Sa-Token 多端登录：同账号在同一设备类型再次登录会踢掉旧会话，
 * 不同设备类型（如 PC 与小程序）之间可并存。
 *
 * @author zhush
 */
public final class DeviceTypeConstants {

    private DeviceTypeConstants() {
    }

    /**
     * PC 端
     */
    public static final String PC = "PC";

    /**
     * APP 端
     */
    public static final String APP = "APP";

    /**
     * 小程序端
     */
    public static final String MINI = "MINI";

}
