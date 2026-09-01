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

import gr.athenarc.messaging.mailer.domain.EmailMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResilientMailerTest {

    private static final String HOST = "http://mailer.test/mailer";
    private static final String MAILS_URI = "http://mailer.test/mailer/mails";

    private static EmailMessage sampleEmail() {
        return new EmailMessage.EmailBuilder()
                .setFrom("no-reply@openaire.eu")
                .setBcc(List.of("a@test.com", "b@test.com"))
                .setSubject("Answer validated")
                .setText("<p>body</p>")
                .setHtml(true)
                .build();
    }

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }

    /** Spy so the retry loop can be driven without real HTTP; retryDelay 0 keeps it instant. */
    private ResilientMailer spyMailer(int maxAttempts) {
        return spy(new ResilientMailer(new RestTemplate(), HOST, maxAttempts, Duration.ZERO));
    }

    // ── retry loop ──────────────────────────────────────────────────────────

    @Test
    void sendsOnceWhenThePostSucceeds() {
        ResilientMailer mailer = spyMailer(5);
        doNothing().when(mailer).postOnce(any());

        mailer.sendMail(sampleEmail());

        verify(mailer, times(1)).postOnce(any());
    }

    @Test
    void retriesTransientFailureThenSucceeds() {
        ResilientMailer mailer = spyMailer(5);
        doThrow(new ResourceAccessException("io", new SocketTimeoutException("read timed out")))
                .doThrow(new ResourceAccessException("io", new ConnectException("refused")))
                .doNothing()
                .when(mailer).postOnce(any());

        mailer.sendMail(sampleEmail());

        verify(mailer, times(3)).postOnce(any());
    }

    @Test
    void exhaustsMaxAttemptsThenThrowsMailDeliveryException() {
        ResilientMailer mailer = spyMailer(5);
        doThrow(new ResourceAccessException("io", new ConnectException("refused")))
                .when(mailer).postOnce(any());

        MailDeliveryException ex = assertThrows(MailDeliveryException.class,
                () -> mailer.sendMail(sampleEmail()));

        verify(mailer, times(5)).postOnce(any());
        assertInstanceOf(ConnectException.class, rootCause(ex));
    }

    @Test
    void retriesServerErrors() {
        ResilientMailer mailer = spyMailer(3);
        doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .when(mailer).postOnce(any());

        assertThrows(MailDeliveryException.class, () -> mailer.sendMail(sampleEmail()));

        verify(mailer, times(3)).postOnce(any());
    }

    @Test
    void doesNotRetryClientErrors() {
        ResilientMailer mailer = spyMailer(5);
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .when(mailer).postOnce(any());

        assertThrows(MailDeliveryException.class, () -> mailer.sendMail(sampleEmail()));

        verify(mailer, times(1)).postOnce(any());
    }

    @Test
    void doesNotRetryUnknownHost() {
        ResilientMailer mailer = spyMailer(5);
        doThrow(new ResourceAccessException("io", new UnknownHostException("mailer.test")))
                .when(mailer).postOnce(any());

        assertThrows(MailDeliveryException.class, () -> mailer.sendMail(sampleEmail()));

        verify(mailer, times(1)).postOnce(any());
    }

    @Test
    void respectsMaxAttemptsOfOne() {
        ResilientMailer mailer = spyMailer(1);
        doThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY))
                .when(mailer).postOnce(any());

        assertThrows(MailDeliveryException.class, () -> mailer.sendMail(sampleEmail()));

        verify(mailer, times(1)).postOnce(any());
    }

    // ── classification ─────────────────────────────────────────────────────

    @Test
    void isRetryableClassification() {
        assertTrue(ResilientMailer.isRetryable(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)));
        assertTrue(ResilientMailer.isRetryable(new ResourceAccessException("io", new ConnectException())));
        assertTrue(ResilientMailer.isRetryable(new ResourceAccessException("io", new SocketTimeoutException())));

        assertFalse(ResilientMailer.isRetryable(new HttpClientErrorException(HttpStatus.BAD_REQUEST)));
        assertFalse(ResilientMailer.isRetryable(new ResourceAccessException("io", new UnknownHostException())));
        assertFalse(ResilientMailer.isRetryable(new IllegalArgumentException("URI is not absolute")));
    }

    // ── integration with a real RestTemplate (URL, payload, exception wrapping) ──

    @Test
    void postsBodyAsJsonToTheMailsEndpoint() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(ExpectedCount.once(), requestTo(MAILS_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.subject").value("Answer validated"))
                .andRespond(withSuccess());

        new ResilientMailer(restTemplate, HOST, 5, Duration.ZERO).sendMail(sampleEmail());

        server.verify();
    }

    @Test
    void wrapsRealIoErrorAsRetryable() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(ExpectedCount.times(2), requestTo(MAILS_URI))
                .andRespond(withException(new SocketTimeoutException("read timed out")));
        server.expect(ExpectedCount.once(), requestTo(MAILS_URI))
                .andRespond(withSuccess());

        assertDoesNotThrow(() ->
                new ResilientMailer(restTemplate, HOST, 5, Duration.ZERO).sendMail(sampleEmail()));

        server.verify();
    }

    @Test
    void failsFastOnRealClientError() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(ExpectedCount.once(), requestTo(MAILS_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(MailDeliveryException.class, () ->
                new ResilientMailer(restTemplate, HOST, 5, Duration.ZERO).sendMail(sampleEmail()));

        server.verify();
    }
}
