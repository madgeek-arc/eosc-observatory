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
import eu.openaire.observatory.utils.UserIds;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Produces a stable, non-reversible reference to an erased data subject, for the purge report line.
 *
 * <p>Art. 17 gives the erasure right, but Art. 5(2) and Art. 24 require the controller to be able to
 * <em>demonstrate</em> compliance — so erasing every trace of the erasure itself defeats the purpose.
 * This lets a report answer "was this person purged?" without retaining a readable address.
 *
 * <p>It is a keyed HMAC rather than a plain digest on purpose. Email addresses are low-entropy, so an
 * unsalted SHA-256 falls to a dictionary attack in seconds — that would be plaintext with extra
 * steps. With a stable key the same address always yields the same reference, so lookup still works,
 * while a reader of the log file cannot reverse it.
 *
 * <p><b>State plainly to the DPO:</b> even keyed, this is <em>pseudonymous, not anonymous</em>. GDPR
 * still applies to it. The gain is reduced exposure, not exemption.
 */
@Component
public class ErasureSubjectReference {

    private static final String ALGORITHM = "HmacSHA256";

    private final ApplicationProperties applicationProperties;

    public ErasureSubjectReference(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /**
     * Returns the hex-encoded HMAC-SHA256 of the normalized user id.
     *
     * <p>Normalizing first means the reference is stable regardless of casing or the quote-wrapping
     * seen in stored ids, and that re-running {@code purge()} logs an identical reference — the
     * idempotent behaviour the purge is designed for.
     */
    public String of(String userId) {
        String secret = applicationProperties.getErasureHashSecret();
        if (secret == null || secret.isBlank()) {
            throw new ServiceException("observatory.erasureHashSecret is not configured; "
                    + "a purge cannot be recorded without it.");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(
                    mac.doFinal(UserIds.normalize(userId).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new ServiceException("Could not compute the erasure subject reference.", e);
        }
    }
}
