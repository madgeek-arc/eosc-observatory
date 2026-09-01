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

package eu.openaire.observatory.messaging.mailer;

import gr.athenarc.messaging.mailer.RelativePaths;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import gr.athenarc.messaging.mailer.service.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;

/**
 * {@link Mailer} that hands each message to the mailer service over one HTTP POST, wrapped in a
 * bounded retry. Replaces the library's {@code MailClient}, whose {@code RestTemplate} has no
 * connect/read timeout and no retry, so a hung mailer would block the calling thread forever and a
 * single transient blip would abort a whole batch of notifications.
 *
 * <p>Applied centrally: this is the {@code @Primary Mailer} (see {@code MailerConfig}), so every
 * mail-sending service picks it up by type with no code change.
 *
 * <p>Only transient failures are retried (mailer unreachable / overloaded / erroring). A 4xx
 * response means we sent a malformed request, so it fails immediately. After the last failed
 * attempt one ERROR is logged (subject + recipient count, never addresses) and a
 * {@link MailDeliveryException} is thrown for the caller's existing {@code catch (Exception)} guard.
 */
public class ResilientMailer implements Mailer {

    private static final Logger logger = LoggerFactory.getLogger(ResilientMailer.class);

    private final RestTemplate restTemplate;
    private final String mailsUri;
    private final int maxAttempts;
    private final Duration retryDelay;

    public ResilientMailer(RestTemplate restTemplate, String host, int maxAttempts, Duration retryDelay) {
        this.restTemplate = restTemplate;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryDelay = retryDelay != null ? retryDelay : Duration.ZERO;
        this.mailsUri = UriComponentsBuilder.fromUriString(host == null ? "" : host)
                .path(RelativePaths.MAILS)
                .build().encode().toUri().toString();
        if (host == null || host.isBlank()) {
            logger.warn("mailer.client.host is not configured; outgoing mail will fail on the first attempt");
        }
    }

    /**
     * Sends on the calling thread by design (for validation notifications, the request thread). If
     * operational experience shows the mailer is flaky enough to matter, the upgrade is to annotate
     * the call site {@code @Async} and route it through a dedicated {@code ThreadPoolTaskExecutor}
     * (see {@code eu.openaire.observatory.configuration.AsyncConfig}) so the round-trip plus retries
     * no longer add latency to the user request.
     *
     * <p>A retry after a read timeout can double-deliver if the mailer actually processed the first
     * POST (there is no server-side dedupe key). A duplicate notification is accepted as the lesser
     * evil compared with a dropped one.
     */
    @Override
    public void sendMail(EmailMessage email) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                postOnce(email);
                return;
            } catch (RuntimeException ex) {
                if (!isRetryable(ex) || attempt >= maxAttempts) {
                    logger.error("Mail delivery failed after {} attempt(s) [subject=\"{}\", recipients={}]: {}",
                            attempt, email.getSubject(), recipientCount(email), rootCause(ex));
                    throw new MailDeliveryException("Mail delivery failed after " + attempt
                            + " attempt(s) for subject \"" + email.getSubject() + "\"", ex);
                }
                logger.warn("Mail delivery attempt {}/{} failed [subject=\"{}\"]; retrying in {}: {}",
                        attempt, maxAttempts, email.getSubject(), retryDelay, rootCause(ex));
                sleepBeforeRetry();
            }
        }
    }

    /** One POST to the mailer. Package-private so the retry loop can be unit-tested via a spy. */
    void postOnce(EmailMessage email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForObject(mailsUri, new HttpEntity<>(email, headers), Void.class);
    }

    /**
     * Retry only failures that are the mailer service having a bad moment; fail fast on anything that
     * says our request was wrong.
     */
    static boolean isRetryable(RuntimeException ex) {
        if (ex instanceof HttpServerErrorException) {
            return true;                                   // 5xx — mailer-side, transient
        }
        if (ex instanceof HttpClientErrorException) {
            return false;                                  // 4xx — we sent a malformed request
        }
        if (ex instanceof ResourceAccessException) {       // I/O: connect refused, socket reset, read timeout
            return !(ex.getCause() instanceof UnknownHostException);   // bad host is misconfig, not transient
        }
        return false;                                      // blank-host IllegalArgumentException, anything unrecognised
    }

    private void sleepBeforeRetry() {
        if (retryDelay.isZero() || retryDelay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(retryDelay.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new MailDeliveryException("Interrupted while waiting to retry mail delivery", ie);
        }
    }

    private static int recipientCount(EmailMessage email) {
        return size(email.getTo()) + size(email.getCc()) + size(email.getBcc());
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static String rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
