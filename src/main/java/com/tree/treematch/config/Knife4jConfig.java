package com.tree.treematch.config;


import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * 自定义Swagger 接口文档配置
 * @author tree
 */
@Slf4j
@Configuration
@EnableSwagger2WebMvc
@EnableKnife4j
@Profile({"dev","test"})
public class Knife4jConfig {
    @Bean
    public Docket createRestApi() {
        // 文档类型
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                //这里标注控制器controller位置
                .apis(RequestHandlerSelectors.basePackage("com.tree.treematch.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * api信息
     * @return
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("匹配树")
                .termsOfServiceUrl("https://gitee.com/Tree-Arthur")
                .version("1.0")
                .description("匹配树接口文档")
                .build();
    }
}