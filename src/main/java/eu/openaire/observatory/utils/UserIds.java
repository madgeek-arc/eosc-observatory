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

package eu.openaire.observatory.utils;

import java.util.Locale;

/**
 * Canonical form for user ids.
 *
 * <p>A user's email address <em>is</em> their id, so every id that reaches storage has to be
 * written in one canonical form — otherwise erasure, which is an exact-match search-and-replace,
 * silently misses it. A single {@code stakeholder} version row holding
 * {@code "\"someone@example.org\""} survived a completed purge for exactly this reason: an endpoint
 * had accepted the address with its quotes included.
 *
 * <p>Normalize on <b>write</b>, plus at the two places ids arrive from outside storage
 * ({@link eu.openaire.observatory.domain.User#getId(org.springframework.security.core.Authentication)}
 * and the purge path variable). Anything later read back out of storage is then already canonical,
 * so reads need no further treatment.
 *
 * <p>Deliberately a plain static rather than a Spring bean: {@code User}, {@code UserGroup} and
 * {@code CommentMessage} are constructed by Jackson and Hibernate, so nothing injectable reaches
 * them. It lives in a neutral utility rather than on {@code User} because the commenting package's
 * entities reference nothing else from the main domain, and a string helper should not be what
 * couples them.
 */
public final class UserIds {

    private UserIds() {
        // utility class
    }

    /**
     * Returns the canonical form of a user id: trimmed, stripped of surrounding double quotes, and
     * lower-cased. {@code null} in, {@code null} out.
     *
     * <p>{@link Locale#ROOT} is used deliberately — the default-locale {@code toLowerCase()} maps
     * {@code I} to a dotless {@code ı} under a Turkish locale, which would make the canonical form
     * depend on where the server happens to run.
     */
    public static String normalize(String userId) {
        if (userId == null) {
            return null;
        }
        String normalized = userId.trim();
        if (normalized.length() > 1 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
