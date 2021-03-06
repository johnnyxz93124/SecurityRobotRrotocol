package com.robotleo.hardware.serial.connection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class RobotSerialConnection extends SerialConnection{

	private String mDevicePath;
	private int mBaudrate;
	private SerialPort mPort;
	private InputStream mInputStream;
	private OutputStream mOutputStream;
	
	
	private transient static SerialConnection mConnection;
	
	public static SerialConnection getInstance() {
		synchronized (RobotSerialConnection.class) {
			if (mConnection == null) {
				mConnection = new RobotSerialConnection();
				mConnection.start();
			}
			return mConnection;
		}
	}

	private RobotSerialConnection(){
		mDevicePath = "/dev/ttyMT2";
		mBaudrate = 115200;
		mReadBufferSize = 0;
	}
	
	@Override
	protected void openConnection() {
			if(mPort == null){
				try {
					mPort = new SerialPort(new File(mDevicePath), mBaudrate, 0);
					mInputStream = mPort.getInputStream();
					mOutputStream = mPort.getOutputStream();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	}

	@Override
	protected void readDataBlock() {
		if (mInputStream != null) {
			try {
				mReadBufferSize = mInputStream.read(mReadBuffer.array());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void sendBuffer(byte[] buffer) {
		if (mInputStream != null) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void closeConnection() {
			try {
				if (mInputStream != null) {
					mInputStream.close();
					mInputStream=null;
				}
				if (mOutputStream != null) {
					mOutputStream.close();
					mOutputStream=null;
				}
				if (mPort != null) {
					mPort.close();
					mPort = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
}
