package com.chenqiao.moduleiot.h2;

import com.aliyun.alink.h2.api.Profile;

/**
 * @author brhao
 * @date 11/06/2018
 */
public class EnvConfig {
    public String appKey;
    public String appSecret;
    public String httpsUrl;
    public String productKey;
    public String deviceName;
    public String deviceSecret;
    public static EnvConfig ENV;

    public static final EnvConfig DAILY_205 = new EnvConfig(
            "",
            "",
            "");

    public static final EnvConfig DAILY_223 = new EnvConfig(
            "",
            "",
            "");

    public static final EnvConfig DAILY_DEV = new EnvConfig(
            "",
            "",
            "",
            "", "", "");

    public static final EnvConfig PRE = new EnvConfig(
            "",
            "",
            "",
            "", "", "");

    public static final EnvConfig INDUSTRY_VPC = new EnvConfig(
            "",
            "",
            "");

    private EnvConfig(String appKey, String appSecret, String httpsUrl) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.httpsUrl = httpsUrl;
    }

    private EnvConfig(String appKey, String appSecret, String httpsUrl, String productKey,
                      String deviceName, String deviceSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.httpsUrl = httpsUrl;
        this.productKey = productKey;
        this.deviceName = deviceName;
        this.deviceSecret = deviceSecret;
    }

    public Profile getAppkeyProfile() {
        return Profile.getAppKeyProfile(httpsUrl, appKey, appSecret);
    }

    public Profile getDeviceProfile() {
        return Profile.getDeviceProfile(httpsUrl, productKey, deviceName, deviceSecret, "clientId");
    }

    public static Profile deviceProfile() {
        return ENV.getDeviceProfile();
    }

    public static Profile appkeyProfile() {
        return ENV.getAppkeyProfile();
    }
}
