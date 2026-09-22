package com.campusone.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
public class SpringDocConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("CampusOne 智慧校园综合服务平台 API")
                .description("基于 Spring Boot 3 + MyBatis-Plus 的智慧校园综合服务平台")
                .version("1.0.0"));
    }
}
