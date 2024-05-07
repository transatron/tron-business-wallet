package com.transatron.payroll;

public class ActionResult<T> {
    T result;
    String errorMessage;

    public ActionResult(T result) {
        this.result = result;
    }

    public ActionResult(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public T getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isError() {
        return errorMessage != null;
    }
    public boolean isSuccess() {
        return errorMessage == null;
    }
}
