package com.campusone.system.log.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String module;
    private String action;
    private String method;
    private String url;
    private String ip;
    private String params;
    private String result;
    private Integer status;
    private Long duration;
    private LocalDateTime createdAt;
}
