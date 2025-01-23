package eu.eosc.observatory.configuration.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OpenApiConfig {

    private final OpenApiProperties properties;

    public OpenApiConfig(OpenApiProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    public OpenAPI defaultOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        if (properties != null) {
            if (properties.getServer() != null) {
                openAPI.addServersItem(properties.getServer());
            }
            if (properties.getInfo() != null) {
                openAPI.info(properties.getInfo());
            }
        }
        return openAPI;
    }
}
