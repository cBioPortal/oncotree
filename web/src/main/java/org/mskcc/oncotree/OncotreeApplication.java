package org.mskcc.oncotree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class OncotreeApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(OncotreeApplication.class);
    }
    
    public static void main(String[] args) {
        SpringApplication.run(OncotreeApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**");
            }

            @Override
            public void addViewControllers(final ViewControllerRegistry registry) {
                super.addViewControllers(registry);
                registry.addRedirectViewController("/oncotree-mappings/","/#/home?tab=mapping").setKeepQueryParams(true).setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        
            }
        };
    }

    @Bean
    ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
            "OncoTree API",
            "OncoTree API definition from MSKCC",
            "0.0.1",
            "",
            "",
            "",
            "");
        return apiInfo;
    }

    @Bean
    public Docket customImplementation() {
        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("org.mskcc.oncotree.api"))
            .build()
            .useDefaultResponseMessages(false)
            .apiInfo(apiInfo());
    }

}
