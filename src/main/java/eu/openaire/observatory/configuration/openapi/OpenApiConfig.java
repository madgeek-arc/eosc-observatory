/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.openaire.observatory.configuration.openapi;

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
