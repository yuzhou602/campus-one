package com.campusone.application.controller;

import com.campusone.application.support.ServiceCatalogRegistry;
import com.campusone.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "服务目录")
@RestController
@RequestMapping("/api/v1/services")
public class ServiceCatalogController {

    @Operation(summary = "服务列表")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(ServiceCatalogRegistry.SERVICES.stream().map(s ->
            Map.of("id", (Object) s.id(), "name", s.name(), "description", s.description(),
                   "icon", s.icon(), "reviewRole", s.reviewRole())
        ).toList());
    }
}
