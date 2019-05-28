package com.chenqiao.moduleiot;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.aliyun.alink.dm.model.ResponseModel;
import com.aliyun.alink.linksdk.cmp.core.base.AMessage;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.tools.AError;
import com.chenqiao.moduleiot.Utils.IDemoCallback;
import com.chenqiao.moduleiot.Utils.InitManager;

import java.util.Map;

public class AliIOTInitializer {


    private static final String TAG = "AliIot";

    public static String productKey = "", deviceName = "", deviceSecret = "", productSecret = "", region="";
    private boolean isInitDone;

    private Context mContext;

    private static class Holder{
        public static final AliIOTInitializer Instance = new AliIOTInitializer();
    }

    public static AliIOTInitializer getInstance(){
        return Holder.Instance;
    }


    private void init(Context context, String pk, String ps, String dn, String ds, String reg){

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
            tryGetFromSP();
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
                            SharedPreferences preferences = mContext.getSharedPreferences("deviceAuthInfo", 0);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("deviceId", productKey+deviceName);
                            editor.putString("deviceSecret", deviceSecret);
                            //提交当前数据
                            editor.commit();
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

    private void tryGetFromSP() {
        Log.d(TAG, "tryGetFromSP() called");
        SharedPreferences authInfo = mContext.getSharedPreferences("deviceAuthInfo", Activity.MODE_PRIVATE);
        String pkDn = authInfo.getString("deviceId", null);
        String ds = authInfo.getString("deviceSecret", null);
        if (pkDn != null && pkDn.equals(productKey + deviceName) && ds != null) {
            Log.d(TAG, "tryGetFromSP update ds from sp.");
            deviceSecret = ds;
        }
    }

    public String getDeviceSecret(){
        return deviceSecret;
    }


}
