/*
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
package eu.openaire.observatory;

import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTestConfig {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("registry")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final ElasticsearchContainer elastic =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.23")
                    .withPassword("password")
                    // disable SSL
                    .withEnv("xpack.security.transport.ssl.enabled", "false")
                    .withEnv("xpack.security.http.ssl.enabled", "false");

    static {
        postgres.start();
        elastic.start();
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("registry.datasource.url", postgres::getJdbcUrl);
        registry.add("registry.datasource.username", postgres::getUsername);
        registry.add("registry.datasource.password", postgres::getPassword);

        registry.add("authorization.datasource.url", postgres::getJdbcUrl);
        registry.add("authorization.datasource.username", postgres::getUsername);
        registry.add("authorization.datasource.password", postgres::getPassword);

        registry.add("commenting.datasource.url", postgres::getJdbcUrl);
        registry.add("commenting.datasource.username", postgres::getUsername);
        registry.add("commenting.datasource.password", postgres::getPassword);

        registry.add("datasets.datasource.url", postgres::getJdbcUrl);
        registry.add("datasets.datasource.username", postgres::getUsername);
        registry.add("datasets.datasource.password", postgres::getPassword);

        registry.add("registry.elasticsearch.uris", elastic::getHttpHostAddress);
        registry.add("registry.elasticsearch.username", () -> "elastic");
        registry.add("registry.elasticsearch.password", () -> "password");
    }


}