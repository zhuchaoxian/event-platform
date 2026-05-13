package org.zc.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(T data) {
        Result<T> r = new Result<>();
        r.code = 1;
        r.msg = "fail";
        r.data = data;
        return r;
    }
}
