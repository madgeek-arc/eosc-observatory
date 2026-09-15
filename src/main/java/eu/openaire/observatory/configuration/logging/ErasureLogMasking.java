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

import eu.openaire.observatory.service.ErasureSubjectReference;
import eu.openaire.observatory.utils.UserIds;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static bridge between Spring and the log4j2 {@link ErasureMaskingRewritePolicy}.
 *
 * <p>The rewrite policy is created by log4j2 during logging configuration, long before the Spring
 * context exists, so it cannot be given the HMAC secret or the set of erased subjects by injection.
 * {@link ErasureLogMaskingConfig} pushes both in here once the context is ready, and
 * {@code ErasureRegisterService} appends to the set as new erasures land. Until {@link #configure}
 * has run, {@link #maskedReferenceFor} returns {@code null} (pass the log line through untouched) —
 * fail-safe, never fail-loud, for something as peripheral as a log field.
 */
public final class ErasureLogMasking {

    private static final Set<String> ERASED_SUBJECT_REFS = ConcurrentHashMap.newKeySet();

    private static volatile String secret;

    private ErasureLogMasking() {
    }

    /** Called once at startup with the configured secret and the subject refs already on record. */
    public static void configure(String hmacSecret, Collection<String> knownSubjectRefs) {
        secret = (hmacSecret == null || hmacSecret.isBlank()) ? null : hmacSecret;
        ERASED_SUBJECT_REFS.addAll(knownSubjectRefs);
    }

    /** Called by {@code ErasureRegisterService} when a fresh erasure is recorded. */
    public static void registerErased(String subjectRef) {
        if (subjectRef != null) {
            ERASED_SUBJECT_REFS.add(subjectRef);
        }
    }

    /**
     * @return {@code "erased:<hmac>"} when {@code userValue} hashes to a known erased subject, or
     *         {@code null} when it does not, when masking is not configured yet, or when the value
     *         is empty.
     */
    public static String maskedReferenceFor(String userValue) {
        String currentSecret = secret;
        if (currentSecret == null || userValue == null || userValue.isBlank()) {
            return null;
        }
        String ref = ErasureSubjectReference.hmacHex(currentSecret, UserIds.normalize(userValue));
        return ERASED_SUBJECT_REFS.contains(ref) ? "erased:" + ref : null;
    }

    /** Test seam. */
    static void reset() {
        secret = null;
        ERASED_SUBJECT_REFS.clear();
    }
}
