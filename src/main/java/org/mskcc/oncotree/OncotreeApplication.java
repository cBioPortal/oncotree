package org.mskcc.oncotree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class OncotreeApplication  extends SpringBootServletInitializer{

    public static void main(String[] args) {
        SpringApplication.run(OncotreeApplication.class, args);
    }

    @Bean
    ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
            "OncoTree API",
            "OncoTree API definition from cBioPortal, MSKCC",
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
            .apiInfo(apiInfo());
    }

}
