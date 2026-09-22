package com.campusone.security;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("token_blacklist")
public class TokenBlacklist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String token;
    private Long userId;
    private LocalDateTime expiresAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
