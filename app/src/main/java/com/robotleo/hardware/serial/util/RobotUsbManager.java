package com.robotleo.hardware.serial.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.robotleo.hardware.serial.driver.UsbSerialDriver;
import com.robotleo.hardware.serial.driver.UsbSerialPort;
import com.robotleo.hardware.serial.driver.UsbSerialProber;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class RobotUsbManager {
    public static interface OnUsbDataListener {
        /**
         * USB数据监听
         *
         * @param type 设备类型 0-小脑  1-语音  2-雷达
         * @param data 设备数据
         */
        public void onUsbData(int type, byte[] data);
    }

    private final String TAG = "RobotUsbManager";
    /* 数据类型为小�?*/
    public static final int DATA_TYPE_CEREBELLUM = 0;
    /* 数据类型为语�?*/
    public static final int DATA_TYPE_VOICE = 1;
    /* 数据类型为雷�?*/
    public static final int DATA_TYPE_RADAR = 2;

    /* 底盘连接是否成功 */
    public static boolean HARDWARE_CONNECTION_CHASSIS = true;
    /* 是否连接雷达 */
    public static boolean HARDWARE_CONNECTION_READAR = true;

    private static RobotUsbManager mRobotManger;
    private Context mContext;
    private UsbManager mUsbManager;

    /* 小脑通信接口 */
    private SerialInputOutputManager mSerialIoManager;
    /* 语音通信接口 */
    private SerialInputOutputManager mVoieSerialIoManager;
    /* 雷达通信接口 */
    private SerialInputOutputManager mRadarSeriaIoManager;

    public OnUsbDataListener mDataListener;
    private final ExecutorService mExecutor = Executors.newFixedThreadPool(4);
    private MyHandler mHandler;

    /**
     * 注册监听
     */
    public void setOnUsbDataListener(OnUsbDataListener mDataListener) {
        if (mDataListener != null) {
            this.mDataListener = mDataListener;
        }
    }

    // 小脑数据上报
    private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {
        public void onRunError(Exception e) {
        }

        public void onNewData(final byte[] data) {
            Log.i(TAG, "mListener" + data.length);
            mHandler.sendMessage(mHandler.obtainMessage(DATA_TYPE_CEREBELLUM, data));
            //mDataListener.onUsbData(DATA_TYPE_CEREBELLUM, data);
        }
    };
    // 语音数据上报
    private final SerialInputOutputManager.Listener mVoieListener = new SerialInputOutputManager.Listener() {
        public void onRunError(Exception e) {
        }

        public void onNewData(final byte[] data) {
            Log.i(TAG, "mVoieListener" + data.length);
            mHandler.sendMessage(mHandler.obtainMessage(DATA_TYPE_VOICE, data));
        }
    };

    // 雷达数据上报
    private final SerialInputOutputManager.Listener mRadarListener = new SerialInputOutputManager.Listener() {
        public void onRunError(Exception e) {
        }

        public void onNewData(final byte[] data) {
            Log.i(TAG, "mRadarListener" + data.length);
            mHandler.sendMessage(mHandler.obtainMessage(DATA_TYPE_RADAR, data));
        }
    };

    public static class MyHandler extends Handler {
        WeakReference<RobotUsbManager> mSensorManager;

        public MyHandler(RobotUsbManager usbManager) {
            mSensorManager = new WeakReference<RobotUsbManager>(usbManager);
        }

        public void handleMessage(Message msg) {
            RobotUsbManager usbManager = mSensorManager.get();

            switch (msg.what) {
                case DATA_TYPE_CEREBELLUM:
                    usbManager.mDataListener.onUsbData(DATA_TYPE_CEREBELLUM, (byte[]) msg.obj);
                    break;
                case DATA_TYPE_VOICE:
                    usbManager.mDataListener.onUsbData(DATA_TYPE_VOICE, (byte[]) msg.obj);
                    break;
                case DATA_TYPE_RADAR:
                    usbManager.mDataListener.onUsbData(DATA_TYPE_RADAR, (byte[]) msg.obj);
                    break;
            }
        }
    }

    private RobotUsbManager() {

    }

    public static synchronized RobotUsbManager getInstance() {
        if (mRobotManger == null) {
            mRobotManger = new RobotUsbManager();
        }
        return mRobotManger;
    }

    public void init(Context context) {
        this.mContext = context;
        initMember();
    }

    /*
     * 初始化成员变�?
     */
    public void initMember() {
        mUsbManager = (UsbManager) mContext
                .getSystemService(Context.USB_SERVICE);
        mHandler = new MyHandler(this);
    }

    // �?��usb 连接
    public void startUsbContion() {
        // 檢測usb设备
        refreshDeviceList();
    }

    private void refreshDeviceList() {
        new AsyncTask<Void, Void, List<UsbSerialPort>>() {
            protected List<UsbSerialPort> doInBackground(Void... params) {
                if (mUsbManager == null) {
                    return null;
                }
                final List<UsbSerialDriver> drivers = UsbSerialProber
                        .getDefaultProber().findAllDrivers(mUsbManager);
                final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();

                for (final UsbSerialDriver driver : drivers) {
                    final List<UsbSerialPort> ports = driver.getPorts();
                    result.addAll(ports);
                }
                return result;
            }

            @SuppressLint("NewApi")
            protected void onPostExecute(List<UsbSerialPort> results) {

                if (results != null && results.size() > 0) {
                    UsbSerialPort sPort = null;// 小脑端口
                    UsbSerialPort mVoiePort = null;// 语音端口
                    UsbSerialPort mRadarPort = null;// 雷达数据端口
                    if (results.size() == 1) {// 只有�?��设备
                        sPort = results.get(0);
                        isPort(sPort, (byte) 1);
                    } else if (results.size() == 2) {// 两个设备4

                        int deviceId1 = results.get(0).getDriver().getDevice()
                                .getDeviceId();
                        int deviceId2 = results.get(1).getDriver().getDevice()
                                .getDeviceId();
                        Toast.makeText(
                                mContext,
                                "deviceId1=" + deviceId1 + "  deviceId2="
                                        + deviceId2, Toast.LENGTH_LONG).show();
                        if (deviceId1 < deviceId2) {
                            sPort = results.get(0);
                            isPort(sPort, (byte) 1);
                            // 语音模块连接
                            mVoiePort = results.get(1);
                            isPort(mVoiePort, (byte) 2);
                        } else {
                            sPort = results.get(1);
                            isPort(sPort, (byte) 1);
                            // 语音模块连接
                            mVoiePort = results.get(0);
                            isPort(mVoiePort, (byte) 2);
                        }

                    } else if (results.size() == 3) {// 三个外接设备
                        int[] array = new int[3];
                        array[0] = results.get(0).getDriver().getDevice()
                                .getDeviceId();
                        array[1] = results.get(1).getDriver().getDevice()
                                .getDeviceId();
                        array[2] = results.get(2).getDriver().getDevice()
                                .getDeviceId();

                        int[] arrayDup = new int[3];
                        for (int i = 0; i < arrayDup.length; i++) {
                            arrayDup[i] = array[i];
                        }

                        arraySequence(array);

                        for (int i = 0; i < array.length; i++) {
                            if (array[0] == arrayDup[i]) {// 小脑
                                // 小脑端口
                                sPort = results.get(i);
                                isPort(sPort, (byte) 1);
                            }
                            if (array[1] == arrayDup[i]) {// 语音
                                // 语音模块连接
                                mVoiePort = results.get(i);
                                isPort(mVoiePort, (byte) 2);
                            }
                            if (array[2] == arrayDup[i]) {// 雷达
                                // 雷达端口
                                mRadarPort = results.get(i);
                                isPort(mRadarPort, (byte) 3);
                            }
                        }
                    }
                } else {
                    Toast.makeText(mContext, "not devices", Toast.LENGTH_LONG).show();
                    stopIoManager((byte) 1);
                    stopIoManager((byte) 2);
                    stopIoManager((byte) 3);
                }

            }
        }.execute((Void) null);
    }

    // 串口排序
    public void arraySequence(int[] array) {
        int temp;
        for (int i = 0; i < array.length; i++) {// 趟数
            for (int j = 0; j < array.length - i - 1; j++) {// 比较次数
                if (array[j] > array[j + 1]) {
                    temp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = temp;
                }
            }
        }
    }

    @SuppressLint("NewApi")
    public void isPort(UsbSerialPort prPort, byte mark) {
        if (prPort == null) {
        } else {
            UsbDeviceConnection connection = mUsbManager.openDevice(prPort
                    .getDriver().getDevice());
            if (connection == null) {
                return;
            }
            Log.i(TAG, "connection");
            try {
                prPort.open(connection);
                Log.i(TAG, "setParameters");
                prPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1,
                        UsbSerialPort.PARITY_NONE);
                Log.i(TAG, "after----setParameters");
            } catch (IOException e) {
                try {
                    prPort.close();
                } catch (IOException e2) {
                }
                prPort = null;
                return;
            }
        }
        Log.i(TAG, "onDeviceStateChange");
        onDeviceStateChange(prPort, mark);
    }

    private void onDeviceStateChange(UsbSerialPort prPort, byte mark) {
        stopIoManager(mark);
        startIoManager(prPort, mark);
    }

    public void stopIoManager(byte mark) {
        if (mSerialIoManager != null && mark == 1) {
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }

        if (mVoieSerialIoManager != null && mark == 2) {
            mVoieSerialIoManager.stop();
            mVoieSerialIoManager = null;
        }

        if (mRadarSeriaIoManager != null && mark == 3) {
            mRadarSeriaIoManager.stop();
            mRadarSeriaIoManager = null;
        }

    }

    private void startIoManager(UsbSerialPort prPort, byte mark) {
        if (prPort != null && mark == 1) {
            Toast.makeText(mContext, "connected mListener", Toast.LENGTH_LONG)
                    .show();
            mSerialIoManager = new SerialInputOutputManager(prPort, mListener);
            mExecutor.submit(mSerialIoManager);
            frameSync();
        }

        if (prPort != null && mark == 2) {
            Toast.makeText(mContext, "connected mVoieListener",
                    Toast.LENGTH_LONG).show();
            mVoieSerialIoManager = new SerialInputOutputManager(prPort,
                    mVoieListener);
            mExecutor.submit(mVoieSerialIoManager);
        }

        if (prPort != null && mark == 3) {
            Toast.makeText(mContext, "connected mRadarListener",
                    Toast.LENGTH_LONG).show();
            mRadarSeriaIoManager = new SerialInputOutputManager(prPort,
                    mRadarListener);
            mExecutor.submit(mRadarSeriaIoManager);
            try {
                mRadarSeriaIoManager.writeAsync(range());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            HARDWARE_CONNECTION_READAR = false;
        }
    }

    /**
     * 帧号同步
     */
    public void frameSync() {
        //FC 23 01 00 0F 41 FE
        byte[] buf = new byte[7];
        buf[0] = (byte) 0xfc;
        buf[1] = 0x23;
        buf[2] = 0x01;
        buf[3] = 0x00;
        buf[4] = 0x0f;
        buf[5] = 0x41;
        buf[6] = (byte) 0xfe;
        try {
            SerialInputOutputManager.writeAsync(buf);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private byte[] range() {
        byte[] buffer = new byte[2];
        buffer[0] = (byte) 0xa5;
        buffer[1] = 0x20;
        return buffer;
    }

    /**
     * 向底盘发送消�?
     *
     * @param data 数据�?
     */
    public void sendRobotMessage(byte[] data) {
        if (mSerialIoManager != null && data.length > 0) {
            try {
                mSerialIoManager.writeAsync(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
