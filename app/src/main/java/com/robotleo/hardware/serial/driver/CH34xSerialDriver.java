package com.robotleo.hardware.serial.driver;

import android.annotation.SuppressLint;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CH34xSerialDriver implements UsbSerialDriver {

    private static final String TAG = CH34xSerialDriver.class.getSimpleName();

    private final UsbDevice mDevice;
    private final UsbSerialPort mPort;

    public CH34xSerialDriver(UsbDevice device) {
        mDevice = device;
        mPort = new CH34xSerialPort(mDevice, 0);
    }

    @Override
    public UsbDevice getDevice() {
        return mDevice;
    }

    @Override
    public List<UsbSerialPort> getPorts() {
        return Collections.singletonList(mPort);
    }

    public class CH34xSerialPort extends CommonUsbSerialPort {

        private static final int USB_WRITE_TIMEOUT_MILLIS = 1000;

        /*
         * Configuration Request Types
         */
        private static final int REQTYPE_HOST_TO_DEVICE = 0x40;

        /*
         * Configuration Request Codes
         */

        private static final int FLUSH_READ_CODE = 0x95;
        private static final int FLUSH_WRITE_CODE = 0x9A;


        private UsbEndpoint mReadEndpoint;
        private UsbEndpoint mWriteEndpoint;

        public CH34xSerialPort(UsbDevice device, int portNumber) {
            super(device, portNumber);
        }

        @Override
        public UsbSerialDriver getDriver() {
            return CH34xSerialDriver.this;

        }

        @SuppressLint("NewApi")
        private int setConfigSingle(int request, int value) {
            return mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, request, value,
                    0, null, 0, USB_WRITE_TIMEOUT_MILLIS);
        }

        @SuppressLint("NewApi")
        private int setConfigSingle_out(int request, int value, int index) {
            return mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, request, value,
                    index, null, 0, USB_WRITE_TIMEOUT_MILLIS);

        }

        @SuppressLint("NewApi")
        private int setConfigSingle_in(int request, int value, int index, byte[] buffer, int length) {
            return mConnection.controlTransfer(0xc0, request, value,
                    index, buffer, length, USB_WRITE_TIMEOUT_MILLIS);

        }

        @SuppressLint("NewApi")
        @Override
        public void open(UsbDeviceConnection connection) throws IOException {
            if (mConnection != null) {
                throw new IOException("Already opened.");
            }
            mConnection = connection;
            boolean opened = false;
            try {

                for (int i = 0; i < mDevice.getInterfaceCount(); i++) {
                    UsbInterface usbIface = mDevice.getInterface(i);

                    if (mConnection.claimInterface(usbIface, true)) {
                        Log.d(TAG, "claimInterface " + i + " SUCCESS");
                    } else {
                        Log.d(TAG, "claimInterface " + i + " FAIL");
                    }
                }

                UsbInterface dataIface = mDevice.getInterface(mDevice.getInterfaceCount() - 1);
                for (int i = 0; i < dataIface.getEndpointCount(); i++) {
                    UsbEndpoint ep = dataIface.getEndpoint(i);
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                            mReadEndpoint = ep;
                        } else {
                            mWriteEndpoint = ep;
                        }
                    }
                }

                int size = 8;
                byte[] buffer = new byte[size];

                setConfigSingle_out(0xa1, 0x00, 0x00);
                int ret = setConfigSingle_in(0x5f, 0, 0, buffer, 0x02);
                if (ret < 0) {
                    opened = false;
                }
                setConfigSingle_out(0x9a, 0x1321, 0xd982);
                setConfigSingle_out(0x9a, 0xf2c0, 0x04);
                int ret1 = setConfigSingle_in(0x95, 0x2518, 0, buffer, 0x02);
                if (ret1 < 0) {
                    opened = false;
                }
                setConfigSingle_out(0x9a, 0x2727, 0x00);
                setConfigSingle_out(0xa4, 0xff, 0x00);
                opened = true;


            } finally {
                if (!opened) {
                    try {
                        close();
                    } catch (IOException e) {
                        // Ignore IOExceptions during close()
                    }
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (mConnection == null) {
                throw new IOException("Already closed");
            }
            try {
                //setConfigSingle(SILABSER_IFC_ENABLE_REQUEST_CODE, UART_DISABLE);
                mConnection.close();
            } finally {
                mConnection = null;
            }
        }

        @SuppressLint("NewApi")
        @Override
        public int read(byte[] dest, int timeoutMillis) throws IOException {
            final int numBytesRead;
            synchronized (mReadBufferLock) {
                int readAmt = Math.min(dest.length, mReadBuffer.length);
                numBytesRead = mConnection.bulkTransfer(mReadEndpoint, mReadBuffer, readAmt,
                        timeoutMillis);
                if (numBytesRead < 0) {
                    // This sucks: we get -1 on timeout, not 0 as preferred.
                    // We *should* use UsbRequest, except it has a bug/api oversight
                    // where there is no way to determine the number of bytes read
                    // in response :\ -- http://b.android.com/28023
                    return 0;
                }
                System.arraycopy(mReadBuffer, 0, dest, 0, numBytesRead);
            }
            return numBytesRead;
        }

        @SuppressLint("NewApi")
        @Override
        public int write(byte[] src, int timeoutMillis) throws IOException {
            int offset = 0;

            while (offset < src.length) {
                final int writeLength;
                final int amtWritten;

                synchronized (mWriteBufferLock) {
                    final byte[] writeBuffer;

                    writeLength = Math.min(src.length - offset, mWriteBuffer.length);
                    if (offset == 0) {
                        writeBuffer = src;
                    } else {
                        // bulkTransfer does not support offsets, make a copy.
                        System.arraycopy(src, offset, mWriteBuffer, 0, writeLength);
                        writeBuffer = mWriteBuffer;
                    }

                    amtWritten = mConnection.bulkTransfer(mWriteEndpoint, writeBuffer, writeLength,
                            timeoutMillis);
                }
                if (amtWritten <= 0) {
                    throw new IOException("Error writing " + writeLength
                            + " bytes at offset " + offset + " length=" + src.length);
                }

                Log.d(TAG, "Wrote amt=" + amtWritten + " attempted=" + writeLength);
                offset += amtWritten;
            }
            return offset;
        }


        @Override
        public void setParameters(int baudRate, int dataBits, int stopBits, int parity)
                throws IOException {
            int value = 0;
            int index = 0;
            char valueHigh = 0, valueLow = 0, indexHigh = 0, indexLow = 0;
            switch (parity) {
                case 0: /*NONE*/
                    valueHigh = 0x00;
                    break;
                case 1: /*ODD*/
                    valueHigh |= 0x08;
                    break;
                case 2: /*Even*/
                    valueHigh |= 0x18;
                    break;
                case 3: /*Mark*/
                    valueHigh |= 0x28;
                    break;
                case 4: /*Space*/
                    valueHigh |= 0x38;
                    break;
                default:    /*None*/
                    valueHigh = 0x00;
                    break;
            }

            if (stopBits == 2) {
                valueHigh |= 0x04;
            }

            switch (dataBits) {
                case 5:
                    valueHigh |= 0x00;
                    break;
                case 6:
                    valueHigh |= 0x01;
                    break;
                case 7:
                    valueHigh |= 0x02;
                    break;
                case 8:
                    valueHigh |= 0x03;
                    break;
                default:
                    valueHigh |= 0x03;
                    break;
            }

            valueHigh |= 0xc0;
            valueLow = 0x9c;

            value |= valueLow;
            value |= valueHigh << 8;

            switch (baudRate) {
                case 50:
                    indexLow = 0;
                    indexHigh = 0x16;
                    break;
                case 75:
                    indexLow = 0;
                    indexHigh = 0x64;
                    break;
                case 110:
                    indexLow = 0;
                    indexHigh = 0x96;
                    break;
                case 135:
                    indexLow = 0;
                    indexHigh = 0xa9;
                    break;
                case 150:
                    indexLow = 0;
                    indexHigh = 0xb2;
                    break;
                case 300:
                    indexLow = 0;
                    indexHigh = 0xd9;
                    break;
                case 600:
                    indexLow = 1;
                    indexHigh = 0x64;
                    break;
                case 1200:
                    indexLow = 1;
                    indexHigh = 0xb2;
                    break;
                case 1800:
                    indexLow = 1;
                    indexHigh = 0xcc;
                    break;
                case 2400:
                    indexLow = 1;
                    indexHigh = 0xd9;
                    break;
                case 4800:
                    indexLow = 2;
                    indexHigh = 0x64;
                    break;
                case 9600:
                    indexLow = 2;
                    indexHigh = 0xb2;
                    break;
                case 19200:
                    indexLow = 2;
                    indexHigh = 0xd9;
                    break;
                case 38400:
                    indexLow = 3;
                    indexHigh = 0x64;
                    break;
                case 57600:
                    indexLow = 3;
                    indexHigh = 0x98;
                    break;
                case 115200:
                    indexLow = 3;
                    indexHigh = 0xcc;
                    break;
                case 230400:
                    indexLow = 3;
                    indexHigh = 0xe6;
                    break;
                case 460800:
                    indexLow = 3;
                    indexHigh = 0xf3;
                    break;
                case 500000:
                    indexLow = 3;
                    indexHigh = 0xf4;
                    break;
                case 921600:
                    indexLow = 7;
                    indexHigh = 0xf3;
                    break;
                case 1000000:
                    indexLow = 3;
                    indexHigh = 0xfa;
                    break;
                case 2000000:
                    indexLow = 3;
                    indexHigh = 0xfd;
                    break;
                case 3000000:
                    indexLow = 3;
                    indexHigh = 0xfe;
                    break;
                default:    // default baudRate "9600"
                    indexLow = 2;
                    indexHigh = 0xb2;
                    break;
            }

            index |= 0x88 | indexLow;
            index |= indexHigh << 8;
            Log.i(TAG, "value=" + value + "  index" + index);
            setConfigSingle_out(0xa1, value, index);

        }

        @Override
        public boolean getCD() throws IOException {
            return false;
        }

        @Override
        public boolean getCTS() throws IOException {
            return false;
        }

        @Override
        public boolean getDSR() throws IOException {
            return false;
        }

        @Override
        public boolean getDTR() throws IOException {
            return true;
        }

        @Override
        public void setDTR(boolean value) throws IOException {
        }

        @Override
        public boolean getRI() throws IOException {
            return false;
        }

        @Override
        public boolean getRTS() throws IOException {
            return true;
        }

        @Override
        public void setRTS(boolean value) throws IOException {
        }

        @Override
        public boolean purgeHwBuffers(boolean purgeReadBuffers,
                                      boolean purgeWriteBuffers) throws IOException {

            if (purgeReadBuffers) {
                setConfigSingle(FLUSH_READ_CODE, 0);
            }

            if (purgeWriteBuffers) {
                setConfigSingle(FLUSH_WRITE_CODE, 0);
            }

            return purgeReadBuffers || purgeWriteBuffers;
        }

    }

    public static Map<Integer, int[]> getSupportedDevices() {
        final Map<Integer, int[]> supportedDevices = new LinkedHashMap<Integer, int[]>();
        supportedDevices.put(Integer.valueOf(UsbId.VENDOR_WCH),
                new int[]{

                        UsbId.CH340,
                        UsbId.CH341

                });
        return supportedDevices;
    }
}
