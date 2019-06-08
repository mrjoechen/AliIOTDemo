package com.chenqiao.moduleiot.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.aliyun.alink.dm.model.RequestModel;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linksdk.cmp.core.base.AMessage;
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener;
import com.aliyun.alink.linksdk.tmp.device.payload.ValueWrapper;
import com.aliyun.alink.linksdk.tmp.listener.IPublishResourceListener;
import com.aliyun.alink.linksdk.tools.AError;
import com.aliyun.alink.linksdk.tools.ALog;
import com.chenqiao.moduleiot.R;

import java.io.UnsupportedEncodingException;
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

/**
 * 注意！！！！
 * 1.该示例只共快速接入使用，只适用于有 Status、Data属性的快速接入测试设备；
 * 2.真实设备可以参考 ControlPanelActivity 里面有数据上下行示例；
 */
public class LightExampleActivity extends BaseActivity {

    private final static int REPORT_MSG = 0x100;

    private static String UPLOAD_MSG = "test_msg";

    private static int COUNT;

    TextView consoleTV;
    String consoleStr;
    private InternalHandler mHandler = new InternalHandler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 在初始化的时候可以设置 灯的初始状态，或者等初始化完成之后 上报一次设备所有属性的状态
        // 注意在调用云端接口之前确保初始化完成了
        setContentView(R.layout.activity_light_example);
        consoleTV = (TextView) findViewById(R.id.textview_console);
        setDownStreamListener();
        showToast("已启动每5秒上报一次状态");
        log("已启动每5秒上报一次状态");
        mHandler.sendEmptyMessageDelayed(REPORT_MSG, 2 * 1000);
    }


    String msg = "";
    /**
     * 数据上行
     * 上报灯的状态
     */
    public void reportHelloWorld() {


//        UPLOAD_MSG = UPLOAD_MSG + "-" + COUNT;

        msg = UPLOAD_MSG + "-" + COUNT;
        log("上报 " + msg);
        COUNT ++;
        try {
            Map<String, ValueWrapper> reportData = new HashMap<>();
            reportData.put("Status", new ValueWrapper.BooleanValueWrapper(1)); // 1开 0 关
            reportData.put("Data", new ValueWrapper.StringValueWrapper(msg)); //
            LinkKit.getInstance().getDeviceThing().thingPropertyPost(reportData, new IPublishResourceListener() {
                @Override
                public void onSuccess(String s, Object o) {
                    Log.d(TAG, "onSuccess() called with: s = [" + s + "], o = [" + o + "]");
                    showToast("设备上报状态成功");
                    log("上报 "+msg+"成功。");
                }

                @Override
                public void onError(String s, AError aError) {
                    Log.d(TAG, "onError() called with: s = [" + s + "], aError = [" + aError + "]");
                    showToast("设备上报状态失败");
                    log("上报 "+msg+"失败。");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDownStreamListener(){
        LinkKit.getInstance().registerOnPushListener(notifyListener);
    }

    private IConnectNotifyListener notifyListener = new IConnectNotifyListener() {
        @Override
        public void onNotify(String s, String s1, AMessage aMessage) {
            try {
                if (s1 != null && s1.contains("service/property/set")) {
                    String result = new String((byte[]) aMessage.data, "UTF-8");
                    RequestModel<String> receiveObj = JSONObject.parseObject(result, new TypeReference<RequestModel<String>>() {
                    }.getType());
                    log("Received a message: " + (receiveObj==null?"":receiveObj.params));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public boolean shouldHandle(String s, String s1) {
            Log.d(TAG, "shouldHandle() called with: s = [" + s + "], s1 = [" + s1 + "]");
            return true;
        }

        @Override
        public void onConnectStateChange(String s, ConnectState connectState) {
            Log.d(TAG, "onConnectStateChange() called with: s = [" + s + "], connectState = [" + connectState + "]");
        }
    };

    private void log(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ALog.d(TAG, "log(), " + str);
                if (TextUtils.isEmpty(str))
                    return;
                consoleStr = consoleStr + "\n \n" + (getTime()) + " " + str;
                consoleTV.setText(consoleStr);
            }
        });

    }

    private void clearMsg() {
        consoleStr = "";
        consoleTV.setText(consoleStr);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mHandler != null) {
            mHandler.removeMessages(REPORT_MSG);
            mHandler.removeCallbacksAndMessages(null);
            showToast("停止定时上报");
        }
        LinkKit.getInstance().unRegisterOnPushListener(notifyListener);
        clearMsg();
    }

    private class InternalHandler extends Handler {
        public InternalHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg == null) {
                return;
            }
            int what = msg.what;
            switch (what) {
                case REPORT_MSG:
                    reportHelloWorld();
                    mHandler.sendEmptyMessageDelayed(REPORT_MSG, 5*1000);
                    break;
            }

        }
    }

}

