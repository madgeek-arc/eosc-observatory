/*
 * Copyright 2026-2026 OpenAIRE AMKE
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

package eu.openaire.observatory.service;

import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.utils.EmailMasking;
import eu.openaire.observatory.utils.UserIds;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Produces a stable, non-reversible pseudonym for a user id, so a user can be identified or
 * correlated without exposing their address.
 *
 * <p>It is a keyed HMAC rather than a plain digest on purpose. Email addresses are low-entropy, so an
 * unsalted SHA-256 falls to a dictionary attack in seconds — that would be plaintext with extra
 * steps. With a stable key the same address always yields the same pseudonym, so lookup still works,
 * while anyone without the key cannot reverse it.
 *
 * <p><b>State plainly to the DPO:</b> even keyed, this is <em>pseudonymous, not anonymous</em>. GDPR
 * still applies to it. The gain is reduced exposure, not exemption.
 */
@Component
public class UserPseudonymizer {

    private static final String ALGORITHM = "HmacSHA256";

    private final ApplicationProperties applicationProperties;

    public UserPseudonymizer(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /**
     * Returns the hex-encoded HMAC-SHA256 of the normalized user id.
     *
     * <p>Normalizing first means the pseudonym is stable regardless of casing or the quote-wrapping
     * seen in stored ids, so one person always maps to one pseudonym.
     */
    public String of(String userId) {
        String secret = applicationProperties.getHmacSecret();
        if (secret == null || secret.isBlank()) {
            throw new ServiceException("observatory.hmacSecret is not configured; "
                    + "user ids cannot be pseudonymized without it.");
        }
        return hmacHex(secret, UserIds.normalize(userId));
    }

    /**
     * Same as {@link #of(String)} but never throws: falls back to a character-masked value (see
     * {@link EmailMasking#mask}) when the secret isn't configured, since pseudonymizing a log field
     * is peripheral and must never break the caller or swallow the exception it's logging alongside.
     */
    public String forLogging(String value) {
        try {
            return of(value);
        } catch (ServiceException e) {
            return EmailMasking.mask(value);
        }
    }

    /**
     * Same as {@link #forLogging(String)} for the user behind {@code auth}; returns {@code "unknown"}
     * when no user id can be obtained from it.
     */
    public String forLogging(Authentication auth) {
        try {
            return forLogging(User.getId(auth));
        } catch (InsufficientAuthenticationException e) {
            return "unknown";
        }
    }

    /**
     * The raw keyed HMAC used by {@link #of}, for callers that already hold the secret and need the
     * same pseudonym without an {@link ApplicationProperties} lookup. {@code value} is hashed as-is —
     * callers normalize first (see {@link #of}).
     */
    public static String hmacHex(String secret, String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new ServiceException("Could not compute the user pseudonym.", e);
        }
    }
}
