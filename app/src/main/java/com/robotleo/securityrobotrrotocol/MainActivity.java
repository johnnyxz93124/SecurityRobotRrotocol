package com.robotleo.securityrobotrrotocol;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.robotleo.hardware.serial.util.HexDump;
import com.robotleo.hardware.serial.util.RobotUsbManager;
import com.robotleo.pad.R;

public class MainActivity extends AppCompatActivity implements RobotUsbManager.OnUsbDataListener {
    private Button buttonSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        init();
    }

    private void initView() {
        buttonSend = findViewById(R.id.button);

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                00 00 01 01
//                02 00 c4 c6
//                06 00 88 8e
                byte[] buf = new byte[12];
                buf[2] = 0x01;
                buf[3] = 0x01;
                buf[4] = 0x02;
                buf[6] = (byte) 0xc4;
                buf[7] = (byte) 0xc6;
                buf[8] = (byte) 0x06;
                buf[10] = (byte) 0x88;
                buf[11] = (byte) 0x8e;

                try {
                    RobotUsbManager.getInstance().sendRobotMessage(buf);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] buf = new byte[12];
                buf[2] = 0x01;
                buf[3] = 0x01;
                buf[4] = 0x02;
                buf[6] = (byte) 0x22;
                buf[7] = (byte) 0x22;
                buf[8] = (byte) 0x22;
                buf[10] = (byte) 0x22;
                buf[11] = (byte) 0x22;

            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] buf = new byte[12];
                buf[2] = 0x01;
                buf[3] = 0x01;
                buf[4] = 0x02;
                buf[6] = (byte) 0x33;
                buf[7] = (byte) 0x33;
                buf[8] = (byte) 0x33;
                buf[10] = (byte) 0x33;
                buf[11] = (byte) 0x33;
            }
        });

    }


    private void init() {
//        UsbDevice device = (UsbDevice) getIntent().getParcelableExtra("device");
//        if (device != null) {
//            Toast.makeText(this, "USB连接成功!"+device.getProductId()+" "+device.getVendorId(), Toast.LENGTH_SHORT).show();
//        }

        RobotUsbManager.getInstance().init(this);
        RobotUsbManager.getInstance().startUsbContion();
//        RobotUsbManager.getInstance().setOnUsbDataListener(this);
//        search();
    }

    private UsbManager mUsbManager;

    private void search() {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (final UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {
                    Log.i("search", "usbDevice=" + usbDevice.getInterfaceCount());
                }
            }
        }).start();
    }

    @Override
    public void onUsbData(int type, byte[] data) {
        String log = HexDump.toHexString(data);
        Log.i("usbdata", log);
    }
}
