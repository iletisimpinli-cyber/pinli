// File: app/src/main/java/com/pinli/app/core/Result.java
package com.pinli.app.core;

public final class Result<T> {
    public final boolean ok;
    public final T data;
    public final String error;

    private Result(boolean ok, T data, String error) {
        this.ok = ok;
        this.data = data;
        this.error = error;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(true, data, null);
    }

    public static <T> Result<T> failure(String error) {
        return new Result<>(false, null, error);
    }
}
