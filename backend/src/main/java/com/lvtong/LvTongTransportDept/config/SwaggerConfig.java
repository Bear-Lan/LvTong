package com.lvtong.LvTongTransportDept.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("绿通快检系统 API")
                        .version("1.1.5")
                        .description("绿通运输查验系统后端接口文档，包含用户管理、登录注册等功能")
                        .contact(new Contact()
                                .name("达生智能")
                                .email("536537642@qq.com"))
                        .license(new License()
                                .name("MIT License")));
    }
}
