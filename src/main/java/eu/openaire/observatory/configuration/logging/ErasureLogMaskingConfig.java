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

package eu.openaire.observatory.configuration.logging;

import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.erasure.repository.ErasureRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Primes {@link ErasureLogMasking} once the Spring context is up: pushes the HMAC secret and the
 * subject references of successful erasures into the static holder the log4j2 rewrite policy reads.
 * {@code ErasureRegisterService} keeps the set current for erasures that happen after startup.
 *
 * <p>Runs on {@link ApplicationReadyEvent} rather than in a constructor so the {@code commenting}
 * datasource is fully initialised before the erasure query. Log lines emitted during startup are not
 * masked, which is acceptable — no request is in flight, so none carry a {@code user} field.
 */
@Component
public class ErasureLogMaskingConfig {

    private static final Logger logger = LoggerFactory.getLogger(ErasureLogMaskingConfig.class);

    private final ApplicationProperties applicationProperties;
    private final ErasureRecordRepository erasureRecordRepository;

    public ErasureLogMaskingConfig(ApplicationProperties applicationProperties,
                                   ErasureRecordRepository erasureRecordRepository) {
        this.applicationProperties = applicationProperties;
        this.erasureRecordRepository = erasureRecordRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    void primeLogMasking() {
        String secret = applicationProperties.getHmacSecret();
        if (secret == null || secret.isBlank()) {
            logger.warn("Erasure log masking is disabled: observatory.hmacSecret is not set. "
                    + "An erased user's identifier will still appear in the 'user' log field.");
            ErasureLogMasking.configure(null, List.of());
            return;
        }
        List<String> refs = new ArrayList<>();
        erasureRecordRepository.findAllByOutcome("SUCCESS").forEach(record -> refs.add(record.getSubjectRef()));
        ErasureLogMasking.configure(secret, refs);
        logger.info("Erasure log masking primed with {} known subject(s).", refs.size());
    }
}
