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

import eu.openaire.observatory.configuration.logging.ErasureLogMasking;
import eu.openaire.observatory.erasure.domain.ErasureRecord;
import eu.openaire.observatory.erasure.repository.ErasureRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Writes the durable erasure register row for {@link UserServiceImpl#purge}.
 *
 * <p>Carries its own {@code @Transactional} boundary on the {@code erasure} datasource because
 * {@code purge()} is deliberately not transactional (it spans several stores).
 */
@Service
public class ErasureRegisterService {

    private final ErasureRecordRepository repository;

    public ErasureRegisterService(ErasureRecordRepository repository) {
        this.repository = repository;
    }

    /** Start a new erasure, or retain the figures of an unfinished attempt being retried. */
    @Transactional("erasureTransactionManager")
    public UUID begin(ErasureRecord record) {
        // Hold through commit so competing starts observe and reuse the winning pending row.
        repository.lockSubject(record.getSubjectRef());
        var existing = repository.findBySubjectRefAndOutcome(record.getSubjectRef(), "PENDING");
        if (existing.isPresent()) {
            return existing.get().getAttemptId();
        }
        record.setOutcome("PENDING").setCompletedAt(null);
        repository.save(record);
        return record.getAttemptId();
    }

    @Transactional(value = "erasureTransactionManager", readOnly = true)
    public boolean isPending(String subjectRef) {
        return repository.findBySubjectRefAndOutcome(subjectRef, "PENDING").isPresent();
    }

    /** Called only after deletion succeeds; failure leaves the committed PENDING row retryable. */
    @Transactional("erasureTransactionManager")
    public void complete(UUID attemptId) {
        ErasureRecord record = repository.findById(attemptId).orElseThrow();
        if ("SUCCESS".equals(record.getOutcome())) {
            return; // A repeated completion must preserve the original completion time.
        }
        if (!"PENDING".equals(record.getOutcome())) {
            throw new IllegalStateException("Only pending erasure attempts can be completed");
        }
        record.setOutcome("SUCCESS").setCompletedAt(Instant.now());
        repository.save(record);
        ErasureLogMasking.registerErased(record.getSubjectRef());
    }

}
