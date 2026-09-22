package com.campusone.system.controller;

import com.campusone.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "系统管理")
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    @Operation(summary = "系统信息")
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> systemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("version", "1.0.0");
        info.put("name", "CampusOne 智慧校园综合服务平台");
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        return ApiResponse.success(info);
    }
}
