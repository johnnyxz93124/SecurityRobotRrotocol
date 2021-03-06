package com.robotleo.hardware.serial.connection;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPort {

	private static final String TAG = "SerialPort";

	/*
	 * Do not remove or rename the field mFd: it is used by native method close();
	 */
	private FileDescriptor mFd;
	private FileInputStream mFileInputStream;
	private FileOutputStream mFileOutputStream;

	public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {

		/* Check access permission */
		if (!device.canRead() || !device.canWrite()) {
			try {
				/* Missing read/write permission, trying to chmod the file */
//				Process su;
//				su = Runtime.getRuntime().exec("/system/bin/su");
//				String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
//						+ "exit\n";
//				su.getOutputStream().write(cmd.getBytes());
				Log.i(TAG, ""+device.getAbsolutePath() );
				String cmd = "chmod 777 "+device.getAbsolutePath() ;
				Process su = Runtime.getRuntime().exec("su"); // 切换到root帐号
				DataOutputStream os =  new DataOutputStream(su.getOutputStream());
				os.writeBytes(cmd + "\n");
				os.writeBytes("exit\n");
				os.flush();
				
				if ((su.waitFor() != 0) || !device.canRead()
						|| !device.canWrite()) {
					throw new SecurityException();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new SecurityException();
			}
		}

		mFd = open(device.getAbsolutePath(), baudrate, flags);
		if (mFd == null) {
			Log.e(TAG, "native open returns null");
			throw new IOException();
		}
		Log.e(TAG, "native open !!! null");
		mFileInputStream = new FileInputStream(mFd);
		mFileOutputStream = new FileOutputStream(mFd);
		frameSync();
	}
	/**
	 * 帧号同步
	 */
	public void frameSync(){
		//FC 23 01 00 0F 41 FE
		byte [] buf=new byte[7];
		buf[0]=(byte) 0xfc;
		buf[1]=0x23;
		buf[2]=0x01;
		buf[3]=0x00;
		buf[4]=0x0f;
		buf[5]=0x41;
		buf[6]=(byte) 0xfe;
		try {
			mFileOutputStream.write(buf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Getters and setters
	public InputStream getInputStream() {
		return mFileInputStream;
	}

	public OutputStream getOutputStream() {
		return mFileOutputStream;
	}

	// JNI
	private native static FileDescriptor open(String path, int baudrate, int flags);
	public native void close();
	static {
		System.loadLibrary("serial_port");
	}
}
