/*
 * Copyright 2021-2026 OpenAIRE AMKE
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

package eu.openaire.observatory;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTestConfig {

    @MockBean
    ClientRegistrationRepository clientRegistrationRepository;

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("registry")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("registry.datasource.url", postgres::getJdbcUrl);
        registry.add("registry.datasource.username", postgres::getUsername);
        registry.add("registry.datasource.password", postgres::getPassword);
        registry.add("registry.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("authorization.datasource.url", postgres::getJdbcUrl);
        registry.add("authorization.datasource.username", postgres::getUsername);
        registry.add("authorization.datasource.password", postgres::getPassword);
        registry.add("authorization.datasource.driverClassName", () -> "org.postgresql.Driver");

        registry.add("commenting.datasource.url", postgres::getJdbcUrl);
        registry.add("commenting.datasource.username", postgres::getUsername);
        registry.add("commenting.datasource.password", postgres::getPassword);
        registry.add("commenting.datasource.driverClassName", () -> "org.postgresql.Driver");

        registry.add("datasets.datasource.url", postgres::getJdbcUrl);
        registry.add("datasets.datasource.username", postgres::getUsername);
        registry.add("datasets.datasource.password", postgres::getPassword);
        registry.add("datasets.datasource.driverClassName", () -> "org.postgresql.Driver");

    }


}