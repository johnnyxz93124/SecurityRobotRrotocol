package com.robotleo.hardware.serial.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.util.Log;

import com.robotleo.hardware.serial.driver.UsbSerialDriver;
import com.robotleo.hardware.serial.driver.UsbSerialPort;
import com.robotleo.hardware.serial.driver.UsbSerialProber;

import java.io.IOException;
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

    private static RobotUsbManager mRobotManger;
    private Context mContext;
    private UsbManager mUsbManager;

    public OnUsbDataListener mDataListener;
    private final ExecutorService mExecutor = Executors.newFixedThreadPool(4);
    private List<SerialInputOutputManager> sinputs = new ArrayList<SerialInputOutputManager>();

    private RobotUsbManager() {

    }

    public static synchronized RobotUsbManager getInstance() {
        if (mRobotManger == null) {
            mRobotManger = new RobotUsbManager();
        }
        return mRobotManger;
    }

    public void init(Context context) {
        mUsbManager = (UsbManager) context
                .getSystemService(Context.USB_SERVICE);
    }


    public void startUsbContion() {
        new AsyncTask<Void, Void, List<UsbSerialDriver>>() {
            @Override
            protected List<UsbSerialDriver> doInBackground(Void... params) {

                final List<UsbSerialDriver> drivers = UsbSerialProber
                        .getDefaultProber().findAllDrivers(mUsbManager);

                return drivers;
            }

            @SuppressLint("NewApi")
            @Override
            protected void onPostExecute(List<UsbSerialDriver> results) {

                Log.i(TAG, "results=" + results.size() + "  " + results.get(0).getPorts().size());

                for (int i = 0; i < results.size(); i++) {
                    isPort(results.get(i).getPorts().get(0));
                }
            }
        }.execute((Void) null);
    }


    @SuppressLint("NewApi")
    public void isPort(UsbSerialPort prPort) {
        try {
            UsbDeviceConnection connection = mUsbManager.openDevice(prPort
                    .getDriver().getDevice());
            prPort.open(connection);
            prPort.setParameters(57600, 8, UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE);

            SerialInputOutputManager sMage = new SerialInputOutputManager(prPort, null);
            mExecutor.submit(sMage);
            sinputs.add(sMage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 向底盘发送消�?
     *
     * @param data 数据�?
     */
    public void sendRobotMessage(byte[] data) {
        try {
            for (int i = 0; i < sinputs.size(); i++) {
                sinputs.get(i).writeAsync(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
