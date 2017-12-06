package com.robotleo.hardware.serial.driver;

public class UsbSerialRuntimeException extends RuntimeException {

    public UsbSerialRuntimeException() {
        super();
    }

    public UsbSerialRuntimeException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public UsbSerialRuntimeException(String detailMessage) {
        super(detailMessage);
    }

    public UsbSerialRuntimeException(Throwable throwable) {
        super(throwable);
    }

}
