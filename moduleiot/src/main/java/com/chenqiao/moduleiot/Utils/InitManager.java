package com.chenqiao.moduleiot.Utils;

import android.content.Context;
import android.util.Log;

import com.aliyun.alink.dm.api.DeviceInfo;
import com.aliyun.alink.dm.api.IoTApiClientConfig;
import com.aliyun.alink.linkkit.api.ILinkKitConnectListener;
import com.aliyun.alink.linkkit.api.IoTDMConfig;
import com.aliyun.alink.linkkit.api.IoTH2Config;
import com.aliyun.alink.linkkit.api.IoTMqttClientConfig;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linkkit.api.LinkKitInitParams;
import com.aliyun.alink.linksdk.cmp.connect.hubapi.HubApiRequest;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.id2.Id2ItlsSdk;
import com.aliyun.alink.linksdk.tmp.device.payload.ValueWrapper;
import com.aliyun.alink.linksdk.tools.AError;

import java.util.HashMap;
import java.util.Map;

/*
 * Copyright (c) 2014-2016 Alibaba Group. All rights reserved.
 * License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

public class InitManager {
    private static final String TAG = "InitManager";

    /**
     * 如果需要动态注册设备获取设备的deviceSecret， 可以参考本接口实现。
     * 动态注册条件检测：
     * 1.云端开启该设备动态注册功能；
     * 2.首先在云端创建 pk，dn；
     * @param context 上下文
     * @param productKey 产品类型
     * @param deviceName 设备名称 需要现在云端创建
     * @param productSecret 产品密钥
     * @param listener 密钥请求回调
     */
    public static void registerDevice(Context context, String productKey, String deviceName, String productSecret, IConnectSendListener listener) {
        DeviceInfo myDeviceInfo = new DeviceInfo();
        myDeviceInfo.productKey = productKey;
        myDeviceInfo.deviceName = deviceName;
        myDeviceInfo.productSecret = productSecret;
        LinkKitInitParams params = new LinkKitInitParams();
        params.connectConfig = new IoTApiClientConfig();
        // 如果明确需要切换域名，可以设置 connectConfig 中 domain 的值；
        params.deviceInfo = myDeviceInfo;
        HubApiRequest hubApiRequest = new HubApiRequest();
        hubApiRequest.path = "/auth/register/device";
        // 调用动态注册接口
        LinkKit.getInstance().deviceRegister(context, params, hubApiRequest, listener);
    }

    /**
     * Android 设备端 SDK 初始化示例代码
     * @param context 上下文
     * @param productKey 产品类型
     * @param deviceName 设备名称
     * @param deviceSecret 设备密钥
     * @param productSecret 产品密钥
     * @param callback 初始化建联结果回调
     */
    public static void init(Context context, String productKey, String deviceName, String deviceSecret, String productSecret, String region, final IDemoCallback callback) {
        // 构造三元组信息对象
        DeviceInfo deviceInfo = new DeviceInfo();
        // 产品类型
        deviceInfo.productKey = productKey;
        // 设备名称
        deviceInfo.deviceName = deviceName;
        // 设备密钥
        deviceInfo.deviceSecret = deviceSecret;
        // 产品密钥
        deviceInfo.productSecret = productSecret;
        //  全局默认域名
        IoTApiClientConfig userData = new IoTApiClientConfig();
        // 设备的一些初始化属性，可以根据云端的注册的属性来设置。
        /**
         * 物模型初始化的初始值
         * 如果这里什么属性都不填，物模型就没有当前设备相关属性的初始值。
         * 用户调用物模型上报接口之后，物模型会有相关数据缓存。
         * 用户根据时间情况设置
         */
        Map<String, ValueWrapper> propertyValues = new HashMap<>();
        // 示例
//        propertyValues.put("LightSwitch", new ValueWrapper.BooleanValueWrapper(0));

        LinkKitInitParams params = new LinkKitInitParams();
        params.deviceInfo = deviceInfo;
        params.propertyValues = propertyValues;
        params.connectConfig = userData;

        /**
         * 如果用户需要设置域名
         */
        IoTH2Config ioTH2Config = new IoTH2Config();
        ioTH2Config.clientId = "client-id";
        ioTH2Config.endPoint = "https://" + productKey + ioTH2Config.endPoint;// 线上环境
        params.iotH2InitParams = ioTH2Config;
        Id2ItlsSdk.init(context);
        /**
         * 慎用：如不清楚是否要设置，可以不设置该参数
         * Mqtt 相关参数设置
         * 域名、产品密钥、认证安全模式等；
         */
        IoTMqttClientConfig clientConfig = new IoTMqttClientConfig(productKey, deviceName, deviceSecret);
        // 对应 receiveOfflineMsg = !cleanSession, 默认不接受离线消息
//        clientConfig.receiveOfflineMsg = true;
        // 设置 mqtt 请求域名，默认"{pk}.iot-as-mqtt.cn-shanghai.aliyuncs.com:1883" ,如果无具体的业务需求，请不要设置。
        //clientConfig.channelHost = productKey + ".iot-as-mqtt." + region + ".aliyuncs.com:1883";
        // 文件配置测试 itls
        if ("itls_secret".equals(deviceSecret)){
            clientConfig.channelHost = productKey + ".itls." + region + ".aliyuncs.com:1883";//线上
            clientConfig.productSecret = productSecret;
            clientConfig.secureMode = 8;
        }
        params.mqttClientConfig = clientConfig;

        /**
         * 设备是否支持被飞燕平台APP发现
         * 需要确保开发的APP具备发现该类型设备的权限
         */
        IoTDMConfig ioTDMConfig = new IoTDMConfig();
        // 启用设备支持发现绑定的功能
        ioTDMConfig.enableNotify = true;
        params.ioTDMConfig = ioTDMConfig;

        /**
         * 设备初始化建联
         * onError 初始化建联失败，如果因网络问题导致初始化失败，需要用户重试初始化
         * onInitDone 初始化成功
         */
        LinkKit.getInstance().init(context, params, new ILinkKitConnectListener() {
            @Override
            public void onError(AError error) {
                Log.d(TAG, "onError() called with: error = [" + (error==null?"null":(error.getCode()+error.getMsg())) + "]");
                callback.onError(error);
            }

            @Override
            public void onInitDone(Object data) {
                Log.d(TAG, "onInitDone() called with: data = [" + data + "]");
                callback.onInitDone(data);
            }
        });
    }
}
