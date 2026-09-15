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

package eu.openaire.observatory.service;

import eu.openaire.observatory.commenting.domain.ErasureRecord;
import eu.openaire.observatory.commenting.repository.ErasureRecordRepository;
import eu.openaire.observatory.configuration.logging.ErasureLogMasking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the durable erasure register row for {@link UserServiceImpl#purge}.
 *
 * <p>Carries its own {@code @Transactional} boundary on the {@code commenting} datasource because
 * {@code purge()} is deliberately not transactional (it spans several stores) — the same pattern by
 * which {@code purge()} already delegates to the transactional {@code SurveyAnswerCommentService}.
 */
@Service
public class ErasureRegisterService {

    private static final Logger logger = LoggerFactory.getLogger(ErasureRegisterService.class);

    private final ErasureRecordRepository repository;

    public ErasureRegisterService(ErasureRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Records an erasure, once per subject. Check-then-insert on the natural (HMAC) primary key, so a
     * re-run of {@code purge()} after a partial failure — every step of which is idempotent — does
     * not overwrite the authoritative first record.
     *
     * @return {@code true} if a new row was written, {@code false} if one already existed.
     */
    @Transactional("commentingTransactionManager")
    public boolean record(ErasureRecord record) {
        if (repository.existsById(record.getSubjectRef())) {
            logger.debug("Erasure register already holds subject {}; leaving the original record untouched.",
                    record.getSubjectRef());
            return false;
        }
        repository.save(record);
        // Keep the log-masking layer current so this subject is masked in logs without a restart.
        ErasureLogMasking.registerErased(record.getSubjectRef());
        return true;
    }
}
