package com.transatron.payroll.tt;

public class TTServiceException extends Exception{
    private int errorCode;

    public TTServiceException(int errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

}
