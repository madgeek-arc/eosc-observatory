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


package eu.openaire.observatory.configuration;

import com.sun.net.httpserver.HttpServer;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import gr.athenarc.messaging.mailer.service.Mailer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailerConfigTest {

    private HttpServer server;
    private final List<String> apiKeys = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/mailer/mails", exchange -> {
            String key = exchange.getRequestHeaders().getFirst("X-API-Key");
            apiKeys.add(key == null ? "<none>" : key);
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private Mailer mailer(String apiKey) {
        String host = "http://localhost:" + server.getAddress().getPort() + "/mailer";
        return new MailerConfig().resilientMailer(RestClient.builder(), host, apiKey,
                Duration.ofSeconds(3), Duration.ofSeconds(3), 1, Duration.ZERO);
    }

    private static EmailMessage email() {
        return new EmailMessage.EmailBuilder()
                .setFrom("no-reply@test.com")
                .setTo(List.of("a@test.com"))
                .setSubject("subject")
                .setText("body")
                .build();
    }

    @Test
    void sendsTheConfiguredApiKey() {
        mailer("secret").sendMail(email());

        assertEquals(List.of("secret"), apiKeys);
    }

    @Test
    void sendsNoApiKeyWhenUnset() {
        mailer("").sendMail(email());

        assertEquals(List.of("<none>"), apiKeys);
    }
}
