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

import eu.openaire.observatory.messaging.mailer.ResilientMailer;
import gr.athenarc.messaging.mailer.service.Mailer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Wires {@link ResilientMailer} as the primary {@link Mailer} for every profile except
 * {@code mail-debug}, where {@link MailDebugConfig} supplies its own primary capturing bean instead.
 * The two configurations are mutually exclusive, so exactly one {@code @Primary Mailer} exists in
 * any run. The mailer library's auto-configured {@code mailerClient} bean stays in the context,
 * unreferenced.
 *
 * <p>Timeouts and retry budget are read from {@code mailer.client.*} / {@code mailer.retry.*} with
 * the inline defaults below, so the app still starts if the keys are absent.
 */
@Configuration
@Profile("!mail-debug")
public class MailerConfig {

    @Bean
    @Primary
    public Mailer resilientMailer(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${mailer.client.host:}") String host,
            @Value("${mailer.client.connect-timeout:3s}") Duration connectTimeout,
            @Value("${mailer.client.read-timeout:3s}") Duration readTimeout,
            @Value("${mailer.retry.max-attempts:5}") int maxAttempts,
            @Value("${mailer.retry.delay:1s}") Duration retryDelay) {
        RestTemplate restTemplate = restTemplateBuilder
                .requestFactorySettings(settings -> settings.withTimeouts(connectTimeout, readTimeout))
                .build();
        return new ResilientMailer(restTemplate, host, maxAttempts, retryDelay);
    }
}
