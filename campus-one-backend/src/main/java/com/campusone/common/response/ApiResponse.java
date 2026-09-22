package com.campusone.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private boolean success;
    private long timestamp;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        r.setSuccess(true);
        r.setTimestamp(Instant.now().toEpochMilli());
        return r;
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(code);
        r.setMessage(message);
        r.setSuccess(false);
        r.setTimestamp(Instant.now().toEpochMilli());
        return r;
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(500, message);
    }
}
