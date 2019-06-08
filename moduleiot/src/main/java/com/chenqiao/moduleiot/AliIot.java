package com.chenqiao.moduleiot;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.aliyun.alink.dm.model.ResponseModel;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linksdk.cmp.core.base.AMessage;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.tmp.device.payload.ValueWrapper;
import com.aliyun.alink.linksdk.tmp.listener.IPublishResourceListener;
import com.aliyun.alink.linksdk.tools.AError;
import com.aliyun.alink.linksdk.tools.ALog;
import com.chenqiao.moduleiot.Utils.Constant;
import com.chenqiao.moduleiot.Utils.FileUtils;
import com.chenqiao.moduleiot.Utils.IDemoCallback;
import com.chenqiao.moduleiot.Utils.InitManager;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.aliyun.alink.linksdk.tools.ThreadTools.runOnUiThread;

/**
 * Created by chenqiao on 2019/5/27.
 * e-mail : mrjctech@gmail.com
 */
public class AliIot {

    private static final String TAG = "AliIot";

    public static String productKey = "", deviceName = "", deviceSecret = "", productSecret = "", region="";
    private boolean isInitDone;

    private Context mContext;


    private static class Holder{
        private static final AliIot Instance = new AliIot();
    }

    public static AliIot getInstance(){
        return Holder.Instance;
    }

    public void init(@NonNull Context context,@NonNull  String pk,@NonNull  String ps,@NonNull  String dn, String ds, String reg){
        mContext = context;

        productKey = pk;
        productSecret = ps;
        deviceName = dn;
        deviceSecret = ds;
        region = reg;

        if (TextUtils.isEmpty(productKey)){
            throw new RuntimeException("productKey is empty!");
        }

        if (TextUtils.isEmpty(productSecret)){
            throw new RuntimeException("productSecret is empty!");
        }

        if (TextUtils.isEmpty(deviceName)){
            throw new RuntimeException("deviceName is empty!");
        }

        if (TextUtils.isEmpty(region)){
            region = "cn-shanghai";
        }

        if (TextUtils.isEmpty(deviceSecret)) {
            tryGetFromFile();
        }

        if (TextUtils.isEmpty(deviceSecret)) {
            InitManager.registerDevice(mContext, productKey, deviceName, productSecret, new IConnectSendListener() {
                @Override
                public void onResponse(ARequest aRequest, AResponse aResponse) {
                    Log.d(TAG, "onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + (aResponse == null ? "null" : aResponse.data) + "]");
                    if (aResponse != null && aResponse.data != null) {
                        // 解析云端返回的数据
                        ResponseModel<Map<String, String>> response = JSONObject.parseObject(aResponse.data.toString(),
                                new TypeReference<ResponseModel<Map<String, String>>>() {
                                }.getType());
                        if ("200".equals(response.code) && response.data != null && response.data.containsKey("deviceSecret") &&
                                !TextUtils.isEmpty(response.data.get("deviceSecret"))) {
                            deviceSecret = response.data.get("deviceSecret");
                            // getDeviceSecret success, to build connection.
                            // 持久化 deviceSecret 初始化建联的时候需要
                            // 用户需要按照实际场景持久化设备的三元组信息，用于后续的连接
//                            SharedPreferences preferences = mContext.getSharedPreferences("deviceAuthInfo", 0);
//                            SharedPreferences.Editor editor = preferences.edit();
//                            editor.putString("deviceId", productKey+deviceName);
//                            editor.putString("deviceSecret", deviceSecret);
//                            //提交当前数据
//                            editor.commit();

                            JsonObject jsonObject = new JsonObject();
                            jsonObject.addProperty("deviceId", productKey+deviceName);
                            jsonObject.addProperty("deviceSecret", deviceSecret);
                            File file = new File(Constant.DISK_FILE);
                            FileUtils.writeToFile(file, jsonObject.toString());

                            connect();
                        }
                    }
                }

                @Override
                public void onFailure(ARequest aRequest, AError aError) {
                    Log.d(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError + "]");
                }
            });
        } else {
            connect();
        }

        if (reportThread != null && !reportThread.isAlive()){
            reportThread.start();
            reportHandler = new Handler(reportThread.getLooper());
        }
    }

