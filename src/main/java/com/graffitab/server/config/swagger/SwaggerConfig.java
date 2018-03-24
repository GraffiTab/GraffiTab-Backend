package com.graffitab.server.config.swagger;

import com.google.common.base.Predicate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.BasicAuth;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(paths())
                .build()
                .apiInfo(metadata())
                .securitySchemes(securitySchemes());
    }

    private List<SecurityScheme> securitySchemes() {
        List<SecurityScheme> schemeList = new ArrayList<>();
        schemeList.add(new BasicAuth("basicAuth"));
        return schemeList;
    }

    private ApiInfo metadata() {
        return new ApiInfo(
                "GraffiTab API Documentation",
                "REST API for GraffiTab's services",
                "1.0",
                "Terms of service",
                "Georgi Christov",
                "Apache License Version 2.0",
                "https://www.apache.org/licenses/LICENSE-2.0");
    }

    private Predicate<String> paths() {
        return or(
                regex("/v1.*"));
    }
}
