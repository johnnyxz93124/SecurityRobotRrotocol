package com.robotleo.hardware.serial.connection;

import java.nio.ByteBuffer;

public abstract class SerialConnection extends Thread {

	protected abstract void openConnection();

	protected abstract void readDataBlock();

	public abstract void sendBuffer(byte[] buffer);

	protected abstract void closeConnection();

	protected volatile boolean mConnected = false;

	//protected final int BUFFER_SIZE = 8192;

	//protected byte[] mReadBuffer = new byte[BUFFER_SIZE];

	protected final ByteBuffer mReadBuffer = ByteBuffer.allocate(1024);

	protected int mReadBufferSize;

	public OnSerialDataListener mListener;

	public interface OnSerialDataListener {
		/**
		 * Called when new incoming data is available.
		 */
		public void onNewData(byte[] data);

		/**
		 * Called when {@link SerialInputOutputManager#run()} aborts due to an
		 * error.
		 */
		public void onRunError(Exception e);
	}

	public synchronized void setListener(OnSerialDataListener listener) {
		mListener = listener;
	}

	int count = 0;

	private void handleData() {
		if (mReadBufferSize == 0 || mListener == null) {
			return;
		}
		
		final byte[] data = new byte[mReadBufferSize];
		mReadBuffer.rewind();
		mReadBuffer.get(data, 0, mReadBufferSize);
		mListener.onNewData(data);
		mReadBuffer.clear();

	}

	@Override
	public void run() {
		mConnected = true;
		openConnection();
		while (mConnected) {
			readDataBlock();
			handleData();
		}
	}

	public void disconnect() {
		mConnected = false;
		closeConnection();
	}
}
