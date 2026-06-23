package com.campus.management.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Result<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(true, message, data);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(true, "操作成功", data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(false, message, null);
    }
}