    private void connect() {
        Log.d(TAG, "connect() called");
        // SDK初始化
        InitManager.init(mContext, productKey, deviceName, deviceSecret, productSecret, region, new IDemoCallback() {
            @Override
            public void onNotify(String s, String s1, AMessage aMessage) {
                Log.d(TAG, "onNotify() called with: s = [" + s + "], s1 = [" + s1 + "], aMessage = [" + aMessage + "]");
            }

            @Override
            public boolean shouldHandle(String s, String s1) {
                Log.d(TAG, "shouldHandle() called with: s = [" + s + "], s1 = [" + s1 + "]");
                return true;
            }

            @Override
            public void onConnectStateChange(String s, ConnectState connectState) {
                Log.d(TAG, "onConnectStateChange() called with: s = [" + s + "], connectState = [" + connectState + "]");
                if (connectState == ConnectState.CONNECTED) {
                    Log.d(TAG,"长链接已建联");
                } else if (connectState == ConnectState.CONNECTFAIL) {
                    Log.d(TAG,"长链接建联失败");
                } else if (connectState == ConnectState.DISCONNECTED) {
                    Log.d(TAG,"长链接已断连");
                }
            }

            @Override
            public void onError(AError aError) {
                Log.d(TAG, "onError() called with: aError = [" + aError + "]");
                Log.d(TAG,"初始化失败");
            }

            @Override
            public void onInitDone(Object data) {
                Log.d(TAG, "onInitDone() called with: data = [" + data + "]");
                Log.d(TAG,"初始化成功");
                isInitDone = true;
            }
        });
    }

    private void tryGetFromFile() {
        Log.d(TAG, "tryGetFromFile() called");
//        SharedPreferences authInfo = mContext.getSharedPreferences("deviceAuthInfo", Activity.MODE_PRIVATE);
//        String pkDn = authInfo.getString("deviceId", null);
//        String ds = authInfo.getString("deviceSecret", null);
//        if (pkDn != null && pkDn.equals(productKey + deviceName) && ds != null) {
//            Log.d(TAG, "tryGetFromSP update ds from sp.");
//            deviceSecret = ds;
//        }


        String s = FileUtils.readTextFromFile(Constant.DISK_FILE);
        JSONObject jsonObject = JSONObject.parseObject(s);
        if (jsonObject == null) return;
        String pkDn = jsonObject.getString("deviceId");
        String ds = jsonObject.getString("deviceSecret");

        if (pkDn != null && pkDn.equals(productKey + deviceName) && ds != null) {
            Log.d(TAG, "tryGetFromFile update ds from file.");
            deviceSecret = ds;
        }
    }

    public String getDeviceSecret(){
        return deviceSecret;
    }



    private HandlerThread reportThread = new HandlerThread("reportHandler");

    private Handler reportHandler;


    /**
     * 数据上行
     * 上报灯的状态
     */
    public void reportLog(final String loooog) {

        if (isInitDone)
            reportHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Map<String, ValueWrapper> reportData = new HashMap<>();
                        reportData.put("Status", new ValueWrapper.BooleanValueWrapper(1)); // 1开 0 关
                        reportData.put("Data", new ValueWrapper.StringValueWrapper(loooog)); //
                        LinkKit.getInstance().getDeviceThing().thingPropertyPost(reportData, new IPublishResourceListener() {
                            @Override
                            public void onSuccess(String s, Object o) {
                                Log.d(TAG, "onSuccess() called with: s = [" + s + "], o = [" + o + "]");
                            }

                            @Override
                            public void onError(String s, AError aError) {
                                Log.d(TAG, "onError() called with: s = [" + s + "], aError = [" + aError + "]");
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

    }



}

